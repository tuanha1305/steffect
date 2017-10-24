package com.facebeauty.com.beautysdk.domain;

import android.graphics.Bitmap;

import java.io.File;
import java.nio.ByteBuffer;

/**
 * Created by wangdi on 2017/8/22.
 */

public class FileSave {
    private ByteBuffer bitmap;
    private File file;
    private Bitmap bitFile;

    public void setBitFile(Bitmap bitFile) {
        this.bitFile = bitFile;
    }

    public Bitmap getBitFile() {
        return bitFile;
    }

    public void setBitmap(ByteBuffer bitmap) {
        this.bitmap = bitmap;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public ByteBuffer getBitmap() {
        return bitmap;
    }

    public File getFile() {
        return file;
    }
}
