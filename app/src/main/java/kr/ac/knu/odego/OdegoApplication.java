package kr.ac.knu.odego;

import android.app.Application;
import android.content.Context;

import com.facebook.stetho.Stetho;
import com.facebook.stetho.okhttp3.StethoInterceptor;
import com.uphyca.stetho_realm.RealmInspectorModulesProvider;

import java.util.Date;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import okhttp3.OkHttpClient;

/**
 * Created by BHI on 2016-05-26.
 */

public class OdegoApplication extends Application {
    private static Context context;
    private RealmConfiguration busInfoRealmConfig;

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;



        busInfoRealmConfig = new RealmConfiguration.Builder(this)
                .name("bus_info.realm")
                .deleteRealmIfMigrationNeeded() // 마이그레이션 필요시 전체삭제 메소드이므로 릴리즈시 삭제요망
                .build();

        Realm.setDefaultConfiguration(busInfoRealmConfig);

        /* Chrome App Stetho 설정 */
        Stetho.initialize(
                Stetho.newInitializerBuilder(this)
                        .enableDumpapp(Stetho.defaultDumperPluginsProvider(this))
                        .enableWebKitInspector(RealmInspectorModulesProvider.builder(this)
                                .withLimit(5000)
                                .build())
                        .build());
        new Thread() {
            @Override
            public void run() {
                new OkHttpClient.Builder()
                        .addNetworkInterceptor(new StethoInterceptor())
                        .build();
            }
        }.start();
    }

    public static Context getContext() {
        return context;
    }

    /**
     * Get the method name.(TEST용 메소드)
     *
     * @param e Thread.currentThread().getStackTrace()를 넣어주세요
     * @return the method name
     */
    public static String getMethodName(StackTraceElement e[]) {
        boolean doNext = false;
        for (StackTraceElement s : e) {
            if(doNext)
                return s.getMethodName();
            doNext = s.getMethodName().equals("getStackTrace");
        }
        return null;
    }

    public static Boolean isToday(Date date) {
        Date today = new Date();
        return !( today.getYear() > date.getYear()
                || today.getMonth() > date.getMonth()
                || today.getDate() > date.getDate() );
    }


}
