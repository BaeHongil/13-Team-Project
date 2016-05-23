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
    private int remainMin;
    private String curBusStopName;
    private String endBusStopName;
    private int remainBusStop;
    private String message;

    public ArrInfo(int remainMin, String curBusStopName, String endBusStopName, int remainBusStop) {
        this.remainMin = remainMin;
        this.curBusStopName = curBusStopName;
        this.endBusStopName = endBusStopName;
        this.remainBusStop = remainBusStop;
    }
}
