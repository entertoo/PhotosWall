package com.example.photoswalldemo;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.GridView;

import com.example.photoswalldemo.utils.DownloadImageListUtil;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity
{

	private static final String BASE_URL = "https://image.baidu.com/search/index?ct=201326592&cl=2&st=-1&lm=-1&nc=1&ie=utf-8&tn=baiduimage&ipn=r&rps=1&pv=&fm=rs8&word=";
	private String[] mImageWords = { "小清新美女", "天空之城", "千与千寻", "清新美女", "美女壁纸" };
	private static String url = BASE_URL + URLEncoder.encode("");
	private static int page = 0;

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
					mPhotoWallAdapter.setItemSize(columnWidth);
					mPhotoWallView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
				}
			}
		});
	}

	public void initData() {
		if (mPhotoWallAdapter == null) {
			mPhotoWallAdapter = new PhotosWallAdapter(this,mImageUrlList, mPhotoWallView);
		}
		mPhotoWallView.setAdapter(mPhotoWallAdapter);
		getImageListFromNet(0);
	}

	public void add(View view){
		page++;
		if(page >= mImageWords.length){
			page = page % mImageWords.length;
		}
		getImageListFromNet(page);
	}

	public void clear(View view){
		mImageUrlList.clear();
		mPhotoWallAdapter.notifyDataSetChanged();
	}

	private void getImageListFromNet(final int i) {

		new Thread(new Runnable() {
			@Override
			public void run() {mDownloadImageListUtil.setDuRegex(true);
				String regex = DownloadImageListUtil.regex[2];
				String word = mImageWords[i];
				url = BASE_URL + URLEncoder.encode(word);
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
		mPhotoWallAdapter.mImageLoader.deleteCache();
	}

}