package kr.ac.knu.odego.activity;

import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.github.ksoichiro.android.observablescrollview.ObservableListView;
import com.github.ksoichiro.android.observablescrollview.ScrollState;
import com.github.ksoichiro.android.observablescrollview.ScrollUtils;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import io.realm.Realm;
import io.realm.RealmResults;
import kr.ac.knu.odego.R;
import kr.ac.knu.odego.adapter.BusStopArrInfoListAdapter;
import kr.ac.knu.odego.common.Parser;
import kr.ac.knu.odego.common.RealmTransaction;
import kr.ac.knu.odego.item.BusStop;
import kr.ac.knu.odego.item.Favorite;
import kr.ac.knu.odego.item.Route;
import kr.ac.knu.odego.item.RouteArrInfo;

public class BusStopArrInfoActivity extends ObsvBaseActivity {

    private View mHeaderView;
    private View HeaderContentsView;
    private Toolbar mToolbarView;
    private View mListBackgroundView;
    private ObservableListView mListView;
    private BusStopArrInfoListAdapter mListAdapter;
    private int headerHeight;

    private Realm mRealm;
    private RealmResults<Favorite> favoriteRealmResults;
    private BusStop mBusStop;
    private String busStopId;
    private Route[] routes;

    private int themeColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_busstoparrinfo) ;

        themeColor = ContextCompat.getColor(this, R.color.busstop_arrinfo_header);
        if (Build.VERSION.SDK_INT >= 21)  // 상태바 색상 변경
            getWindow().setStatusBarColor(themeColor);

        mRealm = Realm.getDefaultInstance();
        mListView = (ObservableListView) findViewById(R.id.list);
        mListView.setScrollViewCallbacks(this);
        mListAdapter = new BusStopArrInfoListAdapter(this, mRealm);
        mListView.setAdapter(mListAdapter);
        headerHeight = getResources().getDimensionPixelSize(R.dimen.busstoparrinfo_header_height);
        setPaddingView(mListView, headerHeight);

        // 도착정보 얻기
        busStopId = getIntent().getExtras().getString("busStopId");
        GetBusStopArrinfoAsyncTask getBusStopArrinfoAsyncTask = new GetBusStopArrinfoAsyncTask();
        getBusStopArrinfoAsyncTask.execute();
        mBusStop = mRealm.where(BusStop.class).equalTo("id", busStopId).findFirst();
        favoriteRealmResults = mRealm.where(Favorite.class).equalTo("mBusStop.id", busStopId).findAll(); // 버스정류장 즐겨찾기 여부

        // 헤더의 정류장 정보 등록
        mHeaderView = findViewById(R.id.header);
        HeaderContentsView = mHeaderView.findViewById(R.id.header_contents);
        TextView headerBusStopNo = (TextView) HeaderContentsView.findViewById(R.id.busstop_no);
        TextView headerBusStopName = (TextView) HeaderContentsView.findViewById(R.id.busstop_name);
        headerBusStopNo.setText(mBusStop.getNo());
        headerBusStopName.setText(mBusStop.getName());

        mToolbarView = (Toolbar)findViewById(R.id.toolbar);
        mToolbarView.setTitle(mBusStop.getName());
        //mToolbarView.setSubtitle("서브타이틀");
        // 툴바 색상 설정
        mToolbarView.setBackgroundColor(ScrollUtils.getColorWithAlpha(0, themeColor));
        setSupportActionBar(mToolbarView);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true); // 뒤로가기 아이콘 표시


        // 플로팅액션버튼 설정
        FloatingActionButton refreshBtn = (FloatingActionButton) findViewById(R.id.refresh);
        refreshBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                GetBusStopArrinfoAsyncTask getBusStopArrinfoAsyncTask = new GetBusStopArrinfoAsyncTask();
                getBusStopArrinfoAsyncTask.execute();

                Snackbar.make(view, getString(R.string.refresh_arrinfo_data), Snackbar.LENGTH_SHORT)
                        .setAction("Action", null).show();
            }
        });

        // mListBackgroundView makes ListView's background except header view.
        mListBackgroundView = findViewById(R.id.list_background);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if( mRealm != null)
            mRealm.close();
    }


    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.favorite_menu, menu);
        MenuItem favoriteMenuItem = menu.findItem(R.id.action_favorite);
        if( favoriteRealmResults.size() > 0 ) {
            favoriteMenuItem.setIcon(R.drawable.favorite_on);
            favoriteMenuItem.setChecked(true);
        }
        else {
            favoriteMenuItem.setIcon(R.drawable.favorite_off);
            favoriteMenuItem.setChecked(false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_favorite:
                boolean isChecked = !item.isChecked();
                item.setChecked(isChecked);
                if( isChecked ) {
                    item.setIcon(R.drawable.favorite_on);
                    RealmTransaction.createBusStopFavorite(mRealm, busStopId);
                } else {
                    item.setIcon(R.drawable.favorite_off);
                    RealmTransaction.deleteBusStopFavorite(mRealm, busStopId);
                }

                return true;
            default:
                return false;
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        onScrollChanged(mListView.getCurrentScrollY(), false, false);
    }

    @Override
    public void onScrollChanged(int scrollY, boolean firstScroll, boolean dragging) {
        float alpha = Math.min(1, (float) scrollY / (headerHeight/2) );
        if( alpha == 1 ) {
            mToolbarView.setBackgroundColor(ScrollUtils.getColorWithAlpha(1, themeColor));
            float textAlpha = Math.min(1, (float) (scrollY-headerHeight/2) / (getActionBarSize()/2) );
            mToolbarView.setTitleTextColor(ScrollUtils.getColorWithAlpha(textAlpha, Color.WHITE));
        }
        else {
            mToolbarView.setBackgroundColor(ScrollUtils.getColorWithAlpha(0, themeColor));
            mToolbarView.setTitleTextColor(ScrollUtils.getColorWithAlpha(0, Color.WHITE));
        }
        //mToolbarView.setSubtitleTextColor(ScrollUtils.getColorWithAlpha(alpha, textColor));

        HeaderContentsView.setAlpha(1-alpha);

        // Translate list background
        mHeaderView.setTranslationY(-scrollY / 2);
        mListBackgroundView.setTranslationY(Math.max(0, -scrollY + headerHeight));
    }

    @Override
    public void onDownMotionEvent() {
    }

    @Override
    public void onUpOrCancelMotionEvent(ScrollState scrollState) {
    }

    private class GetBusStopArrinfoAsyncTask extends AsyncTask<Void, String, RouteArrInfo[]> {

        @Override
        protected RouteArrInfo[] doInBackground(Void... params) {
            Parser mParser = Parser.getInstance();
            try {
                RouteArrInfo[] routeArrInfos = mParser.getBusStopArrInfos(busStopId);

                return routeArrInfos;
            } catch (IOException e) {
                publishProgress( getString(R.string.network_error_msg) );
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            Toast.makeText(getBaseContext(), values[0], Toast.LENGTH_LONG);
        }

        @Override
        protected void onPostExecute(RouteArrInfo[] routeArrInfos) {
            if( routeArrInfos == null )
                return;

            if( routes == null ) { // 버스정류장의 노선들 정보가 없을 때
                routes = new Route[routeArrInfos.length];
                for (int i = 0; i < routeArrInfos.length; i++) {
                    RouteArrInfo routeArrInfo = routeArrInfos[i];
                    final String routeId = routeArrInfo.getRouteId();
                    Route mRoute = mRealm.where(Route.class).equalTo("id", routeId).findFirst();
                    if( mRoute == null ) { // 노선정보가 DB에 없을 때... 대구버스홈페이지에서 파싱해서 들고옴
                        ExecutorService executor = Executors.newSingleThreadExecutor();
                        Future<Route> future = executor.submit(new Callable<Route>() {
                            @Override
                            public Route call() throws Exception {
                                Parser mParser = Parser.getInstance();
                                Realm mRealm = null;
                                try {
                                    mRealm = Realm.getDefaultInstance();
                                    return mParser.getRouteById(mRealm, routeId);
                                } finally {
                                    if( mRealm != null)
                                        mRealm.close();
                                }
                            }
                        });
                        try {
                            mRoute = future.get();
                        } catch (InterruptedException e) {
                            Toast.makeText(getBaseContext(), getString(R.string.network_error_msg), Toast.LENGTH_LONG).show();
                        } catch (ExecutionException e) {
                            Toast.makeText(getBaseContext(), getString(R.string.other_err_msg), Toast.LENGTH_LONG).show();
                        }
                    }

                    routes[i] = mRoute;
                    routeArrInfo.setMRoute(mRoute);
                }
            } else {
                for (int i = 0; i < routeArrInfos.length; i++)
                    routeArrInfos[i].setMRoute(routes[i]);
            }

            mListAdapter.setRouteArrInfos(routeArrInfos);
            mListAdapter.notifyDataSetChanged();
        }
    }
}