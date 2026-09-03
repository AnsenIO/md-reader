package com.mdreader.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * FileOpen — delivers the URI of a file tapped in an external app (file manager, share sheet)
 * to the WebView and reads content:// / file:// URIs that the WebView cannot fetch itself.
 *
 * Design notes (see .scratch/md-reader-apk/issues/03-md-file-association.md):
 * - Delivery path 1: onNewIntent fires for BOTH cold start (BridgeActivity.load() replays it)
 *   and warm restarts; the event is fired with retainUntilConsumed=true so Capacitor holds it
 *   until the first JS addListener registers — covering the intent-arrives-before-JS race.
 * - Delivery path 2: consumePending() reads+clears a SharedPreferences copy, catching events
 *   lost across process death (the retained-event map is in-memory).
 * - Dedup on lastDeliveredUri prevents double-open when the same URI flows through more than
 *   one hook; prefs are always refreshed so path 2 stays correct after reloads.
 * - readFile() goes through ContentResolver, which handles both content:// (SAF) and file://.
 */
@CapacitorPlugin(
    name = "FileOpen",
    permissions = {
        @Permission(strings = { android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED }, alias = "visualSelected")
    }
)
public class FileOpenPlugin extends Plugin {

    private static final String TAG = "MDReader_FileOpen";
    private static final String PREFS_NAME = "md_reader_prefs";
    private static final String KEY_FILE_URI = "pending_file_uri";
    private static final String[] OK_EXTS = { ".md", ".markdown" };

    /** Last URI delivered via the retained event — dedup guard for repeated onNewIntent. */
    private String lastDeliveredUri;

    @Override
    protected void handleOnNewIntent(Intent intent) {
        Log.d(TAG, "handleOnNewIntent action=" + (intent == null ? "null" : intent.getAction()));
        processIntent(intent);
    }

    private boolean isMarkdownPath(String path) {
        if (path == null) return false;
        String lower = path.toLowerCase();
        for (String ext : OK_EXTS) {
            if (lower.endsWith(ext)) return true;
        }
        return false;
    }

    private void processIntent(Intent intent) {
        if (intent == null) return;

        Uri fileUri = null;
        String action = intent.getAction();

        if (Intent.ACTION_VIEW.equals(action)) {
            fileUri = intent.getData();
        } else if (Intent.ACTION_SEND.equals(action)) {
            fileUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        }

        if (fileUri == null) return;

        String scheme = fileUri.getScheme();
        Log.d(TAG, "File URI: scheme=" + scheme + ", path=" + fileUri.getPath());

        // md/markdown from any source. txt kept out of the association set for now — see ticket 03.
        if (!isMarkdownPath(fileUri.getPath())) {
            Log.d(TAG, "Not a supported extension, ignoring");
            return;
        }

        String uriStr = fileUri.toString();

        SharedPreferences prefs = getContext().getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_FILE_URI, uriStr).apply();
        Log.d(TAG, "Stored pending URI: " + uriStr);

        if (!uriStr.equals(lastDeliveredUri)) {
            lastDeliveredUri = uriStr;
            JSObject data = new JSObject();
            data.put("uri", uriStr);
            // Retained until the first listener registers — the intent lands before JS exists.
            notifyListeners("filePending", data, true);
        } else {
            Log.d(TAG, "Same URI as last delivery, skipping event (prefs refreshed)");
        }
    }

    /** Safety net: returns and clears any stored pending URI (survives process death). */
    @PluginMethod
    public void consumePending(PluginCall call) {
        SharedPreferences prefs = getContext().getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE);
        String uri = prefs.getString(KEY_FILE_URI, null);
        if (uri != null) {
            prefs.edit().remove(KEY_FILE_URI).apply();
            Log.d(TAG, "consumePending: cleared stored URI");
        }
        JSObject data = new JSObject();
        data.put("uri", uri == null ? "" : uri);
        call.resolve(data);
    }

    /** Reads a content:// or file:// URI via ContentResolver; returns text (utf8) or base64. */
    @PluginMethod
    public void readFile(PluginCall call) {
        String path = call.getString("path");
        if (path == null || path.isEmpty()) {
            call.reject("NO_PATH");
            return;
        }

        Uri uri = path.startsWith("/") && !path.contains("://") ? Uri.fromFile(new java.io.File(path)) : Uri.parse(path);
        InputStream in = null;
        try {
            in = getContext().getContentResolver().openInputStream(uri);
            if (in == null) {
                call.reject("Unable to open: " + uri);
                return;
            }

            java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int n;
            while ((n = in.read(chunk)) != -1) {
                buf.write(chunk, 0, n);
            }
            byte[] bytes = buf.toByteArray();
            String encoding = call.getString("encoding", "utf8");

            JSObject data = new JSObject();
            if ("base64".equals(encoding)) {
                data.put("data", Base64.getEncoder().encodeToString(bytes));
            } else {
                data.put("data", new String(bytes, StandardCharsets.UTF_8));
            }

            // Filename: display name from the URI when available, else last path segment.
            String name = null;
            try {
                android.database.Cursor c = getContext().getContentResolver()
                        .query(uri, new String[] { android.provider.OpenableColumns.DISPLAY_NAME }, null, null, null);
                if (c != null) {
                    if (c.moveToFirst()) {
                        int idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                        name = idx >= 0 ? c.getString(idx) : null;
                    }
                    c.close();
                }
            } catch (Exception ignored) {}
            if (name == null || name.isEmpty()) {
                String p = uri.getPath() != null ? uri.getPath() : path;
                int slash = p.lastIndexOf('/');
                name = slash >= 0 && slash < p.length() - 1 ? p.substring(slash + 1) : "document.md";
            }
            data.put("name", Uri.decode(name));

            Log.d(TAG, "readFile OK: " + uri + " (" + bytes.length + " bytes)");
            call.resolve(data);
        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied reading " + uri, e);
            call.reject("Permission denied — grant \"All files access\" to MD Reader or share again: " + e.getMessage());
        } catch (java.io.IOException e) {
            Log.e(TAG, "IO error reading " + uri, e);
            call.reject("Could not read file: " + e.getMessage());
        } finally {
            if (in != null) {
                try { in.close(); } catch (Exception ignored) {}
            }
        }
    }

    @PermissionCallback
    private void permissionsReturned(String id, android.content.pm.PackageManager pm) {
        // Nothing to do — the read itself surfaces permission errors to JS.
    }
}
