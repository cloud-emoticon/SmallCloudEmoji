package org.sorz.lab.smallcloudemoji.fragments;


import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.sorz.lab.smallcloudemoji.R;
import org.sorz.lab.smallcloudemoji.db.DaoSession;
import org.sorz.lab.smallcloudemoji.db.DatabaseHelper;
import org.sorz.lab.smallcloudemoji.db.Source;
import org.sorz.lab.smallcloudemoji.db.SourceDao;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SourceFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SourceFragment extends Fragment {
    private static final String SOURCE_ID = "source_id";
    private Context context;
    private DaoSession daoSession;
    private SourceDao sourceDao;
    private Source source;


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
        daoSession = DatabaseHelper.getInstance(context).getDaoSession();
        sourceDao = daoSession.getSourceDao();

        if (getArguments() != null) {
            long sourceId = getArguments().getLong(SOURCE_ID);
            source = sourceDao.queryBuilder()
                    .where(SourceDao.Properties.Id.eq(sourceId))
                    .unique();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_source, container, false);
        // TODO: Handle null properties.
        ((TextView) view.findViewById(R.id.source_name)).setText(source.getName());
        ((TextView) view.findViewById(R.id.source_introduction)).setText(source.getIntroduction());
        ((TextView) view.findViewById(R.id.source_creator)).setText(source.getCreator());
        ((TextView) view.findViewById(R.id.source_server)).setText(source.getServer());
        return view;
    }


}
