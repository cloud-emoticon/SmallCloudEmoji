package org.sorz.lab.smallcloudemoji.parsers;

import org.sorz.lab.smallcloudemoji.db.Category;
import org.sorz.lab.smallcloudemoji.db.CategoryDao;
import org.sorz.lab.smallcloudemoji.db.DaoSession;
import org.sorz.lab.smallcloudemoji.db.Entry;
import org.sorz.lab.smallcloudemoji.db.EntryDao;
import org.sorz.lab.smallcloudemoji.db.Repository;
import org.sorz.lab.smallcloudemoji.db.RepositoryDao;
import org.sorz.lab.smallcloudemoji.exceptions.LoadingCancelException;
import org.sorz.lab.smallcloudemoji.exceptions.PullParserException;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Parse a stream into one repository, and add it into database.
 * The abstract class implements all database-related methods, but not parser-related methods.
 */
abstract class AbstractRepositoryLoader {
    private DaoSession daoSession;
    private RepositoryDao repositoryDao;
    private CategoryDao categoryDao;
    private EntryDao entryDao;

    private Date updateDate;
    private RepositoryLoaderEventListener eventListener;

    private Repository currentRepository;
    private Category currentCategory;
    private List<Entry> currentEntries;


    public void setLoaderEventListener(RepositoryLoaderEventListener eventListener) {
        this.eventListener = eventListener;
    }


    public AbstractRepositoryLoader(DaoSession daoSession) {
        this.daoSession = daoSession;
        repositoryDao = daoSession.getRepositoryDao();
        categoryDao = daoSession.getCategoryDao();
        entryDao = daoSession.getEntryDao();
    }

    public void loadToDatabase(Repository repository, final Reader reader) throws Exception {
        updateDate = new Date();
        if (repository.getLastUpdateDate() == null)
            repository.setLastUpdateDate(updateDate);
        // Insert it into database only when the repository is new one, i.e. ID is null.
        if (repository.getId() == null)
            repositoryDao.insert(repository);
        currentRepository = repository;

        daoSession.callInTx(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                try {
                    loadRepository(reader);
                } catch (Exception e) {
                    // Delete all things if the repository is new one.
                    // Otherwise keep all things to prevent lost usage statistics.
                    if (currentRepository.getLastUpdateDate().equals(updateDate)) {
                        entryDao.queryBuilder()
                                .where(EntryDao.Properties.LastUpdateDate.eq(updateDate))
                                .buildDelete().executeDeleteWithoutDetachingEntities();
                        categoryDao.queryBuilder()
                                .where(CategoryDao.Properties.LastUpdateDate.eq(updateDate))
                                .buildDelete().executeDeleteWithoutDetachingEntities();
                        currentRepository.delete();
                    }
                    throw e;
                }
                return null;
            }
        });

        // Update updateDate after all, because it will be used to determine whether it is new one.
        repository.setLastUpdateDate(updateDate);
        repository.update();
    }

    /**
     * Add a category to database,
     * and all entries will be added to this category before endCategory called.
     * Must be called in the beginning of parsing category.
     * @param name The name of category.
     * @throws LoadingCancelException Loading canceled via LoaderEventListener.
     */
    protected void beginCategory(String name)
            throws LoadingCancelException {
        Category category = null;
        // Try to get category from database first
        // if the repository which it belong to is not new added one.
        if (!currentRepository.getLastUpdateDate().equals(updateDate)) {
            category = categoryDao.queryBuilder()
                    .where(CategoryDao.Properties.RepositoryId.eq(currentRepository.getId()),
                            CategoryDao.Properties.Name.eq(name))
                    .unique();
        }
        if (category == null) {
            category = new Category(null, name, false, updateDate, null);
            category.setRepository(currentRepository);
            categoryDao.insert(category);
        }
        if (eventListener != null)
            if (eventListener.onLoadingCategory(category))
                throw new LoadingCancelException();
        currentCategory = category;
        currentEntries = new ArrayList<Entry>();
    }

    /**
     * Add a entry to buffer.
     * Important: this method must be call between beginCategory() called and endCategory() called.
     * @param emoticon Emoticon string.
     * @param description Optional description string.
     * @throws LoadingCancelException Loading canceled via LoaderEventListener.
     */
    protected void addEntry(String emoticon, String description) throws LoadingCancelException {
        if (description == null)
            description = "";
        Entry entry = new Entry(null, emoticon, description, false, null,
                updateDate, currentCategory.getId());
        currentEntries.add(entry);
        if (eventListener != null)
            if (eventListener.onEntryLoaded(entry))
                throw new LoadingCancelException();
    }


    /**
     * Add or update all entries on buffer into database.
     * Must be called in the end of parsing category.
     */
    protected void endCategory() {
        List<Entry> updateEntries = new ArrayList<Entry>();
        List<Entry> insertEntries = new ArrayList<Entry>();
        if (!currentCategory.getLastUpdateDate().equals(updateDate)) {
            List<Entry> oldEntries = entryDao.queryBuilder()
                    .where(EntryDao.Properties.CategoryId.eq(currentCategory.getId()))
                    .list();
            if (oldEntries.size() > 0) {
                Map<String, Entry> oldEntryMap = new HashMap<String, Entry>(oldEntries.size());
                for (Entry entry : oldEntries) {
                    oldEntryMap.put(entry.getEmoticon(), entry);
                }
                for (Entry entry : currentEntries) {
                    Entry oldEntry = oldEntryMap.get(entry.getEmoticon());
                    if (oldEntry != null) {
                        oldEntry.setLastUpdateDate(updateDate);
                        oldEntry.setDescription(entry.getDescription());
                        updateEntries.add(oldEntry);
                    } else {
                        insertEntries.add(entry);
                    }
                }
            } else {
                insertEntries = currentEntries;
            }
        } else {
            insertEntries = currentEntries;
        }
        entryDao.insertInTx(insertEntries);
        entryDao.updateInTx(updateEntries);

        if (!currentCategory.getLastUpdateDate().equals(updateDate)) {
            currentCategory.setLastUpdateDate(updateDate);
            currentCategory.update();
        }
        currentCategory = null;
        currentEntries = null;
    }

    protected abstract void loadRepository(Reader reader)
            throws PullParserException, IOException, LoadingCancelException;

}
