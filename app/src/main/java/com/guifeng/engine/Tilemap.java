package com.guifeng.engine;


import android.graphics.RectF;

import com.guifeng.pixelcampus.R;
import com.guifeng.utils.TextureUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Random;

// 瓦片地图类
public class Tilemap {

    public int texId;          //纹理图片Id

    protected int[] data;       //瓦片类型
    protected int mapWidth;     //地图宽，瓦片个数
    protected int mapHeight;
    protected int size;         //mapWidth * mapHeight

    private float cellW;        //瓦片宽
    private float cellH;        //瓦片高

    protected float[] vertices;     //一个瓦片坐标与纹理坐标，16个坐标
    protected FloatBuffer quads;    //所有地图坐标，传递给OpenGL

    public Tilemap(int tex) {
        vertices = new float[16];
        cellW = cellH = 16;

        mapWidth = 32;
        mapHeight = 32;
        size = mapWidth * mapHeight;

        texId = TextureUtils.loadTexture(Game.instance, tex);

        buildMap();
    }

    public void buildMap() {
        // 纹理图片tile0.png,尺寸： 256 x 64,
        // 瓦片 16x16， 共64瓦片
        // 根据data不同，选择不同瓦片纹理坐标
        int texWidth, texHeight;  //纹理贴图宽高
        float uw, vh;               //瓦片纹理标准化宽高[0～1]
        texWidth = 256;
        texHeight = 64;
        uw = cellW / texWidth;
        vh = cellH / texHeight;
        int texCols = texWidth / (int) cellW;
        int texRows = texHeight / (int) cellH;

        // 随机生成地图
        data = new int[size];
        for (int i = 0; i < size; i++) {
            data[i] = (int) (Math.random() * texCols * texRows);
        }

        quads = ByteBuffer.
                allocateDirect( size * 16 * Float.SIZE / 8 ).
                order( ByteOrder.nativeOrder() ).
                asFloatBuffer();

        float y1 = 0;               // top of first cell
        float y2 = y1 + cellH;      // bottom
        for (int i = 0; i < mapHeight; i++) {
            float x1 = 0;           // left
            float x2 = x1 + cellW;  // right
            int pos = i * mapWidth; // 第i行瓦片的开始位置
            quads.position(16 * pos); //每瓦片占16位置
            for (int j = 0; j < mapWidth; j++) {

                int loc = data[pos++];
                float uleft = (loc % texCols) * uw;     //纹理坐标left
                float utop = (int) (loc / texRows) * vh; //纹理坐标top

                vertices[0] = x1;
                vertices[1] = y1;

                vertices[2] = uleft;
                vertices[3] = utop;

                vertices[4] = x2;
                vertices[5] = y1;

                vertices[6] = uleft + uw;
                vertices[7] = utop;

                vertices[8] = x2;
                vertices[9] = y2;

                vertices[10] = uleft + uw;
                vertices[11] = utop + vh;

                vertices[12] = x1;
                vertices[13] = y2;

                vertices[14] = uleft;
                vertices[15] = utop + vh;

                quads.put(vertices);

                x1 = x2;    // next col right
                x2 += cellW;
            }
            y1 = y2;        // next row below
            y2 += cellH;
        }
    }

    public void draw() {
        Game.instance.drawQuadSet(quads,size,texId);
    }

}
