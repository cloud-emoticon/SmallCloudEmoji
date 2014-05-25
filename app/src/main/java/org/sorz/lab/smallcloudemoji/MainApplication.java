package org.sorz.lab.smallcloudemoji;


import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.BaseExpandableListAdapter;
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
    private List<EmojiGroup> emojiGroups;
    private BaseExpandableListAdapter adapter;

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

        loadAllGroupsOrDownload();
        final ExpandableListView listView =
                (ExpandableListView) findViewById(R.id.expandableListView);
        adapter = new MainExpandableAdapter(this, emojiGroups);
        listView.setAdapter(adapter);
        listView.expandGroup(0, false);
        listView.expandGroup(adapter.getGroupCount() - 1, false);

        listView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                                        int childPosition, long id) {
                Object emoji = v.getTag();
                if (emoji != null) {
                    ClipboardManager clipboard =
                            (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    ClipData clipData = ClipData.newPlainText("emoji", emoji.toString());
                    clipboard.setPrimaryClip(clipData);
                    Toast.makeText(MainApplication.this, R.string.toast_copied,
                            Toast.LENGTH_SHORT).show();
                    historyDataSource.updateHistory((Emoji) emoji);

                    String action = sharedPreferences.getString("action_after_copied", "MINIMIZE");
                    if (action.equals("MINIMIZE"))
                        getWindow().setWindowState(SmallAppWindow.WindowState.MINIMIZED);
                    else if (action.equals("CLOSE"))
                        finish();
                    if (! action.equals("CLOSE")) {
                        updateFavoriteGroup();
                        adapter.notifyDataSetChanged();
                    }
                } else {  // is the settings item.
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


    private void loadAllGroupsOrDownload() {
        if (emojiGroups == null)
            emojiGroups = new ArrayList<EmojiGroup>();

        updateFavoriteGroup();
        updateCategoryGroups();

        // If all is empty, auto sync source.
        if (emojiGroups.size() == 1 && emojiGroups.get(0).size() == 0) {

            // Minimize the windows rather than mask the process dialog.
            getWindow().setWindowState(SmallAppWindow.WindowState.MINIMIZED);

            String sourceUrl = sharedPreferences.getString("sync_source",
                    getResources().getString(R.string.pref_source_address_default));
            new DownloadXmlAsyncTask(this) {
                @Override
                protected void onPostExecute(Integer result) {
                    super.onPostExecute(result);
                    updateCategoryGroups();
                    adapter.notifyDataSetChanged();
                    getWindow().setWindowState(SmallAppWindow.WindowState.NORMAL);
                }
            }.execute(sourceUrl, "emojis.xml");
        }
    }

    /**
     * Read favorite group into emojiGroups.
     * Create or update.
     */
    private void updateFavoriteGroup() {
        int count = Integer.parseInt(sharedPreferences.getString("favorites_count", "8"));
        EmojiGroup favoriteGroup = new EmojiGroup(
                getResources().getString(R.string.list_title_favorite),
                Arrays.asList(historyDataSource.getFavorites(count)));
        if (emojiGroups.size() == 0)
            emojiGroups.add(favoriteGroup);
        else
            emojiGroups.set(0, favoriteGroup);
    }

    /**
     * Read category groups into emojiGroups.
     * Create or update.
     */
    private void updateCategoryGroups() {
        List<EmojiGroup> categoryGroups;
        try {
            categoryGroups = XmlSourceParser.parserAll(this, "emojis.xml");
        } catch (IOException e) {
            e.printStackTrace();
            return;
        } catch (XmlPullParserException e) {
            e.printStackTrace();
            return;
        }
        if (emojiGroups.size() <= 1) {
            // Only a favorite group in emojiGroups.
            emojiGroups.addAll(categoryGroups);
        } else {
            // Remove all old category groups.
            int size = emojiGroups.size();
            for (int i=1; i < size; ++i)
                emojiGroups.remove(i);
            // Add new groups.
            emojiGroups.addAll(categoryGroups);
        }
    }

}
