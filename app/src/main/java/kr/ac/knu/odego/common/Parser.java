package kr.ac.knu.odego.common;

import android.content.Context;

import com.google.gson.Gson;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import io.realm.Realm;
import kr.ac.knu.odego.OdegoApplication;
import kr.ac.knu.odego.R;
import kr.ac.knu.odego.item.ArrInfo;
import kr.ac.knu.odego.item.BusPosInfo;
import kr.ac.knu.odego.item.BusStop;
import kr.ac.knu.odego.item.NotiReqMsg;
import kr.ac.knu.odego.item.Route;
import kr.ac.knu.odego.item.RouteArrInfo;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Parser {
    private String daeguDomain;
    private String openapiDomain;
    private String busStopNoListUrl;
    private String apiServiceKey;
    private String daeguCityCode;
    private String appServerDomain;
    private String daeguRouteInfoUrl;
    private ArrayList<RouteArrInfo> routeArrInfoList;
    private OkHttpClient client;
    private Boolean isAppServer;
    private Gson gson;

    private Parser() {
        Context mContext = OdegoApplication.getContext();
        daeguDomain = mContext.getString(R.string.daegu_domain);
        openapiDomain = mContext.getString(R.string.open_api_domain);
        busStopNoListUrl = mContext.getString(R.string.busstopno_list_url);
        apiServiceKey = mContext.getString(R.string.api_service_key);
        appServerDomain = mContext.getString(R.string.appserver_doamin);
        daeguRouteInfoUrl = mContext.getString(R.string.daegu_route_info_url);
        client = new OkHttpClient();
        gson = new Gson();
    }

    private static class ParserHolder { // Initialization-on-demand holder idiom
        public static final Parser INSTANCE = new Parser();
    }

    public static Parser getInstance() {
        return ParserHolder.INSTANCE;
    }

    /**
     * AppServer가 살아있는지 확인
     *
     * @return 살아있으면 true, 아니면 false
     */
    public boolean isAppServer() {
        if( isAppServer == null ) {
            String url = appServerDomain;
            Request request = new Request.Builder()
                    .url(url)
                    .build();
            try {
                Response response = client.newCall(request).execute();
                isAppServer = (response.code() == 200);
            } catch (IOException e) { // 서버 접속 실패
                isAppServer = false;
            }
        }
        return isAppServer;
    }

    /**
     * 공공데이터에서 사용하는 대구도시코드 얻기.
     *
     * @return 대구도시코드 daegu city code
     * @throws IOException 네트워크 오류 발생 try-catch로 UI스레드에서 처리요망
     */
    public String getDaeguCityCode() throws IOException {
        if( daeguCityCode == null) {
            String url = openapiDomain +
                    "/BusSttnInfoInqireService/getCtyCodeList" +
                    "?ServiceKey=" + apiServiceKey/*Service Key*/ +
                    "&numOfRows=999"/*검색건수*/ +
                    "&pageNo=1";

            Document doc = Jsoup.connect(url).get();
            Elements elems = doc.select("item");
            for (Element elem : elems) {
                if (elem.getElementsByTag("sname").text().contains("대구"))
                    daeguCityCode = elem.getElementsByTag("code").text();
            }
        }

        return daeguCityCode;
    }

    /**
     * BusStop(버스정류장) Realm DB 구축
     *
     * @param mRealm      현재 쓰레드에서 생성한 realm 인스턴스
     * @param isDeleteAll Realm 내의 BusStop 데이터 삭제 여부
     * @throws IOException 네트워크 오류 발생 try-catch로 UI스레드에서 처리요망
     */
    public void createBusStopDB(Realm mRealm, boolean isDeleteAll) throws IOException {
        if(isAppServer()) {
            String url = appServerDomain + "/busstops";
            Request request = new Request.Builder()
                    .url(url)
                    .build();

            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) throw new IOException("response fail");

            mRealm.beginTransaction();
            if( isDeleteAll ) // 모든 BusStop 객체 삭제
                mRealm.where(BusStop.class).findAll().deleteAllFromRealm();
            mRealm.createAllFromJson(BusStop.class, response.body().byteStream());
            mRealm.commitTransaction();

            return;
        }

        /* AppServer 죽었을 때 직접 파싱 */
        // 버스정류소 리스트URL
        String url = openapiDomain +
                "/BusSttnInfoInqireService/getSttnNoList" +
                "?ServiceKey=" + apiServiceKey + // 공공데이터 인증키
                "&numOfRows=9999" + // 검색건수
                "&pageNo=1" + // 페이지 번호
                "&cityCode=" + getDaeguCityCode();

        /* 버스정류소 리스트 획득 시작 */
        Document doc = Jsoup.connect(url).timeout(10000).get();
        Elements busStopElems = doc.select("item");
        if (busStopElems.isEmpty())
            return;

        mRealm.beginTransaction();
        if( isDeleteAll ) // 모든 BusStop 객체 삭제
            mRealm.where(BusStop.class).findAll().deleteAllFromRealm();

        for (Element busStopElem : busStopElems) {
            BusStop bs = mRealm.createObject(BusStop.class);
            bs.setGpsLati(Double.parseDouble(busStopElem.getElementsByTag("gpslati").text()));
            bs.setGpsLong(Double.parseDouble(busStopElem.getElementsByTag("gpslong").text()));
            bs.setId(busStopElem.getElementsByTag("nodeid").text().substring(3));
            bs.setName(busStopElem.getElementsByTag("nodenm").text());
        }
        /* 버스정류소 리스트 획득 끝 */
        /* 버스정류소번호 리스트 획득 시작 */
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
    }

    /**
     * Route(노선) Realm DB 구축
     *
     * @param mRealm      현재 쓰레드에서 생성한 realm 인스턴스
     * @param isDeleteAll Realm 내의 Route 데이터 삭제 여부
     * @throws IOException 네트워크 오류 발생 try-catch로 UI스레드에서 처리요망
     */
    public void createRouteDB(Realm mRealm, boolean isDeleteAll) throws IOException {
        if(isAppServer()) {
            String url = appServerDomain + "/routes";
            Request request = new Request.Builder()
                    .url(url)
                    .build();
            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) throw new IOException("response fail");

            mRealm.beginTransaction();
            if( isDeleteAll ) // 모든 Route객체 삭제
                mRealm.where(Route.class).findAll().deleteAllFromRealm();
            mRealm.createAllFromJson(Route.class, response.body().byteStream());
            mRealm.commitTransaction();
            return;
        }

        /* AppServer 죽었을 때 직접 파싱 */
        String url = openapiDomain +
                "/BusRouteInfoInqireService/getRouteNoList" +
                "?ServiceKey=" + apiServiceKey + // 공공데이터 인증키
                "&numOfRows=9999" + // 검색건수
                "&pageNo=1" + // 페이지 번호
                "&cityCode=" + getDaeguCityCode();

        // 노선목록 파싱
        Document doc = Jsoup.connect(url).timeout(10000).get();
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
                for( RouteType routeType : RouteType.values() ) {
                    String strBusType = routeType.getName(true);
                    if(strBusType.equals(routeTp)) {
                        route.setType( routeType.getName() );
                        break;
                    }
                }
            }
            mRealm.commitTransaction();
        }
    }

    /**
     * 버스정류장의 노선별 도착정보를 얻습니다.
     * 노선별 도착정보 최초생성시 사용하는 메소드입니다.
     *
     * @param busStopId 버스정류장 ID
     * @return 버스정류장의 노선별 도착정보 배열
     * @throws IOException 네트워크 오류 발생 try-catch로 UI스레드에서 처리요망
     */
    public RouteArrInfo[] getBusStopArrInfos(String busStopId) throws IOException {
        if(isAppServer()) {
            String url = appServerDomain + "/busstops/" + busStopId + "/arrinfos";
            Request request = new Request.Builder()
                    .url(url)
                    .build();

            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) throw new IOException("response fail");

            RouteArrInfo[] routeArrInfos = gson.fromJson(response.body().charStream(), RouteArrInfo[].class);
            return routeArrInfos;
        }

        /* AppServer 죽었을 때 직접 파싱 */
        RouteArrInfo[] routeArrInfos = null;
        if( routeArrInfoList == null )
            routeArrInfoList = new ArrayList<>();
        else
            routeArrInfoList.clear();

        String url = daeguDomain +
                "/realTime.do?act=arrInfoRouteList&bsNm=&bsId=" +
                busStopId;
        Document doc = Jsoup.connect(url).get();
        Elements routeElems = doc.select("li.nx a");
        if (!routeElems.isEmpty()) {
            int routeElemsSize = routeElems.size();

            for(Element routeElem : routeElems) {
                String href = routeElem.attr("href");
                int startOffset = href.indexOf("roId="); // routeID 추출
                int endOffset = href.indexOf("&roNo=");
                if ( startOffset + 5 != endOffset && startOffset != -1 && endOffset != -1 ) { // startOffset == endOffset이면 전체통합노선의 경우라 제외(ex: 523(전체))
                    String routeId = href.substring(startOffset + 5, endOffset);
                    boolean isForward = href.substring(href.indexOf("moveDir=") + 8).equals("1");

                    ArrInfo[] arrInfos = getArrInfos(busStopId, routeId, isForward);
                    routeArrInfoList.add(new RouteArrInfo(routeId, isForward, arrInfos));
                }
            }
        }

        routeArrInfos = routeArrInfoList.toArray(new RouteArrInfo[routeArrInfoList.size()]);
        return routeArrInfos;
    }

    /**
     * 버스정류장의 노선별 도착정보를 업데이트합니다.
     * 최초생성시에는 getBusStopArrInfos(Realm mRealm, String busStopId)을 사용하고, 해당 메소드는 routeArrInfos가 있을 때만 사용하세요.
     *
     * @param busStopId     the bus stop id
     * @param routeArrInfos null값이 아닌 인스턴스
     * @return 버스정류장의 노선별 도착정보 배열
     * @throws IOException 네트워크 오류 발생 try-catch로 UI스레드에서 처리요망
     *
     * @deprecated getBusStopArrInfos()를 사용하세요. AppServer를 이용하기 때문에 update가 의미가 없습니다.
     */
    @Deprecated
    public RouteArrInfo[] updateBusStopArrInfos(String busStopId, RouteArrInfo[] routeArrInfos) throws IOException {
        for (RouteArrInfo routeArrInfo : routeArrInfos) {
            String routeId = routeArrInfo.getRouteId();
            boolean isForward = routeArrInfo.isForward();
            routeArrInfo.setArrInfos(getArrInfos(busStopId, routeId, isForward));
        }

        return routeArrInfos;
    }

    /**
     * getBusStopArrInfos(), updateBusStopArrInfos() 메소드들에서만 사용하는 메소드
     *
     * @param busStopId  버스정류장ID
     * @param routeId  노선ID
     * @param isForward  노선방향, 0이면 역방향, 1이면 정방향
     * @return 하나의 노선도착정보 배열
     * @throws IOException 네트워크 오류 발생 try-catch로 UI스레드에서 처리요망
     */
    private ArrInfo[] getArrInfos(String busStopId, String routeId, boolean isForward) throws IOException {
        String url = daeguDomain +
                "/realTime.do?act=arrInfoRoute&bsNm=&roNo=&bsId=" + busStopId +
                "&roId=" + routeId +
                "&moveDir=" + (isForward ? 1 : 0);

        Document doc = Jsoup.connect(url).get();
        Elements arrInfoElems = doc.select("table.air tbody");
        ArrInfo[] arrInfos = null;
        if( !arrInfoElems.isEmpty() ) {
            int arrInfoElemsSize = arrInfoElems.size();
            arrInfos = new ArrInfo[arrInfoElemsSize];

            for(int arrInfoIndex = 0; arrInfoIndex < arrInfoElemsSize; arrInfoIndex++) {
                Element arrInfoElem = arrInfoElems.get(arrInfoIndex);
                ArrInfo arrInfo = new ArrInfo();
                arrInfo.setRemainMin( Integer.parseInt( arrInfoElem.getElementsByClass("st").text() ) );

                String curBusStopName, endBusStopName;
                for(Element elem : arrInfoElem.select("tr")) {
                    String tr = elem.select("tr").text().trim();
                    if( tr.equals("현재정류소") )
                        arrInfo.setCurBusStopName(elem.select("td").text());
                    else if( tr.equals("종료정류소") )
                        arrInfo.setEndBusStopName(elem.select("td").text());
                    else if( tr.equals("남은정류소") ) {
                        String strRemainBusStopCount = elem.select("td").text();
                        arrInfo.setRemainBusStopCount( Integer.parseInt(
                                strRemainBusStopCount.substring(0, strRemainBusStopCount.indexOf("개소")
                                )) );
                    }
                }
                arrInfos[arrInfoIndex] = arrInfo;
            }
        } else {  // 도착 정보가 없을 때 -> "기점에서 버스가 출발 대기중이거나 운행 정보가 없습니다.", "기점에서 22시 26분에 출발예정입니다. "
            arrInfos = new ArrInfo[1];
            arrInfos[0] = new ArrInfo();

            arrInfoElems = doc.select("p.gd");
            arrInfos[0].setMessage(arrInfoElems.text());
        }

        return arrInfos;
    }

    /**
     * 노선의 상세정보(기점, 종점, 배차간격 등)를 얻습니다.
     * 주로, 버스위치정보 얻을 때 getBusPosInfos()와 함께 사용합니다.
     *
     * @param mRealm  현재 쓰레드에서 생성한 realm 인스턴스
     * @param routeId 노선ID
     * @return 업데이트한 노선
     * @throws IOException 네트워크 오류 발생 try-catch로 UI스레드에서 처리요망
     */
    public Route getRouteDetailInfo(Realm mRealm, String routeId) throws IOException {
        if( isAppServer() ) {
            String url = appServerDomain + "/routes/" + routeId + "/detailinfo";
            Request request = new Request.Builder()
                    .url(url)
                    .build();

            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) throw new IOException("response fail");

            mRealm.beginTransaction();
            Route mRoute = mRealm.createOrUpdateObjectFromJson(Route.class, response.body().byteStream());
            mRealm.commitTransaction();
            return mRoute;
        }

        String url = openapiDomain +
                "/BusRouteInfoInqireService/getRouteInfoIem" +
                "?ServiceKey=" + apiServiceKey + // 공공데이터 인증키
                "&numOfRows=9999" + // 검색건수
                "&pageNo=1" + // 페이지 번호
                "&cityCode=" + getDaeguCityCode() + // 대구도시코드
                "&routeId=DGB" + routeId;

         /* 공공데이터에서 노선상세정보 가져오기 시작 */
        Document doc = Jsoup.connect(url).get();
        Elements routeInfoElems = doc.select("item");
        if (routeInfoElems.isEmpty())
            return getRouteById(mRealm, routeId);

        Element routeInfoElem = routeInfoElems.get(0);

        Route mRoute = mRealm.where(Route.class).equalTo("id", routeId).findFirst();
        mRealm.beginTransaction(); // mRoute를 DB에 반영할 때만 사용
        mRoute.setStartBusStopName(routeInfoElem.getElementsByTag("startnodenm").text());
        mRoute.setEndBusStopName(routeInfoElem.getElementsByTag("endnodenm").text());

        String startTime = routeInfoElem.getElementsByTag("startvehicletime").text();
        String endTime = routeInfoElem.getElementsByTag("endvehicletime").text();
        mRoute.setStartHour(Integer.parseInt(startTime.substring(0, 2)));
        mRoute.setStartMin(Integer.parseInt(startTime.substring(2)));
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
        mRoute.setUpdatedDetail(new Date());
        mRealm.commitTransaction();
        /* 공공데이터에서 노선상세정보 가져오기 끝 */

        return mRoute;
    }

    /**
     *  노선정보가 DB에 없을 때 routeId를 통해 획득
     *
     * @param routeId
     * @return
     * @throws IOException 네트워크 오류 발생 try-catch로 UI스레드에서 처리요망
     */
    public Route getRouteById(Realm mRealm, String routeId) throws IOException {
        if( isAppServer() ) {
            String url = appServerDomain + "/routes/" + routeId;
            Request request = new Request.Builder()
                    .url(url)
                    .build();

            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) throw new IOException("response fail");

            Route route = gson.fromJson(response.body().charStream(), Route.class);
            mRealm.beginTransaction();
            mRealm.copyToRealmOrUpdate(route);
            mRealm.commitTransaction();
            return route;
        }

        String url = daeguRouteInfoUrl;

        Document doc = Jsoup.connect(url).get();
        Elements routeElem = doc.select(".route_detail .align_left");
        if(routeElem.isEmpty())
            return null;

        Route mRoute = new Route();
        mRoute.setId(routeId);
        mRoute.setNo( routeElem.get(0).text() );
        mRoute.setStartBusStopName(routeElem.get(1).text());
        mRoute.setEndBusStopName(routeElem.get(2).text());
        String rawInterval = routeElem.get(3).text();
        if( rawInterval != null )
            mRoute.setInterval( Integer.parseInt(rawInterval) );
        mRoute.setUpdatedDetail( new Date() );

        mRealm.beginTransaction();
        mRealm.copyToRealmOrUpdate(mRoute);
        mRealm.commitTransaction();

        return mRoute;
    }

    /**
     * 특정 노선의 모든 버스위치정보를 얻습니다.
     * 기존의 버스위치정보가 있을 경우 updateBusPosInfos() 메소드를 사용하세요.
     *
     * @param routeId   검색할 노선 ID
     * @param isForward 정방향이면 true(1), 역방향이면 false(0)
     * @return 생성된 버스위치정보 배열
     * @throws IOException 네트워크 오류 발생 try-catch로 UI스레드에서 처리요망
     */
    public BusPosInfo[] getBusPosInfos(String routeId, boolean isForward) throws IOException {
        if( isAppServer() ) {
            String url = appServerDomain + "/routes/" + routeId + "/busposinfos" + "?isforward="+ isForward;
            Request request = new Request.Builder()
                    .url(url)
                    .build();
            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) throw new IOException("response fail");

            BusPosInfo[] busPosInfos = gson.fromJson(response.body().charStream(), BusPosInfo[].class);
            return busPosInfos;
        }

        /* AppServer 죽었을 때 직접 파싱 */
        BusPosInfo[] busPosInfos = null;
        String url = daeguDomain + "/realTime.do?act=posInfo&roNo=" +
                "&roId=" + routeId +
                "&moveDir=" + (isForward ? 1 : 0);
        /* 대구버스에서 노선 정류소리스트 및 버스위치정보 가져오기 시작 */
        Document doc = Jsoup.connect(url).get();
        Elements listElems = doc.select("ol.bl");
        if (listElems.isEmpty())
            return null;

        // 노선 정류소리스트 저장
        Elements busStopElems = listElems.select("span.pl39");
        int busStopElemsSize = busStopElems.size();
        busPosInfos = new BusPosInfo[busStopElemsSize];
        Realm mRealm = null;
        try {
            mRealm = Realm.getDefaultInstance();
            for (int busPosInfoIndex = 0; busPosInfoIndex < busStopElemsSize; busPosInfoIndex++) {
                Element busStopElem = busStopElems.get( busPosInfoIndex );
                String rawBusStopName = busStopElem.select(".pl39").text();
                int startOffset = rawBusStopName.indexOf(". ");
                String busStopName = rawBusStopName.substring(startOffset + 2);

                BusStop mBusStop = mRealm.where(BusStop.class).equalTo("name", busStopName).findFirst();
                busPosInfos[busPosInfoIndex] = new BusPosInfo(mBusStop.getId());
            }
        } finally {
            if (mRealm != null)
                mRealm.close();
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
        return busPosInfos;
    }

    /**
     * 특정 노선의 모든 버스위치정보를 업데이트합니다.
     * 기존의 버스위치정보가 없을 경우 getBusPosInfos() 먼저 사용하세요.
     *
     * @param routeId     검색할 노선 ID
     * @param isForward   정방향이면 true, 역방향이면 false
     * @param busPosInfos 버스위치정보 배열
     * @return 업데이트된 버스위치정보 배열
     * @throws IOException 네트워크 오류 발생 try-catch로 UI스레드에서 처리요망
     *
     * @deprecated getBusPosInfos()를 사용하세요. AppServer를 이용하기 때문에 update가 의미가 없습니다.
     */
    @Deprecated
    public BusPosInfo[] updateBusPosInfos(String routeId, boolean isForward, BusPosInfo[] busPosInfos) throws IOException {
        for(BusPosInfo mBusPosInfo : busPosInfos) // 버스ID 초기화
            mBusPosInfo.setBusId(null);

        String url = daeguDomain +
                "/realTime.do?act=posInfo&roNo=" +
                "&roId=" + routeId +
                "&moveDir=" + (isForward ? 1 : 0);

        Document doc = Jsoup.connect(url).get();
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
        return busPosInfos;
    }

    /**
     * 버스ID로 현재 버스의 index를 알아냅니다
     *
     * @param routeId   Route Id
     * @param isForward 정방향이면 true, 역방향이면 false
     * @param busId     Bus Id
     * @return 버스 위치 index
     * @throws IOException 네트워크 오류 발생 try-catch로 UI스레드에서 처리요망
     */
    public int getBusPosByBusId(String routeId, boolean isForward, String busId) throws IOException {
        if( isAppServer() ) {
            String url = appServerDomain + "/routes/" + routeId + "/buspos"
                    + "?isforward=" + isForward + "&busid=" + busId;
            Request request = new Request.Builder()
                    .url(url)
                    .build();
            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) throw new IOException("response fail");

            int foundIndex = Integer.parseInt( response.body().string() );
            return foundIndex;
        }

        return -1;
    }

    /**
     * 버스도착알림을 서버에 요청
     *
     * @param mNotiReqMsg 요청메시지 객체
     * @throws IOException 네트워크 오류 발생 try-catch로 UI스레드에서 처리요망
     */
    public void sendNotiReqMsg(NotiReqMsg mNotiReqMsg) throws IOException {
        if( isAppServer() ) {
            String url = appServerDomain + "/notifications/busarr";
            MediaType JSON = MediaType.parse("application/json; charset=utf-8");
            RequestBody requestBody = RequestBody.create(JSON, gson.toJson(mNotiReqMsg));
            Request request = new Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build();
            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) throw new IOException("response fail");
        }
    }

    /**
     * 버스도착정류장 수정을 서버에 요청
     *
     * @param fcmToken  FCM 토큰
     * @param destIndex 수정할 도착정류장 index
     * @throws IOException 네트워크 오류 발생 try-catch로 UI스레드에서 처리요망
     */
    public void sendNotiModMsg(String fcmToken, int destIndex) throws IOException {
        if ( isAppServer() ) {
            String url = appServerDomain + "/notifications/busarr/" + fcmToken;
            MediaType JSON = MediaType.parse("application/json; charset=utf-8");
            RequestBody requestBody = RequestBody.create(JSON, "{\"destIndex\":" + destIndex + "}");
            Request request = new Request.Builder()
                    .url(url)
                    .put(requestBody)
                    .build();
            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) throw new IOException("response fail");
        }
    }

    /**
     * 이전 버스도착알림 삭제를 서버에 요청
     *
     * @param fcmToken FCM 토큰
     * @throws IOException 네트워크 오류 발생 try-catch로 UI스레드에서 처리요망
     */
    public void sendNotiDelMsg(String fcmToken) throws IOException {
        if( isAppServer() ) {
            String url = appServerDomain + "/notifications/busarr/" + fcmToken;
            Request request = new Request.Builder()
                    .url(url)
                    .delete()
                    .build();
            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) throw new IOException("response fail");
        }
    }

/*
    *//**
     * String 값에 숫자만 들어있는지 확인
     *
     * @param str 확인할 String
     * @return 숫자만 들어있으면 True, 아니면 False
     *//*
    public Boolean isNum(String str) {
        if( Pattern.matches("^[0-9]+$", str) )
            return true;
        return false;
    }*/
}
