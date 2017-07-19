/**
 */
package com.example;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.Vibrator;

import android.util.Log;

import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;


public class HeartRatePlugin extends CordovaPlugin {
    private static final String TAG = "HeartRatePlugin";
    CallbackContext context;
    private static final AtomicBoolean processing = new AtomicBoolean(false);
    
    private static SurfaceView preview = null;
    private static SurfaceHolder previewHolder = null;
    private static Camera camera = null;
    private static View image = null;

    private static String beatsPerMinuteValue="";
    private static WakeLock wakeLock = null;
    private static int averageIndex = 0;
    private static final int averageArraySize = 4;
    private static final int[] averageArray = new int[averageArraySize];
    private static Context parentReference = null;
    
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
    private static Vibrator v ;
    
    
    
    

  public void initialize(CordovaInterface cordova, CordovaWebView webView) {
    super.initialize(cordova, webView);

    Log.d(TAG, "Initializing MyCordovaPlugin");
  }

  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
      
    context = callbackContext;
    if(action.equals("pluginInitialize")) {
    
        pluginInitialize();
    }
    return true;
  }
                                               
        public void pluginInitialize(){
            
            // setContentView(R.layout.main);
            v = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
            parentReference=this;
            //strSavedDoctorID= HeartRateMonitor.this.getSharedPreferences("app_prefs", MODE_PRIVATE)
            //.getString("doc_id", "---");
            
            preview = new SurfaceView(this);
            
            LayoutParams lp = new FrameLayout.LayoutParams(120, 200);
            
            preview.setLayoutParams(lp);
            // setContentView(view);
            // preview = (SurfaceView) findViewById(R.id.preview);
            previewHolder = preview.getHolder();
            //mTxtVwStopWatch=(TextView)findViewById(R.id.txtvwStopWatch);
            previewHolder.addCallback(surfaceCallback);
            previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
            previewHolder.setSizeFromLayout();
            
            image = findViewById(R.id.image);
            //text = (TextView) findViewById(R.id.text);
            
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "DoNotDimScreen");
            //pubnub = new Pubnub(PUBNUB_PUBLISH_KEY, PUBNUB_SUBSCRIBE_KEY);
            prepareCountDownTimer();
            //configurePubNubClient();
            //pubnubSubscribe();
            
        }
                                               
                                               
                                               
                                               /**
                                                * {@inheritDoc}
                                                */
                                               @Override
                                               public void onConfigurationChanged(Configuration newConfig) {
            super.onConfigurationChanged(newConfig);
        }
                                               
                                               /**
                                                * {@inheritDoc}
                                                */
                                               @Override
                                               public void onResume() {
            super.onResume();
            wakeLock.acquire();
            camera = Camera.open();
            startTime = System.currentTimeMillis();
        }
                                               
                                               /**
                                                * {@inheritDoc}
                                                */
                                               @Override
                                               public void onPause() {
            super.onPause();
            
            wakeLock.release();
            
            camera.setPreviewCallback(null);
            camera.stopPreview();
            camera.release();
            text.setText("---");
            camera = null;
        }
                                               
                                               
                                               
                                               private static PreviewCallback previewCallback = new PreviewCallback() {
            
            
            @Override
            public void onPreviewFrame(byte[] data, Camera cam) {
                if (data == null) throw new NullPointerException();
                Camera.Size size = cam.getParameters().getPreviewSize();
                if (size == null) throw new NullPointerException();
                
                if (!processing.compareAndSet(false, true)) return;
                
                int width = size.width;
                int height = size.height;
                
                int imgAvg = ImageProcessing.decodeYUV420SPtoRedAvg(data.clone(), height, width);
                // Log.i(TAG, "imgAvg="+imgAvg);
                if (imgAvg == 0 || imgAvg == 255) {
                    processing.set(false);
                    return;
                }
                
                int averageArrayAvg = 0;
                int averageArrayCnt = 0;
                for (int i = 0; i < averageArray.length; i++) {
                    if (averageArray[i] > 0) {
                        averageArrayAvg += averageArray[i];
                        averageArrayCnt++;
                    }
                }
                
                int rollingAverage = (averageArrayCnt > 0) ? (averageArrayAvg / averageArrayCnt) : 0;
                TYPE newType = currentType;
                if (imgAvg < rollingAverage) {
                    newType = TYPE.RED;
                    if (newType != currentType) {
                        beats++;
                        // Log.d(TAG, "BEAT!! beats="+beats);
                    }
                } else if (imgAvg > rollingAverage) {
                    newType = TYPE.GREEN;
                }
                
                if (averageIndex == averageArraySize) averageIndex = 0;
                averageArray[averageIndex] = imgAvg;
                averageIndex++;
                
                // Transitioned from one state to another to the same
                if (newType != currentType) {
                    currentType = newType;
                    image.postInvalidate();
                }
                
                long endTime = System.currentTimeMillis();
                double totalTimeInSecs = (endTime - startTime) / 1000d;
                if (totalTimeInSecs >= 10) {
                    double bps = (beats / totalTimeInSecs);
                    int dpm = (int) (bps * 60d);
                    if (dpm < 30 || dpm > 180) {
                        startTime = System.currentTimeMillis();
                        beats = 0;
                        processing.set(false);
                        return;
                    }
                    
                    // Log.d(TAG,
                    // "totalTimeInSecs="+totalTimeInSecs+" beats="+beats);
                    
                    if (beatsIndex == beatsArraySize) beatsIndex = 0;
                    beatsArray[beatsIndex] = dpm;
                    beatsIndex++;
                    
                    int beatsArrayAvg = 0;
                    int beatsArrayCnt = 0;
                    for (int i = 0; i < beatsArray.length; i++) {
                        if (beatsArray[i] > 0) {
                            beatsArrayAvg += beatsArray[i];
                            beatsArrayCnt++;
                        }
                    }
                    int beatsAvg = (beatsArrayAvg / beatsArrayCnt);
                    text.setText(String.valueOf(beatsAvg));
                    beatsPerMinuteValue=String.valueOf(beatsAvg);
                    PluginResult result = new PluginResult(PluginResult.Status.OK, (beatsPerMinuteValue);
                    context.sendPluginResult(result);
                    makePhoneVibrate();
        
                    showReadingCompleteDialog();
                    startTime = System.currentTimeMillis();
                    beats = 0;
                }
                processing.set(false);
            }
        };
                                               
                                               
                                               private static void makePhoneVibrate(){
            v.vibrate(500);
        }
                                               
                                               private static SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {
            
            /**
             * {@inheritDoc}
             */
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    camera.setPreviewDisplay(previewHolder);
                    camera.setPreviewCallback(previewCallback);
                } catch (Throwable t) {
                    Log.e("PreviewDemo-surfaceCallback", "Exception in setPreviewDisplay()", t);
                }
            }
            
            /*
             * {@inheritDoc}
             */
            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                Camera.Parameters parameters = camera.getParameters();
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                Camera.Size size = getSmallestPreviewSize(width, height, parameters);
                if (size != null) {
                    parameters.setPreviewSize(size.width, size.height);
                    Log.d(TAG, "Using width=" + size.width + " height=" + size.height);
                }
                camera.setParameters(parameters);
                camera.startPreview();
            }
            
            /**
             * {@inheritDoc}
             */
            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                // Ignore
            }
        };
                                               
                                               
                                               private static Camera.Size getSmallestPreviewSize(int width, int height, Camera.Parameters parameters) {
            Camera.Size result = null;
            
            for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
                if (size.width <= width && size.height <= height) {
                    if (result == null) {
                        result = size;
                    } else {
                        int resultArea = result.width * result.height;
                        int newArea = size.width * size.height;
                        
                        if (newArea < resultArea) result = size;
                            }
                }
            }
            
            return result;
        }
                                               
                                               
                                               
            private static void showReadingCompleteDialog(){
            AlertDialog.Builder builder = new AlertDialog.Builder(parentReference);
            builder.setTitle("PubNub-HeartRate");
            builder.setMessage("Reading taken Succesfully at- "+beatsPerMinuteValue+" beats per minute.")
            .setCancelable(false)
            .setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    ( (Activity) parentReference).finish();
                }
            })
            .setNegativeButton("Take Another", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    text.setText("---");
                    prepareCountDownTimer();
                    dialog.cancel();
                }
            });
            AlertDialog alert = builder.create();
            alert.show();
        }
                                               
                                               
                                               
    private static int decodeYUV420SPtoRedSum(byte[] yuv420sp, int width, int height) {
            if (yuv420sp == null) return 0;
            
            final int frameSize = width * height;
            
            int sum = 0;
            for (int j = 0, yp = 0; j < height; j++) {
                int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
                for (int i = 0; i < width; i++, yp++) {
                    int y = (0xff & yuv420sp[yp]) - 16;
                    if (y < 0) y = 0;
                        if ((i & 1) == 0) {
                            v = (0xff & yuv420sp[uvp++]) - 128;
                            u = (0xff & yuv420sp[uvp++]) - 128;
                        }
                    int y1192 = 1192 * y;
                    int r = (y1192 + 1634 * v);
                    int g = (y1192 - 833 * v - 400 * u);
                    int b = (y1192 + 2066 * u);
                    
                    if (r < 0) r = 0;
                        else if (r > 262143) r = 262143;
                            if (g < 0) g = 0;
                                else if (g > 262143) g = 262143;
                                    if (b < 0) b = 0;
                                        else if (b > 262143) b = 262143;
                                            
                                            int pixel = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
                                            int red = (pixel >> 16) & 0xff;
                                            sum += red;
                                            }
            }
            return sum;
        }
                                               
        /**
         * Given a byte array representing a yuv420sp image, determine the average
         * amount of red in the image. Note: returns 0 if the byte array is NULL.
         *
         * @param yuv420sp
         *            Byte array representing a yuv420sp image
         * @param width
         *            Width of the image.
         * @param height
         *            Height of the image.
         * @return int representing the average amount of red in the image.
         */
                                               public static int decodeYUV420SPtoRedAvg(byte[] yuv420sp, int width, int height) {
            if (yuv420sp == null) return 0;
            
            final int frameSize = width * height;
            
            int sum = decodeYUV420SPtoRedSum(yuv420sp, width, height);
            return (sum / frameSize);
        }

}
