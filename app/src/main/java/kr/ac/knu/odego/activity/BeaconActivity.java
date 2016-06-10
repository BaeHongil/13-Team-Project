package kr.ac.knu.odego.activity;

import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.ksoichiro.android.observablescrollview.ScrollUtils;

import java.io.IOException;

import io.realm.Realm;
import kr.ac.knu.odego.OdegoApplication;
import kr.ac.knu.odego.R;
import kr.ac.knu.odego.adapter.BeaconListAdapter;
import kr.ac.knu.odego.common.Parser;
import kr.ac.knu.odego.common.RouteType;
import kr.ac.knu.odego.interfaces.BeaconSetGoalListener;
import kr.ac.knu.odego.item.BusPosInfo;
import kr.ac.knu.odego.item.BusStop;
import kr.ac.knu.odego.item.Route;
import lombok.Getter;
import lombok.Setter;

/**
 * Created by Brick on 2016-06-09.
 */
public class BeaconActivity extends AppCompatActivity{

    private View mHeaderView;
    private View HeaderContentsView;
    private View mListBackgroundView;
    private ListView mListView;
    private Toolbar mToolbarView;
    private BeaconListAdapter mListAdapter;

    private TextView bus_goal;

    private Parser mParser = Parser.getInstance();
    private Realm mRealm;
    private Route mRoute;
    private String routeId = "3000306000";
    private BusStop[] busStops;

    private String busIdNo = "1031";

    private int themeColor;
    private boolean isForward = true;
    private BusPosInfo[] busPosInfos;

    private int presentPosition=-1;


    BeaconSetGoalListener beaconSetGoalListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_beacon);

        mRealm = Realm.getDefaultInstance();
        routeId = getIntent().getExtras().getString("routeId");
        mRoute = mRealm.where(Route.class).equalTo("id", routeId).findFirst();

        mListView = (ListView) findViewById(R.id.list);

        beaconSetGoalListener = new BeaconSetGoalListener() {
            @Override
            public void setGoalTextView(String text) {
                setGoal(text);
            }
        };

        // 도착정보 얻기
        GetBusPosInfoAsyncTask getBusPosinfoAsyncTask = new GetBusPosInfoAsyncTask();
        getBusPosinfoAsyncTask.execute(isForward);

        mHeaderView = findViewById(R.id.header);
        HeaderContentsView = mHeaderView.findViewById(R.id.header_contents);
        // 현재 노선상세정보 있는지 확인후 노선정보 등록
        if( mRoute.getUpdatedDetail() != null && OdegoApplication.isToday( mRoute.getUpdatedDetail() ) )
            setHeaderDate();
        else {
            GetRouteDetailInfoAsyncTask getRouteDetailInfoAsyncTask = new GetRouteDetailInfoAsyncTask();
            getRouteDetailInfoAsyncTask.execute();
        }



        // 테마색상 노선유형에 따라 설정
        String routeType = mRoute.getType();
        if (RouteType.MAIN.getName().equals( routeType ))
            themeColor = ContextCompat.getColor(this, R.color.main_bus);
        else if (RouteType.BRANCH.getName().equals( routeType ))
            themeColor = ContextCompat.getColor(this, R.color.branch_bus);
        else if (RouteType.EXPRESS.getName().equals( routeType ))
            themeColor = ContextCompat.getColor(this, R.color.express_bus);
        else if (RouteType.CIRCULAR.getName().equals( routeType ))
            themeColor = ContextCompat.getColor(this, R.color.circular_bus);

        if (Build.VERSION.SDK_INT >= 21)  // 상태바 색상 변경
            getWindow().setStatusBarColor(themeColor);
        mHeaderView.setBackgroundColor(themeColor);

        mToolbarView = (Toolbar)findViewById(R.id.toolbar);
        mToolbarView.setTitle(" ");
        mToolbarView.bringToFront();
        // 툴바 색상 설정
        mToolbarView.setBackgroundColor(ScrollUtils.getColorWithAlpha(0, themeColor));
        setSupportActionBar(mToolbarView);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true); // 뒤로가기 아이콘 표시


        // mListBackgroundView makes ListView's background except header view.
        mListBackgroundView = findViewById(R.id.list_background);

        setAdapter();

    }

    private void setAdapter()
    {
        mListAdapter = new BeaconListAdapter(this, mRealm, mRoute.getType(), busIdNo, presentPosition);
        mListView.setAdapter(mListAdapter);
        mListAdapter.setSetGoalListener(beaconSetGoalListener);

    }



    private void setHeaderDate() {

        TextView headerRouteType = (TextView) HeaderContentsView.findViewById(R.id.route_type);
        TextView headerRouteName = (TextView) HeaderContentsView.findViewById(R.id.route_name);
        TextView headerBusIdNo = (TextView) HeaderContentsView.findViewById(R.id.bus_id_no);
        bus_goal = (TextView) findViewById( R.id.bus_goal );


        headerRouteType.setText(mRoute.getType());
        headerRouteName.setText(mRoute.getNo());
        headerBusIdNo.setText(busIdNo);

    }

    public void setGoal(CharSequence goal)
    {
        bus_goal.setText( goal );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if( mRealm != null)
            mRealm.close();
    }




    private class GetBusPosInfoAsyncTask extends AsyncTask<Boolean, String, BusPosInfo[]> {

        @Override
        protected BusPosInfo[] doInBackground(Boolean... params) {
            boolean isForward = params[0];
            try {
                busPosInfos = mParser.getBusPosInfos(routeId, isForward);

                // 포지션 정하기
                String busNums = null;
                String busIdNm = null;
                for (int i = 0; i < busPosInfos.length; i++) {
                    if (busPosInfos[i].getBusId() != null) {
                        busNums = busPosInfos[i].getBusId().toString();
                        busIdNm = busNums.substring(busNums.length() - 4, busNums.length());
                        if (busIdNm.equals(busIdNo)) {
                            presentPosition = i;
                            break;
                        }
                    }
                }



                return busPosInfos;
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
        protected void onPostExecute(BusPosInfo[] busPosInfos) {
            if (busPosInfos == null)
                return;

            if (busStops == null) { // 버스정류장의 노선들 정보가 없을 때
                busStops = new BusStop[busPosInfos.length];
                for (int i = 0; i < busPosInfos.length; i++) {
                    BusPosInfo busPosInfo = busPosInfos[i];
                    String busStopId = busPosInfo.getBusStopId();
                    BusStop mBusStop = mRealm.where(BusStop.class).equalTo("id", busStopId).findFirst();

                    busStops[i] = mBusStop;
                    busPosInfo.setMBusStop(mBusStop);
                }
            } else {
                for (int i = 0; i < busPosInfos.length; i++)
                    busPosInfos[i].setMBusStop(busStops[i]);
            }

            mListAdapter.setBusPosInfos(busPosInfos, presentPosition);
            mListAdapter.notifyDataSetChanged();
            mListView.setSelection(presentPosition);

        }
    }

    private class GetRouteDetailInfoAsyncTask extends AsyncTask<Void, String, Route> {

        @Override
        protected Route doInBackground(Void... params) {
            Realm mRealm = null;
            try {
                mRealm = Realm.getDefaultInstance();
                mParser.getRouteDetailInfo(mRealm, routeId);
            } catch (IOException e) {
                publishProgress( getString(R.string.network_error_msg) );
            } finally {
                if(mRealm != null)
                    mRealm.close();
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            Toast.makeText(getBaseContext(), values[0], Toast.LENGTH_LONG);
        }

        @Override
        protected void onPostExecute(Route route) {
            setHeaderDate();
        }
    }



}
