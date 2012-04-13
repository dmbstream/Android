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
import com.dmbstream.api.Playlist;
import com.dmbstream.api.UrlWithParameters;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class PlaylistListAdapter extends ArrayAdapter<Playlist> {
	private static final String TAG = PlaylistListAdapter.class.getSimpleName();
	private final LayoutInflater inflater;
	private RootActivity rootActivity = null;
	private int page = 0;
	private long lastUpdate = -1;
	private PlaylistsLoadedListener playlistsLoadedListener;

	public PlaylistListAdapter(Context context) {
		super(context, R.layout.partial_playlist_list_item);
		if (context instanceof RootActivity) {
			rootActivity = (RootActivity) context;
		}
		inflater = LayoutInflater.from(getContext());
	}

	private List<Playlist> morePlaylists;
	private boolean endReached = false;

	private final Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (msg.what >= 0) {
				if (morePlaylists != null) {
					lastUpdate = System.currentTimeMillis();
					// Remove the add more playlists row
					remove(null);
					for (Playlist p : morePlaylists) {
						if (getPosition(p) < 0) {
							add(p);
						}
					}
					if (!endReached) {
						// Add in the row to load more playlists
						add(null);
					}
				}
				if (playlistsLoadedListener != null) {
					playlistsLoadedListener.playlistsLoaded();
				}
			} else {
				Toast.makeText(
						rootActivity,
						rootActivity.getResources().getText(R.string.msg_check_connection),
						Toast.LENGTH_LONG).show();
			}
			if (rootActivity != null) {
				rootActivity.stopIndeterminateProgressIndicator();
			}

		}
	};

	@Override
	public View getView(final int position, View playlistView, final ViewGroup parent) {

		if (playlistView == null) {
			playlistView = inflater.inflate(R.layout.partial_playlist_list_item, parent, false);
		}

		Playlist playlist = getItem(position);

		TextView name = (TextView) playlistView.findViewById(R.id.PlaylistItemName);

		if (playlist != null) {

			name.setText(playlist.name);

			// Need to (re)set this because the views are reused. If we don't
			// then
			// while scrolling, some items will replace the old
			// "Load more playlists"
			// view and will be in italics
			name.setTypeface(name.getTypeface(), Typeface.BOLD);
		} else {
			// null marker means it's the end of the list.
			name.setTypeface(name.getTypeface(), Typeface.ITALIC);
			name.setText(R.string.msg_load_more_playlists);
		}

		return playlistView;
	}
	
	@Override
	public void clear() {
		morePlaylists = null;
		page = 0;
		super.clear();
	}

	public void addMorePlaylists(final UrlWithParameters url) {
		if (rootActivity != null) {
			rootActivity.startIndeterminateProgressIndicator();
		}
		new Thread(new Runnable() {
			@Override
			public void run() {
				if (getMorePlaylists(url)) {
					handler.sendEmptyMessage(0);
				} else {
					handler.sendEmptyMessage(-1);
				}
			}
		}).start();
	}

	private boolean getMorePlaylists(UrlWithParameters url) {
		final int defaultItemsPerPage = 10;
		
    	try {
    		page = page + 1;
    		
    		Log.v(TAG, "getMorePlaylists");
    		
    		url.params.put(ApiConstants.PARAM_PAGE, page);
    		if (!url.params.has(ApiConstants.PARAM_ITEMS_PER_PAGE))
    			url.params.put(ApiConstants.PARAM_ITEMS_PER_PAGE, defaultItemsPerPage);
    		
    		JSONObject result = HttpConnection.postAsJson(url, rootActivity.Token);
    		JSONArray items = result.getJSONArray("items");
    		int size = items.length();
    		if (size > 0) {
    			morePlaylists = new ArrayList<Playlist>(size);
    			for (int i = 0; i < size; i++) {
    				JSONObject item = (JSONObject) items.get(i);
    				Playlist playlist = new Playlist();
    				playlist.loadFromJson(item);
    				morePlaylists.add(playlist);
    			}
    		}
    		
    		int totalCount = result.getInt(ApiConstants.PARAM_TOTAL_COUNT);
    		int itemsPerPage = result.getInt(ApiConstants.PARAM_RETURN_ITEMS_PER_PAGE);
    		endReached = (totalCount <= page * itemsPerPage);
    		
		} catch (Exception e) {
			Log.e(TAG, "Error getting more playlists", e);
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
	 * A call back that can be used to be notified when playlists are done
	 * loading.
	 */
	public interface PlaylistsLoadedListener {
		void playlistsLoaded();
	}

	/**
	 * Sets a listener to be notified when playlists are done loading
	 * 
	 * @param listener
	 *            A {@link PlaylistsLoadedListener}
	 */
	public void setPlaylistsLoadedListener(PlaylistsLoadedListener listener) {
		playlistsLoadedListener = listener;
	}
}
