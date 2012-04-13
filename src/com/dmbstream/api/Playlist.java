package com.dmbstream.api;

import org.json.JSONException;
import org.json.JSONObject;

public class Playlist {
	public int id;
	public String name;

	public Playlist() {
	}
	
	public void loadFromJson(JSONObject item) throws JSONException {
		id = item.getInt("id");
		name = item.getString("name");
	}

	@Override
	public String toString() {
		return id + " - " + name;
	}
}