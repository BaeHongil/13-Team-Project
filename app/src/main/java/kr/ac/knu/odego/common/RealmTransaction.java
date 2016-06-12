package kr.ac.knu.odego.common;

import android.os.Handler;
import android.os.Message;

import java.io.IOException;

import io.realm.Realm;
import io.realm.RealmResults;
import kr.ac.knu.odego.OdegoApplication;
import kr.ac.knu.odego.R;
import kr.ac.knu.odego.item.BeaconArrInfo;
import kr.ac.knu.odego.item.BusStop;
import kr.ac.knu.odego.item.Favorite;
import kr.ac.knu.odego.item.NotiReqMsg;
import kr.ac.knu.odego.item.Route;

/**
 * Created by BHI on 2016-06-06.
 */
public final class RealmTransaction {
    public static void clearHistory(Realm mRealm) {
        mRealm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                RealmResults<BusStop> busStopResults = realm.where(BusStop.class).findAll();
                for( BusStop mBusStop : busStopResults )
                    mBusStop.setHistoryIndex(0);
                RealmResults<Route> RouteResults = realm.where(Route.class).findAll();
                for( Route mRoute : RouteResults )
                    mRoute.setHistoryIndex(0);
            }
        });
    }

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

    public static void createDestIndex(Realm mRealm, final int index, final NotiReqMsg mNotiReqMsg, final Handler mHandler) {
        mRealm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                Parser mParser = Parser.getInstance();
                try {
                    mParser.sendNotiReqMsg(mNotiReqMsg);
                } catch (IOException e) {
                    Message msg = Message.obtain();
                    msg.obj = OdegoApplication.getContext().getString(R.string.network_error_msg);
                    mHandler.sendMessage(msg);
                }

                BeaconArrInfo mBeaconArrInfo = realm.where(BeaconArrInfo.class).equalTo("index", index).findFirst();
                mBeaconArrInfo.setDestIndex(mNotiReqMsg.getDestIndex());
            }
        });
    }

    public static void modifyDestIndex(Realm mRealm, final int index, final String fcmToken, final int destIndex, final Handler mHandler) {
        mRealm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                Parser mParser = Parser.getInstance();
                try {
                    mParser.sendNotiModMsg(fcmToken, destIndex);
                } catch (IOException e) {
                    Message msg = Message.obtain();
                    msg.obj = OdegoApplication.getContext().getString(R.string.network_error_msg);
                    mHandler.sendMessage(msg);
                }

                BeaconArrInfo mBeaconArrInfo = realm.where(BeaconArrInfo.class).equalTo("index", index).findFirst();
                mBeaconArrInfo.setDestIndex(destIndex);
            }
        });
    }

    public static void deleteDestIndex(Realm mRealm, final int index, final String fcmToken, final Handler mHandler) {
        mRealm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                Parser mParser = Parser.getInstance();
                try {
                    mParser.sendNotiDelMsg(fcmToken);
                } catch (IOException e) {
                    Message msg = Message.obtain();
                    msg.obj = OdegoApplication.getContext().getString(R.string.network_error_msg);
                    mHandler.sendMessage(msg);
                }

                BeaconArrInfo mBeaconArrInfo = realm.where(BeaconArrInfo.class).equalTo("index", index).findFirst();
                mBeaconArrInfo.setDestIndex(-1);
            }
        });
    }


}
