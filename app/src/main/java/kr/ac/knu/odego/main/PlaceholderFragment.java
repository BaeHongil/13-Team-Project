package kr.ac.knu.odego.main;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SearchView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import kr.ac.knu.odego.R;
import kr.ac.knu.odego.adapter.BusStopListAdapter;
import kr.ac.knu.odego.adapter.FavoriteListAdapter;
import kr.ac.knu.odego.adapter.RouteListAdapter;
import kr.ac.knu.odego.common.Parser;
import kr.ac.knu.odego.item.FavoriteItem;

/**
 * 탭페이지에 들어갈 Fragment 생성.
 */
public class PlaceholderFragment extends Fragment {
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";
    private ExecutorService executorService;

    public PlaceholderFragment() {
        executorService = Executors.newSingleThreadExecutor();
    }

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static PlaceholderFragment newInstance(int sectionNumber) {
        PlaceholderFragment fragment = new PlaceholderFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView;
        switch ( getArguments().getInt(ARG_SECTION_NUMBER) ) {
            case 0:
                rootView = inflater.inflate(R.layout.search_list_fragment, container, false);
                final FavoriteListAdapter mFavoriteListAdapter = new FavoriteListAdapter(getActivity());
                final ListView favoriteListView = (ListView) rootView.findViewById(R.id.list_view);
                favoriteListView.setAdapter(mFavoriteListAdapter);

                SearchView mSearchView = (SearchView) rootView.findViewById(R.id.search_view);
                mSearchView.setVisibility(View.GONE);

                mFavoriteListAdapter.getmFavoriteItemList().add( new FavoriteItem(FavoriteItem.BUS_STOP, "937", "00253") );
                mFavoriteListAdapter.getmFavoriteItemList().add( new FavoriteItem(FavoriteItem.ROUTE, "경북대학교 정문앞", "01053") );
                mFavoriteListAdapter.getmFavoriteItemList().add( new FavoriteItem(FavoriteItem.BUS_STOP, "101", "12053") );

                break;
            case 1:
                rootView = inflater.inflate(R.layout.search_list_fragment, container, false);
                final RouteListAdapter mRouteListAdapter = new RouteListAdapter(getActivity());
                final ListView routeListView = (ListView) rootView.findViewById(R.id.list_view);
                routeListView.setAdapter(mRouteListAdapter);

                SearchView mRouteSearchView = (SearchView) rootView.findViewById(R.id.search_view);
                mRouteSearchView.setQueryHint(getString(R.string.route_search_view_hint));
                mRouteSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    Future mFutrue;

                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        return false;
                    }

                    @Override
                    public boolean onQueryTextChange(final String newText) {
                        if( mFutrue != null && !mFutrue.isDone())
                            mFutrue.cancel(true);

                        if( newText.isEmpty() ) {
                            mRouteListAdapter.getmRouteItemList().clear();
                            mRouteListAdapter.notifyDataSetChanged();

                            return false;
                        }

                        mFutrue = executorService.submit(new Runnable() {
                            @Override
                            public void run() {
                                Parser.getRouteListByNo(mRouteListAdapter.getmRouteItemList(), newText);
                                if( !Thread.interrupted() )
                                    routeListView.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            mRouteListAdapter.notifyDataSetChanged();
                                        }
                                    });
                            }
                        });

                        return false;
                    }
                });

                break;

            case 2:
                rootView = inflater.inflate(R.layout.search_list_fragment, container, false);
                final BusStopListAdapter mBusstopListAdapter = new BusStopListAdapter(getActivity());
                final ListView busStopListView = (ListView) rootView.findViewById(R.id.list_view);
                busStopListView.setAdapter(mBusstopListAdapter);

                SearchView mBusStopSearchView = (SearchView) rootView.findViewById(R.id.search_view);
                mBusStopSearchView.setQueryHint(getString(R.string.busstop_search_view_hint));
                mBusStopSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    Future mFutrue;

                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        return false;
                    }

                    @Override
                    public boolean onQueryTextChange(final String newText) {
                        if( mFutrue != null && !mFutrue.isDone())
                            mFutrue.cancel(true);

                        if( newText.isEmpty() ) {
                            mBusstopListAdapter.getmBusStopItemList().clear();
                            mBusstopListAdapter.notifyDataSetChanged();

                            return false;
                        }

                        mFutrue = executorService.submit(new Runnable() {
                            @Override
                            public void run() {
                                Parser.getBusStopListByWord(mBusstopListAdapter.getmBusStopItemList(), newText);
                                if( !Thread.interrupted() )
                                    busStopListView.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            mBusstopListAdapter.notifyDataSetChanged();
                                        }
                                    });
                            }
                        });
                        return false;
                    }
                });

                break;
            default:
                rootView = inflater.inflate(R.layout.blank_fragment, container, false);
                Log.d("blank", container.toString());
                break;
        }

        return rootView;
    }
}