package com.example.photoswalldemo;

import java.util.ArrayList;
import java.util.List;

import com.example.photoswalldemo.utils.ImageLoader;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;

/**
 * @author haopi
 * @创建时间 2016年8月7日 下午8:46:34
 * @描述 AbsListView的适配器，负责异步从网络上下载图片展示在照片墙上。
 * 
 * @修改提交者 $Author$
 * @提交时间 $Date$
 * @当前版本 $Rev$
 * 
 */
public class PhotoesWallAdapter extends BaseAdapter
{
	private Context context;

	private List<String> imageUrlList = new ArrayList<String>();

	/** 记录每个子项的高度 */
	private int mItemHeight = 0;

	public ImageLoader mImageLoader;

	public PhotoesWallAdapter(Context context, int textViewResourceId, List<String> data, AbsListView absListView) {
		this.imageUrlList = data;
		this.context = context;
		mImageLoader = new ImageLoader(context,absListView);
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
		ViewHolder holder;
		if (convertView == null) {
			holder = new ViewHolder();
			convertView = View.inflate(context, R.layout.photo_layout, null);
			holder.iv = (ImageView) convertView.findViewById(R.id.photo);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		if (holder.iv.getLayoutParams().height != mItemHeight) {
			holder.iv.getLayoutParams().height = mItemHeight;
		}
		// 给ImageView设置一个Tag，保证异步加载图片时不会乱序
		holder.iv.setTag(url);
		// 设置默认的图片
		holder.iv.setImageResource(R.drawable.empty_photo);
		// 加载数据
		mImageLoader.loadBitmaps(holder.iv, url);
		return convertView;
	}

	class ViewHolder
	{
		ImageView iv;
	}

	/** 设置item子项的高度 */
	public void setItemHeight(int height) {
		if (height == mItemHeight) {
			return;
		}
		mItemHeight = height;
		mImageLoader.setEdgeLength(height);
		notifyDataSetChanged();
	}

}