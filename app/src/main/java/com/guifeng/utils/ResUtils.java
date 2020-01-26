package com.guifeng.utils;

import android.content.Context;
import android.content.res.Resources;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ResUtils {
    /**
     * 读取资源
     *
     * @param instance  上下文
     * @param resourceId raw资源id
     *
     * @return 读取的资源内容，例如着色器脚本
     */
    public static String readResource(Context instance, int resourceId) {
        StringBuilder builder = new StringBuilder();
        try {
            InputStream inputStream = instance.getResources().openRawResource(resourceId);
            InputStreamReader streamReader = new InputStreamReader(inputStream);

            BufferedReader bufferedReader = new BufferedReader(streamReader);
            String textLine;
            while ((textLine = bufferedReader.readLine()) != null) {
                builder.append(textLine);
                builder.append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Resources.NotFoundException e) {
            e.printStackTrace();
        }
        return builder.toString();
    }
}
