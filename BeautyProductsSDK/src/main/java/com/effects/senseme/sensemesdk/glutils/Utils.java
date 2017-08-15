package com.effects.senseme.sensemesdk.glutils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Created by wangdi on 2017/7/30.
 */

public class Utils {
    /**
     * @param vertexs float 数组
     * @return 获取浮点形缓冲数据
     */
    public static FloatBuffer getFloatBuffer(float[] vertexs) {
        // 初始化ByteBuffer，长度为arr数组的长度*4，因为一个int占4个字节
        ByteBuffer qbb = ByteBuffer.allocateDirect(vertexs.length * 4);
        // 数组排列用nativeOrder
        qbb.order(ByteOrder.nativeOrder());
        FloatBuffer mBuffer = qbb.asFloatBuffer();
        mBuffer.put(vertexs);
        mBuffer.position(0);
        return mBuffer;
    }
}
