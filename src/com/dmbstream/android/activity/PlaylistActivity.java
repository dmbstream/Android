package com.dmbstream.android.activity;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.dmbstream.android.R;
import com.dmbstream.android.util.AnalyticsUtil;
import com.dmbstream.android.util.Constants;
import com.dmbstream.android.util.HttpConnection;
import com.dmbstream.api.ApiConstants;
import com.dmbstream.api.Track;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.TableRow.LayoutParams;

public class PlaylistActivity extends TitleActivity {
	private static final String TAG = PlaylistActivity.class.getSimpleName();
	
	private TextView invalidPlaylist;
	private TextView errorLoadingPlaylist;
	private ScrollView scroller;
	private TextView name;
	private TableLayout tracksTable;

	private ArrayList<Track> tracks = new ArrayList<Track>();
	
    private View.OnClickListener trackClickListener;

	private JSONObject _playlist;

	private static final int MESSAGE_INVALID_CONCERT = 0x0;
	private static final int MESSAGE_ERROR_LOADING_CONCERT = 0x1;
	private static final int MESSAGE_SUCCESS = 0x2;

	private final Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			stopIndeterminateProgressIndicator();
			switch (msg.what) {
				case MESSAGE_SUCCESS:
					showPlaylistDetails();
					break;
				case MESSAGE_INVALID_CONCERT:
					showInvalidPlaylist();
					
					break;
				case MESSAGE_ERROR_LOADING_CONCERT:
					showErrorLoadingPlaylist();
					break;
			}
		}
	};

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewGroup container = (ViewGroup) findViewById(R.id.Content);
        ViewGroup.inflate(this, R.layout.view_playlist, container);
        
        final Bundle bundle = getIntent().getExtras();
        
        invalidPlaylist = (TextView) findViewById(R.id.invalidPlaylist);
        errorLoadingPlaylist = (TextView) findViewById(R.id.errorLoadingPlaylist);
        scroller = (ScrollView) findViewById(R.id.scroller);
        tracksTable = (TableLayout)findViewById(R.id.tracksTable);
        name = (TextView) findViewById(R.id.name);
        scroller.setVisibility(View.GONE);
        trackClickListener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.d(TAG, "tr click");
				v.showContextMenu();
			}
		};
		Button playAllButton = (Button) findViewById(R.id.playAllButton);
		playAllButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.d(TAG, "playAllButton click");
				AnalyticsUtil.trackEvent(PlaylistActivity.this, "Playlist", "Click", "PlayAll", 0);
				playTracks(tracks);
			}
		});
		Button queueAllButton = (Button) findViewById(R.id.queueAllButton);
		queueAllButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.d(TAG, "queueAllButton click");
				AnalyticsUtil.trackEvent(PlaylistActivity.this, "Playlist", "Click", "QueueAll", 0);
				queueTracks(tracks);
			}
		});
		
		startIndeterminateProgressIndicator();

		final Integer playlistId = bundle.getInt(Constants.EXTRA_PLAYLIST_ID);
		Thread fetchPlaylistThread = new Thread(new Runnable() {
			public void run() {
				int result = PlaylistActivity.this.fetchPlaylist(playlistId);
				handler.sendEmptyMessage(result);
			}
		});
		fetchPlaylistThread.start();
    }
    
    @Override
    public CharSequence getMainTitle() {
    	return getString(R.string.msg_loading);
    }
    @Override
    protected String getAnalyticsPageName() {
    	return null;
    }
    
    private int fetchPlaylist(Integer playlistId) {
    	Log.d(TAG, "fetchPlaylist");
    	
    	try {
    		_playlist = HttpConnection.getAsJson(ApiConstants.instance().baseUrl("api/playlists/" + playlistId), Token);

    		// TODO: Verify playlist contains playlist info and not an error message
    		
    		return MESSAGE_SUCCESS;
		} catch (Exception e) {
			e.printStackTrace();
		}
    	
    	return MESSAGE_INVALID_CONCERT;
    }
    
    private void showInvalidPlaylist() {
        invalidPlaylist.setVisibility(View.VISIBLE);
        errorLoadingPlaylist.setVisibility(View.GONE);
        scroller.setVisibility(View.GONE);
    }
    private void showErrorLoadingPlaylist() {
        errorLoadingPlaylist.setVisibility(View.VISIBLE);
        invalidPlaylist.setVisibility(View.GONE);
        scroller.setVisibility(View.GONE);
    }
    private void showPlaylistDetails() {
		String playlistName;
		try {
			int id = _playlist.getInt("id");
			playlistName = _playlist.getString("name");
			JSONArray tracks = _playlist.getJSONArray("tracks");
	
			setTitleLeft(playlistName);
			name.setText(playlistName);
			for (int i = 0; i < tracks.length(); i++) {
				JSONObject item = (JSONObject) tracks.get(i);
				Track track = new Track();
				track.loadFromJson(item);
	
				addTrackRow(i, track);
			}
			scroller.setVisibility(View.VISIBLE);
			AnalyticsUtil.trackPageView(this, "/Playlists?id=" + id);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			
			showErrorLoadingPlaylist();
		}
    }

    private void addTrackRow(int index, Track track) {
    	tracks.add(track);
    	
		int layout = R.layout.partial_table_row;
    	TableRow tr = (TableRow) getLayoutInflater().inflate(layout, null);

    	tr.setTag(R.id.concert_track, track);
    	tr.setTag(R.id.track_index, index);
    	tr.setOnClickListener(trackClickListener);    	
		registerForContextMenu(tr);
    	
		LayoutParams nameLayout = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		nameLayout.gravity = Gravity.CENTER_VERTICAL | Gravity.LEFT;
		TextView nameText = new TextView(this);
		nameText.setText(track.title);
		nameText.setLayoutParams(nameLayout);
//		nameText.setTextAppearance(getApplicationContext(), R.style.searchText);
		nameText.setPadding(5, 4, 10, 4);
		tr.addView(nameText);

		LayoutParams durationLayout = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		durationLayout.gravity = Gravity.CENTER_VERTICAL | Gravity.RIGHT;
		TextView durationText = new TextView(this);
		durationText.setText(track.duration);
		durationText.setLayoutParams(durationLayout);
//		durationText.setTextAppearance(getApplicationContext(), R.style.searchText);
		durationText.setPadding(5, 4, 10, 4);
		tr.addView(durationText);

		tracksTable.addView(tr);
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		Log.v(TAG, "onCreateContextMenu launched");
    	
		final Track track = (Track) v.getTag(R.id.concert_track);
		if (track != null) {
	    	final int trackIndex = (Integer) v.getTag(R.id.track_index);
	    	v.getContext();
	    	
	    	Log.d(TAG, "Track click: " + track.id + " - " + track.title);
	    	Log.d(TAG, "Track Index: " + trackIndex);
	    	menu.setHeaderTitle(track.title);
	    	
	    	MenuItem playButton = menu.add(Menu.NONE, R.id.menu_track_play, 0, R.string.msg_menu_play_now);
	    	playButton.setOnMenuItemClickListener(new OnMenuItemClickListener() {
	    		@Override
	    		public boolean onMenuItemClick(MenuItem arg0) {
	    			Log.v(TAG, "Play track: " + track.toString());
					AnalyticsUtil.trackEvent(PlaylistActivity.this, "Playlist", "Click", "PlayTrack", 0);
	    			playTrack(track);
	    			return true;
	    		}
	    	});
	    	MenuItem queueButton = menu.add(Menu.NONE, R.id.menu_track_queue, 1, R.string.msg_menu_play_queue);
	    	queueButton.setOnMenuItemClickListener(new OnMenuItemClickListener() {
	    		@Override
	    		public boolean onMenuItemClick(MenuItem arg0) {
	    			Log.v(TAG, "Queue track: " + track.toString());
					AnalyticsUtil.trackEvent(PlaylistActivity.this, "Playlist", "Click", "QueueTrack", 0);
	    			queueTrack(track);
	    			return true;
	    		}
	    	});
	    	if (trackIndex < tracks.size() - 1) {
		    	MenuItem playAllButton = menu.add(Menu.NONE, R.id.menu_track_play_now_and_queue_remaining, 2, R.string.msg_menu_play_now_and_queue_remaining);
		    	playAllButton.setOnMenuItemClickListener(new OnMenuItemClickListener() {
		    		@Override
		    		public boolean onMenuItemClick(MenuItem arg0) {
		    			Log.v(TAG, "Play and queue remaining");
						AnalyticsUtil.trackEvent(PlaylistActivity.this, "Playlist", "Click", "PlayAndQueueRemaining", 0);
		    			ArrayList<Track> playTracks = new ArrayList<Track>();
		    			for (int i = trackIndex; i < PlaylistActivity.this.tracks.size(); i++) {
		    				playTracks.add(PlaylistActivity.this.tracks.get(i));
		    			}
		    			Log.v(TAG, "Play track(s) from index: " + trackIndex + ". Track count: " + playTracks.size());
		    			playTracks(playTracks);
		    			return true;
		    		}
		    	});
		    	MenuItem queueAllButton = menu.add(Menu.NONE, R.id.menu_track_play_queue_and_remaining, 3, R.string.msg_menu_play_queue_and_remaining);
		    	queueAllButton.setOnMenuItemClickListener(new OnMenuItemClickListener() {
		    		@Override
		    		public boolean onMenuItemClick(MenuItem arg0) {
		    			Log.v(TAG, "Queue remaining");
						AnalyticsUtil.trackEvent(PlaylistActivity.this, "Playlist", "Click", "QueueFromHere", 0);
		    			ArrayList<Track> queueTracks = new ArrayList<Track>();
		    			for (int i = trackIndex; i < PlaylistActivity.this.tracks.size(); i++) {
		    				queueTracks.add(PlaylistActivity.this.tracks.get(i));
		    			}
		    			Log.v(TAG, "Queue track(s) from index: " + trackIndex + ". Track count: " + queueTracks.size());
		    			queueTracks(queueTracks);
		    			return true;
		    		}
		    	});
	    	}
		} else {
			super.onCreateContextMenu(menu, v, menuInfo);
		}
	}	

    private void playTrack(Track track) {
    	ArrayList<Track> tracks = new ArrayList<Track>();
    	tracks.add(track);
    	playTracks(tracks);
    }
    private void playTracks(List<Track> tracks) {
    	if (getDownloadService() == null)
    		return;

    	getDownloadService().queue(tracks, true);
    }
    
    private void queueTrack(Track track) {
    	ArrayList<Track> tracks = new ArrayList<Track>();
    	tracks.add(track);
    	queueTracks(tracks);
    }
    private void queueTracks(List<Track> tracks) {
    	if (getDownloadService() == null)
    		return;

    	getDownloadService().queue(tracks, false);
    }
}