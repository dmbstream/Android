// Copyright 2009 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.dmbstream.api;

import android.net.Uri;
import android.util.Log;

import java.util.Map;
import java.util.Map.Entry;

import com.dmbstream.android.util.Constants;

public class ApiConstants {
	private static final String TAG = ApiConstants.class.getSimpleName();
	// Main URL
	private static final String TestUrl =  Constants.IsEmulator ? "http://10.0.2.2" : "http://192.168.1.158";
	private static final String BaseUrl = Constants.IsDebug ? TestUrl : "http://dmbstream.com";
	
	// General params
	public static final String PARAM_ID = "id";
	public static final String PARAM_API_KEY = "apiKey";

	public static final String PARAM_PAGE = "page";
	public static final String PARAM_ITEMS_PER_PAGE = "ItemsPerPage";
	public static final String PARAM_RETURN_ITEMS_PER_PAGE = "items_per_page";
	public static final String PARAM_TOTAL_COUNT = "total";
	
	// Chat params
	public static final String PARAM_INCLUDE_ACTIVE_USERS = "includeActiveUsers";
	public static final String PARAM_LAST_MESSAGE_ID = "lastMessageId";
	public static final String PARAM_MAX_ITEMS = "maxItems";

	
	private final String apiKey;
	private final String appName;
	private static ApiConstants instance;

	public String baseUrl(String path) {
		return String.format("%s/%s?", BaseUrl, path);
	}
	
	public String createUrl(String path, Map<String, String> params) {
		String uri = baseUrl(path);
//		params.put(PARAM_API_KEY, this.apiKey);
//		params.put(PARAM_SC, PARAM_SC_VALUE);
		return addParams(uri, params);
	}

	public String addParams(String url, Map<String, String> params) {
		StringBuilder uri = new StringBuilder(url);
		for (Entry<String, String> param : params.entrySet()) {
			uri.append("&").append(Uri.encode(param.getKey())).append("=")
					.append(Uri.encode(param.getValue()));
		}
		Log.d(TAG, uri.toString());
		return uri.toString();
	}

	private ApiConstants(String apiKey, String appName) {
		// Force construction through static methods
		this.apiKey = apiKey;
		this.appName = appName;
	}

	public static void createInstance(String apiKey, String appName) {
		if (instance == null) {
			instance = new ApiConstants(apiKey, appName);
		}
	}

	public static ApiConstants instance() {
		return instance;
	}

	public boolean apiKeyIsValid() {
		return apiKey != null && apiKey.length() > 0;
	}
	public String getApiKey() {
		return apiKey;
	}
	public String getAppName() {
		return appName;
	}
}
