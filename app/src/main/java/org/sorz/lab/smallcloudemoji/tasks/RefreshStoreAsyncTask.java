package org.sorz.lab.smallcloudemoji.tasks;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import org.sorz.lab.smallcloudemoji.db.DaoSession;
import org.sorz.lab.smallcloudemoji.db.DatabaseHelper;
import org.sorz.lab.smallcloudemoji.db.Repository;
import org.sorz.lab.smallcloudemoji.db.Source;
import org.sorz.lab.smallcloudemoji.db.SourceDao;
import org.sorz.lab.smallcloudemoji.exceptions.PullParserException;
import org.sorz.lab.smallcloudemoji.parsers.StoreSourceLoader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Download store xml and pass it to loader.
 * And clear icon cache files when appropriate.
 */
public class RefreshStoreAsyncTask extends AsyncTask<String, Integer, Integer> {
    private final Context context;
    private final DaoSession daoSession;
    private final SharedPreferences preferences;

    private static final String LAST_UPDATE_TIME = "store_last_update_time";
    protected static final int RESULT_SUCCESS = 0;
    protected static final int RESULT_ERROR_SERVER_FAIL = 1;
    protected static final int RESULT_ERROR_IO = 2;

    public RefreshStoreAsyncTask(Context context) {
        super();
        this.context = context;
        daoSession = DatabaseHelper.getInstance(context, true).getDaoSession();
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Override
    protected Integer doInBackground(String... params) {
        InputStream inputStream = null;
        HttpURLConnection connection = null;

        try {
            URL url = new URL(params[0]);
            connection = (HttpURLConnection) url.openConnection();
            int statusCode = connection.getResponseCode();
            if (statusCode != HttpURLConnection.HTTP_OK)
                return RESULT_ERROR_SERVER_FAIL;

            inputStream = connection.getInputStream();
            StoreSourceLoader storeSourceLoader = new StoreSourceLoader(daoSession);
            String lastUpdateTime = preferences.getString(LAST_UPDATE_TIME, "0");
            String updateTime = storeSourceLoader.loadToDatabase(
                    new BufferedReader(new InputStreamReader(inputStream)), lastUpdateTime);

            // Clear icon file cache if store has been updated.
            if (!updateTime.equals(lastUpdateTime)) {
                preferences.edit().putString(LAST_UPDATE_TIME, updateTime).apply();
                File[] files = context.getCacheDir().listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        return name.startsWith("icon-") && name.endsWith(".webp");
                    }
                });
                for (File file : files) {
                    //noinspection ResultOfMethodCallIgnored
                    file.delete();
                }
            } else {
                // Check and ensure installed sign is correct.
                // Checking after updating is unnecessary because all installed sign will be
                // reset after updating.
                SourceDao sourceDao = daoSession.getSourceDao();
                List<Repository> repositories = daoSession.getRepositoryDao().queryBuilder().list();
                HashSet<String> urlSet = new HashSet<String>(repositories.size());
                for (Repository repository : repositories)
                    urlSet.add(repository.getUrl());

                List<Source> sources = sourceDao.queryBuilder().list();
                List<Source> updateSources = new ArrayList<Source>();
                for (Source source : sources) {
                    boolean installed = urlSet.contains(source.getCodeUrl());
                    if (source.getInstalled() != installed) {
                        source.setInstalled(installed);
                        updateSources.add(source);
                    }
                }
                if (!updateSources.isEmpty())
                    sourceDao.updateInTx(updateSources);
            }

        } catch (MalformedURLException e) {
            e.printStackTrace();
            return RESULT_ERROR_SERVER_FAIL;
        } catch (IOException e) {
            return RESULT_ERROR_IO;
        } catch (PullParserException e) {
            return RESULT_ERROR_SERVER_FAIL;
        } finally {
            DatabaseHelper.getInstance(context).close();
            try {
                if (inputStream != null)
                    inputStream.close();
            } catch (IOException e) {
                // Ignore it
            }
            if (connection != null)
                connection.disconnect();
        }
        return RESULT_SUCCESS;
    }

}
