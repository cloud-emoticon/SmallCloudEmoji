package org.sorz.lab.smallcloudemoji;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import java.util.List;


/**
 * The adapter of ExpandableListView on MainApplication.
 */
public class MainExpandableAdapter extends BaseExpandableListAdapter {
    final private Context context;
    final private LayoutInflater inflater;
    private List<EmojiGroup> emojiGroups;


    public MainExpandableAdapter(Context context, List<EmojiGroup> emojiGroups) {
        super();
        this.context = context;
        this.emojiGroups = emojiGroups;

        inflater = LayoutInflater.from(context);
    }

    @Override
    public int getGroupCount() {
        return emojiGroups.size() + 1;  // Add one item, Settings.
    }

    @Override
    public int getChildrenCount(int i) {
        if (i == emojiGroups.size())
            return 1;
        return emojiGroups.get(i).size();
    }

    @Override
    public EmojiGroup getGroup(int groupPosition) {
        if (groupPosition >= emojiGroups.size())
            return null;
        return emojiGroups.get(groupPosition);
    }

    @Override
    public Emoji getChild(int groupPosition, int childPosition) {
        List<Emoji> group = getGroup(groupPosition);
        if (group == null)
            return null;
        else
            return group.get(childPosition);
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
        if (groupPosition == emojiGroups.size())
            title = context.getResources().getString(R.string.list_title_options);
        else
            title = emojiGroups.get(groupPosition).toString();
        if (convertView != null && convertView instanceof TextView) {
            ((TextView) convertView).setText(title);
            return convertView;
        } else {
            return createGroupView(title);
        }

    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
                             View convertView, ViewGroup parent) {
        String title;
        if (groupPosition == emojiGroups.size())
            title = context.getResources().getString(R.string.list_title_setting);
        else
            title = getChild(groupPosition, childPosition).toString();
        if (convertView != null && convertView instanceof TextView) {
            ((TextView) convertView).setText(title);
            return convertView;
        } else {
            return createChildView(title);
        }
    }

    @Override
    public boolean isChildSelectable(int i, int i2) {
        return true;
    }

    private View createGroupView(String title) {
        TextView textView = (TextView) inflater.inflate(R.layout.item_group, null);
        textView.setText(title);
        return textView;
    }

    private View createChildView(String title) {
        TextView textView = (TextView) inflater.inflate(R.layout.item_child, null);
        textView.setText(title);
        return textView;
    }

}
