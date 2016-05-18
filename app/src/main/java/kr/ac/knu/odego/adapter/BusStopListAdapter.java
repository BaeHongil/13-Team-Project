package kr.ac.knu.odego.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import io.realm.RealmResults;
import kr.ac.knu.odego.common.ListItemView;
import kr.ac.knu.odego.item.BusStop;

/**
 * Created by BHI on 2016-05-14.
 */
public class BusStopListAdapter extends BaseAdapter {
    private Context mContext;
    private RealmResults<BusStop> mBusStopList;

    public BusStopListAdapter(Context mContext) {
        this.mContext = mContext;
    }

    public void setmBusStopList(RealmResults<BusStop> mBusStopList) {
        this.mBusStopList = mBusStopList;
    }

    @Override
    public int getCount() {
        if(mBusStopList == null)
            return 0;
        return mBusStopList.size();
    }

    @Override
    public Object getItem(int position) {
        if(mBusStopList == null)
            return null;
        return mBusStopList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ListItemView mListItemView;
        BusStop mBusStop = mBusStopList.get(position);
        if( convertView != null ) {
            mListItemView = (ListItemView) convertView;
            mListItemView.setItemNameText(mBusStop.getName());
            mListItemView.setItemDetailText(mBusStop.getNo());
        } else
            mListItemView = new ListItemView(mContext, mBusStop);

        return mListItemView;
    }
}
