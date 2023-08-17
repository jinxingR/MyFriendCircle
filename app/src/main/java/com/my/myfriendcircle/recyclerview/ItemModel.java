package com.my.myfriendcircle.recyclerview;

public class ItemModel {
  public long id;
  public String title;
  public int imgRes;
  public String imgUrl;
  public int height;

  public ItemModel() {
  }


  public ItemModel(String title) {
    this.title = title;
  }
}