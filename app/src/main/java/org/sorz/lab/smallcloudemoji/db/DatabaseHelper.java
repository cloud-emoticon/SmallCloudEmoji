package org.sorz.lab.smallcloudemoji.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import org.sorz.lab.smallcloudemoji.R;


/**
 * A singleton class to open database.
 */
public class DatabaseHelper {
    private static DatabaseHelper ourInstance;
    private static int userCounter = 0;
    private final DaoMaster daoMaster;
    private final DaoSession daoSession;

    public static DatabaseHelper getInstance(Context context) {
        return getInstance(context, false);
    }

    public static synchronized DatabaseHelper getInstance(Context context, Boolean iWillCloseIt) {
        if (iWillCloseIt)
            userCounter += 1;
        if (ourInstance == null)
            ourInstance = new DatabaseHelper(context);
        return ourInstance;
    }

    private DatabaseHelper(Context context) {
        DaoMaster.OpenHelper helper = new ReleaseOpenHelper(context, "repo.db", null);
        SQLiteDatabase db = helper.getWritableDatabase();
        daoMaster = new DaoMaster(db);
        daoSession = daoMaster.newSession();

        if (daoSession.getRepositoryDao().queryBuilder().count() == 0) {
            String sourceUrl = context.getString(R.string.pref_source_address_default);
            Repository repository = new Repository(null, sourceUrl, "Default", false, 100, null);
            daoSession.getRepositoryDao().insert(repository);
        }
    }

    public DaoSession getDaoSession() {
        return daoSession;
    }

    public void close() {
        userCounter -= 1;
        if (userCounter <= 0) {
            daoMaster.getDatabase().close();
            ourInstance = null;
        }
    }
}
