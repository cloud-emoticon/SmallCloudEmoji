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
public class DatabaseOpenHelper {
    private DaoMaster daoMaster;
    private DaoSession daoSession;
    private Repository defaultRepository;

    public DatabaseOpenHelper(Context context) {
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

    public DaoMaster getDaoMaster() {
        return daoMaster;
    }

    public DaoSession getDaoSession() {
        return daoSession;
    }

    public Repository getDefaultRepository() {
        return defaultRepository;
    }
}
