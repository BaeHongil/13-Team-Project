package kr.ac.knu.odego.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import kr.ac.knu.odego.R;
import kr.ac.knu.odego.item.ArrInfo;
import kr.ac.knu.odego.item.RouteArrInfo;

/**
 * Created by Brick on 2016-05-30.
 */
public class BusStopArrInfoListAdapter extends BaseAdapter {
    private Context mContext;
    private RouteArrInfo[] routeArrInfos;
    private LayoutInflater inflater;

    public BusStopArrInfoListAdapter(Context mContext) {
        this.mContext = mContext;
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

        if(convertView==null) {
            itemView = inflater.inflate(R.layout.activity_busstoparrinfo_list_item, parent, false);
            viewHolder = new RouteArrInfoViewHolder(itemView);
            itemView.setTag( viewHolder );
        }
        else {
            itemView = convertView;
            viewHolder = (RouteArrInfoViewHolder) itemView.getTag();
        }
        viewHolder.favoriteBtn.setImageResource(R.drawable.favorite_off);
        viewHolder.routeNo.setText( routeArrInfo.getMRoute().getNo() );
        viewHolder.routeDirection.setText( routeArrInfo.getMRoute().getDirection() );
        viewHolder.routeDirection.setSelected(true);
        ArrInfo[] arrInfos = routeArrInfo.getArrInfos();

        if( arrInfos[0].getMessage() != null ) {
            viewHolder.remainedMin1.setText("도착정보없음");
            viewHolder.remainedBusStop1.setVisibility(View.GONE);
            viewHolder.remainedMin2.setVisibility(View.GONE);
            viewHolder.remainedBusStop2.setVisibility(View.GONE);
            return itemView;
        }
        if( arrInfos.length == 1 ) {
            viewHolder.remainedMin1.setText( String.valueOf(arrInfos[0].getRemainMin()) );
            viewHolder.remainedBusStop1.setText( String.valueOf(arrInfos[0].getRemainBusStopCount()) );

            viewHolder.remainedBusStop1.setVisibility(View.VISIBLE);
            viewHolder.remainedMin2.setVisibility(View.GONE);
            viewHolder.remainedBusStop2.setVisibility(View.GONE);
            return itemView;
        }
        viewHolder.remainedMin1.setText( String.valueOf(arrInfos[0].getRemainMin()) );
        viewHolder.remainedBusStop1.setText( String.valueOf(arrInfos[0].getRemainBusStopCount()) );
        viewHolder.remainedMin2.setText( String.valueOf(arrInfos[1].getRemainMin()) );
        viewHolder.remainedBusStop2.setText( String.valueOf(arrInfos[1].getRemainBusStopCount()) );

        viewHolder.remainedBusStop1.setVisibility(View.VISIBLE);
        viewHolder.remainedMin2.setVisibility(View.VISIBLE);
        viewHolder.remainedBusStop2.setVisibility(View.VISIBLE);

        return itemView;
    }

    private class RouteArrInfoViewHolder {
        public View itemView;
        public ImageButton favoriteBtn;
        public TextView routeNo;
        public TextView routeDirection;
        public TextView remainedMin1, remainedMin2;
        public TextView remainedBusStop1, remainedBusStop2;

        public RouteArrInfoViewHolder(View itemView) {
            this.itemView = itemView;
            favoriteBtn = (ImageButton) itemView.findViewById(R.id.favorite_btn);
            routeNo = (TextView) itemView.findViewById(R.id.route_no);
            routeDirection = (TextView) itemView.findViewById(R.id.route_direction);
            remainedMin1 = (TextView) itemView.findViewById(R.id.remained_min1);
            remainedMin2 = (TextView) itemView.findViewById(R.id.remained_min2);
            remainedBusStop1 = (TextView) itemView.findViewById(R.id.remained_busstop1);
            remainedBusStop2 = (TextView) itemView.findViewById(R.id.remained_busstop2);
        }
    }
}
