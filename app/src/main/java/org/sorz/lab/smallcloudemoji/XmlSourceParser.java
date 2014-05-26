package org.sorz.lab.smallcloudemoji;

import android.content.Context;

import org.ktachibana.cloudemoji.helpers.RepoXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * Read emojis (groups) from a XML file which is downloaded from source.
 * A adapter connecting to orz.katachibana.cloudemoji.helpers.RepoXmlParser.
 */
public class XmlSourceParser {

    public static List<EmoticonGroup> parserAll(Context context, String filename)
            throws IOException, XmlPullParserException {
        Reader reader = null;
        RepoXmlParser.Emoji emojis;
        try {
            reader = new BufferedReader(new InputStreamReader(context.openFileInput(filename)));
            RepoXmlParser repoXmlParser = new RepoXmlParser();
            emojis = repoXmlParser.parse(reader);
        } finally {
            if (reader != null)
                try {
                    reader.close();
                } catch (IOException e) {
                    // Ignore
                }
        }
        List<EmoticonGroup> emoticonGroups = new ArrayList<EmoticonGroup>();
        for (RepoXmlParser.Category category : emojis.categories) {
            EmoticonGroup group = new EmoticonGroup(category.name);
            for (RepoXmlParser.Entry entry : category.entries)
                group.add(new Emoticon(entry.string, entry.note, false));
            emoticonGroups.add(group);
        }
        return emoticonGroups;
    }
}
