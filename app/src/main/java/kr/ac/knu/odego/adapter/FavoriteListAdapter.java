package kr.ac.knu.odego.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;

import kr.ac.knu.odego.common.ListItemView;
import kr.ac.knu.odego.item.Favorite;

/**
 * Created by BHI on 2016-05-14.
 */
public class FavoriteListAdapter extends BaseAdapter {
    private Context mContext;
    private ArrayList<Favorite> mFavoriteList;

    public FavoriteListAdapter(Context mContext) {
        this.mContext = mContext;

        setmFavoriteList( new ArrayList<Favorite>() );
    }

    public void setmFavoriteList(ArrayList<Favorite> mFavoriteList) {
        this.mFavoriteList = mFavoriteList;
    }

    public ArrayList<Favorite> getmFavoriteList() {
        return mFavoriteList;
    }

    @Override
    public int getCount() {
        return mFavoriteList.size();
    }

    @Override
    public Object getItem(int position) {
        return mFavoriteList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ListItemView mListItemView;
        Favorite mFavorite = mFavoriteList.get(position);
        if( convertView != null ) {
            mListItemView = (ListItemView) convertView;
            mListItemView.setItemNameText(mFavorite.getName());
            mListItemView.setItemDetailText(mFavorite.getUrl());
        } else
            mListItemView = new ListItemView(mContext, mFavorite);

        return mListItemView;
    }
}
