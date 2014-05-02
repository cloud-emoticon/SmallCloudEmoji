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
    private final static String SQL_SELECT_FAV = "SELECT emoji, note FROM history "
            + "ORDER BY top DESC, last_use DESC LIMIT ";
    private final static String SQL_INSERT = "INSERT INTO history (emoji, note) VALUES (?, ?)";
    private final static String SQL_UPDATE_TIMES = "UPDATE history SET times = times + 1, "
            + "last_use = CURRENT_TIMESTAMP WHERE id =";
    private final static String SQL_UPDATE_NOTE = "UPDATE history SET note = ? WHERE id = ";
    private final static String SQL_DELETE_ALL = "DELETE FROM history";


    public HistoryDataSource(Context context) {
        SQLiteOpenHelper dbOpenHelper = new OrzSQLiteOpenHelper(context);
        database = dbOpenHelper.getWritableDatabase();
    }

    public Emoji[] getFavorites(int count) {
        Cursor cursor = database.rawQuery(SQL_SELECT_FAV + count, null);
        Emoji[] emojis = new Emoji[cursor.getCount()];
        int i = 0;
        while(cursor.moveToNext())
            emojis[i++] = new Emoji(cursor.getString(0), cursor.getString(1));
        cursor.close();
        return emojis;
    }

    /**
     * Add the emoji into history if it does not exist.
     * Or update the times, last used time (and note).
     */
    public void updateHistory(Emoji emoji) {
        Cursor cursor = database.rawQuery(SQL_SELECT_BY_EMOJI, new String[] {emoji.toString()});
        if (cursor.getCount() == 0) {
            database.execSQL(SQL_INSERT, new String[]{emoji.toString(), emoji.getNote()});
        } else {
            cursor.moveToFirst();
            int id = cursor.getInt(0);
            String note = cursor.getString(1);
            database.execSQL(SQL_UPDATE_TIMES + id);
            if (! note.equals(emoji.getNote()))
                database.execSQL(SQL_UPDATE_NOTE + id, new String[]{emoji.getNote()});
        }
        cursor.close();

    }

    public void cleanHistory() {
        database.execSQL(SQL_DELETE_ALL);
    }


    @Override
    public void close() throws IOException {
        database.close();
    }
}
