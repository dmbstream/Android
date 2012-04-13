package com.dmbstream.android.activity;

import android.app.Activity;
import android.app.ActivityGroup;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bugsense.trace.BugSenseHandler;
import com.dmbstream.android.R;
import com.dmbstream.android.service.DownloadFile;
import com.dmbstream.android.service.DownloadServiceImpl;
import com.dmbstream.android.service.IDownloadService;
import com.dmbstream.android.util.AnalyticsUtil;
import com.dmbstream.android.util.Constants;
import com.dmbstream.android.util.HttpConnection;
import com.dmbstream.android.util.Util;
import com.dmbstream.android.view.PlaylistView;
import com.dmbstream.api.ApiConstants;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Calendar;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

public abstract class RootActivity extends ActivityGroup implements
		OnClickListener {
	private static final String TAG = RootActivity.class.getSimpleName();
	private PlaylistView playlistView;
	private ProgressBar progressIndicator;

	public String Token;
	String Username;
	int UserId;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Token = Util.getUserToken(this);
		super.onCreate(savedInstanceState);

		setContentView(R.layout.layout_main);

		// Override the normal volume controls so that the user can alter the
		// volume
		// when a stream is not playing.
		setVolumeControlStream(AudioManager.STREAM_MUSIC);

		ViewGroup titleFrame = (ViewGroup) findViewById(R.id.TitleContent);

		ImageButton mainSearchButton = (ImageButton) findViewById(R.id.MainSearchButton);
		mainSearchButton.setOnClickListener(this);

		if (!this.getClass().equals(MainMenuActivity.class)) {
			ImageButton logo = (ImageButton) findViewById(R.id.Logo);
			if (logo != null) {
				logo.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View arg0) {
						AnalyticsUtil.trackEvent(RootActivity.this, "Header", "Click", "Home", 0);
						Intent intent = new Intent(RootActivity.this, MainMenuActivity.class);
						startActivityWithoutAnimation(intent);
					}

				});
			}
		}

		playlistView = new PlaylistView(this);
		titleFrame.addView(playlistView, new ViewGroup.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));

		progressIndicator = (ProgressBar) findViewById(R.id.WindowProgressIndicator);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		playlistView.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
		playlistView.onPause();
	}

    public IDownloadService getDownloadService() {
        // If service is not available, request it to start and wait for it.
        for (int i = 0; i < 5; i++) {
            IDownloadService downloadService = DownloadServiceImpl.getInstance();
            if (downloadService != null) {
                return downloadService;
            }
            Log.w(TAG, "DownloadService not running. Attempting to start it.");
            startService(new Intent(this, DownloadServiceImpl.class));
            Util.sleepQuietly(50L);
        }
        return DownloadServiceImpl.getInstance();
    }

	public void startIndeterminateProgressIndicator() {
		progressIndicator.setVisibility(View.VISIBLE);
	}

	public void stopIndeterminateProgressIndicator() {
		progressIndicator.setVisibility(View.INVISIBLE);
	}

	@Override
	protected void onStart() {
		super.onStart();

		Log.d(TAG, "Checking for api token");
		if (StringUtils.isEmpty(Token)) {
			Log.d(TAG, "No API Token found, redirecting to login class");
			Intent intent = new Intent(this, LoginActivity.class);
			startActivityWithoutAnimation(intent);
			return;
		}

		if (!ApiConstants.instance().apiKeyIsValid()) {
			final AlertDialog dialog = new AlertDialog.Builder(this)
					.setOnCancelListener(
							new DialogInterface.OnCancelListener() {
								@Override
								public void onCancel(DialogInterface dialog) {
									finish();
								}
							}).setMessage(R.string.msg_api_key_error).create();
			dialog.show();

			// Make the TextView clickable. Must be called after show()
			// http://stackoverflow.com/questions/1997328/android-clickable-hyperlinks-in-alertdialog
			((TextView) dialog.findViewById(android.R.id.message))
					.setMovementMethod(LinkMovementMethod.getInstance());

		}

		long lastAccessed = Util.getLastAccessed(this);
		Username = Util.getUsername(this);
		UserId = Util.getUserId(this);
		Calendar hourAgo = Calendar.getInstance();
		hourAgo.add(Calendar.HOUR_OF_DAY, -1);
		if (lastAccessed <= hourAgo.getTimeInMillis()) {
			try {
				Log.d(TAG, "Getting user information");
				JSONObject user = HttpConnection.getAsJson(ApiConstants.instance().baseUrl("api/users/current"), Token);

				Username = user.getString("name");
				UserId = user.getInt("id");
				boolean isDonor = user.getBoolean("is_donor");

				Util.setUserId(this, UserId);
				Util.setUsername(this, Username);
				Util.setLastAccessed(this, Calendar.getInstance().getTimeInMillis());
				Util.setIsDonor(this, isDonor);
				if (!isDonor)
					Util.setPreloadCount(this, 1);
			} catch (Exception ex) {
				Log.e(TAG, "Error getting user data: " + ex);
				BugSenseHandler.log(TAG + "_GetUserInfo", ex);
				if (lastAccessed < 0) {
					final AlertDialog dialog = new AlertDialog.Builder(this)
						.setMessage(R.string.msg_username_error)
						.setPositiveButton(R.string.common_ok, new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int which) {
								Util.setUserToken(RootActivity.this, null);
								Intent intent = new Intent(RootActivity.this, LoginActivity.class);
								startActivityWithoutAnimation(intent);
								finish();
							}
								
						}).create();
					dialog.show();
				}
			}
		}

		// Navigation on top of player, player on top of content
		playlistView.bringToFront();
	}

	@Override
	public void finish() {
		super.finish();
		noAnimation();
	}

	protected void startActivityWithoutAnimation(Intent i) {
		startActivity(i);
		noAnimation();
	}

	/**
	 * Prevents the default animation on the pending transition. Only works on
	 * SDK version 5 and up, but may be safely called from any version.
	 */
	private void noAnimation() {
		try {
			Method overridePendingTransition = Activity.class.getMethod(
					"overridePendingTransition", new Class[] { int.class,
							int.class });
			overridePendingTransition.invoke(this, 0, 0);
		} catch (SecurityException e) {
			Log.w(TAG, "", e);
		} catch (NoSuchMethodException e) {
			// Don't log an error here; we anticipate an error on SDK < 5
		} catch (IllegalArgumentException e) {
			Log.w(TAG, "", e);
		} catch (IllegalAccessException e) {
			Log.w(TAG, "", e);
		} catch (InvocationTargetException e) {
			Log.w(TAG, "", e);
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Log.d(TAG, "onCreateOptionsMenu");
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.options_menu, menu);
		return true;
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.d(TAG, "onCreateOptionsMenu");
    	Intent intent;
		switch (item.getItemId()) {
			case R.id.menu_donate:
				AnalyticsUtil.trackEvent(RootActivity.this, "Options", "Click", "Donate", 0);
				Uri uri = Uri.parse("http://dmbstream.com/donate");
				intent = new Intent(Intent.ACTION_VIEW, uri);
				startActivity(intent);
				return true;
			case R.id.menu_home:
				AnalyticsUtil.trackEvent(RootActivity.this, "Options", "Click", "Home", 0);
				intent = new Intent(RootActivity.this, MainMenuActivity.class);
				startActivityWithoutAnimation(intent);
				return true;
			case R.id.menu_chat:
				AnalyticsUtil.trackEvent(RootActivity.this, "Options", "Click", "Chat", 0);
				intent = new Intent(RootActivity.this, ChatActivity.class);
				startActivityWithoutAnimation(intent);
				return true;
			case R.id.menu_random:
				AnalyticsUtil.trackEvent(RootActivity.this, "Options", "Click", "Random", 0);
				intent = new Intent(RootActivity.this, ConcertActivity.class);
				intent.putExtra(Constants.EXTRA_CONCERT_ID, Constants.RandomConcertValue);
				startActivityWithoutAnimation(intent);
				return true;
			case R.id.menu_settings:
				AnalyticsUtil.trackEvent(RootActivity.this, "Options", "Click", "Settings", 0);
				intent = new Intent(RootActivity.this, SettingsActivity.class);
				startActivityWithoutAnimation(intent);
				return true;
			case R.id.menu_logout:
				AnalyticsUtil.trackEvent(RootActivity.this, "Options", "Click", "Logout", 0);
				Util.setUserToken(RootActivity.this, null);
				intent = new Intent(RootActivity.this, LoginActivity.class);
				startActivityWithoutAnimation(intent);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
		Log.d(TAG, "Creating context menu for list items");

		super.onCreateContextMenu(menu, view, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.playlist_context_menu, menu);

		// Set the menu header to the name of the item in the playlist
		try {
			AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
			if (info == null)
				return;
            DownloadFile track = playlistView.getItemAtPosition(info.position);
			menu.setHeaderTitle(track.getSong().title);
		} catch (ClassCastException e) {
			Log.e(TAG, "MenuInfo was an unexpected type", e);
		}

	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        DownloadFile track = playlistView.getItemAtPosition(info.position);
		switch (item.getItemId()) {
			case R.id.PlaylistPlayEntry:
				AnalyticsUtil.trackEvent(RootActivity.this, "Playlist", "LongPress", "Play", 0);
				playlistView.play(info.position);
				return true;
			case R.id.PlaylistRemoveEntry:
				AnalyticsUtil.trackEvent(RootActivity.this, "Playlist", "LongPress", "Remove", 0);
				playlistView.remove(track);
				return true;
			default:
				return super.onContextItemSelected(item);
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.MainSearchButton:
				AnalyticsUtil.trackEvent(RootActivity.this, "Header", "Click", "Search", 0);
				startActivityWithoutAnimation(new Intent(this, SearchActivity.class));
				break;
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
			if (playlistView.isExpanded()) {
				playlistView.setExpanded(false);
				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}
}