# PhotoesWallDemo
一个简单的图片墙，结合DiskLruCache、LruCache技术达到三级缓存目的，对DiskLruCache进行封装，使用更方便。另外采用网页爬虫对百度图片进行抓取。

![](imgs/1.png)


### 有3个工具类：
1. DownloadImageListUtil

	简单的网页爬虫，抓取百度的图片

		// 把图片地址从HTML文件中提取到List中
		public ArrayList<String> parseHtmlToImgList(String url, String regex){
	
		}

2. DiskLruCacheUtil

	
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

3. imageLoader


	结合DiskLruCache、LruCache技术达到三级缓存

4. 细节：本次试用的是GridView制作图片墙，每个图片的大小长宽都不一样，所以对图片进行了简单的处理：缩放截取正中部分后的位图，使图片显示在屏幕中为正方形且不会变形，看起来好看些。