package com.facebeauty.com.beautysdk;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

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
                sqlite.insert(DBColumn.TABLE_NAME, null, values);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
//                closeSqlite(sqlite);
            }
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
