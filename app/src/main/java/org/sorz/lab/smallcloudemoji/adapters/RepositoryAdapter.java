package org.sorz.lab.smallcloudemoji.adapters;

import android.content.Context;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.sorz.lab.smallcloudemoji.R;
import org.sorz.lab.smallcloudemoji.db.DaoSession;
import org.sorz.lab.smallcloudemoji.db.Repository;
import org.sorz.lab.smallcloudemoji.db.RepositoryDao;

import java.util.List;

/**
 * Get repositories from database and generate a view for each them.
 */
public class RepositoryAdapter extends BaseAdapter {
    final private Context context;
    final private LayoutInflater inflater;
    private List<Repository> repositories;
    private RepositoryDao repositoryDao;

    public RepositoryAdapter(Context context, DaoSession daoSession) {
        super();
        this.context = context;
        repositoryDao = daoSession.getRepositoryDao();
        inflater = LayoutInflater.from(context);

        loadRepositories();
    }

    private void loadRepositories() {
        repositories = repositoryDao.queryBuilder()
                .orderAsc(RepositoryDao.Properties.Order)
                .list();
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public boolean isEnabled(int position) {
        return false;
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {

    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {

    }

    @Override
    public int getCount() {
        return repositories.size();
    }

    @Override
    public Repository getItem(int position) {
        return repositories.get(position);
    }

    @Override
    public long getItemId(int position) {
        return repositories.get(position).getId();
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null)
            convertView = inflater.inflate(R.layout.item_repository, parent, false);
        TextView alias = (TextView) convertView.findViewById(R.id.repository_alias);
        TextView url = (TextView) convertView.findViewById(R.id.repository_url);
        Repository repository = getItem(position);
        alias.setText(repository.getAlias());
        url.setText(repository.getUrl());
        convertView.findViewById(R.id.repository_buttons).setTag(repository);
        return convertView;
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return repositories.isEmpty();
    }

    @Override
    public void notifyDataSetChanged() {
        loadRepositories();
        super.notifyDataSetChanged();
    }
}
