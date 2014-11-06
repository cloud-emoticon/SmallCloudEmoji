package org.sorz.lab.smallcloudemoji.activites;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import org.sorz.lab.smallcloudemoji.R;
import org.sorz.lab.smallcloudemoji.db.DaoSession;
import org.sorz.lab.smallcloudemoji.db.DatabaseHelper;
import org.sorz.lab.smallcloudemoji.db.DatabaseUpgrader;
import org.sorz.lab.smallcloudemoji.fragments.RepositoryFragment;
import org.sorz.lab.smallcloudemoji.fragments.SettingsFragment;
import org.sorz.lab.smallcloudemoji.fragments.StoreFragment;


public class SettingsActivity extends Activity implements
        FragmentManager.OnBackStackChangedListener,
        SettingsFragment.OnSourceManageClickListener,
        RepositoryFragment.OnEmoticonStoreClickListener {
    private final static String REPOSITORY_FRAGMENT_IS_SHOWING = "REPOSITORY_FRAGMENT_IS_SHOWING";
    private final static String STORE_FRAGMENT_IS_SHOWING = "STORE_FRAGMENT_IS_SHOWING";
    private RepositoryFragment repositoryFragment;
    private StoreFragment storeFragment;
    private boolean tabletLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().addOnBackStackChangedListener(this);

        // Open database.
        DatabaseHelper databaseHelper = DatabaseHelper.getInstance(this, true);
        DaoSession daoSession = databaseHelper.getDaoSession();
        DatabaseUpgrader.checkAndDoUpgrade(this, daoSession);

        setContentView(R.layout.activity_settings);
        repositoryFragment = (RepositoryFragment) getFragmentManager()
                .findFragmentById(R.id.repository_frag);
        tabletLayout = repositoryFragment != null;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            getFragmentManager().popBackStack();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(REPOSITORY_FRAGMENT_IS_SHOWING,
                repositoryFragment != null && !repositoryFragment.isHidden());
        outState.putBoolean(STORE_FRAGMENT_IS_SHOWING,
                storeFragment != null && !storeFragment.isHidden());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        if (!tabletLayout) {
            if (savedInstanceState.getBoolean(REPOSITORY_FRAGMENT_IS_SHOWING, false)) {
                onSourceManageClick();
            } else if (savedInstanceState.getBoolean(STORE_FRAGMENT_IS_SHOWING, false)) {
                onSourceManageClick();
                onEmoticonStoreClick();
            }
        } else {
            // TODO: Support tablet layout.
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        DatabaseHelper.getInstance(this).close();
    }

    @Override
    public void onBackStackChanged() {
        ActionBar actionBar = getActionBar();
        if (actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(
                    getFragmentManager().getBackStackEntryCount() > 0);
    }

    @Override
    public boolean onSourceManageClick() {
        if (tabletLayout)
            return false;  // TODO: Support Store
        FragmentManager fragmentManager = getFragmentManager();
        Fragment settingsFragment = fragmentManager.findFragmentById(R.id.settings_frag);
        if (repositoryFragment == null)
            repositoryFragment = new RepositoryFragment();
        fragmentManager.beginTransaction()
                .hide(settingsFragment)
                .add(R.id.settings_container, repositoryFragment)
                .addToBackStack(null)
                .commit();
        return true;
    }

    @Override
    public void onEmoticonStoreClick() {
        if (tabletLayout) {
            return; // TODO: Support tablet layout
        }
        FragmentManager fragmentManager = getFragmentManager();
        if (storeFragment == null)
            storeFragment = StoreFragment.newInstance(getString(R.string.store_url));
        fragmentManager.beginTransaction()
                .hide(repositoryFragment)
                .add(R.id.settings_container, storeFragment)
                .addToBackStack(null)
                .commit();
    }

    public void hideRepository(View view) {
        repositoryFragment.hideRepository(view);
    }

    public void moveUpRepository(View view) {
        repositoryFragment.moveUpRepository(view);
    }

    public void moveDownRepository(View view) {
        repositoryFragment.moveDownRepository(view);
    }

    public void popMoreMenu(View view) {
        repositoryFragment.popMoreMenu(view);
    }

}
