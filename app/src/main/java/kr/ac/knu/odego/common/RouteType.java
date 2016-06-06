package kr.ac.knu.odego.common;

/**
 * Created by BHI on 2016-05-22.
 */
public enum RouteType {
    MAIN("간선버스"), BRANCH("지선버스"), EXPRESS("급행버스"), CIRCULAR("순환버스");

    private String name;

    RouteType(String name) {
        this.name = name;
    }

    /**
     * 버스타입의 한글명 얻기
     *
     * @param isParsing Parsing할 때 사용할 경우 True, 아닐 경우 False
     * @return RouteType name
     */
    public String getName(Boolean isParsing) {
        if( isParsing && name().equals("CIRCULAR") )
            return "시외버스";

        return name;
    }

    public String getName() {
        return name;
    }
}