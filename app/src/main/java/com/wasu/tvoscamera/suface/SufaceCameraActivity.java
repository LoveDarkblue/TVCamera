package com.wasu.tvoscamera.suface;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.wasu.tvoscamera.R;
import com.wasu.tvoscamera.TextureCameraActivity;

public class SufaceCameraActivity extends AppCompatActivity {
    public static void toActivity(Context context) {
        Intent intent = new Intent(context, SufaceCameraActivity.class);
        context.startActivity(intent);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_suface_camera);
    }
}
