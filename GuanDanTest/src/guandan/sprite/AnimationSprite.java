/**
 * AnimationSprite.java
 * @author 胡裕靖
 * March 28th 2011
 * 
 * AnimatinoSprite类
 * 动画子画面,实现动画播放功能
 */
package guandan.sprite;

import guandan.constants.Constants;
import android.graphics.Canvas;
import android.graphics.Rect;

public class AnimationSprite extends Sprite {

	// Fields////////////////////////////////////////////////

	/**
	 * 动画的帧数
	 */
	private int frameNum;

	/**
	 * 当前是第几帧
	 */
	private int currentFrame;

	/**
	 * 动画的名称 用来索引其图像
	 */
	private String animationName;

	/**
	 * 表示动画是否循环播放
	 */
	private boolean isLoop;

	/**
	 * 更新一帧动画的游戏周期数
	 */
	private int frameDelay;

	/**
	 * 对当前帧的计数 每个游戏周期加1 如果超过frameDelay那么动画变到下一帧 且其值重新归零
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
		 * 在目前非缩放情况下这样即可
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
		 * 最后再换算一次
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
