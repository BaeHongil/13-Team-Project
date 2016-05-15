package kr.ac.knu.odego.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;

import kr.ac.knu.odego.common.ListItemView;
import kr.ac.knu.odego.item.FavoriteItem;

/**
 * Created by BHI on 2016-05-14.
 */
public class FavoriteListAdapter extends BaseAdapter {
    private Context mContext;
    private ArrayList<FavoriteItem> mFavoriteItemList;

    public FavoriteListAdapter(Context mContext) {
        this.mContext = mContext;

        setmFavoriteItemList( new ArrayList<FavoriteItem>() );
    }

    public void setmFavoriteItemList(ArrayList<FavoriteItem> mFavoriteItemList) {
        this.mFavoriteItemList = mFavoriteItemList;
    }

    public ArrayList<FavoriteItem> getmFavoriteItemList() {
        return mFavoriteItemList;
    }

    @Override
    public int getCount() {
        return mFavoriteItemList.size();
    }

    @Override
    public Object getItem(int position) {
        return mFavoriteItemList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ListItemView mListItemView;
        FavoriteItem mFavoriteItem = mFavoriteItemList.get(position);
        if( convertView != null ) {
            mListItemView = (ListItemView) convertView;
            mListItemView.setItemNameText(mFavoriteItem.getName());
            mListItemView.setItemDetailText(mFavoriteItem.getUrl());
        } else
            mListItemView = new ListItemView(mContext, mFavoriteItem);

        return mListItemView;
    }
}
