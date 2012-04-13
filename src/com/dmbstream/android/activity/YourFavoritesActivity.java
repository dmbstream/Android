package com.dmbstream.android.activity;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.dmbstream.android.util.*;
import com.dmbstream.api.ApiConstants;
import com.dmbstream.api.Concert;
import com.dmbstream.api.Playlist;
import com.dmbstream.api.UrlWithParameters;
import com.dmbstream.android.R;

public class YourFavoritesActivity extends TitleActivity {
	private static final String TAG = YourFavoritesActivity.class.getSimpleName();

	private LayoutInflater inflater;
	private LinearLayout concertList;
	private LinearLayout playlistList;

	private boolean hasLoaded = false;

	private TextView concertHeader;
	private TextView playlistHeader;
	
	private View.OnClickListener concertClickListener;
	private View.OnClickListener playlistClickListener;

	private TextView addMoreConcerts;

	private TextView addMorePlaylists;

	private int defaultItemsPerPage = 10;
	private int concertPage = 0;
	private int playlistPage = 0;
	
	private static final int MESSAGE_ADD_MORE = 0;

	private final Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == MESSAGE_ADD_MORE) {
				if (moreConcerts != null) {
					
					if (totalConcertCount <= 0) {
						concertHeader.setVisibility(View.GONE);
						concertList.setVisibility(View.GONE);
						addMoreConcerts.setVisibility(View.GONE);
					} else {
						boolean endReached = (totalConcertCount <= concertPage * concertItemsPerPage);
						addMoreConcerts.setVisibility(!endReached ? View.VISIBLE : View.GONE);
						
						for (Concert concert : moreConcerts) {
							View listItem = inflater.inflate(R.layout.partial_concert_list_item, null);
							TextView itemArtist = (TextView)listItem.findViewById(R.id.ConcertItemArtist);
							TextView itemDate = (TextView)listItem.findViewById(R.id.ConcertItemDate);
							TextView itemName = (TextView)listItem.findViewById(R.id.ConcertItemName);
	
							Date d = new Date(concert.date.getTimeInMillis());
							DateFormat shortDateFormat = new SimpleDateFormat("yyyy-MM-dd");
	
							itemDate.setText(shortDateFormat.format(d));
							itemArtist.setText(concert.artistAbbreviation);
							itemName.setText(concert.name);
							
							listItem.setBackgroundResource(R.drawable.tr_bg);
							listItem.setPadding(10, 10, 10, 10);
							listItem.setOnClickListener(concertClickListener);
							listItem.setTag(R.id.concert_id, concert.id);
							
							concertList.addView(listItem);
						}
					}
					moreConcerts = null;
				}
				if (morePlaylists != null) {
					if (totalPlaylistCount <= 0) {
						playlistHeader.setVisibility(View.GONE);
						playlistList.setVisibility(View.GONE);
						addMorePlaylists.setVisibility(View.GONE);
					} else {
						boolean endReached = (totalPlaylistCount <= playlistPage * playlistItemsPerPage);
						addMorePlaylists.setVisibility(!endReached ? View.VISIBLE : View.GONE);
					
						for (Playlist playlist : morePlaylists) {
							View listItem = inflater.inflate(R.layout.partial_playlist_list_item, null);
							TextView itemName = (TextView) listItem.findViewById(R.id.PlaylistItemName);
	
							itemName.setText(playlist.name);
	
							listItem.setBackgroundResource(R.drawable.tr_bg);
							listItem.setPadding(10, 10, 10, 10);
							listItem.setOnClickListener(playlistClickListener);
							listItem.setTag(R.id.playlist_id, playlist.id);
	
							playlistList.addView(listItem);
						}
					}
					morePlaylists = null;
				}
				
				// TODO: Display a message if they have no favorites
			} else {
				Util.toast(YourFavoritesActivity.this, R.string.msg_check_connection);
			}
			stopIndeterminateProgressIndicator();
		}
	};

	private ArrayList<Concert> moreConcerts;
	private ArrayList<Playlist> morePlaylists;
	private int totalConcertCount;
	private int concertItemsPerPage;
	private int totalPlaylistCount;
	private int playlistItemsPerPage;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		inflater = LayoutInflater.from(this);
		
		ViewGroup container = (ViewGroup) findViewById(R.id.Content);
		ViewGroup.inflate(this, R.layout.view_favorite_list, container);

		concertHeader = (TextView) findViewById(R.id.concert_list_header_title);
		concertList = (LinearLayout) findViewById(R.id.concertList);
		playlistHeader = (TextView) findViewById(R.id.concert_list_header_title);
		playlistList = (LinearLayout) findViewById(R.id.playlistList);
		addMoreConcerts = (TextView) findViewById(R.id.AddMoreConcerts);
		addMorePlaylists = (TextView) findViewById(R.id.AddMorePlaylists);
		
		addMoreConcerts.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.v(TAG, "addMoreConcerts::Click");
				AnalyticsUtil.trackEvent(YourFavoritesActivity.this, "YourFavorites", "Click", "MoreConcerts", 0);
				addMoreConcerts();
			}
		});
		addMorePlaylists.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.v(TAG, "addMorePlaylists::Click");
				AnalyticsUtil.trackEvent(YourFavoritesActivity.this, "YourFavorites", "Click", "MorePlaylists", 0);
				addMorePlaylists();
			}
		});
		
		concertClickListener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.d(TAG, "concert click");
				
				String concertId = (String)v.getTag(R.id.concert_id);
				
				AnalyticsUtil.trackEvent(YourFavoritesActivity.this, "YourFavorites", "Click", "Concert", 0);
				Intent i = new Intent(YourFavoritesActivity.this, ConcertActivity.class);
				i.putExtra(Constants.EXTRA_CONCERT_ID, concertId);
				startActivityWithoutAnimation(i);
			}
		};
		playlistClickListener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.d(TAG, "playlist click");
				Integer playlistId = (Integer)v.getTag(R.id.playlist_id);
				
				AnalyticsUtil.trackEvent(YourFavoritesActivity.this, "YourFavorites", "Click", "Playlist", 0);
				Intent i = new Intent(YourFavoritesActivity.this, PlaylistActivity.class);
				i.putExtra(Constants.EXTRA_PLAYLIST_ID, playlistId);
				startActivityWithoutAnimation(i);
			}
		};
	}
	
	@Override
	public CharSequence getMainTitle() {
		return getString(R.string.msg_your_favorites_title);
	}
	@Override
	protected String getAnalyticsPageName() {
		return "/YourFavorites";
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		
		// Call this here instead of onCreate to ensure that UserId is set
		if (!hasLoaded) {
			hasLoaded = true;
			startIndeterminateProgressIndicator();
			new Thread(new Runnable() {
				@Override
				public void run() {
					getMoreConcerts();
					getMorePlaylists();
					handler.sendEmptyMessage(MESSAGE_ADD_MORE);
				}
			}).start();
		}
	}

	private void addMoreConcerts() {
		startIndeterminateProgressIndicator();
		new Thread(new Runnable() {
			@Override
			public void run() {
				getMoreConcerts();
				handler.sendEmptyMessage(MESSAGE_ADD_MORE);
			}
		}).start();
	}
	private void getMoreConcerts() {
		UrlWithParameters url = getConcertsApiUrl();
		concertPage  = concertPage + 1;
		try {
			url.params.put(ApiConstants.PARAM_PAGE, concertPage);
			url.params.put(ApiConstants.PARAM_ITEMS_PER_PAGE,
					defaultItemsPerPage);

			moreConcerts = new ArrayList<Concert>();
			JSONObject result = HttpConnection.postAsJson(url, Token);

			totalConcertCount = result.getInt(ApiConstants.PARAM_TOTAL_COUNT);
			concertItemsPerPage = result.getInt(ApiConstants.PARAM_RETURN_ITEMS_PER_PAGE);

			JSONArray items = result.getJSONArray("items");
			int size = items.length();
			if (size > 0) {
				for (int i = 0; i < size; i++) {
					JSONObject item = (JSONObject) items.get(i);
					Concert concert = new Concert();
					concert.loadFromJson(item);

					moreConcerts.add(concert);
				}
			}
		} catch (Exception e) {
			Log.e(TAG, "Something went wrong loading concerts" + e);
		}		
	}
	
	private UrlWithParameters getConcertsApiUrl() {
		UrlWithParameters url = new UrlWithParameters();
		url.url = ApiConstants.instance().baseUrl("api/users/" + UserId + "/favoriteConcerts");
		url.params = new JSONObject();
		try {
			url.params.put("sortAsc", "Name");
		} catch (JSONException e) {
			Log.e(TAG, "Error adding query to latest playlists request", e);
		}
		
		return url;
	}
	
	private void addMorePlaylists() {
		startIndeterminateProgressIndicator();
		new Thread(new Runnable() {
			@Override
			public void run() {
				getMorePlaylists();
				handler.sendEmptyMessage(MESSAGE_ADD_MORE);
			}
		}).start();
	}
	
	private void getMorePlaylists() {
		UrlWithParameters url = getPlaylistsApiUrl();
		playlistPage  = playlistPage + 1;
		try {
			url.params.put(ApiConstants.PARAM_PAGE, playlistPage);
			url.params.put(ApiConstants.PARAM_ITEMS_PER_PAGE, defaultItemsPerPage);

			morePlaylists = new ArrayList<Playlist>();
			JSONObject result = HttpConnection.postAsJson(url, Token);

			totalPlaylistCount = result.getInt(ApiConstants.PARAM_TOTAL_COUNT);
			playlistItemsPerPage = result.getInt(ApiConstants.PARAM_RETURN_ITEMS_PER_PAGE);

			JSONArray items = result.getJSONArray("items");
			int size = items.length();
			if (size > 0) {
				for (int i = 0; i < size; i++) {
					JSONObject item = (JSONObject) items.get(i);
					Playlist playlist = new Playlist();
					playlist.loadFromJson(item);

					morePlaylists.add(playlist);
				}
			}
		} catch (Exception e) {
			Log.e(TAG, "Something went wrong loading playlists" + e);
		}		
	}

	private UrlWithParameters getPlaylistsApiUrl() {
		UrlWithParameters url = new UrlWithParameters();
		url.url = ApiConstants.instance().baseUrl("api/users/" + UserId + "/favoritePlaylists");
		url.params = new JSONObject();
		try {
			url.params.put("sortAsc", "Name");
		} catch (JSONException e) {
			Log.e(TAG, "Error adding query to latest playlists request", e);
		}
		
		return url;
	}
}
