package com.general.compass;




import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.general.waps.Advertisement;
import com.general.waps.QuitPopAd;
import com.umeng.analytics.MobclickAgent;
import com.umeng.onlineconfig.OnlineConfigAgent;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.app.Activity;
import android.content.Context;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.AccelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements SensorEventListener
{

	private SensorManager sensorManager;
	private Sensor mOrientation;

	private float directionRotate;// 目标方向旋转浮点数
	private float dRotate;// 当前方向浮点数
	protected final Handler handler = new Handler();
	private AccelerateInterpolator aInterpolator;// 动画从开始到结束，变化率是一个加速的过程,就是一个动画速率
	private LocationManager lManager;// 位置管理对象
	private String stringProvider;// 位置提供者名称，GPS设备还是网络
	private float fAngle;//记录旋转角度，用于顶部显示方向的判断
	private int angleInt = 0;
	private boolean mStopDrawing;// 是否停止指南针旋转的标志位
	private final float MAX_ROATE_DEGREE = 1.0f;// 最多旋转一周，即360°
	LinearLayout layoutDirection;// 显示顶部方向
	LinearLayout layoutAngle;// 显示顶部角度
	LinearLayout posDirection;//显示正方向（东西南北）
	TextView textviewLocation;// 显示位置的view
	TextView locationShow;//经纬度显示view
	LayoutParams lParams;
	CompassView cView;// 指南针view
	private String loc = null; // 保存定位信息
	public LocationClient mLocationClient = null;
	public BDLocationListener myListener = new MyLocationListener();
	boolean advState = false;
	private int checkCount = 0;// 检查广告是否开启次数
	private final int CC_AD_MAXC = 3; // 检查次数
	private Handler mHandler;
	
	// 这个是更新指南针旋转的线程，handler的灵活使用，每10毫秒检测方向变化值，对应更新指南针旋转
	protected Runnable mCompassViewUpdater = new Runnable()
	{
		@Override
		public void run()
		{
			if (cView != null && !mStopDrawing)
			{
				if (dRotate != directionRotate)
				{
					// calculate the short routine
					float to = directionRotate;
					if (to - dRotate > 180)
					{
						to -= 360;
					} else if (to - dRotate < -180)
					{
						to += 360;
					}

					// limit the max speed to MAX_ROTATE_DEGREE
					float distance = to - dRotate;
					if (Math.abs(distance) > MAX_ROATE_DEGREE)
					{
						distance = distance > 0 ? MAX_ROATE_DEGREE : (-1.0f * MAX_ROATE_DEGREE);
					}

					// need to slow down if the distance is short
					dRotate = Reorientation(dRotate + ((to - dRotate) * aInterpolator.getInterpolation(Math.abs(distance) > MAX_ROATE_DEGREE ? 0.4f : 0.3f)));// 用了一个加速动画去旋转图片，很细致
					cView.updateDirection(dRotate);// 更新指南针旋转
				}
				directionData();// 显示方向和方向偏角
				handler.postDelayed(mCompassViewUpdater, 10);// 10毫米后重新执行自己
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		lParams = new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
		initData();// 初始化传感器
		
		mLocationClient = new LocationClient(getApplicationContext()); // 声明LocationClient类
		mLocationClient.registerLocationListener(myListener); // 注册监听函数
		LocationClientOption option = new LocationClientOption();
		option.setOpenGps(true);// 打开GPS
		option.setAddrType("all");// 返回的定位结果包含地址信息
		option.setCoorType("bd09ll");// 返回的定位结果是百度经纬度,默认值gcj02
		option.setScanSpan(3000);// 设置发起定位请求的间隔时间为3000ms
		option.disableCache(false);// 禁止启用缓存定位
		option.setPriority(LocationClientOption.NetWorkFirst);// 网络定位优先
		mLocationClient.setLocOption(option);// 使用设置
		mLocationClient.start();// 开启定位SDK
		mLocationClient.requestLocation();// 开始请求位置
		Looper looper = Looper.myLooper();
		mHandler = new MessageHandler(looper);
		showAd();
	}
	private void showAd()
	{
		// 友盟在线参数
		OnlineConfigAgent.getInstance().updateOnlineConfig(this);
		String showAdc = OnlineConfigAgent.getInstance().getConfigParams(this, "show_adv"); // 是否显示广告
		// 广告 ------------
		if("0".equals(showAdc))
		{
			advState = true;
		}
		if(advState)
		{
			// 悬浮窗 开关
			String floatValue = OnlineConfigAgent.getInstance().getConfigParams(this, "float_view");// 是否显示积分墙
			boolean floatState = false;
			if ("0".equals(floatValue))
			{
				floatState = true;
			}
			Advertisement.getInstance(this).showAdSelectad(true, floatState, true);
		}
		if(!advState && checkCount < CC_AD_MAXC)
		{
			// 广告未加载再次检查
			checkCount++;
			
			new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					try
					{
						Thread.sleep(2000);
					} catch (Exception e)
					{}
					 Message message = Message.obtain();
					 mHandler.sendMessage(message);
				}
			}).start();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void onSensorChanged(SensorEvent event)
	{
		// x表示手机指向的方位，0表示北,90表示东，180表示南，270表示西
		fAngle = event.values[0];
		directionRotate = Reorientation((fAngle * -1.0f));
	}

	@Override
	protected void onResume()// 我们在onResume方法中创建一个方向传感器，并向系统注册监听器
	{                        // 在恢复的生命周期里判断、启动位置更新服务和传感器服务
		super.onResume();
		MobclickAgent.onResume(this);
		if (stringProvider != null)
		{
			updateLocation(lManager.getLastKnownLocation(stringProvider));
			lManager.requestLocationUpdates(stringProvider, 2000, 10, mLocationListener);// 2秒或者距离变化10米时更新一次地理位置
		} else
		{
			textviewLocation.setText(R.string.cannot_get_location);
		}
		if (sensorManager != null)
		{
			sensorManager.registerListener(this, mOrientation, SensorManager.SENSOR_DELAY_NORMAL);
		} else
		{
			Toast.makeText(this, R.string.cannot_get_sensor, Toast.LENGTH_SHORT).show();
		}
		mStopDrawing = false;
		handler.postDelayed(mCompassViewUpdater, 10);
	}

	/**
	 * 最后我们在onPause()中注销所有传感器的监听，释放方向感应器资源!
	 */
	@Override
	protected void onPause()
	{
		super.onPause();
		MobclickAgent.onPause(this);
		mStopDrawing = true;
		lManager.removeUpdates(mLocationListener);
		// 注销所有传感器的监听
		sensorManager.unregisterListener(this);
	}

	/**
	 * 初始化数据
	 */
	public void initData()
	{
		dRotate = 0.0f;
		// 初始化传感器
		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mOrientation = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);

		aInterpolator = new AccelerateInterpolator();// 实例化加速动画对象

		lManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		Criteria criteria = new Criteria();// 条件对象，即指定条件过滤获得LocationProvider
		criteria.setAccuracy(Criteria.ACCURACY_FINE);// 较高精度
		criteria.setAltitudeRequired(false);// 是否需要高度信息
		criteria.setBearingRequired(false);// 是否需要方向信息
		criteria.setCostAllowed(true);// 是否产生费用
		criteria.setPowerRequirement(Criteria.POWER_LOW);// 设置低电耗
		stringProvider = lManager.getBestProvider(criteria, true);// 获取条件最好的Provider

		// 初始化view
		mStopDrawing = true;
		cView = (CompassView) findViewById(R.id.compass_pointer);
		layoutDirection = (LinearLayout) findViewById(R.id.layout_direction);
		layoutAngle = (LinearLayout) findViewById(R.id.layout_angle);
		textviewLocation = (TextView) findViewById(R.id.textview_location);
		locationShow = (TextView) findViewById(R.id.location_show);
		posDirection = (LinearLayout) findViewById(R.id.pos_direction);
		
		
	}

	/**
	 * 调整方向传感器获取的值
	 * @param degree 度数
	 * @return
	 */
	private float Reorientation(float degree)
	{
		return (degree + 720) % 360;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		if (keyCode == KeyEvent.KEYCODE_BACK)
		{
			// 调用退屏广告
			if(advState)
			{
				QuitPopAd.getInstance().show(this);
			}
			else
			{
				return super.onKeyDown(keyCode, event);
			}
		}
		return false;
	}
	
	

	// 位置信息更新监听
	LocationListener mLocationListener = new LocationListener()
	{

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras)
		{
			if (status != LocationProvider.OUT_OF_SERVICE)
			{
				updateLocation(lManager.getLastKnownLocation(stringProvider));
			} 
		}

		@Override
		public void onProviderEnabled(String provider)
		{
		}

		@Override
		public void onProviderDisabled(String provider)
		{
		}

		@Override
		public void onLocationChanged(Location location)
		{
			updateLocation(location);// 更新位置
		}
	};

	/**
	 * 更新位置显示
	 * @param location
	 */
	private void updateLocation(Location location)
	{
		if (location == null)
		{
		} else
		{
			StringBuilder sb = new StringBuilder();
			double latitude = location.getLatitude();
			double longitude = location.getLongitude();

			if (latitude >= 0.0f)
			{
				sb.append(getString(R.string.location_north, getLocationString(latitude)));
			} else
			{
				sb.append(getString(R.string.location_south, getLocationString(-1.0 * latitude)));
			}

			sb.append("    ");

			if (longitude >= 0.0f)
			{
				sb.append(getString(R.string.location_east, getLocationString(longitude)));
			} else
			{
				sb.append(getString(R.string.location_west, getLocationString(-1.0 * longitude)));
			}
			locationShow.setText(sb.toString());// 显示经纬度，其实还可以作反向编译，显示具体地址
		}
	}

	/**
	 * 把经纬度转换成度分秒显示
	 * @param input
	 * @return
	 */
	private String getLocationString(double input)
	{
		int du = (int) input;
		int fen = (((int) ((input - du) * 3600))) / 60;
		int miao = (((int) ((input - du) * 3600))) % 60;
		return String.valueOf(du) + "°" + String.valueOf(fen) + "′" + String.valueOf(miao) + "″";
	}
	
	/**
	 * 显示方向
	 */
	private void directionData()
	{
		//0北方
		//90东方
		//180南方
		//270西方
		
		
		// 先移除layout中所有的view
		layoutDirection.removeAllViews();
		layoutAngle.removeAllViews();
		posDirection.removeAllViews();

		// 下面是根据layoutDirection，作方向名称图片的处理
		ImageView east = null;//东
		ImageView west = null;//西
		ImageView south = null;//南
		ImageView north = null;//北
		ImageView eastSouth = null;//东偏南
		ImageView eastNorth = null;//东偏北
		ImageView westSouth = null;//西偏南
		ImageView westNorth =null;//西偏北
		//ImageView error = null;//错误
		
		if(((fAngle > 5.0f) && (fAngle < 85.0f)) || ((fAngle > -355.0f) && (fAngle < -275.0f)))
		{
			//显示东偏北xx度。
			//显示东偏北
			eastNorth = new ImageView(this);
			eastNorth.setImageResource(R.drawable.east_north);
			eastNorth.setLayoutParams(lParams);
			numHandle(1);
		}
		else if(((fAngle > 95.0f) && (fAngle < 175.0f)) || ((fAngle > -265.0f) && (fAngle < -185.0f)))
		{
			//显示东偏南xx度。
			//显示东偏南
			eastSouth = new ImageView(this);
			eastSouth.setImageResource(R.drawable.east_south);
			eastSouth.setLayoutParams(lParams);
			numHandle(2);
		}
		else if(((fAngle > 185.0f) && (fAngle < 265.0f)) || ((fAngle > -185.0f) && (fAngle < -95.0f)))
		{
			//显示西偏南xx度。
			//显示西偏南
			westSouth = new ImageView(this);
			westSouth.setImageResource(R.drawable.west_south);
			westSouth.setLayoutParams(lParams);
			numHandle(3);
		}
		else if(((fAngle > 275.0f) && (fAngle < 355.0f)) || ((fAngle > -85.0f) && (fAngle < -5.0f)))
		{
			//显示西偏北xx度。
			//显示西偏北
			westNorth = new ImageView(this);
			westNorth.setImageResource(R.drawable.west_north);
			westNorth.setLayoutParams(lParams);
			numHandle(4);
		}
		else if((fAngle >= -5.0f) && (fAngle <= 5.0f))
		{
			//显示北方
			north = new ImageView(this);
			north.setImageResource(R.drawable.north);
			north.setLayoutParams(lParams);
		}
		else if(((fAngle >= 85.0f) && (fAngle <= 95.0f)) || ((fAngle >= -275) && (fAngle <= -265.0f)))
		{
			//显示东方
			east = new ImageView(this);
			east.setImageResource(R.drawable.east);
			east.setLayoutParams(lParams);
		}
		else if(((fAngle >= 175.0f) && (fAngle <= 185.0f)) || ((fAngle >= -185.0f) && (fAngle <= -175.0f)))
		{
			//显示南方
			south = new ImageView(this);
			south.setImageResource(R.drawable.south);
			south.setLayoutParams(lParams);
		}
		else if(((fAngle >= 265.0f) && (fAngle <= 275.0f)) || ((fAngle >= -95.0f) && (fAngle <= -85.0f)))
		{
			//显示西方
			west = new ImageView(this);
			west.setImageResource(R.drawable.west);
			west.setLayoutParams(lParams);
		}
		else
		{
			//方向获取失败
			//error = new ImageView(this);
			//error.setImageResource(R.drawable.error);
			//error.setLayoutParams(lParams);
		}
		
		if(east != null)//显示方向东
		{
			posDirection.addView(east);
		}
		else if(west != null)//显示方向西
		{
			posDirection.addView(west);
		}
		else if(south != null)//显示方向南
		{
			posDirection.addView(south);
		}
		else if(north != null)//显示方向北
		{
			posDirection.addView(north);
		}
		else if(eastSouth != null)//显示方向东偏南
		{
			layoutDirection.addView(eastSouth);
		}
		else if(eastNorth != null)//显示方向东偏北
		{
			layoutDirection.addView(eastNorth);
		}
		else if(westSouth != null)//显示方向西偏南
		{
			layoutDirection.addView(westSouth);
		}
		else if(westNorth != null)//显示方向西偏北
		{
			layoutDirection.addView(westNorth);
		}else//显示方向失败
		{
			//layoutDirection.addView(error);
		}
	}
	
	/**
	 * 方向度数
	 * @param num
	 */
	private void numHandle(int num)
	{
		angleInt = (int) fAngle;
		if(fAngle > 0)
		{
			switch (num)
			{
			case 1://东偏北
				angleInt = 90 - angleInt;
				break;
			case 2://东偏南
				angleInt = angleInt - 90;
				break;
			case 3://西偏南
				angleInt = 270 - angleInt;
				break;
			case 4://西偏北
				angleInt = angleInt - 270;
				break;
			default:
				break;
			}
		}
		else
		{
			switch (num)
			{
			case 1://东偏北
				angleInt = Math.abs(angleInt) - 270;
				break;
			case 2://东偏南
				angleInt = 270 - Math.abs(angleInt);
				break;
			case 3://西偏南
				angleInt = Math.abs(angleInt) - 90;
				break;
			case 4://西偏北
				angleInt = 90 - Math.abs(angleInt);
				break;
			default:
				break;
			}
		}
		//显示方向度数的十位
		layoutAngle.addView(getNumberImage(angleInt / 10));
		angleInt = angleInt % 10;
		//显示方向度数的个位
		layoutAngle.addView(getNumberImage(angleInt));
		// 下面是增加一个°的图片
		ImageView degreeImageView = new ImageView(this);
		degreeImageView.setImageResource(R.drawable.degree);
		degreeImageView.setLayoutParams(lParams);
		layoutAngle.addView(degreeImageView);
		
	}
	
	/**
	 * 获取方向度数对应的图片，返回ImageView
	 * @param number
	 * @return
	 */
	private ImageView getNumberImage(int number)
	{
		ImageView image = new ImageView(this);
		LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		switch (number)
		{
		case 0:
			image.setImageResource(R.drawable.number_0);
			break;
		case 1:
			image.setImageResource(R.drawable.number_1);
			break;
		case 2:
			image.setImageResource(R.drawable.number_2);
			break;
		case 3:
			image.setImageResource(R.drawable.number_3);
			break;
		case 4:
			image.setImageResource(R.drawable.number_4);
			break;
		case 5:
			image.setImageResource(R.drawable.number_5);
			break;
		case 6:
			image.setImageResource(R.drawable.number_6);
			break;
		case 7:
			image.setImageResource(R.drawable.number_7);
			break;
		case 8:
			image.setImageResource(R.drawable.number_8);
			break;
		case 9:
			image.setImageResource(R.drawable.number_9);
			break;
		}
		image.setLayoutParams(lp);
		return image;
	}
	
	@Override
	public void onDestroy()
	{
		stopListener();//停止监听
		super.onDestroy();
		Advertisement.getInstance(this).close();
	}

	/*
	 * 
	 */
	public class MyLocationListener implements BDLocationListener
	{
		@Override
		public void onReceiveLocation(BDLocation location)
		{
			if (location != null)
			{
				StringBuffer sb = new StringBuffer(128);// 接受服务返回的缓冲区
				
				//sb.append(location.getCity());// 获得城市
				sb.append(location.getAddrStr());
				loc = sb.toString().trim();
				textviewLocation.setText(loc);
			} else
			{
				textviewLocation.setText(getText(R.string.cannot_get_location).toString());
				return;
			}
		}

		@Override
		public void onReceivePoi(BDLocation arg0)
		{
			// TODO Auto-generated method stub
		}
	}
	
	/**
	 * 停止，减少资源消耗
	 */
	private void stopListener()
	{
		if (mLocationClient != null && mLocationClient.isStarted())
		{
			mLocationClient.stop();// 关闭定位SDK
			mLocationClient = null;
		}
	}
	
	 //子类化一个Handler
    class MessageHandler extends Handler {
        public MessageHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
        	showAd();
        }
    }
}
