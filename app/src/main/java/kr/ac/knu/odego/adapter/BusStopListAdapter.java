package kr.ac.knu.odego.adapter;

import android.view.View;
import android.view.ViewGroup;

import io.realm.OrderedRealmCollection;
import io.realm.RealmRecyclerViewAdapter;
import kr.ac.knu.odego.R;
import kr.ac.knu.odego.item.BusStop;
import kr.ac.knu.odego.fragment.BusStopSearchFragment;

/**
 * Created by BHI on 2016-05-14.
 */
public class BusStopListAdapter extends RealmRecyclerViewAdapter<BusStop, BusStopListAdapter.BusStopViewHolder> {
    private final int BUSSTOP_ICON = R.drawable.bus_stop_01;
    private BusStopSearchFragment fragment;
    private int dataLimit;

    public BusStopListAdapter(BusStopSearchFragment fragment, OrderedRealmCollection<BusStop> data, boolean autoUpdate) {
        super(fragment.getActivity(), data, autoUpdate);
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
    public BusStopViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = inflater.inflate(R.layout.fragment_search_list_item, parent, false);
        return new BusStopViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(BusStopViewHolder holder, int position) {
        BusStop mBusStop = getData().get(position);
        holder.data = mBusStop;
        holder.mItemIcon.setImageResource(BUSSTOP_ICON);
        holder.mItemName.setText(mBusStop.getName());
        holder.mItemDetail.setText(mBusStop.getNo());
    }

    // 뷰홀더
    public class BusStopViewHolder extends ViewHolder<BusStop> implements View.OnClickListener {
        public BusStopViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            fragment.addBusStopHistory(data.getId());
        }
    }
}
