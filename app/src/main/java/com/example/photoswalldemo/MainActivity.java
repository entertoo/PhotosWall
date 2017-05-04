package com.example.photoswalldemo;

import java.util.ArrayList;
import java.util.List;

import com.example.photoswalldemo.utils.DownloadImageListUtil;

import android.app.Activity;
import android.os.Bundle;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.GridView;

/**
 * @author haopi
 * @创建时间 2016年8月7日 下午8:46:34
 * @描述 照片墙主活动，使用GridView展示照片墙。
 * 
 * @修改提交者 $Author$
 * @提交时间 $Date$
 * @当前版本 $Rev$
 * 
 */
public class MainActivity extends Activity
{

	/** 用于展示照片墙的GridView */
	private GridView mPhotoWallView;

	/** GridView的适配器 */
	private PhotoesWallAdapter mPhotoWallAdapter;

	private int mImageThumbSize;
	private int mImageThumbSpacing;

	private List<String> mImageUrlList = new ArrayList<String>();
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
				final int numColumns = (int) Math
						.floor(mPhotoWallView.getWidth() / (mImageThumbSize + mImageThumbSpacing));
				if (numColumns > 0) {
					int columnWidth = (mPhotoWallView.getWidth() / numColumns) - mImageThumbSpacing;
					mPhotoWallAdapter.setItemSize(columnWidth, columnWidth * 2, columnWidth * 2, columnWidth);
					mPhotoWallView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
				}
			}
		});
	}

	public void initData() {
		if (mPhotoWallAdapter == null) {
			mPhotoWallAdapter = new PhotoesWallAdapter(this, 0, mImageUrlList, mPhotoWallView);
		}
		mPhotoWallView.setAdapter(mPhotoWallAdapter);
		getImageListFromNet();
	}

	public void getImageListFromNet() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				String url = "http://image.baidu.com/search/index?ct=201326592&cl=2&lm=-1&nc=1&ie=utf-8&tn=baiduimage&ipn=r&pv=&fm=rs7&ofr=%E7%BE%8E%E5%A5%B3%E5%9B%BE%E7%89%87&oriquery=%E7%BE%8E%E5%A5%B3%E5%9B%BE%E7%89%87&word=%E6%A8%A1%E7%89%B9";
				mDownloadImageListUtil.setBaiduRegex(true);
				String regex = DownloadImageListUtil.regexs[2];
				final ArrayList<String> imageUrlList = mDownloadImageListUtil.ParseHtmlToImgList(url, regex);
				System.out.println("--------------------------------");
				System.out.println("mImageUrlList: " + imageUrlList);

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
		mPhotoWallAdapter.mImageLoader.fluchCache();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		// 退出程序时结束所有的下载任务
		mPhotoWallAdapter.mImageLoader.cancelAllTasks();
		mPhotoWallAdapter.mImageLoader.stopCache();
	}

}