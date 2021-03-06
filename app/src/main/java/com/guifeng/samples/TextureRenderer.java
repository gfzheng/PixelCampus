package com.guifeng.samples;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import com.guifeng.pixelcampus.R;
import com.guifeng.utils.ResUtils;
import com.guifeng.utils.ShaderUtils;
import com.guifeng.utils.TextureUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class TextureRenderer implements GLSurfaceView.Renderer {

    private static final String TAG = "TextureRenderer";

    private final FloatBuffer vertexBuffer, mTexVertexBuffer;

    private final ShortBuffer mVertexIndexBuffer;

    private int mProgram;

    private int textureId;

    /**
     * 顶点坐标
     * (x,y,z)
     */
    private float[] POSITION_VERTEX = new float[]{
            0f, 0f, 0f,     //顶点坐标V0
            1f, 1f, 0f,     //顶点坐标V1
            -1f, 1f, 0f,    //顶点坐标V2
            -1f, -1f, 0f,   //顶点坐标V3
            1f, -1f, 0f     //顶点坐标V4
    };

    /**
     * 纹理坐标
     * (s,t)
     */
    private static final float[] TEX_VERTEX = {
            0.5f, 0.5f, //纹理坐标V0
            1f, 0f,     //纹理坐标V1
            0f, 0f,     //纹理坐标V2
            0f, 1.0f,   //纹理坐标V3
            1f, 1.0f    //纹理坐标V4
    };

    /**
     * 索引
     */
    private static final short[] VERTEX_INDEX = {
            0, 1, 2,  //V0,V1,V2 三个顶点组成一个三角形
            0, 2, 3,  //V0,V2,V3 三个顶点组成一个三角形
            0, 3, 4,  //V0,V3,V4 三个顶点组成一个三角形
            0, 4, 1   //V0,V4,V1 三个顶点组成一个三角形
    };

    private int uMatrixLocation;

    private float[] mMatrix = new float[16];

    private static Context instance;

    public TextureRenderer(Context app) {
        instance = app;
        //分配内存空间,每个浮点型占4字节空间
        vertexBuffer = ByteBuffer.allocateDirect(POSITION_VERTEX.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        //传入指定的坐标数据
        vertexBuffer.put(POSITION_VERTEX);
        vertexBuffer.position(0);

        mTexVertexBuffer = ByteBuffer.allocateDirect(TEX_VERTEX.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(TEX_VERTEX);
        mTexVertexBuffer.position(0);

        mVertexIndexBuffer = ByteBuffer.allocateDirect(VERTEX_INDEX.length * 2)
                .order(ByteOrder.nativeOrder())
                .asShortBuffer()
                .put(VERTEX_INDEX);
        mVertexIndexBuffer.position(0);
    }


    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        //设置背景颜色
        GLES30.glClearColor(0.5f, 0.5f, 0.5f, 0.5f);

        //编译
        final int vertexShaderId = ShaderUtils.compileVertexShader(ResUtils.readResource(instance, R.raw.vertex_texture_shader));
        final int fragmentShaderId = ShaderUtils.compileFragmentShader(ResUtils.readResource(instance, R.raw.fragment_texture_shader));
        //链接程序片段
        mProgram = ShaderUtils.linkProgram(vertexShaderId, fragmentShaderId);

        uMatrixLocation = GLES30.glGetUniformLocation(mProgram, "u_Matrix");

        //加载纹理
        textureId = TextureUtils.loadTexture(instance, R.drawable.badges);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES30.glViewport(0, 0, width, height);

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
        float zoom = 1f;
        // 滚屏
        float scroll_x = 0.2f;
        float scroll_y = 0.4f;

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
            mMatrix[12] = scroll_x * mMatrix[0];
            mMatrix[13] = scroll_y * mMatrix[5];
            //等价于：
            //Matrix.orthoM(mMatrix, 0, -1f, 1f, -1*aspectRatio, 1*aspectRatio, -1f, 1f);

        }

        float invW2 = 2f / width;
        float invH2 = 2f / height;
        float camera_x = 1f;
        float camera_y = 1f;

        //Matrix.setIdentityM(mMatrix, 0);
        //mMatrix[0] = +zoom * invW2;
        //mMatrix[5] = -zoom * invH2;

        //mMatrix[12] += -1 + camera_x * invW2 - (scroll_x) * mMatrix[0];
        //mMatrix[13] += +1 - camera_y * invH2 - (scroll_y) * mMatrix[5];
        //Matrix.setIdentityM(mMatrix, 0);
        //Matrix.translateM(mMatrix, 0, 0, 0, -1);
        //Matrix.scaleM(mMatrix, 0,2f / width, 2f / height, 1);
        //Matrix.translateM( mMatrix, 0, 1f, 1f, 0 );
        //Matrix.scaleM( mMatrix, 0, zoom, zoom, 0 );
        //Matrix.translateM( mMatrix,0, scroll_x, scroll_y, -1f );
        //Matrix.rotateM(mMatrix,0, 45f,1,0f, 0f);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);

        //使用程序片段
        GLES30.glUseProgram(mProgram);

        GLES30.glUniformMatrix4fv(uMatrixLocation, 1, false, mMatrix, 0);

        GLES30.glEnableVertexAttribArray(0);
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 0, vertexBuffer);

        GLES30.glEnableVertexAttribArray(1);
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, 0, mTexVertexBuffer);

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        //绑定纹理
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);

        // 绘制
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, VERTEX_INDEX.length, GLES30.GL_UNSIGNED_SHORT, mVertexIndexBuffer);
    }
}
