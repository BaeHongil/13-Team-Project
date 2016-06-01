package kr.ac.knu.odego.common;

import android.content.Context;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Pattern;

import io.realm.Realm;
import kr.ac.knu.odego.OdegoApplication;
import kr.ac.knu.odego.R;
import kr.ac.knu.odego.item.ArrInfo;
import kr.ac.knu.odego.item.BusPosInfo;
import kr.ac.knu.odego.item.BusStop;
import kr.ac.knu.odego.item.Route;
import kr.ac.knu.odego.item.RouteArrInfo;

public class Parser {
    private String daeguDomain;
    private String openapiDomain;
    private String apiServiceKey;
    private String daeguCityCode;
    private ArrayList<RouteArrInfo> routeArrInfoList;

    private Parser() {
        Context mContext = OdegoApplication.getContext();
        daeguDomain = mContext.getString(R.string.daegu_domain);
        openapiDomain = mContext.getString(R.string.open_api_domain);
        apiServiceKey = mContext.getString(R.string.api_service_key);
    }

    private static class ParserHolder { // Initialization-on-demand holder idiom
        public static final Parser INSTANCE = new Parser();
    }

    public static Parser getInstance() {
        return ParserHolder.INSTANCE;
    }

    /**
     * 공공데이터에서 사용하는 대구도시코드 얻기.
     *
     * @return 대구도시코드
     */
    public String getDaeguCityCode() {
        if( daeguCityCode == null) {
            StringBuilder urlBuilder = new StringBuilder(openapiDomain);
            urlBuilder.append("BusSttnInfoInqireService/getCtyCodeList")
                    .append("?ServiceKey=").append(apiServiceKey) /*Service Key*/
                    .append("&numOfRows=999") /*검색건수*/
                    .append("&pageNo=1"); /*페이지 번호*/

            try {
                Document doc = Jsoup.connect(urlBuilder.toString()).get();
                Elements elems = doc.select("item");
                for (Element elem : elems) {
                    if (elem.getElementsByTag("sname").text().contains("대구"))
                        daeguCityCode = elem.getElementsByTag("code").text();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return daeguCityCode;
    }

    /**
     * BusStop(버스정류장) Realm DB 구축
     *
     * @param mRealm      현재 쓰레드에서 생성한 realm 인스턴스
     * @param isDeleteAll Realm 내의 BusStop 데이터 삭제 여부
     */
    public void createBusStopDB(Realm mRealm, boolean isDeleteAll) {
        // 버스정류소 리스트URL
        StringBuilder urlBuilder = new StringBuilder(openapiDomain);
        urlBuilder.append("BusSttnInfoInqireService/getSttnNoList")
                .append("?ServiceKey=").append(apiServiceKey) // 공공데이터 인증키
                .append("&numOfRows=9999") // 검색건수
                .append("&pageNo=1") // 페이지 번호
                .append("&cityCode=").append(getDaeguCityCode()); // 대구도시코드
        //    urlBuilder.append("&nodeNm="); // 찾을 정류소명

        try {
            /* 버스정류소 리스트 획득 시작 */
            Document doc = Jsoup.connect(urlBuilder.toString()).timeout(10000).get();
            Elements busStopElems = doc.select("item");
            if (busStopElems.isEmpty())
                return;

            mRealm.beginTransaction();
            if( isDeleteAll ) // 모든 BusStop 객체 삭제
                mRealm.where(BusStop.class).findAll().deleteAllFromRealm();

            for (Element busStopElem : busStopElems) {
                BusStop bs = mRealm.createObject(BusStop.class);
                bs.setGpslati(Double.parseDouble(busStopElem.getElementsByTag("gpslati").text()));
                bs.setGpslong(Double.parseDouble(busStopElem.getElementsByTag("gpslong").text()));
                bs.setId(busStopElem.getElementsByTag("nodeid").text().substring(3));
                bs.setName(busStopElem.getElementsByTag("nodenm").text());
            }
            /* 버스정류소 리스트 획득 끝 */
            /* 버스정류소번호 리스트 획득 시작 */
            String busStopNoListUrl = "http://businfo.daegu.go.kr/ba/arrbus/arrbus.do?act=findByBusStopNo&bsNm="; // 정류소번호리스트 URL
            doc = Jsoup.connect(busStopNoListUrl).timeout(10000).get();
            Elements busStopNoElems = doc.select("tbody tr");
            if ( busStopNoElems.isEmpty() )
                return;

            for (Element busStopNoElem : busStopNoElems) {
                String name = busStopNoElem.child(0).text();
                String no = busStopNoElem.child(1).text();
                if (!no.equals("0")) {
                    BusStop bs = mRealm.where(BusStop.class).equalTo("name", name).findFirst();
                    if (bs != null)
                        bs.setNo(no);
                }
            }
            mRealm.commitTransaction();

            /* 버스정류소번호 리스트 획득 끝 */
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Route(노선) Realm DB 구축
     *
     * @param mRealm      현재 쓰레드에서 생성한 realm 인스턴스
     * @param isDeleteAll Realm 내의 Route 데이터 삭제 여부
     */
    public void createRouteDB(Realm mRealm, boolean isDeleteAll) {
        StringBuilder urlBuilder = new StringBuilder(openapiDomain);
        urlBuilder.append("BusRouteInfoInqireService/getRouteNoList")
                .append("?ServiceKey=").append(apiServiceKey) // 공공데이터 인증키
                .append("&numOfRows=9999") // 검색건수
                .append("&pageNo=1") // 페이지 번호
                .append("&cityCode=").append(getDaeguCityCode()); // 대구도시코드
        try {
            // 노선목록 파싱
            Document doc = Jsoup.connect(urlBuilder.toString()).timeout(10000).get();
            Elements routeElems = doc.select("item");
            if (!routeElems.isEmpty()) {
                mRealm.beginTransaction();
                if( isDeleteAll ) // 모든 Route객체 삭제
                    mRealm.where(Route.class).findAll().deleteAllFromRealm();

                for (Element routeElem : routeElems) {
                    Route route = mRealm.createObject(Route.class);
                    route.setId(routeElem.getElementsByTag("routeid").text().substring(3));

                    // 방면 정보가 존재하는 경우 direction 변수 저장
                    String routeNo = routeElem.getElementsByTag("routeno").text();
                    int offset = routeNo.indexOf("[");
                    if( offset != -1 ) {
                        route.setNo( routeNo.substring(0, offset) );
                        route.setDirection( routeNo.substring(offset+1, routeNo.length()-1) ); // 대괄호를 빼기 위해 +1, -1을 함
                    } else
                        route.setNo( routeNo );

                    String routeTp = routeElem.getElementsByTag("routetp").text();
                    for( BusType busType: BusType.values() ) {
                        if(busType.getName(true).equals(routeTp)) {
                            route.setType(busType.name());
                            break;
                        }
                    }
                }
                mRealm.commitTransaction();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 버스정류장의 노선별 도착정보를 얻습니다.
     * 노선별 도착정보 최초생성시 사용하는 메소드입니다.
     *
     * @param mRealm    현재 쓰레드에서 생성한 realm 인스턴스
     * @param busStopId the bus stop id
     * @return 버스정류장의 노선별 도착정보 배열
     */
    public RouteArrInfo[] getBusStopArrInfos(Realm mRealm, String busStopId) {
        StringBuilder urlBuilder = new StringBuilder(daeguDomain);
        urlBuilder.append("realTime.do?act=arrInfoRouteList&bsNm=&bsId=")
                .append(busStopId);

        RouteArrInfo[] routeArrInfos = null;
        try {
            if( routeArrInfoList == null )
                routeArrInfoList = new ArrayList<>();
            else
                routeArrInfoList.clear();

            Document doc = Jsoup.connect(urlBuilder.toString()).get();
            Elements routeElems = doc.select("li.nx a");
            if (!routeElems.isEmpty()) {
                int routeElemsSize = routeElems.size();

                for(Element routeElem : routeElems) {
                    String href = routeElem.attr("href");
                    int startOffset = href.indexOf("roId="); // routeID 추출
                    int endOffset = href.indexOf("&roNo=");
                    if ( startOffset + 5 != endOffset && startOffset != -1 && endOffset != -1 ) { // startOffset == endOffset이면 전체통합노선의 경우라 제외(ex: 523(전체))
                        String routeId = href.substring(startOffset + 5, endOffset);
                        int moveDir = Integer.parseInt( href.substring(href.indexOf("moveDir=") + 8) );

                        Route mRoute = mRealm.where(Route.class).equalTo("id", routeId).findFirst();
                        ArrInfo[] arrInfos = getArrInfos(busStopId, routeId, moveDir);
                        routeArrInfoList.add(new RouteArrInfo(mRoute, moveDir, arrInfos));
                    }
                }
            }

            routeArrInfos = routeArrInfoList.toArray(new RouteArrInfo[routeArrInfoList.size()]);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return routeArrInfos;
    }

    /**
     * 버스정류장의 노선별 도착정보를 업데이트합니다.
     * 최초생성시에는 getBusStopArrInfos(Realm mRealm, String busStopId)을 사용하고, 해당 메소드는 routeArrInfos가 있을 때만 사용하세요.
     *
     * @param mRealm        현재 쓰레드에서 생성한 realm 인스턴스
     * @param busStopId     the bus stop id
     * @param routeArrInfos null값이 아닌 인스턴스
     * @return 버스정류장의 노선별 도착정보 배열
     */
    public RouteArrInfo[] updateBusStopArrInfos(Realm mRealm, String busStopId, RouteArrInfo[] routeArrInfos) {
        for(int routeArrInfoIndex = 0; routeArrInfoIndex < routeArrInfos.length; routeArrInfoIndex++) {
            String routeId = routeArrInfos[routeArrInfoIndex].getMRoute().getId();
            int moveDir = routeArrInfos[routeArrInfoIndex].getMoveDir();
            routeArrInfos[routeArrInfoIndex].setArrInfoArray( getArrInfos(busStopId, routeId, moveDir) );
        }

        return routeArrInfos;
    }

    // getBusStopArrInfos(), updateBusStopArrInfos() 메소드들에서만 사용하는 메소드
    // busStopId    버스정류장ID
    // routeId      노선ID
    // moveDir      노선방향, 0이면 역방향, 1이면 정방향
    private ArrInfo[] getArrInfos(String busStopId, String routeId, int moveDir) {
        StringBuilder urlBuilder = new StringBuilder(daeguDomain);
        urlBuilder.append("realTime.do?act=arrInfoRoute&bsNm=&roNo=&bsId=").append(busStopId)
                .append("&roId=").append(routeId)
                .append("&moveDir=").append(moveDir);

        try {
            Document doc = Jsoup.connect(urlBuilder.toString()).get();
            Elements arrInfoElems = doc.select("table.air tbody");
            ArrInfo[] arrInfos = null;
            if( !arrInfoElems.isEmpty() ) {
                int arrInfoElemsSize = arrInfoElems.size();
                arrInfos = new ArrInfo[arrInfoElemsSize];

                for(int arrInfoIndex = 0; arrInfoIndex < arrInfoElemsSize; arrInfoIndex++) {
                    Element arrInfoElem = arrInfoElems.get(arrInfoIndex);
                    int remainTime = Integer.parseInt( arrInfoElem.getElementsByClass("st").text() );
                    String curBusStopName = arrInfoElem.child(1).getElementsByTag("td").text();

                    Element secondChild = arrInfoElem.child(2);
                    String endBusStopName, strRemainBusStop;
                    if( secondChild.getElementsByTag("th").text().equals("종료정류소") ) { // 버스막차일 경우에만 생기는 항목
                        endBusStopName = secondChild.getElementsByTag("td").text();
                        strRemainBusStop = arrInfoElem.child(3).getElementsByTag("td").text();
                    } else {
                        endBusStopName = null;
                        strRemainBusStop = secondChild.getElementsByTag("td").text();
                    }
                    int remainBusStop = Integer.parseInt( strRemainBusStop.substring(0, strRemainBusStop.indexOf("개소")) );

                    arrInfos[arrInfoIndex] = new ArrInfo(remainTime, curBusStopName, endBusStopName, remainBusStop);
                }
            } else {  // 도착 정보가 없을 때 -> "기점에서 버스가 출발 대기중이거나 운행 정보가 없습니다.", "기점에서 22시 26분에 출발예정입니다. "
                arrInfos = new ArrInfo[1];
                arrInfos[0] = new ArrInfo();

                arrInfoElems = doc.select("p.gd");
                arrInfos[0].setMessage(arrInfoElems.text());
            }
            return arrInfos;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 노선의 상세정보(기점, 종점, 배차간격 등)를 얻습니다.
     * 주로, 버스위치정보 얻을 때 getBusPosInfos()와 함께 사용합니다.
     *
     * @param mRoute 업데이트할 노선(Realm에서 copyFromRealm()된 객체만 가능합니다)
     * @return 업데이트한 노선
     */
    public Route getRouteInfo(Route mRoute) {
        StringBuilder urlBuilder = new StringBuilder(openapiDomain);
        urlBuilder.append("BusRouteInfoInqireService/getRouteInfoIem")
                .append("?ServiceKey=").append(apiServiceKey) // 공공데이터 인증키
                .append("&numOfRows=9999") // 검색건수
                .append("&pageNo=1") // 페이지 번호
                .append("&cityCode=").append(getDaeguCityCode()) // 대구도시코드
                .append("&routeId=DGB").append(mRoute.getId()); // 노선ID

        try {
            /* 공공데이터에서 노선상세정보 가져오기 시작 */
            Document doc = Jsoup.connect(urlBuilder.toString()).get();
            Elements routeInfoElems = doc.select("item");
            if (routeInfoElems.isEmpty())
                return null;
            Element routeInfoElem = routeInfoElems.get(0);

         //   mRealm.beginTransaction(); // mRoute를 DB에 반영할 때만 사용
            mRoute.setStartBusStopName(routeInfoElem.getElementsByTag("startnodenm").text());
            mRoute.setEndBusStopName(routeInfoElem.getElementsByTag("endnodenm").text());

            String startTime = routeInfoElem.getElementsByTag("startvehicletime").text();
            String endTime = routeInfoElem.getElementsByTag("endvehicletime").text();
            mRoute.setStartHour(Integer.parseInt(startTime.substring(0, 2)));
            mRoute.setStartMin(Integer.parseInt(startTime.substring(2)));
            ;
            mRoute.setEndHour(Integer.parseInt(endTime.substring(0, 2)));
            mRoute.setEndMin(Integer.parseInt(endTime.substring(2)));

            // 배차간격은 휴일만 있는 것도 있고 평일만 있는 것이 존재하는 것을 확인
            Elements intervalTimeElem = routeInfoElem.getElementsByTag("intervaltime");
            Elements intervalSatTimeElem = routeInfoElem.getElementsByTag("intervalsattime");
            Elements intervalSunTimeElem = routeInfoElem.getElementsByTag("intervalsuntime");
            if (!intervalTimeElem.isEmpty())
                mRoute.setInterval(Integer.parseInt(intervalTimeElem.text()));
            if (!intervalSatTimeElem.isEmpty())
                mRoute.setIntervalSat(Integer.parseInt(intervalSatTimeElem.text()));
            if (!intervalSunTimeElem.isEmpty())
                mRoute.setIntervalSun(Integer.parseInt(intervalSunTimeElem.text()));
        //    mRealm.commitTransaction();
            /* 공공데이터에서 노선상세정보 가져오기 끝 */
        } catch (IOException e) {
            e.printStackTrace();
        }

        return mRoute;
    }

    /**
     * 특정 노선의 모든 버스위치정보를 얻습니다.
     * 기존의 버스위치정보가 있을 경우 updateBusPosInfos() 메소드를 사용하세요.
     *
     * @param mRealm    현재 쓰레드에서 생성한 realm 인스턴스
     * @param routeId    검색할 노선 ID
     * @param isForward 정방향이면 true, 역방향이면 false
     * @return 생성된 버스위치정보 배열
     */
    public BusPosInfo[] getBusPosInfos(Realm mRealm, String routeId, boolean isForward) {
        BusPosInfo[] busPosInfos = null;

        try {
            StringBuilder urlBuilder = new StringBuilder(daeguDomain);
            urlBuilder.append("realTime.do?act=posInfo&roNo=")
                    .append("&roId=").append(routeId)
                    .append("&moveDir=").append(isForward ? 1 : 0);
            /* 대구버스에서 노선 정류소리스트 및 버스위치정보 가져오기 시작 */

            Document doc = Jsoup.connect(urlBuilder.toString()).get();
            Elements listElems = doc.select("ol.bl");
            if (listElems.isEmpty())
                return null;

            // 노선 정류소리스트 저장
            Elements busStopElems = listElems.select("span.pl39");
            int busStopElemsSize = busStopElems.size();
            busPosInfos = new BusPosInfo[busStopElemsSize];
            for (int busPosInfoIndex = 0; busPosInfoIndex < busStopElemsSize; busPosInfoIndex++) {
                Element busStopElem = busStopElems.get( busPosInfoIndex );
                String rawBusStopName = busStopElem.select(".pl39").text();
                int startOffset = rawBusStopName.indexOf(". ");
                String busStopName = rawBusStopName.substring(startOffset + 2);

                BusStop mBusStop = mRealm.where(BusStop.class).equalTo("name", busStopName).findFirst();
                busPosInfos[busPosInfoIndex] = new BusPosInfo(mBusStop);
            }

            // 버스위치정보 저장
            Elements busPosElems = listElems.select("li.bloc_b");
            if( !busPosElems.isEmpty() ) {
                for(int busPosElemIndex = 0; busPosElemIndex < busPosElems.size(); busPosElemIndex++) {
                    Element busPosElem = busPosElems.get(busPosElemIndex);
                    String rawBusId = busPosElem.text();
                    int endOffset = rawBusId.indexOf("(");
                    String busId = rawBusId.substring(0, endOffset).trim();

                    int busPosInfoIndex = busPosElem.elementSiblingIndex() - busPosElemIndex - 1;
                    busPosInfos[busPosInfoIndex].setBusId(busId);
                    if( busPosElem.hasClass("nsbus") )
                        busPosInfos[busPosInfoIndex].setNonStepBus(true);
                }
            }
            /* 대구버스에서 노선정류소정보 및 버스위치정보 가져오기 끝 */

 /*           urlBuilder.append("BusRouteInfoInqireService/getRouteAcctoThrghSttnList")
                    .append("?ServiceKey=").append(apiServiceKey) // 공공데이터 인증키
                    .append("&numOfRows=9999") // 검색건수
                    .append("&pageNo=1") // 페이지 번호
                    .append("&cityCode=").append(getDaeguCityCode()) // 대구도시코드
                    .append("&routeId=DGB").append(mRoute.getId()); // 노선ID

            *//* 공공데이터에서 노선별경유정류소목록 가져오기 시작 *//*
            doc = Jsoup.connect(urlBuilder.toString()).get();
            Elements busStopElems = doc.select("item");
            if (busStopElems.isEmpty())
                return null;
            int busStopElemsSize = busStopElems.size();
            if(isForward) { // 정방향
                if( busPosInfoList == null )
                    busPosInfoList = new ArrayList<>();
                else
                    busPosInfoList.clear();

                for(int itemIndex = 0; itemIndex < busStopElemsSize; itemIndex++) { // 삼항연산자는 역방향이면 최초 item노드는 배제하기 위해서임
                    Element busStopElem = busStopElems.get(itemIndex);
                    int nodeord = Integer.parseInt( busStopElem.getElementsByTag("nodeord").text() );
                    if( nodeord == 1 && itemIndex != 0 )
                        break;

                    String nodeId = busStopElem.getElementsByTag("nodeid").text().substring(3);
                    BusStop mBusStop = mRealm.where(BusStop.class).equalTo("id", nodeId).findFirst();
                    busPosInfoList.add( new BusPosInfo(mBusStop) );
                }
                busPosInfos = busPosInfoList.toArray(new BusPosInfo[busPosInfoList.size()]);
            } else { // 역방향
                int itemIndex;
                for(itemIndex = 1; itemIndex < busStopElemsSize; itemIndex++) { // 삼항연산자는 역방향이면 최초 item노드는 배제하기 위해서임
                    Element busStopElem = busStopElems.get(itemIndex);
                    int nodeord = Integer.parseInt( busStopElem.getElementsByTag("nodeord").text() );
                    if( nodeord == 1 ) {
                        busPosInfos = new BusPosInfo[busStopElemsSize - itemIndex];
                        break;
                    }
                }
                int firstIndex = itemIndex;
                for(; itemIndex < busStopElemsSize; itemIndex++) {
                    Element busStopElem = busStopElems.get(itemIndex);
                    String nodeId = busStopElem.getElementsByTag("nodeid").text().substring(3);
                    BusStop mBusStop = mRealm.where(BusStop.class).equalTo("id", nodeId).findFirst();
                    busPosInfos[itemIndex-firstIndex] = new BusPosInfo(mBusStop);
                }
            }
            *//* 공공데이터에서 노선별경유정류소목록 가져오기 끝 */


        } catch (IOException e) {
            e.printStackTrace();
        }

        return busPosInfos;
    }

    /**
     * 특정 노선의 모든 버스위치정보를 업데이트합니다.
     * 기존의 버스위치정보가 없을 경우 getBusPosInfos() 먼저 사용하세요.
     *
     * @param routeId      검색할 노선 ID
     * @param isForward   정방향이면 true, 역방향이면 false
     * @param busPosInfos 버스위치정보 배열
     * @return 업데이트된 버스위치정보 배열
     */
    public BusPosInfo[] updateBusPosInfos(String routeId, boolean isForward, BusPosInfo[] busPosInfos) {
        for(BusPosInfo mBusPosInfo : busPosInfos) // 버스ID 초기화
            mBusPosInfo.setBusId(null);

        StringBuilder urlBuilder = new StringBuilder(daeguDomain);
        urlBuilder.append("realTime.do?act=posInfo&roNo=")
                .append("&roId=").append(routeId)
                .append("&moveDir=").append(isForward ? 1 : 0);

        try {
            Document doc = Jsoup.connect(urlBuilder.toString()).get();
            // 버스위치정보 저장
            Elements busPosElems = doc.select("li.bloc_b");
            if( !busPosElems.isEmpty() ) {
                for(int busPosElemIndex = 0; busPosElemIndex < busPosElems.size(); busPosElemIndex++) {
                    Element busPosElem = busPosElems.get(busPosElemIndex);
                    String rawBusId = busPosElem.text();
                    int endOffset = rawBusId.indexOf(" (");
                    String busId = rawBusId.substring(0, endOffset);

                    int busPosInfoIndex = busPosElem.elementSiblingIndex() - busPosElemIndex - 1;
                    busPosInfos[busPosInfoIndex].setBusId(busId);
                    if( busPosElem.hasClass("nsbus") )
                        busPosInfos[busPosInfoIndex].setNonStepBus(true);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return busPosInfos;
    }

    /**
     * String 값에 숫자만 들어있는지 확인
     *
     * @param str 확인할 String
     * @return 숫자만 들어있으면 True, 아니면 False
     */
    public Boolean isNum(String str) {
        if( Pattern.matches("^[0-9]+$", str) )
            return true;
        return false;
    }
}
