package com.example;

//import java.text.DateFormat;
//import java.text.SimpleDateFormat;
//import java.util.Calendar;
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

//import android.app.Activity;
//import android.app.AlertDialog;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
//import android.hardware.Camera.PreviewCallback;
import android.os.Bundle;
//import android.os.CountDownTimer;
//import android.os.PowerManager;
//import android.os.PowerManager.WakeLock;
//import android.os.Vibrator;

import android.util.Log;

//import javax.security.auth.callback.Callback;

//import android.view.SurfaceHolder;
//import android.view.SurfaceView;
//import android.widget.TextView;
//import android.widget.Toast;
//import android.view.View;


public class HeartRatePlugin extends CordovaPlugin {
   
    
    private static final String TAG = "HeartRatePlugin";
    private static CallbackContext context;
    private static final AtomicBoolean processing = new AtomicBoolean(false);
    
   /* private static SurfaceView preview = null;
    private static SurfaceHolder previewHolder = null;
    private static Camera camera = null;
    private static View image = null;*/
    
    Camera camera;

    private static String beatsPerMinuteValue="";
   // private static WakeLock wakeLock = null;
    private static int averageIndex = 0;
    private static final int averageArraySize = 4;
    private static final int[] averageArray = new int[averageArraySize];
   // private static Context parentReference = null;
        public static final int TAKE_PIC_SEC = 0;
    
    
    public static enum TYPE {
        GREEN, RED
    };
    
    private static TYPE currentType = TYPE.GREEN;
    
    public static TYPE getCurrent() {
        return currentType;
    }
    
    private static int beatsIndex = 0;
    private static final int beatsArraySize = 3;
    private static final int[] beatsArray = new int[beatsArraySize];
    private static double beats = 0;
    private static long startTime = 0;
  //  private static Vibrator v ;
    
    protected final static String[] permissions = { Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE };
    
    
  public void initialize(CordovaInterface cordova, CordovaWebView webView) {
    super.initialize(cordova, webView);

    Log.d(TAG, "Initializing MyCordovaPlugin");
      
  }

  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
      
    context = callbackContext;
    if(action.equals("pluginInitialize")) {
        
        boolean takePicturePermission = PermissionHelper.hasPermission(this, Manifest.permission.CAMERA);
        if(!takePicturePermission){
            PermissionHelper.requestPermission(this, TAKE_PIC_SEC, Manifest.permission.CAMERA);
        }
        else{

            Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
            startActivityForResult(intent, 0);

        
            PluginResult result = new PluginResult(PluginResult.Status.OK, (beatsPerMinuteValue));
            context.sendPluginResult(result);
        }
    }
    return true;
  }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && requestCode == 0) {
            //String result = data.toURI();
            // ...
            Log.d(TAG, "Got Result from Camera with data string: " + data.toURI());
            PluginResult result = new PluginResult(PluginResult.Status.OK, (beatsPerMinuteValue));
            context.sendPluginResult(result);
        }
    }


}
