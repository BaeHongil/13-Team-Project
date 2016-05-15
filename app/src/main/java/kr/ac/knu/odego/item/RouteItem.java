package kr.ac.knu.odego.item;

/**
 * Created by BHI on 2016-05-14.
 */
public class RouteItem {
    private String roNo;
    private String url;

    public RouteItem(String roNo, String url) {
        setRoNo(roNo);
        setUrl(url);
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
}
