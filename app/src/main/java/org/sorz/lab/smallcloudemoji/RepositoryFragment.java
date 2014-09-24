package org.sorz.lab.smallcloudemoji;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import org.sorz.lab.smallcloudemoji.db.DaoSession;
import org.sorz.lab.smallcloudemoji.db.DatabaseHelper;


/**
 * A fragment representing a list of repositories.
 */
public class RepositoryFragment extends Fragment {
    private final static int REQUEST_FOR_ADDING_REPOSITORY = 1;
    private Context context;
    private DaoSession daoSession;
    private RepositoryAdapter adapter;

    public RepositoryFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getActivity();
        daoSession = DatabaseHelper.getInstance(context).getDaoSession();
        adapter = new RepositoryAdapter(context, daoSession);

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_repository, container, false);
        ListView listView = (ListView) view.findViewById(android.R.id.list);
        listView.setAdapter(adapter);
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
                // TODO: Refresh repositories list
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

}
