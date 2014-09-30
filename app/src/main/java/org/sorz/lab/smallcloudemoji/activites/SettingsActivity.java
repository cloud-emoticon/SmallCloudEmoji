package org.sorz.lab.smallcloudemoji.activites;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;

import org.sorz.lab.smallcloudemoji.R;
import org.sorz.lab.smallcloudemoji.db.Category;
import org.sorz.lab.smallcloudemoji.db.CategoryDao;
import org.sorz.lab.smallcloudemoji.db.DaoSession;
import org.sorz.lab.smallcloudemoji.db.DatabaseHelper;
import org.sorz.lab.smallcloudemoji.db.DatabaseUpgrader;
import org.sorz.lab.smallcloudemoji.db.EntryDao;
import org.sorz.lab.smallcloudemoji.db.Repository;
import org.sorz.lab.smallcloudemoji.db.RepositoryDao;
import org.sorz.lab.smallcloudemoji.fragments.RepositoryFragment;
import org.sorz.lab.smallcloudemoji.fragments.SettingsFragment;
import org.sorz.lab.smallcloudemoji.tasks.DownloadXmlAsyncTask;

import java.util.ArrayList;
import java.util.List;


public class SettingsActivity extends Activity implements
        FragmentManager.OnBackStackChangedListener,
        SettingsFragment.OnSourceManageClickListener {
    private final static String REPOSITORY_FRAGMENT_IS_SHOWING = "REPOSITORY_FRAGMENT_IS_SHOWING";
    private RepositoryFragment repositoryFragment;
    private RepositoryDao repositoryDao;
    private DaoSession daoSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().addOnBackStackChangedListener(this);

        // Open database.
        DatabaseHelper databaseHelper = DatabaseHelper.getInstance(this, true);
        daoSession = databaseHelper.getDaoSession();
        repositoryDao = daoSession.getRepositoryDao();
        DatabaseUpgrader.checkAndDoUpgrade(this, daoSession);

        setContentView(R.layout.activity_settings);
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
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState.getBoolean(REPOSITORY_FRAGMENT_IS_SHOWING, false)) {
            onSourceManageClick();
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

    private Repository getRepositoryFromView(View view) {
        return (Repository) ((View) view.getParent()).getTag();
    }

    public void hideRepository(View view) {
        ImageButton button = (ImageButton) view;
        Repository repository = getRepositoryFromView(view);
        repository.setHidden(!repository.getHidden());
        repository.update();
        if (repository.getHidden())
            button.setBackgroundResource(R.drawable.ic_eye_slash);
        else
            button.setBackgroundResource(R.drawable.ic_eye_normal);
    }

    public void moveUpRepository(View view) {
        Repository repository = getRepositoryFromView(view);
        Repository targetRepository = repositoryDao.queryBuilder()
                .where(RepositoryDao.Properties.Order.lt(repository.getOrder()))
                .orderDesc(RepositoryDao.Properties.Order)
                .limit(1).unique();
        swapRepositoryOrder(repository, targetRepository);
    }

    public void moveDownRepository(View view) {
        Repository repository = getRepositoryFromView(view);
        Repository targetRepository = repositoryDao.queryBuilder()
                .where(RepositoryDao.Properties.Order.gt(repository.getOrder()))
                .orderAsc(RepositoryDao.Properties.Order)
                .limit(1).unique();
        swapRepositoryOrder(repository, targetRepository);
    }

    private void swapRepositoryOrder(Repository repositoryA, Repository repositoryB) {
        if (repositoryA == null || repositoryB == null)
            return;

        int order = repositoryA.getOrder();
        repositoryA.setOrder(repositoryB.getOrder());
        repositoryB.setOrder(order);
        repositoryDao.updateInTx(repositoryA, repositoryB);
        repositoryFragment.notifyRepositoriesChanged();
    }

    public void popMoreMenu(View view) {
        final Repository repository = getRepositoryFromView(view);
        PopupMenu popupMenu = new PopupMenu(this, view);
        popupMenu.inflate(R.menu.repository_more);

        // Disable deleting if only one remaining.
        long numRepositories = repositoryDao.queryBuilder()
                .count();
        if (numRepositories <= 1)
            popupMenu.getMenu().findItem(R.id.menu_repository_delete).setEnabled(false);

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                return moreMenuItemClicked(menuItem.getItemId(), repository);
            }
        });
        popupMenu.show();
    }

    private boolean moreMenuItemClicked(int itemId, Repository repository) {
        if (itemId == R.id.menu_repository_sync) {
            new DownloadXmlAsyncTask(this, daoSession).execute(repository);
        } else if (itemId == R.id.menu_repository_rename) {
            renameRepository(repository);
        } else if (itemId == R.id.menu_repository_delete) {
            deleteRepositoryIfConfirmed(repository);
        }
        return true;
    }

    private void renameRepository(final Repository repository) {
        final EditText editText = new EditText(this);
        editText.setText(repository.getAlias());
        editText.selectAll();

        DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String newAlias = editText.getText().toString().trim();
                if (newAlias.isEmpty())
                    return;
                repository.setAlias(newAlias);
                repository.update();
                repositoryFragment.notifyRepositoriesChanged();
            }
        };

        new AlertDialog.Builder(this)
                .setTitle(R.string.repository_rename_title)
                .setCancelable(true)
                .setView(editText)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.yes, onClickListener)
                .show();
        editText.requestFocus();
    }

    private void deleteRepositoryIfConfirmed(final Repository repository) {
        // Generate message according to whether stars the on repository.
        String message = String.format(getString(R.string.confirm_delete_repository_part1),
                repository.getAlias());
        List<Category> categories = repository.getCategories();
        final List<Long> categoryIds = new ArrayList<Long>(categories.size());
        for (Category category : categories)
            categoryIds.add(category.getId());
        long numStar = daoSession.getEntryDao().queryBuilder()
                .where(EntryDao.Properties.CategoryId.in(categoryIds),
                        EntryDao.Properties.Star.eq(true))
                .count();
        if (numStar > 0)
            message += String.format(getString(R.string.confirm_delete_repository_part2),
                    numStar);

        // The actual code deleting the repository.
        DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                daoSession.runInTx(new Runnable() {
                    @Override
                    public void run() {
                        daoSession.getEntryDao().queryBuilder()
                                .where(EntryDao.Properties.CategoryId.in(categoryIds))
                                .buildDelete().executeDeleteWithoutDetachingEntities();
                        daoSession.getCategoryDao().queryBuilder()
                                .where(CategoryDao.Properties.RepositoryId.eq(repository.getId()))
                                .buildDelete().executeDeleteWithoutDetachingEntities();
                        repository.delete();
                    }
                });
                repositoryFragment.notifyRepositoriesChanged();
            }
        };

        // Show alert dialog.
        new AlertDialog.Builder(this)
                .setCancelable(true)
                .setTitle(R.string.confirm_delete_repository_title)
                .setMessage(message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.yes, onClickListener)
                .show();
    }
}
