package com.facebeauty.com.beautysdk;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashSet;

/**
 * Created by ios-dev on 17/10/12.
 */

public class DBService {
    private static volatile DBService mInstance;
    private DBHelper mDB;
    protected Context mContext;
    private String TAG;

    public synchronized static DBService getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new DBService(context.getApplicationContext());
        }
        return mInstance;
    }

    protected DBService(Context context) {
        TAG = this.getClass().getName();
        mContext = context;
        mDB = DBHelper.getInstance(context);
    }

    public void release() {
        if (mDB != null) {
            try {
                mDB.release();
                mDB = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (mInstance != null)
            mInstance = null;
    }

    public void addData(byte[] bookKey) {
        synchronized (this) {
            SQLiteDatabase sqlite = null;
            try {
                sqlite = mDB.getWritableDatabase();
                ContentValues values = new ContentValues();
                values.put(DBColumn.DATA, bookKey);
                long id = sqlite.insert(DBColumn.TABLE_NAME, null, values);
                Log.d("liupan", "liupan id===" + id);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                closeSqlite(sqlite);
            }
        }
    }

    public void endRecord(File destFile, int imageHeight, int imageWidth) {

        ByteBuffer mTmpBuffer = ByteBuffer.allocate(imageHeight * imageWidth * 4);

        SQLiteDatabase sqlite = mDB.getReadableDatabase();
        Cursor cursor = null;
        sqlite.beginTransaction();

        MyAndroidSequenceEncoder sequenceEncoderMp4;
        try {
            sequenceEncoderMp4 = new MyAndroidSequenceEncoder(destFile);
//            String booksql = "select * from " + DBColumn.TABLE_NAME ;
            cursor = sqlite.query(DBColumn.TABLE_NAME, new String[]{DBColumn.ID, DBColumn.DATA}, null, null, null, null, null, null);
//            cursor = sqlite.rawQuery(booksql, null);
            while (cursor != null&& cursor.moveToNext()) {
                int index = cursor.getColumnIndex(DBColumn.DATA);
//                   String tempOneFrameData = cursor.getString(index);
//                    byte[] oneFrameData = tempOneFrameData.getBytes();
                byte[] oneFrameData = cursor.getBlob(index);
                Bitmap srcBitmap = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_4444);
                mTmpBuffer.position(0);
                mTmpBuffer.put(oneFrameData);
                srcBitmap.copyPixelsFromBuffer(mTmpBuffer);
                sequenceEncoderMp4.encodeImage(srcBitmap);
                srcBitmap.recycle();

            }
            sqlite.setTransactionSuccessful();
//
//            for (int i = 0;i<byteBuffers.size();i++) {
//                ByteBuffer byteBuffer = byteBuffers.get(i);
//                Bitmap srcBitmap = Bitmap.createBitmap(imageWidths.get(i), imageHeights.get(i), Bitmap.Config.ARGB_4444);
//                byteBuffer.position(0);
//                srcBitmap.copyPixelsFromBuffer(byteBuffer);
//                sequenceEncoderMp4.encodeImage(srcBitmap);
//                srcBitmap.recycle();
//            }

            sequenceEncoderMp4.finish();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            sqlite.endTransaction();
            closeCursor(cursor);
            closeSqlite(sqlite);
        }
    }

    protected void closeCursor(Cursor cursor) {
        try {
            if (cursor != null) {
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void closeSqlite(SQLiteDatabase sqlite) {
        try {
            if (mDB != null)
                mDB.closeSqlite(sqlite);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
