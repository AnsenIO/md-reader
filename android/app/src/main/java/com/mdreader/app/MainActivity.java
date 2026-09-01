package com.mdreader.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.getcapacitor.BridgeActivity;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginManager;

public class MainActivity extends BridgeActivity {
    private static final String TAG = "MainActivity";
    private static final String PREFS_NAME = "md_reader_prefs";
    private static final String KEY_FILE_URI = "pending_file_uri";
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Register custom plugin
        PluginManager pm = this.getBridge().getPluginManager();
        MdReaderPlugin mdReaderPlugin = new MdReaderPlugin();
        mdReaderPlugin.setBridge(this.getBridge());
        pm.registerPlugin(mdReaderPlugin);
        
        // Handle incoming file intent
        handleIntent(getIntent());
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }
    
    private void handleIntent(Intent intent) {
        if (intent == null) return;
        
        String action = intent.getAction();
        
        Log.d(TAG, "Intent action: " + action);
        
        Uri fileUri = null;
        
        if (Intent.ACTION_VIEW.equals(action)) {
            fileUri = intent.getData();
        } else if (Intent.ACTION_SEND.equals(action)) {
            fileUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        }
        
        if (fileUri != null) {
            String scheme = fileUri.getScheme();
            String path = fileUri.getPath();
            
            Log.d(TAG, "File URI: scheme=" + scheme + ", path=" + path);
            
            // Check if it's a markdown/text file
            if (path != null && path.matches(".*\\.(md|markdown|txt)$")) {
                // Store the file URI for the JS to read
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                prefs.edit().putString(KEY_FILE_URI, fileUri.toString()).apply();
                
                Log.d(TAG, "Stored pending file URI: " + fileUri.toString());
            }
        }
    }
}
