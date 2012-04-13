// Copyright 2009 Google Inc.
// Copyright 2011 NPR
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

package com.dmbstream.android.activity;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;

import com.dmbstream.api.ApiConstants;
import com.dmbstream.api.UrlWithParameters;
import com.dmbstream.android.R;
import com.dmbstream.android.util.AnalyticsUtil;
import com.dmbstream.android.util.Constants;

import org.json.JSONException;
import org.json.JSONObject;

public class SearchResultsActivity extends ConcertListActivity implements
		View.OnKeyListener {

	private static final String TAG = SearchResultsActivity.class.getSimpleName();

	private String query;
	private EditText searchBox;
	private InputMethodManager inputMethodManager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Intent intent = getIntent();

		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			query = intent.getStringExtra(SearchManager.QUERY);
		} else {
			query = intent.getStringExtra(Constants.EXTRA_QUERY_TERM);
			Log.d(TAG, "Query from other intent: " + query);
		}

		// After setting up variables so that they can be accessed in getApiUrl
		// which is called at the end of super.onCreate
		super.onCreate(savedInstanceState);

		// Push the search box into the layout group below the logo bar and above
		// the 'search results' title bar
		ViewGroup container = (ViewGroup) findViewById(R.id.TitleGroup);
		container.addView(ViewGroup.inflate(this, R.layout.partial_search_box, null), 0);
		searchBox = (EditText) findViewById(R.id.SearchText);
		searchBox.setText(query);
		searchBox.setOnKeyListener(this);
		ImageButton searchButton = (ImageButton) findViewById(R.id.search_go_button);
		searchButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				AnalyticsUtil.trackEvent(SearchResultsActivity.this, "SearchResults", "Click", "Search", 0);
				search();
			}
		});

		inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		if (inputMethodManager != null) {
			inputMethodManager.hideSoftInputFromWindow(
					searchBox.getWindowToken(),
					InputMethodManager.HIDE_NOT_ALWAYS);
		}
	}

	@Override
	protected UrlWithParameters getApiUrl() {
		UrlWithParameters url = new UrlWithParameters();
		url.url = ApiConstants.instance().baseUrl("api/concerts/search");
		url.params = new JSONObject();
		try {
			url.params.put("query", query);
		} catch (JSONException e) {
			Log.e(TAG, "Error adding query to search request", e);
		}
		
		return url;
	}

	@Override
	public boolean onKey(View view, int i, KeyEvent keyEvent) {
		switch (keyEvent.getKeyCode()) {
		case KeyEvent.KEYCODE_SEARCH:
		case KeyEvent.KEYCODE_ENTER:
			AnalyticsUtil.trackEvent(SearchResultsActivity.this, "SearchResults", "Enter", "Query", 0);
			search();
			return true;
		}
		return false;
	}

	private void search() {
		if (inputMethodManager != null) {
			inputMethodManager.hideSoftInputFromWindow(
					searchBox.getWindowToken(),
					InputMethodManager.HIDE_NOT_ALWAYS);
		}
		query = searchBox.getText().toString();
		if (query.length() > 0) {
			refresh();
		}
	}

	@Override
	public CharSequence getMainTitle() {
		return getString(R.string.msg_search_results_title);
	}
	@Override
	protected String getAnalyticsPageName() {
		return "/Search/Results";
	}
}
