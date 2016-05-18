package kr.ac.knu.odego.main;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import kr.ac.knu.odego.R;
import kr.ac.knu.odego.common.Parser;
import kr.ac.knu.odego.item.BusStop;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private CoordinatorLayout mContentsLayout;
    private ViewPager mViewPager;
    private BluetoothAdapter mBtAdapter;
    private final boolean IS_BT = false;

    private Realm mRealm;
    private RealmConfiguration busStopRealmConfig;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mContentsLayout = (CoordinatorLayout) findViewById(R.id.contents_layout);
        // fragment 탭 페이지 설정
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        // 플로팅액션버튼 설정
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        // 좌측 네이게이션 설정
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // 블루투스 어뎁터 설정
        if( IS_BT )
            mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        // Realm 설정
        busStopRealmConfig = new RealmConfiguration.Builder(this)
                .name("busstoplist.realm")
                .build();

        Realm.setDefaultConfiguration(busStopRealmConfig);
        mRealm = Realm.getInstance(busStopRealmConfig);
        // Parser로 DB 생성
        new AsyncTask<Context, Context, Boolean>() {
            @Override
            protected Boolean doInBackground(Context... params) {
                Realm mRealm = null;
                Boolean isProgress = false;
                try {
                    mRealm = Realm.getDefaultInstance();
                    if( mRealm.where(BusStop.class).count() == 0 ) {
                        isProgress = true;
                        publishProgress(params[0]);
                        Parser.getInstance().createBusStopDB(mRealm, false);
                    }
                } finally {
                    if (mRealm != null) {
                        mRealm.close();
                    }
                }

                return isProgress;
            }

            @Override
            protected void onProgressUpdate(Context... values) {
                Context mContext = values[0];
                View.inflate(mContext, R.layout.acitivity_progress, mContentsLayout);
                mViewPager.setVisibility(View.INVISIBLE);
            }

            @Override
            protected void onPostExecute(Boolean aBoolean) {
                if( aBoolean ) {
                    mContentsLayout.removeView( mContentsLayout.findViewById(R.id.progress_layout) );
                    mViewPager.setVisibility(View.VISIBLE);
                }
            }
        }.execute(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    //    Parser.getInstance().closeRealm();
        mRealm.close();
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

        if( IS_BT ) {
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
            if(IS_BT) {
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
    @SuppressWarnings("StatementWithEmptyBody")
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


    /**
     * Fragement Page 어뎁터
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return PlaceholderFragment.newInstance(position);
        }

        @Override
        public int getCount() {
            return 4;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.page_title_0);
                case 1:
                    return getString(R.string.page_title_1);
                case 2:
                    return getString(R.string.page_title_2);
                case 3:
                    return getString(R.string.page_title_3);
            }
            return null;
        }
    }
}
