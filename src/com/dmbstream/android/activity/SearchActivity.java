package com.dmbstream.android.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;

import com.dmbstream.android.R;
import com.dmbstream.android.util.AnalyticsUtil;
import com.dmbstream.android.util.Constants;

public class SearchActivity extends TitleActivity implements OnClickListener,
		View.OnKeyListener {
	private EditText searchText;
	private InputMethodManager inputMethodManager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		ViewGroup container = (ViewGroup) findViewById(R.id.Content);
		ViewGroup.inflate(this, R.layout.view_search, container);

		searchText = (EditText) findViewById(R.id.SearchText);
		searchText.setOnKeyListener(this);
		inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		// only will trigger it if no physical keyboard is open
		if (inputMethodManager != null) {
			inputMethodManager.showSoftInput(searchText,
					InputMethodManager.SHOW_IMPLICIT);
		}

		ImageButton searchButton = (ImageButton) findViewById(R.id.search_go_button);
		searchButton.setOnClickListener(this);
	}

	private void search() {
		if (inputMethodManager != null) {
			inputMethodManager.hideSoftInputFromWindow(
					searchText.getWindowToken(),
					InputMethodManager.HIDE_IMPLICIT_ONLY);
		}

		String text = searchText.getText().toString();
		if (text.length() > 0) {
			Intent i = new Intent(this, SearchResultsActivity.class);
			i.putExtra(Constants.EXTRA_QUERY_TERM, text);

			startActivityWithoutAnimation(i);
		}
	}

	@Override
	public void onClick(View v) {
		super.onClick(v);
		switch (v.getId()) {
			case R.id.search_go_button:
				AnalyticsUtil.trackEvent(SearchActivity.this, "Search", "Click", "Search", 0);
				search();
		}
	}

	@Override
	public CharSequence getMainTitle() {
		return getString(R.string.msg_search_title);
	}
	@Override
	protected String getAnalyticsPageName() {
		return "/Search";
	}

	@Override
	public boolean onKey(View view, int i, KeyEvent keyEvent) {
		switch (keyEvent.getKeyCode()) {
		case KeyEvent.KEYCODE_SEARCH:
		case KeyEvent.KEYCODE_ENTER:
			AnalyticsUtil.trackEvent(SearchActivity.this, "Search", "Enter", "Query", 0);
			search();
			return true;
		}
		return false;
	}
}
