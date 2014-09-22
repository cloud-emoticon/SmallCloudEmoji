package org.sorz.lab.smallcloudemoji;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;


public class SettingsActivity extends Activity implements
        SettingsFragment.OnSourceManageClickListener {
    private RepositoryFragment repositoryFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
    public void onSourceManageClick() {
        FragmentManager fragmentManager = getFragmentManager();
        Fragment fragment = fragmentManager.findFragmentById(R.id.repository_frag);

        if (fragment == null) {
            Fragment settingsFragment = fragmentManager.findFragmentById(R.id.settings_frag);
            if (repositoryFragment == null)
                repositoryFragment = new RepositoryFragment();

            fragmentManager.beginTransaction()
                    .hide(settingsFragment)
                    .add(R.id.frame_layout, repositoryFragment)
                    .addToBackStack(null)
                    .commit();
        } else {
            // TODO: Set focus on repositoryFragment
        }
    }
}
