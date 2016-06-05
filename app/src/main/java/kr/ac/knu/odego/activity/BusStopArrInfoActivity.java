package kr.ac.knu.odego.activity;

import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;

import com.github.ksoichiro.android.observablescrollview.ObservableListView;
import com.github.ksoichiro.android.observablescrollview.ObservableScrollViewCallbacks;
import com.github.ksoichiro.android.observablescrollview.ScrollState;
import com.github.ksoichiro.android.observablescrollview.ScrollUtils;

import kr.ac.knu.odego.R;

public class BusStopArrInfoActivity extends AppCompatActivity implements ObservableScrollViewCallbacks {

    private View mHeaderView;
    private Toolbar mToolbarView;
    private View mListBackgroundView;
    private ObservableListView mListView;
    private int mParallaxImageHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_busstoparrinfo) ;

        if (Build.VERSION.SDK_INT >= 21)  // 상태바 색상 변경
            getWindow().setStatusBarColor(getResources().getColor(R.color.main_bus));

        mHeaderView = findViewById(R.id.header);
        mHeaderView.setBackgroundColor(getResources().getColor(R.color.main_bus));
        mToolbarView = (Toolbar)findViewById(R.id.toolbar);
        // 툴바 타이틀 설정
        mToolbarView.setTitle("타이틀");
        mToolbarView.setSubtitle("서브타이틀");
        // 툴바 색상 설정
        mToolbarView.setBackgroundColor(ScrollUtils.getColorWithAlpha(0, getResources().getColor(R.color.main_bus)));
        setSupportActionBar(mToolbarView);

        mListView = (ObservableListView) findViewById(R.id.list);
        mListView.setScrollViewCallbacks(this);
        mParallaxImageHeight = getResources().getDimensionPixelSize(R.dimen.flexible_space_layout_height);
        setPaddingView(mListView, mParallaxImageHeight);

        // mListBackgroundView makes ListView's background except header view.
        mListBackgroundView = findViewById(R.id.list_background);
    }

    public void setPaddingView(ListView listView, int maxHeight) {
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

    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        MenuItem btMenuItem = menu.findItem(R.id.action_bluetooth);

        btMenuItem.setIcon(R.drawable.bt_on);

        return true;
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        onScrollChanged(mListView.getCurrentScrollY(), false, false);
    }

    @Override
    public void onScrollChanged(int scrollY, boolean firstScroll, boolean dragging) {
        int baseColor = getResources().getColor(R.color.main_bus);
        int textColor = getResources().getColor(R.color.colorPrimaryDark);
        float alpha = Math.min(1, (float) scrollY / mParallaxImageHeight);
        mToolbarView.setBackgroundColor(ScrollUtils.getColorWithAlpha(alpha, baseColor));
        mToolbarView.setTitleTextColor(ScrollUtils.getColorWithAlpha(alpha, textColor));
        mToolbarView.setSubtitleTextColor(ScrollUtils.getColorWithAlpha(alpha, textColor));

        // Translate list background
        mHeaderView.setTranslationY(-scrollY / 2);
        mListBackgroundView.setTranslationY(Math.max(0, -scrollY + mParallaxImageHeight));
    }

    @Override
    public void onDownMotionEvent() {
    }

    @Override
    public void onUpOrCancelMotionEvent(ScrollState scrollState) {
    }
}