package com.example.photoswalldemo.utility;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;

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

public final class DiskLruCacheUtil {
	private Context context;
	private DiskLruCache diskLruCache;
	private int length;

	public DiskLruCacheUtil(Context context) {
		this.context = context;
	}

	public void setItemLength(int length){
		this.length = length;
	}

	/** 创建一个DiskLruCache的实例 */
	public DiskLruCache doOpen() {
		try {
			// 创建硬件缓存文件
			File cacheDir = getDiskCacheDir(context, "bitmap");
			if (!cacheDir.exists()) {
				cacheDir.mkdirs();
			}
			// 创建一个DiskLruCache的实例
			diskLruCache = DiskLruCache.open(cacheDir, getAppVersion(context), 1, 100 * 1024 * 1024);
			return diskLruCache;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	/** DiskLruCache来进行写入，写入的操作是借助DiskLruCache.Editor这个类完成 */
	public void doEdit(final String imageUrl) {
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
			diskLruCache.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/** 从缓存中获取图片 */
	public Bitmap doGet(String imageUrl) {
		try {
			String key = hashKeyForDisk(imageUrl);
			DiskLruCache.Snapshot snapShot = diskLruCache.get(key);
			if (snapShot != null) {
				InputStream is = snapShot.getInputStream(0);
				return BitmapFactory.decodeStream(is);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	/** 从缓存中移除 */
	public void doRemove(String imageUrl) {
		try {
			String key = hashKeyForDisk(imageUrl);
			diskLruCache.remove(key);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/** 删除缓存 */
	public void doDelete() {
		try {
			diskLruCache.delete();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/** 获取缓存地址 */
	private File getDiskCacheDir(Context context, String uniqueName) {
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
	private int getAppVersion(Context context) {
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
		BufferedInputStream in = null;
		try {
			final URL url = new URL(urlString);
			urlConnection = (HttpURLConnection) url.openConnection();
			//urlConnection.setConnectTimeout(2500);
			//urlConnection.setReadTimeout(2500);
			if(urlConnection.getResponseCode() == 200){
				in = new BufferedInputStream(urlConnection.getInputStream(), 1024);
				byte[] byteData = readStream(in);
				Bitmap suitableBitmap = getSuitableBitmap(byteData);
				BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream, 1024);
				if(suitableBitmap != null){
					suitableBitmap.compress(Bitmap.CompressFormat.JPEG, 100, bufferedOutputStream);
				}
				return true;
			}
			return false;
		} catch (final Exception e) {
			e.printStackTrace();
		} finally {
			if (urlConnection != null) {
				urlConnection.disconnect();
			}
			try {
				if (in != null) {
					in.close();
				}
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	private Bitmap getSuitableBitmap(byte[] byteData) {
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeByteArray(byteData, 0, byteData.length, options);
		int actualWidth = options.outWidth;
		int actualHeight = options.outHeight;
		options.inJustDecodeBounds = false;
		options.inSampleSize = findBestSampleSize(actualWidth, actualHeight, length, length);
		System.out.println(actualWidth + ", " + actualHeight + ", " + length + ", sampleSize: " + options.inSampleSize);
		return BitmapFactory.decodeByteArray(byteData, 0, byteData.length, options);
	}

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

	/** 并把图片的URL传入到这个方法中，就可以得到对应的key,使用MD5算法对传入的key进行加密并返回 */
	private String hashKeyForDisk(String key) {
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
		for (byte aByte : bytes) {
			String hex = Integer.toHexString(0xFF & aByte);
			if (hex.length() == 1) {
				sb.append('0');
			}
			sb.append(hex);
		}
		return sb.toString();
	}

	/** 得到图片字节流再转换成字节数组 */
	private byte[] readStream(InputStream inStream) throws Exception {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		int len;
		while ((len = inStream.read(buffer)) != -1) {
			bos.write(buffer, 0, len);
		}
		bos.close();
		inStream.close();
		return bos.toByteArray();
	}

}
