package kr.ac.knu.odego.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;

import io.realm.OrderedRealmCollection;
import io.realm.RealmRecyclerViewAdapter;
import kr.ac.knu.odego.R;
import kr.ac.knu.odego.common.RouteType;
import kr.ac.knu.odego.item.BeaconArrInfo;

/**
 * Created by BHI on 2016-05-14.
 */
public class BeaconArrInfoListAdapter extends RealmRecyclerViewAdapter<BeaconArrInfo, BeaconArrInfoListAdapter.BeaconArrInfoViewHolder> {

    public BeaconArrInfoListAdapter(Context mContext, OrderedRealmCollection<BeaconArrInfo> data, boolean autoUpdate) {
        super(mContext, data, autoUpdate);
    }

    @Override
    public BeaconArrInfoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = inflater.inflate(R.layout.fragment_beacon_arrinfo_list_item, parent, false);
        return new BeaconArrInfoViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(BeaconArrInfoViewHolder holder, int position) {
        BeaconArrInfo mBeaconArrInfo = getItem(position);

        holder.mBeaconArrInfo = mBeaconArrInfo;
        String routeType = mBeaconArrInfo.getMRoute().getType();
        if( routeType.equals( RouteType.MAIN.getName() ) )
            holder.itemIcon.setImageResource(R.drawable.bus_main);
        else if( routeType.equals( RouteType.BRANCH.getName() ) )
            holder.itemIcon.setImageResource(R.drawable.bus_branch);
        else if( routeType.equals( RouteType.CIRCULAR.getName() ) )
            holder.itemIcon.setImageResource(R.drawable.bus_circular);
        else
            holder.itemIcon.setImageResource(R.drawable.bus_express);

        holder.routeNo.setText( mBeaconArrInfo.getMRoute().getNo() );

        int startBusStopIndex = mBeaconArrInfo.getStartIndex();
        int destBusStopIndex = mBeaconArrInfo.getDestIndex();
        String startBusStopName = mBeaconArrInfo.getBusStops().get(startBusStopIndex).getName();
        String destBusStopName = mBeaconArrInfo.getBusStops().get(destBusStopIndex).getName();
        holder.startDest.setText(startBusStopName + " -> " +destBusStopName);

        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd a hh:mm");
        holder.date.setText( dateFormat.format( mBeaconArrInfo.getUpdated() ) );
    }

    public class BeaconArrInfoViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public BeaconArrInfo mBeaconArrInfo;

        public ImageView itemIcon;
        public TextView routeNo;
        public TextView startDest, date;

        public BeaconArrInfoViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);

            itemIcon = (ImageView) itemView.findViewById(R.id.item_icon);
            routeNo = (TextView) itemView.findViewById(R.id.route_no);
            startDest = (TextView) itemView.findViewById(R.id.start_dest);
            date = (TextView) itemView.findViewById(R.id.date);
        }

        @Override
        public void onClick(View v) {
            if (v == itemView) {
                /*if (data.getMBusStop() != null) {
                    Intent intent = new Intent(context, BusStopArrInfoActivity.class);
                    intent.putExtra("busStopId", data.getMBusStop().getId());
                    context.startActivity(intent);
                } else {
                    Intent intent = new Intent(context, BeaconActivity.class);
                    intent.putExtra("routeId", data.getMRoute().getId());
                    context.startActivity(intent);
                }*/
            }
        }
    }
}
