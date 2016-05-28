package kr.ac.knu.odego.item;

import io.realm.RealmObject;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;
import lombok.Getter;
import lombok.Setter;

/**
 * Created by BHI on 2016-05-17.
 */
@Getter
@Setter
public class BusStop extends RealmObject {
    @PrimaryKey
    private String id; // 정류소고유ID
    @Index
    private String name; // 정류소이름
    @Index
    private String no; // 정류소번호
    private double gpslati, gpslong; // 위도, 경도
    @Index
    private int historyIndex; // 최근기록
}
