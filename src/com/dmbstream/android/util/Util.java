/*
 This file was part of Subsonic.

 Subsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Subsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Subsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2009 (C) Sindre Mehus
 */
package com.dmbstream.android.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Xml.Encoding;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;
import com.dmbstream.android.R;
import com.dmbstream.android.activity.MainMenuActivity;
import com.dmbstream.android.model.PlayerState;
import com.dmbstream.android.model.RepeatMode;
import com.dmbstream.android.provider.DmbstreamAppWidgetProvider;
import com.dmbstream.android.receiver.MediaButtonIntentReceiver;
import com.dmbstream.android.service.DownloadServiceImpl;
import com.dmbstream.api.Track;

import org.apache.http.HttpEntity;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * @author Sindre Mehus
 * @version $Id: Util.java 2588 2011-12-07 13:55:18Z sindre_mehus $
 */
public final class Util {

    private static final String TAG = Util.class.getSimpleName();

    private static final DecimalFormat GIGA_BYTE_FORMAT = new DecimalFormat("0.00 GB");
    private static final DecimalFormat MEGA_BYTE_FORMAT = new DecimalFormat("0.00 MB");
    private static final DecimalFormat KILO_BYTE_FORMAT = new DecimalFormat("0 KB");

    private static DecimalFormat GIGA_BYTE_LOCALIZED_FORMAT = null;
    private static DecimalFormat MEGA_BYTE_LOCALIZED_FORMAT = null;
    private static DecimalFormat KILO_BYTE_LOCALIZED_FORMAT = null;
    private static DecimalFormat BYTE_LOCALIZED_FORMAT = null;

    public static final String EVENT_META_CHANGED = "com.dmbstream.android.EVENT_META_CHANGED";
    public static final String EVENT_PLAYSTATE_CHANGED = "com.dmbstream.android.EVENT_PLAYSTATE_CHANGED";

    // Used by hexEncode()
    private static final char[] HEX_DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    private final static Pair<Integer, Integer> NOTIFICATION_TEXT_COLORS = new Pair<Integer, Integer>();
    private static Toast toast;

    private Util() {
    }

    public static boolean isOffline(Context context) {
    	ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    	NetworkInfo netInfo = cm.getActiveNetworkInfo();
    	
    	return netInfo == null || !netInfo.isConnected();
    }
    public static boolean isWifiConnected(Context context) {
    	ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    	NetworkInfo wifi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
	    
    	return wifi != null && wifi.isConnected();
    }

    public static String getUserToken(Context context) {
        SharedPreferences prefs = getPreferences(context);
        return prefs.getString(Constants.ApiToken, null);
    }

    public static void setUserToken(Context context, String token) {
        SharedPreferences prefs = getPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(Constants.ApiToken, token);
        editor.commit();
    }
    public static Long getLastAccessed(Context context) {
    	SharedPreferences prefs = getPreferences(context);
    	return prefs.getLong(Constants.SHARED_LASTACCESSED, -1);
    }
    public static void setLastAccessed(Context context, Long millis) {
        SharedPreferences prefs = getPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(Constants.SHARED_LASTACCESSED, millis);
        editor.commit();
    }
    public static String getUsername(Context context) {
    	SharedPreferences prefs = getPreferences(context);
    	return prefs.getString(Constants.SHARED_USERNAME, "");
    }
    public static void setUsername(Context context, String username) {
        SharedPreferences prefs = getPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(Constants.SHARED_USERNAME, username);
        editor.commit();
    }
    public static int getUserId(Context context) {
    	SharedPreferences prefs = getPreferences(context);
    	return prefs.getInt(Constants.SHARED_USERID, -1);
    }
    public static void setUserId(Context context, int userId) {
    	SharedPreferences prefs = getPreferences(context);
    	SharedPreferences.Editor editor = prefs.edit();
    	editor.putInt(Constants.SHARED_USERID, userId);
    	editor.commit();
    }
    public static boolean getIsDonor(Context context) {
    	SharedPreferences prefs = getPreferences(context);
    	return prefs.getBoolean(Constants.SHARED_IS_DONOR, false);
    }
    public static void setIsDonor(Context context, boolean isDonor) {
        SharedPreferences prefs = getPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(Constants.SHARED_IS_DONOR, isDonor);
        editor.commit();
    }
    
    public static boolean isScreenLitOnDownload(Context context) {
        SharedPreferences prefs = getPreferences(context);
        return prefs.getBoolean(Constants.PREFERENCES_KEY_SCREEN_LIT_ON_DOWNLOAD, false);
    }

    public static RepeatMode getRepeatMode(Context context) {
        SharedPreferences prefs = getPreferences(context);
        return RepeatMode.valueOf(prefs.getString(Constants.PREFERENCES_KEY_REPEAT_MODE, RepeatMode.OFF.name()));
    }

    public static void setRepeatMode(Context context, RepeatMode repeatMode) {
        SharedPreferences prefs = getPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(Constants.PREFERENCES_KEY_REPEAT_MODE, repeatMode.name());
        editor.commit();
    }

    public static boolean getShouldScrobble(Context context) {
    	SharedPreferences prefs = getPreferences(context);
    	return prefs.getBoolean(Constants.PREFERENCES_KEY_SCROBBLE, true);
    }

    public static void setSelectedMusicFolderId(Context context, String musicFolderId) {
        SharedPreferences prefs = getPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(Constants.PREFERENCES_KEY_MUSIC_FOLDER_ID, musicFolderId);
        editor.commit();
    }

    public static String getSelectedMusicFolderId(Context context) {
        SharedPreferences prefs = getPreferences(context);
        return prefs.getString(Constants.PREFERENCES_KEY_MUSIC_FOLDER_ID, null);
    }

    public static int getPreloadCount(Context context) {
        SharedPreferences prefs = getPreferences(context);
        int preloadCount = 5;
        try {
        	preloadCount = Integer.parseInt(prefs.getString(Constants.PREFERENCES_KEY_PRELOAD_COUNT, "5"));
        } catch (Exception ex) {
        	preloadCount = prefs.getInt(Constants.PREFERENCES_KEY_PRELOAD_COUNT, 5);
        }
        return preloadCount == -1 ? Integer.MAX_VALUE : preloadCount;
    }
    public static void setPreloadCount(Context context, int preloadCount) {
        SharedPreferences prefs = getPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(Constants.PREFERENCES_KEY_PRELOAD_COUNT, preloadCount + "");
        editor.commit();
    }

    public static int getCacheSizeMB(Context context) {
        SharedPreferences prefs = getPreferences(context);
        int cacheSize = Integer.parseInt(prefs.getString(Constants.PREFERENCES_KEY_CACHE_SIZE, "-1"));
        return cacheSize == -1 ? Integer.MAX_VALUE : cacheSize;
    }

    public static SharedPreferences getPreferences(Context context) {
    	return PreferenceManager.getDefaultSharedPreferences(context);
//        return context.getSharedPreferences(Constants.PREFERENCES_FILE_NAME, Context.MODE_PRIVATE);
    }

    public static String getContentType(HttpEntity entity) {
        if (entity == null || entity.getContentType() == null) {
            return null;
        }
        return entity.getContentType().getValue();
    }

    /**
     * Get the contents of an <code>InputStream</code> as a <code>byte[]</code>.
     * <p/>
     * This method buffers the input internally, so there is no need to use a
     * <code>BufferedInputStream</code>.
     *
     * @param input the <code>InputStream</code> to read from
     * @return the requested byte array
     * @throws NullPointerException if the input is null
     * @throws IOException          if an I/O error occurs
     */
    public static byte[] toByteArray(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        copy(input, output);
        return output.toByteArray();
    }

    public static long copy(InputStream input, OutputStream output)
            throws IOException {
        byte[] buffer = new byte[1024 * 4];
        long count = 0;
        int n;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

    public static void atomicCopy(File from, File to) throws IOException {
        FileInputStream in = null;
        FileOutputStream out = null;
        File tmp = null;
        try {
            tmp = new File(to.getPath() + ".tmp");
            in = new FileInputStream(from);
            out = new FileOutputStream(tmp);
            in.getChannel().transferTo(0, from.length(), out.getChannel());
            out.close();
            if (!tmp.renameTo(to)) {
                throw new IOException("Failed to rename " + tmp + " to " + to);
            }
            Log.i(TAG, "Copied " + from + " to " + to);
        } catch (IOException x) {
            close(out);
            delete(to);
            throw x;
        } finally {
            close(in);
            close(out);
            delete(tmp);
        }
    }

    public static void close(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (Throwable x) {
            // Ignored
        }
    }

    public static boolean delete(File file) {
        if (file != null && file.exists()) {
            if (!file.delete()) {
                Log.w(TAG, "Failed to delete file " + file);
                return false;
            }
            Log.i(TAG, "Deleted file " + file);
        }
        return true;
    }

    public static void toast(Context context, int messageId) {
        toast(context, messageId, true);
    }

    public static void toast(Context context, int messageId, boolean shortDuration) {
        toast(context, context.getString(messageId), shortDuration);
    }

    public static void toast(Context context, String message) {
        toast(context, message, true);
    }

    public static void toast(Context context, String message, boolean shortDuration) {
        if (toast == null) {
            toast = Toast.makeText(context, message, shortDuration ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
        } else {
            toast.setText(message);
            toast.setDuration(shortDuration ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG);
        }
        toast.show();
    }

    /**
     * Converts a byte-count to a formatted string suitable for display to the user.
     * For instance:
     * <ul>
     * <li><code>format(918)</code> returns <em>"918 B"</em>.</li>
     * <li><code>format(98765)</code> returns <em>"96 KB"</em>.</li>
     * <li><code>format(1238476)</code> returns <em>"1.2 MB"</em>.</li>
     * </ul>
     * This method assumes that 1 KB is 1024 bytes.
     * To get a localized string, please use formatLocalizedBytes instead.
     *
     * @param byteCount The number of bytes.
     * @return The formatted string.
     */
    public static synchronized String formatBytes(long byteCount) {

        // More than 1 GB?
        if (byteCount >= 1024 * 1024 * 1024) {
            NumberFormat gigaByteFormat = GIGA_BYTE_FORMAT;
            return gigaByteFormat.format((double) byteCount / (1024 * 1024 * 1024));
        }

        // More than 1 MB?
        if (byteCount >= 1024 * 1024) {
            NumberFormat megaByteFormat = MEGA_BYTE_FORMAT;
            return megaByteFormat.format((double) byteCount / (1024 * 1024));
        }

        // More than 1 KB?
        if (byteCount >= 1024) {
            NumberFormat kiloByteFormat = KILO_BYTE_FORMAT;
            return kiloByteFormat.format((double) byteCount / 1024);
        }

        return byteCount + " B";
    }

    /**
     * Converts a byte-count to a formatted string suitable for display to the user.
     * For instance:
     * <ul>
     * <li><code>format(918)</code> returns <em>"918 B"</em>.</li>
     * <li><code>format(98765)</code> returns <em>"96 KB"</em>.</li>
     * <li><code>format(1238476)</code> returns <em>"1.2 MB"</em>.</li>
     * </ul>
     * This method assumes that 1 KB is 1024 bytes.
     * This version of the method returns a localized string.
     *
     * @param byteCount The number of bytes.
     * @return The formatted string.
     */
    public static synchronized String formatLocalizedBytes(long byteCount, Context context) {

        // More than 1 GB?
        if (byteCount >= 1024 * 1024 * 1024) {
            if (GIGA_BYTE_LOCALIZED_FORMAT == null) {
                GIGA_BYTE_LOCALIZED_FORMAT = new DecimalFormat(context.getResources().getString(R.string.util_bytes_format_gigabyte));
            }

            return GIGA_BYTE_LOCALIZED_FORMAT.format((double) byteCount / (1024 * 1024 * 1024));
        }

        // More than 1 MB?
        if (byteCount >= 1024 * 1024) {
            if (MEGA_BYTE_LOCALIZED_FORMAT == null) {
                MEGA_BYTE_LOCALIZED_FORMAT = new DecimalFormat(context.getResources().getString(R.string.util_bytes_format_megabyte));
            }

            return MEGA_BYTE_LOCALIZED_FORMAT.format((double) byteCount / (1024 * 1024));
        }

        // More than 1 KB?
        if (byteCount >= 1024) {
            if (KILO_BYTE_LOCALIZED_FORMAT == null) {
                KILO_BYTE_LOCALIZED_FORMAT = new DecimalFormat(context.getResources().getString(R.string.util_bytes_format_kilobyte));
            }

            return KILO_BYTE_LOCALIZED_FORMAT.format((double) byteCount / 1024);
        }

        if (BYTE_LOCALIZED_FORMAT == null) {
            BYTE_LOCALIZED_FORMAT = new DecimalFormat(context.getResources().getString(R.string.util_bytes_format_byte));
        }

        return BYTE_LOCALIZED_FORMAT.format((double) byteCount);
    }

    public static String formatDuration(Integer seconds) {
        if (seconds == null) {
            return null;
        }

        int minutes = seconds / 60;
        int secs = seconds % 60;

        StringBuilder builder = new StringBuilder(6);
        builder.append(minutes).append(":");
        if (secs < 10) {
            builder.append("0");
        }
        builder.append(secs);
        return builder.toString();
    }

    public static boolean equals(Object object1, Object object2) {
        if (object1 == object2) {
            return true;
        }
        if (object1 == null || object2 == null) {
            return false;
        }
        return object1.equals(object2);

    }

    /**
     * Encodes the given string by using the hexadecimal representation of its UTF-8 bytes.
     *
     * @param s The string to encode.
     * @return The encoded string.
     */
    public static String utf8HexEncode(String s) {
        if (s == null) {
            return null;
        }
        byte[] utf8;
        try {
            utf8 = s.getBytes(Encoding.UTF_8.name());
        } catch (UnsupportedEncodingException x) {
            throw new RuntimeException(x);
        }
        return hexEncode(utf8);
    }

    /**
     * Converts an array of bytes into an array of characters representing the hexadecimal values of each byte in order.
     * The returned array will be double the length of the passed array, as it takes two characters to represent any
     * given byte.
     *
     * @param data Bytes to convert to hexadecimal characters.
     * @return A string containing hexadecimal characters.
     */
    public static String hexEncode(byte[] data) {
        int length = data.length;
        char[] out = new char[length << 1];
        // two characters form the hex value.
        for (int i = 0, j = 0; i < length; i++) {
            out[j++] = HEX_DIGITS[(0xF0 & data[i]) >>> 4];
            out[j++] = HEX_DIGITS[0x0F & data[i]];
        }
        return new String(out);
    }

    /**
     * Calculates the MD5 digest and returns the value as a 32 character hex string.
     *
     * @param s Data to digest.
     * @return MD5 digest as a hex string.
     */
    public static String md5Hex(String s) {
        if (s == null) {
            return null;
        }

        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            return hexEncode(md5.digest(s.getBytes(Encoding.UTF_8.name())));
        } catch (Exception x) {
            throw new RuntimeException(x.getMessage(), x);
        }
    }

    public static boolean isNetworkConnected(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        boolean connected = networkInfo != null && networkInfo.isConnected();

        boolean wifiConnected = connected && networkInfo.getType() == ConnectivityManager.TYPE_WIFI;
        boolean wifiRequired = isWifiRequiredForDownload(context);

        return connected && (!wifiRequired || wifiConnected);
    }

    public static boolean isExternalStoragePresent() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    private static boolean isWifiRequiredForDownload(Context context) {
    	SharedPreferences prefs = getPreferences(context);
    	return prefs.getBoolean(Constants.PREFERENCES_KEY_WIFI_REQUIRED_FOR_DOWNLOAD, false);
    }
    public static boolean getHighQualityWifi(Context context) {
    	SharedPreferences prefs = getPreferences(context);
    	String quality = prefs.getString(Constants.PREFERENCES_KEY_SONG_QUALITY_WIFI, "high");
    	if ("high".equals(quality))
    		return true;
    	return false;
    }
    public static boolean getHighQualityMobile(Context context) {
    	SharedPreferences prefs = getPreferences(context);
    	String quality = prefs.getString(Constants.PREFERENCES_KEY_SONG_QUALITY_MOBILE, "high");
    	if ("high".equals(quality))
    		return true;
    	return false;
    }

    public static void info(Context context, int titleId, int messageId) {
        showDialog(context, android.R.drawable.ic_dialog_info, titleId, messageId);
    }

    private static void showDialog(Context context, int icon, int titleId, int messageId) {
        new AlertDialog.Builder(context)
                .setIcon(icon)
                .setTitle(titleId)
                .setMessage(messageId)
                .setPositiveButton(R.string.common_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    public static void showPlayingNotification(final Context context, final DownloadServiceImpl downloadService, Handler handler, Track song) {

        // Use the same text for the ticker and the expanded notification
        String title = song.title;
        String text = song.artist;
        
        // Set the icon, scrolling text and timestamp
        final Notification notification = new Notification(R.drawable.notify_playing, title, System.currentTimeMillis());
        notification.flags |= Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;

        RemoteViews contentView = new RemoteViews(context.getPackageName(), R.layout.partial_notification);

		// set the text for the notifications
        contentView.setTextViewText(R.id.notification_title, title);
        contentView.setTextViewText(R.id.notification_artist, text);

        Pair<Integer, Integer> colors = getNotificationTextColors(context);
        if (colors.getFirst() != null) {
            contentView.setTextColor(R.id.notification_title, colors.getFirst());
        }
        if (colors.getSecond() != null) {
            contentView.setTextColor(R.id.notification_artist, colors.getSecond());
        }

        notification.contentView = contentView;
        
        // Send them to the main menu when if they click the notification
        // TODO: Send them to the concert, playlist, compilation details or chat page?
        Intent notificationIntent = new Intent(context, MainMenuActivity.class);
        notification.contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);

        // Send the notification and put the service in the foreground.
        handler.post(new Runnable() {
            @Override
            public void run() {
                startForeground(downloadService, Constants.NOTIFICATION_ID_PLAYING, notification);
            }
        });

        // Update widget
        DmbstreamAppWidgetProvider.getInstance().notifyChange(context, downloadService, true);
    }

    public static void hidePlayingNotification(final Context context, final DownloadServiceImpl downloadService, Handler handler) {

        // Remove notification and remove the service from the foreground
        handler.post(new Runnable() {
            @Override
            public void run() {
                stopForeground(downloadService, true);
            }
        });

        // Update widget
        DmbstreamAppWidgetProvider.getInstance().notifyChange(context, downloadService, false);
    }

    public static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException x) {
            Log.w(TAG, "Interrupted from sleep.", x);
        }
    }

    public static void startActivityWithoutTransition(Activity currentActivity, Class<? extends Activity> newActivitiy) {
        startActivityWithoutTransition(currentActivity, new Intent(currentActivity, newActivitiy));
    }

    public static void startActivityWithoutTransition(Activity currentActivity, Intent intent) {
        currentActivity.startActivity(intent);
        disablePendingTransition(currentActivity);
    }

    public static void disablePendingTransition(Activity activity) {

        // Activity.overridePendingTransition() was introduced in Android 2.0.  Use reflection to maintain
        // compatibility with 1.5.
        try {
            Method method = Activity.class.getMethod("overridePendingTransition", int.class, int.class);
            method.invoke(activity, 0, 0);
        } catch (Throwable x) {
            // Ignored
        }
    }

    public static Drawable createDrawableFromBitmap(Context context, Bitmap bitmap) {
        // BitmapDrawable(Resources, Bitmap) was introduced in Android 1.6.  Use reflection to maintain
        // compatibility with 1.5.
        try {
            Constructor<BitmapDrawable> constructor = BitmapDrawable.class.getConstructor(Resources.class, Bitmap.class);
            return constructor.newInstance(context.getResources(), bitmap);
        } catch (Throwable x) {
            return new BitmapDrawable(bitmap);
        }
    }

    public static void registerMediaButtonEventReceiver(Context context) {

        // Only do it if enabled in the settings.
        SharedPreferences prefs = getPreferences(context);
        boolean enabled = prefs.getBoolean(Constants.PREFERENCES_KEY_MEDIA_BUTTONS, true);

        if (enabled) {

            // AudioManager.registerMediaButtonEventReceiver() was introduced in Android 2.2.
            // Use reflection to maintain compatibility with 1.5.
            try {
                AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                ComponentName componentName = new ComponentName(context.getPackageName(), MediaButtonIntentReceiver.class.getName());
                Method method = AudioManager.class.getMethod("registerMediaButtonEventReceiver", ComponentName.class);
                method.invoke(audioManager, componentName);
            } catch (Throwable x) {
                // Ignored.
            }
        }
    }

    public static void unregisterMediaButtonEventReceiver(Context context) {
        // AudioManager.unregisterMediaButtonEventReceiver() was introduced in Android 2.2.
        // Use reflection to maintain compatibility with 1.5.
        try {
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            ComponentName componentName = new ComponentName(context.getPackageName(), MediaButtonIntentReceiver.class.getName());
            Method method = AudioManager.class.getMethod("unregisterMediaButtonEventReceiver", ComponentName.class);
            method.invoke(audioManager, componentName);
        } catch (Throwable x) {
            // Ignored.
        }
    }

    private static void startForeground(Service service, int notificationId, Notification notification) {
        // Service.startForeground() was introduced in Android 2.0.
        // Use reflection to maintain compatibility with 1.5.
        try {
            Method method = Service.class.getMethod("startForeground", int.class, Notification.class);
            method.invoke(service, notificationId, notification);
            Log.i(TAG, "Successfully invoked Service.startForeground()");
        } catch (Throwable x) {
            NotificationManager notificationManager = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(Constants.NOTIFICATION_ID_PLAYING, notification);
            Log.i(TAG, "Service.startForeground() not available. Using work-around.");
        }
    }

    private static void stopForeground(Service service, boolean removeNotification) {
        // Service.stopForeground() was introduced in Android 2.0.
        // Use reflection to maintain compatibility with 1.5.
        try {
            Method method = Service.class.getMethod("stopForeground", boolean.class);
            method.invoke(service, removeNotification);
            Log.i(TAG, "Successfully invoked Service.stopForeground()");
        } catch (Throwable x) {
            NotificationManager notificationManager = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(Constants.NOTIFICATION_ID_PLAYING);
            Log.i(TAG, "Service.stopForeground() not available. Using work-around.");
        }
    }

    /**
     * <p>Broadcasts the given song info as the new song being played.</p>
     */
    public static void broadcastNewTrackInfo(Context context, Track song) {
        Intent intent = new Intent(EVENT_META_CHANGED);

        if (song != null) {
            intent.putExtra("title", song.title);
            intent.putExtra("artist", song.artist);
            intent.putExtra("album", song.concert);
        } else {
            intent.putExtra("title", "");
            intent.putExtra("artist", "");
            intent.putExtra("album", "");
            intent.putExtra("coverart", "");
        }

        context.sendBroadcast(intent);
    }

    /**
     * <p>Broadcasts the given player state as the one being set.</p>
     */
    public static void broadcastPlaybackStatusChange(Context context, PlayerState state) {
        Intent intent = new Intent(EVENT_PLAYSTATE_CHANGED);

        switch (state) {
            case STARTED:
                intent.putExtra("state", "play");
                break;
            case STOPPED:
                intent.putExtra("state", "stop");
                break;
            case PAUSED:
                intent.putExtra("state", "pause");
                break;
            case COMPLETED:
                intent.putExtra("state", "complete");
                break;
            default:
                return; // No need to broadcast.
        }

        context.sendBroadcast(intent);
    }

    /**
     * Resolves the default text color for notifications.
     *
     * Based on http://stackoverflow.com/questions/4867338/custom-notification-layouts-and-text-colors/7320604#7320604
     */
    private static Pair<Integer, Integer> getNotificationTextColors(Context context) {
        if (NOTIFICATION_TEXT_COLORS.getFirst() == null && NOTIFICATION_TEXT_COLORS.getSecond() == null) {
            try {
                Notification notification = new Notification();
                String title = "title";
                String content = "content";
                notification.setLatestEventInfo(context, title, content, null);
                LinearLayout group = new LinearLayout(context);
                ViewGroup event = (ViewGroup) notification.contentView.apply(context, group);
                findNotificationTextColors(event, title, content);
                group.removeAllViews();
            } catch (Exception x) {
                Log.w(TAG, "Failed to resolve notification text colors.", x);
            }
        }
        return NOTIFICATION_TEXT_COLORS;
    }

    private static void findNotificationTextColors(ViewGroup group, String title, String content) {
        for (int i = 0; i < group.getChildCount(); i++) {
            if (group.getChildAt(i) instanceof TextView) {
                TextView textView = (TextView) group.getChildAt(i);
                String text = textView.getText().toString();
                if (title.equals(text)) {
                    NOTIFICATION_TEXT_COLORS.setFirst(textView.getTextColors().getDefaultColor());
                }
                else if (content.equals(text)) {
                    NOTIFICATION_TEXT_COLORS.setSecond(textView.getTextColors().getDefaultColor());
                }
            }
            else if (group.getChildAt(i) instanceof ViewGroup)
                findNotificationTextColors((ViewGroup) group.getChildAt(i), title, content);
        }
    }
}
