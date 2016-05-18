package kr.ac.knu.odego.item;

/**
 * Created by BHI on 2016-05-15.
 */
public class Favorite {
    public final static int ROUTE = 0;
    public final static int BUS_STOP = 1;

    private int type;
    private String name;
    private String url;

    public Favorite(int type, String name, String url) {
        this.type = type;
        this.name = name;
        this.url = url;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
