package org.krupkas.SeeBig;

import java.io.IOException;
import java.util.List;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;


class Preview extends ViewGroup implements SurfaceHolder.Callback {
    private final String TAG = "Preview";

    SurfaceView mSurfaceView;
    SurfaceHolder mHolder;
    Size mPreviewSize;
    List<Size> mSupportedPreviewSizes;
    Camera mCamera;
    private ScaleGestureDetector mScaleDetector;
    private int     zoomValue = 0;
    private int     maxZoom = 0;
    private int     zoomStep = 1;
    private boolean wasScaleEvent = false;

    Preview(Context context) {
        super(context);
        
        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());

        mSurfaceView = new SurfaceView(context);
        addView(mSurfaceView);

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }
    
    void startAutofocus(){
        List<String>    focusModes;
        
        mCamera.cancelAutoFocus();
        
        Camera.Parameters params = mCamera.getParameters();
        
        focusModes = params.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)){
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            mCamera.setParameters(params);
        }
        /* not until API level 14
        else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)){
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }
        */
        else {         
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            mCamera.setParameters(params);
            // not needed for continuous autofocus
            mCamera.autoFocus(myAutoFocusCallback);
        }    
    }
    
    AutoFocusCallback myAutoFocusCallback = new AutoFocusCallback(){
        @Override
        public void onAutoFocus(boolean arg0, Camera arg1) {
            // buttonTakePicture.setEnabled(true);
        }
    };

    public void setCamera(Camera camera) {
        Camera.Parameters   params;
        
        mCamera = camera;
        if (mCamera != null) {
            params = camera.getParameters();
            mSupportedPreviewSizes = params.getSupportedPreviewSizes();
            requestLayout();
            if (params.isZoomSupported()){
                maxZoom = params.getMaxZoom();
                // give user 10 steps of zooming
                zoomStep = maxZoom/10;
                if (zoomStep == 0){
                    // in case there are fewer than 10 zoom steps in the camera
                    zoomStep = 1;
                }
            }
        }
    }


    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (changed && getChildCount() > 0) {
            final View child = getChildAt(0);

            final int width = r - l;
            final int height = b - t;

            int previewWidth = width;
            int previewHeight = height;
            if (mPreviewSize != null) {
                previewWidth = mPreviewSize.width;
                previewHeight = mPreviewSize.height;
            }

            // Center the child SurfaceView within the parent.
            if (width * previewHeight > height * previewWidth) {
                final int scaledChildWidth = previewWidth * height / previewHeight;
                child.layout((width - scaledChildWidth) / 2, 0,
                        (width + scaledChildWidth) / 2, height);
            } else {
                final int scaledChildHeight = previewHeight * width / previewWidth;
                child.layout(0, (height - scaledChildHeight) / 2,
                        width, (height + scaledChildHeight) / 2);
            }
        }
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN){
            wasScaleEvent = false;
        }
        
        if (ev.getAction() == MotionEvent.ACTION_UP){
            if (!wasScaleEvent){
                startAutofocus();
            }
        }
        
        // Let the ScaleGestureDetector inspect all events.
        mScaleDetector.onTouchEvent(ev);
        
        return true;
    }
    

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            Camera.Parameters  params;
               
            if (maxZoom == 0){
                // camera cannot zoom
                invalidate();
                return true;
            }
            
            if (detector.getScaleFactor() > 1.0){
                // zoom in
                if (zoomValue < maxZoom){
                    zoomValue += zoomStep;
                    if (zoomValue > maxZoom){
                        zoomValue = maxZoom;
                    }
                    wasScaleEvent = true;
                }
            }
            else {
                // zoom out
                if (zoomValue > 0){
                    zoomValue -= zoomStep;
                    if (zoomValue < 0){
                        zoomValue = 0;
                    }
                    wasScaleEvent = true;
                }
            }
            params = mCamera.getParameters();
            params.setZoom(zoomValue);       
            mCamera.setParameters(params);
            
            //Log.e("SeeBig","maxZoom="+maxZoom + ", zoomValue="+zoomValue);

            invalidate();
            return true;
        }
    }

    
    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, acquire the camera and tell it where
        // to draw.
        try {
            if (mCamera != null) {
                mCamera.setPreviewDisplay(holder);
            }
        } catch (IOException exception) {
            Log.e(TAG, "IOException caused by setPreviewDisplay()", exception);
        }
    }
    

    public void surfaceDestroyed(SurfaceHolder holder) {
        // Surface will be destroyed when we return, so stop the preview.
        if (mCamera != null) {
            mCamera.stopPreview();
        }
    }
    

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        mCamera.startPreview();
        startAutofocus();
    }
    
}
