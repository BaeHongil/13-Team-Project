package kr.ac.knu.odego;

import com.google.gson.Gson;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.net.SocketTimeoutException;

import kr.ac.knu.odego.item.BusPosInfo;
import kr.ac.knu.odego.item.BusStop;
import kr.ac.knu.odego.item.Route;
import kr.ac.knu.odego.item.RouteArrInfo;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
public class ExampleUnitTest {
    OkHttpClient client = new OkHttpClient();

    @Before
    public void Before() {
    }
    /**
     * Get the method name.
     *
     * @param e Thread.currentThread().getStackTrace()를 넣어주세요
     * @return the method name
     */
    @Ignore
    public String getMethodName(StackTraceElement e[]) {
        boolean doNext = false;
        for (StackTraceElement s : e) {
            if(doNext)
                return s.getMethodName();
            doNext = s.getMethodName().equals("getStackTrace");
        }
        return null;
    }

    @Ignore
    public void getBusStopArrInfosServerTest() throws Exception {
        OkHttpClient client = new OkHttpClient();

        String url = "http://bhi.iptime.org:1313/busstops/7021025700/arrinfos";
        Request request = new Request.Builder()
                .url(url)
                .build();

        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) throw new IOException("response fail");

        Gson gson = new Gson();
        RouteArrInfo[] routeArrInfos = gson.fromJson(response.body().charStream(), RouteArrInfo[].class);
        //System.out.println( response.body().string() );
    }

    @Ignore
    public void getRouteDetailInfoServerTest() throws Exception {
        OkHttpClient client = new OkHttpClient();

        String url = "http://bhi.iptime.org:1313/routes/4060002000/detailinfo";
        Request request = new Request.Builder()
                .url(url)
                .build();

        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) throw new IOException("response fail");

        Gson gson = new Gson();
        Route route = gson.fromJson(response.body().charStream(), Route.class);
        //System.out.println( response.body().string() );
    }

    @Ignore
    public void getBusPosInfosServerTest() throws Exception {
        OkHttpClient client = new OkHttpClient();

        String url = "http://bhi.iptime.org:1313/routes/4060002000/busposinfos?isforward=true";
        Request request = new Request.Builder()
                .url(url)
                .build();
        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) throw new IOException("response fail");

        Gson gson = new Gson();
        BusPosInfo[] busPosInfos = gson.fromJson(response.body().charStream(), BusPosInfo[].class);

        //System.out.println( response.body().string() );
    }

    @Ignore
    public void getBusStopDbServerTest() throws Exception {
        OkHttpClient client = new OkHttpClient();

        String url = "http://bhi.iptime.org:1313/busstops";
        Request request = new Request.Builder()
                .url(url)
                .build();

        try {
            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) throw new IOException("response fail");

            Gson gson = new Gson();
            BusStop[] busStops = gson.fromJson(response.body().charStream(), BusStop[].class);
       //     System.out.println( response.body().string() );
            System.out.println();

        } catch (SocketTimeoutException e) { // 서버 꺼짐
            System.out.println("SocketTimeoutException");
        }
    }

    @Ignore
    public void getRouteDbServerTest() throws Exception {
        OkHttpClient client = new OkHttpClient();

        String url = "http://bhi.iptime.org:1313/routes";
        Request request = new Request.Builder()
                .url(url)
                .build();

        try {
            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) throw new IOException("response fail");

            Gson gson = new Gson();
            Route[] routes = gson.fromJson(response.body().charStream(), Route[].class);
            //     System.out.println( response.body().string() );
            System.out.println();

        } catch (SocketTimeoutException e) { // 서버 꺼짐
            System.out.println("SocketTimeoutException");
        }
    }

    @Ignore
    public void okHttpClientTest() throws Exception {

        String url = "http://bhi.iptime.org:1313/busstops";
        Request request = new Request.Builder()
                .url(url)
                .build();

        try {
            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) throw new IOException("response fail");

            Gson gson = new Gson();

            System.out.println( response.body().string() );
            System.out.println();

        } catch (SocketTimeoutException e) { // 서버 꺼짐
            System.out.println("SocketTimeoutException");
        }

        url = "http://bhi.iptime.org:1313/routes";
        request = new Request.Builder()
                .url(url)
                .build();

        try {
            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) throw new IOException("response fail");

            Gson gson = new Gson();

            System.out.println( response.body().string() );
            System.out.println();

        } catch (SocketTimeoutException e) { // 서버 꺼짐
            System.out.println("SocketTimeoutException");
        }
    }

    @Test
    public void exceptionTest() throws Exception {

        String url = "http://bhi.iptime.org:1313" + "/routes/1000001000/buspos?isforward=true&busid=%EB%8C%80%EA%B5%AC70%EC%9E%90%202824";
        Request request = new Request.Builder()
                .url(url)
                .build();
        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) throw new IOException("response fail");
        System.out.println(response.body().string());
    }
}