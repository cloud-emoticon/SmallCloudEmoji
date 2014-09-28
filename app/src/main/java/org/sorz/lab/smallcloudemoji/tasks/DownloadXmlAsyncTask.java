package org.sorz.lab.smallcloudemoji.tasks;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.widget.Toast;

import com.google.common.io.CountingInputStream;

import org.sorz.lab.smallcloudemoji.R;
import org.sorz.lab.smallcloudemoji.db.Category;
import org.sorz.lab.smallcloudemoji.db.DaoSession;
import org.sorz.lab.smallcloudemoji.db.Entry;
import org.sorz.lab.smallcloudemoji.db.Repository;
import org.sorz.lab.smallcloudemoji.parsers.LoadingCancelException;
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


/**
 * Download and save a XML file.
 */
public class DownloadXmlAsyncTask extends AsyncTask<Repository, Integer, Integer> {
    private Context context;
    private DaoSession daoSession;
    private ProgressDialog progressDialog;

    private static final int CANCELLED = -1;

    public DownloadXmlAsyncTask(Context context, DaoSession daoSession) {
        super();
        this.context = context;
        this.daoSession = daoSession;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        progressDialog = new ProgressDialog(context);
        progressDialog.setTitle(R.string.download_title);
        progressDialog.setMessage(context.getString(R.string.download_message));
        progressDialog.setMax(100);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setCancelable(true);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                cancel(true);
            }
        });
        progressDialog.show();
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
                    return R.string.download_no_found;
                } else {
                    return R.string.download_http_error;
                }
            }
            if (connection == null)
                return R.string.download_http_error;
            final int fileLength = connection.getContentLength();
            final CountingInputStream counting = new CountingInputStream(connection.getInputStream());
            inputStream = counting;

            RepositoryXmlLoader xmlLoader = new RepositoryXmlLoader(daoSession);
            xmlLoader.setLoaderEventListener(new RepositoryLoaderEventListener() {
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
            xmlLoader.loadToDatabase(repository,
                    new BufferedReader(new InputStreamReader(inputStream)));
        } catch (LoadingCancelException e) {
            return CANCELLED;
        } catch (MalformedURLException e) {
            return R.string.download_malformed_url;
        } catch (IOException e) {
            return R.string.download_io_exception;
        } catch (XmlPullParserException e) {
            return R.string.download_file_parser_error;
        } catch (Exception e) {
            e.printStackTrace();
            return R.string.download_unknown_error;
        } finally {
            try {
                if (inputStream != null)
                    inputStream.close();
            } catch (IOException e) {
                // Ignore it
            }
            if (connection != null)
                connection.disconnect();
        }
        return R.string.download_success;
    }

    @Override
    protected void onProgressUpdate(Integer... value) {
        super.onProgressUpdate(value);
        progressDialog.setIndeterminate(false);
        progressDialog.setProgress(value[0]);

    }

    @Override
    protected void onPostExecute(Integer result) {
        progressDialog.dismiss();
        if (R.string.download_success == result) {
                Toast.makeText(context, R.string.download_success, Toast.LENGTH_SHORT).show();
        } else {
            String message = String.format(
                    context.getString(R.string.download_fail),
                    context.getString(result));
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onCancelled(Integer result) {
        if (result != CANCELLED) {
            onPostExecute(result);
        } else {
            progressDialog.dismiss();
        }
    }
}
