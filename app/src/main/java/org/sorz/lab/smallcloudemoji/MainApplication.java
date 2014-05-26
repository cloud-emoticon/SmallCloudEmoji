package org.sorz.lab.smallcloudemoji;


import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AdapterView;
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
    private List<EmoticonGroup> emoticonGroups;
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
        adapter = new MainExpandableAdapter(this, emoticonGroups);
        listView.setAdapter(adapter);
        listView.expandGroup(0, false);
        listView.expandGroup(adapter.getGroupCount() - 1, false);

        listView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                                        int childPosition, long id) {
                Object emoticon = v.getTag();
                if (emoticon != null) {
                    ClipboardManager clipboard =
                            (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    ClipData clipData = ClipData.newPlainText("emoticon", emoticon.toString());
                    clipboard.setPrimaryClip(clipData);
                    Toast.makeText(MainApplication.this, R.string.toast_copied,
                            Toast.LENGTH_SHORT).show();
                    historyDataSource.updateHistory((Emoticon) emoticon);

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

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Object emojiObject = view.getTag();
                if (emojiObject == null)
                    return false;
                Emoticon emoticon = (Emoticon) emojiObject;
                if (emoticon.hasStar())
                    historyDataSource.unsetStar(emoticon);
                else
                    historyDataSource.setStar(emoticon);
                updateFavoriteGroup();
                adapter.notifyDataSetChanged();
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
        if (emoticonGroups == null)
            emoticonGroups = new ArrayList<EmoticonGroup>();

        updateFavoriteGroup();
        updateCategoryGroups();

        // If all is empty, auto sync source.
        if (emoticonGroups.size() == 1 && emoticonGroups.get(0).size() == 0) {

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
     * Read favorite group into emoticonGroups.
     * Create or update.
     */
    private void updateFavoriteGroup() {
        int count = Integer.parseInt(sharedPreferences.getString("favorites_count", "8"));
        EmoticonGroup favoriteGroup = new EmoticonGroup(
                getResources().getString(R.string.list_title_favorite),
                Arrays.asList(historyDataSource.getFavorites(count)));
        if (emoticonGroups.size() == 0)
            emoticonGroups.add(favoriteGroup);
        else
            emoticonGroups.set(0, favoriteGroup);
    }

    /**
     * Read category groups into emoticonGroups.
     * Create or update.
     */
    private void updateCategoryGroups() {
        List<EmoticonGroup> categoryGroups;
        try {
            categoryGroups = XmlSourceParser.parserAll(this, "emojis.xml");
        } catch (IOException e) {
            e.printStackTrace();
            return;
        } catch (XmlPullParserException e) {
            e.printStackTrace();
            return;
        }
        if (emoticonGroups.size() <= 1) {
            // Only a favorite group in emoticonGroups.
            emoticonGroups.addAll(categoryGroups);
        } else {
            // Remove all old category groups.
            int size = emoticonGroups.size();
            for (int i=1; i < size; ++i)
                emoticonGroups.remove(i);
            // Add new groups.
            emoticonGroups.addAll(categoryGroups);
        }
    }

}
