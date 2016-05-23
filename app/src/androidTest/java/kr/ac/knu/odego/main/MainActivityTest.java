package kr.ac.knu.odego.main;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import kr.ac.knu.odego.common.Parser;
import kr.ac.knu.odego.item.ArrInfo;
import kr.ac.knu.odego.item.Route;
import kr.ac.knu.odego.item.RouteArrInfo;

/**
 * Created by BHI on 2016-05-24.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class MainActivityTest extends ActivityInstrumentationTestCase2<MainActivity> {
    private MainActivity activity;
    private Parser mParser;

    public MainActivityTest() {
        super(MainActivity.class);
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        injectInstrumentation(InstrumentationRegistry.getInstrumentation());
        activity = getActivity();
        mParser = Parser.getInstance();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testOnCreate() throws Exception {
        RealmConfiguration busDBRealmConfig = new RealmConfiguration.Builder(activity)
                .name("busdb.realm")
                .build();

        Realm.setDefaultConfiguration(busDBRealmConfig);
        Realm mReam = null;
        try {
            Realm mRealm = Realm.getInstance(busDBRealmConfig);
            RouteArrInfo[] routeArrInfos = mParser.getBusStopArrInfos(mRealm, null, "7021025700");

            for( RouteArrInfo routeArrInfo : routeArrInfos ) {
                Route mRoute = routeArrInfo.getMRoute();
                Log.i("mRealm", mRoute.getNo());
                ArrInfo[] arrInfos = routeArrInfo.getArrInfoArray();
                for( ArrInfo arrInfo : arrInfos) {
                    if( arrInfo.getMessage() == null )
                        Log.i("mRealm", arrInfo.getRemainBusStop() + " " + arrInfo.getRemainMin());
                    else
                        Log.i("mRealm", arrInfo.getMessage());
                }
            }
        } finally {
            if(mReam != null)
                mReam.close();
        }

    }
}