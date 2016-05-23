package kr.ac.knu.odego.common;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;

import io.realm.Realm;
import kr.ac.knu.odego.item.ArrInfo;
import kr.ac.knu.odego.item.BusStop;
import kr.ac.knu.odego.item.Route;
import kr.ac.knu.odego.item.RouteArrInfo;

public class Parser {
    private static Parser instance;
    private String daeguDomain = "http://m.businfo.go.kr/bp/m/";
    private String openapiDomain = "http://openapi.tago.go.kr/openapi/service/";
    private HashMap<String, Integer> mBusStopNoMap;
    private String daeguCityCode;
    private ArrayList<RouteArrInfo> routeArrInfoList;

    private Parser() {

    }

    public static Parser getInstance() {
        if( instance == null )
            instance = new Parser();
        return instance;
    }

    /**
     * 공공데이터에서 사용하는 대구도시코드 얻기.
     *
     * @return 대구도시코드
     */
    public String getDaeguCityCode() {
        if( daeguCityCode == null) {
            StringBuilder urlBuilder = new StringBuilder(openapiDomain);
            urlBuilder.append("BusSttnInfoInqireService/getCtyCodeList");
            urlBuilder.append("?ServiceKey=%2FINPAsm7NTY0H7pQwDLNdW5dFd%2FhZxqvngMPEUKPW2de5TVRU2fhgI6x6CsUpkhjJYmH5tG4vYCahsntFWxJ%2Bg%3D%3D"); /*Service Key*/
            urlBuilder.append("&numOfRows=999"); /*검색건수*/
            urlBuilder.append("&pageNo=1"); /*페이지 번호*/

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
     * @param mRealm      해당 메소드 실행하는 쓰레드의 Realm 인스턴스
     * @param isDeleteAll Realm 내의 BusStop 데이터 삭제 여부
     */
    public void createBusStopDB(Realm mRealm, boolean isDeleteAll) {
        // 버스정류소 리스트URL
        StringBuilder urlBuilder = new StringBuilder(openapiDomain);
        urlBuilder.append("BusSttnInfoInqireService/getSttnNoList");
        urlBuilder.append("?ServiceKey=%2FINPAsm7NTY0H7pQwDLNdW5dFd%2FhZxqvngMPEUKPW2de5TVRU2fhgI6x6CsUpkhjJYmH5tG4vYCahsntFWxJ%2Bg%3D%3D"); // 공공데이터 인증키
        urlBuilder.append("&numOfRows=9999"); // 검색건수
        urlBuilder.append("&pageNo=1"); // 페이지 번호
        urlBuilder.append("&cityCode="); // 도시코드
        urlBuilder.append(getDaeguCityCode()); // 대구도시코드
        //    urlBuilder.append("&nodeNm="); // 찾을 정류소명

        try {
            Document doc = Jsoup.connect(urlBuilder.toString()).timeout(10000).get();
            Elements elems = doc.select("item");
            if (!elems.isEmpty()) {
                mRealm.beginTransaction();
                if( isDeleteAll ) // 모든 BusStop 객체 삭제
                    mRealm.where(BusStop.class).findAll().deleteAllFromRealm();

                for (Element elem : elems) {
                    BusStop bs = mRealm.createObject(BusStop.class);
                    bs.setGpslati(Double.parseDouble(elem.getElementsByTag("gpslati").text()));
                    bs.setGpslong(Double.parseDouble(elem.getElementsByTag("gpslong").text()));
                    bs.setId(elem.getElementsByTag("nodeid").text().substring(3));
                    bs.setName(elem.getElementsByTag("nodenm").text());
                }
                mRealm.commitTransaction();
            }

            String busStopNoListUrl = "http://businfo.daegu.go.kr/ba/arrbus/arrbus.do?act=findByBusStopNo&bsNm="; // 정류소번호리스트 URL
            doc = Jsoup.connect(busStopNoListUrl).timeout(10000).get();
            Elements titles = doc.select("tbody tr");
            if (!titles.isEmpty()) {
                mRealm.beginTransaction();
                for (Element elem : titles) {
                    String name = elem.child(0).text();
                    String no = elem.child(1).text();
                    if (!no.equals("0")) {
                        BusStop bs = mRealm.where(BusStop.class).equalTo("name", name).findFirst();
                        if (bs != null)
                            bs.setNo(no);
                    }
                }
                mRealm.commitTransaction();

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Route(노선) Realm DB 구축
     *
     * @param mRealm      해당 메소드 실행하는 쓰레드의 Realm 인스턴스
     * @param isDeleteAll Realm 내의 Route 데이터 삭제 여부
     */
    public void createRouteDB(Realm mRealm, boolean isDeleteAll) {
        StringBuilder urlBuilder = new StringBuilder(openapiDomain);
        urlBuilder.append("BusRouteInfoInqireService/getRouteNoList");
        urlBuilder.append("?ServiceKey=%2FINPAsm7NTY0H7pQwDLNdW5dFd%2FhZxqvngMPEUKPW2de5TVRU2fhgI6x6CsUpkhjJYmH5tG4vYCahsntFWxJ%2Bg%3D%3D"); // 공공데이터 인증키
        urlBuilder.append("&numOfRows=9999"); // 검색건수
        urlBuilder.append("&pageNo=1"); // 페이지 번호
        urlBuilder.append("&cityCode="); // 도시코드
        urlBuilder.append(getDaeguCityCode()); // 대구도시코드
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

                    /*// 노선의 세부정보 파싱
                    urlBuilder.setLength(0);
                    urlBuilder.append(openapiDomain); // 찾을 노선명
                    urlBuilder.append("BusRouteInfoInqireService/getRouteInfoIem");
                    urlBuilder.append("?ServiceKey=%2FINPAsm7NTY0H7pQwDLNdW5dFd%2FhZxqvngMPEUKPW2de5TVRU2fhgI6x6CsUpkhjJYmH5tG4vYCahsntFWxJ%2Bg%3D%3D"); // 공공데이터 인증키
                    urlBuilder.append("&numOfRows=9999"); // 검색건수
                    urlBuilder.append("&pageNo=1"); // 페이지 번호
                    urlBuilder.append("&cityCode="); // 도시코드
                    urlBuilder.append(getDaeguCityCode()); // 대구도시코드
                    urlBuilder.append("&routeId=DGB"); // 도시코드
                    urlBuilder.append(route.getId()); // 노선ID
                    doc = Jsoup.connect(urlBuilder.toString()).get();
                    Elements routeInfoElems = doc.select("item");
                    if( !routeInfoElems.isEmpty() ) {
                        Element routeInfoElem = routeInfoElems.get(0);

                        // 방면 정보가 존재하는 경우 direction 변수 저장
                        String routeNo = routeInfoElem.getElementsByTag("routeno").text();
                        int offset = routeNo.indexOf("[");
                        if( offset != -1 ) {
                            route.setNo( routeNo.substring(0, offset) );
                            route.setDirection( routeNo.substring(offset+1, routeNo.length()-1) ); // 대괄호를 빼기 위해 +1, -1을 함
                        } else
                            route.setNo( routeNo );

                        String routeTp = routeInfoElem.getElementsByTag("routetp").text();
                        for( BusType busType: BusType.values() ) {
                            if(busType.getName(true).equals( routeTp )) {
                                route.setType(busType.name());
                                break;
                            }
                        }
                        route.setStartBusStopName( routeInfoElem.getElementsByTag("startnodenm").text() );
                        route.setEndBusStopName( routeInfoElem.getElementsByTag("endnodenm").text() );

                        String startTime = routeInfoElem.getElementsByTag("startvehicletime").text();
                        String endTime = routeInfoElem.getElementsByTag("endvehicletime").text();
                        route.setStartHour( Integer.parseInt(startTime.substring(0, 2)) );
                        route.setStartMin( Integer.parseInt(startTime.substring(2)) );;
                        route.setEndHour( Integer.parseInt(endTime.substring(0, 2)) );
                        route.setEndMin( Integer.parseInt(endTime.substring(2)) );

                        // 배차간격은 휴일만 있는 것도 있고 평일만 있는 것이 존재하는 것을 확인
                        Elements intervalTimeElem = routeInfoElem.getElementsByTag("intervaltime");
                        Elements intervalSatTimeElem = routeInfoElem.getElementsByTag("intervalsattime");
                        Elements intervalSunTimeElem = routeInfoElem.getElementsByTag("intervalsuntime");
                        if( !intervalTimeElem.isEmpty() )
                            route.setInterval( Integer.parseInt(intervalTimeElem.text()) );
                        if( !intervalSatTimeElem.isEmpty() )
                            route.setIntervalSat( Integer.parseInt(intervalSatTimeElem.text()) );
                        if( !intervalSunTimeElem.isEmpty() )
                            route.setIntervalSun( Integer.parseInt(intervalSunTimeElem.text()) );
                    }*/
                }
                mRealm.commitTransaction();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 버스정류장의 노선별 도착정보를 얻습니다. routeArrInfos 파라미터에 null을 넣을 시에는 반드시 반환값을 사용하세요.
     *
     * @param mRealm        현재 쓰레드에서 만든 realm 객체
     * @param routeArrInfos 만약, Adapter에 routeArrInfos가 없으면 null
     * @param busStopId     the bus stop id
     * @return 버스정류장의 노선별 도착정보 배열
     */
    public RouteArrInfo[] getBusStopArrInfos(Realm mRealm, RouteArrInfo[] routeArrInfos, String busStopId) {
        StringBuilder urlBuilder = new StringBuilder(daeguDomain);
        urlBuilder.append("realTime.do?act=arrInfoRouteList&bsNm=&bsId=");
        urlBuilder.append(busStopId);

        try {
            if( routeArrInfos == null ) {
                if( routeArrInfoList == null )
                    routeArrInfoList = new ArrayList<>();
                else
                    routeArrInfoList.clear();

                Document doc = Jsoup.connect(urlBuilder.toString()).get();
                Elements routeElems = doc.select("li.nx a");
                if (!routeElems.isEmpty()) {
                    int routeElemsSize = routeElems.size();

                    for(int routeArrInfoIndex = 0; routeArrInfoIndex < routeElemsSize; routeArrInfoIndex++) {
                        Element routeElem = routeElems.get(routeArrInfoIndex);

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
            } else {
                for(int routeArrInfoIndex = 0; routeArrInfoIndex < routeArrInfos.length; routeArrInfoIndex++) {
                    String routeId = routeArrInfos[routeArrInfoIndex].getMRoute().getId();
                    int moveDir = routeArrInfos[routeArrInfoIndex].getMoveDir();
                    routeArrInfos[routeArrInfoIndex].setArrInfoArray( getArrInfos(busStopId, routeId, moveDir) );
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return routeArrInfos;
    }

    // getBusStopArrInfos() 메소드에서만 사용하는 메소드
    private ArrInfo[] getArrInfos(String busStopId, String routeId, int moveDir) {
        StringBuilder urlBuilder = new StringBuilder(daeguDomain);
        urlBuilder.append("realTime.do?act=arrInfoRoute&bsNm=&roNo=&bsId=");
        urlBuilder.append(busStopId);
        urlBuilder.append("&roId=");
        urlBuilder.append(routeId);
        urlBuilder.append("&moveDir=");
        urlBuilder.append(moveDir);

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
                    }
                    else {
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

    // 버스위치정보 검색
    public LinkedHashMap<String,String> getRouteByUrl(String url) {
        Document doc;
        try {
            doc = Jsoup.connect(url).get();
            Elements titles = doc.select(".bl");
            LinkedHashMap<String, String> linkList = new LinkedHashMap<String, String>();
            if( !titles.isEmpty() ) {
                for(Element e : titles) {
                    for(Element e2 : e.children()) {
                        if( e2.classNames().contains("bloc_b") ) { // nsbus는 저상버스
                            System.out.print("위치 : ");
                            System.out.println(e2.text());
                        }
                        else
                            System.out.println(e2.child(1).text().substring( e2.child(1).text().indexOf(". ")+2 ));
                    }
                    //System.out.println(e.text());
                }
            }

            return linkList;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
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
