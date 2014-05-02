package org.sorz.lab.smallcloudemoji;


import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.Toast;

import com.sony.smallapp.SmallAppWindow;
import com.sony.smallapp.SmallApplication;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Small application for Sony.
 */
public class MainApplication extends SmallApplication {
    private SharedPreferences sharedPreferences;
    private HistoryDataSource historyDataSource;

    @Override
    public void onCreate() {
        super.onCreate();
        setContentView(R.layout.application_main);
        setTitle(R.string.app_name);

        SmallAppWindow.Attributes attr = getWindow().getAttributes();
        attr.minHeight = 350;
        attr.minWidth = 350;
        attr.width = 500;
        attr.height = 650;
        getWindow().setAttributes(attr);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        historyDataSource = new HistoryDataSource(this);

        ExpandableListView listView =
                (ExpandableListView) findViewById(R.id.expandableListView);
        final ExpandableListAdapter adapter = getListAdapterWithData();
        listView.setAdapter(adapter);
        listView.expandGroup(0, false);
        listView.expandGroup(adapter.getGroupCount() - 1, false);

        listView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                                        int childPosition, long id) {
                Object emoji = adapter.getChild(groupPosition, childPosition);
                if (emoji != null) {
                    ClipboardManager clipboard =
                            (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    ClipData clipData = ClipData.newPlainText("emoji", emoji.toString());
                    clipboard.setPrimaryClip(clipData);
                    Toast.makeText(MainApplication.this, R.string.toast_copied,
                            Toast.LENGTH_SHORT).show();
                    historyDataSource.updateHistory((Emoji) emoji);
                    if (sharedPreferences.getBoolean("auto_close", true))
                        finish();
                } else {
                    Intent intent = new Intent(MainApplication.this, SettingsActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                }
                return true;
            }
        });

    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    private ExpandableListAdapter getListAdapterWithData() {
        List<EmojiGroup> emojiGroups = new ArrayList<EmojiGroup>();

        // Add favorite group:
        int count = Integer.parseInt(sharedPreferences.getString("favorites_count", "8"));
        EmojiGroup favoriteGroup = new EmojiGroup(
                getResources().getString(R.string.list_title_favorite),
                Arrays.asList(historyDataSource.getFavorites(count)));
        emojiGroups.add(favoriteGroup);

        // Add all emojis:
        try {
            List<EmojiGroup> xmlGroups = XmlSourceParser.parserAll(this, "emojis.xml");
            emojiGroups.addAll(xmlGroups);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        }

        return new MainExpandableAdapter(this, emojiGroups);
    }

}
