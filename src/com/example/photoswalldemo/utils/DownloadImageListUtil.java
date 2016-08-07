package com.example.photoswalldemo.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.text.TextUtils;

/**
 * @author haopi
 * @创建时间 2016年7月29日 下午6:22:01
 * @描述 TODO
 * 
 * 
 * @修改提交者 $Author: chp $
 * @提交时间 $Date: 2016-07-31 10:50:29 +0800 (Sun, 31 Jul 2016) $
 * @当前版本 $Rev: 19 $
 * 
 */
public class DownloadImageListUtil
{
	private boolean isBaiduRegex;

	public void setBaiduRegex(boolean isBaiduRegex) {
		this.isBaiduRegex = isBaiduRegex;
	}

	// 规则0、1是普通图片规则，2是百度图片规则
	public static String[] regexs = { "\\b(http://){1}[^\\s]+?(\\.jpg){1}\\b",
			"<img\\b[^>]*\\bsrc\\b\\s*=\\s*('|\")?([^'\"\n\r\f>]+(\\.jpg)\\b)[^>]*>", "\"objURL\":\"(.*?)\"" };

	// 把图片地址从HTML文件中解析出来
	public ArrayList<String> ParseHtmlToImgList(String url, String regex) {
		// 从网页获取Html数据
		String html = getImageHtml(url);
		if (html != null) {
			ArrayList<String> imgList = new ArrayList<String>();
			Pattern pattern = Pattern.compile(regex);
			Matcher matcher = pattern.matcher(html);
			String group = null;
			if (isBaiduRegex) {
				while (matcher.find()) {
					group = matcher.group(1);
					if (!TextUtils.isEmpty(group)) {
						imgList.add(group);
					}
				}
			} else {
				while (matcher.find()) {
					group = matcher.group();
					if (!TextUtils.isEmpty(group)) {
						imgList.add(group);
					}
				}
			}
			return imgList;
		}
		return null;
	}

	// 从URL获取HTML
	private String getImageHtml(String url) {
		try {
			URL img_Url = new URL(url);
			HttpURLConnection connection = (HttpURLConnection) img_Url.openConnection();
			connection.setConnectTimeout(10000);
			connection.setRequestMethod("GET");
			int responseCode = connection.getResponseCode();

			if (responseCode == 200) {
				InputStream is = connection.getInputStream();
				BufferedReader reader = new BufferedReader(new InputStreamReader(is));
				StringBuilder sb = new StringBuilder();
				String line = null;
				while ((line = reader.readLine()) != null) {
					sb.append(line + '\n');
				}
				is.close();
				return sb.toString();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

}
