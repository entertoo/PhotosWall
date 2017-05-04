package com.example.photoswalldemo.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.os.AsyncTask;
import android.util.LruCache;
import android.view.View;
import android.widget.ImageView;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import libcore.io.DiskLruCache;

/**
 * haopi
 * 结合DiskLruCache和LruCache对图片进行三级缓存
 *
 */
public class ImageLoader
{
	/** GridView或ListView的实例 */
	private View view;
	/** DiskLruCache缓存工具类 */
	private DiskLruCacheUtil mDiskLruCacheUtil;
	/** 记录所有正在下载或等待下载的任务 */
	private Set<BitmapWorkerTask> taskCollection;
	/** 图片缓存技术的核心类，用于缓存所有下载好的图片，在程序内存达到设定值时会将最少最近使用的图片移除掉 */
	private LruCache<String, Bitmap> mLruCache;
	/** 图片硬盘缓存核心类 */
	private DiskLruCache mDiskLruCache;
	/** 记录每个子项的高度 */
	private int edgeLength = 0;
	/** 是否显示图片正方形区域不变形 */
	private boolean isCenterSquare = false;

	public ImageLoader(Context context, View view) {
		this.view = view;
		taskCollection = new HashSet<>();
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
		mDiskLruCacheUtil = new DiskLruCacheUtil(context, Config.RGB_565);
		mDiskLruCache = mDiskLruCacheUtil.doOpen();
	}

	/**
	 * 加载Bitmap对象。此方法会在LruCache中检查所有屏幕中可见的ImageView的Bitmap对象，
	 * 如果发现任何一个ImageView的Bitmap对象不在缓存中，就会开启异步线程去下载图片。
	 */
	public void loadBitmaps(ImageView imageView, String imageUrl) {
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
			if (isCenterSquare) {
				// 截取图片正方形区域
				bitmap = centerSquareScaleBitmap(bitmap, edgeLength);
			}
			imageView.setImageBitmap(bitmap);
			if (mLruCache.get(imageUrl) == null) {
				// 把缓存文件中的数据加入内存中
				mLruCache.put(imageUrl, bitmap);
			}
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
	private class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap>
	{
		// 图片的URL地址
		private String imageUrl;

		@Override
		protected Bitmap doInBackground(String... params) {
			imageUrl = params[0];
			mDiskLruCacheUtil.doEdit(imageUrl, mDiskLruCache);
			Bitmap bitmap = mDiskLruCacheUtil.doGet(imageUrl, mDiskLruCache);
			if (isCenterSquare) {
				// 截取图片正方形区域
				bitmap = centerSquareScaleBitmap(bitmap, edgeLength);
			}
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
			ImageView imageView = (ImageView) view.findViewWithTag(imageUrl);
			if (imageView != null && bitmap != null) {
				imageView.setImageBitmap(bitmap);
			}
			taskCollection.remove(this);
		}
	}

	/** 将缓存记录同步到journal文件中 */
	public void fluchCache() {
		if (mDiskLruCache != null) {
			try {
				mDiskLruCache.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/** 获取缓存大小 */
	public long getCacheSize() {
		if (mDiskLruCache != null) {
			long size = mDiskLruCache.size();
			return size;
		}
		return 0;
	}

	/** 取消所有正在下载或等待下载的任务 */
	public void cancelAllTasks() {
		if (taskCollection != null) {
			for (BitmapWorkerTask task : taskCollection) {
				task.cancel(false);
			}
		}
	}

	/** 关闭 */
	public void stopCache() {
		if (mDiskLruCache != null) {
			try {
				mDiskLruCache.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/** 设置图片的最大宽高,最好设置为真实图片的宽高两倍 */
	public void setDiskBitmapMaxWidthAndHeight(int maxWidth, int maxHeight) {
		mDiskLruCacheUtil.setMaxWidthAndHeight(maxWidth, maxHeight);
	}

	/** 设置正方形可视区域边长，如果为true */
	public void setEdgeLength(int edgeLength) {
		this.edgeLength = edgeLength;
	}

	/** 设置是否显示图片正方形区域不变形 */
	public void setCenterSquare(boolean isCenterSquare) {
		this.isCenterSquare = isCenterSquare;
	}

	/**
	 * @param bitmap
	 *            原图
	 * @param edgeLength
	 *            希望得到的正方形部分的边长
	 * @return 缩放截取正中部分后的位图。
	 */
	private Bitmap centerSquareScaleBitmap(Bitmap bitmap, int edgeLength) {
		if (null == bitmap || edgeLength <= 0) {
			return null;
		}
		Bitmap result = bitmap;
		int widthOrg = bitmap.getWidth();
		int heightOrg = bitmap.getHeight();
		if (widthOrg > edgeLength && heightOrg > edgeLength) {
			// 压缩到一个最小长度是edgeLength的bitmap,和原图的比例相等
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
