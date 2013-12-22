package com.charon.dmc;

import java.util.ArrayList;
import java.util.List;

import org.cybergarage.upnp.Device;

import com.charon.dmc.engine.DLNAContainer;
import com.charon.dmc.engine.MultiPointController;
import com.charon.dmc.inter.IController;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class ControlActivity extends Activity {
	private IController mController;
	private TextView tv_title;
	private SeekBar sb_progress;
	private TextView tv_current;
	private TextView tv_total;
	private ImageView iv_pre;
	private ImageView iv_next;
	private ImageView iv_play;
	private ImageView iv_pause;
	private ImageView iv_back_fast;
	private ImageView iv_go_fast;
	private SeekBar sb_voice;
	private ImageView iv_mute;
	private ImageView iv_volume;

	private Device mDevice;

	private List<String> urls = new ArrayList<String>();
	private int index;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_control);
		setController(new MultiPointController());
		findView();
		initView();
	}

	private void setController(IController controller) {
		this.mController = controller;
	}

	private void findView() {
		tv_title = (TextView) findViewById(R.id.tv_title);
		sb_progress = (SeekBar) findViewById(R.id.sb_progress);
		tv_current = (TextView) findViewById(R.id.tv_current);
		tv_total = (TextView) findViewById(R.id.tv_total);
		iv_pre = (ImageView) findViewById(R.id.iv_pre);
		iv_next = (ImageView) findViewById(R.id.iv_next);
		iv_play = (ImageView) findViewById(R.id.iv_play);
		iv_pause = (ImageView) findViewById(R.id.iv_pause);
		iv_back_fast = (ImageView) findViewById(R.id.iv_back_fast);
		iv_go_fast = (ImageView) findViewById(R.id.iv_go_fast);
		sb_voice = (SeekBar) findViewById(R.id.sb_voice);
		iv_mute = (ImageView) findViewById(R.id.iv_mute);
		iv_volume = (ImageView) findViewById(R.id.iv_volume);
	}

	private void initView() {
		urls.add("");
		urls.add("");
		urls.add("");
		mDevice = DLNAContainer.getInstance().getSelectedDevice();
		// init the state
		int maxVolumnValue = mController.getMaxVolumeValue(mDevice);
		if (maxVolumnValue == -1) {
			maxVolumnValue = 0;
		}
		sb_voice.setMax(maxVolumnValue);

		int currentVoice = mController.getVoice(mDevice);
		if (currentVoice == -1) {
			currentVoice = 0;
		}
		sb_voice.setProgress(currentVoice);

		String mute = mController.getMute(mDevice);
		if (mute != null) {
			initMuteImg(mute);
		}
		
		sb_progress.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				isAutoPlay = false;
				int progress = seekBar.getProgress();
				seek(mediaRenderDevice, secToTime(progress));
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				removeChangeMessage();
			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				tv_current.setText(secToTime(progress));
				if (fromUser) {
					removeChangeMessage();
					if (System.currentTimeMillis() - preSeekTime < 1000) {
						return;
					} else {
						preSeekTime = System.currentTimeMillis();
					}
					// seek(mediaRenderDevice, secToTime(progress));
				}
			}
		});

		getMute();

		sb_voice.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				setVoice(mediaRenderDevice, seekBar.getProgress());
				if (iv_mute.getVisibility() == View.VISIBLE) {
					setMute(mediaRenderDevice, "0");
				}

				if (seekBar.getProgress() <= 1) {
					setMute(mediaRenderDevice, "1");
				}
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				// 声音为0的时候让其静音
				if (seekBar.getProgress() <= 1) {
					iv_mute.setVisibility(View.VISIBLE);
					iv_volume.setVisibility(View.GONE);
					setMute(mediaRenderDevice, "1");
				}
				if (seekBar.getProgress() >= 2) {
					iv_mute.setVisibility(View.GONE);
					iv_volume.setVisibility(View.VISIBLE);
				}
			}
		});
		initState();
		fl_play_switch.setOnClickListener(this);
		iv_pre.setOnClickListener(this);
		iv_next.setOnClickListener(this);
		fl_volume.setOnClickListener(this);
		iv_back.setOnClickListener(this);
		iv_go_fast.setOnClickListener(this);
		iv_back_fast.setOnClickListener(this);

		tv_device.setText(operator.getRenderName());

		mController.play(mDevice, getCurrentPlayPath());

	}

	private void initMuteImg(String mute) {
		if ("1".equals(mute)) {
			iv_mute.setVisibility(View.VISIBLE);
			iv_volume.setVisibility(View.GONE);
			sb_voice.setProgress(0);
		} else if ("0".equals(mute)) {
			iv_mute.setVisibility(View.GONE);
			iv_volume.setVisibility(View.VISIBLE);
		}
	}

	private String getCurrentPlayPath() {
		return urls.get(index);
	}

	/**
	 * 将分装数转换成标准的时间字符串
	 * 
	 * @param time
	 * @return
	 */
	public static String secToTime(int time) {
		String timeStr = null;
		int hour = 0;
		int minute = 0;
		int second = 0;
		if (time <= 0)
			return "00:00:00";
		else {
			minute = time / 60;
			if (minute < 60) {
				second = time % 60;
				timeStr = "00:" + unitFormat(minute) + ":" + unitFormat(second);
			} else {
				hour = minute / 60;
				if (hour > 99)
					return "99:59:59";
				minute = minute % 60;
				second = time - hour * 3600 - minute * 60;
				timeStr = unitFormat(hour) + ":" + unitFormat(minute) + ":"
						+ unitFormat(second);
			}
		}
		return timeStr;
	}

	public static String unitFormat(int i) {
		String retStr = null;
		if (i >= 0 && i < 10)
			retStr = "0" + Integer.toString(i);
		else if (i >= 10 && i <= 60) {
			retStr = "" + i;
		} else {
			retStr = "00";
		}
		return retStr;
	}
}
