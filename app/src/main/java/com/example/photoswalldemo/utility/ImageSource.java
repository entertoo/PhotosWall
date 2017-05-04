package com.example.photoswalldemo.utility;

import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * haopi
 * 2016年7月29日 下午6:22:01
 * 
 */
public class ImageSource
{
	private boolean isDuRegex;

	public void setDuRegex(boolean isBaiduRegex) {
		this.isDuRegex = isBaiduRegex;
	}

	// 规则0、1是普通图片规则，2是百度图片规则
	public static String[] regex = { "\\b(http://){1}[^\\s]+?(\\.jpg){1}\\b",
			"<img\\b[^>]*\\bsrc\\b\\s*=\\s*('|\")?([^'\"\n\r\f>]+(\\.jpg)\\b)[^>]*>", "\"objURL\":\"(.*?)\"" };

	// 把图片地址从HTML文件中解析出来
	public ArrayList<String> ParseHtmlToImage(String url, String regex) {
		// 从网页获取Html数据
		String html = getImageHtml(url);
		if (html != null) {
			ArrayList<String> imgList = new ArrayList<>();
			Pattern pattern = Pattern.compile(regex);
			Matcher matcher = pattern.matcher(html);
			String group;
			if (isDuRegex) {
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
			connection.setConnectTimeout(2500);
			connection.setRequestMethod("GET");
			int responseCode = connection.getResponseCode();

			if (responseCode == 200) {
				InputStream is = connection.getInputStream();
				BufferedReader reader = new BufferedReader(new InputStreamReader(is));
				StringBuilder sb = new StringBuilder();
				String line;
				while ((line = reader.readLine()) != null) {
					sb.append(line).append('\n');
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
