/**
 * ImageLoader.java
 * @author 李若冰
 * Mar 17th 2011
 * 
 * 图片加载类
 * 加载工程中所需图片
 */

package guandan.game;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;

import guandan.constants.Constants;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.content.Context;
import android.content.res.Resources;

public class ImageLoader {

	

	private int numOfDesktop;
	private Integer[][] pokerImages;
	private HashMap<String, Integer> photoesImageMap;
	private HashMap<String, Integer> animationImageMap;
	private HashMap<String, Integer> animationFrameNumMap;

	// private List<String> nameOfNpc;
	private Context imageLoaderContext;

	public Context getImageLoaderContext() {
		return imageLoaderContext;
	}

	private ArrayList<Integer> desktopImageList;
	public ArrayList<Integer> getDesktopImageList() {
		return desktopImageList;
	}
	
	private ArrayList<String> selectedOpponentList;

	/**
	 * 当前选中的npc头像数目
	 */
	private int countOpponent;

	/**
	 * 当前选中的桌面背景
	 */
	private int indexDesktop;

	private int screenWidth;

	public int getScreenWidth() {
		return screenWidth;
	}

	private int screenHeight;

	public int getScreenHeight() {
		return screenHeight;
	}

	private static ImageLoader imageLoader;

	public synchronized static ImageLoader getInstance() {
		return imageLoader;
	}

	public  static void createInstance(Context context, int width,
			int height) {
		if (imageLoader == null)
			imageLoader = new ImageLoader(context, width, height);
	}

	private ImageLoader(Context context, int width, int height) {
		imageLoaderContext = context;

		screenWidth = width;
		screenHeight = height;
		/**
		 * 初始化选中的npc头像列表
		 */
		initSelectedOpponentList();

		loadDesktops();
		loadPokerImages();
		loadPhotoes();
		loadAnimationImages();

		// loadButtonImages();
	}

	public void initSelectedOpponentList() {
		selectedOpponentList = new ArrayList<String>();
		countOpponent = 0;

		addToSelectedOpponentList("Bingo");
		addToSelectedOpponentList("Jack");
		addToSelectedOpponentList("Micheal");
	}

	public void makeNullSelectedOpponentList() {
		selectedOpponentList = new ArrayList<String>();
		countOpponent = 0;
	}

	/**
	 * 清空选中的npc头像链表
	 */
	public void clearSelectedOpponentList() {
		selectedOpponentList.clear();
		countOpponent = 0;
	}

	/**
	 * npc头像是否选满，选满为3
	 * 
	 * @return
	 */
	public Boolean isSelectedOpponentListFull() {
		return (countOpponent == 3);
	}

	/**
	 * 设置当前选中的桌面背景索引号
	 * 
	 * @param index
	 */
	public void setIndexDesktop(int index) {
		indexDesktop = index;
	}

	/**
	 * 得到当前选中的桌面背景索引号
	 * 
	 * @return indexDesktop
	 */
	public int getIndexDesktop() {
		return indexDesktop;
	}

	/**
	 * 得到当前已选中的npc数目
	 * 
	 * @return countOpponent
	 */
	public int getCountOpponent() {
		return countOpponent;
	}

	/**
	 * 得到相应的NPC名字
	 * 
	 * @param index
	 * @return
	 */
	public String getTheOpponent(int index) {
		return selectedOpponentList.get(index);
	}

	/**
	 * 得到npc头像链表
	 * 
	 * @return
	 */
	public ArrayList<String> getSelectedOpponentList() {
		return selectedOpponentList;
	}

	/**
	 * 增加一个npc头像
	 * 
	 * @param oppName
	 */
	public void addToSelectedOpponentList(String oppName) {
		++countOpponent;
		selectedOpponentList.add(oppName);
	}

	/**
	 * 删去一个npc头像
	 * 
	 * @param oppName
	 */
	public void deleteFromSelectedOpponentList(String oppName) {
		--countOpponent;
		selectedOpponentList.remove(oppName);
	}

	/**
	 * 该函数从文件中读取各个桌布的图像，并将其存放到成员desktopImageList中
	 * 
	 * 修改 By 胡裕靖 将原来的readDesktopTxt去掉 直接在本函数内部进行文本文件的读取
	 */
	private void loadDesktops() {

		desktopImageList = new ArrayList<Integer>();
		String defPackageName = imageLoaderContext.getResources()
				.getResourcePackageName(0x7f020000);

		try {
			InputStream is = imageLoaderContext.getResources().openRawResource(
					R.raw.desktop);
			BufferedReader bufReader = new BufferedReader(
					new InputStreamReader(is));
			String line = "";

			numOfDesktop = 0;
			while ((line = bufReader.readLine()) != null) {
				int resourceID = imageLoaderContext.getResources()
						.getIdentifier(line.toString(), "drawable",
								defPackageName);

				desktopImageList.add(resourceID);
				++numOfDesktop;
			}

			bufReader.close();
			is.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public int getNumOfDesktop() {
		return numOfDesktop;
	}

	/**
	 * 该函数从文件中载入所有NPC的头像图像， 将其存放到哈希表成员photoesImageMap中
	 * 
	 * 修改 By 胡裕靖 将原来的readPhotoImageTxt去掉 直接在本函数内部进行文本文件的读取
	 * 并使用StringTokenizer类来对每一行进行分割 因为该类可以支持分割符为正则表达式
	 */
	private void loadPhotoes() {

		photoesImageMap = new HashMap<String, Integer>();
		String defPackageName = imageLoaderContext.getResources()
				.getResourcePackageName(0x7f020000);

		try {
			InputStream is = imageLoaderContext.getResources().openRawResource(
					R.raw.npc);
			BufferedReader bufReader = new BufferedReader(
					new InputStreamReader(is));
			String line = "";

			while ((line = bufReader.readLine()) != null) {
				StringTokenizer token = new StringTokenizer(line);

				String imgFileName = token.nextToken();
				String npcName = token.nextToken();

				int resourceID = imageLoaderContext.getResources()
						.getIdentifier(imgFileName, "drawable", defPackageName);

				photoesImageMap.put(npcName, resourceID);
			}
			bufReader.close();
			is.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 载入扑克牌的图像到内存中，放入成员pokerImages中
	 * 
	 * 修改 By 胡裕靖 将原来的readPokerImageTxt去掉 直接在本函数内部进行文本文件的读取
	 * 并使用StringTokenizer类来对每一行进行分割 因为该类可以支持分割符为正则表达式
	 */
	private void loadPokerImages() {

		pokerImages = new Integer[5][14];
		String defPackageName = imageLoaderContext.getResources()
				.getResourcePackageName(0x7f020000);

		try {
			InputStream is = imageLoaderContext.getResources().openRawResource(
					R.raw.poker);
			BufferedReader br = new BufferedReader(new InputStreamReader(is));

			String line = "";

			while ((line = br.readLine()) != null) {
				StringTokenizer tokenizer = new StringTokenizer(line);
				String imgFileName = tokenizer.nextToken();
				int x = Integer.parseInt(tokenizer.nextToken());
				int y = Integer.parseInt(tokenizer.nextToken());

				int resourceID = imageLoaderContext.getResources()
						.getIdentifier(imgFileName, "drawable", defPackageName);

				pokerImages[x][y] = resourceID;
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 加载动画图像
	 * 
	 * 修改 By 胡裕靖 将原来的readAnimationImageTxt去掉 直接在本函数内部进行文本文件的读取
	 * 并使用StringTokenizer类来对每一行进行分割 因为该类可以支持分割符为正则表达式
	 */
	private void loadAnimationImages() {

		animationImageMap = new HashMap<String, Integer>();
		animationFrameNumMap = new HashMap<String, Integer>();

		String defPackageName = imageLoaderContext.getResources()
				.getResourcePackageName(0x7f020000);

		try {
			InputStream is = imageLoaderContext.getResources().openRawResource(
					R.raw.animation);
			BufferedReader bufReader = new BufferedReader(
					new InputStreamReader(is));

			String line = "";
			while ((line = bufReader.readLine()) != null) {
				StringTokenizer token = new StringTokenizer(line);
				String aniName = token.nextToken();
				String imgFileName = token.nextToken();
				int frameNum = Integer.parseInt(token.nextToken());

				int resourceID = imageLoaderContext.getResources()
						.getIdentifier(imgFileName, "drawable", defPackageName);

				animationFrameNumMap.put(aniName, frameNum);
				animationImageMap.put(aniName, resourceID);
			}

			bufReader.close();
			is.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 添加 By 胡裕靖 加载按钮图像 raw文件夹的button_images.txt记录各个按钮的名称和图像文件名
	 * 该函数根据这个文本文件来依次读取图像
	 */
	// private void loadButtonImages()
	// {
	// buttonImageMap = new HashMap<String, ArrayList<Integer>>();
	//
	// String defPackageName =
	// imageLoaderContext.getResources().getResourcePackageName(0x7f020000);
	//
	// try
	// {
	// InputStream inStream =
	// imageLoaderContext.getResources().openRawResource(R.raw.button_images);
	// BufferedReader bufReader = new BufferedReader(new
	// InputStreamReader(inStream));
	// String line = "";
	//
	// while((line = bufReader.readLine()) != null)
	// {
	// StringTokenizer token = new StringTokenizer(line);
	//
	// /**
	// * 每一行的第一个字段记录按钮的名称
	// */
	// String buttonName = token.nextToken();
	//
	// ArrayList<Integer> buttonImgIDList = new ArrayList<Integer>();
	//
	// while(token.countTokens() != 0)
	// {
	// String imgName = token.nextToken();
	//
	// int resourceID = imageLoaderContext.getResources().getIdentifier(
	// imgName, "drawable", defPackageName );
	//
	// buttonImgIDList.add(resourceID);
	// }
	//
	// if(!buttonImageMap.containsKey(buttonName))
	// {
	// buttonImageMap.put(buttonName, buttonImgIDList);
	// }
	// }
	//
	// bufReader.close();
	// inStream.close();
	// }
	// catch(IOException e) {
	// e.printStackTrace();
	// }
	// }

	public int getDesktopImage(int index) {
		return desktopImageList.get(index);
	}

	public int getPokerImage(int pattern, int points) {
		return pokerImages[pattern][points];
	}

	public int getPhotoImage(String aiName) {
		return photoesImageMap.get(aiName);
	}

	public int getAnimationImage(String aniName) {
		return animationImageMap.get(aniName);
	}

	public int getAnimationFrameNum(String aniName) {
		// 返回的是Integer对象
		return animationFrameNumMap.get(aniName);
	}

	// public ArrayList<Integer> getButtonImage(String buttonName)
	// {
	// return buttonImageMap.get(buttonName);
	// }

	public Bitmap parseResourceID(int resourceID) {
		Resources r = imageLoaderContext.getResources();
		Drawable drawable = r.getDrawable(resourceID);
		int width = drawable.getIntrinsicWidth();
		int height = drawable.getIntrinsicHeight();

		width = (int) (((float) screenWidth * width) / ((float) Constants.SCREEN_WIDTH_DEFINED));
		height = (int) (((float) screenHeight * height) / ((float) Constants.SCREEN_HEIGHT_DEFINED));

		Bitmap bitmap = Bitmap.createBitmap(width, height, drawable
				.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888
				: Bitmap.Config.RGB_565);
		Canvas canvas = new Canvas(bitmap);
		drawable.setBounds(0, 0, width, height);
		drawable.draw(canvas);
		return bitmap;

	}

	public Bitmap getBitmapById(int resourceID){
		return BitmapFactory.decodeResource(
				imageLoaderContext.getResources(),
				resourceID);
	}
}
