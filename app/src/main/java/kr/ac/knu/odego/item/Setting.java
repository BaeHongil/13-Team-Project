package kr.ac.knu.odego.item;

import io.realm.RealmObject;
import lombok.Getter;
import lombok.Setter;

/**
 * Created by BHI on 2016-06-15.
 */
@Getter
@Setter
public class Setting extends RealmObject {
    private int requestRemainCount;
}
