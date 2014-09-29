package org.sorz.lab.smallcloudemoji.activites;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import org.sorz.lab.smallcloudemoji.R;
import org.sorz.lab.smallcloudemoji.db.DaoSession;
import org.sorz.lab.smallcloudemoji.db.DatabaseHelper;
import org.sorz.lab.smallcloudemoji.db.Repository;
import org.sorz.lab.smallcloudemoji.db.RepositoryDao;
import org.sorz.lab.smallcloudemoji.tasks.DownloadXmlAsyncTask;


public class AddRepositoryActivity extends Activity {
    public final static int RESULT_SUCCESS_ADDED = 2;
    private TextView urlTextView;
    private TextView aliasTextView;
    private DaoSession daoSession;
    private RepositoryDao repositoryDao;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_repository);
        urlTextView = (TextView) findViewById(R.id.repository_url);
        aliasTextView = (TextView) findViewById(R.id.repository_alias);
        daoSession = DatabaseHelper.getInstance(this, true).getDaoSession();
        repositoryDao = daoSession.getRepositoryDao();

        Uri uri = getIntent().getData();
        if (uri != null) {
            if (uri.getScheme().equalsIgnoreCase("cloudemoticon"))
                urlTextView.setText("http" + uri.toString().substring(13));
            else
                urlTextView.setText("https" + uri.toString().substring(14));
            aliasTextView.requestFocus();
        }
    }

    public void cancel(View v) {
        finish();
    }

    public void confirm(View v) {
        final String url = urlTextView.getText().toString().trim();
        final String alias = aliasTextView.getText().toString().trim();

        // Verify input.
        if (url.isEmpty() || alias.isEmpty()) {
            if (url.isEmpty())
                urlTextView.setError(getString(R.string.input_cannot_be_empty));
            if (alias.isEmpty())
                aliasTextView.setError(getString(R.string.input_cannot_be_empty));
            return;
        }

        // Check duplication.
        boolean exist = repositoryDao.queryBuilder()
                .where(RepositoryDao.Properties.Url.eq(url))
                .count() != 0;
        if (exist) {
            urlTextView.setError(getString(R.string.input_repository_exist));
            return;
        }

        // Get smallest order for adding on top.
        Repository topRepository =  repositoryDao.queryBuilder()
                .orderAsc(RepositoryDao.Properties.Order)
                .limit(1).unique();
        int order = 100;
        if (topRepository != null)
            order = topRepository.getOrder() - 10;

        Repository repository = new Repository(null, url, alias, false, order, null);
        new DownloadXmlAsyncTask(this, daoSession) {
            @Override
            protected void onPostExecute(Integer result) {
                super.onPostExecute(result);
                if (R.string.download_success == result) {
                    setResult(RESULT_SUCCESS_ADDED);
                    finish();
                }
            }
        }.execute(repository);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        DatabaseHelper.getInstance(this).close();
    }


}
