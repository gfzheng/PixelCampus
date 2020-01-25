package com.guifeng.pixelcampus;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.opengl.GLSurfaceView;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    // GL 需要2个必要组件：GLSurfaceView视图 和 GLSurfaceView.Renderer渲染器
    // GL view needed to draw things on.
    private GLSurfaceView mGLSurfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mGLSurfaceView = new GLSurfaceView(this);
        setContentView(mGLSurfaceView);
        //设置版本
        mGLSurfaceView.setEGLContextClientVersion(3);

        //创建渲染器
        GLSurfaceView.Renderer renderer = new ColorRenderer(Color.GRAY);
        //GLSurfaceView.Renderer renderer = new NativeColorRenderer(Color.GRAY);
        mGLSurfaceView.setRenderer(renderer);
    }
}
