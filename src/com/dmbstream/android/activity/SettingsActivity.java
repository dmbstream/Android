package com.dmbstream.android.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.util.Log;

import java.io.File;
import org.json.JSONObject;

import com.dmbstream.android.R;
import com.dmbstream.android.service.DownloadServiceImpl;
import com.dmbstream.android.service.IDownloadService;
import com.dmbstream.android.util.AnalyticsUtil;
import com.dmbstream.android.util.Constants;
import com.dmbstream.android.util.FileUtil;
import com.dmbstream.android.util.HttpConnection;
import com.dmbstream.android.util.Util;
import com.dmbstream.api.ApiConstants;

public class SettingsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = SettingsActivity.class.getSimpleName();
    private ListPreference songQualityWifi;
    private ListPreference songQualityMobile;
    private ListPreference cacheSize;
    private EditTextPreference cacheLocation;
    private ListPreference preloadCount;

    @Override
    public void onCreate(Bundle savedInstanceState) {
    	Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);

        songQualityWifi = (ListPreference) findPreference(Constants.PREFERENCES_KEY_SONG_QUALITY_WIFI);
        songQualityMobile = (ListPreference) findPreference(Constants.PREFERENCES_KEY_SONG_QUALITY_MOBILE);
        cacheSize = (ListPreference) findPreference(Constants.PREFERENCES_KEY_CACHE_SIZE);
        cacheLocation = (EditTextPreference) findPreference(Constants.PREFERENCES_KEY_CACHE_LOCATION);
        preloadCount = (ListPreference) findPreference(Constants.PREFERENCES_KEY_PRELOAD_COUNT);

//        findPreference("clearSearchHistory").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
//            @Override
//            public boolean onPreferenceClick(Preference preference) {
//                SearchRecentSuggestions suggestions = new SearchRecentSuggestions(SettingsActivity.this, SearchSuggestionProvider.AUTHORITY, SearchSuggestionProvider.MODE);
//                suggestions.clearHistory();
//                Util.toast(SettingsActivity.this, R.string.settings_search_history_cleared);
//                return false;
//            }
//        });
        
        boolean isDonor = Util.getIsDonor(SettingsActivity.this);
        if (!isDonor) {
	        // If they're not a donor, disable the ability to change preloadCount
			try {
				Log.d(TAG, "Getting user information");
				JSONObject user = HttpConnection.getAsJson(ApiConstants.instance().baseUrl("api/users/current"), Util.getUserToken(this));
				isDonor = user.getBoolean("is_donor");
				preloadCount.setEnabled(isDonor);
				if (!isDonor) {
					Util.setIsDonor(this, isDonor);
				}
			} catch (Exception ex) {
				Log.w(TAG, "Unable to check user donor status");
			}
        }
        SharedPreferences prefs = Util.getPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);

        update();
    }
    @Override
    protected void onResume() {
    	super.onResume();
    	AnalyticsUtil.trackPageView(this, "/Settings");
    }

    @Override
    protected void onDestroy() {
    	Log.d(TAG, "onDestroy");
        super.onDestroy();

        SharedPreferences prefs = Util.getPreferences(this);
        prefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.d(TAG, "Preference changed: " + key);
        update();
        
        CharSequence value = "";
        if (Constants.PREFERENCES_KEY_SONG_QUALITY_WIFI.equals(key)) {
        	value = songQualityWifi.getEntry();
        }
        else if (Constants.PREFERENCES_KEY_SONG_QUALITY_MOBILE.equals(key)) {
        	value = songQualityMobile.getEntry();
        }
        else if (Constants.PREFERENCES_KEY_CACHE_SIZE.equals(key)) {
        	value = cacheSize.getEntry();
        }
        else if (Constants.PREFERENCES_KEY_CACHE_LOCATION.equals(key)) {
        	value = sharedPreferences.getString(key, "");
            setCacheLocation(sharedPreferences.getString(key, ""));
        }
        else if (Constants.PREFERENCES_KEY_PRELOAD_COUNT.equals(key)) {
        	value = preloadCount.getEntry();
        }
        else if (Constants.PREFERENCES_KEY_MEDIA_BUTTONS.equals(key)) {
        	value = sharedPreferences.getBoolean(key, true) + "";
            setMediaButtonsEnabled(sharedPreferences.getBoolean(key, true));
        }
		AnalyticsUtil.trackEvent(SettingsActivity.this, "Settings", key, value.toString(), 0);        
    }

    private void update() {
    	Log.d(TAG, "update");
    	
    	songQualityWifi.setSummary(songQualityWifi.getEntry());
    	songQualityMobile.setSummary(songQualityMobile.getEntry());
        cacheSize.setSummary(cacheSize.getEntry());
        cacheLocation.setSummary(cacheLocation.getText());
        preloadCount.setSummary(preloadCount.getEntry());
    }

    private void setMediaButtonsEnabled(boolean enabled) {
    	Log.d(TAG, "setMediaButtonsEnabled");
        if (enabled) {
            Util.registerMediaButtonEventReceiver(this);
        } else {
            Util.unregisterMediaButtonEventReceiver(this);
        }
    }

    private void setCacheLocation(String path) {
    	Log.d(TAG, "setCacheLocation");
        File dir = new File(path);
        if (!FileUtil.ensureDirectoryExistsAndIsReadWritable(dir)) {
            Util.toast(this, R.string.settings_cache_location_error, false);

            // Reset it to the default.
            String defaultPath = FileUtil.getDefaultMusicDirectory().getPath();
            if (!defaultPath.equals(path)) {
                SharedPreferences prefs = Util.getPreferences(this);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(Constants.PREFERENCES_KEY_CACHE_LOCATION, defaultPath);
                editor.commit();
                cacheLocation.setSummary(defaultPath);
                cacheLocation.setText(defaultPath);
            }

            // Clear download queue.
            IDownloadService downloadService = DownloadServiceImpl.getInstance();
            downloadService.clear();
        } else {
        	FileUtil.hideMedia(dir);
        }
    }
}