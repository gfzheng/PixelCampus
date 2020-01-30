package com.guifeng.engine;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import com.guifeng.pixelcampus.R;
import com.guifeng.utils.ResUtils;
import com.guifeng.utils.ShaderUtils;
import com.guifeng.utils.TextureUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class Game extends Activity implements GLSurfaceView.Renderer, View.OnTouchListener {
    public static Game instance;

    // 瓦片结构：一个瓦片由2个三角形组成，4个顶点，6个索引
    // 0---1
    // | \ |
    // 3---2
    public static final short[] CELL_IDX = {0, 1, 2, 0, 2, 3};
    public static final int CELL_SIZE = CELL_IDX.length;
    private static ShortBuffer indices;
    private static int indexSize = 0;

    // Actual size of the screen
    public static int width;
    public static int height;

    protected GLSurfaceView view;

    // Accumulated touch events
    protected ArrayList<MotionEvent> motionEvents = new ArrayList<MotionEvent>();

    // Density: mdpi=1, hdpi=1.5, xhdpi=2...
    public static float density = 1;

    private static String version;

    // Current time in milliseconds
    protected long now;
    // Milliseconds passed since previous update
    protected long step;

    public static float timeScale = 1f;
    public static float elapsed = 0f;

    // 缩放比例，1为满屏
    float zoom;
    // 滚屏坐标
    float scroll_x;
    float scroll_y;

    private int mProgram;           //着色器
    private int uMatrixLocation;    //变换矩阵在着色器中的位置
    private float[] mMatrix = new float[16];    //变换矩阵

    public Game() {
    }

    @Override
    protected void onResume() {
        super.onResume();
        now = 0;
        view.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        view.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        instance = null;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;

        DisplayMetrics m = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(m);
        density = m.density;

        try {
            version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            version = "???";
        }

        view = new GLSurfaceView(this);
        view.setEGLContextClientVersion(3);
        view.setEGLConfigChooser(false);
        view.setRenderer(this);
        view.setOnTouchListener(this);
        setContentView(view);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES30.glEnable(GL10.GL_BLEND);
        // For premultiplied alpha:
        // GLES20.glBlendFunc( GL10.GL_ONE, GL10.GL_ONE_MINUS_SRC_ALPHA );
        GLES30.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);

        GLES30.glEnable(GL10.GL_SCISSOR_TEST);

        //TextureCache.reload();

        //设置背景颜色
        GLES30.glClearColor(0.5f, 0.5f, 0.5f, 0.5f);

        //编译着色器
        final int vertexShaderId = ShaderUtils.compileVertexShader(ResUtils.readResource(instance, R.raw.vertex_texture_shader));
        final int fragmentShaderId = ShaderUtils.compileFragmentShader(ResUtils.readResource(instance, R.raw.fragment_texture_shader));
        //链接程序片段
        mProgram = ShaderUtils.linkProgram(vertexShaderId, fragmentShaderId);
        //转换矩阵
        uMatrixLocation = GLES30.glGetUniformLocation(mProgram, "u_Matrix");

        //加载纹理
        //textureId = TextureUtils.loadTexture(instance, R.drawable.badges);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);

        Game.width = width;
        Game.height = height;

        //正交矩阵:right, left, bottom, top, near, far
        //  2/(r-l), 0, 0, -(r+l)/(r-l)
        //  0, 2/(t-b), 0, -(t+b)/(t-b)
        //  0, 0, -2/(f-n), -(f+n)/(f-n)
        //  0, 0, 0, 1

        //横屏与竖屏根据长宽比配置
        //先单位阵
        Matrix.setIdentityM(mMatrix, 0);
        // near, far 都是 1, -1
        mMatrix[10] = -1f;

//        final float aspectRatio = width > height ?
//                (float) width / (float) height :
//                (float) height / (float) width;

        // 缩放比例，1为满屏
        zoom = 4f / 32 / 16;
        // 滚屏
        scroll_x = 0f;
        scroll_y = 0f;

        if (width > height) {
            //横屏
            //等价于：
            //Matrix.orthoM(mMatrix, 0, -1 * aspectRatio, 1 * aspectRatio, -1f, 1f, -1f, 1f);
            mMatrix[0] = zoom * (float) height / (float) width;
            mMatrix[5] = zoom;
            mMatrix[12] = scroll_x * mMatrix[0];
            mMatrix[13] = scroll_y * mMatrix[5];
        } else {
            //竖屏
            mMatrix[0] = zoom;
            mMatrix[5] = zoom * (float) width / (float) height;
            mMatrix[12] = (-32 * 8 + scroll_x) * mMatrix[0];
            mMatrix[13] = (-32 * 8 + scroll_y) * mMatrix[5];
            //等价于：
            //Matrix.orthoM(mMatrix, 0, -1f, 1f, -1*aspectRatio, 1*aspectRatio, -1f, 1f);

        }

    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (width == 0 || height == 0) {
            return;
        }

        long rightNow = System.currentTimeMillis();
        step = (now == 0 ? 0 : rightNow - now);
        now = rightNow;

        update();

        //NoosaScript.get().resetCamera();
        GLES30.glScissor(0, 0, width, height);
        GLES30.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        draw();
    }

    protected void draw() {
        // 使用程序片段
        GLES30.glUseProgram(mProgram);
        // 设置着色器矩阵
        GLES30.glUniformMatrix4fv(uMatrixLocation, 1, false, mMatrix, 0);

        //draw scene
    }

    // 画地图瓦片集合
    public void drawQuadSet(FloatBuffer vertices, int size, int tx) {

        if (size == 0) {
            return;
        }

        vertices.position(0);
        GLES30.glEnableVertexAttribArray(0);
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 4 * Float.SIZE / 8, vertices);

        vertices.position(2);
        GLES30.glEnableVertexAttribArray(1);
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, 4 * Float.SIZE / 8, vertices);

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        //绑定纹理
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, tx);

        GLES20.glDrawElements(
                GLES20.GL_TRIANGLES,
                CELL_SIZE * size,
                GLES20.GL_UNSIGNED_SHORT,
                getCellIndices(size));

    }

    // 瓦片的三角面顶点索引生成
    public static ShortBuffer getCellIndices(int size) {

        if (size > indexSize) {

            // TODO: Optimize it!

            indexSize = size;
            indices = ByteBuffer.
                    allocateDirect(size * CELL_SIZE * Short.SIZE / 8).
                    order(ByteOrder.nativeOrder()).
                    asShortBuffer();

            short[] values = new short[size * 6];
            int pos = 0;
            int limit = size * 4;
            for (int ofs = 0; ofs < limit; ofs += 4) {
                values[pos++] = (short) (ofs + 0);
                values[pos++] = (short) (ofs + 1);
                values[pos++] = (short) (ofs + 2);
                values[pos++] = (short) (ofs + 0);
                values[pos++] = (short) (ofs + 2);
                values[pos++] = (short) (ofs + 3);
            }

            indices.put(values);
            indices.position(0);
        }

        return indices;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        synchronized (motionEvents) {
            motionEvents.add(MotionEvent.obtain(event));
        }
        return true;
    }

    protected void update() {

        synchronized (motionEvents) {
            //Touchscreen.processTouchEvents( motionEvents );
            motionEvents.clear();
        }

    }

    public static void vibrate(int milliseconds) {
        ((Vibrator) instance.getSystemService(VIBRATOR_SERVICE)).vibrate(milliseconds);
    }
}
