package kr.ac.knu.odego.common;

import io.realm.Realm;
import kr.ac.knu.odego.item.BusStop;
import kr.ac.knu.odego.item.Favorite;
import kr.ac.knu.odego.item.Route;

/**
 * Created by BHI on 2016-06-06.
 */
public final class RealmTransaction {
    public static void createRouteFavorite(Realm mRealm, final String routeId) {
        mRealm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                Route mRoute = realm.where(Route.class).equalTo("id", routeId).findFirst();
                Favorite mFavorite = realm.createObject(Favorite.class);
                mFavorite.setMRoute(mRoute);
                int maxIndex = realm.where(Favorite.class).max("index").intValue();
                mFavorite.setIndex(maxIndex + 1);
            }
        });
    }

    public static void deleteRouteFavorite(Realm mRealm, final String routeId) {
        mRealm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                realm.where(Favorite.class).equalTo("mRoute.id", routeId).findAll().deleteAllFromRealm();
            }
        });
    }

    public static void createBusStopFavorite(Realm mRealm, final String busStopId) {
        mRealm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                BusStop mBusStop = realm.where(BusStop.class).equalTo("id", busStopId).findFirst();
                Favorite mFavorite = realm.createObject(Favorite.class);
                mFavorite.setMBusStop(mBusStop);
                int maxIndex = realm.where(Favorite.class).max("index").intValue();
                mFavorite.setIndex(maxIndex + 1);
            }
        });
    }

    public static void deleteBusStopFavorite(Realm mRealm, final String busStopId) {
        mRealm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                realm.where(Favorite.class).equalTo("mBusStop.id", busStopId).findAll().deleteAllFromRealm();
            }
        });
    }
}
