package kr.ac.knu.odego.adapter;

import android.content.Context;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import io.realm.Realm;
import kr.ac.knu.odego.R;
import kr.ac.knu.odego.common.RealmTransaction;
import kr.ac.knu.odego.common.RouteType;
import kr.ac.knu.odego.interfaces.BeaconSetGoalListener;
import kr.ac.knu.odego.item.BusPosInfo;
import kr.ac.knu.odego.item.BusStop;
import kr.ac.knu.odego.item.Favorite;
import lombok.Getter;
import lombok.Setter;

/**
 * Created by Brick on 2016-06-09.
 */
public class BeaconListAdapter extends BaseAdapter {
    private Context mContext;
    private Realm mRealm;
    private BusPosInfo[] busPosInfos;
    private LayoutInflater inflater;
    private int busOffImg, busOnImg, busOnNsImg, goalImg;
    private int budIdBackgroundColor;

    private BeaconSetGoalListener goalListener;


    ImageView postImageView;
    TextView postBusId;

    @Setter
    @Getter
    int selectedPositions;

    public BeaconListAdapter(Context mContext, Realm mRealm, String routeType) {
        this.mContext = mContext;
        this.mRealm = mRealm;

        inflater = LayoutInflater.from(mContext);

        if( routeType.equals( RouteType.MAIN.getName() ) ) {
            busOffImg = R.drawable.busposinfo_main_bus_off;
            busOnImg = R.drawable.busposinfo_main_bus_on;
            busOnNsImg = R.drawable.busposinfo_main_bus_on_nonstep;
            budIdBackgroundColor = ContextCompat.getColor(mContext, R.color.main_bus_dark);
            goalImg = R.drawable.beacon_main_goal;

        } else if( routeType.equals( RouteType.BRANCH.getName() ) ) {
            busOffImg = R.drawable.busposinfo_branch_bus_off;
            busOnImg = R.drawable.busposinfo_branch_bus_on;
            busOnNsImg = R.drawable.busposinfo_branch_bus_on_nonstep;
            budIdBackgroundColor = ContextCompat.getColor(mContext, R.color.branch_bus_dark);
            goalImg = R.drawable.beacon_branch_goal;

        } else if( routeType.equals( RouteType.CIRCULAR.getName() ) ) {
            busOffImg = R.drawable.busposinfo_circular_bus_off;
            busOnImg = R.drawable.busposinfo_circular_bus_on;
            busOnNsImg = R.drawable.busposinfo_circular_bus_on_nonstep;
            budIdBackgroundColor = ContextCompat.getColor(mContext, R.color.circular_bus_dark);
            goalImg = R.drawable.beacon_circular_goal;

        } else {
            busOffImg = R.drawable.busposinfo_express_bus_off;
            busOnImg = R.drawable.busposinfo_express_bus_on;
            busOnNsImg = R.drawable.busposinfo_express_bus_on_nonstep;
            budIdBackgroundColor = ContextCompat.getColor(mContext, R.color.express_bus_dark);
            goalImg = R.drawable.beacon_express_goal;

        }
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
        //presentPosition = position;

        View itemView = null;
        ViewHolder viewHolder = null;
        BusPosInfo busPosInfo = busPosInfos[position];



        BusStop mBusStop = busPosInfo.getMBusStop();
        if(convertView==null) {
            itemView = inflater.inflate(R.layout.activity_busposinfo_list_item, parent, false);
            viewHolder = new ViewHolder(itemView);
            viewHolder.busStopId = mBusStop.getId();
            itemView.setTag( viewHolder );
        } else {
            itemView = convertView;
            viewHolder = (ViewHolder) itemView.getTag();
            viewHolder.busStopId = mBusStop.getId();
        }

        if( mRealm.where(Favorite.class).equalTo("mBusStop.id", mBusStop.getId()).count() > 0 )
            viewHolder.favoriteBtn.setChecked(true);
        else
        viewHolder.busStopName.setText(mBusStop.getName());
        viewHolder.busStopNo.setText(mBusStop.getNo());
        viewHolder.busId.setVisibility(View.INVISIBLE);

        // 버스가 없을 때
        if( busPosInfo.getBusId() == null ) {
            viewHolder.busIcon.setImageResource(busOffImg);
            viewHolder.busId.setVisibility(View.INVISIBLE);
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

    private class ViewHolder implements View.OnClickListener {
        public String busStopId;

        public View itemView;
        public ToggleButton favoriteBtn;
        public TextView busStopName;
        public TextView busStopNo;
        public ImageView busIcon;
        public TextView busId;

        public ViewHolder(View itemView) {
            this.itemView = itemView;

            itemView.setOnClickListener(this);
            favoriteBtn = (ToggleButton) itemView.findViewById(R.id.favorite_btn);
            favoriteBtn.setOnClickListener(this);
            busStopName = (TextView) itemView.findViewById(R.id.busstop_name);
            busStopNo = (TextView) itemView.findViewById(R.id.busstop_no);
            busIcon = (ImageView) itemView.findViewById(R.id.bus_icon);
            busId = (TextView) itemView.findViewById(R.id.bus_id);

        }

        public void goalOn()
        {
            busIcon.setImageResource(goalImg);
            busId.setBackgroundColor(Color.WHITE);
            busId.setTextColor(budIdBackgroundColor);
            busId.setText("목적지");
            busId.setVisibility(View.VISIBLE);

            postBusId = busId;
            postImageView = busIcon;
        }
        public void goalOff()
        {
            busIcon.setImageResource(busOffImg);
            busId.setText(null);
            busId.setVisibility(View.INVISIBLE);
        }

        public void setGoal(Boolean bool)
        {
            if(bool) {
                goalOn();
                Log.d("params","goal On");
            }
            else{
                goalOff();
                Log.d("params","goal Off");
            }
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
            }
            else {

                // 처음 고름
                if ( postBusId == null ) {
                    goalListener.setGoalTextView(busStopName.getText().toString());
                    setGoal(true);
                }

                else if( postBusId == busId) {
                    postBusId.setText(" ");
                    postImageView.setImageResource( busOffImg );
                    postBusId = null;
                    setGoal(false);
                }

                else {
                    goalListener.setGoalTextView(busStopName.getText().toString());
                    postBusId.setText( null );
                    postImageView.setImageResource( busOffImg );

                    postBusId = busId;
                    postImageView = busIcon;

                    setGoal(true);
                }
            }
        }
    }

    public void setSetGoalListener(BeaconSetGoalListener goalListener)
    {
           this.goalListener = goalListener;
    }



}
