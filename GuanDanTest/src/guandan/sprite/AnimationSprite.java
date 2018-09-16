/**
 * AnimationSprite.java
 * @author ��ԣ��
 * March 28th 2011
 * 
 * AnimatinoSprite��
 * �����ӻ���,ʵ�ֶ������Ź���
 */
package guandan.sprite;

import guandan.constants.Constants;
import android.graphics.Canvas;
import android.graphics.Rect;

public class AnimationSprite extends Sprite {

	// Fields////////////////////////////////////////////////

	/**
	 * ������֡��
	 */
	private int frameNum;

	/**
	 * ��ǰ�ǵڼ�֡
	 */
	private int currentFrame;

	/**
	 * ���������� ����������ͼ��
	 */
	private String animationName;

	/**
	 * ��ʾ�����Ƿ�ѭ������
	 */
	private boolean isLoop;

	/**
	 * ����һ֡��������Ϸ������
	 */
	private int frameDelay;

	/**
	 * �Ե�ǰ֡�ļ��� ÿ����Ϸ���ڼ�1 �������frameDelay��ô�����䵽��һ֡ ����ֵ���¹���
	 */
	private int frameDelayCount;

	private int oneFrameWidth;
	private int oneFrameHeight;

	// Methods///////////////////////////////////////////////

	public AnimationSprite(String aniName, boolean bLoop, int frDelay) {
		super();

		animationName = aniName;
		isLoop = bLoop;
		frameDelay = frDelay;
		currentFrame = 0;
		frameDelayCount = 0;

		setImage();
	}

	public void onDraw(Canvas canvas) {
		/**
		 * ��Ŀǰ�������������������
		 */
		canvas.drawBitmap(spriteImage,
				new Rect(currentFrame * oneFrameWidth, 0, currentFrame
						* oneFrameWidth + oneFrameWidth, oneFrameHeight),
				new Rect(spritePosX, spritePosY, spritePosX + spriteWidth,
						spritePosY + spriteHeight), null);
	}

	protected void setImage() {
		int resourceID = imageLoader.getAnimationImage(animationName);
		spriteImage = imageLoader.parseResourceID(resourceID);
		frameNum = imageLoader.getAnimationFrameNum(animationName);
		
		if(spriteImage == null || frameNum == 0)
		{
			spriteWidth = 0;
			spriteHeight = 0;
			oneFrameWidth = 0;
			oneFrameHeight = 0;
		}
		else if(frameNum > 1)
		{
			oneFrameWidth = spriteImage.getWidth()/frameNum;
			oneFrameHeight = spriteImage.getHeight();
		}
		else
		{
			oneFrameWidth = spriteImage.getWidth();
			oneFrameHeight = spriteImage.getHeight();
		}
		
		/**
		 * ����ٻ���һ��
		 */
		spriteWidth = (int)(((float)screenWidth*oneFrameWidth)/((float)Constants.SCREEN_WIDTH_DEFINED));
		spriteHeight = (int)(((float)screenHeight*oneFrameHeight)/((float)Constants.SCREEN_HEIGHT_DEFINED));
	}

	public void setLoop(boolean bLoop) {
		isLoop = bLoop;
	}

	public void update() {
		frameDelayCount++;

		if (frameDelayCount >= frameDelay) {
			frameDelayCount = 0;
			currentFrame++;

			if (currentFrame >= frameNum) {
				if (isLoop)
					currentFrame = 0;
				else
					setActive(false);
			}
		}
	}

}