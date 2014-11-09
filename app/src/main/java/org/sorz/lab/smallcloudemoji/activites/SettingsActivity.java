package org.sorz.lab.smallcloudemoji.activites;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.LruCache;
import android.view.MenuItem;
import android.view.View;

import org.sorz.lab.smallcloudemoji.R;
import org.sorz.lab.smallcloudemoji.db.DaoSession;
import org.sorz.lab.smallcloudemoji.db.DatabaseHelper;
import org.sorz.lab.smallcloudemoji.db.DatabaseUpgrader;
import org.sorz.lab.smallcloudemoji.fragments.RepositoryFragment;
import org.sorz.lab.smallcloudemoji.fragments.SettingsFragment;
import org.sorz.lab.smallcloudemoji.fragments.SourceFragment;
import org.sorz.lab.smallcloudemoji.fragments.StoreFragment;
import org.sorz.lab.smallcloudemoji.interfaces.IconCacheHolder;

import java.util.Stack;


public class SettingsActivity extends Activity implements
        FragmentManager.OnBackStackChangedListener,
        SettingsFragment.OnSourceManageClickListener,
        RepositoryFragment.OnEmoticonStoreClickListener,
        StoreFragment.OnSourceClickListener,
        IconCacheHolder {
    private final static String REPOSITORY_FRAGMENT_IS_SHOWING = "REPOSITORY_FRAGMENT_IS_SHOWING";
    private final static String STORE_FRAGMENT_IS_SHOWING = "STORE_FRAGMENT_IS_SHOWING";
    public final static int REQUEST_FOR_ADDING_REPOSITORY = 1;

    private LruCache<String, Bitmap> iconCache;
    private RepositoryFragment repositoryFragment;
    private StoreFragment storeFragment;
    private boolean tabletLayout;
    private Stack<CharSequence> titleHistory = new Stack<CharSequence>();

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
            if (savedInstanceState.getBoolean(STORE_FRAGMENT_IS_SHOWING, false)) {
                onEmoticonStoreClick();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        DatabaseHelper.getInstance(this).close();
    }

    private void setActionBarTitle(int id) {
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            titleHistory.push(actionBar.getTitle());
            titleHistory.push(getString(id));
        }
    }

    @Override
    public void onBackStackChanged() {
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(
                    getFragmentManager().getBackStackEntryCount() > 0);
            if (!titleHistory.empty())
                actionBar.setTitle(titleHistory.pop());
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_FOR_ADDING_REPOSITORY) {
            if (repositoryFragment != null)
                repositoryFragment.onActivityResult(requestCode, resultCode, data);
            if (storeFragment != null)
                storeFragment.onActivityResult(requestCode, resultCode, data);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onSourceManageClick() {
        if (tabletLayout)
            return false;
        setActionBarTitle(R.string.pref_source_manage_title);
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
        setActionBarTitle(R.string.title_emoticon_store);
        FragmentManager fragmentManager = getFragmentManager();
        if (storeFragment == null)
            storeFragment = StoreFragment.newInstance(getString(R.string.store_url));
        fragmentManager.beginTransaction()
                .hide(repositoryFragment)
                .add(R.id.settings_container, storeFragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onSourceClick(long sourceId) {
        setActionBarTitle(R.string.title_emoticon_store);
        FragmentManager fragmentManager = getFragmentManager();
        SourceFragment sourceFragment = SourceFragment.newInstance(sourceId);
        fragmentManager.beginTransaction()
                .hide(storeFragment)
                .add(R.id.settings_container, sourceFragment)
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

    @Override
    public synchronized LruCache<String, Bitmap> getIconCache() {
        if (iconCache == null) {
            // Get max available VM memory in KiB.
            final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
            // Use 1/16th of the available memory for icon cache.
            // ~70 icons in 120 x 120px for 64MiB available memory.
            final int cacheSize = maxMemory / 16;

            iconCache = new LruCache<String, Bitmap>(cacheSize) {
                @Override
                protected int sizeOf(String key, Bitmap bitmap) {
                    // The cache size will be measured in kilobytes rather than
                    // number of items.
                    return bitmap.getByteCount() / 1024;
                }
            };
        }
        return iconCache;
    }
}
