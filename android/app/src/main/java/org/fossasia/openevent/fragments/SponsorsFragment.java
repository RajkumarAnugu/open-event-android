package org.fossasia.openevent.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.fossasia.openevent.OpenEventApp;
import org.fossasia.openevent.R;
import org.fossasia.openevent.adapters.SponsorsListAdapter;
import org.fossasia.openevent.dbutils.DataDownloadManager;
import org.fossasia.openevent.dbutils.DbSingleton;
import org.fossasia.openevent.events.SponsorDownloadEvent;
import org.fossasia.openevent.utils.NetworkUtils;
import org.fossasia.openevent.utils.RecyclerItemClickListener;

import timber.log.Timber;

/**
 * Created by MananWason on 05-06-2015.
 */
public class SponsorsFragment extends Fragment {
    private RecyclerView sponsorsRecyclerView;

    private SponsorsListAdapter sponsorsListAdapter;

    private SwipeRefreshLayout swipeRefreshLayout;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        final View view = inflater.inflate(R.layout.list_sponsors, container, false);
        Bus bus = OpenEventApp.getEventBus();
        bus.register(this);
        sponsorsRecyclerView = (RecyclerView) view.findViewById(R.id.list_sponsors);
        final DbSingleton dbSingleton = DbSingleton.getInstance();

        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.sponsor_swipe_refresh);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (NetworkUtils.haveNetworkConnection(getActivity())) {
                    DataDownloadManager.getInstance().downloadSponsors();
                } else {
                    OpenEventApp.getEventBus().post(new SponsorDownloadEvent(true));
                }
            }
        });
        sponsorsListAdapter = new SponsorsListAdapter(dbSingleton.getSponsorList());
        sponsorsRecyclerView.setAdapter(sponsorsListAdapter);
        sponsorsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        sponsorsRecyclerView.addOnItemTouchListener(new RecyclerItemClickListener(view.getContext(), new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View itemView, int position) {
                String sponsorUrl = dbSingleton.getSponsorList().get(position).getUrl();
                if (!sponsorUrl.startsWith("http") || !sponsorUrl.startsWith("https")) {
                    sponsorUrl = "http://" + sponsorUrl;
                }
                if (Patterns.WEB_URL.matcher(sponsorUrl).matches()) {
                    Intent sponsorsIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(sponsorUrl));
                    startActivity(sponsorsIntent);
                } else {
                    Snackbar.make(view, R.string.invalid_url, Snackbar.LENGTH_LONG).show();
                }
            }
        }));

        return view;
    }


    @Subscribe
    public void sponsorDownloadDone(SponsorDownloadEvent event) {

        swipeRefreshLayout.setRefreshing(false);
        if (event.isState()) {
            sponsorsListAdapter.refresh();
            Timber.d("Refresh done");

        } else {
            if (getActivity() != null) {
                Snackbar.make(getView(), getActivity().getString(R.string.refresh_failed), Snackbar.LENGTH_LONG).show();
            }
            Timber.d("Refresh not done");

        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        super.onCreateOptionsMenu(menu, inflater);
    }
}
