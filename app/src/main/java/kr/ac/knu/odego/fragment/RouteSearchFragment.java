package kr.ac.knu.odego.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.TextView;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;
import kr.ac.knu.odego.R;
import kr.ac.knu.odego.activity.BeaconActivity;
import kr.ac.knu.odego.activity.BusPosInfoActivity;
import kr.ac.knu.odego.adapter.RouteListAdapter;
import kr.ac.knu.odego.item.Route;

public class RouteSearchFragment extends Fragment {
    private Realm mRealm;
    private RecyclerView routeListView;
    private RouteListAdapter mRouteListAdapter;

    private RelativeLayout noContentsLayout;
    private TextView noMessageTextView;
    private String noHistoryMessage;
    private String noResultMessage;

    private RealmResults<Route> historyResults;
    private int historyNum;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        // 기타 변수 설정
        noHistoryMessage = getString(R.string.no_history);
        noResultMessage = getString(R.string.no_result);
        historyNum = getResources().getInteger(R.integer.history_num);
    }

    @Override
    public void onStart() {
        super.onStart();
        mRealm = Realm.getDefaultInstance();
        historyResults = mRealm.where(Route.class)
                .notEqualTo("historyIndex", 0)
                .findAllSorted("historyIndex", Sort.DESCENDING);

        if( historyResults.size() == 0 ) {
            routeListView.setVisibility(View.GONE);
            noContentsLayout.setVisibility(View.VISIBLE);
        } else {
            mRouteListAdapter.setDataLimit(historyNum);
            mRouteListAdapter.updateData(historyResults);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if(mRealm != null)
            mRealm.close();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_search_list, container, false);

        // RecylcerView 생성
        routeListView = (RecyclerView) rootView.findViewById(R.id.recycler_view);
        routeListView.setLayoutManager(new LinearLayoutManager(getContext()));
        mRouteListAdapter = new RouteListAdapter(this, null, false);
        routeListView.setAdapter(mRouteListAdapter);

        // 내용없는 레이아웃
        noContentsLayout = (RelativeLayout) rootView.findViewById(R.id.no_contents_layout);
        noMessageTextView = (TextView) noContentsLayout.findViewById(R.id.no_message);

        // SearchView
        SearchView mRouteSearchView = (SearchView) rootView.findViewById(R.id.search_view);
        mRouteSearchView.setQueryHint(getString(R.string.route_search_view_hint));
        mRouteSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(final String newText) {
                RealmResults<Route> results = null;
                if( newText.isEmpty() ) {
                    results = historyResults;
                    mRouteListAdapter.setDataLimit(historyNum);
                    noMessageTextView.setText(noHistoryMessage);
                } else {
                    results = mRealm.where(Route.class)
                            .contains("no", newText)
                            .or()
                            .contains("direction", newText)
                            .findAll(); // 정류소 검색 쿼리
                    noMessageTextView.setText(noResultMessage);
                }

                if( results.size() == 0 ) {
                    routeListView.setVisibility(View.GONE);
                    noContentsLayout.setVisibility(View.VISIBLE);
                } else {
                    mRouteListAdapter.resetDataLimit();
                    mRouteListAdapter.updateData(results);
                    routeListView.setVisibility(View.VISIBLE);
                    noContentsLayout.setVisibility(View.GONE);
                }

                return false;
            }
        });

        return rootView;
    }

    public void addRouteHistory(final String routeId) {
        Intent intent = new Intent( getContext(), BusPosInfoActivity.class);
        intent.putExtra("routeId", routeId);
        startActivity(intent);

        mRealm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                Route mRoute = realm.where(Route.class).equalTo("id", routeId).findFirst();
                int maxIndex = realm.where(Route.class).max("historyIndex").intValue();
                mRoute.setHistoryIndex(maxIndex + 1);
            }
        });
    }

}