package com.example.photoswalldemo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.LruCache;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import libcore.io.DiskLruCache;

/**
 * AbsListView的适配器，负责异步从网络上下载图片展示在照片墙上。
 * 
 * @author guolin
 */
public class PhotoWallAdapter extends BaseAdapter
{

	/** 记录所有正在下载或等待下载的任务 */
	private Set<BitmapWorkerTask> taskCollection;

	/** 图片缓存技术的核心类，用于缓存所有下载好的图片，在程序内存达到设定值时会将最少最近使用的图片移除掉 */
	private LruCache<String, Bitmap> mLruCache;

	/** 图片硬盘缓存核心类 */
	private DiskLruCache mDiskLruCache;

	/** GridView或ListView的实例 */
	private AbsListView mPhotoWallView;
	private Context context;

	private List<String> imageUrlList = new ArrayList<String>();

	/** 记录每个子项的高度 */
	private int mItemHeight = 0;

	private DiskLruCacheUtil mDiskLruCacheUtil;

	public PhotoWallAdapter(Context context, int textViewResourceId, List<String> objects, AbsListView photoWall) {
		mPhotoWallView = photoWall;
		imageUrlList = objects;
		this.context = context;
		taskCollection = new HashSet<BitmapWorkerTask>();

		// 获取应用程序最大可用内存
		int maxMemory = (int) Runtime.getRuntime().maxMemory();
		int cacheSize = maxMemory / 8;
		// 设置图片缓存大小为程序最大可用内存的1/8
		mLruCache = new LruCache<String, Bitmap>(cacheSize) {
			@Override
			protected int sizeOf(String key, Bitmap bitmap) {
				return bitmap.getByteCount();
			}
		};
		mDiskLruCacheUtil = new DiskLruCacheUtil(context);
		mDiskLruCache = mDiskLruCacheUtil.doOpen();
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
		System.out.println("url: " + url);
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
		loadBitmaps(holder.iv, url);
		return convertView;
	}

	class ViewHolder
	{
		ImageView iv;
	}

	/**
	 * 加载Bitmap对象。此方法会在LruCache中检查所有屏幕中可见的ImageView的Bitmap对象，
	 * 如果发现任何一个ImageView的Bitmap对象不在缓存中，就会开启异步线程去下载图片。
	 */
	private void loadBitmaps(ImageView imageView, String imageUrl) {
		// 从内存中获取数据
		Bitmap bitmap = mLruCache.get(imageUrl);
		// 如果内存中有数据
		if (imageView != null && bitmap != null) {
			imageView.setImageBitmap(bitmap);
			// 从内存中加载数据
			System.out.println("********从内存中加载数据********");
			return;
		}
		// 从缓存文件中获取数据DiskUrlCache
		bitmap = mDiskLruCacheUtil.doGet(imageUrl, mDiskLruCache);
		if (imageView != null && bitmap != null) {
			bitmap = centerSquareScaleBitmap(bitmap, mItemHeight);
			imageView.setImageBitmap(bitmap);
			// 从缓存中加载数据
			System.out.println("********从缓存中加载数据********");
			return;
		}
		// 从网络获取数据
		getBitmapFromNet(imageUrl);
	}

	/** 从网络获取数据 */
	public void getBitmapFromNet(String imageUrl) {
		BitmapWorkerTask task = new BitmapWorkerTask();
		taskCollection.add(task);
		task.execute(imageUrl);
	}

	/** 异步下载图片的任务 */
	class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap>
	{
		/** 图片的URL地址 */
		private String imageUrl;

		@Override
		protected Bitmap doInBackground(String... params) {
			imageUrl = params[0];
			mDiskLruCacheUtil.doEdit(imageUrl, mDiskLruCache);
			Bitmap bitmap = mDiskLruCacheUtil.doGet(imageUrl, mDiskLruCache);
			bitmap = centerSquareScaleBitmap(bitmap, mItemHeight);
			System.out.println("********从网络获取数据********");
			if (bitmap != null && imageUrl != null && mLruCache.get(imageUrl) == null) {
				// 把网络下载的数据加入内存中
				mLruCache.put(imageUrl, bitmap);
			}
			return bitmap;
		}

		@Override
		protected void onPostExecute(Bitmap bitmap) {
			super.onPostExecute(bitmap);
			// 根据Tag找到相应的ImageView控件，将下载好的图片显示出来。
			ImageView imageView = (ImageView) mPhotoWallView.findViewWithTag(imageUrl);
			if (imageView != null && bitmap != null) {
				imageView.setImageBitmap(bitmap);
			}
			taskCollection.remove(this);
		}
	}

	/**
	 * 取消所有正在下载或等待下载的任务。
	 */
	public void cancelAllTasks() {
		if (taskCollection != null) {
			for (BitmapWorkerTask task : taskCollection) {
				task.cancel(false);
			}
		}
	}

	/**
	 * 设置item子项的高度。
	 */
	public void setItemHeight(int height) {
		if (height == mItemHeight) {
			return;
		}
		mItemHeight = height;
		notifyDataSetChanged();
	}

	/**
	 * 将缓存记录同步到journal文件中。
	 */
	public void fluchCache() {
		if (mDiskLruCache != null) {
			try {
				mDiskLruCache.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * @param bitmap
	 *            原图
	 * @param edgeLength
	 *            希望得到的正方形部分的边长
	 * @return 缩放截取正中部分后的位图。
	 */
	public Bitmap centerSquareScaleBitmap(Bitmap bitmap, int edgeLength) {
		if (null == bitmap || edgeLength <= 0) {
			return null;
		}

		Bitmap result = bitmap;
		int widthOrg = bitmap.getWidth();
		int heightOrg = bitmap.getHeight();

		if (widthOrg > edgeLength && heightOrg > edgeLength) {
			// 压缩到一个最小长度是edgeLength的bitmap
			int longerEdge = (int) (edgeLength * Math.max(widthOrg, heightOrg) / Math.min(widthOrg, heightOrg));
			int scaledWidth = widthOrg > heightOrg ? longerEdge : edgeLength;
			int scaledHeight = widthOrg > heightOrg ? edgeLength : longerEdge;
			Bitmap scaledBitmap;

			try {
				scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true);
			} catch (Exception e) {
				return null;
			}

			// 从图中截取正中间的正方形部分。
			int xTopLeft = (scaledWidth - edgeLength) / 2;
			int yTopLeft = (scaledHeight - edgeLength) / 2;

			try {
				result = Bitmap.createBitmap(scaledBitmap, xTopLeft, yTopLeft, edgeLength, edgeLength);
				scaledBitmap.recycle();
			} catch (Exception e) {
				return null;
			}
		}

		return result;
	}
}