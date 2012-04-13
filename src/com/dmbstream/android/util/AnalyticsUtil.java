package com.dmbstream.android.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.dmbstream.android.R;
import com.google.android.apps.analytics.GoogleAnalyticsTracker;

public final class AnalyticsUtil {

	private static final String TAG = AnalyticsUtil.class.getSimpleName();

	/**
	 * Custom variables scope levels.
	 */
	public static final int SCOPE_VISITOR_LEVEL = 1;
	public static final int SCOPE_SESSION_LEVEL = 2;
	public static final int SCOPE_PAGE_LEVEL = 3;

	/**
	 * Custom variables slot (1-5).
	 */
	private static final int CUSTOM_VAR_INDEX_DEVICE = 1;
	private static final int CUSTOM_VAR_INDEX_APP_VERSION = 2;
	private static final int CUSTOM_VAR_INDEX_ANDROID_VERSION = 3;
	private static final int CUSTOM_VAR_INDEX_SDK_VERSION = 4;
	private static final int CUSTOM_VAR_INDEX_OPERATOR = 5;

	/**
	 * The instance.
	 */
	private static AnalyticsUtil instance;

	/**
	 * @return the instance
	 */
	public static AnalyticsUtil getInstance() {
		Log.v(TAG, "getInstance()");
		if (instance == null) {
			instance = new AnalyticsUtil();
		}
		return instance;
	}

	/**
	 * The Google Analytics tracker.
	 */
	private static GoogleAnalyticsTracker tracker;

	/**
	 * True if the tracker is started.
	 */
	private static boolean trackerStarted = false;

	/**
	 * @param context 	the context
	 * @return the Google Analytics tracker
	 */
	private static GoogleAnalyticsTracker getGoogleAnalyticsTracker(Context context) {
		Log.v(TAG, "getGoogleAnalyticsTracker()");
		if (tracker == null) {
			Log.v(TAG, "Initializing the Google Analytics tracker...");
			tracker = GoogleAnalyticsTracker.getInstance();
			// initTrackerWithUserData(context);
			Log.v(TAG, "Initializing the Google Analytics tracker... DONE");
		}
		if (!trackerStarted) {
			Log.v(TAG, "Starting the Google Analytics tracker...");
			tracker.startNewSession(context.getString(R.string.google_analytics_id), 5, context);
			trackerStarted = true;
			Log.v(TAG, "Starting the Google Analytics tracker... DONE");
		}
		return tracker;
	}

	/**
	 * Initialize the Google Analytics tracker with user device properties.
	 * 
	 * @param context 	the context
	 */
	private static void initTrackerWithUserData(Context context) {
		// only 5 Custom Variables allowed!

		// 1 - Application version
		try {
			PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
			String appVersion = packageInfo.versionName;
			Log.v(TAG, "app_version: " + appVersion);
			getGoogleAnalyticsTracker(context).setCustomVar(
					CUSTOM_VAR_INDEX_APP_VERSION, "version", appVersion,
					SCOPE_VISITOR_LEVEL);
			// getTracker(context).setProductVersion("MonTransit", appVersion);
		} catch (Exception e) {
		}

		// 2 - Android version
		String androidVersion = Build.VERSION.RELEASE;
		Log.v(TAG, "os_rel: '" + androidVersion + "'.");
		getGoogleAnalyticsTracker(context).setCustomVar(
				CUSTOM_VAR_INDEX_ANDROID_VERSION, "android", androidVersion,
				SCOPE_VISITOR_LEVEL);

		// 3 - Android SDK
		String sdk = Build.VERSION.SDK;
		Log.v(TAG, "sdk_version: '" + sdk + "'.");
		getGoogleAnalyticsTracker(context).setCustomVar(
				CUSTOM_VAR_INDEX_SDK_VERSION, "sdk", sdk, SCOPE_VISITOR_LEVEL);

		// 4 - Device
		String device = Build.MODEL;
		Log.v(TAG, "device: '" + device + "'.");
		getGoogleAnalyticsTracker(context).setCustomVar(
				CUSTOM_VAR_INDEX_DEVICE, "device", device, SCOPE_VISITOR_LEVEL);

		// 5 - Network Operator
		try {
			TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
			String operator = telephonyManager.getNetworkOperatorName();
			Log.v(TAG, "operator: '" + operator + "'.");
			getGoogleAnalyticsTracker(context).setCustomVar(
					CUSTOM_VAR_INDEX_OPERATOR, "operator", operator,
					SCOPE_VISITOR_LEVEL);
		} catch (Exception e) {
		}
		// already provided by Analytics:
		// user language + country
		// service provider
	}

	/**
	 * Track an event.
	 * 
	 * @param context 	the context
	 * @param category 	the event category
	 * @param action 	the event action
	 * @param label 	the event label
	 * @param value 	the event value
	 */
	public static void trackEvent(Context context, String category, String action, String label, int value) {
		Log.v(TAG, "trackEvent()");
		if (Constants.EnableAnalytics) {
			initTrackerWithUserData(context);
			getGoogleAnalyticsTracker(context).trackEvent(category, action, label, value);
		}
	}

	/**
	 * Track a page view.
	 * 
	 * @param context 	the context
	 * @param page 		the viewed page.
	 */
	public static void trackPageView(Context context, String page) {
		Log.v(TAG, "trackPageView(" + page + ")");
		if (Constants.EnableAnalytics) {
			try {
				initTrackerWithUserData(context);
				getGoogleAnalyticsTracker(context).trackPageView(page);
			} catch (Throwable t) {
				Log.w(TAG, "Error while tracing view.", t);
			}
		}
	}

	/**
	 * Dispatch the Google Analytics tracker content.
	 * 
	 * @param context 	the context
	 */
	public static void dispatch(Context context) {
		Log.v(TAG, "dispatch()");
		if (Constants.EnableAnalytics) {
			try {
				getGoogleAnalyticsTracker(context).dispatch();
			} catch (Throwable t) {
				Log.w(TAG, "Error while dispatching analytics data.", t);
			}
		}
	}

	/**
	 * Stop the Google Analytics tracker
	 * 
	 * @param context 	the context
	 */
	public static void stop(Context context) {
		Log.v(TAG, "stop()");
		if (Constants.EnableAnalytics) {
			getGoogleAnalyticsTracker(context).stopSession();
			trackerStarted = false;
		}
	}
}
