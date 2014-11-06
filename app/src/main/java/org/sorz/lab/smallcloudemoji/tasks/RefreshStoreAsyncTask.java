package org.sorz.lab.smallcloudemoji.tasks;

import android.content.Context;
import android.os.AsyncTask;

import org.sorz.lab.smallcloudemoji.db.DaoSession;
import org.sorz.lab.smallcloudemoji.db.DatabaseHelper;
import org.sorz.lab.smallcloudemoji.exceptions.PullParserException;
import org.sorz.lab.smallcloudemoji.parsers.StoreSourceLoader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Download store xml and pass it to loader.
 */
public class RefreshStoreAsyncTask extends AsyncTask<String, Integer, Integer> {
    private Context context;
    private DaoSession daoSession;

    protected static final int RESULT_SUCCESS = 0;
    protected static final int RESULT_ERROR_SERVER_FAIL = 1;
    protected static final int RESULT_ERROR_IO = 2;

    public RefreshStoreAsyncTask(Context context) {
        super();
        this.context = context;
        daoSession = DatabaseHelper.getInstance(context, true).getDaoSession();
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
            storeSourceLoader.loadToDatabase(
                    new BufferedReader(new InputStreamReader(inputStream)));

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
