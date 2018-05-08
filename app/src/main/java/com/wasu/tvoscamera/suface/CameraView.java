package com.wasu.tvoscamera.suface;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class CameraView extends SurfaceView implements SurfaceHolder.Callback {

    public static final int BACK_CAMERA =0, FRONT_CAMERA =1;

    // 切换摄像头
    int mCameraIndex =FRONT_CAMERA;
    public void setCameraIndex(int index) {
        if(index!=mCameraIndex)
            MainHandler.getInstance().post(new JobRestart(index, mResolution));
    }

    public int getCameraIndex() {
        return mCameraIndex;
    }

    // 设置分辨率
    int mResolution =(1280<<16)|(720);
    public void setResolution(int res) {
        if(res!=mResolution)
            MainHandler.getInstance().post(new JobRestart(mCameraIndex, res));
    }

    // 屏幕方向改变时需要更新旋转角度
    public void updateOrientation() {
        if (mSurfaceOk)
            Camera2.instance.updateOrientation(this.getContext());
    }

    /////////////////////////////////////////////////////////////////////////////

    class JobRestart implements Runnable {
        final int mIndex , mResolution;
        JobRestart(int index, int resolution) {
            mIndex =index;
            mResolution =resolution;
        }
        @Override
        public void run() {
            restart(mIndex, mResolution);
        }
    }
    void restart(int index, int res) {
        mCameraIndex =index;
        mResolution =res;
        if (mSurfaceOk) {
            Camera2 c =Camera2.instance;
            if (c.open(mCameraIndex == FRONT_CAMERA, this))
                c.run(mResolution);
        }
    }

    public CameraView(Context context) {
        super(context);
        this.Init();
    }

    public CameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.Init();
    }

    public CameraView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.Init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public CameraView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.Init();
    }

    boolean mSurfaceOk=false;

    void Init() {
        setZOrderMediaOverlay(true);
        getHolder().addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder)
    {
        mSurfaceOk =true;
        this.restart(mCameraIndex, mResolution);
    }
    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2)
    {
    }
    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder)
    {
        mSurfaceOk=false;
        Camera2.instance.on_surface_destroy(this);
    }
}
