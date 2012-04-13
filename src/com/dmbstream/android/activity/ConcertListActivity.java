package com.dmbstream.android.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.dmbstream.android.R;
import com.dmbstream.android.adapter.ConcertListAdapter;
import com.dmbstream.android.util.*;
import com.dmbstream.api.Concert;
import com.dmbstream.api.UrlWithParameters;

public abstract class ConcertListActivity extends TitleActivity implements OnItemClickListener {

	private ConcertListAdapter listAdapter;
	private ListView listView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		ViewGroup container = (ViewGroup) findViewById(R.id.Content);
		ViewGroup.inflate(this, R.layout.view_concert_list, container);

		listView = (ListView) findViewById(R.id.ListView01);

		listView.setOnItemClickListener(this);
		listAdapter = new ConcertListAdapter(this);
		listView.setAdapter(listAdapter);

		addConcerts();
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

//		// Ignore the click action after a long press on an audio track
//		if (position == lastLongPressPosition) {
//			lastLongPressPosition = -1;
//			return;
//		}

		Concert concert = (Concert) parent.getAdapter().getItem(position);
		if (concert == null) {
			addConcerts();
		} else {
			Intent i = new Intent(this, ConcertActivity.class);
			i.putExtra(Constants.EXTRA_CONCERT_ID, concert.id);
			startActivityWithoutAnimation(i);
		}
	}

	private void addConcerts() {
		UrlWithParameters url = getApiUrl();
		listAdapter.addMoreConcerts(url);
	}

	/**
	 * Gets the API URL used for looking up the items for the list.
	 * <p/>
	 * The default implementation pulls the URL from the Intent's
	 * EXTRA_QUERY_URL value. A subclass may override this to get the URL
	 * another way.
	 * 
	 * @return A URL for the API.
	 */
	protected UrlWithParameters getApiUrl() {
		UrlWithParameters url = new UrlWithParameters();
		url.url = getIntent().getStringExtra(Constants.EXTRA_QUERY_URL); 
		return url;
	}

	public void refresh() {
		listAdapter.clear();
		addConcerts();
	}
}
