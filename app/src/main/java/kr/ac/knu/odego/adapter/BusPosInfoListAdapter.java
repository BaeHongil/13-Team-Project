package kr.ac.knu.odego.adapter;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import javax.annotation.Resource;

import io.realm.Realm;
import kr.ac.knu.odego.R;
import kr.ac.knu.odego.activity.BusStopArrInfoActivity;
import kr.ac.knu.odego.common.RealmTransaction;
import kr.ac.knu.odego.common.RouteType;
import kr.ac.knu.odego.item.BusPosInfo;
import kr.ac.knu.odego.item.BusStop;
import kr.ac.knu.odego.item.Favorite;

/**
 * Created by Brick on 2016-05-30.
 */
public class BusPosInfoListAdapter extends BaseAdapter {
    private Context mContext;
    private Realm mRealm;
    private BusPosInfo[] busPosInfos;
    private LayoutInflater inflater;
    private int busOffImg, busOnImg, busOnNsImg, busOffFinalImg, busOffFirstImg;
    private int budIdBackgroundColor;

    private String routeType;

    private final String MAIN = "main";
    private final String BRANCH = "branch";
    private final String CIRCULAR = "circular";
    private final String EXPRESS = "express";

    public BusPosInfoListAdapter(Context mContext, Realm mRealm, String routeType) {
        this.mContext = mContext;
        this.mRealm = mRealm;

        Resources res = mContext.getResources();
        String packageName = mContext.getPackageName();
        String type;
        this.routeType = routeType;
        inflater = LayoutInflater.from(mContext);
        if( routeType.equals( RouteType.MAIN.getName() ) )
            type = MAIN;
        else if( routeType.equals( RouteType.BRANCH.getName() ) )
            type = BRANCH;
        else if( routeType.equals( RouteType.CIRCULAR.getName() ) )
            type = CIRCULAR;
        else
            type = EXPRESS;
        busOffImg = res.getIdentifier("busposinfo_"+type+"_bus_off","drawable", packageName );
        busOnImg = res.getIdentifier("busposinfo_"+type+"_bus_on","drawable", packageName );
        busOnNsImg = res.getIdentifier("busposinfo_"+type+"_bus_on_nonstep","drawable", packageName );
        budIdBackgroundColor = ContextCompat.getColor(mContext, R.color.main_bus_dark);
        budIdBackgroundColor = ContextCompat.getColor(mContext,
                res.getIdentifier(""+type+"_bus_dark","color", packageName ));
        busOffFirstImg = res.getIdentifier("busposinfo_"+type+"_bus_off_first","drawable", packageName );
        busOffFinalImg = res.getIdentifier("busposinfo_"+type+"_bus_off_final","drawable", packageName );

    }

    public void setBusPosInfos(BusPosInfo[] busPosInfos) {
        this.busPosInfos = busPosInfos;
    }

    @Override
    public int getCount() {
        if(busPosInfos == null)
            return 0;
        return busPosInfos.length;
    }

    @Override
    public Object getItem(int position) {
        if(busPosInfos == null)
            return null;
        return busPosInfos[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View itemView = null;
        BusPosInfoViewHolder viewHolder = null;
        BusPosInfo busPosInfo = busPosInfos[position];

        BusStop mBusStop = busPosInfo.getMBusStop();
        if(convertView==null) {
            itemView = inflater.inflate(R.layout.activity_busposinfo_list_item, parent, false);
            viewHolder = new BusPosInfoViewHolder(itemView);
            viewHolder.busStopId = mBusStop.getId();
            itemView.setTag( viewHolder );
        } else {
            itemView = convertView;
            viewHolder = (BusPosInfoViewHolder) itemView.getTag();
            viewHolder.busStopId = mBusStop.getId();
        }



        if( mRealm.where(Favorite.class).equalTo("mBusStop.id", mBusStop.getId()).count() > 0 )
            viewHolder.favoriteBtn.setChecked(true);
        else
            viewHolder.favoriteBtn.setChecked(false);
        viewHolder.busStopName.setText(mBusStop.getName());
        viewHolder.busStopNo.setText(mBusStop.getNo());




        // 버스가 없을 때
        if( busPosInfo.getBusId() == null ) {
            viewHolder.busIcon.setImageResource(busOffImg);
            viewHolder.busId.setVisibility(View.INVISIBLE);

            //처음일 때
            if(busPosInfos[0].getMBusStop().getName().equals(
                    viewHolder.busStopName.getText().toString()
            ))
                viewHolder.busIcon.setImageResource( busOffFirstImg );

            //마지막일 때
            if(busPosInfos[busPosInfos.length -1].getMBusStop().getName().equals(
                    viewHolder.busStopName.getText().toString()
            ))
                viewHolder.busIcon.setImageResource( busOffFinalImg );

            return itemView;
        }

        // 버스가 있을 때
        if( busPosInfo.isNonStepBus() )
            viewHolder.busIcon.setImageResource(busOnNsImg);
        else
            viewHolder.busIcon.setImageResource(busOnImg);
        String busId = busPosInfo.getBusId();
        String busIdNum = busId.substring(busId.length() - 4, busId.length());
        viewHolder.busId.setText(busIdNum);
        viewHolder.busId.setBackgroundColor(budIdBackgroundColor);
        viewHolder.busId.setVisibility(View.VISIBLE);

        return itemView;
    }

    private class BusPosInfoViewHolder implements View.OnClickListener {
        public String busStopId;

        public View itemView;
        public ToggleButton favoriteBtn;
        public TextView busStopName;
        public TextView busStopNo;
        public ImageView busIcon;
        public TextView busId;

        public BusPosInfoViewHolder(View itemView) {
            this.itemView = itemView;
            itemView.setOnClickListener(this);
            favoriteBtn = (ToggleButton) itemView.findViewById(R.id.favorite_btn);
            favoriteBtn.setOnClickListener(this);
            busStopName = (TextView) itemView.findViewById(R.id.busstop_name);
            busStopNo = (TextView) itemView.findViewById(R.id.busstop_no);
            busIcon = (ImageView) itemView.findViewById(R.id.bus_icon);
            busId = (TextView) itemView.findViewById(R.id.bus_id);
        }

        @Override
        public void onClick(View v) {
            if( v == favoriteBtn ) {
                if( !favoriteBtn.isChecked() ) { // 즐겨찾기 해제
                    RealmTransaction.deleteBusStopFavorite(mRealm, busStopId);
                    return;
                }
                // 즐겨찾기 설정
                RealmTransaction.createBusStopFavorite(mRealm, busStopId);
            } else if( v == itemView ) {
                Intent intent = new Intent( mContext , BusStopArrInfoActivity.class);
                intent.putExtra("busStopId", busStopId);
                mContext.startActivity(intent);
            }
        }
    }

}

