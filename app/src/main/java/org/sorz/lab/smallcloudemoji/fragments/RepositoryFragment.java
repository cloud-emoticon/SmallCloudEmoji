package org.sorz.lab.smallcloudemoji.fragments;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.sorz.lab.smallcloudemoji.R;
import org.sorz.lab.smallcloudemoji.activites.AddRepositoryActivity;
import org.sorz.lab.smallcloudemoji.adapters.RepositoryAdapter;
import org.sorz.lab.smallcloudemoji.db.Category;
import org.sorz.lab.smallcloudemoji.db.CategoryDao;
import org.sorz.lab.smallcloudemoji.db.DaoSession;
import org.sorz.lab.smallcloudemoji.db.DatabaseHelper;
import org.sorz.lab.smallcloudemoji.db.EntryDao;
import org.sorz.lab.smallcloudemoji.db.Repository;
import org.sorz.lab.smallcloudemoji.db.RepositoryDao;
import org.sorz.lab.smallcloudemoji.tasks.DownloadAsyncTask;

import java.util.ArrayList;
import java.util.List;


/**
 * A fragment representing a list of repositories.
 */
public class RepositoryFragment extends Fragment {
    private final static int REQUEST_FOR_ADDING_REPOSITORY = 1;
    private Context context;
    private OnEmoticonStoreClickListener mListener;
    private DaoSession daoSession;
    private RepositoryDao repositoryDao;
    private RepositoryAdapter adapter;
    private ListView listView;

    public RepositoryFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getActivity();
        mListener = (OnEmoticonStoreClickListener) context;
        daoSession = DatabaseHelper.getInstance(context).getDaoSession();
        repositoryDao = daoSession.getRepositoryDao();
        adapter = new RepositoryAdapter(context, daoSession);

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_repository, container, false);
        listView = (ListView) view.findViewById(R.id.repository_list);
        View footer = getActivity().getLayoutInflater().inflate(R.layout.store_entry, null);
        View goEmoticonStore = footer.findViewById(R.id.goEmoticonStore);
        listView.addFooterView(footer, null, false);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                popMoreMenu(view.findViewById(R.id.button_more));
            }
        });
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent,
                                           View view, int position, long id) {
                Repository repository = adapter.getItem(position);
                Uri uri = Uri.parse(repository.getUrl());
                ClipboardManager clipboard =
                        (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clipData = ClipData.newUri(context.getContentResolver(),
                        repository.getAlias(), uri);
                clipboard.setPrimaryClip(clipData);
                Toast.makeText(context, R.string.toast_repository_copied,
                        Toast.LENGTH_SHORT).show();
                return true;
            }
        });
        goEmoticonStore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.onEmoticonStoreClick();
            }
        });
        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.repository_actions, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_repository_add) {
            Intent intent = new Intent(context, AddRepositoryActivity.class);
            startActivityForResult(intent, REQUEST_FOR_ADDING_REPOSITORY);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_FOR_ADDING_REPOSITORY) {
            if (resultCode == AddRepositoryActivity.RESULT_SUCCESS_ADDED) {
                notifyRepositoriesChanged();
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void notifyRepositoriesChanged() {
        adapter.notifyDataSetChanged();
        listView.invalidateViews();  // This shit wasted my one hour.
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
        notifyRepositoriesChanged();
    }

    public void popMoreMenu(View view) {
        final Repository repository = getRepositoryFromView(view);
        PopupMenu popupMenu = new PopupMenu(context, view);
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
            syncRepository(repository);
        } else if (itemId == R.id.menu_repository_rename) {
            renameRepository(repository);
        } else if (itemId == R.id.menu_repository_delete) {
            deleteRepositoryIfConfirmed(repository);
        }
        return true;
    }

    private void syncRepository(final Repository repository) {
        View itemView = (View) getActivity().findViewById(R.id.repository_list)
                .findViewWithTag(repository).getParent();
        final ProgressBar progressBar =
                (ProgressBar) itemView.findViewById(R.id.repository_progressbar);
        new DownloadAsyncTask(context) {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                progressBar.setProgress(0);
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                super.onProgressUpdate(values);
                progressBar.setProgress(values[0]);
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            protected void onPostExecute(Integer result) {
                super.onPostExecute(result);
                progressBar.setVisibility(View.GONE);
            }

            @Override
            protected void onCancelled(Integer result) {
                super.onCancelled(result);
                if (result != DownloadAsyncTask.RESULT_CANCELLED)
                    return;
                progressBar.setVisibility(View.GONE);
            }
        }.execute(repository);
    }

    private void renameRepository(final Repository repository) {
        final EditText editText = new EditText(context);
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
                notifyRepositoriesChanged();
            }
        };

        new AlertDialog.Builder(context)
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
                notifyRepositoriesChanged();
            }
        };

        // Show alert dialog.
        new AlertDialog.Builder(context)
                .setCancelable(true)
                .setTitle(R.string.confirm_delete_repository_title)
                .setMessage(message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.yes, onClickListener)
                .show();
    }

    public interface OnEmoticonStoreClickListener {
        public void onEmoticonStoreClick();
    }

}
