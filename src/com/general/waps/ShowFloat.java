package com.general.waps;


import com.general.compass.R;
import com.general.flashlight.wp.AppConnect;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.widget.ImageView;

public class ShowFloat
{
	WindowManager wm = null;
	WindowManager.LayoutParams wmParams = null;
	View view;
	ImageView img1;
	private float mTouchStartX;
	private float mTouchStartY;
	private float x;
	private float y;
	int state;
	private float StartX;
	private float StartY;
	int delaytime = 1000;
	private Context context;

	public ShowFloat(Context con)
	{
		context = con;
	}

	public void showFloat()
	{
		view = LayoutInflater.from(context).inflate(R.layout.floating, null);
		img1 = (ImageView) view.findViewById(R.id.img1);
		createView();
		handler.postDelayed(task, delaytime);
	}

	private void createView()
	{
		SharedPreferences shared = context.getSharedPreferences("float_flag", Activity.MODE_PRIVATE);
		SharedPreferences.Editor editor = shared.edit();
		editor.putInt("float", 1);
		editor.commit();
		// 获取WindowManager
		wm = (WindowManager) context.getApplicationContext().getSystemService("window");
		// 设置LayoutParams(全局变量）相关参数
		wmParams = ((MyApplication) ((Activity) context).getApplication()).getMywmParams();
		wmParams.type = 2002;
		wmParams.flags |= 8;
		wmParams.gravity = Gravity.LEFT | Gravity.TOP; // 调整悬浮窗口至左上角
		// 以屏幕左上角为原点，设置x、y初始值
		wmParams.x = 0;
		wmParams.y = 0;
		// 设置悬浮窗口长宽数据
		wmParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
		wmParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
		wmParams.format = 1;

		wm.addView(view, wmParams);

		view.setOnTouchListener(new OnTouchListener()
		{
			public boolean onTouch(View v, MotionEvent event)
			{
				// 获取相对屏幕的坐标，即以屏幕左上角为原点
				x = event.getRawX();
				y = event.getRawY() - 25; // 25是系统状态栏的高度
				switch (event.getAction())
				{
				case MotionEvent.ACTION_DOWN:
					state = MotionEvent.ACTION_DOWN;
					StartX = x;
					StartY = y;
					// 获取相对View的坐标，即以此View左上角为原点
					mTouchStartX = event.getX();
					mTouchStartY = event.getY();
					break;
				case MotionEvent.ACTION_MOVE:
					state = MotionEvent.ACTION_MOVE;
					updateViewPosition();
					break;

				case MotionEvent.ACTION_UP:
					state = MotionEvent.ACTION_UP;

					updateViewPosition();
					showImg();
					mTouchStartX = mTouchStartY = 0;
					break;
				}
				return true;
			}
		});

	}

	public void showImg()
	{
		if (Math.abs(x - StartX) < 1.5 && Math.abs(y - StartY) < 1.5)//点击事件
		{
			AppConnect.getInstance(context).showOffers(context);// 显示推荐列表（综合）
		}
	}

	private Handler handler = new Handler();
	private Runnable task = new Runnable()
	{
		public void run()
		{
			// TODO Auto-generated method stub
			handler.postDelayed(this, delaytime);
			wm.updateViewLayout(view, wmParams);
		}
	};

	private void updateViewPosition()
	{
		// 更新浮动窗口位置参数
		wmParams.x = (int) (x - mTouchStartX);
		wmParams.y = (int) (y - mTouchStartY);
		wm.updateViewLayout(view, wmParams);
	}

	/**
	 * 销毁悬浮窗
	 */
	public void onShowFloatDestroy()
	{
		handler.removeCallbacks(task);
		wm.removeView(view);
	}
}
