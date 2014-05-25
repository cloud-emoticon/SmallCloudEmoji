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

import java.util.List;


/**
 * The adapter of ExpandableListView on MainApplication.
 */
public class MainExpandableAdapter extends BaseExpandableListAdapter {
    final private Context context;
    final private LayoutInflater inflater;

    private List<EmojiGroup> emojiGroups;
    private boolean showNote;


    public MainExpandableAdapter(Context context, List<EmojiGroup> emojiGroups) {
        super();
        this.context = context;
        this.emojiGroups = emojiGroups;
        inflater = LayoutInflater.from(context);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        showNote = preferences.getBoolean("show_note", true);
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
            return createGroupView(parent, title);
        }
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
                             View convertView, ViewGroup parent) {
        String title;
        String note = "";
        if (groupPosition == emojiGroups.size()) {
            title = context.getResources().getString(R.string.list_title_setting);
        } else {
            Emoji emoji = getChild(groupPosition, childPosition);
            title = emoji.toString();
            if (showNote)
                note = emoji.getNote();
        }
        if (convertView != null && convertView instanceof RelativeLayout)
            return convertChildView(convertView, title, note);
        else
            return createChildView(parent, title, note);
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

    private View createChildView(ViewGroup parent, String line1, String line2) {
        View itemView = inflater.inflate(R.layout.item_child, parent, false);
        TextView text1 = (TextView) itemView.findViewById(R.id.text1);
        TextView text2 = (TextView) itemView.findViewById(R.id.text2);
        text1.setText(line1);
        if (! line2.isEmpty()) {
            text2.setText(line2);
            text2.setVisibility(View.VISIBLE);
        }
        return itemView;
    }

    private View convertChildView(View itemView, String line1, String line2) {
        TextView text1 = (TextView) itemView.findViewById(R.id.text1);
        TextView text2 = (TextView) itemView.findViewById(R.id.text2);
        text1.setText(line1);
        text2.setText(line2);

        if (line2.isEmpty())
            text2.setVisibility(View.GONE);
        else
            text2.setVisibility(View.VISIBLE);
        return itemView;
    }

}
