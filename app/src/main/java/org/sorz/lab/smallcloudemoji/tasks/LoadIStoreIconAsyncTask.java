package org.sorz.lab.smallcloudemoji.tasks;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

import org.sorz.lab.smallcloudemoji.adapters.StoreSourceAdapter;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Set icon of source on store list from internet or local cache.
 */
public class LoadIStoreIconAsyncTask extends AsyncTask<String, Void, Bitmap> {
    final static int MAX_FILE_LENGTH = 512 * 1024;  // 512 KiB

    final int position;
    StoreSourceAdapter.ViewHolder viewHolder;

    public LoadIStoreIconAsyncTask(StoreSourceAdapter.ViewHolder viewHolder) {
        this.viewHolder = viewHolder;
        position = viewHolder.position;
    }

    @Override
    protected Bitmap doInBackground(String... params) {
        HttpURLConnection connection = null;
        InputStream inputStream = null;

        try {
            URL url = new URL(params[0]);
            connection = (HttpURLConnection) url.openConnection();
            if (connection.getContentLength() > MAX_FILE_LENGTH) {
                return null;
            }
            inputStream = connection.getInputStream();
            return BitmapFactory.decodeStream(inputStream);

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
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
        return null;
    }

    @Override
    protected void onPostExecute(Bitmap result) {
        if (viewHolder.position == position) {
            viewHolder.icon.setImageBitmap(result);
        }
    }
}
