package kr.ac.knu.odego.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmResults;
import io.realm.Sort;
import kr.ac.knu.odego.R;
import kr.ac.knu.odego.adapter.BeaconArrInfoListAdapter;
import kr.ac.knu.odego.item.BeaconArrInfo;

public class TheOtherFragment extends Fragment {
    private Realm mRealm;
    private BeaconArrInfoListAdapter mBeaconArrInfoListAdapter;
    private RealmResults<BeaconArrInfo> results;
    private RealmChangeListener<RealmResults<BeaconArrInfo>> realmChangeListener;
    private RelativeLayout noContentsLayout;

    @Override
    public void onStart() {
        super.onStart();
        mRealm = Realm.getDefaultInstance();
        results = mRealm.where(BeaconArrInfo.class).findAllSorted("index", Sort.DESCENDING);
        setVisibilityNoContentsLayout(results);
        realmChangeListener = new RealmChangeListener<RealmResults<BeaconArrInfo>>() {
            @Override
            public void onChange(RealmResults<BeaconArrInfo> results) {
                setVisibilityNoContentsLayout(results);
            }
        };
        mBeaconArrInfoListAdapter.updateData(results);
    }

    @Override
    public void onStop() {
        super.onStop();
        if( results != null && realmChangeListener != null )
            results.removeChangeListener(realmChangeListener);
        if(mRealm != null)
            mRealm.close();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_list, container, false);

        RecyclerView BeaconArrInfoRecyclerView = (RecyclerView) rootView.findViewById(R.id.recycler_view);
        BeaconArrInfoRecyclerView.setLayoutManager( new LinearLayoutManager(getContext()) );
        mBeaconArrInfoListAdapter = new BeaconArrInfoListAdapter(getContext(), null, true);
        BeaconArrInfoRecyclerView.setAdapter(mBeaconArrInfoListAdapter);

        noContentsLayout = (RelativeLayout) rootView.findViewById(R.id.no_contents_layout);

        return rootView;
    }

    public void setVisibilityNoContentsLayout(RealmResults<BeaconArrInfo> results) {
        if( results.size() == 0 )
            noContentsLayout.setVisibility(View.VISIBLE);
        else
            noContentsLayout.setVisibility(View.GONE);
    }
}