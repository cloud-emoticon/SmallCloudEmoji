package org.sorz.lab.smallcloudemoji.adapters;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import org.sorz.lab.smallcloudemoji.R;
import org.sorz.lab.smallcloudemoji.db.Category;
import org.sorz.lab.smallcloudemoji.db.DaoSession;
import org.sorz.lab.smallcloudemoji.db.Entry;
import org.sorz.lab.smallcloudemoji.db.FavoriteCategory;
import org.sorz.lab.smallcloudemoji.db.Repository;
import org.sorz.lab.smallcloudemoji.db.RepositoryDao;

import java.util.ArrayList;
import java.util.List;


/**
 * The adapter of ExpandableListView on MainApplication.
 */
public class MainExpandableAdapter extends BaseExpandableListAdapter {
    final private Context context;
    final private LayoutInflater inflater;

    private final boolean showNote;
    private final boolean showSourceName;
    private final DaoSession daoSession;
    private final List<Category> categories = new ArrayList<Category>();

    private static class GroupViewHolder {
        TextView category;
        TextView repository;
    }

    private static class ChildViewHolder {
        TextView emoticon;
        TextView description;
        View star;
    }

    public MainExpandableAdapter(Context context, DaoSession daoSession) {
        super();
        this.context = context;
        this.daoSession = daoSession;
        inflater = LayoutInflater.from(context);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        showNote = preferences.getBoolean("show_note", true);
        showSourceName = preferences.getBoolean("show_source_name", true);

        reloadCategories();
    }

    private void reloadCategories() {
        categories.clear();
        // Load favorites.
        categories.add(new FavoriteCategory(context, daoSession));
        // Load all categories.
        RepositoryDao repositoryDao = daoSession.getRepositoryDao();
        List<Repository> repositories = repositoryDao.queryBuilder()
                .where(RepositoryDao.Properties.Hidden.eq(false))
                .orderAsc(RepositoryDao.Properties.Order)
                .list();
        for (Repository repository : repositories)
            categories.addAll(repository.getCategories());
    }

    public void notifyDataSetChanged(boolean reloadAllCategories) {
        if (reloadAllCategories)
            reloadCategories();
        notifyDataSetChanged();
    }

    @Override
    public void notifyDataSetChanged() {
        // Reset favorites.
        categories.get(0).resetEntries();
        super.notifyDataSetChanged();
    }

    @Override
    public int getGroupCount() {
        return categories.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        if (groupPosition == categories.size())  // == Settings group
            return 1;
        else
            return categories.get(groupPosition).getEntries().size();
    }

    @Override
    public Category getGroup(int groupPosition) {
        return categories.get(groupPosition);
    }

    @Override
    public Entry getChild(int groupPosition, int childPosition) {
        return categories.get(groupPosition).getEntries().get(childPosition);
    }

    @Override
    public long getGroupId(int i) {
        return 0;
    }

    @Override
    public long getChildId(int i, int i2) {
        return 0;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
                             ViewGroup parent) {
        GroupViewHolder viewHolder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_group, parent, false);
            viewHolder = new GroupViewHolder();
            viewHolder.category = (TextView) convertView.findViewById(android.R.id.text1);
            viewHolder.repository = (TextView) convertView.findViewById(android.R.id.text2);
            convertView.setTag(R.id.view_holder, viewHolder);
        } else {
            viewHolder = (GroupViewHolder) convertView.getTag(R.id.view_holder);
        }
        Category category = getGroup(groupPosition);

        viewHolder.category.setText(category.getName());
        String repositoryAlias;
        if (category instanceof FavoriteCategory || !showSourceName) {
            repositoryAlias = "";
        } else {
            repositoryAlias = category.getRepository().getAlias();
            if (repositoryAlias.equals("Default"))
                repositoryAlias = "";
        }
        viewHolder.repository.setText(repositoryAlias);
        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
                             View convertView, ViewGroup parent) {
        ChildViewHolder viewHolder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_child, parent, false);
            viewHolder = new ChildViewHolder();
            viewHolder.emoticon = (TextView) convertView.findViewById(R.id.text1);
            viewHolder.description = (TextView) convertView.findViewById(R.id.text2);
            viewHolder.star = convertView.findViewById(R.id.star);
            convertView.setTag(R.id.view_holder, viewHolder);
        } else {
            viewHolder = (ChildViewHolder) convertView.getTag(R.id.view_holder);
        }
        Entry entry = getChild(groupPosition, childPosition);
        convertView.setTag(R.id.entry, entry);
        viewHolder.emoticon.setText(entry.getEmoticon());
        if (showNote && !entry.getDescription().isEmpty()) {
            viewHolder.description.setText(entry.getDescription());
            viewHolder.description.setVisibility(View.VISIBLE);
        } else {
            viewHolder.description.setVisibility(View.GONE);
        }
        viewHolder.star.setVisibility(entry.getStar() ? View.VISIBLE : View.GONE);
        return convertView;
    }

    @Override
    public boolean isChildSelectable(int i, int i2) {
        return true;
    }


}
