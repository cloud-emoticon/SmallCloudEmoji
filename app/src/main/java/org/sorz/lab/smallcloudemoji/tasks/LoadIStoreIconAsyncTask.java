package org.sorz.lab.smallcloudemoji.tasks;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.LruCache;

import org.sorz.lab.smallcloudemoji.adapters.StoreSourceAdapter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

/**
 * Set icon of source on store list from internet or local cache.
 */
public class LoadIStoreIconAsyncTask extends AsyncTask<String, Void, Bitmap> {
    final private static int MAX_FILE_LENGTH = 512 * 1024;  // 512 KiB
    final private static int MAX_CACHE_PERIOD = 3600 * 24 * 30;  // 30 days

    final Context context;
    final int position;
    final StoreSourceAdapter.ViewHolder viewHolder;
    final LruCache<String, Bitmap> memoryCache;

    public LoadIStoreIconAsyncTask(Context context,
                                   StoreSourceAdapter.ViewHolder viewHolder,
                                   LruCache<String, Bitmap> memoryCache) {
        this.context = context;
        this.viewHolder = viewHolder;
        this.memoryCache = memoryCache;
        position = viewHolder.position;
    }

    @Override
    protected Bitmap doInBackground(String... params) {
        String iconUrl = params[0];

        // Try to retrieve from memory cache.
        Bitmap icon = memoryCache.get(iconUrl);
        if (icon != null)
            return icon;

        // Try to retrieve from file cache.
        File cacheFile = new File(context.getCacheDir(), "icon-" + iconUrl.hashCode() + ".webp");
        long currentTime = new Date().getTime();
        if (cacheFile.exists()) {
            if (currentTime - cacheFile.lastModified() < MAX_CACHE_PERIOD) {
                icon = BitmapFactory.decodeFile(cacheFile.getPath());
                memoryCache.put(iconUrl, icon);
                return icon;
            } else {
                // Cache expired.
                //noinspection ResultOfMethodCallIgnored
                cacheFile.delete();
            }
        }

        // Download from URL.
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            URL url = new URL(iconUrl);
            connection = (HttpURLConnection) url.openConnection();
            if (connection.getContentLength() > MAX_FILE_LENGTH) {
                return null;
            }
            inputStream = new BufferedInputStream(connection.getInputStream());
            icon = BitmapFactory.decodeStream(inputStream);
            memoryCache.put(iconUrl, icon);
            outputStream = new BufferedOutputStream(new FileOutputStream(cacheFile));
            icon.compress(Bitmap.CompressFormat.WEBP, 80, outputStream);
            return icon;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
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
        return null;
    }

    @Override
    protected void onPostExecute(Bitmap result) {
        if (viewHolder.position == position) {
            viewHolder.icon.setImageBitmap(result);
        }
    }
}
