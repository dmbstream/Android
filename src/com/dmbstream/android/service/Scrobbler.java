package com.dmbstream.android.service;

import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.dmbstream.android.util.HttpConnection;
import com.dmbstream.android.util.Util;
import com.dmbstream.api.ApiConstants;
import com.dmbstream.api.Track;

/**
 * Scrobbles played songs to Last.fm.
 *
 * @author Sindre Mehus
 * @version $Id: Scrobbler.java 2184 2011-02-24 12:19:24Z sindre_mehus $
 */
public class Scrobbler {

    private static final String TAG = Scrobbler.class.getSimpleName();

    private String lastSubmission;
    private String lastNowPlaying;

    public void scrobble(final Context context, final DownloadFile song, final boolean isCompletedPlay) {
        if (song == null) {
            return;
        }
        final Track track = song.getSong();
        final String id = track.id;

        // Avoid duplicate registrations.
        if (isCompletedPlay && id.equals(lastSubmission)) {
            return;
        }
        if (!isCompletedPlay && id.equals(lastNowPlaying)) {
            return;
        }
        if (isCompletedPlay) {
            lastSubmission = id;
        } else {
            lastNowPlaying = id;
        }

        Log.d(TAG, "Scrobble " + song);
        new Thread("Scrobble " + song) {
            @Override
            public void run() {
                try {
                	JSONObject params = new JSONObject();
                	params.put("id", track.id);
                	HttpConnection.postAsJson(ApiConstants.instance().baseUrl("api/tracks/trackplay"), Util.getUserToken(context), params);

                	if (Util.getShouldScrobble(context)) {
	                	Intent intent = new Intent("net.jjc1138.android.scrobbler.action.MUSIC_STATUS");
	            		intent.putExtra("playing", !isCompletedPlay);
	            		intent.putExtra("artist", track.artist);
	            		intent.putExtra("track", track.title);
	            		intent.putExtra("secs", track.getDurationAsSeconds());
	            		intent.putExtra("album", track.concert);
	            		
	            		context.sendBroadcast(intent);
                	}
                    Log.i(TAG, "Scrobbled '" + (isCompletedPlay ? "submission" : "now playing") + "' for " + song);
                } catch (Exception x) {
                    Log.i(TAG, "Failed to scrobble'" + (isCompletedPlay ? "submission" : "now playing") + "' for " + song, x);
                }
            }
        }.start();
    }
}
