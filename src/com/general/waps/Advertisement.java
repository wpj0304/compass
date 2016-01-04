package com.general.waps;

import com.general.flashlight.wp.AppConnect;

import android.app.Activity;
import android.content.Context;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

public class Advertisement
{
	private static Context context;
	private static Advertisement advertisement;
	private ShowFloat showFloat;

	public static Advertisement getInstance(Context con)
	{
		if(null == advertisement)
		{
			context = con;
			advertisement = new Advertisement();
			init();
		}
		return advertisement;
	}
	
	/**
	 * 初始化
	 */
	private static void init()
	{
		final String APP_ID = "1b2717d6009a0aa55356cc498e924e7c"; // 应用标识
		final String APP_PID = "baidu"; // 分发渠道标识
		AppConnect.getInstance(APP_ID, APP_PID, context);// 初始化统计器，并通过代码设置APP_ID,
	}

	/**
	 * 加载全部广告，也可选择性的加载某些类型的广告。
	 * @param bannerAd 是否加载互动广告
	 * @param offerAd 是否加载积分墙广告
	 * @param popAd 是否加载插屏广告
	 */
	public void showAdSelectad(boolean bannerAd, boolean offerAd, boolean popAd)
	{
		
		// --------------互动广告--------
		if (bannerAd)
		{
			showBannerAd();
		}
		// --------------积分墙广告-------
		if(offerAd)
		{
			showOfferAd();
		}
		// --------------插屏广告-------
		if (popAd)
		{
			showPopAd();
		}
		// -------------抽屉式广告墙----------
	}

	/**
	 * 加载互动广告
	 */
	public void showBannerAd()
	{
		LinearLayout adlayout = new LinearLayout(context);
		adlayout.setGravity(Gravity.CENTER_HORIZONTAL);
		RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		AppConnect.getInstance(context).showBannerAd(context, adlayout);
		layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);// 此代码可以设置顶端或低端
		((Activity) context).addContentView(adlayout, layoutParams);
	}

	/**
	 * 加载积分墙广告
	 */
	public void showOfferAd()
	{
		showFloat = new ShowFloat(context);
		showFloat.showFloat();//显示悬浮窗
	}

	/**
	 * 加载插屏广告
	 */
	public void showPopAd()
	{
		AppConnect.getInstance(context).initPopAd(context);// 预加载插屏广告内容（仅在使用到插屏广告的情况，才需要添加）
		// 使用子线程，用于等待差评广告加载数据，等待时间10秒，
		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					Thread.sleep(10000);// 延时10，用于等待插屏广告的数据加载。
				} catch (InterruptedException e)
				{
					e.printStackTrace();
				}
				boolean hasPopAd = AppConnect.getInstance(context).hasPopAd(context);// 判断插屏广告是否已初始化完成，用于确定是否能成功调用插屏广告
				if (hasPopAd)
				{
					AppConnect.getInstance(context).showPopAd(context);// 显示插屏广告
				}
			}
		}).start();
	}
	
	/**
	 *  关闭广告
	 */
	public void close()
	{
		AppConnect.getInstance(context).close();
		if(null != showFloat)
		{
			showFloat.onShowFloatDestroy();
		}
	}
}
