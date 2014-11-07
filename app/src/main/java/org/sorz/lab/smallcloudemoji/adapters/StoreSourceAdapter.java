package org.sorz.lab.smallcloudemoji.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.sorz.lab.smallcloudemoji.R;
import org.sorz.lab.smallcloudemoji.db.Source;
import org.sorz.lab.smallcloudemoji.db.SourceDao;
import org.sorz.lab.smallcloudemoji.tasks.LoadIStoreIconAsyncTask;

import java.util.List;

/**
 * Get sources from database and generate a view for each them.
 */
public class StoreSourceAdapter extends BaseAdapter {
    final private Context context;
    final private LayoutInflater inflater;
    private SourceDao sourceDao;
    private List<Source> sources;

    private LruCache<String, Bitmap> iconCache;

    public static class ViewHolder {
        public int position;
        public ImageView icon;
        TextView name;
        TextView introduction;
        TextView creator;
        TextView installed;
    }

    public StoreSourceAdapter(Context context, SourceDao sourceDao) {
        super();
        this.context = context;
        this.sourceDao = sourceDao;
        inflater = LayoutInflater.from(context);

        // Get max available VM memory in KiB.
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        // Use 1/16th of the available memory for icon cache.
        // ~70 icons in 120 x 120px for 64MiB available memory.
        final int cacheSize = maxMemory / 16;

        iconCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                // The cache size will be measured in kilobytes rather than
                // number of items.
                return bitmap.getByteCount() / 1024;
            }
        };

        loadSources();
    }

    private void loadSources() {
        sources = sourceDao.queryBuilder().list();
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public boolean isEnabled(int position) {
        return true;
    }


    @Override
    public int getCount() {
        return sources.size();
    }

    @Override
    public Source getItem(int position) {
        return sources.get(position);
    }

    @Override
    public long getItemId(int position) {
        return sources.get(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_store_source, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.name = (TextView) convertView.findViewById(R.id.source_name);
            viewHolder.introduction = (TextView) convertView.findViewById(R.id.source_introduction);
            viewHolder.icon = (ImageView) convertView.findViewById(R.id.source_icon);
            viewHolder.creator = (TextView) convertView.findViewById(R.id.source_creator);
            viewHolder.installed = (TextView) convertView.findViewById(R.id.source_installed);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        Source source = getItem(position);
        viewHolder.position = position;
        viewHolder.name.setText(source.getName());
        viewHolder.introduction.setText(source.getIntroduction());
        viewHolder.creator.setText(source.getCreator());
        viewHolder.installed.setVisibility(source.getInstalled() ? View.VISIBLE : View.GONE);
        viewHolder.icon.setImageResource(R.drawable.ic_empty_avatar);
        new LoadIStoreIconAsyncTask(context, viewHolder, iconCache).execute(source.getIconUrl());
        return convertView;
    }

    @Override
    public boolean isEmpty() {
        return sources.isEmpty();
    }

    @Override
    public void notifyDataSetChanged() {
        loadSources();
        iconCache.evictAll();
        super.notifyDataSetChanged();
    }
}
