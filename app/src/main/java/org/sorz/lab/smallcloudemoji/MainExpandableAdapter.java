package org.sorz.lab.smallcloudemoji;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

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
    private List<Category> categories = new ArrayList<Category>();

    public MainExpandableAdapter(Context context, DaoSession daoSession) {
        super();
        this.context = context;
        inflater = LayoutInflater.from(context);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        showNote = preferences.getBoolean("show_note", true);

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

    @Override
    public void notifyDataSetChanged() {
        // Reset favorites.
        categories.get(0).resetEntries();
        super.notifyDataSetChanged();
    }

    @Override
    public int getGroupCount() {
        System.out.println("getGroupCount");
        return categories.size() + 1;  // Add one item, Settings.
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        System.out.println("getChildrenCount " + groupPosition);
        if (groupPosition == categories.size())  // == Settings group
            return 1;
        else
            return categories.get(groupPosition).getEntries().size();
    }

    @Override
    public Category getGroup(int groupPosition) {
        System.out.println("getGroup " + groupPosition);
        return categories.get(groupPosition);
    }

    @Override
    public Entry getChild(int groupPosition, int childPosition) {
        System.out.println("getChild " + groupPosition + "" + childPosition);
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
        String title;
        if (groupPosition == categories.size()) // == Settings
            title = context.getResources().getString(R.string.list_title_options);
        else if (groupPosition == 0)  // == Favorites
            title = context.getResources().getString(R.string.list_title_favorite);
        else
            title = getGroup(groupPosition).getName();
        if (convertView != null && convertView instanceof TextView) {
            ((TextView) convertView).setText(title);
            return convertView;
        } else {
            return createGroupView(parent, title);
        }
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
                             View convertView, ViewGroup parent) {
        if (convertView == null || convertView instanceof RelativeLayout)
            convertView = inflater.inflate(R.layout.item_child, parent, false);

        if (groupPosition == categories.size()) {  // == Settings
            String title = context.getResources().getString(R.string.list_title_setting);
            convertSettingsChildView(convertView, title);
        } else {
            convertEmojiChildView(convertView,
                    categories.get(groupPosition).getEntries().get(childPosition));
        }
        return convertView;
    }

    @Override
    public boolean isChildSelectable(int i, int i2) {
        return true;
    }

    private View createGroupView(ViewGroup parent, String title) {
        TextView textView = (TextView) inflater.inflate(R.layout.item_group, parent, false);
        textView.setText(title);
        return textView;
    }

    private void convertSettingsChildView(View itemView, String title) {
        TextView text1 = (TextView) itemView.findViewById(R.id.text1);
        TextView text2 = (TextView) itemView.findViewById(R.id.text2);
        View star = itemView.findViewById(R.id.star);
        text1.setText(title);
        text2.setVisibility(View.GONE);
        star.setVisibility(View.GONE);
    }

    private void convertEmojiChildView(View itemView, Entry entry) {
        TextView text1 = (TextView) itemView.findViewById(R.id.text1);
        TextView text2 = (TextView) itemView.findViewById(R.id.text2);
        View star = itemView.findViewById(R.id.star);

        text1.setText(entry.getEmoticon());
        if (showNote && ! entry.getDescription().isEmpty()) {
            text2.setText(entry.getDescription());
            text2.setVisibility(View.VISIBLE);
        } else {
            text2.setVisibility(View.GONE);
        }
        star.setVisibility(entry.getStar() ? View.VISIBLE : View.GONE);
        itemView.setTag(entry);
    }

}
