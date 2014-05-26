package org.sorz.lab.smallcloudemoji;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.Closeable;
import java.io.IOException;

/**
 * Read and write history table in SQLite.
 */
public class HistoryDataSource implements Closeable {
    private SQLiteDatabase database;

    private final static String SQL_SELECT_BY_EMOJI = "SELECT id, note FROM history WHERE emoji = ?";
    private final static String SQL_SELECT_FAV = "SELECT emoji, note, top FROM history "
            + "ORDER BY top DESC, last_use DESC LIMIT ";
    private final static String SQL_INSERT = "INSERT INTO history (emoji, note) VALUES (?, ?)";
    private final static String SQL_INSERT_STAR = "INSERT INTO history (emoji, note, times, top)"
            + "VALUES (?, ?, 0, 1)";
    private final static String SQL_UPDATE_TIMES_NOTE = "UPDATE history SET times = times + 1, "
            + "last_use = CURRENT_TIMESTAMP, note = ? WHERE id =";
    private final static String SQL_UPDATE_SET_STAR = "UPDATE history SET top = 1 WHERE id = ";
    private final static String SQL_UPDATE_UNSET_STAR = "UPDATE history SET top = 0 WHERE id = ";
    private final static String SQL_UPDATE_CLEAN_STARS = "UPDATE history SET top = 0";
    private final static String SQL_UPDATE_CLEAN_HISTORY = "UPDATE history SET times = 0 WHERE top = 1";
    private final static String SQL_DELETE_UNUSED = "DELETE FROM history WHERE times = 0 AND top = 0 ";
    private final static String SQL_DELETE_HISTORY = "DELETE FROM history WHERE top == 0";


    public HistoryDataSource(Context context) {
        SQLiteOpenHelper dbOpenHelper = new OrzSQLiteOpenHelper(context);
        database = dbOpenHelper.getWritableDatabase();
    }

    public Emoticon[] getFavorites(int count) {
        Cursor cursor = database.rawQuery(SQL_SELECT_FAV + count, null);
        Emoticon[] emoticons = new Emoticon[cursor.getCount()];
        int i = 0;
        while(cursor.moveToNext())
            emoticons[i++] = new Emoticon(cursor.getString(0),
                    cursor.getString(1), cursor.getInt(2) != 0);
        cursor.close();
        return emoticons;
    }

    /**
     * Add the emoticon into history if it does not exist.
     * Or update the times, last used time (and note).
     */
    public void updateHistory(Emoticon emoticon) {
        int id = searchAndGetEmojiId(emoticon);
        if (id == -1)  // No exists in history.
            database.execSQL(SQL_INSERT, new String[]{emoticon.toString(), emoticon.getNote()});
        else
            database.execSQL(SQL_UPDATE_TIMES_NOTE + id, new String[]{emoticon.getNote()});
    }

    public void setStar(Emoticon emoticon) {
        int id = searchAndGetEmojiId(emoticon);
        if (id == -1) // No exists in history.
            database.execSQL(SQL_INSERT_STAR, new String[]{emoticon.toString(), emoticon.getNote()});
        else
            database.execSQL(SQL_UPDATE_SET_STAR + id);
    }

    public void unsetStar(Emoticon emoticon) {
        int id = searchAndGetEmojiId(emoticon);
        if (id == -1) // No exists in history.
            return;
        database.execSQL(SQL_UPDATE_UNSET_STAR + id);
        database.execSQL(SQL_DELETE_UNUSED);
    }

    public void cleanAllStars() {
        database.execSQL(SQL_UPDATE_CLEAN_STARS);
        database.execSQL(SQL_DELETE_UNUSED);
    }

    public void cleanHistory() {
        database.execSQL(SQL_DELETE_HISTORY);
        database.execSQL(SQL_UPDATE_CLEAN_HISTORY);
    }

    /**
     * Check if the emoticon is in history table.
     * @return -1 or the ID of emoticon if it exists.
     */
    private int searchAndGetEmojiId(Emoticon emoticon) {
        Cursor cursor = database.rawQuery(SQL_SELECT_BY_EMOJI, new String[] {emoticon.toString()});
        int id;
        if (cursor.getCount() == 0) {
            id = -1;
        } else {
            cursor.moveToFirst();
            id = cursor.getInt(0);
        }
        cursor.close();
        return id;
    }


    @Override
    public void close() throws IOException {
        database.close();
    }
}
