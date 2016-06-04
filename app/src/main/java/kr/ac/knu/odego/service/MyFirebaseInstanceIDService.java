package kr.ac.knu.odego.service;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

/**
 * Created by BHI on 2016-05-31.
 */
public class MyFirebaseInstanceIDService extends FirebaseInstanceIdService {
    // 최초 토큰 생성시 실행되는 메소드
    @Override
    public void onTokenRefresh() {
        super.onTokenRefresh();
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();

        sendRegistrationToServer(refreshedToken);
    }

    public void sendRegistrationToServer(String refreshedToken) {

    }
}
