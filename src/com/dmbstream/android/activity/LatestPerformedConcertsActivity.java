package com.dmbstream.android.activity;

import android.os.Bundle;
import android.util.Log;

import com.dmbstream.android.R;
import com.dmbstream.api.ApiConstants;
import com.dmbstream.api.UrlWithParameters;

import org.json.JSONException;
import org.json.JSONObject;

public class LatestPerformedConcertsActivity extends ConcertListActivity {

	private static final String TAG = LatestPerformedConcertsActivity.class.getSimpleName();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// After setting up variables so that they can be accessed in getApiUrl
		// which is called at the end of super.onCreate
		super.onCreate(savedInstanceState);
	}

	@Override
	protected UrlWithParameters getApiUrl() {
		UrlWithParameters url = new UrlWithParameters();
		url.url = ApiConstants.instance().baseUrl("api/concerts");
		url.params = new JSONObject();
		try {
			url.params.put("sortDesc", "ConcertDate");
		} catch (JSONException e) {
			Log.e(TAG, "Error adding query to latest performed concerts request", e);
		}
		
		return url;
	}

	@Override
	public CharSequence getMainTitle() {
		return getString(R.string.msg_latest_performed_concerts_title);
	}
	@Override
	protected String getAnalyticsPageName() {
		return "/Concerts/LatestPerformed";
	}
}
