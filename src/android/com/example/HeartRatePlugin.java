package com.example;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.apache.cordova.PluginResult.Status;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import android.content.pm.PackageManager;
import org.apache.cordova.PermissionHelper;
import android.Manifest;
import android.net.Uri;
import android.content.Context;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.os.Bundle;

import android.util.Log;

public class HeartRatePlugin extends CordovaPlugin {

  private static final String TAG = "HeartRatePlugin";
  private static final String READ = "read";
  private static final int SEARCH_REQ_CODE = 102;
  private static final int GET_HEARTBEAT_CODE = 22;
  private CallbackContext callbackContext;
  protected final static String[] permissions = { Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE };

  public void initialize(CordovaInterface cordova, CordovaWebView webView) {
    super.initialize(cordova, webView);
    Log.d(TAG, "Initializing MyCordovaPlugin");
  }

  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    if(action.equals("pluginInitialize")) {
      this.callbackContext = callbackContext;
      if(cordova.hasPermission(READ))
      {
        this.openNewActivity();
      }
      else
      {
          getReadPermission(SEARCH_REQ_CODE);
      }
    }
    return true;
  }

  protected void getReadPermission(int requestCode)
{
    cordova.requestPermission(this, requestCode, READ);
}

public void onRequestPermissionResult(int requestCode, String[] permissions,
                                         int[] grantResults) throws JSONException
{
          for(int r:grantResults)
          {
              if(r == PackageManager.PERMISSION_DENIED)
              {
                  // this.callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, PERMISSION_DENIED_ERROR));
                  // return;
              }
          }
          switch(requestCode)
          {
          case SEARCH_REQ_CODE:
            // Context context=this.cordova.getActivity().getApplicationContext();
            this.openNewActivity();
            break;
          }
  }



  private void openNewActivity() {
      PluginResult r = new PluginResult(PluginResult.Status.NO_RESULT);
      r.setKeepCallback(true);
      callbackContext.sendPluginResult(r);

      Intent i = new Intent(this.cordova.getActivity(), HeartRateMonitor.class);
      i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      cordova.startActivityForResult(this,i,GET_HEARTBEAT_CODE);
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
      if(requestCode == GET_HEARTBEAT_CODE) {
          if(data != null) {
            String heartbeat = data.getStringExtra("heartbeat");
            Log.d(TAG, "Heartbeat: " + heartbeat);
            Log.d(TAG, "Heartbeat: " + heartbeat);
            Log.d(TAG, "Heartbeat: " + heartbeat);
            Log.d(TAG, "Heartbeat: " + heartbeat);
            Log.d(TAG, "Heartbeat: " + heartbeat);
            Log.d(TAG, "Heartbeat: " + heartbeat);
            this.callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, heartbeat));
          }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void pluginInitialize(){

        Log.d(TAG, "We are entering execute");
      }
}
