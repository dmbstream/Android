package com.dmbstream.android;

import android.app.Application;
import android.content.pm.PackageInfo;
import android.util.Log;

import com.bugsense.trace.BugSenseHandler;
import com.dmbstream.android.util.FileUtil;
import com.dmbstream.api.ApiConstants;

/**
 * Handles application startup code for any activity
 */
public class DmbstreamApplication extends Application {
	private static final String TAG = DmbstreamApplication.class.getSimpleName();

	@Override
	public void onCreate() {
		super.onCreate();

		try {
			String bugsenseKey = FileUtil.readFile(getResources(), "com.dmbstream.android:raw/bugsense_key").toString();
			BugSenseHandler.setup(this, bugsenseKey);			
		} catch (Exception e) {
			Log.e(TAG, "Error setting up bugsense", e);
		}
		
		String key = "";
		String version = "";
		try {
			key = FileUtil.readFile(getResources(), "com.dmbstream.android:raw/api_key").toString();
			PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
			version = info.packageName + " v" + info.versionName;
		} catch (Exception e) {
			Log.e(TAG, "Error reading application api key", e);
		}
		ApiConstants.createInstance(key, version);
		
		FileUtil.hideMedia(FileUtil.getSubsonicDirectory());
	}

	@Override
	public void onTerminate() {
		super.onTerminate();
	}
}
