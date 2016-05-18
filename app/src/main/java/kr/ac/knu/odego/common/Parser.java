package kr.ac.knu.odego.common;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;

import io.realm.Realm;
import kr.ac.knu.odego.item.BusStop;
import kr.ac.knu.odego.item.Route;

public class Parser {
    private static Parser instance;
    private String daeguDomain = "http://m.businfo.go.kr/bp/m/";
    private String openapiDomain = "http://openapi.tago.go.kr/openapi/service/";
    private HashMap<String, Integer> mBusStopNoMap;
    private String daeguCityCode;

    // 버스정류소 DB구축
    private Parser() {

    }

    public static Parser getInstance() {
        if( instance == null )
            instance = new Parser();
        return instance;
    }

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

    public void createBusStopDB(Realm mRealm, Boolean isDeleteAll) {
        try {
            if( isDeleteAll )
                mRealm.where(BusStop.class).findAll().deleteAllFromRealm();

            // 버스정류소 리스트URL
            StringBuilder urlBuilder = new StringBuilder(openapiDomain);
            urlBuilder.append("BusSttnInfoInqireService/getSttnNoList");
            urlBuilder.append("?ServiceKey=%2FINPAsm7NTY0H7pQwDLNdW5dFd%2FhZxqvngMPEUKPW2de5TVRU2fhgI6x6CsUpkhjJYmH5tG4vYCahsntFWxJ%2Bg%3D%3D"); /*Service Key*/
            urlBuilder.append("&numOfRows=9999"); /*검색건수*/
            urlBuilder.append("&pageNo=1"); /*페이지 번호*/
            urlBuilder.append("&cityCode="); /*도시코드*/
            urlBuilder.append(getDaeguCityCode());
            urlBuilder.append("&nodeNm="); /*정류소명*/

            Document doc = Jsoup.connect(urlBuilder.toString()).timeout(10000).get();
            Elements elems = doc.select("item");
            if (!elems.isEmpty()) {
                mRealm.beginTransaction();
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
                        if (bs != null) {
                            bs.setNo(no);
                        }

                    }

                }
                mRealm.commitTransaction();

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 버스노선 검색
    public ArrayList<Route> getRouteListByNo(ArrayList<Route> mRouteList, String no) {
        Document doc;
        try {
            String url = daeguDomain + "realTime.do?act=posInfoMain&roNo=" + URLEncoder.encode(no, "euc-kr");
            doc = Jsoup.connect(url).get();
            Elements titles = doc.select("ul.bl.mr15 .nx a");
            mRouteList.clear();
            if( !titles.isEmpty() ) {
                for(Element e: titles) {
                    String routeNo = e.text();
                    int offset = routeNo.indexOf("("); // 방면부분 추출
                    if( offset != -1 )
                        mRouteList.add(new Route( routeNo.substring(0, offset), e.absUrl("href"), routeNo.substring(offset) ));
                    else
                        mRouteList.add(new Route( routeNo, e.absUrl("href") ));
                }

            }
            else if( doc.select("ul.bl.mr15 li.gd").isEmpty() )
                mRouteList.add(new Route(no, url));

            return mRouteList;
        } catch (IOException e1) {
            e1.printStackTrace();
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

    /*// 버스정류장 검색
    public ArrayList<BusStopItem> getBusStopListByWord(ArrayList<BusStopItem> mbusStopItemsList, String word) {
        Document busStopListDoc, busStopNoDoc;
        try {
            if( isNum(word) ) {
                String busStopListUrlByNo = "http://businfo.daegu.go.kr/ba/page/winc.do?sec=busstop&SearchType=No&bsNm=" + word;

            } else if( word.length() >= 2 ) {
                String busStopNoUrl = "http://businfo.daegu.go.kr/ba/arrbus/arrbus.do?act=findByBusStopNo&bsNm=" + URLEncoder.encode(word, "euc-kr");

            }
            String busStopListUrl = daeguDomain + "realTime.do?act=arrInfoMain&bsNm=" + URLEncoder.encode(word, "euc-kr");

            busStopListDoc = Jsoup.connect(busStopListUrl).get();
       //     busStopNoDoc = Jsoup.connect(busStopNoUrl).get();
            Elements listElems = busStopListDoc.select("a.pl39");
            Elements noElems = busStopNoDoc.select("td.center");

            mbusStopItemsList.clear();
            if( !listElems.isEmpty() ) {
                for(int i = 0; i < listElems.size(); i++) {
                    Element listElem = listElems.get(i);
                    Element noElem = noElems.get(i);

                    String bsNm = listElem.text();
                    mbusStopItemsList.add( new BusStopItem(bsNm.substring(bsNm.indexOf(". ") + 2), listElem.absUrl("href"), noElem.text()) ); // substring은 정류장이름 앞에 숫자 제거

                }
            }
            else if( !noElems.isEmpty() )
                mbusStopItemsList.add( new BusStopItem(word, busStopListUrl, noElems.get(0).text()) );

            return mbusStopItemsList;
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        return null;
    }*/

    // 문자열 안에 숫자만 있는지 확인
    public Boolean isNum(String str) {
        if( Pattern.matches("^[0-9]+$", str) )
            return true;
        return false;
    }
}
