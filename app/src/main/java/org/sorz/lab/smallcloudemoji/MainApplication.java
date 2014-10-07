package org.sorz.lab.smallcloudemoji;


import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.Toast;

import com.sony.smallapp.SmallAppWindow;
import com.sony.smallapp.SmallApplication;

import org.sorz.lab.smallcloudemoji.activites.SettingsActivity;
import org.sorz.lab.smallcloudemoji.adapters.MainExpandableAdapter;
import org.sorz.lab.smallcloudemoji.db.DaoSession;
import org.sorz.lab.smallcloudemoji.db.DatabaseHelper;
import org.sorz.lab.smallcloudemoji.db.DatabaseUpgrader;
import org.sorz.lab.smallcloudemoji.db.Entry;
import org.sorz.lab.smallcloudemoji.db.EntryDao;
import org.sorz.lab.smallcloudemoji.db.Repository;
import org.sorz.lab.smallcloudemoji.db.RepositoryDao;
import org.sorz.lab.smallcloudemoji.tasks.DownloadAsyncTask;

import java.util.Date;

/**
 * Small application for Sony.
 */
public class MainApplication extends SmallApplication {
    private SharedPreferences sharedPreferences;
    private MainExpandableAdapter adapter;
    private DaoSession daoSession;

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

        // Open database.
        DatabaseHelper databaseHelper = DatabaseHelper.getInstance(this, true);
        daoSession = databaseHelper.getDaoSession();
        EntryDao entryDao = daoSession.getEntryDao();
        RepositoryDao repositoryDao = daoSession.getRepositoryDao();
        DatabaseUpgrader.checkAndDoUpgrade(this, daoSession);

        // Download if it's empty.
        if (entryDao.queryBuilder().limit(1).count() == 0) {
            Repository repository = repositoryDao.queryBuilder().limit(1).unique();
            // Minimize the windows rather than mask the process dialog.
            getWindow().setWindowState(SmallAppWindow.WindowState.MINIMIZED);
            updateRepository(repository);

        }

        final ExpandableListView listView =
                (ExpandableListView) findViewById(R.id.expandableListView);

        // Add options button.
        LayoutInflater inflater = LayoutInflater.from(this);
        View bottom = inflater.inflate(R.layout.item_options, null);
        Button button = (Button) bottom.findViewById(R.id.options_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainApplication.this, SettingsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        });
        listView.addFooterView(bottom);

        adapter = new MainExpandableAdapter(this, daoSession);
        listView.setAdapter(adapter);
        listView.expandGroup(0, false);

        listView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                                        int childPosition, long id) {
                Entry entry = adapter.getChild(groupPosition, childPosition);
                ClipboardManager clipboard =
                        (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData clipData = ClipData.newPlainText("emoticon", entry.getEmoticon());
                clipboard.setPrimaryClip(clipData);
                Toast.makeText(MainApplication.this, R.string.toast_copied,
                        Toast.LENGTH_SHORT).show();
                entry.setLastUsed(new Date());
                entry.update();

                String action = sharedPreferences.getString("action_after_copied", "MINIMIZE");
                if (action.equals("MINIMIZE"))
                    getWindow().setWindowState(SmallAppWindow.WindowState.MINIMIZED);
                else if (action.equals("CLOSE"))
                    finish();
                return true;
            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Entry entry = (Entry) view.getTag(R.id.entry);
                if (entry == null)
                    return false;
                entry.setStar(!entry.getStar());
                entry.update();
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
        DatabaseHelper.getInstance(this).close();
    }

    private void updateRepository(final Repository repository) {
        new DownloadAsyncTask(this, daoSession) {
            private ProgressDialog progressDialog;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                progressDialog = new ProgressDialog(MainApplication.this);
                progressDialog.setTitle(R.string.download_title);
                progressDialog.setMessage(getString(R.string.download_message));
                progressDialog.setMax(100);
                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                progressDialog.setCancelable(true);
                progressDialog.setCanceledOnTouchOutside(false);
                progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        cancel(true);
                    }
                });
                progressDialog.show();
            }

            @Override
            protected void onPostExecute(Integer result) {
                super.onPostExecute(result);
                progressDialog.dismiss();
                repository.resetCategories();
                adapter.notifyDataSetChanged(true);
                getWindow().setWindowState(SmallAppWindow.WindowState.NORMAL);
            }

            @Override
            protected void onCancelled(Integer result) {
                super.onCancelled();
                if (result == DownloadAsyncTask.RESULT_SUCCESS)
                    return;
                progressDialog.dismiss();
            }
        }.execute(repository);
    }

}
