package kr.ac.knu.odego.activity;

import android.content.res.TypedArray;
import android.support.v7.app.AppCompatActivity;
import android.util.TypedValue;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;

import com.github.ksoichiro.android.observablescrollview.ObservableScrollViewCallbacks;

import kr.ac.knu.odego.R;

/**
 * Created by BHI on 2016-06-06.
 */
public abstract class ObsvBaseActivity extends AppCompatActivity implements ObservableScrollViewCallbacks {
    private int actionBarSize;

    protected void setPaddingView(ListView listView, int maxHeight) {
        // Set padding view for ListView. This is the flexible space.
        View paddingView = new View(this);
        AbsListView.LayoutParams lp = new AbsListView.LayoutParams(
                AbsListView.LayoutParams.MATCH_PARENT,
                maxHeight);
        paddingView.setLayoutParams(lp);
        // This is required to disable header's list selector effect
        paddingView.setClickable(true);

        listView.addHeaderView(paddingView);
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
}
