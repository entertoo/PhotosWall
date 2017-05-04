package com.example.photoswalldemo.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.os.Environment;
import libcore.io.DiskLruCache;

/**
 * @author haopi
 * @创建时间 2016年7月31日 上午11:14:05
 * @描述 TODO
 * 
 * 
 * @修改提交者 $Author$
 * @提交时间 $Date$
 * @当前版本 $Rev$
 * 
 */
public class DiskLruCacheUtil
{
	private Context context;
	private int mMaxWidth;
	private int mMaxHeight;
	private Config mDecodeConfig;

	/** 设置图片的最大宽高 */
	public void setMaxWidthAndHeight(int maxWidth, int maxHeight) {
		mMaxWidth = maxWidth;
		mMaxHeight = maxHeight;
	}

	public DiskLruCacheUtil(Context context, Config decodeConfig) {
		this.context = context;
		this.mDecodeConfig = decodeConfig;
	}

	/** 创建一个DiskLruCache的实例 */
	public DiskLruCache doOpen() {
		DiskLruCache diskLruCache = null;
		try {
			// 创建硬件缓存文件
			File cacheDir = getDiskCacheDir(context, "bitmap");
			if (!cacheDir.exists()) {
				cacheDir.mkdirs();
			}
			// 创建一个DiskLruCache的实例
			diskLruCache = DiskLruCache.open(cacheDir, getAppVersion(context), 1, 10 * 1024 * 1024);
			return diskLruCache;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return diskLruCache;
	}

	/** DiskLruCache来进行写入，写入的操作是借助DiskLruCache.Editor这个类完成 */
	public void doEdit(final String imageUrl, final DiskLruCache diskLruCache) {
		try {
			String key = hashKeyForDisk(imageUrl);
			DiskLruCache.Editor editor = diskLruCache.edit(key);
			if (editor != null) {
				OutputStream outputStream = editor.newOutputStream(0);
				if (downloadUrlToStream(imageUrl, outputStream)) {
					editor.commit();
				} else {
					editor.abort();
				}
			}
			// diskLruCache.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/** 从缓存中获取图片 */
	public Bitmap doGet(String imageUrl, DiskLruCache diskLruCache) {
		try {
			String key = hashKeyForDisk(imageUrl);
			DiskLruCache.Snapshot snapShot = diskLruCache.get(key);
			if (snapShot != null) {
				InputStream is = snapShot.getInputStream(0);
				Bitmap bitmap = BitmapFactory.decodeStream(is);
				return bitmap;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	/** 从缓存中移除 */
	public void doRemove(String imageUrl, DiskLruCache diskLruCache) {
		try {
			String key = hashKeyForDisk(imageUrl);
			diskLruCache.remove(key);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/** 获取缓存地址 */
	public File getDiskCacheDir(Context context, String uniqueName) {
		String cachePath;
		if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
				|| !Environment.isExternalStorageRemovable()) {
			cachePath = context.getExternalCacheDir().getPath();
		} else {
			cachePath = context.getCacheDir().getPath();
		}
		return new File(cachePath + File.separator + uniqueName);
	}

	/** 获取当前应用版本号 */
	public int getAppVersion(Context context) {
		try {
			PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
			return info.versionCode;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		return 1;
	}

	/** 网络下载图片 */
	private boolean downloadUrlToStream(String urlString, OutputStream outputStream) {
		HttpURLConnection urlConnection = null;
		BufferedOutputStream out = null;
		BufferedInputStream in = null;
		try {
			final URL url = new URL(urlString);
			urlConnection = (HttpURLConnection) url.openConnection();
			in = new BufferedInputStream(urlConnection.getInputStream(), 8 * 1024);
			// 中间隔断，从源头控制缓存图片的大小，防止OOM
			byte[] byteData = readStream(in);
			// 控制需要的图片大小
			Bitmap suitableBitmap = getSuitableBitmap(byteData);
			suitableBitmap.compress(CompressFormat.JPEG, 100, new BufferedOutputStream(outputStream, 8 * 1024));

			// out = new BufferedOutputStream(outputStream, 8 * 1024);
			// int b;
			// while ((b = in.read()) != -1) {
			// out.write(b);
			// }

			return true;
		} catch (final Exception e) {
			e.printStackTrace();
		} finally {
			if (urlConnection != null) {
				urlConnection.disconnect();
			}
			try {
				if (out != null) {
					out.close();
				}
				if (in != null) {
					in.close();
				}
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	/** 并把图片的URL传入到这个方法中，就可以得到对应的key,使用MD5算法对传入的key进行加密并返回 */
	public String hashKeyForDisk(String key) {
		String cacheKey;
		try {
			final MessageDigest mDigest = MessageDigest.getInstance("MD5");
			mDigest.update(key.getBytes());
			cacheKey = bytesToHexString(mDigest.digest());
		} catch (NoSuchAlgorithmException e) {
			cacheKey = String.valueOf(key.hashCode());
		}
		return cacheKey;
	}

	private String bytesToHexString(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < bytes.length; i++) {
			String hex = Integer.toHexString(0xFF & bytes[i]);
			if (hex.length() == 1) {
				sb.append('0');
			}
			sb.append(hex);
		}
		return sb.toString();
	}

	// ---------------------------------拓展:获取合适的图片大小-------------------------------------//
	/** 得到图片字节流再转换成字节数组 */
	public byte[] readStream(InputStream inStream) throws Exception {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024 * 8];
		int len = 0;
		while ((len = inStream.read(buffer)) != -1) {
			bos.write(buffer, 0, len);
		}
		bos.close();
		inStream.close();
		return bos.toByteArray();
	}

	/** 获取合适大小的图片 */
	public Bitmap getSuitableBitmap(byte[] data) {
		BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
		Bitmap bitmap = null;
		if (mMaxWidth == 0 && mMaxHeight == 0) {
			decodeOptions.inPreferredConfig = mDecodeConfig;
			bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, decodeOptions);
		} else {
			// If we have to resize this image, first get the natural bounds.
			decodeOptions.inJustDecodeBounds = true;
			BitmapFactory.decodeByteArray(data, 0, data.length, decodeOptions);
			int actualWidth = decodeOptions.outWidth;
			int actualHeight = decodeOptions.outHeight;

			// Then compute the dimensions we would ideally like to decode to.
			int desiredWidth = getResizedDimension(mMaxWidth, mMaxHeight, actualWidth, actualHeight);
			int desiredHeight = getResizedDimension(mMaxHeight, mMaxWidth, actualHeight, actualWidth);

			// Decode to the nearest power of two scaling factor.
			decodeOptions.inJustDecodeBounds = false;
			// decodeOptions.inPreferQualityOverSpeed = PREFER_QUALITY_OVER_SPEED;
			decodeOptions.inSampleSize = findBestSampleSize(actualWidth, actualHeight, desiredWidth, desiredHeight);
			Bitmap tempBitmap = BitmapFactory.decodeByteArray(data, 0, data.length, decodeOptions);

			// If necessary, scale down to the maximal acceptable size.
			if (tempBitmap != null
					&& (tempBitmap.getWidth() > desiredWidth || tempBitmap.getHeight() > desiredHeight)) {
				bitmap = Bitmap.createScaledBitmap(tempBitmap, desiredWidth, desiredHeight, true);
				tempBitmap.recycle();
			} else {
				bitmap = tempBitmap;
			}
		}
		return bitmap;
	}

	/**
     * Scales one side of a rectangle to fit aspect ratio.
     *
     * @param maxPrimary Maximum size of the primary dimension (i.e. width for
     *        max width), or zero to maintain aspect ratio with secondary
     *        dimension
     * @param maxSecondary Maximum size of the secondary dimension, or zero to
     *        maintain aspect ratio with primary dimension
     * @param actualPrimary Actual size of the primary dimension
     * @param actualSecondary Actual size of the secondary dimension
     */
	private int getResizedDimension(int maxPrimary, int maxSecondary, int actualPrimary, int actualSecondary) {
		// If no dominant value at all, just return the actual.
		if (maxPrimary == 0 && maxSecondary == 0) {
			return actualPrimary;
		}
		// If primary is unspecified, scale primary to match secondary's scaling ratio.
		if (maxPrimary == 0) {
			double ratio = (double) maxSecondary / (double) actualSecondary;
			return (int) (actualPrimary * ratio);
		}
		if (maxSecondary == 0) {
			return maxPrimary;
		}
		double ratio = (double) actualSecondary / (double) actualPrimary;
		int resized = maxPrimary;
		if (resized * ratio > maxSecondary) {
			resized = (int) (maxSecondary / ratio);
		}
		return resized;
	}

	/**
	 * 计算出最佳的图片缩放比例
     * Returns the largest power-of-two divisor for use in downscaling a bitmap
     * that will not result in the scaling past the desired dimensions.
     *
     * @param actualWidth Actual width of the bitmap
     * @param actualHeight Actual height of the bitmap
     * @param desiredWidth Desired width of the bitmap
     * @param desiredHeight Desired height of the bitmap
     */
	private int findBestSampleSize(int actualWidth, int actualHeight, int desiredWidth, int desiredHeight) {
		double wr = (double) actualWidth / desiredWidth;
		double hr = (double) actualHeight / desiredHeight;
		double ratio = Math.min(wr, hr);
		float n = 1.0f;
		while ((n * 2) <= ratio) {
			n *= 2;
		}
		return (int) n;
	}
	// ---------------------------------拓展:获取合适的图片大小-------------------------------------//
}
