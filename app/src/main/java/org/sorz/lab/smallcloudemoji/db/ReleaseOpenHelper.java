package org.sorz.lab.smallcloudemoji.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

public class ReleaseOpenHelper extends DaoMaster.OpenHelper {
    public ReleaseOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory) {
        super(context, name, factory);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion == 1 && newVersion == 2) {
            SourceDao.createTable(db, false);
        }
    }
}
