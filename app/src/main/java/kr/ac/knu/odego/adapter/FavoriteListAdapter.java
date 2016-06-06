package kr.ac.knu.odego.adapter;

import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ToggleButton;

import io.realm.OrderedRealmCollection;
import io.realm.RealmRecyclerViewAdapter;
import kr.ac.knu.odego.R;
import kr.ac.knu.odego.activity.BusPosInfoActivity;
import kr.ac.knu.odego.activity.BusStopArrInfoActivity;
import kr.ac.knu.odego.common.RealmTransaction;
import kr.ac.knu.odego.common.RouteType;
import kr.ac.knu.odego.fragment.FavoriteFragment;
import kr.ac.knu.odego.item.BusStop;
import kr.ac.knu.odego.item.Favorite;
import kr.ac.knu.odego.item.Route;

/**
 * Created by BHI on 2016-05-14.
 */
public class FavoriteListAdapter extends RealmRecyclerViewAdapter<Favorite, FavoriteListAdapter.FavoriteViewHolder> {
    private final int ROUTE_ICON = R.drawable.bus_01;
    private final int BUSSTOP_ICON = R.drawable.bus_stop_01;
    private FavoriteFragment fragment;

    public FavoriteListAdapter(FavoriteFragment fragment, OrderedRealmCollection<Favorite> data, boolean autoUpdate) {
        super(fragment.getContext(), data, autoUpdate);
        this.fragment = fragment;
    }

    @Override
    public FavoriteViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = inflater.inflate(R.layout.fragment_favorite_list_item, parent, false);
        return new FavoriteViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(FavoriteViewHolder holder, int position) {
        Favorite mFavorite = getData().get(position);
        holder.data = mFavorite;
        holder.favoriteBtn.setChecked(true);
        if( mFavorite.getMBusStop() != null ) {
            BusStop mBusStop = mFavorite.getMBusStop();
            holder.mItemIcon.setImageResource(BUSSTOP_ICON);
            holder.mItemName.setText(mBusStop.getName());
            holder.mItemDetail.setText(mBusStop.getNo());
        } else {
            Route mRoute = mFavorite.getMRoute();

            String routeType = mRoute.getType();
            if (RouteType.MAIN.getName().equals( routeType ))
                holder.mItemIcon.setImageResource(R.drawable.bus_main);
            else if (RouteType.BRANCH.getName().equals( routeType ))
                holder.mItemIcon.setImageResource(R.drawable.bus_branch);
            else if (RouteType.EXPRESS.getName().equals( routeType ))
                holder.mItemIcon.setImageResource(R.drawable.bus_express);
            else if (RouteType.CIRCULAR.getName().equals( routeType ))
                holder.mItemIcon.setImageResource(R.drawable.bus_circular);

            holder.mItemName.setText(mRoute.getNo());
            holder.mItemDetail.setText(mRoute.getDirection());
        }
    }

    public class FavoriteViewHolder extends ViewHolder<Favorite> implements View.OnClickListener {
        public ToggleButton favoriteBtn;

        public FavoriteViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            favoriteBtn = (ToggleButton) itemView.findViewById(R.id.favorite_btn);
            favoriteBtn.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if( v == itemView ) {
                if( data.getMBusStop() != null ) {
                    Intent intent = new Intent( context , BusStopArrInfoActivity.class);
                    intent.putExtra("busStopId", data.getMBusStop().getId() );
                    context.startActivity(intent);
                } else {
                    Intent intent = new Intent( context , BusPosInfoActivity.class);
                    intent.putExtra("routeId", data.getMRoute().getId() );
                    context.startActivity(intent);
                }
            } else if( v== favoriteBtn ) {
                if( data.getMBusStop() != null )
                    RealmTransaction.deleteBusStopFavorite(fragment.getmRealm(), data.getMBusStop().getId());
                else
                    RealmTransaction.deleteRouteFavorite(fragment.getmRealm(), data.getMRoute().getId());
            }
        }
    }
}
