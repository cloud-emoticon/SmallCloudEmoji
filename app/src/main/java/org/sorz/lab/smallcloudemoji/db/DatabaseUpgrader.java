package org.sorz.lab.smallcloudemoji.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import org.sorz.lab.smallcloudemoji.parsers.RepositoryXmlLoader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * Migrates old SQLite data into new greenDAO.
 */
public class DatabaseUpgrader {
    static final private String SQL_FAVORITES = "SELECT emoji FROM history WHERE top = 1";

    static public void checkAndDoUpgrade(Context context, DaoSession daoSession) {
        File xmlFile = context.getFileStreamPath("emojis.xml");
        if (!xmlFile.exists())
            return;
        RepositoryDao repositoryDao = daoSession.getRepositoryDao();
        Repository repository = repositoryDao.queryBuilder().limit(1).unique();
        if (!repository.getCategories().isEmpty())
            return;

        Reader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(xmlFile)));
            new RepositoryXmlLoader(daoSession).loadToDatabase(repository, reader);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (reader != null)
                try {
                    reader.close();
                } catch (IOException e) {
                    // Ignore
                }
        }
        repository.resetCategories();
        //noinspection ResultOfMethodCallIgnored
        xmlFile.delete();

        File databaseFile = context.getDatabasePath("emoji.db");
        SQLiteDatabase sqLiteDatabase = null;
        if (databaseFile.exists()) {
            try {
                sqLiteDatabase = SQLiteDatabase.openDatabase(databaseFile.getPath(),
                        null, SQLiteDatabase.OPEN_READONLY);
                Cursor cursor = sqLiteDatabase.rawQuery(SQL_FAVORITES, null);
                EntryDao entryDao = daoSession.getEntryDao();
                List<Entry> starEntries = new ArrayList<Entry>();
                while (cursor.moveToNext()) {
                    String emoticon = cursor.getString(0);
                    List<Entry> entries = entryDao.queryBuilder()
                            .where(EntryDao.Properties.Emoticon.eq(emoticon))
                            .list();
                    starEntries.addAll(entries);
                }
                for (Entry entry : starEntries)
                    entry.setStar(true);
                entryDao.updateInTx(starEntries);
            } catch (SQLiteException e) {
                e.printStackTrace();
            } finally {
                if (sqLiteDatabase != null)
                    sqLiteDatabase.close();
            }
            //noinspection ResultOfMethodCallIgnored
            databaseFile.delete();
        }

    }

}
