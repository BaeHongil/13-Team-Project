package kr.ac.knu.odego.item;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Created by BHI on 2016-05-23.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RouteArrInfo {
    private Route mRoute; // 노선정보
    private int moveDir; // 노선방향 ex) 정방향, 역방향
    private ArrInfo[] arrInfoArray; // 도착정보 배열
}
