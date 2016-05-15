package kr.ac.knu.odego.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;

import kr.ac.knu.odego.common.ListItemView;
import kr.ac.knu.odego.item.RouteItem;

/**
 * Created by BHI on 2016-05-14.
 */
public class RouteListAdapter extends BaseAdapter {
    private Context mContext;
    private ArrayList<RouteItem> mRouteItemList;

    public RouteListAdapter(Context mContext) {
        this.mContext = mContext;

        setmRouteItemList( new ArrayList<RouteItem>() );
    }

    public void setmRouteItemList(ArrayList<RouteItem> mRouteItemList) {
        this.mRouteItemList = mRouteItemList;
    }

    public ArrayList<RouteItem> getmRouteItemList() {
        return mRouteItemList;
    }

    @Override
    public int getCount() {
        return mRouteItemList.size();
    }

    @Override
    public Object getItem(int position) {
        return mRouteItemList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ListItemView mListItemView;
        RouteItem mRouteItem = mRouteItemList.get(position);
        if( convertView != null ) {
            mListItemView = (ListItemView) convertView;
            mListItemView.setItemNameText(mRouteItem.getRoNo());
            mListItemView.setItemDetailText(mRouteItem.getDirection());
        } else
            mListItemView = new ListItemView(mContext, mRouteItem);

        return mListItemView;
    }
}
