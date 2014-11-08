package org.sorz.lab.smallcloudemoji.tasks;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import com.google.common.io.CountingInputStream;

import org.sorz.lab.smallcloudemoji.R;
import org.sorz.lab.smallcloudemoji.db.Category;
import org.sorz.lab.smallcloudemoji.db.DaoSession;
import org.sorz.lab.smallcloudemoji.db.DatabaseHelper;
import org.sorz.lab.smallcloudemoji.db.Entry;
import org.sorz.lab.smallcloudemoji.db.Repository;
import org.sorz.lab.smallcloudemoji.db.Source;
import org.sorz.lab.smallcloudemoji.db.SourceDao;
import org.sorz.lab.smallcloudemoji.exceptions.LoadingCancelException;
import org.sorz.lab.smallcloudemoji.parsers.RepositoryJsonLoader;
import org.sorz.lab.smallcloudemoji.parsers.RepositoryLoader;
import org.sorz.lab.smallcloudemoji.parsers.RepositoryLoaderEventListener;
import org.sorz.lab.smallcloudemoji.parsers.RepositoryXmlLoader;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;


/**
 * Download and save a XML file.
 */
public class DownloadAsyncTask extends AsyncTask<Repository, Integer, Integer> {
    private Context context;
    private DaoSession daoSession;

    protected static final int RESULT_CANCELLED = -1;
    protected static final int RESULT_SUCCESS = 0;
    protected static final int RESULT_ERROR_MALFORMED_URL = 1;
    protected static final int RESULT_ERROR_IO = 2;
    protected static final int RESULT_ERROR_XML_PARSER = 3;
    protected static final int RESULT_ERROR_UNKNOWN = 4;
    protected static final int RESULT_ERROR_NOT_FOUND = 5;
    protected static final int RESULT_ERROR_OTHER_HTTP = 6;
    protected static final int RESULT_ERROR_UNSUPPORTED_FORMAT = 7;


    public DownloadAsyncTask(Context context) {
        super();
        this.context = context;
        daoSession = DatabaseHelper.getInstance(context, true).getDaoSession();
    }

    @Override
    protected Integer doInBackground(Repository... params) {
        InputStream inputStream = null;
        HttpURLConnection connection = null;
        Repository repository = params[0];

        try {
            URL url = new URL(repository.getUrl());
            for (int i = 0; i < 10; ++i) {  // Limit redirection (between HTTP and HTTPS) < 10 times.
                connection = (HttpURLConnection) url.openConnection();
                int statusCode = connection.getResponseCode();
                if (statusCode == HttpURLConnection.HTTP_OK) {
                    break;
                } else if (statusCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                        statusCode == HttpURLConnection.HTTP_MOVED_PERM) {
                    url = connection.getURL();
                } else if (statusCode == HttpURLConnection.HTTP_NOT_FOUND) {
                    return RESULT_ERROR_NOT_FOUND;
                } else {
                    return RESULT_ERROR_OTHER_HTTP;
                }
            }
            if (connection == null)
                return RESULT_ERROR_OTHER_HTTP;
            final int fileLength = connection.getContentLength();
            final CountingInputStream counting = new CountingInputStream(connection.getInputStream());
            inputStream = counting;

            RepositoryLoader repositoryLoader;
            String contentType = connection.getContentType();
            String filename = connection.getURL().getFile().toLowerCase();
            if (contentType.startsWith("text/xml") || filename.endsWith(".xml"))
                repositoryLoader = new RepositoryXmlLoader(daoSession);
            else if (contentType.startsWith("application/json") || filename.endsWith(".json"))
                repositoryLoader = new RepositoryJsonLoader(daoSession);
            else
                return RESULT_ERROR_UNSUPPORTED_FORMAT;

            repositoryLoader.setLoaderEventListener(new RepositoryLoaderEventListener() {
                private long lastUpdateProcess;

                public boolean onLoadingCategory(Category category) {
                    return isCancelled();
                }

                @Override
                public boolean onEntryLoaded(Entry entry) {
                    if (System.currentTimeMillis() - lastUpdateProcess >= 100) {
                        int process = (int) counting.getCount() * 100 / fileLength;
                        publishProgress(process);
                        lastUpdateProcess = System.currentTimeMillis();
                    }
                    return isCancelled();
                }
            });
            repositoryLoader.loadToDatabase(repository,
                    new BufferedReader(new InputStreamReader(inputStream)));

            // Update source install state.
            SourceDao sourceDao = daoSession.getSourceDao();
            List<Source> sources = sourceDao.queryBuilder()
                    .where(SourceDao.Properties.CodeUrl.eq(repository.getUrl()),
                            SourceDao.Properties.Installed.eq(false))
                    .list();
            if (!sources.isEmpty()) {
                for (Source source : sources)
                    source.setInstalled(true);
                sourceDao.updateInTx(sources);
            }
        } catch (LoadingCancelException e) {
            return RESULT_CANCELLED;
        } catch (MalformedURLException e) {
            return RESULT_ERROR_MALFORMED_URL;
        } catch (IOException e) {
            return RESULT_ERROR_IO;
        } catch (XmlPullParserException e) {
            return RESULT_ERROR_XML_PARSER;
        } catch (Exception e) {
            e.printStackTrace();
            return RESULT_ERROR_UNKNOWN;
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

    @Override
    protected void onCancelled(Integer result) {
        super.onCancelled(result);
        if (result != DownloadAsyncTask.RESULT_CANCELLED)
            onPostExecute(result);
    }

    @Override
    protected void onPostExecute(Integer result) {
        super.onPostExecute(result);
        if (result == RESULT_SUCCESS) {
            Toast.makeText(context, R.string.download_success, Toast.LENGTH_SHORT).show();
            return;
        }
        String message = context.getString(R.string.download_fail);
        if (result == RESULT_ERROR_MALFORMED_URL)
            message = String.format(message, context.getString(R.string.download_malformed_url));
        else if (result == RESULT_ERROR_IO)
            message = String.format(message, context.getString(R.string.download_io_exception));
        else if (result == RESULT_ERROR_NOT_FOUND)
            message = String.format(message, context.getString(R.string.download_no_found));
        else if (result == RESULT_ERROR_XML_PARSER)
            message = String.format(message, context.getString(R.string.download_file_parser_error));
        else if (result == RESULT_ERROR_OTHER_HTTP)
            message = String.format(message, context.getString(R.string.download_http_error));
        else if (result == RESULT_ERROR_UNSUPPORTED_FORMAT)
            message = String.format(message, context.getString(R.string.download_unsupported));
        else
            message = String.format(message, context.getString(R.string.download_unknown_error));
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

}
