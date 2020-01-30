package com.guifeng.pixelcampus;

import android.opengl.GLES30;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.guifeng.engine.Game;
import com.guifeng.engine.Tilemap;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class PixelCampus extends Game {

    private Tilemap tilemap;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        super.onSurfaceCreated(gl, config);
        tilemap = new Tilemap(R.drawable.tiles0);
    }

    @Override
    protected void update() {
        super.update();
    }

    @Override
    protected void draw() {
        super.draw();
        //draw scene

        tilemap.draw();

    }
}
