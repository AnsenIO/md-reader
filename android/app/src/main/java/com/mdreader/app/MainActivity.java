package com.mdreader.app;

import android.util.Log;
import android.view.View;

import com.getcapacitor.BridgeActivity;

/**
 * Registers FileOpenPlugin BEFORE super.onCreate() -> BridgeActivity.load(), so the plugin is
 * instantiated (and its onNewIntent replay runs) with the launch intent already set. The plugin
 * owns all pending-URI bookkeeping; see FileOpenPlugin and .scratch ticket 03 for design notes.
 *
 * Edge-to-edge: targetSdk 36 forces edge-to-edge on Android 15/16 (no opt-out), so content would
 * render under the status/nav bars — Capacitor v5's layout has no fitsSystemWindows. Pad the root
 * content view with system-window insets so markdown stays clear of system bars.
 */
public class MainActivity extends BridgeActivity {

    private static final String TAG = "MainActivity";

    @Override
    public void onCreate(android.os.Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: registering FileOpen before bridge load");
        registerPlugin(FileOpenPlugin.class);
        super.onCreate(savedInstanceState);
        applySystemWindowInsets();
    }

    private void applySystemWindowInsets() {
        View root = findViewById(android.R.id.content);
        if (root == null) return;
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            int top = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars()).top;
            int bottom = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.navigationBars()).bottom;
            v.setPadding(v.getPaddingLeft(), top, v.getPaddingRight(), bottom);
            return insets;
        });
    }
}
