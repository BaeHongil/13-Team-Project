package kr.ac.knu.odego.item;

import java.util.Date;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import lombok.Getter;
import lombok.Setter;

/**
 * Created by BHI on 2016-06-04.
 */
@Getter
@Setter
public class BeaconArrInfo extends RealmObject {
    @PrimaryKey
    private int index;
    private Date updated;
    private Route mRoute;
    private boolean isForward;
    private String busId;
    private int startIndex;
    private int destIndex = -1;
    private RealmList<BusStop> busStops;

    public BeaconArrInfo() {

    }

    public BeaconArrInfo(boolean isForward, String busId, int startIndex) {
        this.isForward = isForward;
        this.busId = busId;
        this.startIndex = startIndex;
    }

    public BeaconArrInfo(BeaconArrInfoResMsg resMsg) {
        this( resMsg.isForward(), resMsg.getBusId(), resMsg.getFoundIndex() );
    }
}
