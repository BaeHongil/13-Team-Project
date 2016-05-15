package kr.ac.knu.odego.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;

import kr.ac.knu.odego.common.ListItemView;
import kr.ac.knu.odego.item.BusStopItem;

/**
 * Created by BHI on 2016-05-14.
 */
public class BusStopListAdapter extends BaseAdapter {
    private Context mContext;
    private ArrayList<BusStopItem> mBusStopItemList;

    public BusStopListAdapter(Context mContext) {
        this.mContext = mContext;

        setmBusStopItemList( new ArrayList<BusStopItem>() );
    }

    public void setmBusStopItemList(ArrayList<BusStopItem> mBusStopItemList) {
        this.mBusStopItemList = mBusStopItemList;
    }

    public ArrayList<BusStopItem> getmBusStopItemList() {
        return mBusStopItemList;
    }

    @Override
    public int getCount() {
        return mBusStopItemList.size();
    }

    @Override
    public Object getItem(int position) {
        return mBusStopItemList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ListItemView mListItemView;
        BusStopItem mBusStopItem = mBusStopItemList.get(position);
        if( convertView != null ) {
            mListItemView = (ListItemView) convertView;
            mListItemView.setItemNameText(mBusStopItem.getBsNm());
        } else
            mListItemView = new ListItemView(mContext, mBusStopItem);

        return mListItemView;
    }
}
