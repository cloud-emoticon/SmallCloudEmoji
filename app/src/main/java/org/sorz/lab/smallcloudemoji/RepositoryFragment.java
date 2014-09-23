package org.sorz.lab.smallcloudemoji;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import org.sorz.lab.smallcloudemoji.db.DaoSession;
import org.sorz.lab.smallcloudemoji.db.DaoSessionHolder;


/**
 * A fragment representing a list of repositories.
 */
public class RepositoryFragment extends Fragment {
    private Context context;
    private DaoSession daoSession;
    private RepositoryAdapter adapter;

    public RepositoryFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getActivity();
        daoSession = ((DaoSessionHolder) context).getDaoSession();
        adapter = new RepositoryAdapter(context, daoSession);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_repository, container, false);
        ListView listView = (ListView) view.findViewById(android.R.id.list);
        listView.setAdapter(adapter);
        return view;
    }

}
