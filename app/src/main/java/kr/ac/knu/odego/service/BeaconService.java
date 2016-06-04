package kr.ac.knu.odego.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.gson.Gson;
import com.perples.recosdk.RECOBeacon;
import com.perples.recosdk.RECOBeaconManager;
import com.perples.recosdk.RECOBeaconRegion;
import com.perples.recosdk.RECOBeaconRegionState;
import com.perples.recosdk.RECOErrorCode;
import com.perples.recosdk.RECOMonitoringListener;
import com.perples.recosdk.RECORangingListener;
import com.perples.recosdk.RECOServiceConnectListener;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import kr.ac.knu.odego.OdegoApplication;
import kr.ac.knu.odego.R;
import kr.ac.knu.odego.item.BeaconArrInfo;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class BeaconService extends Service implements RECOServiceConnectListener, RECOMonitoringListener, RECORangingListener {
    private final IBinder mBinder = new MyBinder();
    private ExecutorService executorService;
    private Future mFuture;
    private OkHttpClient client;
    private String appServerDomain;

    private final boolean ENABLE_BACKGROUND_RANGING_TIMEOUT = true; // Ranging 때 Beacon을 못 찾을 시 10초 후에 Ranging종료
    private long mScanDuration = 1*1000L; // Monitoring 스캔시간
    private long mSleepDuration = 10*1000L; // Monitoring 스캔종료 후 sleep시간
    private long mRegionExpirationTime = 40*1000L; // Monitoring시 Region을 벗어났을 때, didExitRegion을 호출할 때까지 시간

    private RECOBeaconManager mRecoManager;
    private RECOBeaconRegion busRegion;

    private String fcmToken;

    private boolean firstState = true;

    @Override
    public void onCreate() {
        Log.i(getClass().getSimpleName(), OdegoApplication.getMethodName(Thread.currentThread().getStackTrace()));
        super.onCreate();

        appServerDomain = getString(R.string.appserver_doamin);

        executorService = Executors.newFixedThreadPool(2);
        fcmToken = FirebaseInstanceId.getInstance().getToken();
        client = new OkHttpClient();

        busRegion = new RECOBeaconRegion(getString(R.string.beacon_uuid), "BUS Region");
        busRegion.setRegionExpirationTimeMillis(mRegionExpirationTime);

        mRecoManager = RECOBeaconManager.getInstance(getApplicationContext(), true, ENABLE_BACKGROUND_RANGING_TIMEOUT);
        mRecoManager.setScanPeriod(mScanDuration);
        mRecoManager.setSleepPeriod(mSleepDuration);
        mRecoManager.setMonitoringListener(this);
        mRecoManager.setRangingListener(this);
        mRecoManager.bind(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(getClass().getSimpleName(), OdegoApplication.getMethodName(Thread.currentThread().getStackTrace()));
        if( intent != null) {
            Log.i(getClass().getSimpleName(), OdegoApplication.getMethodName(Thread.currentThread().getStackTrace()) + " intent is not null");
            resetTimeOut();
        }
        else {
            Log.i(getClass().getSimpleName(), OdegoApplication.getMethodName(Thread.currentThread().getStackTrace()) + " intent is null");
            setTimeOut(30 * 60 * 1000L);
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.i(getClass().getSimpleName(), OdegoApplication.getMethodName(Thread.currentThread().getStackTrace()));
        executorService.shutdown();
        try {
            mRecoManager.stopMonitoringForRegion(busRegion);
            mRecoManager.unbind();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(getClass().getSimpleName(), OdegoApplication.getMethodName(Thread.currentThread().getStackTrace()));
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(getClass().getSimpleName(), OdegoApplication.getMethodName(Thread.currentThread().getStackTrace()));
        return super.onUnbind(intent);
    }

    /**
     * 서비스 종료예약
     *
     * @param millis Time out 시간
     */
    public void setTimeOut(final long millis) {
        Log.i(getClass().getSimpleName(), OdegoApplication.getMethodName(Thread.currentThread().getStackTrace()));
        resetTimeOut();
        mFuture = executorService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(millis);
                    stopSelf();
                } catch (InterruptedException e) { }
            }
        });
    }

    /**
     * 서비스 종료예약 취소
     */
    public void resetTimeOut() {
        Log.i(getClass().getSimpleName(), OdegoApplication.getMethodName(Thread.currentThread().getStackTrace()));
        if( mFuture != null && !mFuture.isDone() )
            mFuture.cancel(true);
    }

    private void sendBeaconDataToServer(Collection<RECOBeacon> beacons) {
        Log.i(getClass().getSimpleName(), OdegoApplication.getMethodName(Thread.currentThread().getStackTrace()));

        int maxRssi = Integer.MIN_VALUE;
        RECOBeacon nearBeacon = null;
        for (RECOBeacon beacon : beacons) {
            int rssi = beacon.getRssi();
            if (rssi > maxRssi) {
                maxRssi = rssi;
                nearBeacon = beacon;
            }
        }

        final String UUID = nearBeacon.getProximityUuid();
        final int major = nearBeacon.getMajor();
        final int minor = nearBeacon.getMinor();
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(OdegoApplication.getMethodName(Thread.currentThread().getStackTrace()), UUID + " " + major + " " + minor);
                String url = appServerDomain + "/beacons/" + UUID + "/" + major + "/" + minor + "/busposinfos";
                Request request = new Request.Builder()
                        .url(url)
                        .build();
                try {
                    Response response = client.newCall(request).execute();
                    if (!response.isSuccessful()) throw new IOException("response fail");

                    Log.i("Thread", response.toString() +" "+ response.body());

                    Gson gson = new Gson();
                    BeaconArrInfo mBeaconArrInfo = gson.fromJson(response.body().charStream(), BeaconArrInfo.class);
                    /* 도착할 버스정류장 선택 액티비티 부르기 */
                    /*Intent intent = new Intent(this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);*/
                    /*NotiRequestMsg mNotiRequestMsg = new NotiRequestMsg(mBeaconArrInfo.getRouteId(),
                            mBeaconArrInfo.isForward(),
                            mBeaconArrInfo.getBusId(),
                            72,
                            2,
                            fcmToken);

                    final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
                    RequestBody body = RequestBody.create(JSON, gson.toJson(mNotiRequestMsg));
                    request = new Request.Builder()
                            .url("http://bhi.iptime.org:1313/notifyarrival")
                            .post(body)
                            .build();
                    response = client.newCall(request).execute();
                    Log.i("Thread", response.body().string());*/
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /* RECOServiceConnectListener 메소드들 시작 */
    @Override
    public void onServiceConnect() {
        try {
            mRecoManager.startMonitoringForRegion(busRegion);
        } catch (RemoteException e) {
            Log.e("BackMonitoringService", "RemoteException has occured while executing RECOManager.startMonitoringForRegion()");
            e.printStackTrace();
        }
    }

    @Override
    public void onServiceFail(RECOErrorCode recoErrorCode) {

    }
    /* RECOServiceConnectListener 메소드들 끝 */

    /* RECOMonitoringListener Override 메소드들 시작*/
    @Override
    public void didStartMonitoringForRegion(RECOBeaconRegion recoBeaconRegion) {

    }

    @Override
    public void monitoringDidFailForRegion(RECOBeaconRegion recoBeaconRegion, RECOErrorCode recoErrorCode) {

    }

    @Override
    public void didDetermineStateForRegion(RECOBeaconRegionState recoBeaconRegionState, RECOBeaconRegion recoBeaconRegion) {
        if(firstState == true) {
            if(recoBeaconRegionState == RECOBeaconRegionState.RECOBeaconRegionOutside)
                firstState = false;
            else if(recoBeaconRegionState == RECOBeaconRegionState.RECOBeaconRegionInside) {
                firstState = false;
                Log.i(getClass().getSimpleName(), OdegoApplication.getMethodName(Thread.currentThread().getStackTrace()) + " 비콘 찾음");
                try {
                    mRecoManager.stopMonitoringForRegion(recoBeaconRegion);
                    mRecoManager.startRangingBeaconsInRegion(recoBeaconRegion);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void didEnterRegion(RECOBeaconRegion recoBeaconRegion, Collection<RECOBeacon> beacons) {
        Log.i(getClass().getSimpleName(), OdegoApplication.getMethodName(Thread.currentThread().getStackTrace()));
        if( beacons.size() > 0 )
            sendBeaconDataToServer(beacons);
    }

    @Override
    public void didExitRegion(RECOBeaconRegion recoBeaconRegion) {
        Log.i(getClass().getSimpleName(), OdegoApplication.getMethodName(Thread.currentThread().getStackTrace()));
    }
    /* RECOMonitoringListener Override 메소드들 끝*/

    /* RECORangingListener Override 메소드들 시작*/
    @Override
    public void didRangeBeaconsInRegion(Collection<RECOBeacon> beacons, RECOBeaconRegion recoBeaconRegion) {
        Log.i(getClass().getSimpleName(), OdegoApplication.getMethodName(Thread.currentThread().getStackTrace()));
        if( beacons.size() > 0 ) {
            sendBeaconDataToServer(beacons);

            try {
                mRecoManager.stopRangingBeaconsInRegion(recoBeaconRegion);
                mRecoManager.startMonitoringForRegion(recoBeaconRegion);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void rangingBeaconsDidFailForRegion(RECOBeaconRegion recoBeaconRegion, RECOErrorCode recoErrorCode) {
        Log.i(getClass().getSimpleName(), OdegoApplication.getMethodName(Thread.currentThread().getStackTrace()));
    }
    /* RECORangingListener Override 메소드들 끝*/


    public class MyBinder extends Binder {
        BeaconService getService() {
            return BeaconService.this;
        }
    }
}
