package com.example.photoswalldemo;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.GridView;

import com.example.photoswalldemo.utils.DownloadImageListUtil;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity
{
	private static final String url = "https://image.baidu.com/search/index?ct=201326592&cl=2&st=-1&lm=-1&nc=1&ie=utf-8&tn=baiduimage&ipn=r&rps=1&pv=&fm=rs13&word=%E5%A4%B4%E5%83%8F%20%E5%A5%B3%E7%94%9F&oriquery=%E8%90%8C%E5%A5%B3&ofr=%E8%90%8C%E5%A5%B3&hs=2";

	/** 用于展示照片墙的GridView */
	private GridView mPhotoWallView;

	/** GridView的适配器 */
	private PhotosWallAdapter mPhotoWallAdapter;

	private int mImageThumbSize;
	private int mImageThumbSpacing;

	private List<String> mImageUrlList = new ArrayList<>();
	private DownloadImageListUtil mDownloadImageListUtil;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
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
		// 监听获取图片的宽高
		mPhotoWallView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

			@Override
			public void onGlobalLayout() {
				// 计算列数
				final int numColumns = (int) Math.floor(mPhotoWallView.getWidth() / (mImageThumbSize + mImageThumbSpacing));
				if (numColumns > 0) {
					int columnWidth = (mPhotoWallView.getWidth() / numColumns) - mImageThumbSpacing;
					System.out.println("columnWidth: " + columnWidth);
					mPhotoWallAdapter.setItemSize(columnWidth);
					mPhotoWallView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
				}
			}
		});
	}
	public int dp2px(float dp) {
		float scale = this.getResources().getDisplayMetrics().density;
		return (int) (dp * scale + 0.5f);
	}

	public void initData() {
		if (mPhotoWallAdapter == null) {
			mPhotoWallAdapter = new PhotosWallAdapter(this,mImageUrlList, mPhotoWallView);
		}
		mPhotoWallView.setAdapter(mPhotoWallAdapter);
		getImageListFromNet();
	}

	public void add(View view){
		getImageListFromNet();
	}

	private void getImageListFromNet() {
		new Thread(new Runnable() {
			@Override
			public void run() {mDownloadImageListUtil.setDuRegex(true);
				String regex = DownloadImageListUtil.regex[2];
				final ArrayList<String> imageUrlList = mDownloadImageListUtil.ParseHtmlToImgList(url, regex);
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
		mPhotoWallAdapter.mImageLoader.flushCache();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		// 退出程序时结束所有的下载任务
		mPhotoWallAdapter.mImageLoader.cancelAllTasks();
		mPhotoWallAdapter.mImageLoader.stopCache();
	}

}