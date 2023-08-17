package com.my.myfriendcircle;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.my.myfriendcircle.listview.MomentsHeaderListViewActivity;
import com.my.myfriendcircle.listview.QzoneHeaderListViewActivity;
import com.my.myfriendcircle.recyclerview.BrowseCircleActivity;
import com.my.myfriendcircle.recyclerview.MomentsHeaderRecyclerViewActivity;
import com.my.myfriendcircle.recyclerview.QzoneHeaderRecyclerViewActivity;


public class MainActivity extends AppCompatActivity {

  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
  }

  public void qzoneListViewClick(View view) {
    startActivity(new Intent(this, QzoneHeaderListViewActivity.class));
  }

  public void momentsListViewClick(View view) {
    startActivity(new Intent(this, MomentsHeaderListViewActivity.class));
  }

  public void qzoneRecyclerViewClick(View view) {
    startActivity(new Intent(this, QzoneHeaderRecyclerViewActivity.class));
  }

  public void momentsRecyclerViewClick(View view) {
    startActivity(new Intent(this, MomentsHeaderRecyclerViewActivity.class));
  }
  public void circleRecyclerViewClick(View view) {
    startActivity(new Intent(this, BrowseCircleActivity.class));
  }
}
