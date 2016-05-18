package kr.ac.knu.odego.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;

import kr.ac.knu.odego.common.ListItemView;
import kr.ac.knu.odego.item.Route;

/**
 * Created by BHI on 2016-05-14.
 */
public class RouteListAdapter extends BaseAdapter {
    private Context mContext;
    private ArrayList<Route> mRouteList;

    public RouteListAdapter(Context mContext) {
        this.mContext = mContext;

        setmRouteList( new ArrayList<Route>() );
    }

    public void setmRouteList(ArrayList<Route> mRouteList) {
        this.mRouteList = mRouteList;
    }

    public ArrayList<Route> getmRouteList() {
        return mRouteList;
    }

    @Override
    public int getCount() {
        return mRouteList.size();
    }

    @Override
    public Object getItem(int position) {
        return mRouteList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ListItemView mListItemView;
        Route mRoute = mRouteList.get(position);
        if( convertView != null ) {
            mListItemView = (ListItemView) convertView;
            mListItemView.setItemNameText(mRoute.getRoNo());
            mListItemView.setItemDetailText(mRoute.getDirection());
        } else
            mListItemView = new ListItemView(mContext, mRoute);

        return mListItemView;
    }
}
