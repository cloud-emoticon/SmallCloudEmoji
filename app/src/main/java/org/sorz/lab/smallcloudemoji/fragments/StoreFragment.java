package org.sorz.lab.smallcloudemoji.fragments;

import android.app.ListFragment;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;


/**
 * A fragment representing a list of source.
 */
public class StoreFragment extends ListFragment {
    private static final String STORE_URL = "storeUrl";
    private String storeUrl;

    public static StoreFragment newInstance(String storeUrl) {
        StoreFragment fragment = new StoreFragment();
        Bundle args = new Bundle();
        args.putString(STORE_URL, storeUrl);
        fragment.setArguments(args);
        return fragment;
    }

    public StoreFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            storeUrl = getArguments().getString(storeUrl);
        }

        // TODO: Change Adapter to display your content
    }


    @Override
    public void onDetach() {
        super.onDetach();
    }


    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
    }

}
