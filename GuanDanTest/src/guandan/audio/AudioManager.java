package guandan.audio;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import guandan.game.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.util.Log;


public class AudioManager {
	static final String TAG = "AudioMgr";

	static private AudioManager am;

	// Game sound (WAVs)
	private volatile static HashMap<String, AudioClip> mSounds = new HashMap<String, AudioClip>();

	private int MAX_CLIPS = 20;
	private int mClipCount = 0;
	private static Context mContext;

	// BG music
	private static AudioClip music;
	private static int mVolume;
	private static int sVolume;

	private SharedPreferences music_setting;

	public static synchronized AudioManager getInstance() {
		return am;
	}

	
	public static void initInstance(Context context) {
		if (am == null)
			am = new AudioManager(context);
	}

	private AudioManager(Context context) {
		mContext = context;
		music_setting = context.getSharedPreferences("Guandan_Setting",
				Context.MODE_PRIVATE);

		mVolume = music_setting.getBoolean("music", true) ? 100 : 0;
		sVolume = music_setting.getBoolean("sound", true) ? 100 : 0;
		preloadSounds(context);

		am = this;
	}

	public synchronized void startSound(int sidx) {

		if (sidx < 0 || sidx >= SoundsName.sounds.length)
			return;

		// The sound key
		int id = SoundsName.sounds[sidx];
		String key;

		if (id == 0)
			return;

		try {
			key = mContext.getResources().getResourceName(id);// name.toLowerCase();
		} catch (NotFoundException e) {
			return;
		}

		if (mSounds.containsKey(key)) {
			// Log.d(TAG, "Playing " + key + " from cache");
			mSounds.get(key).play();
		} else {
			// load clip from disk

			// If the sound table is full remove a random entry
			if (mClipCount > MAX_CLIPS) {
				// Remove a last key
				int idx = mSounds.size() - 1;

				// Log.d(TAG, "Removing cached sound " + idx
				// + " HashMap size=" + mSounds.size());

				String k = (String) mSounds.keySet().toArray()[idx];
				AudioClip clip = mSounds.remove(k);
				clip.release();
				clip = null;
				mClipCount--;
			}

			// Log.d(TAG, "Play & Cache " + key + " id:" + id);

			AudioClip clip = new AudioClip(mContext, id);
			clip.setVolume(sVolume);
			clip.play();

			mSounds.put(key, clip);
			mClipCount++;
		}
	}

	/**
	 * @param context
	 *            先载入必须的声音文件
	 */
	private void preloadSounds(Context context) {
		int[] IDS = new int[] { R.raw.menuback, R.raw.battleback,
				R.raw.clickmenu, R.raw.start };

		// WAVs
		Resources res = mContext.getResources();

		for (int i = 0; i < IDS.length; i++) {
			final int id = IDS[i];
			final String key = res.getResourceName(id);

			Log.d(TAG, "PreLoading sound " + key + " ID " + id);
			mSounds.put(key, new AudioClip(context, id));
		}

	}

	public static void startMusic(int midx) {

		if (midx < 0 || midx >= SoundsName.musics.length)
			return;

		int id = SoundsName.musics[midx];

		if (id == 0)
			return;

		try {
			mContext.getResources().getResourceName(id);
		} catch (NotFoundException e) {
			System.err.println("Music resID for idx " + midx + " no found");
			return;
		}

		if (music != null) {
			music.stop();
			music.release();
		}

		Log.d(TAG, "Starting music " + id);
		music = new AudioClip(mContext, id); // Uri.fromFile( sound ));

		music.setVolume(mVolume);
		music.loop();
	}

	public static void stopMusic() {
		music.stop();
	}

	/**
	 * @param vol
	 */
	public static void setMusicVolume(int vol) {
		mVolume = vol;
		if (music != null) {
			music.setVolume(mVolume);
		} else
			Log.e(TAG, "setMusicVolume " + vol
					+ " called with NULL music player");
	}

	public static void musicOff() {
		
		//stopMusic();
		setMusicVolume(1);
	}

	public static void musicOn() {
		
		//startMusic(midx)
		setMusicVolume(10);
	}

	public void setSoundVolume(int vol) {
		sVolume = vol;
	}

	public static void soundOff() {
		sVolume = 1;
		Iterator iter = mSounds.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry entry = (Map.Entry) iter.next();
			((AudioClip) entry.getValue()).setVolume(sVolume);
		}

	}

	public static void soundOn() {
		sVolume = 10;
		Iterator iter = mSounds.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry entry = (Map.Entry) iter.next();
			((AudioClip) entry.getValue()).setVolume(sVolume);
		}
	}
}
