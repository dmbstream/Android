package com.dmbstream.api;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import com.dmbstream.android.helpers.JsonHelper;
import com.dmbstream.android.helpers.ValidationHelper;

public class Track implements Comparable<Track>, Serializable {
	private static final long serialVersionUID = -9219427704921663468L;
	
	public String id;
	public String title;
	public String artist;
	public String artistAbbreviation;
	public String concert;
	public String concertId;
	public int order;
	public int playCount;
	public String duration;
	public int trackNumber;
	public String url_low;
	public String url_high;

	private int _durationMs = -1;
	/*
	 * Gets the duration as seconds
	 */
	public int getDurationAsSeconds() {
		if (_durationMs != -1)
			return _durationMs;

		if (ValidationHelper.isNullOrWhitespace(duration))
			return _durationMs = 0;
		
		String[] parts = duration.split(":");
		_durationMs = Integer.parseInt(parts[0]) * 60;
		_durationMs += Integer.parseInt(parts[1]);
		
		return _durationMs;
	}

	/**
	 * Compares the order of the two songs
	 */
	@Override
	public int compareTo(Track other)
	{
		return order - other.order;
/*		if (concertId == other.concertId)
			return trackNumber - other.trackNumber;
		if (albumId > other.albumId)
			return 1;
		return -1;*/
	}
		
	/**
	 * Get the id of the given song.
	 *
	 * @param song The Song to get the id from.
	 * @return The id, or 0 if the given song is null.
	 */
	public static String getId(Track song)
	{
		if (song == null)
			return "";
		return song.id;
	}

	public void loadFromJson(JSONObject item) throws JSONException {
		id = item.getString("id");
		title = item.getString("name");
		concertId = item.getString("concert_id");
		order = item.getInt("order");
		playCount = item.getInt("play_count");
		duration = item.getString("duration");
		url_low = item.getString("url_low");
		url_high = item.getString("url_high");
		
		// Check to see if it's extended info track item
		if (item.has("concert")) {
			JSONObject concert = item.getJSONObject("concert");
			
			String concertName = concert.getString("name");
			Calendar concertDate = JsonHelper.parseDate(concert.getString("date"));
			Date d = new Date(concertDate.getTimeInMillis());
			DateFormat shortDateFormat = new SimpleDateFormat("yyyy-MM-dd");
			
			this.concert = shortDateFormat.format(d) + " :: " + concertName;
			JSONObject artist = concert.getJSONObject("artist");
			this.artist = artist.getString("name");
			this.artistAbbreviation = artist.getString("abbr");
		}
	}

	public String getPlayUrl(boolean highQuality) {
		return highQuality ? url_high : url_low;
	}
	
	@Override
	public String toString() {
		return id + " - " + title;
	}
}