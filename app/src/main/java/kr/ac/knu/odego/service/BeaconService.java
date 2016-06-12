package kr.ac.knu.odego.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

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
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import io.realm.Realm;
import io.realm.RealmList;
import kr.ac.knu.odego.OdegoApplication;
import kr.ac.knu.odego.R;
import kr.ac.knu.odego.activity.BeaconActivity;
import kr.ac.knu.odego.item.BeaconArrInfo;
import kr.ac.knu.odego.item.BeaconArrInfoResMsg;
import kr.ac.knu.odego.item.BusStop;
import kr.ac.knu.odego.item.Route;
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

        SendBeaconData sendBeaconData = new SendBeaconData();
        sendBeaconData.execute(nearBeacon);
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
        Log.i(getClass().getSimpleName(), recoBeaconRegionState.name());
        if( firstState ) {
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
        public BeaconService getService() {
            return BeaconService.this;
        }
    }

    private class SendBeaconData extends AsyncTask<RECOBeacon, String, Boolean> {

        @Override
        protected Boolean doInBackground(RECOBeacon... params) {
            RECOBeacon beacon = params[0];

            Log.i(OdegoApplication.getMethodName(Thread.currentThread().getStackTrace()),
                    beacon.getProximityUuid() + " " + beacon.getMajor() + " " + beacon.getMinor());
            String url = appServerDomain + "/beacons/"
                    + beacon.getProximityUuid()
                    + "/" + beacon.getMajor()
                    + "/" + beacon.getMinor()
                    + "/busposinfos";
            Request request = new Request.Builder()
                    .url(url)
                    .build();
            Realm mRealm = null;
            try {
                Response response = client.newCall(request).execute();
                if (!response.isSuccessful()) throw new IOException("response fail");
                if ( response.code() == 404 ) return false;

                Log.i("Thread", response.toString() +" "+ response.body());

                Gson gson = new Gson();
                BeaconArrInfoResMsg mBeaconArrInfoResMsg = gson.fromJson(response.body().charStream(), BeaconArrInfoResMsg.class);

                mRealm = Realm.getDefaultInstance();
                mRealm.beginTransaction();
                BeaconArrInfo mBeaconArrInfo = new BeaconArrInfo(mBeaconArrInfoResMsg);
                Number maxIndex = mRealm.where(BeaconArrInfo.class).max("index");
                int index;
                if( maxIndex == null )
                    index = 0;
                else
                    index = maxIndex.intValue() + 1;
                mBeaconArrInfo.setIndex( index );
                mBeaconArrInfo.setUpdated( new Date() );

                mBeaconArrInfo = mRealm.copyToRealm(mBeaconArrInfo);
                Route mRoute = mRealm.where(Route.class).equalTo( "id", mBeaconArrInfoResMsg.getRouteId() ).findFirst();
                mBeaconArrInfo.setMRoute( mRoute );
                RealmList<BusStop> busStops = mBeaconArrInfo.getBusStops();
                for( String busStopId : mBeaconArrInfoResMsg.getBusStopIds() ) {
                    BusStop mBusStop = mRealm.where(BusStop.class).equalTo("id", busStopId ).findFirst();
                    busStops.add( mBusStop );
                }

                mRealm.commitTransaction();

                String contentTitle = String.format(getString(R.string.noti_title), mRoute.getNo());
                String contentText = getString(R.string.noti_text_setup_dest);
                long[] vibrate = new long[]{0, 1000, 500, 1000};
                OdegoApplication.createNotification( getBaseContext(), contentTitle, contentText, vibrate );

                return true;
            } catch (IOException e) {
                publishProgress( getString(R.string.network_error_msg) );
            } finally {
                if( mRealm != null )
                    mRealm.close();
            }

            return false;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            Toast.makeText(getBaseContext(), values[0], Toast.LENGTH_LONG).show();
        }
    }

    private void createNotification(String contentTitle) {
        NotificationCompat.Builder mBuilder = (NotificationCompat.Builder) new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.logo)
                .setContentTitle( contentTitle )
                .setContentText( getString(R.string.noti_text_setup_dest) )
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setVibrate( new long[]{0, 1000, 500, 1000} );
        Intent notifyIntent = new Intent(this, BeaconActivity.class);
        notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent notifyPendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        notifyIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(notifyPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        mNotificationManager.notify(getResources().getInteger(R.integer.notification_id), mBuilder.build());
    }
}
