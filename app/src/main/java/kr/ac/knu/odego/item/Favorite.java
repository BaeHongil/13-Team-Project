package kr.ac.knu.odego.item;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import lombok.Getter;
import lombok.Setter;

/**
 * Created by BHI on 2016-05-15.
 */
@Getter
@Setter
public class Favorite extends RealmObject {
    @PrimaryKey
    private int index;
    private BusStop mBusStop;
    private Route mRoute;
}
