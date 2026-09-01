package com.mdreader.app;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.webkit.WebView;

import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.ActivityCallback;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

@CapacitorPlugin(name = "MdReader")
public class MdReaderPlugin extends Plugin {
    
    private static final int PICK_FILE_REQUEST = 1;
    private String callId = null;
    
    @Override
    public void load() {
        // Register for activity result
    }
    
    @PluginMethod
    public void browseFiles(PluginCall call) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        
        // Filter for text files
        String[] mimeTypes = {"text/plain", "text/markdown", "application/octet-stream"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        
        // Allow multiple selection
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        
        callId = call.getCallbackId();
        call.save();
        
        getActivity().startActivityForResult(
            Intent.createChooser(intent, "Select file"),
            PICK_FILE_REQUEST
        );
    }
    
    @ActivityCallback
    protected void onActivityResult(PluginCall call, int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_FILE_REQUEST && resultCode == getActivity().RESULT_OK) {
            if (callId == null) return;
            
            PluginCall savedCall = this.getSavedCall(callId);
            if (savedCall == null) return;
            
            if (data != null && data.getData() != null) {
                Uri uri = data.getData();
                String path = uri.toString();
                savedCall.resolve();
                savedCall.set("uri", path);
            } else {
                savedCall.reject("No file selected");
            }
        } else {
            if (callId != null) {
                PluginCall savedCall = this.getSavedCall(callId);
                if (savedCall != null) {
                    savedCall.reject("Activity cancelled");
                }
            }
        }
    }
    
    @PluginMethod
    public void readFileFromUri(PluginCall call) {
        String uriString = call.getString("uri");
        if (uriString == null || uriString.isEmpty()) {
            call.reject("No URI provided");
            return;
        }
        
        Uri uri = Uri.parse(uriString);
        
        try {
            InputStream inputStream = getActivity().getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                call.reject("Cannot open file");
                return;
            }
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder content = new StringBuilder();
            String line;
            
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            
            reader.close();
            inputStream.close();
            
            call.resolve();
            call.set("content", content.toString());
            call.set("fileName", getFileName(uri));
            
        } catch (Exception e) {
            call.reject("Failed to read file: " + e.getMessage());
        }
    }
    
    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (android.database.Cursor cursor = getActivity().getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndexOrThrow(android.provider.MediaStore.MediaColumns.DISPLAY_NAME));
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            if (result != null) {
                int lastSlash = result.lastIndexOf('/');
                if (lastSlash >= 0) {
                    result = result.substring(lastSlash + 1);
                }
            }
        }
        return result;
    }
    
    @PluginMethod
    public void clearPendingFile(PluginCall call) {
        getBridge().getContext().getApplicationContext()
            .getSharedPreferences("md_reader_prefs", 0)
            .edit()
            .remove("pending_file_uri")
            .apply();
        call.resolve();
    }
}
