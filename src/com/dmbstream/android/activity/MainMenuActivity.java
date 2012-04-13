package com.dmbstream.android.activity;

import com.dmbstream.android.R;
import com.dmbstream.android.util.AnalyticsUtil;
import com.dmbstream.android.util.Constants;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableRow;

public class MainMenuActivity extends TitleActivity {
	private static final String TAG = MainMenuActivity.class.getSimpleName();
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewGroup container = (ViewGroup) findViewById(R.id.Content);
        ViewGroup.inflate(this, R.layout.view_main_menu, container);
        
        TableRow chatRow = (TableRow) findViewById(R.id.chatRow);
        chatRow.setOnClickListener(new View.OnClickListener() {
        	
        	@Override
        	public void onClick(View v) {
        		Log.d(TAG, "chatRow click");
        		
        		AnalyticsUtil.trackEvent(MainMenuActivity.this, "MainMenu", "Click", "Chat", 0);
        		Intent intent = new Intent(MainMenuActivity.this, ChatActivity.class);
        		startActivityWithoutAnimation(intent);
        	}
        });
        TableRow latestAddedConcertsRow = (TableRow) findViewById(R.id.latestAddedConcertsRow);
        latestAddedConcertsRow.setOnClickListener(new View.OnClickListener() {
        	
        	@Override
        	public void onClick(View v) {
        		Log.d(TAG, "latestAddedConcertsRow click");
        		
        		AnalyticsUtil.trackEvent(MainMenuActivity.this, "MainMenu", "Click", "LatestAdded", 0);
        		Intent intent = new Intent(MainMenuActivity.this, LatestAddedConcertsActivity.class);
        		startActivityWithoutAnimation(intent);
        	}
        });
        TableRow latestPerformedConcertsRow = (TableRow) findViewById(R.id.latestPerformedConcertsRow);
        latestPerformedConcertsRow.setOnClickListener(new View.OnClickListener() {
        	
        	@Override
        	public void onClick(View v) {
            	Log.d(TAG, "latestPerformedConcertsRow click");
        		
        		AnalyticsUtil.trackEvent(MainMenuActivity.this, "MainMenu", "Click", "LatestPerformed", 0);
        		Intent intent = new Intent(MainMenuActivity.this, LatestPerformedConcertsActivity.class);
        		startActivityWithoutAnimation(intent);
        	}
        });
        TableRow searchRow = (TableRow) findViewById(R.id.searchRow);
        searchRow.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
		    	Log.d(TAG, "searchRow click");
				
        		AnalyticsUtil.trackEvent(MainMenuActivity.this, "MainMenu", "Click", "Search", 0);
        		Intent intent = new Intent(MainMenuActivity.this, SearchActivity.class);
        		startActivityWithoutAnimation(intent);
			}
		});
        TableRow randomRow = (TableRow) findViewById(R.id.randomRow);
        randomRow.setOnClickListener(new View.OnClickListener() {
        	
        	@Override
        	public void onClick(View v) {
        		Log.d(TAG, "randomRow click");
        		
        		AnalyticsUtil.trackEvent(MainMenuActivity.this, "MainMenu", "Click", "Random", 0);
        		Intent intent = new Intent(MainMenuActivity.this, ConcertActivity.class);
        		intent.putExtra(Constants.EXTRA_CONCERT_ID, Constants.RandomConcertValue);
        		startActivityWithoutAnimation(intent);
        	}
        });        
        TableRow yourFavoritesRow = (TableRow) findViewById(R.id.yourFavoritesRow);
        yourFavoritesRow.setOnClickListener(new View.OnClickListener() {
        	
        	@Override
        	public void onClick(View v) {
        		Log.d(TAG, "yourFavoritesRow click");
        		
        		AnalyticsUtil.trackEvent(MainMenuActivity.this, "MainMenu", "Click", "YourFavorites", 0);
        		Intent intent = new Intent(MainMenuActivity.this, YourFavoritesActivity.class);
        		startActivityWithoutAnimation(intent);
        	}
        });        
        TableRow yourPlaylistsRow = (TableRow) findViewById(R.id.yourPlaylistsRow);
        yourPlaylistsRow.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
		    	Log.d(TAG, "yourPlaylistsRow click");
        		
        		AnalyticsUtil.trackEvent(MainMenuActivity.this, "MainMenu", "Click", "YourPlaylists", 0);
        		Intent intent = new Intent(MainMenuActivity.this, YourPlaylistsActivity.class);
        		startActivityWithoutAnimation(intent);
			}
		});        
    }
    
    @Override
    protected void onStart() {
    	super.onStart();
    	// Call this again, since the username will be set at this point
    	setTitleLeft(getMainTitle());
    }
    
    @Override
    public CharSequence getMainTitle() {
    	return String.format(getString(R.string.msg_main_welcome_format), Username);
    }
    @Override
    protected String getAnalyticsPageName() {
    	if (UserId > 0) 
    		return "/";
    	return null;
    }
}
