package kr.ac.knu.odego.item;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Created by BHI on 2016-05-23.
 */
@Getter
@Setter
@NoArgsConstructor
public class RouteArrInfo {
    private Route mRoute; // Realm 객체는 Thread가 다르면 사용불가이므로 UI 스레드에서 받아오도록
    private String routeId; // 노선ID
    private boolean isForward; // 노선방향 ex) 정방향 = 1, 역방향 = 0
    private ArrInfo[] arrInfos; // 도착정보 배열

    public RouteArrInfo(String routeId, boolean isForward, ArrInfo[] arrInfos) {
        this.routeId = routeId;
        this.isForward = isForward;
        this.arrInfos = arrInfos;
    }
}
