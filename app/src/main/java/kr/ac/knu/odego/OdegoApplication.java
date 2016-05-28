package kr.ac.knu.odego;

import android.app.Application;
import android.content.Context;

import io.realm.Realm;
import io.realm.RealmConfiguration;

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
    }

    public static Context getContext() {
        return context;
    }
}
