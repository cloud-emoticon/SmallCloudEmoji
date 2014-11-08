package org.sorz.lab.smallcloudemoji.fragments;


import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.sorz.lab.smallcloudemoji.R;
import org.sorz.lab.smallcloudemoji.activites.AddRepositoryActivity;
import org.sorz.lab.smallcloudemoji.activites.SettingsActivity;
import org.sorz.lab.smallcloudemoji.db.DaoSession;
import org.sorz.lab.smallcloudemoji.db.DatabaseHelper;
import org.sorz.lab.smallcloudemoji.db.Source;
import org.sorz.lab.smallcloudemoji.db.SourceDao;
import org.sorz.lab.smallcloudemoji.interfaces.IconCacheHolder;

import java.text.DateFormat;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SourceFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SourceFragment extends Fragment {
    private static final String SOURCE_ID = "source_id";

    private Context context;
    private Source source;
    private LruCache<String, Bitmap> iconCache;

    private Button installButton;


    public static SourceFragment newInstance(long sourceId) {
        SourceFragment fragment = new SourceFragment();
        Bundle args = new Bundle();
        args.putLong(SOURCE_ID, sourceId);
        fragment.setArguments(args);
        return fragment;
    }

    public SourceFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getActivity();
        DaoSession daoSession = DatabaseHelper.getInstance(context).getDaoSession();
        SourceDao sourceDao = daoSession.getSourceDao();
        iconCache = ((IconCacheHolder) context).getIconCache();

        if (getArguments() != null) {
            long sourceId = getArguments().getLong(SOURCE_ID);
            source = sourceDao.queryBuilder()
                    .where(SourceDao.Properties.Id.eq(sourceId))
                    .unique();
        }

        setHasOptionsMenu(true);
    }

    private void setTextViewIfNotNull(View view, int id, String text) {
        TextView textView = (TextView) view.findViewById(id);
        if (text != null)
            textView.setText(text);
    }

    private void updateInstallState() {
        if (source.getInstalled()) {
            installButton.setText(R.string.source_installed);
            installButton.setEnabled(false);
        } else {
            installButton.setText(R.string.source_install);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_source, container, false);
        installButton = (Button) view.findViewById(R.id.install);

        setTextViewIfNotNull(view, R.id.source_name, source.getName());
        setTextViewIfNotNull(view, R.id.source_introduction, source.getIntroduction());
        setTextViewIfNotNull(view, R.id.source_creator, source.getCreator());
        setTextViewIfNotNull(view, R.id.source_creator_url, source.getCreatorUrl());
        setTextViewIfNotNull(view, R.id.source_code_url, source.getCodeUrl());

        String server = source.getServer();
        if ("dropboxusercontent.com".equals(server))
            server = "Dropbox";
        setTextViewIfNotNull(view, R.id.source_server, server);

        String postDate = null;
        if (source.getPostDate() != null)
            postDate = DateFormat.getDateInstance().format(source.getPostDate());
        setTextViewIfNotNull(view, R.id.source_postdate, postDate);

        if (source.getIconUrl() != null) {
            Bitmap icon = iconCache.get(source.getIconUrl());
            if (icon != null)
                ((ImageView) view.findViewById(R.id.source_icon)).setImageBitmap(icon);
        }

        // Install URL on store missing "-s" in protocol currently.
        // So instead generating it from code URL.
        final String installUrl = source.getCodeUrl().replaceFirst("^http", "cloudemoticon");
        installButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(installUrl));
                intent.putExtra("source_name", source.getName());
                startActivityForResult(intent, SettingsActivity.REQUEST_FOR_ADDING_REPOSITORY);
            }
        });

        updateInstallState();
        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.source_actions, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_open_browser) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(source.getStoreUrl()));
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SettingsActivity.REQUEST_FOR_ADDING_REPOSITORY) {
            if (resultCode == AddRepositoryActivity.RESULT_SUCCESS_ADDED) {
                updateInstallState();
                // Poke other fragments to update themselves.
                if (context instanceof SettingsActivity)
                    ((SettingsActivity) context).onActivityResult(requestCode, resultCode, data);
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

}
