package kr.ac.knu.odego.activity;

import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.DataSetObserver;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import io.realm.Realm;
import kr.ac.knu.odego.OdegoApplication;
import kr.ac.knu.odego.R;
import kr.ac.knu.odego.adapter.SectionsPagerAdapter;
import kr.ac.knu.odego.common.Parser;
import kr.ac.knu.odego.common.RealmTransaction;
import kr.ac.knu.odego.common.ViewUtil;
import kr.ac.knu.odego.fragment.BusStopSearchFragment;
import kr.ac.knu.odego.fragment.FavoriteFragment;
import kr.ac.knu.odego.fragment.RouteSearchFragment;
import kr.ac.knu.odego.fragment.TheOtherFragment;
import kr.ac.knu.odego.item.BusStop;
import kr.ac.knu.odego.item.Route;
import kr.ac.knu.odego.item.Setting;
import kr.ac.knu.odego.service.BeaconService;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, ViewPager.OnPageChangeListener {
    private CoordinatorLayout mContentsLayout;
    private ViewPager mViewPager;
    private BluetoothAdapter mBtAdapter;

    private Realm mRealm, mSettingRealm;
    private Setting mSetting;
    private int requestRemainCount;

    private BeaconService mBeaconService;
    private boolean mBound = false;
    private boolean isStartSplash = false;

    private CharSequence tab_main1, tab_main2, tab_main3, tab_main4;
    private CharSequence tab_off_main1, tab_off_main2, tab_off_main3, tab_off_main4;
    private int mPrevPosition = 0;

    private SectionsPagerAdapter mSectionsPagerAdapter;
    private TabLayout tabLayout;
    private LinearLayout progressLayout;
    private DrawerLayout drawer;

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

        mSectionsPagerAdapter.addFragment(new FavoriteFragment(), tab_main1);
        mSectionsPagerAdapter.addFragment(new RouteSearchFragment(), tab_off_main2);
        mSectionsPagerAdapter.addFragment(new BusStopSearchFragment(), tab_off_main3);
        mSectionsPagerAdapter.addFragment(new TheOtherFragment(), tab_off_main4);

        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.addOnPageChangeListener(this);

        tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        progressLayout = (LinearLayout) findViewById(R.id.progress_layout);

        // 좌측 네이게이션 설정
        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
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
        mSettingRealm = Realm.getInstance(OdegoApplication.getSettingRealmConfig());
        mSetting = mSettingRealm.where(Setting.class).findFirst();
        if( mSetting == null ) {
            mSettingRealm.executeTransactionAsync(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    Setting setting = realm.createObject(Setting.class);
                    setting.setRequestRemainCount(2);
                }
            }, new Realm.Transaction.OnSuccess() {
                @Override
                public void onSuccess() {
                    mSetting = mSettingRealm.where(Setting.class).findFirst();
                }
            });
        }

        // Parser로 DB 생성
        new DataBaseCreateAsyncTask().execute(false);
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
        if( mSettingRealm != null )
            mSettingRealm.close();
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

        if (id == R.id.nav_clear_db) {
            createYesNoDialog(
                    R.string.nav_dialog_update_db_title,
                    R.string.nav_dialog_update_db_message,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            new DataBaseCreateAsyncTask().execute(true);
                        }
                    });

        } else if (id == R.id.nav_modify_remaincount) {
            createChoiceDialog(
                    R.string.nav_dialog_modify_remaincount_title,
                    R.array.nav_dialog_remaincount_list,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            RealmTransaction.modifySetting(mSettingRealm, requestRemainCount);
                        }
                    });

        } else if (id == R.id.nav_delete_favorite) {
            createYesNoDialog(
                R.string.nav_dialog_delete_favorite_title,
                R.string.nav_dialog_delete_favorite_message,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        RealmTransaction.clearFavorite(mRealm);
                    }
                });

        } else if (id == R.id.nav_delete_recent_search) {
            createYesNoDialog(
                    R.string.nav_dialog_delete_recent_search_title,
                    R.string.nav_dialog_delete_recent_search_message,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            RealmTransaction.clearSearchHistory(mRealm);
                        }
                    });

        } else if (id == R.id.nav_delete_beaconarrinfo) {
            createYesNoDialog(
                    R.string.nav_dialog_nav_delete_beaconarrinfo_title,
                    R.string.nav_dialog_nav_delete_beaconarrinfo_message,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            RealmTransaction.clearBeaconArrInfo(mRealm);
                        }
                    });

        }/* else if (id == R.id.nav_open_source_license) {

        }*/

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void createYesNoDialog(int titleId, int messageId, DialogInterface.OnClickListener onYesClickListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle( titleId )
                .setMessage( messageId )
                .setPositiveButton(R.string.ok, onYesClickListener)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
        builder.create().show();
    }

    private void createChoiceDialog(int titleId, int itemsId, DialogInterface.OnClickListener onYesClickListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle( titleId )
                .setSingleChoiceItems(R.array.nav_dialog_remaincount_list, mSetting.getRequestRemainCount(), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        requestRemainCount = which;
                    }
                })
                .setPositiveButton(R.string.ok, onYesClickListener)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
        builder.create().show();
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
        mSectionsPagerAdapter.notifyDataSetChanged();
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }


    /* 내부 클래스 시작 */

    /**
     * 버스정류장, 노선 DB 생성 AsyncTask
     */
    private class DataBaseCreateAsyncTask extends AsyncTask<Boolean, String, Boolean> {

        @Override
        protected Boolean doInBackground(Boolean... params) {
            boolean isDeleteAll = false;
            if( params.length == 1)
                isDeleteAll = params[0];

            ExecutorService executor = Executors.newSingleThreadExecutor();
            Realm mRealm = null;
            try {
                mRealm = Realm.getDefaultInstance();
                boolean isBusStopDB = false;
                boolean isRouteDB = false;
                if( isDeleteAll ) { // isDeleteAll이 true면 전체 삭제
                    mRealm.beginTransaction();
                    mRealm.deleteAll();
                    mRealm.commitTransaction();
                } else  { // 전체삭제가 아닐 때만 확인
                    if (mRealm.where(BusStop.class).count() != 0) isBusStopDB = true;
                    if (mRealm.where(Route.class).count() != 0) isRouteDB = true;
                    if (isBusStopDB && isRouteDB) return false; // DB 둘 다 있을 때는 바로 끝내기
                }

                publishProgress();
                Parser mParser = Parser.getInstance();
                if (!isBusStopDB && !isRouteDB) { // DB 둘 다 없을 때는 Thread 하나 더 생성해서 DB생성
                    Future future = executor.submit( new CreateBusDbCallable() );
                    mParser.createRouteDB(mRealm);
                    future.get();
                } else if ( !isBusStopDB ) { // BusStop DB만 없을 때
                    mParser.createBusStopDB(mRealm);
                } else // Route DB만 없을 때
                    mParser.createRouteDB(mRealm);

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
            if(values.length != 0) { // false면 네트워크 오류
                String toastMsg = values[0];
                Toast.makeText(getBaseContext(), toastMsg, Toast.LENGTH_LONG);
                return;
            }

            mViewPager.setVisibility(View.GONE);
            progressLayout.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            if( aBoolean ) {
                progressLayout.setVisibility(View.GONE);
                mViewPager.setVisibility(View.VISIBLE);
            }

            // 비콘 서비스 실행여부
            if( mBtAdapter != null && mBtAdapter.isEnabled() && !mBound ) {
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
                mParser.createBusStopDB(mRealm);
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

