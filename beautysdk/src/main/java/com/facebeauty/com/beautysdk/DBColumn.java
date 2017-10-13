package com.facebeauty.com.beautysdk;

/**
 * Created by ios-dev on 17/10/12.
 */

public class DBColumn {
    public static final String DB_TABLE_NAME = "shelfbook";
    public static final String TABLE_NAME = "shelfbook_5";
    public static final String ID = "_id";
    public static final String DATA = "aaaaaa";

    public static String createShelfTable() {
        String sql = "create table IF NOT EXISTS " +
                DBColumn.TABLE_NAME + " (" +
                DBColumn.ID + " integer primary key autoincrement, " +
                DBColumn.DATA + " blob );";
//                ShelfBookDBColumn.BOOK_FINISH + " int default 0, " +
//                ShelfBookDBColumn.BOOK_TYPE + " int default 0, " +
//                ShelfBookDBColumn.TRY_OR_FULL + " int default 0, " +
//                ShelfBookDBColumn.READ_PROGRESS + " varchar, " +
//                ShelfBookDBColumn.LAST_TIME + " long default 0, " +
//                ShelfBookDBColumn.USER_ID + " varchar, " +
//                ShelfBookDBColumn.USER_NAME + " varchar, " +
//                ShelfBookDBColumn.GROUP_ID + " int default 0, " +
//                ShelfBookDBColumn.LOCAL_GROUP_ID + " int default 0, " +
//                ShelfBookDBColumn.IS_FOLLOW + " int default 0, " +
//                ShelfBookDBColumn.MONTHLY_PAYMENT_TYPE + "  int default 0, " +
//                ShelfBookDBColumn.MONTHLY_END_TIME + " long default 0, " +
//                ShelfBookDBColumn.LOCAL_IMPORT + " int default 0, " +
//                ShelfBookDBColumn.OVER_DUE + " int default 0, " +
//                ShelfBookDBColumn.BOOK_STRUCT + " varchar, " +
//                ShelfBookDBColumn.TOTAL_TIME + " varchar, " +
//                ShelfBookDBColumn.ExpColumn1 + " varchar, " +
//                ShelfBookDBColumn.ExpColumn2 + " varchar, " +
//                ShelfBookDBColumn.ExpColumn3 + " varchar);";
        return sql;
    }


}
