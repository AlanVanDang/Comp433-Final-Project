package com.example.assignment4;

import android.graphics.Bitmap;

public class ImageItem {
    private String tags;
    private String date;
    private Bitmap image;
    private Boolean selected;

    public ImageItem(String tags, String date, Bitmap image) {
        this.tags = tags;
        this.date = date;
        this.image = image;
        this.selected = false;
    }

    public String getTags() {
        return tags;
    }

    public String getDate() {
        return date;
    }

    public Bitmap getImage() {
        return image;
    }
    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}

