package org.sorz.lab.smallcloudemoji.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.widget.Toast;

import org.sorz.lab.smallcloudemoji.R;
import org.sorz.lab.smallcloudemoji.db.DaoSession;
import org.sorz.lab.smallcloudemoji.db.DatabaseHelper;
import org.sorz.lab.smallcloudemoji.db.Entry;
import org.sorz.lab.smallcloudemoji.db.EntryDao;
import org.sorz.lab.smallcloudemoji.db.Repository;

import java.util.List;

/**
 * Show all settings. Shown inside SettingsActivity.
 * Include source sync (downloading XML).
 */
public class SettingsFragment extends PreferenceFragment {
    private Context context;
    private DaoSession daoSession;
    private Repository repository;
    private OnSourceManageClickListener mListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getActivity();
        mListener = (OnSourceManageClickListener) context;
        daoSession = DatabaseHelper.getInstance(context).getDaoSession();

        addPreferencesFromResource(R.xml.preferences);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        repository = daoSession.getRepositoryDao().queryBuilder()
                .limit(1).unique();

        // Usage history clean
        Preference historyCleanPreference = findPreference("history_clean");
        historyCleanPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                EntryDao entryDao = daoSession.getEntryDao();
                List<Entry> entries = entryDao.queryBuilder()
                        .where(EntryDao.Properties.LastUsed.isNotNull())
                        .list();
                for (Entry entry : entries) {
                    entry.setLastUsed(null);
                }
                entryDao.updateInTx(entries);
                Toast.makeText(context, R.string.pref_history_clean_done,
                        Toast.LENGTH_SHORT).show();
                return true;
            }
        });

        // Stars clean
        Preference starCleanPreference = findPreference("star_clean");
        starCleanPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle(R.string.pref_star_clean_confirm_title)
                        .setMessage(R.string.pref_star_clean_confirm_msg)
                        .setCancelable(true);
                builder.setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        EntryDao entryDao = daoSession.getEntryDao();
                        List<Entry> entries = entryDao.queryBuilder()
                                .where(EntryDao.Properties.Star.eq(true))
                                .list();
                        for (Entry entry : entries) {
                            entry.setStar(false);
                        }
                        entryDao.updateInTx(entries);
                        Toast.makeText(context, R.string.pref_star_clean_done,
                                Toast.LENGTH_SHORT).show();
                    }
                });
                builder.show();
                return true;
            }
        });

        // Click source management
        Preference sourceManagePreference = findPreference("source_manage");
        sourceManagePreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                mListener.onSourceManageClick();
                return true;
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

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public interface OnSourceManageClickListener {
        public void onSourceManageClick();
    }

}
