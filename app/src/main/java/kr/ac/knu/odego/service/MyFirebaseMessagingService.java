package kr.ac.knu.odego.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import io.realm.Realm;
import kr.ac.knu.odego.OdegoApplication;
import kr.ac.knu.odego.R;
import kr.ac.knu.odego.activity.BeaconActivity;
import kr.ac.knu.odego.item.BeaconArrInfo;

/**
 * Created by BHI on 2016-05-31.
 */
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Realm mRealm = null;
        try {
            mRealm = Realm.getDefaultInstance();
            int beaconArrInfoIndex = mRealm.where(BeaconArrInfo.class).max("index").intValue();
            BeaconArrInfo mBeaconArrInfo = mRealm.where(BeaconArrInfo.class).equalTo("index", beaconArrInfoIndex).findFirst();
            int remainCount = Integer.parseInt( remoteMessage.getData().get("remainCount") );
            createNotification( String.format(getString(R.string.noti_contentTitle), mBeaconArrInfo.getMRoute().getNo()),
                    remainCount + "정류장 남았습니다");
            Log.i(OdegoApplication.getMethodName(Thread.currentThread().getStackTrace()), remoteMessage.getData().toString());
        } finally {
            if(mRealm != null)
                mRealm.close();
        }
    }

    private void createNotification(String contentTitle, String contentText) {
        NotificationCompat.Builder mBuilder = (NotificationCompat.Builder) new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.logo)
                .setContentTitle( contentTitle )
                .setContentText( contentText )
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
