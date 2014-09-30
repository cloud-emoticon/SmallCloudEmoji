package org.sorz.lab.smallcloudemoji.adapters;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.RelativeLayout;
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

    private boolean showNote;
    private DaoSession daoSession;
    private List<Category> categories = new ArrayList<Category>();

    public MainExpandableAdapter(Context context, DaoSession daoSession) {
        super();
        this.context = context;
        this.daoSession = daoSession;
        inflater = LayoutInflater.from(context);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        showNote = preferences.getBoolean("show_note", true);

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
        Category category = getGroup(groupPosition);
        if (convertView == null)
            convertView = inflater.inflate(R.layout.item_group, parent, false);
        TextView text1 = (TextView) convertView.findViewById(android.R.id.text1);
        TextView text2 = (TextView) convertView.findViewById(android.R.id.text2);
        text1.setText(category.getName());
        String repositoryAlias;
        if (category instanceof FavoriteCategory) {
            repositoryAlias = "";
        } else {
            repositoryAlias = category.getRepository().getAlias();
            if (repositoryAlias.equals("Default"))
                repositoryAlias = "";
        }
        text2.setText(repositoryAlias);
        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
                             View convertView, ViewGroup parent) {
        if (convertView == null || convertView instanceof RelativeLayout)
            convertView = inflater.inflate(R.layout.item_child, parent, false);
        convertChildView(convertView,
                categories.get(groupPosition).getEntries().get(childPosition));
        return convertView;
    }

    @Override
    public boolean isChildSelectable(int i, int i2) {
        return true;
    }

    private void convertChildView(View itemView, Entry entry) {
        TextView text1 = (TextView) itemView.findViewById(R.id.text1);
        TextView text2 = (TextView) itemView.findViewById(R.id.text2);
        View star = itemView.findViewById(R.id.star);

        text1.setText(entry.getEmoticon());
        if (showNote && !entry.getDescription().isEmpty()) {
            text2.setText(entry.getDescription());
            text2.setVisibility(View.VISIBLE);
        } else {
            text2.setVisibility(View.GONE);
        }
        star.setVisibility(entry.getStar() ? View.VISIBLE : View.GONE);
        itemView.setTag(entry);
    }

}
