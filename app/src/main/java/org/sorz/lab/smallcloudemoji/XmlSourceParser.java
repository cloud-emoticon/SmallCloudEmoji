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

    public static List<EmojiGroup> parserAll(Context context, String filename)
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
        List<EmojiGroup> emojiGroups = new ArrayList<EmojiGroup>();
        for (RepoXmlParser.Category category : emojis.categories) {
            EmojiGroup group = new EmojiGroup(category.name);
            for (RepoXmlParser.Entry entry : category.entries)
                group.add(new Emoji(entry.string, entry.note, false));
            emojiGroups.add(group);
        }
        return emojiGroups;
    }
}
