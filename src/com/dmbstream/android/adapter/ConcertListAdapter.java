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

package com.dmbstream.android.adapter;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import com.dmbstream.android.R;
import com.dmbstream.android.activity.RootActivity;
import com.dmbstream.android.util.HttpConnection;
import com.dmbstream.api.ApiConstants;
import com.dmbstream.api.Concert;
import com.dmbstream.api.UrlWithParameters;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ConcertListAdapter extends ArrayAdapter<Concert> {
	private static final String TAG = ConcertListAdapter.class.getSimpleName();
	private final LayoutInflater inflater;
	private RootActivity rootActivity = null;
	private int page = 0;
	private long lastUpdate = -1;
	private ConcertsLoadedListener concertsLoadedListener;

	public ConcertListAdapter(Context context) {
		super(context, R.layout.partial_concert_list_item);
		if (context instanceof RootActivity) {
			rootActivity = (RootActivity) context;
		}
		inflater = LayoutInflater.from(getContext());
	}

	private List<Concert> moreConcerts;
	private boolean endReached = false;

	private final Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (msg.what >= 0) {
				if (moreConcerts != null) {
					lastUpdate = System.currentTimeMillis();
					// Remove the add more concerts row
					remove(null);
					for (Concert c : moreConcerts) {
						if (getPosition(c) < 0) {
							add(c);
						}
					}
					if (!endReached) {
						// Add in the row to load more concerts
						add(null);
					}
				}
				if (concertsLoadedListener != null) {
					concertsLoadedListener.concertsLoaded();
				}
			} else {
				Toast.makeText(
						rootActivity,
						rootActivity.getResources().getText(
								R.string.msg_check_connection),
						Toast.LENGTH_LONG).show();
			}
			if (rootActivity != null) {
				rootActivity.stopIndeterminateProgressIndicator();
			}

		}
	};

	@Override
	public View getView(final int position, View convertView, final ViewGroup parent) {

		if (convertView == null) {
			convertView = inflater.inflate(R.layout.partial_concert_list_item,
					parent, false);
		}

		Concert concert = getItem(position);

		TextView date = (TextView) convertView.findViewById(R.id.ConcertItemDate);
		TextView artist = (TextView) convertView.findViewById(R.id.ConcertItemArtist);
		TextView name = (TextView) convertView.findViewById(R.id.ConcertItemName);

		if (concert != null) {

			Date d = new Date(concert.date.getTimeInMillis());
			DateFormat shortDateFormat = new SimpleDateFormat("yyyy-MM-dd");

			date.setText(shortDateFormat.format(d));
			artist.setText(concert.artistAbbreviation);
			name.setText(concert.name);

			// Need to (re)set this because the views are reused. If we don't
			// then
			// while scrolling, some items will replace the old
			// "Load more concerts"
			// view and will be in italics
			name.setTypeface(name.getTypeface(), Typeface.BOLD);
			date.setVisibility(View.VISIBLE);
			artist.setVisibility(View.VISIBLE);
		} else {
			// null marker means it's the end of the list.
			name.setTypeface(name.getTypeface(), Typeface.ITALIC);
			name.setText(R.string.msg_load_more_concerts);
			date.setVisibility(View.GONE);
			artist.setVisibility(View.GONE);
		}

		return convertView;
	}

	@Override
	public void clear() {
		moreConcerts = null;
		page = 0;
		super.clear();
	}

	public void addMoreConcerts(final UrlWithParameters url) {
		if (rootActivity != null) {
			rootActivity.startIndeterminateProgressIndicator();
		}
		new Thread(new Runnable() {
			@Override
			public void run() {
				if (getMoreConcerts(url)) {
					handler.sendEmptyMessage(0);
				} else {
					handler.sendEmptyMessage(-1);
				}
			}
		}).start();
	}

	private boolean getMoreConcerts(UrlWithParameters url) {
		final int defaultItemsPerPage = 10;

		try {
			page = page + 1;

			Log.v(TAG, "getMoreConcerts");

			url.params.put(ApiConstants.PARAM_PAGE, page);
			if (!url.params.has(ApiConstants.PARAM_ITEMS_PER_PAGE))
				url.params.put(ApiConstants.PARAM_ITEMS_PER_PAGE, defaultItemsPerPage);

			JSONObject result = HttpConnection.postAsJson(url, rootActivity.Token);
			JSONArray items = result.getJSONArray("items");
			int size = items.length();
			if (size > 0) {
				moreConcerts = new ArrayList<Concert>(size);
				for (int i = 0; i < size; i++) {
					JSONObject item = (JSONObject) items.get(i);
					Concert concert = new Concert();
					concert.loadFromJson(item);
					moreConcerts.add(concert);
				}
			}

			int totalCount = result.getInt(ApiConstants.PARAM_TOTAL_COUNT);
			int itemsPerPage = result.getInt(ApiConstants.PARAM_RETURN_ITEMS_PER_PAGE);
			endReached = (totalCount <= page * itemsPerPage);

		} catch (Exception e) {
			Log.e(TAG, "Error getting more concerts", e);
		}

		return true;
	}

	/**
	 * Returns the time (in milliseconds since the epoch) of when the last
	 * update was.
	 * 
	 * @return A time unit in milliseconds since the epoch
	 */
	public long getLastUpdate() {
		return lastUpdate;
	}

	/**
	 * A call back that can be used to be notified when concerts are done
	 * loading.
	 */
	public interface ConcertsLoadedListener {
		void concertsLoaded();
	}

	/**
	 * Sets a listener to be notified when concerts are done loading
	 * 
	 * @param listener
	 *            A {@link ConcertsLoadedListener}
	 */
	public void setConcertsLoadedListener(ConcertsLoadedListener listener) {
		concertsLoadedListener = listener;
	}
}
