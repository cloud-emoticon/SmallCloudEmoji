/* Copyright 2014 Sorz
 * Licensed under the Apache License, Version 2.0
 *
 *
 * This class is modified from
 * https://github.com/KTachibanaM/cloudemoji/blob/master/CloudEmoji/src/main/
 * java/org/ktachibana/cloudemoji/helpers/RepoXmlParser.java
 *
 * Copyright 2014 KTachibanaM for the original file
 * Licensed under the Apache License, Version 2.0
 */
package org.sorz.lab.smallcloudemoji.parsers;

import android.util.Xml;

import org.sorz.lab.smallcloudemoji.db.Category;
import org.sorz.lab.smallcloudemoji.db.CategoryDao;
import org.sorz.lab.smallcloudemoji.db.DaoSession;
import org.sorz.lab.smallcloudemoji.db.Entry;
import org.sorz.lab.smallcloudemoji.db.EntryDao;
import org.sorz.lab.smallcloudemoji.db.Repository;
import org.sorz.lab.smallcloudemoji.db.RepositoryDao;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;


public class RepositoryXmlLoader {
    private static final String ns = null;
    private DaoSession daoSession;
    private RepositoryDao repositoryDao;
    private CategoryDao categoryDao;
    private EntryDao entryDao;
    private Date updateDate;
    private RepositoryLoaderEventListener eventListener;

    public RepositoryXmlLoader(DaoSession daoSession) {
        this.daoSession = daoSession;
        repositoryDao = daoSession.getRepositoryDao();
        categoryDao = daoSession.getCategoryDao();
        entryDao = daoSession.getEntryDao();
    }

    public void setLoaderEventListener(RepositoryLoaderEventListener eventListener) {
        this.eventListener = eventListener;
    }

    public void loadToDatabase(final Repository repository, Reader xmlReader)
            throws Exception {
        updateDate = new Date();
        // Insert it into database only when the repository is new one, i.e. ID is null.
        if (repository.getId() == null) {
            repository.setLastUpdateDate(updateDate);
            repositoryDao.insert(repository);
        }
        final XmlPullParser parser = Xml.newPullParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
        parser.setInput(xmlReader);
        parser.nextTag();

        daoSession.callInTx(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                try {
                    loadRepository(repository, parser);
                } catch (Exception e) {
                    // Delete all things if the repository is new one.
                    // Otherwise keep all things to prevent lost usage information.
                    if (repository.getLastUpdateDate().equals(updateDate)) {
                        entryDao.queryBuilder()
                                .where(EntryDao.Properties.LastUpdateDate.eq(updateDate))
                                .buildDelete().executeDeleteWithoutDetachingEntities();
                        categoryDao.queryBuilder()
                                .where(CategoryDao.Properties.LastUpdateDate.eq(updateDate))
                                .buildDelete().executeDeleteWithoutDetachingEntities();
                        repository.delete();
                    }
                    // Delete all outdated items.
                    if (!repository.getLastUpdateDate().equals(updateDate)) {
                        entryDao.queryBuilder()
                                .where(EntryDao.Properties.LastUpdateDate.notEq(updateDate))
                                .buildDelete().executeDeleteWithoutDetachingEntities();
                        categoryDao.queryBuilder()
                                .where(CategoryDao.Properties.LastUpdateDate.notEq(updateDate))
                                .buildDelete().executeDeleteWithoutDetachingEntities();
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


    private void loadRepository(Repository repository, XmlPullParser parser)
            throws XmlPullParserException, IOException, LoadingCancelException {
        parser.require(XmlPullParser.START_TAG, ns, "emoji");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tagName = parser.getName();
            if (tagName.equals("infoos")) {
                // Ignore infoos
                //noinspection StatementWithEmptyBody
                while (parser.next() != XmlPullParser.END_TAG ||
                        !parser.getName().equals("infoos"))
                    ;
            } else if (tagName.equals("category")) {
                loadCategory(parser, repository);
            }
        }
    }


    private void loadCategory(XmlPullParser parser, Repository repository)
            throws XmlPullParserException, IOException, LoadingCancelException {
        parser.require(XmlPullParser.START_TAG, ns, "category");
        String categoryName = parser.getAttributeValue(null, "name");

        Category category = null;
        // Try to get category from database first
        // if the repository which it belong to is not new added one.
        if (!repository.getLastUpdateDate().equals(updateDate)) {
            category = categoryDao.queryBuilder()
                    .where(CategoryDao.Properties.RepositoryId.eq(repository.getId()),
                            CategoryDao.Properties.Name.eq(categoryName))
                    .unique();
        }
        if (category == null) {
            category = new Category(null, categoryName, false, updateDate, null);
            category.setRepository(repository);
            categoryDao.insert(category);
        }
        if (eventListener != null)
            if (eventListener.onLoadingCategory(category))
                throw new LoadingCancelException();

        List<Entry> entries = new ArrayList<Entry>();
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            if (parser.getName().equals("entry")) {
                Entry entry = loadEntry(parser, category);
                entries.add(entry);
                if (eventListener != null)
                    if (eventListener.onEntryLoaded(entry))
                        throw new LoadingCancelException();
            }
        }
        List<Entry> updateEntries = new ArrayList<Entry>();
        List<Entry> insertEntries = new ArrayList<Entry>();
        if (!category.getLastUpdateDate().equals(updateDate)) {
            List<Entry> oldEntries = entryDao.queryBuilder()
                    .where(EntryDao.Properties.CategoryId.eq(category.getId()))
                    .list();
            if (oldEntries.size() > 0) {
                Map<String, Entry> oldEntryMap = new HashMap<String, Entry>(oldEntries.size());
                for (Entry entry : oldEntries) {
                    oldEntryMap.put(entry.getEmoticon(), entry);
                }
                for (Entry entry : entries) {
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
                insertEntries = entries;
            }
        } else {
            insertEntries = entries;
        }
        entryDao.insertInTx(insertEntries);
        entryDao.updateInTx(updateEntries);

        if (!category.getLastUpdateDate().equals(updateDate)) {
            category.setLastUpdateDate(updateDate);
            category.update();
        }
    }


    private Entry loadEntry(XmlPullParser parser, Category category)
            throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "entry");
        String emoticon = "";
        String description = "";
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals("string")) {
                emoticon = readString(parser);
            } else if (name.equals("note")) {
                description = readNote(parser);
            }
        }
        return new Entry(null, emoticon, description, false, null, updateDate, category.getId());
    }


    private String readString(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "string");
        String string = parser.nextText();
        if (parser.getEventType() != XmlPullParser.END_TAG) {
            parser.nextTag();
        }
        parser.require(XmlPullParser.END_TAG, ns, "string");
        return string;
    }


    private String readNote(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "note");
        String note = parser.nextText();
        if (parser.getEventType() != XmlPullParser.END_TAG) {
            parser.nextTag();
        }
        parser.require(XmlPullParser.END_TAG, ns, "note");
        return note;
    }

}
