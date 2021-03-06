/*
 This file is part of Subsonic.

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

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Iterator;
import java.util.List;

import android.content.Context;
import android.content.res.Resources;
import android.os.Environment;
import android.util.Log;
import com.dmbstream.api.Track;

/**
 * @author Sindre Mehus
 */
public class FileUtil {

	private static final String TAG = FileUtil.class.getSimpleName();
	private static final String[] FILE_SYSTEM_UNSAFE = { "/", "\\", "..", ":",
			"\"", "?", "*", "<", ">" };
	private static final String[] FILE_SYSTEM_UNSAFE_DIR = { "\\", "..", ":",
			"\"", "?", "*", "<", ">" };
	private static final List<String> MUSIC_FILE_EXTENSIONS = Arrays.asList(
			"mp3", "ogg", "aac", "flac", "m4a", "wav", "wma");
	private static final File DEFAULT_MUSIC_DIR = createDirectory("music");

	public static File getSongFile(Context context, Track song) {
		File dir = getAlbumDirectory(context, song);

		return new File(dir, fileSystemSafe(song.id) + ".mp3");
	}

	private static File getAlbumDirectory(Context context, Track song) {
		String concert = fileSystemSafe(song.concertId);
		return new File(getMusicDirectory(context).getPath() + "/" + concert);
	}

	public static void createDirectoryForParent(File file) {
		File dir = file.getParentFile();
		if (!dir.exists()) {
			if (!dir.mkdirs()) {
				Log.e(TAG, "Failed to create directory " + dir);
			}
		}
	}

	private static File createDirectory(String name) {
		File dir = new File(getSubsonicDirectory(), name);
		if (!dir.exists() && !dir.mkdirs()) {
			Log.e(TAG, "Failed to create " + name);
		}
		return dir;
	}

	public static File getSubsonicDirectory() {
		return new File(Environment.getExternalStorageDirectory(), "dmbstream");
	}

	public static File getDefaultMusicDirectory() {
		return DEFAULT_MUSIC_DIR;
	}

	public static File getMusicDirectory(Context context) {
		String path = Util.getPreferences(context).getString(Constants.PREFERENCES_KEY_CACHE_LOCATION, DEFAULT_MUSIC_DIR.getPath());
		File dir = new File(path);
		return ensureDirectoryExistsAndIsReadWritable(dir) ? dir : getDefaultMusicDirectory();
	}

	public static boolean ensureDirectoryExistsAndIsReadWritable(File dir) {
		if (dir == null) {
			return false;
		}

		if (dir.exists()) {
			if (!dir.isDirectory()) {
				Log.w(TAG, dir + " exists but is not a directory.");
				return false;
			}
		} else {
			if (dir.mkdirs()) {
				Log.i(TAG, "Created directory " + dir);
			} else {
				Log.w(TAG, "Failed to create directory " + dir);
				return false;
			}
		}

		if (!dir.canRead()) {
			Log.w(TAG, "No read permission for directory " + dir);
			return false;
		}

		if (!dir.canWrite()) {
			Log.w(TAG, "No write permission for directory " + dir);
			return false;
		}
		return true;
	}

	/**
	 * Makes a given filename safe by replacing special characters like slashes
	 * ("/" and "\") with dashes ("-").
	 * 
	 * @param filename
	 *            The filename in question.
	 * @return The filename with special characters replaced by hyphens.
	 */
	private static String fileSystemSafe(String filename) {
		if (filename == null || filename.trim().length() == 0) {
			return "unnamed";
		}

		for (String s : FILE_SYSTEM_UNSAFE) {
			filename = filename.replace(s, "-");
		}
		return filename;
	}

	/**
	 * Makes a given filename safe by replacing special characters like colons
	 * (":") with dashes ("-").
	 * 
	 * @param path
	 *            The path of the directory in question.
	 * @return The the directory name with special characters replaced by
	 *         hyphens.
	 */
	private static String fileSystemSafeDir(String path) {
		if (path == null || path.trim().length() == 0) {
			return "";
		}

		for (String s : FILE_SYSTEM_UNSAFE_DIR) {
			path = path.replace(s, "-");
		}
		return path;
	}

	public static CharSequence readFile(Resources resources, int id) {
		BufferedReader in = null;
		try {
			in = new BufferedReader(new InputStreamReader(resources.openRawResource(id)), 8192);
			String line;
			StringBuilder buffer = new StringBuilder();
			while ((line = in.readLine()) != null) {
				buffer.append(line).append('\n');
			}
			// Chomp the last newline
			if (buffer.length() > 0) {
				buffer.deleteCharAt(buffer.length() - 1);
			}
			return buffer;
		} catch (IOException e) {
			return "";
		} finally {
			closeStream(in);
		}
	}

	public static CharSequence readFile(Resources resources, String filename) {
		return readFile(resources, resources.getIdentifier(filename, null, null));
	}

	/**
	 * Closes the specified stream.
	 * 
	 * @param stream
	 *            The stream to close.
	 */
	private static void closeStream(Closeable stream) {
		if (stream != null) {
			try {
				stream.close();
			} catch (IOException e) {
				// Ignore
			}
		}
	}

	/**
	 * Similar to {@link File#listFiles()}, but returns a sorted set. Never
	 * returns {@code null}, instead a warning is logged, and an empty set is
	 * returned.
	 */
	public static SortedSet<File> listFiles(File dir) {
		File[] files = dir.listFiles();
		if (files == null) {
			Log.w(TAG, "Failed to list children for " + dir.getPath());
			return new TreeSet<File>();
		}

		return new TreeSet<File>(Arrays.asList(files));
	}

	public static SortedSet<File> listMusicFiles(File dir) {
		SortedSet<File> files = listFiles(dir);
		Iterator<File> iterator = files.iterator();
		while (iterator.hasNext()) {
			File file = iterator.next();
			if (!file.isDirectory() && !isMusicFile(file)) {
				iterator.remove();
			}
		}
		return files;
	}

	private static boolean isMusicFile(File file) {
		String extension = getExtension(file.getName());
		return MUSIC_FILE_EXTENSIONS.contains(extension);
	}

	/**
	 * Returns the extension (the substring after the last dot) of the given
	 * file. The dot is not included in the returned extension.
	 * 
	 * @param name
	 *            The filename in question.
	 * @return The extension, or an empty string if no extension is found.
	 */
	public static String getExtension(String name) {
		int index = name.lastIndexOf('.');
		return index == -1 ? "" : name.substring(index + 1).toLowerCase();
	}

	/**
	 * Returns the base name (the substring before the last dot) of the given
	 * file. The dot is not included in the returned basename.
	 * 
	 * @param name
	 *            The filename in question.
	 * @return The base name, or an empty string if no basename is found.
	 */
	public static String getBaseName(String name) {
		int index = name.lastIndexOf('.');
		return index == -1 ? name : name.substring(0, index);
	}

	public static <T extends Serializable> boolean serialize(Context context,
			T obj, String fileName) {
		File file = new File(context.getCacheDir(), fileName);
		ObjectOutputStream out = null;
		try {
			out = new ObjectOutputStream(new FileOutputStream(file));
			out.writeObject(obj);
			Log.i(TAG, "Serialized object to " + file);
			return true;
		} catch (Throwable x) {
			Log.w(TAG, "Failed to serialize object to " + file);
			return false;
		} finally {
			Util.close(out);
		}
	}

	public static <T extends Serializable> T deserialize(Context context,
			String fileName) {
		File file = new File(context.getCacheDir(), fileName);
		if (!file.exists() || !file.isFile()) {
			return null;
		}

		ObjectInputStream in = null;
		try {
			in = new ObjectInputStream(new FileInputStream(file));
			T result = (T) in.readObject();
			Log.i(TAG, "Deserialized object from " + file);
			return result;
		} catch (Throwable x) {
			Log.w(TAG, "Failed to deserialize object from " + file, x);
			return null;
		} finally {
			Util.close(in);
		}
	}

	public static void hideMedia(File dir) {
		// Hide the media from being scanned by other programs
		File nomediaDir = new File(dir, ".nomedia");
		if (!nomediaDir.exists()) {
			if (!nomediaDir.mkdir()) {
				Log.w(TAG, "Failed to create " + nomediaDir);
			}
		}
	}
}
