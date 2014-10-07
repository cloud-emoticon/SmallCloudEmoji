package org.sorz.lab.smallcloudemoji.parsers;

import android.util.JsonReader;

import org.sorz.lab.smallcloudemoji.db.DaoSession;
import org.sorz.lab.smallcloudemoji.exceptions.LoadingCancelException;
import org.sorz.lab.smallcloudemoji.exceptions.PullParserException;

import java.io.IOException;
import java.io.Reader;

/**
 * Load a repository to database from a json stream (reader).
 */
public class RepositoryJsonLoader extends RepositoryLoader {
    private JsonReader parser;

    public RepositoryJsonLoader(DaoSession daoSession) {
        super(daoSession);
    }

    @Override
    protected void loadRepository(Reader reader)
            throws PullParserException, IOException, LoadingCancelException {
        parser = new JsonReader(reader);

        parser.beginObject();
        while (parser.hasNext()) {
            String name = parser.nextName();
            if (! name.equals("categories")) {
                parser.skipValue();
                continue;
            }
            parser.beginArray();
            while (parser.hasNext()) {
                loadCategory();
            }
            parser.endArray();
        }
        parser.endObject();
    }


    public void loadCategory() throws IOException, LoadingCancelException {
        boolean nameFound = false;
        parser.beginObject();
        while (parser.hasNext()) {
            String name = parser.nextName();
            if (name.equals("name") && !nameFound) {
                nameFound = true;
                beginCategory(parser.nextString());
            } else if (name.equals("entries")) {
                parser.beginArray();
                while (parser.hasNext()) {
                    loadEntry();
                }
                parser.endArray();
            } else {
                parser.skipValue();
            }
        }
        parser.endObject();
        if (!nameFound)
            beginCategory("Category");  // Apply default name.
        endCategory();
    }

    public void loadEntry() throws IOException, LoadingCancelException {
        String description = "";
        String emoticon = null;
        parser.beginObject();
        while (parser.hasNext()) {
            String name = parser.nextName();
            if (name.equals("description"))
                description = parser.nextString();
            else if (name.equals("emoticon"))
                emoticon = parser.nextString();
            else
                parser.skipValue();
        }
        parser.endObject();
        if (emoticon != null)
            addEntry(emoticon, description);
    }
}
