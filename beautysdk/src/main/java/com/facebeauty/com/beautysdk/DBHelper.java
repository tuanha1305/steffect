package com.facebeauty.com.beautysdk;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabaseLockedException;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by ios-dev on 17/10/12.
 */

public class DBHelper  extends SQLiteOpenHelper{

    private static final int DB_VERSION = 1;
    private static final String DB_NAME = DBColumn.DB_TABLE_NAME + ".db";
    private static DBHelper mInstance;
    private Context mContext;

    private AtomicInteger mOpenCounter = new AtomicInteger(0);
    private SQLiteDatabase mDatabase;

    public static synchronized DBHelper getInstance(Context context){
        if(mInstance == null)
            mInstance = new DBHelper(context);
        return mInstance;
    }

    private DBHelper(Context context) {
        super(context, getDBName(), null, DB_VERSION);
        mContext = context.getApplicationContext();
    }

    @Override
    public synchronized SQLiteDatabase getWritableDatabase() {
        if(mOpenCounter.incrementAndGet() == 1 || mDatabase == null || !mDatabase.isOpen()) {
            // Opening new database
            while(true){
                try{
                    mDatabase = super.getWritableDatabase();
                    break;
                }catch(SQLiteDatabaseLockedException e){
                    try{
                        Thread.sleep(100);
                    }catch(Throwable ex){
                        ex.printStackTrace();
                    }
                }
            }
        }
        return mDatabase;
    }

    @Override
    public synchronized SQLiteDatabase getReadableDatabase() {
        return getWritableDatabase();
    }

    public synchronized void closeSqlite(SQLiteDatabase sqlite) {
//		LogM.l("close db");
//        if(mOpenCounter.decrementAndGet() == 0) {
//            // Closing database
//            mDatabase.close();
//        }
    }

    private static String getDBName(){
        String name = DB_NAME;
        return name;
    }

    public synchronized void release(){
        if(mInstance != null){
            try{
                if(mDatabase != null && mDatabase.isOpen())
                    mDatabase.close();
                this.close();
            }catch(Exception e){
                e.printStackTrace();
            }
            mInstance = null;
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try{
            String  sql = DBColumn.createShelfTable();
            db.execSQL(sql);
        }catch(Exception e){
            e.printStackTrace();
        }
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}
