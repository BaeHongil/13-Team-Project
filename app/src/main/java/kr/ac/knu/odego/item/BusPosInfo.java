package kr.ac.knu.odego.item;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * Created by BHI on 2016-05-24.
 */
@Getter
@Setter
@RequiredArgsConstructor
public class BusPosInfo {
    @NonNull
    private BusStop mBusStop;
    private String busId; // 해당 정류소에 버스가 있을 때만 값이 존재
    private boolean isNonStepBus;
}
