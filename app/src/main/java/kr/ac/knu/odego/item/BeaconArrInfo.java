package kr.ac.knu.odego.item;

import lombok.Getter;
import lombok.Setter;

/**
 * Created by BHI on 2016-06-04.
 */
@Getter
@Setter
public class BeaconArrInfo {
    private String routeId;
    private boolean isForward;
    private String busId;
    private int foundIndex;
    private BusPosInfo[] busPosInfos;
}
