package org.sorz.lab.smallcloudemoji.interfaces;

import android.graphics.Bitmap;
import android.util.LruCache;


public interface IconCacheHolder {
    public LruCache<String, Bitmap> getIconCache();

}
