package org.sorz.lab.smallcloudemoji;

import android.app.Activity;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;


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

        new DownloadXmlAsyncTask(this) {
            @Override
            protected void onPostExecute(Integer result) {
                super.onPostExecute(result);
                if (R.string.download_success == result) {
                    editor.putString("source_address", newSourceUrl);
                    editor.commit();
                    finish();
                }
            }
        }.execute(newSourceUrl, "emojis.xml");
    }

}
