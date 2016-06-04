package kr.ac.knu.odego.service;

import android.content.Intent;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import kr.ac.knu.odego.OdegoApplication;
import kr.ac.knu.odego.activity.MainActivity;

/**
 * Created by BHI on 2016-05-31.
 */
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.i(OdegoApplication.getMethodName(Thread.currentThread().getStackTrace()), remoteMessage.getData().toString());
    }


}
