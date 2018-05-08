package com.wasu.tvoscamera.suface;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.WindowManager;

import java.util.List;

public class Camera2 {

    static final String Tag ="debug";

    Camera mDevice =null;
    SurfaceView mSurface =null;
    int mIndex =-1;
    int mResolution =0, mResolution2 =(1280<<16)|(720);
    int mRotation =0;
    int mFormat =0;
    int mFrameLen =0;
    boolean mRunning =false;
    boolean mFlip =false; // 是否水平翻转

    private Camera2() {
    }
    public static final Camera2 instance = new Camera2();

    public interface Callback {
        void onCameraOutput(byte[] data, int resolution);
        void onCameraClosed();
    }
    volatile Callback mCB=null;

    public void setCallback(Callback cb) {
        synchronized (this){
            mCB =cb;
        }
    }

    public void setFlip(boolean b) {
        mFlip =b;
    }

    public int getResolution() {
        return mResolution2;
    }

    boolean run(int resolution) {
        if (null == mDevice)
            return false;

        Camera.Size size =getSize(resolution);
        if (null==size)
            return false;
        int newRes =(size.width<<16)|(size.height);

        if (mRunning) {
            if (newRes==mResolution)
                return true;
            mRunning = false;
            mDevice.stopPreview();
            mDevice.setPreviewCallback(null);
        }

        try	{
            mDevice.setPreviewDisplay(mSurface.getHolder());

            Camera.Parameters p = mDevice.getParameters();
            p.setPreviewFormat(ImageFormat.NV21);
            p.setPreviewSize(size.width, size.height);
            mDevice.setParameters(p);
            mResolution=newRes;
            mResolution2=mResolution;
            mRotation=0;

            mFormat = p.getPreviewFormat();
            mFrameLen = size.width * size.height * ImageFormat.getBitsPerPixel(mFormat)/8;
            if(mFrameLen < size.width*(size.height+size.height/2))
                return false;

            updateOrientation(mSurface.getContext());
            mDevice.addCallbackBuffer(new byte[mFrameLen]);
            mDevice.addCallbackBuffer(new byte[mFrameLen]);
            mDevice.startPreview();
            mRunning = true;
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        setFps();
        Log.w(Tag, "Camera2.run()");
        return true;
    }
    void stop() {
        if (mRunning){
            mRunning=false;
            mDevice.stopPreview();
            mDevice.setPreviewCallback(null);
        }
    }

    boolean open(boolean isFront, SurfaceView sf) {
        int index =indexOf(isFront);
        if (mDevice!=null) {
            if (mIndex == index && sf == mSurface)
                return true; // already opened
            this.close();
        }
        if (index < 0 || null==sf)
            return false;

        try {
            mDevice = Camera.open(index);
            mIndex =index;
            mSurface =sf;
            mRunning =false;
            return true;
        }
        catch(Exception e) {
            mDevice=null;
            e.printStackTrace();
            return false;
        }
    }
    void close() {
        mRunning =false;
        if(mDevice!=null){
            mDevice.stopPreview();
            mDevice.setPreviewCallback(null);
            mDevice.release();
            mDevice=null;
            mResolution=0;
        }
        mSurface=null;
        synchronized (this){
            if (mCB!=null)
                mCB.onCameraClosed();
        }
        Log.w(Tag, "Camera2.close()");
    }
    void on_surface_destroy(SurfaceView sf) {
        if (sf==mSurface){
            this.close();
        }
    }

    int indexOf(boolean isFront) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        int count = Camera.getNumberOfCameras();
        for (int i=0; i < count; i++)
        {
            Camera.getCameraInfo(i, info);
            switch (info.facing)
            {
                case Camera.CameraInfo.CAMERA_FACING_FRONT:
                    if (isFront)
                        return i;
                    continue;

                case Camera.CameraInfo.CAMERA_FACING_BACK:
                    if (!isFront)
                        return i;
                    continue;
            }
        }
        return -1;
    }

    Camera.Size getSize(int resolution) {
        int w =resolution >>16;
        int h =resolution &0xffff;
        if (w >1920)
            w =1920;
        else if (w < 192)
            w =192;
        if (h >1080)
            h =1080;
        else if (h < 144)
            h =144;

        Camera.Parameters p = mDevice.getParameters();
        List<Camera.Size> list = p.getSupportedPreviewSizes();
        Camera.Size size=null;
        int error=0;
        int wh = w*h;
        for (Camera.Size s:list)
        {
            int e = s.width*s.height - wh;
            if (e <0) e=-e;
            if (size==null || e <error) {
                size=s;
                error=e;
                if (e==0)
                    break;
            }
        }
        return size;
    }

    void setFps()
    {
        Camera.Parameters p = mDevice.getParameters();
        List<int[]> list=p.getSupportedPreviewFpsRange();
        int[] selected =null;
        for (int[] r:list) {
            if (selected==null || selected[0]<r[0])
                selected =r;
        }
        try	{
            int fps =selected[1];
            if (fps>32000)
                fps=32000;
            p.setPreviewFpsRange(fps,fps);
            mDevice.setParameters(p);
            return;
        }
        catch (RuntimeException e) {
            e.printStackTrace();
        }
        p.setPreviewFpsRange(selected[0], selected[1]);
        mDevice.setParameters(p);
    }

    public void updateOrientation(Context context)
    {
        WindowManager wm = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        int degrees = 0;
        switch (display.getRotation()) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(mIndex, info);

        int rotation =0;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            degrees = (info.orientation + degrees) % 360;
            rotation =degrees;
            degrees = (360 - degrees) % 360;   // compensate the mirror
        }
        else {
            degrees = ( info.orientation - degrees + 360) % 360;
            rotation = degrees;
        }
        mDevice.setDisplayOrientation(degrees);

        if ( (degrees%180)==0 ) {
            mResolution2 = mResolution;
            rotation =degrees;
        } else {
            mResolution2 =(mResolution>>16) |((mResolution&0xffff)<<16);
            rotation =(rotation+180)%360;
        }
        mRotation=rotation;
        Log.v(Tag, "Camera Surface rotation:"+degrees + " camera.orientation:"+info.orientation + " rotation="+rotation);
    }
}
