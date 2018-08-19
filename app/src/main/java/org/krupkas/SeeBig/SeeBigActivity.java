package org.krupkas.SeeBig;

import org.krupkas.SeeBig.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;

import java.util.List;

public class SeeBigActivity extends Activity implements View.OnClickListener {
  Button buttonClick;
  boolean lightOn = false;
  Parameters camParams;
  private final int FLASH_NOT_SUPPORTED = 0;
  private final int FLASH_TORCH_NOT_SUPPORTED = 1;
  private Preview mPreview;
  private int defaultCameraId;
  int cameraCurrentlyLocked;
  Camera mCamera;
  private int numberOfCameras;


  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    // Hide the window title.
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    
    // Find the total number of cameras available
    numberOfCameras = Camera.getNumberOfCameras();

    // Find the ID of the default camera
    CameraInfo cameraInfo = new CameraInfo();
    for (int i = 0; i < numberOfCameras; i++) {
        Camera.getCameraInfo(i, cameraInfo);
        if (cameraInfo.facing == CameraInfo.CAMERA_FACING_BACK) {
            defaultCameraId = i;
        }
    }


    setContentView(R.layout.main);

    mPreview = new Preview(this);
    ((FrameLayout) findViewById(R.id.preview)).addView(mPreview);

    buttonClick = (Button)findViewById(R.id.toggleButton1);
    buttonClick.setOnClickListener(this);
    buttonClick.setText(getString(R.string.lightOn));
  }

  
  @Override
  protected void onResume() {
      super.onResume();

      // Open the default i.e. the first rear facing camera.
      mCamera = Camera.open(defaultCameraId);
      mCamera.setDisplayOrientation(90);
         
      cameraCurrentlyLocked = defaultCameraId;
      mPreview.setCamera(mCamera);
  }

  @Override
  protected void onPause() {
      super.onPause();

      // Because the Camera object is a shared resource, it's very
      // important to release it when the activity is paused.
      if (mCamera != null) {
          mPreview.setCamera(null);
          mCamera.release();
          mCamera = null;
      }
  }

 
  @Override
  public void onClick(View v) {
        camParams = mCamera.getParameters();
        List<String> flashModes = camParams.getSupportedFlashModes();
        if (lightOn){
            camParams.setFlashMode(Parameters.FLASH_MODE_OFF);
            lightOn = false;
            buttonClick.setText(getString(R.string.lightOn));
        }
        else {
            if (flashModes.contains(Parameters.FLASH_MODE_TORCH)){
                camParams.setFlashMode(Parameters.FLASH_MODE_TORCH);
                lightOn = true;
                buttonClick.setText(getString(R.string.lightOff));
            }
            else {
                showDialog(SeeBigActivity.this, FLASH_TORCH_NOT_SUPPORTED);
            }
        }
        mCamera.setParameters(camParams);
  }
  
  
  public void showDialog (Context context, int dialogId){
      AlertDialog.Builder builder;
      AlertDialog alertDialog;
      switch(dialogId){
          case FLASH_NOT_SUPPORTED:
              builder = new AlertDialog.Builder(context);
              builder.setMessage("Sorry, Your phone does not support Flashlight")
              .setCancelable(false)
              .setNeutralButton("Close", new OnClickListener() {

                  @Override
                  public void onClick(DialogInterface dialog, int which) {
                      finish();
                  }
              });
              alertDialog = builder.create();
              alertDialog.show();
              break;
              
          case FLASH_TORCH_NOT_SUPPORTED:
              builder = new AlertDialog.Builder(context);
              builder.setMessage("Sorry, Your camera flash does not support flashlight feature")
              .setCancelable(false)
              .setNeutralButton("Close", new OnClickListener() {

                  @Override
                  public void onClick(DialogInterface dialog, int which) {
                      finish();
                  }
              });
              alertDialog = builder.create();
              alertDialog.show();
      }
  }

}
