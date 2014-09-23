package org.sorz.lab.smallcloudemoji;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import org.sorz.lab.smallcloudemoji.db.DaoSession;
import org.sorz.lab.smallcloudemoji.db.DatabaseHelper;
import org.sorz.lab.smallcloudemoji.db.DatabaseUpgrader;


public class SettingsActivity extends Activity implements
        SettingsFragment.OnSourceManageClickListener {
    private RepositoryFragment repositoryFragment;
    private DaoSession daoSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Open database.
        DatabaseHelper databaseHelper = DatabaseHelper.getInstance(this, true);
        daoSession = databaseHelper.getDaoSession();
        DatabaseUpgrader.checkAndDoUpgrade(this, daoSession);

        setContentView(R.layout.activity_settings);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        ActionBar actionBar = getActionBar();
        if (actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == android.R.id.home) {
            if (getFragmentManager().getBackStackEntryCount() == 0)
                finish();
            else
                getFragmentManager().popBackStack();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        DatabaseHelper.getInstance(this).close();
    }

    @Override
    public void onSourceManageClick() {
        FragmentManager fragmentManager = getFragmentManager();
        Fragment settingsFragment = fragmentManager.findFragmentById(R.id.settings_frag);
        if (repositoryFragment == null)
            repositoryFragment = new RepositoryFragment();
        fragmentManager.beginTransaction()
                .hide(settingsFragment)
                .add(R.id.settings_container, repositoryFragment)
                .addToBackStack(null)
                .commit();
    }
}
