package com.dmbstream.android.activity;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.bugsense.trace.BugSenseHandler;
import com.dmbstream.android.R;
import com.dmbstream.android.helpers.JsonHelper;
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

public class ConcertActivity extends TitleActivity {
	private static final String TAG = ConcertActivity.class.getSimpleName();
	
	private TextView invalidConcert;
	private TextView errorLoadingConcert;
	private ScrollView scroller;
	private TextView date;
	private TextView name;
	private TextView artist;
	private TableLayout tracksTable;
	private TextView notes;

	private ArrayList<Track> tracks = new ArrayList<Track>();
	
    private View.OnClickListener trackClickListener;

	private JSONObject _concert;
	private String concertAnalyticsId;

	private static final int MESSAGE_INVALID_CONCERT = 0x0;
	private static final int MESSAGE_ERROR_LOADING_CONCERT = 0x1;
	private static final int MESSAGE_SUCCESS = 0x2;

	private final Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			stopIndeterminateProgressIndicator();
			switch (msg.what) {
				case MESSAGE_SUCCESS:
					showConcertDetails();
					break;
				case MESSAGE_INVALID_CONCERT:
					showInvalidConcert();
					
					break;
				case MESSAGE_ERROR_LOADING_CONCERT:
					showErrorLoadingConcert();
					break;
			}
		}
	};

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewGroup container = (ViewGroup) findViewById(R.id.Content);
        ViewGroup.inflate(this, R.layout.view_concert, container);
        
        final Bundle bundle = getIntent().getExtras();
        
        invalidConcert = (TextView) findViewById(R.id.invalidConcert);
        errorLoadingConcert = (TextView) findViewById(R.id.errorLoadingConcert);
        scroller = (ScrollView) findViewById(R.id.scroller);
        tracksTable = (TableLayout)findViewById(R.id.tracksTable);
        date = (TextView) findViewById(R.id.date);
        name = (TextView) findViewById(R.id.name);
        artist = (TextView) findViewById(R.id.artist);
        notes = (TextView) findViewById(R.id.notes);
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
				AnalyticsUtil.trackEvent(ConcertActivity.this, "Concert", "Click", "PlayAll", 0);
				playTracks(tracks);
			}
		});
		Button queueAllButton = (Button) findViewById(R.id.queueAllButton);
		queueAllButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.d(TAG, "queueAllButton click");
				AnalyticsUtil.trackEvent(ConcertActivity.this, "Concert", "Click", "QueueAll", 0);
				queueTracks(tracks);
			}
		});
		
		if (bundle.containsKey(Constants.EXTRA_CONCERT_JSON)) {
			try {
				_concert = new JSONObject(bundle.getString(Constants.EXTRA_CONCERT_JSON));
				concertAnalyticsId = _concert.getString("id");
				handler.sendEmptyMessage(MESSAGE_SUCCESS);
			} catch (JSONException e) {
				e.printStackTrace();
				handler.sendEmptyMessage(MESSAGE_ERROR_LOADING_CONCERT);
			}
		} else {
	        startIndeterminateProgressIndicator();

	        final String concertId = bundle.getString(Constants.EXTRA_CONCERT_ID);
	        concertAnalyticsId = concertId;
	        Thread fetchConcertThread = new Thread(new Runnable() {
	        	public void run() {
	        		int result = ConcertActivity.this.fetchConcert(concertId);
	        		handler.sendEmptyMessage(result);
	        	}
	        });
	        fetchConcertThread.start();
		}
    }
    
    @Override
    public CharSequence getMainTitle() {
    	return getString(R.string.msg_loading);
    }
    @Override
    protected String getAnalyticsPageName() {
    	if (concertAnalyticsId.equals(Constants.RandomConcertValue)) {
    		return "/Concerts/Random";
    	}
    	// Instead, we track /Concert?id= from the showConcertDetails() method
    	return null;
    }
    
    private int fetchConcert(String concertId) {
    	Log.d(TAG, "fetchRandomConcert");
    	
    	try {
    		
    		if (concertId.equals(Constants.RandomConcertValue)) {
    			_concert = HttpConnection.getAsJson(ApiConstants.instance().baseUrl("api/concerts/random"), Token);
    		} else {
        		_concert = HttpConnection.getAsJson(ApiConstants.instance().baseUrl("api/concerts/" + concertId), Token);    			
    		}

    		// TODO: Verify concert contains concert info and not an error message
    		
    		return MESSAGE_SUCCESS;
		} catch (Exception e) {
			e.printStackTrace();
			BugSenseHandler.log(TAG + "_fetchConcert_" + concertId, e);
			return MESSAGE_ERROR_LOADING_CONCERT;
		}
    }
    
    private void showInvalidConcert() {
        invalidConcert.setVisibility(View.VISIBLE);
        errorLoadingConcert.setVisibility(View.GONE);
        scroller.setVisibility(View.GONE);
    }
    private void showErrorLoadingConcert() {
        errorLoadingConcert.setVisibility(View.VISIBLE);
        invalidConcert.setVisibility(View.GONE);
        scroller.setVisibility(View.GONE);
    }
    private void showConcertDetails() {
    	
    	if (_concert == null) {
    		showErrorLoadingConcert();
    		return;
    	}
    	
		String concertName;
		try {
			String id = _concert.getString("id");
			concertName = _concert.getString("name");
			Calendar concertDate = JsonHelper.parseDate(_concert.getString("date"));
			JSONObject concertArtist = _concert.getJSONObject("artist");
//			JSONObject concertVenue = _concert.getJSONObject("venue");
			JSONArray tracks = _concert.getJSONArray("tracks");
			String concertNotes = "";
			if (_concert.has("notes")) {
				concertNotes = _concert.getString("notes");
			}
	
			DateFormat dateFormat = android.text.format.DateFormat
					.getDateFormat(getApplicationContext());
			Date d = new Date(concertDate.getTimeInMillis());
	
			DateFormat shortDateFormat = new SimpleDateFormat("yyyy-MM-dd");
			
			setTitleLeft(shortDateFormat.format(d) + " :: " + concertName);
			name.setText(concertName);
			date.setText(dateFormat.format(d));
			artist.setText(concertArtist.getString("name"));
			notes.setText(concertNotes);
			for (int i = 0; i < tracks.length(); i++) {
				JSONObject item = (JSONObject) tracks.get(i);
				Track track = new Track();
				track.loadFromJson(item);
				track.artist = concertArtist.getString("name");
				track.artistAbbreviation = concertArtist.getString("abbr");
				track.concert = shortDateFormat.format(d) + " :: " + concertName;
	
				addTrackRow(i, track);
			}
			scroller.setVisibility(View.VISIBLE);
			AnalyticsUtil.trackPageView(this, "/Concerts?id=" + id);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			
			showErrorLoadingConcert();
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
	    	
	    	Log.d(TAG, "Track click: " + track.id + " - " + track.title);
	    	Log.d(TAG, "Track Index: " + trackIndex);
	    	menu.setHeaderTitle(track.title);
	    	
	    	MenuItem playButton = menu.add(Menu.NONE, R.id.menu_track_play, 0, R.string.msg_menu_play_now);
	    	playButton.setOnMenuItemClickListener(new OnMenuItemClickListener() {
	    		@Override
	    		public boolean onMenuItemClick(MenuItem arg0) {
	    			Log.v(TAG, "Play track: " + track.toString());
					AnalyticsUtil.trackEvent(ConcertActivity.this, "Concert", "Click", "PlayTrack", 0);
	    			playTrack(track);
	    			return true;
	    		}
	    	});
	    	MenuItem queueButton = menu.add(Menu.NONE, R.id.menu_track_queue, 1, R.string.msg_menu_play_queue);
	    	queueButton.setOnMenuItemClickListener(new OnMenuItemClickListener() {
	    		@Override
	    		public boolean onMenuItemClick(MenuItem arg0) {
	    			Log.v(TAG, "Queue track: " + track.toString());
					AnalyticsUtil.trackEvent(ConcertActivity.this, "Concert", "Click", "QueueTrack", 0);
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
						AnalyticsUtil.trackEvent(ConcertActivity.this, "Concert", "Click", "PlayAndQueueRemaining", 0);
		    			ArrayList<Track> playTracks = new ArrayList<Track>();
		    			for (int i = trackIndex; i < ConcertActivity.this.tracks.size(); i++) {
		    				playTracks.add(ConcertActivity.this.tracks.get(i));
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
						AnalyticsUtil.trackEvent(ConcertActivity.this, "Concert", "Click", "QueueFromHere", 0);
		    			ArrayList<Track> queueTracks = new ArrayList<Track>();
		    			for (int i = trackIndex; i < ConcertActivity.this.tracks.size(); i++) {
		    				queueTracks.add(ConcertActivity.this.tracks.get(i));
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