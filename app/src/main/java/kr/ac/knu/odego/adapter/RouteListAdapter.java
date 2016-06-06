package kr.ac.knu.odego.adapter;

import android.view.View;
import android.view.ViewGroup;

import io.realm.OrderedRealmCollection;
import io.realm.RealmRecyclerViewAdapter;
import kr.ac.knu.odego.R;
import kr.ac.knu.odego.common.RouteType;
import kr.ac.knu.odego.fragment.RouteSearchFragment;
import kr.ac.knu.odego.item.Route;

/**
 * Created by BHI on 2016-05-14.
 */
public class RouteListAdapter extends RealmRecyclerViewAdapter<Route, RouteListAdapter.RouteViewHolder> {
    private final int ROUTE_ICON = R.drawable.bus_01;
    private RouteSearchFragment fragment;
    private int dataLimit;

    public RouteListAdapter(RouteSearchFragment fragment, OrderedRealmCollection<Route> data, boolean autoUpdate) {
        super(fragment.getContext(), data, autoUpdate);
        this.fragment = fragment;
        resetDataLimit();
    }

    // 결과개수 제한
    public void setDataLimit(int dataLimit) {
        this.dataLimit = dataLimit;
    }

    public void resetDataLimit() {
        this.dataLimit = -1;
    }

    @Override
    public int getItemCount() {
        int itemCount = super.getItemCount();
        if( dataLimit != -1 && dataLimit < itemCount )
            return dataLimit;
        return itemCount;
    }

    @Override
    public RouteViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = inflater.inflate(R.layout.fragment_search_list_item, parent, false);
        return new RouteViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(RouteViewHolder holder, int position) {
        Route mRoute = getData().get(position);
        holder.data = mRoute;

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

    public class RouteViewHolder extends ViewHolder<Route> implements View.OnClickListener {
        public RouteViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            fragment.addRouteHistory(data.getId());
        }
    }
}
