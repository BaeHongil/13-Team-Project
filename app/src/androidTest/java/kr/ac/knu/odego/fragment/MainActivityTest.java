package kr.ac.knu.odego.fragment;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import kr.ac.knu.odego.activity.MainActivity;
import kr.ac.knu.odego.common.Parser;
import kr.ac.knu.odego.item.BusPosInfo;
import kr.ac.knu.odego.item.Route;

/**
 * Created by BHI on 2016-05-24.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class MainActivityTest extends ActivityInstrumentationTestCase2<MainActivity> {
    private MainActivity activity;
    private Parser mParser;
    private Realm mRealm;
    private RealmConfiguration busDBRealmConfig;

    public MainActivityTest() {
        super(MainActivity.class);
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        injectInstrumentation(InstrumentationRegistry.getInstrumentation());
        activity = getActivity();
        mParser = Parser.getInstance();

        busDBRealmConfig = new RealmConfiguration.Builder(activity)
                .name("busdb.realm")
                .build();
        Realm.setDefaultConfiguration(busDBRealmConfig);
        mRealm = Realm.getInstance(busDBRealmConfig);
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        if( mRealm != null )
            mRealm.close();
    }

    @Ignore
    public void realmTest() throws Exception {
        String methodName = "realmTest";
        RealmConfiguration busDBRealmConfig = new RealmConfiguration.Builder(activity)
                .name("busdb.realm")
                .build();

        Realm.setDefaultConfiguration(busDBRealmConfig);
        Realm mReam = null;
        try {
            Realm mRealm = Realm.getInstance(busDBRealmConfig);
            Route mRoute = mRealm.where(Route.class).findFirst();
            Log.i(methodName, mRoute.getNo());
            mRoute.setInterval(0);
        } finally {
            if(mReam != null)
                mReam.close();
        }
    }

    @Ignore
    public void jsoupTest() throws Exception {
        String methodName = "jsoupTest";
        Document doc = Jsoup.connect("http://m.businfo.go.kr/bp/m/realTime.do?act=posInfo&roId=3000937000&roNo=937&moveDir=1").get();
        Elements titles = doc.select("li.bloc_b");
        if (!titles.isEmpty()) {
            for (Element e : titles) {
                Element preEle = e.previousElementSibling();
                String rawBusId = e.child(0).text();
                int endOffset = rawBusId.indexOf(" (");
                String busId = rawBusId.substring(0, endOffset);

                Log.i(methodName, preEle.elementSiblingIndex() + "");
                Log.i(methodName, busId);
            }
        }
    }

    @Ignore
    public String getMethodName(StackTraceElement e[]) {
        boolean doNext = false;
        for (StackTraceElement s : e) {
            if(doNext)
                return s.getMethodName();
            doNext = s.getMethodName().equals("getStackTrace");
        }
        return null;
    }

    @Test
    public void getBusPosInfosTest() throws Exception {
        String methodName = getMethodName(Thread.currentThread().getStackTrace());

        Route routeRealm = mRealm.where(Route.class).equalTo("id", "3000306000").findFirst();
        Route mRoute = mRealm.copyFromRealm(routeRealm);
        BusPosInfo[] busPosInfos = mParser.getBusPosInfos(mRealm, mRoute.getId(), true);
        busPosInfos = mParser.updateBusPosInfos(mRoute.getId(), true, busPosInfos);
        for( BusPosInfo mBusPosInfo : busPosInfos) {
            Log.i(methodName, mBusPosInfo.getMBusStop().getName());
            if( mBusPosInfo.getBusId() != null )
                Log.i(methodName, (mBusPosInfo.isNonStepBus() ? "저상":"") + mBusPosInfo.getBusId());
        }

        mParser.getRouteDetailInfo(mRealm, mRoute);
        Log.i(methodName, "시작시간" + mRoute.getStartHour());
        Log.i(methodName, "시작분" + mRoute.getStartMin());
        Log.i(methodName, "배차간격" + mRoute.getInterval());
        Log.i(methodName, "배차간격(휴일)" + mRoute.getIntervalSun());
    }
}