package kr.ac.knu.odego.item;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * Created by BHI on 2016-06-04.
 */
@Getter
@Setter
@AllArgsConstructor
public class NotiRequestMsg {
    private String routeId;
    private boolean isForward;
    private String busId;
    private int requestIndex; // 내릴 정류장 index
    private int requestRemainCount; // 남은 정류장 갯수가 몇 개일 때 알림을 받을 것인지 (1 또는 2)
    private String fcmToken; // 푸시메시지를 받기 위한 FCM 토큰 - FirebaseInstanceId.getInstance().getToken()
}
