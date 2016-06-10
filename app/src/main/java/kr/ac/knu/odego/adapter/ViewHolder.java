package kr.ac.knu.odego.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import kr.ac.knu.odego.R;

/**
 * Created by BHI on 2016-05-27.
 */
public class ViewHolder<Data> extends RecyclerView.ViewHolder {
    TextView mItemName, mItemDetail;
    ImageView mItemIcon;
    Data data;
    public ViewHolder(View itemView) {
        super(itemView);
        mItemName = (TextView) itemView.findViewById(R.id.item_name);
        mItemDetail = (TextView) itemView.findViewById(R.id.item_detail);
        mItemIcon = (ImageView) itemView.findViewById(R.id.item_icon);
    }
}
