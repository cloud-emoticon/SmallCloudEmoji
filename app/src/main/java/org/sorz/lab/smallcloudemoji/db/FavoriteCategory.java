package org.sorz.lab.smallcloudemoji.db;

import android.content.Context;

import org.sorz.lab.smallcloudemoji.R;

import java.util.List;

/**
 * Collect all stared entries. Only getEntries() and resetEntries() are available.
 */
public class FavoriteCategory extends Category {
    private List<Entry> entries;
    private EntryDao entryDao;


    public FavoriteCategory(Context context, DaoSession daoSession) {
        super(null, context.getResources().getString(R.string.list_title_favorite),
                false, null, null);
        this.entryDao = daoSession.getEntryDao();
    }

    @Override
    public List<Entry> getEntries() {
        if (entries == null) {
            List<Entry> entriesNew = entryDao.queryBuilder()
                    .where(EntryDao.Properties.Star.eq(true))
                    .list();
            synchronized (this) {
                if (entries == null) {
                    entries = entriesNew;
                }
            }
        }
        return entries;
    }

    @Override
    public void resetEntries() {
        entries = null;
    }
}
