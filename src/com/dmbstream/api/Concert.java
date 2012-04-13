package com.dmbstream.api;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import com.dmbstream.android.helpers.JsonHelper;

public class Concert {
	public String id;
	public String name;
	public String artist;
	public String artistAbbreviation;
	public Calendar date;
	public String notes;

	public Concert() {
	}
	
	public void loadFromJson(JSONObject item) throws JSONException {
		id = item.getString("id");
		name = item.getString("name");
		date = JsonHelper.parseDate(item.getString("date"));
		
		if (item.has("notes")) {
			notes = item.getString("notes");
		} else {
			notes = "";
		}

		JSONObject artist = item.getJSONObject("artist");
		this.artist = artist.getString("name");
		this.artistAbbreviation = artist.getString("abbr");
	}

	@Override
	public String toString() {
		return id + " - " + name;
	}

	private String _fullName = null;
	public CharSequence getFullName() {
		if (_fullName != null)
			return _fullName;
		
		Date d = new Date(date.getTimeInMillis());
		DateFormat shortDateFormat = new SimpleDateFormat("yyyy-MM-dd");

		return _fullName = artistAbbreviation + " :: " + shortDateFormat.format(d) + " :: " + name; 
	}
}