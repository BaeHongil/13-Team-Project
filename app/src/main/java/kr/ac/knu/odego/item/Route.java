package kr.ac.knu.odego.item;

/**
 * Created by BHI on 2016-05-14.
 */
public class Route {
    private String roNo;
    private String url;
    private String direction;

    public Route(String roNo, String url) {
        this.roNo = roNo;
        this.url = url;
    }

    public Route(String roNo, String url, String direction) {
        this.roNo = roNo;
        this.url = url;
        this.direction = direction;
    }

    public String getRoNo() {
        return roNo;
    }

    public void setRoNo(String roNo) {
        this.roNo = roNo;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }
}
