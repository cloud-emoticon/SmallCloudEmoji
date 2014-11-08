package org.sorz.lab.smallcloudemoji.parsers;

import android.util.Xml;

import org.sorz.lab.smallcloudemoji.db.DaoSession;
import org.sorz.lab.smallcloudemoji.db.RepositoryDao;
import org.sorz.lab.smallcloudemoji.db.Source;
import org.sorz.lab.smallcloudemoji.db.SourceDao;
import org.sorz.lab.smallcloudemoji.exceptions.PullParserException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Read store xml and store sources into database.
 */
public class StoreSourceLoader {
    private final DaoSession daoSession;
    private final SourceDao sourceDao;
    private final RepositoryDao repositoryDao;

    private static final String NAME_SPACE = null;
    private static final String ROOT_TAG = "cloudemoticonstore";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");


    public StoreSourceLoader(DaoSession daoSession) {
        this.daoSession = daoSession;
        sourceDao = daoSession.getSourceDao();
        repositoryDao = daoSession.getRepositoryDao();
    }

    /**
     * Parse source from XML reader to databases.
     *
     * @param reader         Reader of XML file.
     * @param lastUpdateTime Latest update time of store.
     * @return Update time of current store (may be not changed if it's already updated so that
     * parsing is canceled.
     */
    public String loadToDatabase(Reader reader, String lastUpdateTime)
            throws IOException, PullParserException {

        XmlPullParser parser = Xml.newPullParser();
        String updateTime;
        try {
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(reader);

            parser.nextTag();
            parser.require(XmlPullParser.START_TAG, NAME_SPACE, ROOT_TAG);

            // Continue parser only when newer version exists.
            updateTime = parser.getAttributeValue(NAME_SPACE, "updatetime");
            if (lastUpdateTime != null && updateTime != null) {
                if (lastUpdateTime.equals(updateTime))
                    return updateTime;
            }

            // Check whether already installed for each.
            final List<Source> sources = loadStore(parser);
            for (Source source : sources) {
                long count = repositoryDao.queryBuilder()
                        .where(RepositoryDao.Properties.Url.eq(source.getCodeUrl()))
                        .limit(1)
                        .count();
                source.setInstalled(count != 0);
            }

            daoSession.runInTx(new Runnable() {
                @Override
                public void run() {
                    sourceDao.deleteAll();
                    sourceDao.insertInTx(sources);
                }
            });

        } catch (XmlPullParserException e) {
            e.printStackTrace();
            throw new PullParserException(e);
        }
        if (updateTime == null)
            return "0";
        else
            return updateTime;
    }

    private static List<Source> loadStore(XmlPullParser parser)
            throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, NAME_SPACE, ROOT_TAG);

        ArrayList<Source> sources = new ArrayList<Source>();
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            if (parser.getName().equals("source")) {
                Source source = loadSource(parser);
                if (source != null)
                    sources.add(source);
            }
        }
        return sources;
    }

    private static Source loadSource(XmlPullParser parser)
            throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, NAME_SPACE, "source");

        Source source = new Source();
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tagName = parser.getName();
            if (tagName.equals("name"))
                source.setName(parser.nextText());
            else if (tagName.equals("iconurl"))
                source.setIconUrl(parser.nextText());
            else if (tagName.equals("postedon"))
                try {
                    source.setPostDate(DATE_FORMAT.parse(parser.nextText()));
                } catch (ParseException e) {
                    e.printStackTrace();  // Ignore
                }
            else if (tagName.equals("introduction"))
                source.setIntroduction(parser.nextText());
            else if (tagName.equals("creator"))
                source.setCreator(parser.nextText());
            else if (tagName.equals("creatorurl"))
                source.setCreatorUrl(parser.nextText());
            else if (tagName.equals("server"))
                source.setServer(parser.nextText());
            else if (tagName.equals("serverurl"))
                source.setServerUrl(parser.nextText());
            else if (tagName.equals("dataformat"))
                source.setDataFormat(parser.nextText());
            else if (tagName.equals("installurl"))
                source.setInstallUrl(parser.nextText());
            else if (tagName.equals("codeurl"))
                source.setCodeUrl(parser.nextText());
            else if (tagName.equals("storeurl"))
                source.setStoreUrl(parser.nextText());
        }
        if (source.getCodeUrl() == null || source.getName() == null)
            return null;
        return source;
    }

}
