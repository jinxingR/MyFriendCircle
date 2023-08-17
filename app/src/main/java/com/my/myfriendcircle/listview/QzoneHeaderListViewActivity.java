package com.my.myfriendcircle.listview;

import static android.view.View.OVER_SCROLL_NEVER;

import android.os.Bundle;
import android.os.Handler;
import android.widget.ArrayAdapter;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class QzoneHeaderListViewActivity extends AppCompatActivity {

  private HeaderPullRefreshListView lv;

  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    lv = new HeaderPullRefreshListView(this);
    setContentView(lv);

    lv.setOverScrollMode(OVER_SCROLL_NEVER);

    ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new String[] {
        "Qzone", "Qzone", "Qzone", "Qzone", "Qzone", "Qzone", "Qzone", "Qzone", "Qzone", "Qzone", "Qzone", "Qzone", "Qzone", "Qzone", "Qzone",
        "Qzone", "Qzone", "Qzone", "Qzone", "Qzone", "Qzone", ".........."
    });

    // 头部缩放  QQ空间
    QzoneRefreshHeader header = new QzoneRefreshHeader(this);
    lv.addHeaderView(header);
    lv.setHeaderDividersEnabled(false);
    lv.setRefreshHeader(header);
    lv.setAdapter(adapter);
    lv.setRefreshListener(new HeaderPullRefreshListView.OnRefreshListener() {
      @Override public void onRefresh() {
        new Handler().postDelayed(new Runnable() {
          @Override public void run() {
            lv.refreshComplete();
          }
        }, 1000);
      }
    });
    lv.refresh();
  }
}
