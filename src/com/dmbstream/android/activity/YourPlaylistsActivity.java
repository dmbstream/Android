package com.dmbstream.android.activity;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.dmbstream.android.adapter.PlaylistListAdapter;
import com.dmbstream.android.util.*;
import com.dmbstream.api.ApiConstants;
import com.dmbstream.api.Playlist;
import com.dmbstream.api.UrlWithParameters;
import com.dmbstream.android.R;

public class YourPlaylistsActivity extends TitleActivity implements
		OnItemClickListener {
	private static final String TAG = YourPlaylistsActivity.class.getSimpleName();

	private PlaylistListAdapter listAdapter;
	private ListView listView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		ViewGroup container = (ViewGroup) findViewById(R.id.Content);
		ViewGroup.inflate(this, R.layout.view_playlist_list, container);

		listView = (ListView) findViewById(R.id.ListView01);

		listView.setOnItemClickListener(this);
		listAdapter = new PlaylistListAdapter(this);
		listView.setAdapter(listAdapter);
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		
		// Call this here instead of onCreate to ensure that UserId is set
		if (listAdapter.isEmpty()) {
			addPlaylists();
		}
	}

	@Override
	public CharSequence getMainTitle() {
		return getString(R.string.msg_your_playlists_title);
	}
	@Override
	protected String getAnalyticsPageName() {
		return "/YourPlaylists";
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

//		// Ignore the click action after a long press on an audio track
//		if (position == lastLongPressPosition) {
//			lastLongPressPosition = -1;
//			return;
//		}

		Playlist playlist = (Playlist) parent.getAdapter().getItem(position);
		if (playlist == null) {
			AnalyticsUtil.trackEvent(YourPlaylistsActivity.this, "YourPlaylists", "Click", "MorePlaylist", 0);
			addPlaylists();
		} else {
			AnalyticsUtil.trackEvent(YourPlaylistsActivity.this, "YourPlaylists", "Click", "Playlist", 0);
			Intent i = new Intent(this, PlaylistActivity.class);
			i.putExtra(Constants.EXTRA_PLAYLIST_ID, playlist.id);
			startActivityWithoutAnimation(i);
		}
	}

	private void addPlaylists() {
		UrlWithParameters url = getApiUrl();
		listAdapter.addMorePlaylists(url);
	}

	private UrlWithParameters getApiUrl() {
		UrlWithParameters url = new UrlWithParameters();
		url.url = ApiConstants.instance().baseUrl("api/users/" + UserId + "/playlists");
		url.params = new JSONObject();
		try {
			url.params.put("sortAsc", "Name");
		} catch (JSONException e) {
			Log.e(TAG, "Error adding query to latest concerts request", e);
		}
		
		return url;
	}
}
