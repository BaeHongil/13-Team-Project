package kr.ac.knu.odego.activity;

import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.lang.reflect.Field;
import java.text.Format;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import io.realm.Realm;
import kr.ac.knu.odego.R;
import kr.ac.knu.odego.adapter.SectionsPagerAdapter;
import kr.ac.knu.odego.common.Parser;
import kr.ac.knu.odego.fragment.BusStopSearchFragment;
import kr.ac.knu.odego.fragment.FavoriteFragment;
import kr.ac.knu.odego.fragment.RouteSearchFragment;
import kr.ac.knu.odego.fragment.TheOtherFragment;
import kr.ac.knu.odego.item.BusStop;
import kr.ac.knu.odego.item.Route;
import kr.ac.knu.odego.service.BeaconService;
import lombok.Getter;
import lombok.Setter;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, ViewPager.OnPageChangeListener {
    private CoordinatorLayout mContentsLayout;
    private ViewPager mViewPager;
    private BluetoothAdapter mBtAdapter;

    private Realm mRealm;

    private BeaconService mBeaconService;
    private boolean mBound = false;
    private boolean isStartSplash = false;

    private CharSequence tab_main1, tab_main2, tab_main3, tab_main4;
    private CharSequence tab_off_main1, tab_off_main2, tab_off_main3, tab_off_main4;
    private CharSequence mPageMark;
    private int mPrevPosition = 0;

    private SectionsPagerAdapter mSectionsPagerAdapter;
    TabLayout tabLayout;

    DataSetObserver mObserver = new DataSetObserver() {
        @Override
        public void onChanged() {
            super.onChanged();
        }

        @Override
        public void onInvalidated() {
            super.onInvalidated();
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mContentsLayout = (CoordinatorLayout) findViewById(R.id.contents_layout);
        // 탭 메뉴 세팅
        tab_main1 = ViewUtil.iconText(ViewUtil.drawable(this, R.drawable.main_on_btn1),"");
        tab_main2 = ViewUtil.iconText(ViewUtil.drawable(this, R.drawable.main_on_btn2),"");
        tab_main3 = ViewUtil.iconText(ViewUtil.drawable(this, R.drawable.main_on_btn3),"");
        tab_main4 = ViewUtil.iconText(ViewUtil.drawable(this, R.drawable.main_on_btn4),"");

        tab_off_main1 = ViewUtil.iconText(ViewUtil.drawable(this, R.drawable.main_off_btn1),"");
        tab_off_main2 = ViewUtil.iconText(ViewUtil.drawable(this, R.drawable.main_off_btn2),"");
        tab_off_main3 = ViewUtil.iconText(ViewUtil.drawable(this, R.drawable.main_off_btn3),"");
        tab_off_main4 = ViewUtil.iconText(ViewUtil.drawable(this, R.drawable.main_off_btn4),"");

        // fragment 탭 페이지 설정
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        mSectionsPagerAdapter.registerDataSetObserver(mObserver);

        mSectionsPagerAdapter.addFragment(new FavoriteFragment(),
                tab_main1
        );
        mSectionsPagerAdapter.addFragment(new RouteSearchFragment(),
                tab_off_main2
        );
        mSectionsPagerAdapter.addFragment(new BusStopSearchFragment(),
                tab_off_main3
        );
        mSectionsPagerAdapter.addFragment(new TheOtherFragment(),
                tab_off_main4
        );



        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);


        mViewPager.addOnPageChangeListener(this);

        tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        /*// 플로팅액션버튼 설정
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.refresh);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });*/

        // 좌측 네이게이션 설정
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // 블루투스 어뎁터 설정
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        // mBtAdapter.enable();

        // realm 초기화
        mRealm = Realm.getDefaultInstance();

        // Parser로 DB 생성
        new DataBaseCreateAsyncTask().execute();

    }

    @Override
    protected void onStart() {
        super.onStart();
        if( !isStartSplash ) {
            // splash 띄우기
            startActivity(new Intent(this, SplashActivity.class));
            isStartSplash = true;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if( mRealm != null )
            mRealm.close();
        if( mBound ) {
            mBeaconService.setTimeOut(5 * 60 * 1000L);
            unbindService(mConnection);
            mBound = false;
        }
    }

    /**
     * 뒤로가기 버튼 Override
     */
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    /**
     * 액션바 옵션메뉴 Override
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        MenuItem btMenuItem = menu.findItem(R.id.action_bluetooth);

        if( mBtAdapter != null ) {
            if (mBtAdapter.isEnabled())
                btMenuItem.setIcon(R.drawable.bt_on);
            else
                btMenuItem.setIcon(R.drawable.bt_off);
        }

        return true;
    }

    /**
     * 액션바 옵션메뉴 핸들러
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_bluetooth) {
            if(mBtAdapter != null) {
                if ( mBtAdapter.isEnabled() ) {
                    mBtAdapter.disable();
                    Toast.makeText(this, "Bluetooth를 끕니다", Toast.LENGTH_SHORT).show();
                    item.setIcon(R.drawable.bt_off);
                }
                else {
                    mBtAdapter.enable();
                    Toast.makeText(this, "Bluetooth를 켭니다", Toast.LENGTH_SHORT).show();
                    item.setIcon(R.drawable.bt_on);
                }
            }

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * 좌측 네비게이션메뉴 핸들러
     */
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position){


        //이전 페이지에 해당하는 페이지 표시 이미지 변경
        switch ( mPrevPosition ) {
            case 0:
                mSectionsPagerAdapter.getMFragmentTitleList().set(mPrevPosition, tab_off_main1);
                break;
            case 1:
                mSectionsPagerAdapter.getMFragmentTitleList().set(mPrevPosition, tab_off_main2);
                break;
            case 2:
                mSectionsPagerAdapter.getMFragmentTitleList().set(mPrevPosition, tab_off_main3);
                break;
            case 3:
                mSectionsPagerAdapter.getMFragmentTitleList().set(mPrevPosition, tab_off_main4);
                break;
        }

        //현재 페이지에 해당하는 페이지 표시 이미지 변경
        switch ( position ) {
            case 0:
                mSectionsPagerAdapter.getMFragmentTitleList().set(position, tab_main1);
                break;
            case 1:
                mSectionsPagerAdapter.getMFragmentTitleList().set(position, tab_main2);
                break;
            case 2:
                mSectionsPagerAdapter.getMFragmentTitleList().set(position, tab_main3);
                break;
            case 3:
                mSectionsPagerAdapter.getMFragmentTitleList().set(position, tab_main4);
                break;
        }

        mPrevPosition = position;                //이전 포지션 값을 현재로 변경
        mSectionsPagerAdapter.notifyDataSetChanged();    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }


    /* 내부 클래스 시작 */

    /**
     * Fragement Page 어뎁터
     */

    /**
     * 버스정류장, 노선 DB 생성 AsyncTask
     */
    private class DataBaseCreateAsyncTask extends AsyncTask<Void, String, Boolean> {
        @Override
        protected Boolean doInBackground(Void... params) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Realm mRealm = null;
            try {
                mRealm = Realm.getDefaultInstance();
                boolean isBusStopDB = true;
                boolean isRouteDB = true;
                if (mRealm.where(BusStop.class).count() == 0) isBusStopDB = false;
                if (mRealm.where(Route.class).count() == 0) isRouteDB = false;
                if (isBusStopDB && isRouteDB) return false; // DB 둘 다 있을 때는 바로 끝내기

                publishProgress(null);
                Parser mParser = Parser.getInstance();
                if (!isBusStopDB && !isRouteDB) { // DB 둘 다 없을 때는 Thread 하나 더 생성해서 DB생성
                    Future future = executor.submit(new CreateBusDbCallable());
                    mParser.createRouteDB(mRealm, false);
                    future.get();
                } else if ( !isBusStopDB ) { // BusStop DB만 없을 때
                    mParser.createBusStopDB(mRealm, false);
                } else // Route DB만 없을 때
                    mParser.createRouteDB(mRealm, false);
            } catch (IOException IOException) {
                publishProgress( getBaseContext().getString(R.string.network_error_msg) );
                return false;
            } catch (InterruptedException | ExecutionException e) {
                publishProgress( getBaseContext().getString(R.string.other_err_msg) );
                return false;
            } finally {
                if (mRealm != null)
                    mRealm.close();
            }

            return true;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            if(values != null) { // false면 네트워크 오류
                String toastMsg = values[0];
                Toast.makeText(getBaseContext(), toastMsg, Toast.LENGTH_LONG);
                return;
            }
            View.inflate(getBaseContext(), R.layout.progress, mContentsLayout);
            mViewPager.setVisibility(View.INVISIBLE);
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            if( aBoolean ) {
                mContentsLayout.removeView( mContentsLayout.findViewById(R.id.progress_layout) );
                mViewPager.setVisibility(View.VISIBLE);
            }

            if( mBtAdapter != null && mBtAdapter.isEnabled() ) {
                Intent intent = new Intent(getBaseContext(), BeaconService.class);
                startService(intent);
                bindService(intent, mConnection, BIND_AUTO_CREATE);
            }
        }
    }

    // 버스DB 생성 Callable
    private class CreateBusDbCallable implements Callable<Void> {
        @Override
        public Void call() throws Exception {
            Parser mParser = Parser.getInstance();
            Realm mRealm = null;
            try {
                mRealm = Realm.getDefaultInstance();
                mParser.createBusStopDB(mRealm, false);
            } finally {
                if (mRealm != null)
                    mRealm.close();
            }
            return null;
        }
    }

    // 서비스 연결
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BeaconService.MyBinder binder = (BeaconService.MyBinder) service;
            mBeaconService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
        }
    };
}

class ViewUtil {
    public static int TEXT_SIZE  = -1;
    public static int TEXT_SIZE_BIG = -1;

    public static Drawable drawable(Context context, int id) {
        if (TEXT_SIZE == -1) {
            TEXT_SIZE = (int) new TextView(context).getTextSize();
            TEXT_SIZE_BIG = (int) (TEXT_SIZE * 2.5);
        }
        if (Build.VERSION.SDK_INT >= 21) {
            return context.getResources().getDrawable(id, context.getTheme());
        } else {
            return context.getResources().getDrawable(id);
        }
    }

    public static CharSequence iconText(Drawable icon, String text) {
        SpannableString iconText = new SpannableString(" "+text);
        icon.setBounds(0, 0, TEXT_SIZE_BIG, TEXT_SIZE_BIG);
        ImageSpan imageSpan = new ImageSpan(icon);

        iconText.setSpan(imageSpan, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return iconText;
    }
}
