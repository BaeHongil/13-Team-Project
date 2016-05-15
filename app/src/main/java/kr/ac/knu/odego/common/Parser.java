package kr.ac.knu.odego.common;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import kr.ac.knu.odego.item.BusStopItem;
import kr.ac.knu.odego.item.RouteItem;

public class Parser {
    private static String domain = "http://m.businfo.go.kr/bp/m/";

    public Parser() {
        // TODO Auto-generated constructor stub
    }

    // 버스노선 검색
    public static ArrayList<RouteItem> getRouteListByNo(ArrayList<RouteItem> mRouteItemList, String no) {
        Document doc;
        try {
            String url = domain + "realTime.do?act=posInfoMain&roNo=" + URLEncoder.encode(no, "euc-kr");
            doc = Jsoup.connect(url).get();
            Elements titles = doc.select("ul.bl.mr15 .nx a");
            mRouteItemList.clear();
            if( !titles.isEmpty() ) {
                for(Element e: titles) {
                    String routeNo = e.text();
                    int offset = routeNo.indexOf("(");
                    if( offset != -1 )
                        mRouteItemList.add(new RouteItem( routeNo.substring(0, offset), e.absUrl("href"), routeNo.substring(offset) ));
                    else
                        mRouteItemList.add(new RouteItem( routeNo, e.absUrl("href") ));
                }

            }
            else if( doc.select("ul.bl.mr15 li.gd").isEmpty() )
                mRouteItemList.add(new RouteItem(no, url));

            return mRouteItemList;
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        return null;
    }

    // 버스위치정보 검색
    public static LinkedHashMap<String,String> getRouteByUrl(String url) {
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

    // 버스정류장 검색
    public static ArrayList<BusStopItem> getBusStopListByWord(ArrayList<BusStopItem> mbusStopItemsList, String word) {
        Document busStopListDoc, busStopNoDoc;
        try {
            String busStopListurl = domain + "realTime.do?act=arrInfoMain&bsNm=" + URLEncoder.encode(word, "euc-kr");
            String busStopNoUrl = "http://businfo.daegu.go.kr/ba/arrbus/arrbus.do?act=findByBusStopNo&bsNm=" + URLEncoder.encode(word, "euc-kr");

            busStopListDoc = Jsoup.connect(busStopListurl).get();
            busStopNoDoc = Jsoup.connect(busStopNoUrl).get();
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
                mbusStopItemsList.add( new BusStopItem(word, busStopListurl, noElems.get(0).text()) );

            return mbusStopItemsList;
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        return null;
    }
}
