package kr.ac.knu.odego.adapter;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.ToggleButton;

import io.realm.Realm;
import kr.ac.knu.odego.R;
import kr.ac.knu.odego.activity.BusPosInfoActivity;
import kr.ac.knu.odego.common.RealmTransaction;
import kr.ac.knu.odego.common.RouteType;
import kr.ac.knu.odego.item.ArrInfo;
import kr.ac.knu.odego.item.Favorite;
import kr.ac.knu.odego.item.Route;
import kr.ac.knu.odego.item.RouteArrInfo;

/**
 * Created by Brick on 2016-05-30.
 */
public class BusStopArrInfoListAdapter extends BaseAdapter {
    private Context mContext;
    private Realm mRealm;
    private RouteArrInfo[] routeArrInfos;
    private LayoutInflater inflater;

    public BusStopArrInfoListAdapter(Context mContext, Realm mRealm) {
        this.mContext = mContext;
        this.mRealm = mRealm;
        inflater = LayoutInflater.from(mContext);
    }

    public void setRouteArrInfos(RouteArrInfo[] routeArrInfos) {
        this.routeArrInfos = routeArrInfos;
    }

    @Override
    public int getCount() {
        if(routeArrInfos == null)
            return 0;
        return routeArrInfos.length;
    }

    @Override
    public Object getItem(int position) {
        if(routeArrInfos == null)
            return null;
        return routeArrInfos[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View itemView = null;
        RouteArrInfoViewHolder viewHolder = null;
        RouteArrInfo routeArrInfo = routeArrInfos[position];

        Route mRoute = routeArrInfo.getMRoute();
        if(convertView==null) {
            itemView = inflater.inflate(R.layout.activity_busstoparrinfo_list_item, parent, false);
            viewHolder = new RouteArrInfoViewHolder(itemView);
            viewHolder.routeId = mRoute.getId();
            itemView.setTag( viewHolder );
        }
        else {
            itemView = convertView;
            viewHolder = (RouteArrInfoViewHolder) itemView.getTag();
            viewHolder.routeId = mRoute.getId();
        }
        if( mRealm.where(Favorite.class).equalTo("mRoute.id", mRoute.getId()).count() > 0 )
            viewHolder.favoriteBtn.setChecked(true);
        else
            viewHolder.favoriteBtn.setChecked(false);

        viewHolder.routeNo.setText( mRoute.getNo() );
        String routeType = mRoute.getType();
        int routeNoColor;
        if (RouteType.MAIN.getName().equals( routeType ))
            routeNoColor = ContextCompat.getColor(mContext, R.color.main_bus);
        else if (RouteType.BRANCH.getName().equals( routeType ))
            routeNoColor = ContextCompat.getColor(mContext, R.color.branch_bus);
        else if (RouteType.EXPRESS.getName().equals( routeType ))
            routeNoColor = ContextCompat.getColor(mContext, R.color.express_bus);
        else if (RouteType.CIRCULAR.getName().equals( routeType ))
            routeNoColor = ContextCompat.getColor(mContext, R.color.circular_bus);
        else
            routeNoColor = ContextCompat.getColor(mContext, R.color.colorPrimaryDark);
        viewHolder.routeNo.setTextColor( routeNoColor );
        viewHolder.routeDirection.setText( mRoute.getDirection() );
        viewHolder.routeDirection.setSelected(true);

        ArrInfo[] arrInfos = routeArrInfo.getArrInfos();
        if( arrInfos[0].getMessage() != null ) {
            viewHolder.remainedMin1.setText("도착정보없음");
            viewHolder.remainedMin1.setTextColor( ContextCompat.getColor(mContext, R.color.colorPrimary) );
            viewHolder.remainedBusStop1.setVisibility(View.GONE);
            viewHolder.remainedMin2.setVisibility(View.GONE);
            viewHolder.remainedBusStop2.setVisibility(View.GONE);
            return itemView;
        }
        if( arrInfos.length == 1 ) {
            viewHolder.remainedMin1.setText( arrInfos[0].getRemainMin() + "분" );
            viewHolder.remainedMin1.setTextColor( ContextCompat.getColor(mContext, R.color.colorPrimaryDark) );
            viewHolder.remainedBusStop1.setText( arrInfos[0].getRemainBusStopCount() + "개소" );

            viewHolder.remainedBusStop1.setVisibility(View.VISIBLE);
            viewHolder.remainedMin2.setVisibility(View.GONE);
            viewHolder.remainedBusStop2.setVisibility(View.GONE);
            return itemView;
        }
        viewHolder.remainedMin1.setText( arrInfos[0].getRemainMin() + "분" );
        viewHolder.remainedMin1.setTextColor( ContextCompat.getColor(mContext, R.color.colorPrimaryDark) );
        viewHolder.remainedBusStop1.setText( arrInfos[0].getRemainBusStopCount() + "개소" );
        viewHolder.remainedMin2.setText( arrInfos[1].getRemainMin() + "분" );
        viewHolder.remainedBusStop2.setText( arrInfos[1].getRemainBusStopCount() + "개소" );

        viewHolder.remainedBusStop1.setVisibility(View.VISIBLE);
        viewHolder.remainedMin2.setVisibility(View.VISIBLE);
        viewHolder.remainedBusStop2.setVisibility(View.VISIBLE);

        return itemView;
    }

    private class RouteArrInfoViewHolder implements View.OnClickListener {
        public String routeId;

        public View itemView;
        public ToggleButton favoriteBtn;
        public TextView routeNo;
        public TextView routeDirection;
        public TextView remainedMin1, remainedMin2;
        public TextView remainedBusStop1, remainedBusStop2;

        public RouteArrInfoViewHolder(View itemView) {
            this.itemView = itemView;
            itemView.setOnClickListener(this);
            favoriteBtn = (ToggleButton) itemView.findViewById(R.id.favorite_btn);
            favoriteBtn.setOnClickListener(this);
            routeNo = (TextView) itemView.findViewById(R.id.route_no);
            routeDirection = (TextView) itemView.findViewById(R.id.route_direction);
            remainedMin1 = (TextView) itemView.findViewById(R.id.remained_min1);
            remainedMin2 = (TextView) itemView.findViewById(R.id.remained_min2);
            remainedBusStop1 = (TextView) itemView.findViewById(R.id.remained_busstop1);
            remainedBusStop2 = (TextView) itemView.findViewById(R.id.remained_busstop2);
        }

        @Override
        public void onClick(View v) {
            if( v == favoriteBtn ) {
                if( !favoriteBtn.isChecked() ) { // 즐겨찾기 해제
                    RealmTransaction.deleteRouteFavorite(mRealm, routeId);
                    return;
                }
                // 즐겨찾기 설정
                RealmTransaction.createRouteFavorite(mRealm, routeId);
            } else if( v == itemView ) {
                Intent intent = new Intent( mContext , BusPosInfoActivity.class);
                intent.putExtra("routeId", routeId);
                mContext.startActivity(intent);
            }
        }
    }

}
