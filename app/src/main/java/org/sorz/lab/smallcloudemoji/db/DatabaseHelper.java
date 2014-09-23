package org.sorz.lab.smallcloudemoji.db;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;

import org.sorz.lab.smallcloudemoji.R;

import java.util.Date;

/**
 * Created by xierch on 2014/9/16.
 */
public class DatabaseHelper {
    private static DatabaseHelper ourInstance;
    private static int userCounter = 0;
    private DaoMaster daoMaster;
    private DaoSession daoSession;
    private Repository defaultRepository;

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
        DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(context, "repo.db", null);
        SQLiteDatabase db = helper.getWritableDatabase();
        daoMaster = new DaoMaster(db);
        daoSession = daoMaster.newSession();

        defaultRepository = daoSession.getRepositoryDao().queryBuilder()
                .where(RepositoryDao.Properties.Alias.eq("Default"))
                .limit(1).unique();
        if (defaultRepository == null) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            String sourceUrl = preferences.getString("sync_source",
                    context.getResources().getString(R.string.pref_source_address_default));
            defaultRepository = new Repository(null, sourceUrl, "Default", false, 100, new Date());
            daoSession.getRepositoryDao().insert(defaultRepository);
        }
    }

    public DaoSession getDaoSession() {
        return daoSession;
    }

    public Repository getDefaultRepository() {
        return defaultRepository;
    }

    public void close() {
        userCounter -= 1;
        if (userCounter == 0) {
            daoMaster.getDatabase().close();
            ourInstance = null;
        }
    }
}
