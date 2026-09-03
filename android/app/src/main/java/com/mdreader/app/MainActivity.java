package com.mdreader.app;

import android.util.Log;

import com.getcapacitor.BridgeActivity;

/**
 * Registers FileOpenPlugin BEFORE super.onCreate() -> BridgeActivity.load(), so the plugin is
 * instantiated (and its handleOnCreate runs) with the launch intent already set. The plugin owns
 * all pending-URI bookkeeping; see FileOpenPlugin and .scratch ticket 03 for design notes.
 */
public class MainActivity extends BridgeActivity {

    private static final String TAG = "MainActivity";

    @Override
    public void onCreate(android.os.Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: registering FileOpen before bridge load");
        registerPlugin(FileOpenPlugin.class);
        super.onCreate(savedInstanceState);
    }
}
