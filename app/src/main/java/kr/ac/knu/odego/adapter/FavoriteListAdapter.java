package kr.ac.knu.odego.adapter;

import android.view.View;
import android.view.ViewGroup;

import io.realm.OrderedRealmCollection;
import io.realm.RealmRecyclerViewAdapter;
import kr.ac.knu.odego.R;
import kr.ac.knu.odego.common.RouteType;
import kr.ac.knu.odego.item.BusStop;
import kr.ac.knu.odego.item.Favorite;
import kr.ac.knu.odego.item.Route;
import kr.ac.knu.odego.fragment.FavoriteFragment;

/**
 * Created by BHI on 2016-05-14.
 */
public class FavoriteListAdapter extends RealmRecyclerViewAdapter<Favorite, FavoriteListAdapter.FavoriteViewHolder> {
    private final int ROUTE_ICON = R.drawable.bus_01;
    private final int BUSSTOP_ICON = R.drawable.bus_stop_01;
    FavoriteFragment fragment;

    public FavoriteListAdapter(FavoriteFragment fragment, OrderedRealmCollection<Favorite> data, boolean autoUpdate) {
        super(fragment.getContext(), data, autoUpdate);
        this.fragment = fragment;
    }

    @Override
    public FavoriteViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = inflater.inflate(R.layout.fragment_search_list_item, parent, false);
        return new FavoriteViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(FavoriteViewHolder holder, int position) {
        Favorite mFavorite = getData().get(position);
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

    public class FavoriteViewHolder extends ViewHolder<Favorite> {

        public FavoriteViewHolder(View itemView) {
            super(itemView);
        }
    }
}
