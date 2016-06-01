package kr.ac.knu.odego.item;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Created by BHI on 2016-05-14.
 */
@Getter
@Setter
@NoArgsConstructor
public class ArrInfo {
    private int remainMin; // 버스도착예정시간
    private String curBusStopName; // 도착예정인 버스의 현재정류소
    private String endBusStopName; // 막차 때, 마지막 정류소
    private int remainBusStop; // 현재버스의 남은 정류소
    private String message; // 도착정보없을 때 메시지(이 값이 null이 아니면 위의 값들 다 없음)

    public ArrInfo(int remainMin, String curBusStopName, String endBusStopName, int remainBusStop) {
        this.remainMin = remainMin;
        this.curBusStopName = curBusStopName;
        this.endBusStopName = endBusStopName;
        this.remainBusStop = remainBusStop;
    }
}
