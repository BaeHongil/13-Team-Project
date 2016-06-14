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
import kr.ac.knu.odego.item.Setting;

/**
 * Created by BHI on 2016-06-06.
 */
public final class RealmTransaction {

    /**
     *  ODEGO 설정데이터를 수정합니다.
     *
     * @param mRealm             Realm 객체(settingRealmConfig으로 생성한 객체)
     * @param requestRemainCount 도착지알림 정류장 수
     */
    public static void modifySetting(Realm mRealm, final int requestRemainCount) {
        mRealm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                Setting mSetting = realm.where(Setting.class).findFirst();
                mSetting.setRequestRemainCount(requestRemainCount);
            }
        });
    }

    /**
     * 즐겨찾기 전체 삭제
     *
     * @param mRealm Realm 객체
     */
    public static void clearFavorite(Realm mRealm) {
        mRealm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                realm.where(Favorite.class).findAll().deleteAllFromRealm();
            }
        });
    }

    /**
     * 검색기록 전체 삭제
     *
     * @param mRealm Realm 객체
     */
    public static void clearSearchHistory(Realm mRealm) {
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

    /**
     * 버스탑승기록 전체 삭제
     *
     * @param mRealm Realm 객체
     */
    public static void clearBeaconArrInfo(Realm mRealm) {
        mRealm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                realm.where(BeaconArrInfo.class).findAll().deleteAllFromRealm();
            }
        });
    }

    /**
     * 노선 즐겨찾기 추가
     *
     * @param mRealm  Realm 객체
     * @param routeId 즐겨찾기 추가할 노선ID
     */
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

    /**
     * 노선 즐겨찾기 삭제
     *
     * @param mRealm  Realm 객체
     * @param routeId 즐겨찾기 삭제할 노선ID
     */
    public static void deleteRouteFavorite(Realm mRealm, final String routeId) {
        mRealm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                realm.where(Favorite.class).equalTo("mRoute.id", routeId).findAll().deleteAllFromRealm();
            }
        });
    }

    /**
     * 버스정류장 즐겨찾기 생성
     *
     * @param mRealm    Realm 객체
     * @param busStopId 즐겨찾기 추가할 버스정류장 id
     */
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

    /**
     * 버스정류장 즐겨찾기 삭제
     *
     * @param mRealm    Realm 객체
     * @param busStopId 즐겨찾기 삭제할 버스정류장 id
     */
    public static void deleteBusStopFavorite(Realm mRealm, final String busStopId) {
        mRealm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                realm.where(Favorite.class).equalTo("mBusStop.id", busStopId).findAll().deleteAllFromRealm();
            }
        });
    }


    /**
     * dest index 생성
     *
     * @param mRealm      Realm 객체
     * @param index       수정할 BeaconArrInfo의 index
     * @param mNotiReqMsg ODEGO서버에 보낼 NotiReqMsg 객체
     * @param mHandler    실행 중인 액티비티의 handler
     */
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

    /**
     * dest index 수정
     *
     * @param mRealm    Realm 객체
     * @param index     수정할 BeaconArrInfo의 index
     * @param fcmToken  fcm token
     * @param destIndex 수정할 dest index
     * @param mHandler  실행 중인 액티비티의 handler
     */
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

    /**
     * dest index 삭제
     *
     * @param mRealm   Realm 객체
     * @param index    수정할 BeaconArrInfo의 index
     * @param fcmToken fcm token
     * @param mHandler 실행 중인 액티비티의 handler
     */
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
