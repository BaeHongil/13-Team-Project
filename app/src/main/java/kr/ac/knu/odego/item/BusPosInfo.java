package kr.ac.knu.odego.item;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Created by BHI on 2016-05-24.
 */

@Getter
@Setter
@NoArgsConstructor
public class BusPosInfo {
    private BusStop mBusStop; // Realm 객체는 Thread가 다르면 사용불가이므로 UI 스레드에서 받아오도록
    private String busStopId;
    private String busId; // 해당 정류소에 버스가 있을 때만 값이 존재
    private boolean isNonStepBus;

    public BusPosInfo(String busStopId) {
        this.busStopId = busStopId;
    }
}
