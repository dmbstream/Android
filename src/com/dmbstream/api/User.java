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

public class User implements Comparable<User>, Serializable {
	private static final long serialVersionUID = -1194011895181467294L;
	
	public int id;
	public String name;
	public boolean isDonor;
	public String token;

	/**
	 * Compares the order of the two songs
	 */
	@Override
	public int compareTo(User other)
	{
		return id - other.id;
/*		if (concertId == other.concertId)
			return trackNumber - other.trackNumber;
		if (albumId > other.albumId)
			return 1;
		return -1;*/
	}
		
	public void loadFromJson(JSONObject item) throws JSONException {
		id = item.getInt("id");
		name = item.getString("name");
		isDonor = item.getBoolean("is_donor");
		if (item.has("token"))
			token = item.getString("token");
	}
	
	@Override
	public String toString() {
		return id + " - " + name;
	}
}