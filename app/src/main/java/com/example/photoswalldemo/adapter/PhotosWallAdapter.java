package com.example.photoswalldemo.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.example.photoswalldemo.R;
import com.example.photoswalldemo.utility.ImageLoader;

import java.util.ArrayList;
import java.util.List;

public class PhotosWallAdapter extends BaseAdapter {

    private Context context;
    private List<String> imageUrlList = new ArrayList<>();

    /**
     * 记录每个子项的高度
     */
    private int mItemHeight = 0;

    public ImageLoader mImageLoader;

    public PhotosWallAdapter(Context context, List<String> data, AbsListView absListView) {
        this.imageUrlList = data;
        this.context = context;
        mImageLoader = new ImageLoader(context, absListView);
    }

    @Override
    public int getCount() {
        if (imageUrlList != null) {
            return imageUrlList.size();
        }
        return 0;
    }

    @Override
    public Object getItem(int position) {
        return imageUrlList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        String url = imageUrlList.get(position);
        System.out.println(position + "-url: " + url);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(mItemHeight, mItemHeight);
        ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = View.inflate(context, R.layout.photo_layout, null);
            holder.iv = (ImageView) convertView.findViewById(R.id.photo);
            holder.iv.setLayoutParams(layoutParams);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.iv.setTag(url);
        // 设置默认的图片
        holder.iv.setImageResource(R.drawable.empty_photo);
        // 加载数据
        mImageLoader.loadBitmaps(holder.iv, url);
        return convertView;
    }

    private class ViewHolder {
        ImageView iv;
    }

    /**
     * 设置item子项的大小
     */
    public void setItemSize(int edgeLength) {
        mItemHeight = edgeLength;
        mImageLoader.setItemLength(edgeLength);
    }

}