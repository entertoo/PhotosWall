package com.example.photoswalldemo;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.view.ViewTreeObserver;
import android.widget.GridView;

/**
 * 照片墙主活动，使用GridView展示照片墙。
 */
public class MainActivity extends Activity
{

	/** 用于展示照片墙的GridView */
	private GridView mPhotoWallView;

	/** GridView的适配器 */
	private PhotoWallAdapter mPhotoWallAdapter;

	private int mImageThumbSize;
	private int mImageThumbSpacing;

	private List<String> mImageUrlList = new ArrayList<String>();
	private DownloadImageListUtil mDownloadImageListUtil;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mDownloadImageListUtil = new DownloadImageListUtil();
		
		initView();
		initEvent();
		initData();
	}

	public void initView() {
		setContentView(R.layout.activity_main);
		mPhotoWallView = (GridView) findViewById(R.id.photo_wall);
	}

	public void initEvent() {
		mImageThumbSize = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size);
		mImageThumbSpacing = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_spacing);
		
		mPhotoWallView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
	
			@Override
			public void onGlobalLayout() {
				// 计算列数
				final int numColumns = (int) Math.floor(mPhotoWallView.getWidth() / (mImageThumbSize + mImageThumbSpacing));
				if (numColumns > 0) {
					int columnWidth = (mPhotoWallView.getWidth() / numColumns) - mImageThumbSpacing;
					mPhotoWallAdapter.setItemHeight(columnWidth);
					mPhotoWallView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
				}
			}
		});
	}

	public void initData() {
		if(mPhotoWallAdapter == null){
			mPhotoWallAdapter = new PhotoWallAdapter(this, 0, mImageUrlList, mPhotoWallView);
		}
		mPhotoWallView.setAdapter(mPhotoWallAdapter);
		getImageListFromNet();
	}

	public void getImageListFromNet() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				String url = "http://image.baidu.com/search/index?ct=201326592&cl=2&st=-1&lm=-1&nc=1&ie=utf-8&tn=baiduimage&ipn=r&rps=1&pv=&fm=rs5&word=%E7%99%BD%E7%9A%99%E4%B8%B0%E6%BB%A1%E8%AF%B1%E4%BA%BA%E5%A6%87%E5%A5%B3&oriquery=%E9%A5%B1%E6%BB%A1%E4%B8%B0%E6%BB%A1%E4%B8%B0%E8%85%B4%E5%A6%87%E5%A5%B3&ofr=%E9%A5%B1%E6%BB%A1%E4%B8%B0%E6%BB%A1%E4%B8%B0%E8%85%B4%E5%A6%87%E5%A5%B3";
				mDownloadImageListUtil.setBaiduRegex(true);
				String regex = DownloadImageListUtil.regexs[2];
				final ArrayList<String> imageUrlList = mDownloadImageListUtil.ParseHtmlToImgList(url, regex);
				System.out.println("mImageUrlList: " + mImageUrlList);
				
				runOnUiThread(new Runnable() {
					public void run() {
						mImageUrlList.addAll(imageUrlList);
						mPhotoWallAdapter.notifyDataSetChanged();
					}
				});
			}
		}).start();
	}

	@Override
	protected void onPause() {
		super.onPause();
		mPhotoWallAdapter.fluchCache();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		// 退出程序时结束所有的下载任务
		mPhotoWallAdapter.cancelAllTasks();
	}

}