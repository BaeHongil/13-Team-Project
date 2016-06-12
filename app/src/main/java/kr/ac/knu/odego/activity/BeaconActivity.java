package kr.ac.knu.odego.activity;

import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.ksoichiro.android.observablescrollview.ScrollUtils;
import com.google.firebase.iid.FirebaseInstanceId;

import java.io.IOException;

import io.realm.Realm;
import kr.ac.knu.odego.R;
import kr.ac.knu.odego.adapter.BeaconListAdapter;
import kr.ac.knu.odego.common.Parser;
import kr.ac.knu.odego.common.RealmTransaction;
import kr.ac.knu.odego.common.RouteType;
import kr.ac.knu.odego.interfaces.BeaconBusDestListener;
import kr.ac.knu.odego.item.BeaconArrInfo;
import kr.ac.knu.odego.item.NotiReqMsg;

/**
 * Created by Brick on 2016-06-09.
 */
public class BeaconActivity extends AppCompatActivity{

    private View mHeaderView;
    private View HeaderContentsView;
    private Toolbar mToolbarView;;
    private ListView mListView;
    private BeaconListAdapter mListAdapter;
    private TextView busDest;

    private Parser mParser = Parser.getInstance();
    private Realm mRealm;
    private BeaconArrInfo mBeaconArrInfo;
    private LinearLayout progressLayout;

    private int themeColor;
    private String fcmToken = FirebaseInstanceId.getInstance().getToken();
    private Handler mHandler = new BeaconActivityHandler();
    private NotiReqMsg mNotiReqMsg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_beacon);

        mRealm = Realm.getDefaultInstance();
        int beaconArrInfoIndex = mRealm.where(BeaconArrInfo.class).max("index").intValue();
        mBeaconArrInfo = mRealm.where(BeaconArrInfo.class).equalTo("index", beaconArrInfoIndex).findFirst();
        mNotiReqMsg = new NotiReqMsg(mBeaconArrInfo.getMRoute().getId(), mBeaconArrInfo.isForward(),
                mBeaconArrInfo.getBusId(), mBeaconArrInfo.getDestIndex(), 2, fcmToken);

        // 프로그레스바 레이아웃 얻기
        progressLayout = (LinearLayout) findViewById(R.id.progress_layout);
        TextView progressText = (TextView) findViewById(R.id.progress_text);
        progressText.setText(R.string.refreshing_busposinfo_data);

        // 도착정보 얻기
        mListView = (ListView) findViewById(R.id.list);
        setAdapter();
        GetBusPosAsyncTask getBusPosAsyncTask = new GetBusPosAsyncTask();
        getBusPosAsyncTask.execute(mBeaconArrInfo.getMRoute().getId(), mBeaconArrInfo.isForward(), mBeaconArrInfo.getBusId());

        mHeaderView = findViewById(R.id.header);
        HeaderContentsView = mHeaderView.findViewById(R.id.header_contents);
        setHeaderData();

        // 테마색상 노선유형에 따라 설정
        String routeType = mBeaconArrInfo.getMRoute().getType();
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
        mToolbarView.setTitle("");
        mToolbarView.bringToFront();
        // 툴바 색상 설정
        mToolbarView.setBackgroundColor(ScrollUtils.getColorWithAlpha(0, themeColor));
        setSupportActionBar(mToolbarView);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true); // 뒤로가기 아이콘 표시

        // 플로팅액션버튼 설정
        FloatingActionButton refreshBtn = (FloatingActionButton) findViewById(R.id.refresh);
        refreshBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                GetBusPosAsyncTask getBusPosAsyncTask = new GetBusPosAsyncTask();
                getBusPosAsyncTask.execute(mBeaconArrInfo.getMRoute().getId(), mBeaconArrInfo.isForward(), mBeaconArrInfo.getBusId());

                Snackbar.make(view, getString(R.string.refresh_busposinfo_data), Snackbar.LENGTH_SHORT)
                        .setAction("Action", null).show();
            }
        });
    }

    private void setAdapter() {
        mListAdapter = new BeaconListAdapter(this, mBeaconArrInfo.getBusStops(), mRealm,
                mBeaconArrInfo.getMRoute().getType(), mBeaconArrInfo.getBusId(),
                mBeaconArrInfo.getStartIndex(), mBeaconArrInfo.getDestIndex());
        mListAdapter.setBusDestListener(new BeaconBusDestListener() {
            @Override
            public void onChangeBusDest(String text, int destIndex) {
                setDestTextView(text);

                // destIndex가 -1일 때
                if( destIndex == -1 ) {
                    if( mBeaconArrInfo.getDestIndex() != -1 )
                        RealmTransaction.deleteDestIndex(mRealm, mBeaconArrInfo.getIndex(), fcmToken, mHandler);
                    return;
                }

                // destIndex가 -1이 아닐 때
                if( mBeaconArrInfo.getDestIndex() == -1 ) {
                    mNotiReqMsg.setDestIndex(destIndex);
                    RealmTransaction.createDestIndex(mRealm, mBeaconArrInfo.getIndex(), mNotiReqMsg, mHandler);
                } else
                    RealmTransaction.modifyDestIndex(mRealm, mBeaconArrInfo.getIndex(), fcmToken, destIndex, mHandler);
            }
        });
        mListView.setAdapter(mListAdapter);
    }

    private void setHeaderData() {
        TextView headerRouteType = (TextView) HeaderContentsView.findViewById(R.id.route_type);
        TextView headerRouteName = (TextView) HeaderContentsView.findViewById(R.id.route_name);
        TextView headerBusIdNo = (TextView) HeaderContentsView.findViewById(R.id.bus_id_no);
        busDest = (TextView) findViewById( R.id.bus_goal );

        headerRouteType.setText(mBeaconArrInfo.getMRoute().getType());
        headerRouteName.setText(mBeaconArrInfo.getMRoute().getNo());
        headerBusIdNo.setText(mBeaconArrInfo.getBusId());
    }

    public void setDestTextView(CharSequence dest) {
        busDest.setText( dest );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if( mRealm != null)
            mRealm.close();
    }

    private class GetBusPosAsyncTask extends AsyncTask<Object, String, Integer> {
        @Override
        protected void onPreExecute() {
            mListView.setVisibility(View.GONE);
            progressLayout.setVisibility(View.VISIBLE);
        }

        @Override
        protected Integer doInBackground(Object... params) {
            String routeId = (String) params[0];
            Boolean isForward = (Boolean) params[1];
            String busId = (String) params[2];
            try {
                int curIndex = mParser.getBusPosByBusId(routeId, isForward, busId);
                return curIndex;
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
        protected void onPostExecute(Integer curIndex) {
            if( curIndex != null ) {
                mListAdapter.setCurIndex(curIndex);

                progressLayout.setVisibility(View.GONE);
                mListView.setSelection(curIndex - 2);
                mListView.setVisibility(View.VISIBLE);
            }
        }
    }

    private class BeaconActivityHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            String text = (String) msg.obj;
            Toast.makeText(getBaseContext(), text, Toast.LENGTH_LONG);
        }
    }
}
