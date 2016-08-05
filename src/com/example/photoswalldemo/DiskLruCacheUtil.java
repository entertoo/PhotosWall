package com.example.photoswalldemo;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
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

	public DiskLruCacheUtil(Context context) {
		this.context = context;
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
			//diskLruCache.flush();
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

	/** 下载图片 */
	private boolean downloadUrlToStream(String urlString, OutputStream outputStream) {
		HttpURLConnection urlConnection = null;
		BufferedOutputStream out = null;
		BufferedInputStream in = null;
		try {
			final URL url = new URL(urlString);
			urlConnection = (HttpURLConnection) url.openConnection();
			in = new BufferedInputStream(urlConnection.getInputStream(), 8 * 1024);
			out = new BufferedOutputStream(outputStream, 8 * 1024);
			int b;
			while ((b = in.read()) != -1) {
				out.write(b);
			}
			return true;
		} catch (final IOException e) {
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

	/** 并把图片的URL传入到这个方法中，就可以得到对应的key,使用MD5算法对传入的key进行加密并返回。 */
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

}
