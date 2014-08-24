package org.sorz.lab.smallcloudemoji;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Download and save a XML file.
 */
class DownloadXmlAsyncTask extends AsyncTask<String, Integer, Integer> {
    private Context context;
    private ProgressDialog progressDialog;
    private File targetFile;
    private File temporaryFile;

    private static final int CANCELLED = -1;

    public DownloadXmlAsyncTask(Context context) {
        super();
        this.context = context;
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
    protected Integer doInBackground(String... params) {
        targetFile = new File(context.getFilesDir(), params[1]);
        temporaryFile = new File(context.getFilesDir(), params[1] + ".tmp");

        InputStream inputStream = null;
        OutputStream outputStream = null;
        HttpURLConnection connection = null;

        try {
            URL url = new URL(params[0]);
            for (int i=0; i<10; ++i) {  // Limit redirection (between HTTP and HTTPS) < 10 times.
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
            int fileLength = connection.getContentLength();
            int totalReceived = 0;
            inputStream = connection.getInputStream();
            outputStream = new FileOutputStream(temporaryFile);

            byte buffer[] = new byte[4096];
            int received;
            while ((received = inputStream.read(buffer)) != -1) {
                totalReceived += received;
                if (fileLength > 0)
                    publishProgress((int) 100.0 * totalReceived / fileLength);
                outputStream.write(buffer, 0, received);

                if (isCancelled())
                    return CANCELLED;
            }

        } catch (MalformedURLException e) {
            return R.string.download_malformed_url;
        } catch (IOException e) {
            return R.string.download_io_exception;
        } finally {
            try {
                if (inputStream != null)
                    inputStream.close();
                if (outputStream != null)
                    outputStream.close();
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
            targetFile.delete();
            if (! temporaryFile.renameTo(targetFile.getAbsoluteFile()))
                Toast.makeText(context, R.string.download_file_operation_error,
                        Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(context, R.string.download_success, Toast.LENGTH_SHORT).show();
        } else {
            String message = String.format(
                    context.getString(R.string.download_fail),
                    context.getString(result));
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            temporaryFile.delete();
        }
    }

    @Override
    protected void onCancelled(Integer result) {
        if (result != CANCELLED) {
            onPostExecute(result);
        } else {
            progressDialog.dismiss();
            temporaryFile.delete();
        }
    }
}
