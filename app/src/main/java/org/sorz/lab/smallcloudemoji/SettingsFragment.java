package org.sorz.lab.smallcloudemoji;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.widget.Toast;

/**
 * Show all settings. Shown inside SettingsActivity.
 * Include source sync (downloading XML).
 */
public class SettingsFragment extends PreferenceFragment {
    private Context context;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getActivity();
        addPreferencesFromResource(R.xml.preferences);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        // Favorites number: show value as summary.
        final EditTextPreference favoritesCountPreference =
                (EditTextPreference) findPreference("favorites_count");
        final String favoritesCountPreferenceSummary =
                getResources().getString(R.string.pref_favorites_count_summary);
        favoritesCountPreference.setSummary(String.format(favoritesCountPreferenceSummary,
                favoritesCountPreference.getText()));


        // Favorites clean
        Preference favoritesCleanPreference = findPreference("favorites_clean");
        favoritesCleanPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                HistoryDataSource historyDataSource = new HistoryDataSource(context);
                historyDataSource.cleanHistory();
                Toast.makeText(context, R.string.pref_favorites_clean_done,
                        Toast.LENGTH_SHORT).show();
                return false;
            }
        });

        // Source address preference: show value as summary.
        final EditTextPreference sourceUrlPreference =
                (EditTextPreference) findPreference("source_address");
        sourceUrlPreference.setSummary(sourceUrlPreference.getText());

        // Update summaries
        preferences.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (key.equals("source_address"))
                    sourceUrlPreference.setSummary(sourceUrlPreference.getText());
                else if (key.equals("favorites_count"))
                    favoritesCountPreference.setSummary(
                            String.format(favoritesCountPreferenceSummary,
                                    favoritesCountPreference.getText()));
            }
        });

        // Restore default source
        Preference restoreSourcePreference = findPreference("restore_source");
        restoreSourcePreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                String defaultSourceUrl = context.getString(R.string.pref_source_address_default);
                sourceUrlPreference.setText(defaultSourceUrl);
                Toast.makeText(context, R.string.pref_restore_source_done,
                        Toast.LENGTH_SHORT).show();
                return true;
            }
        });

        // Sync source
        Preference syncSourcePreference = findPreference("sync_source");
        syncSourcePreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new DownloadXmlAsyncTask(context).execute(sourceUrlPreference.getText(),
                        "emojis.xml");
                return false;
            }
        });

        // Version
        try {
            PackageManager packageManager = context.getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
            String version = String.format(getResources().getString(R.string.pref_version_title),
                    packageInfo.versionName, packageInfo.versionCode);
            findPreference("version").setTitle(version);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

    }

}
