package org.sorz.lab.smallcloudemoji;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Open the SQLite database.
 */
class OrzSQLiteOpenHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "emoji.db";
    private static final int DB_VERSION = 1;

    private static final String SQL_CREATE
            = "CREATE TABLE history ( "
            + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "emoji TEXT NOT NULL, "
            + "note TEXT, "
            + "times INTEGER NOT NULL DEFAULT 1, "
            + "last_use TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
            + "top BOOLEAN DEFAULT 0"
            + ");";


    public OrzSQLiteOpenHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
