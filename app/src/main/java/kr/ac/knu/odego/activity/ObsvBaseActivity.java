package kr.ac.knu.odego.activity;

import android.content.res.TypedArray;
import android.support.v7.app.AppCompatActivity;
import android.util.TypedValue;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.github.ksoichiro.android.observablescrollview.ObservableScrollViewCallbacks;
import com.github.ksoichiro.android.observablescrollview.ScrollState;

import kr.ac.knu.odego.R;

/**
 * Created by BHI on 2016-06-06.
 */
public abstract class ObsvBaseActivity extends AppCompatActivity implements ObservableScrollViewCallbacks {
    private int actionBarSize;
    private View footer;
    private LinearLayout progressLayout;

    protected void setPaddingView(final ListView listView, int maxHeight) {
        // Set padding view for ListView. This is the flexible space.
        View paddingView = new View(this);
        AbsListView.LayoutParams lp = new AbsListView.LayoutParams(
                AbsListView.LayoutParams.MATCH_PARENT,
                maxHeight);
        paddingView.setLayoutParams(lp);
        // This is required to disable header's list selector effect
        paddingView.setClickable(true);

        listView.addHeaderView(paddingView);
        footer = getLayoutInflater().inflate(R.layout.list_up_btn_item, listView, false);
        ImageButton imgBtn = (ImageButton) footer.findViewById(R.id.list_up_btn);
        imgBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listView.setSelection(0);
            }
        });
        listView.addFooterView(footer);
    }

    protected void setUpProgressLayout(int refreshingMsgResId, int headerHeight) {
        progressLayout = (LinearLayout) findViewById(R.id.progress_layout);
        progressLayout.setPadding(0, headerHeight, 0, 0);
        TextView progressText = (TextView) findViewById(R.id.progress_text);
        progressText.setText(refreshingMsgResId);
    }

    protected void setUpProgressLayout(String refreshingMsg, int headerHeight) {
        progressLayout = (LinearLayout) findViewById(R.id.progress_layout);
        progressLayout.setPadding(0, headerHeight, 0, 0);
        TextView progressText = (TextView) findViewById(R.id.progress_text);
        progressText.setText(refreshingMsg);
    }

    public LinearLayout getProgressLayout() {
        return progressLayout;
    }

    protected int getActionBarSize() {
        if( actionBarSize == 0 ) {
            TypedValue typedValue = new TypedValue();
            int[] textSizeAttr = new int[]{R.attr.actionBarSize};
            int indexOfAttrTextSize = 0;
            TypedArray a = obtainStyledAttributes(typedValue.data, textSizeAttr);
            actionBarSize = a.getDimensionPixelSize(indexOfAttrTextSize, -1);
            a.recycle();
        }

        return actionBarSize;
    }

    public View getFooter() {
        return footer;
    }

    @Override
    public void onDownMotionEvent() {
    }

    @Override
    public void onUpOrCancelMotionEvent(ScrollState scrollState) {
    }
}
