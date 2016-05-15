package kr.ac.knu.odego.item;

/**
 * Created by BHI on 2016-05-15.
 */
public class BusStopItem {
    private String bsNm;
    private String url;
    private String no;

    public BusStopItem(String bsNm, String url, String no) {
        this.bsNm = bsNm;
        this.url = url;
        this.no = no;
    }

    public String getBsNm() {
        return bsNm;
    }

    public void setBsNm(String bsNm) {
        this.bsNm = bsNm;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getNo() {
        return no;
    }

    public void setNo(String no) {
        this.no = no;
    }
}
