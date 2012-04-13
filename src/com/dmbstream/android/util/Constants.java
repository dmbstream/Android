package com.dmbstream.android.util;

public class Constants {
	public static final Boolean IsDebug = false;
	public static final Boolean IsEmulator = false;
	public static final Boolean EnableAnalytics = !IsDebug;

    public static final int NOTIFICATION_ID_PLAYING = 100;
    public static final int NOTIFICATION_ID_ERROR = 101;

	public static final String EXTRA_SUBACTIVITY_ID = "subactivity_id";
	public static final String EXTRA_QUERY_TERM = "query_term";
	public static final String EXTRA_QUERY_URL = "query_url";
	public static final String EXTRA_ACTIVITY_DATA = "activity_data";

	public static final String EXTRA_DESCRIPTION = "extra_description";
	public static final String EXTRA_GROUPING = "extra_grouping";

	public static final String ApiToken = "user_api_token";
	// Note: This matches the name returned by context.getDefaultSharedPreferences 
	public static final String PREFERENCES_FILE_NAME = "com.dmbstream.android_preferences";
	public static final String PREFERENCES_KEY_SONG_QUALITY_WIFI = "songQualityWifi";
	public static final String PREFERENCES_KEY_SONG_QUALITY_MOBILE = "songQualityMobile";
    public static final String PREFERENCES_KEY_MUSIC_FOLDER_ID = "musicFolderId";
    public static final String PREFERENCES_KEY_CACHE_SIZE = "cacheSize";
    public static final String PREFERENCES_KEY_CACHE_LOCATION = "cacheLocation";
    public static final String PREFERENCES_KEY_PRELOAD_COUNT = "preloadCount";
    public static final String PREFERENCES_KEY_HIDE_MEDIA = "hideMedia";
    public static final String PREFERENCES_KEY_MEDIA_BUTTONS = "mediaButtons";
    public static final String PREFERENCES_KEY_SCREEN_LIT_ON_DOWNLOAD = "screenLitOnDownload";
    public static final String PREFERENCES_KEY_SCROBBLE = "scrobble";
    public static final String PREFERENCES_KEY_REPEAT_MODE = "repeatMode";
    public static final String PREFERENCES_KEY_WIFI_REQUIRED_FOR_DOWNLOAD = "wifiRequiredForDownload";

	public static final String SHARED_LASTACCESSED = "LastAccessed";
	public static final String SHARED_USERNAME = "Username";
	public static final String SHARED_USERID = "UserId";
	public static final String SHARED_IS_DONOR = "IsDonor";

	public static final String EXTRA_CONCERT_JSON = "concert_json";
	public static final String EXTRA_CONCERT_ID = "concert_id";
	public static final String EXTRA_PLAYLIST_ID = "playlist_id";
	public static final String RandomConcertValue = "__random__";

	private Constants() {
		// no instantiation
	}
}
