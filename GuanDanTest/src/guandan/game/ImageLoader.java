/**
 * ImageLoader.java
 * @author ������
 * Mar 17th 2011
 * 
 * ͼƬ������
 * ���ع���������ͼƬ
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
	 * ��ǰѡ�е�npcͷ����Ŀ
	 */
	private int countOpponent;

	/**
	 * ��ǰѡ�е����汳��
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
		 * ��ʼ��ѡ�е�npcͷ���б�
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
	 * ���ѡ�е�npcͷ������
	 */
	public void clearSelectedOpponentList() {
		selectedOpponentList.clear();
		countOpponent = 0;
	}

	/**
	 * npcͷ���Ƿ�ѡ����ѡ��Ϊ3
	 * 
	 * @return
	 */
	public Boolean isSelectedOpponentListFull() {
		return (countOpponent == 3);
	}

	/**
	 * ���õ�ǰѡ�е����汳��������
	 * 
	 * @param index
	 */
	public void setIndexDesktop(int index) {
		indexDesktop = index;
	}

	/**
	 * �õ���ǰѡ�е����汳��������
	 * 
	 * @return indexDesktop
	 */
	public int getIndexDesktop() {
		return indexDesktop;
	}

	/**
	 * �õ���ǰ��ѡ�е�npc��Ŀ
	 * 
	 * @return countOpponent
	 */
	public int getCountOpponent() {
		return countOpponent;
	}

	/**
	 * �õ���Ӧ��NPC����
	 * 
	 * @param index
	 * @return
	 */
	public String getTheOpponent(int index) {
		return selectedOpponentList.get(index);
	}

	/**
	 * �õ�npcͷ������
	 * 
	 * @return
	 */
	public ArrayList<String> getSelectedOpponentList() {
		return selectedOpponentList;
	}

	/**
	 * ����һ��npcͷ��
	 * 
	 * @param oppName
	 */
	public void addToSelectedOpponentList(String oppName) {
		++countOpponent;
		selectedOpponentList.add(oppName);
	}

	/**
	 * ɾȥһ��npcͷ��
	 * 
	 * @param oppName
	 */
	public void deleteFromSelectedOpponentList(String oppName) {
		--countOpponent;
		selectedOpponentList.remove(oppName);
	}

	/**
	 * �ú������ļ��ж�ȡ����������ͼ�񣬲������ŵ���ԱdesktopImageList��
	 * 
	 * �޸� By ��ԣ�� ��ԭ����readDesktopTxtȥ�� ֱ���ڱ������ڲ������ı��ļ��Ķ�ȡ
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
	 * �ú������ļ�����������NPC��ͷ��ͼ�� �����ŵ���ϣ����ԱphotoesImageMap��
	 * 
	 * �޸� By ��ԣ�� ��ԭ����readPhotoImageTxtȥ�� ֱ���ڱ������ڲ������ı��ļ��Ķ�ȡ
	 * ��ʹ��StringTokenizer������ÿһ�н��зָ� ��Ϊ�������֧�ַָ��Ϊ�������ʽ
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
	 * �����˿��Ƶ�ͼ���ڴ��У������ԱpokerImages��
	 * 
	 * �޸� By ��ԣ�� ��ԭ����readPokerImageTxtȥ�� ֱ���ڱ������ڲ������ı��ļ��Ķ�ȡ
	 * ��ʹ��StringTokenizer������ÿһ�н��зָ� ��Ϊ�������֧�ַָ��Ϊ�������ʽ
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
	 * ���ض���ͼ��
	 * 
	 * �޸� By ��ԣ�� ��ԭ����readAnimationImageTxtȥ�� ֱ���ڱ������ڲ������ı��ļ��Ķ�ȡ
	 * ��ʹ��StringTokenizer������ÿһ�н��зָ� ��Ϊ�������֧�ַָ��Ϊ�������ʽ
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
	 * ���� By ��ԣ�� ���ذ�ťͼ�� raw�ļ��е�button_images.txt��¼������ť�����ƺ�ͼ���ļ���
	 * �ú�����������ı��ļ������ζ�ȡͼ��
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
	// * ÿһ�еĵ�һ���ֶμ�¼��ť������
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
		// ���ص���Integer����
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