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

    private List<EmoticonGroup> emoticonGroups;
    private boolean showNote;


    public MainExpandableAdapter(Context context, List<EmoticonGroup> emoticonGroups) {
        super();
        this.context = context;
        this.emoticonGroups = emoticonGroups;
        inflater = LayoutInflater.from(context);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        showNote = preferences.getBoolean("show_note", true);
    }

    @Override
    public int getGroupCount() {
        return emoticonGroups.size() + 1;  // Add one item, Settings.
    }

    @Override
    public int getChildrenCount(int i) {
        if (i == emoticonGroups.size())
            return 1;
        return emoticonGroups.get(i).size();
    }

    @Override
    public EmoticonGroup getGroup(int groupPosition) {
        if (groupPosition >= emoticonGroups.size())
            return null;
        return emoticonGroups.get(groupPosition);
    }

    @Override
    public Emoticon getChild(int groupPosition, int childPosition) {
        List<Emoticon> group = getGroup(groupPosition);
        if (group == null)
            return null;
        Emoticon emoticon = group.get(childPosition);

        // Due to the star which is only tagged in favorites group.
        // Check and use it if which is also in favorites group.
        if (groupPosition != 0) { // != favorites group
            EmoticonGroup favorites = getGroup(0);
            int index = favorites.indexOf(emoticon);
            if (index != -1)
                emoticon = favorites.get(index);
        }
        return emoticon;
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
        if (groupPosition == emoticonGroups.size())
            title = context.getResources().getString(R.string.list_title_options);
        else
            title = emoticonGroups.get(groupPosition).toString();
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

        if (groupPosition == emoticonGroups.size()) {
            String title = context.getResources().getString(R.string.list_title_setting);
            convertSettingsChildView(convertView, title);
        } else {
            Emoticon emoticon = getChild(groupPosition, childPosition);
            convertEmojiChildView(convertView, emoticon);
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

    private void convertEmojiChildView(View itemView, Emoticon emoticon) {
        TextView text1 = (TextView) itemView.findViewById(R.id.text1);
        TextView text2 = (TextView) itemView.findViewById(R.id.text2);
        View star = itemView.findViewById(R.id.star);

        text1.setText(emoticon.toString());
        if (showNote && ! emoticon.getNote().isEmpty()) {
            text2.setText(emoticon.getNote());
            text2.setVisibility(View.VISIBLE);
        } else {
            text2.setVisibility(View.GONE);
        }
        star.setVisibility(emoticon.hasStar() ? View.VISIBLE : View.GONE);
        itemView.setTag(emoticon);
    }

}
