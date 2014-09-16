package org.sorz.lab.smallcloudemoji;

import android.app.Activity;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;

import org.sorz.lab.smallcloudemoji.db.DaoMaster;
import org.sorz.lab.smallcloudemoji.db.DaoSession;
import org.sorz.lab.smallcloudemoji.db.DatabaseOpenHelper;
import org.sorz.lab.smallcloudemoji.db.Repository;


public class ViewNewSourceActivity extends Activity {
    String newSourceUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_new_source);

        Uri uri = getIntent().getData();
        if (uri.getScheme().equalsIgnoreCase("cloudemoticon")) {
            newSourceUrl = "http" + uri.toString().substring(13);
        } else {
            newSourceUrl = "https" + uri.toString().substring(14);
        }
    }

    public void cancel(View v) {
        finish();
    }

    public void confirm(View v) {
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(ViewNewSourceActivity.this);
        final SharedPreferences.Editor editor = sharedPreferences.edit();

        DatabaseOpenHelper databaseOpenHelper = new DatabaseOpenHelper(this);
        DaoSession daoSession = databaseOpenHelper.getDaoSession();
        final DaoMaster daoMaster = databaseOpenHelper.getDaoMaster();
        final Repository repository = databaseOpenHelper.getDefaultRepository();

        final String oldUrl = repository.getUrl();
        repository.setUrl(newSourceUrl);

        new DownloadXmlAsyncTask(this, daoSession) {
            @Override
            protected void onPostExecute(Integer result) {
                super.onPostExecute(result);
                if (R.string.download_success == result) {
                    editor.putString("source_address", newSourceUrl);
                    editor.commit();
                    repository.update();
                    daoMaster.getDatabase().close();
                    finish();
                } else {
                    repository.setUrl(oldUrl);
                    repository.update();
                    daoMaster.getDatabase().close();
                }
            }
        }.execute(repository);
    }

}
