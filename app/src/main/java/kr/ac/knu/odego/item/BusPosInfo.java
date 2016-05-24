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
    private String busId;
    private boolean isNonStepBus;
}
