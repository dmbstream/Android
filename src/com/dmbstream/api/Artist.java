package com.dmbstream.api;

import org.json.JSONException;
import org.json.JSONObject;

public class Artist {
	public String id;
	public String name;
	public String abbreviation;

	public Artist() {
	}
	
	public void loadFromJson(JSONObject item) throws JSONException {
		id = item.getString("id");
		name = item.getString("name");
		abbreviation = item.getString("abbr");
	}

	@Override
	public String toString() {
		return id + " - " + name;
	}
}