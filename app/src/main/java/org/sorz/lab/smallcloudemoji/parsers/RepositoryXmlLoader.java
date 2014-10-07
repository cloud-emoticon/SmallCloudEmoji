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

import org.sorz.lab.smallcloudemoji.db.DaoSession;
import org.sorz.lab.smallcloudemoji.exceptions.LoadingCancelException;
import org.sorz.lab.smallcloudemoji.exceptions.PullParserException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.Reader;


public class RepositoryXmlLoader extends AbstractRepositoryLoader {
    private static final String ns = null;

    public RepositoryXmlLoader(DaoSession daoSession) {
        super(daoSession);
    }

    @Override
    protected void loadRepository(Reader reader)
            throws PullParserException, IOException, LoadingCancelException {
        final XmlPullParser parser = Xml.newPullParser();
        try {
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(reader);
            parser.nextTag();
            loadRepository(parser);
        } catch (XmlPullParserException e) {
            throw new PullParserException(e);
        } finally {
            reader.close();
        }

    }

    private void loadRepository(XmlPullParser parser)
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
                loadCategory(parser);
            }
        }
    }


    private void loadCategory(XmlPullParser parser)
            throws XmlPullParserException, IOException, LoadingCancelException {
        parser.require(XmlPullParser.START_TAG, ns, "category");
        String categoryName = parser.getAttributeValue(null, "name");

        beginCategory(categoryName);

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            if (parser.getName().equals("entry")) {
                loadEntry(parser);
            }
        }
        endCategory();
    }


    private void loadEntry(XmlPullParser parser)
            throws XmlPullParserException, IOException, LoadingCancelException {
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
        addEntry(emoticon, description);
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
