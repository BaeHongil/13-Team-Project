package kr.ac.knu.odego.main;

import android.content.Context;
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
import kr.ac.knu.odego.adapter.BusStopListAdapter;
import kr.ac.knu.odego.item.BusStop;

public class BusStopSearchFragment extends Fragment {
    private Realm mRealm;
    private BusStopListAdapter mBusstopListAdapter;
    private RecyclerView busStopListView;

    private RelativeLayout noContentsLayout;
    private TextView noMessageTextView;
    private String noHistoryMessage;
    private String noResultMessage;

    private RealmResults<BusStop> historyResults;
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
        historyResults = mRealm.where(BusStop.class)
                .notEqualTo("historyIndex", 0)
                .findAllSorted("historyIndex", Sort.DESCENDING);

        if( historyResults.size() == 0 ) {
            busStopListView.setVisibility(View.GONE);
            noContentsLayout.setVisibility(View.VISIBLE);
        } else {
            mBusstopListAdapter.setDataLimit(historyNum);
            mBusstopListAdapter.updateData(historyResults);
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
        busStopListView = (RecyclerView) rootView.findViewById(R.id.recycler_view);
        busStopListView.setLayoutManager( new LinearLayoutManager(getContext() ));
        mBusstopListAdapter = new BusStopListAdapter(this, null, false);
        busStopListView.setAdapter(mBusstopListAdapter);

        // 내용없는 레이아웃
        noContentsLayout = (RelativeLayout) rootView.findViewById(R.id.no_contents_layout);
        noMessageTextView = (TextView) noContentsLayout.findViewById(R.id.no_message);


        // SearchView
        SearchView mBusStopSearchView = (SearchView) rootView.findViewById(R.id.search_view);
        mBusStopSearchView.setQueryHint(getString(R.string.busstop_search_view_hint));
        mBusStopSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                RealmResults<BusStop> results = null;
                if( newText.isEmpty() ) {
                    results = historyResults;
                    mBusstopListAdapter.setDataLimit(historyNum);
                    noMessageTextView.setText(noHistoryMessage);
                }
                else  {
                    results = mRealm.where(BusStop.class)
                            .contains("name", newText)
                            .or()
                            .contains("no", newText)
                            .findAll(); // 정류소 검색 쿼리
                    noMessageTextView.setText(noResultMessage);
                }
                if( results.size() == 0 ) {
                    busStopListView.setVisibility(View.GONE);
                    noContentsLayout.setVisibility(View.VISIBLE);
                } else {
                    mBusstopListAdapter.resetDataLimit();
                    mBusstopListAdapter.updateData(results);
                    busStopListView.setVisibility(View.VISIBLE);
                    noContentsLayout.setVisibility(View.GONE);
                }

                return false;
            }
        });
        return rootView;
    }

    public void addBusStopHistory(final String busStopId) {
        mRealm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                BusStop mBusStop = realm.where(BusStop.class).equalTo("id", busStopId).findFirst();
                int maxIndex = realm.where(BusStop.class).max("historyIndex").intValue();
                mBusStop.setHistoryIndex(maxIndex + 1);
            }
        });
    }
}