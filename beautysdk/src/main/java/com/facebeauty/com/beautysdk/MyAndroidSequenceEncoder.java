package com.facebeauty.com.beautysdk;

import android.graphics.Bitmap;

import org.jcodec.scale.BitmapUtil;

import java.io.File;
import java.io.IOException;

/**
 * Created by liupan on 17/9/26.
 */

public class MyAndroidSequenceEncoder extends MySequenceEncoder {
    public MyAndroidSequenceEncoder(File out) throws IOException {
        super(out);
    }

    public void encodeImage(Bitmap bi) throws IOException {
        encodeNativeFrame(BitmapUtil.fromBitmap(bi));
    }
}
