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
package com.dmbstream.android.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.os.PowerManager;
import android.util.Log;
import com.dmbstream.android.util.CancellableTask;
import com.dmbstream.android.util.FileUtil;
import com.dmbstream.android.util.Util;
import com.dmbstream.android.util.CacheCleaner;
import com.dmbstream.api.Track;

import org.apache.http.HttpResponse;

/**
 * @author Sindre Mehus
 * @version $Id$
 */
public class DownloadFile {

    private static final String TAG = DownloadFile.class.getSimpleName();
    private final Context context;
    private final Track song;
    private final File partialFile;
    private final File completeFile;

    private CancellableTask downloadTask;
    private boolean failed;
    private boolean isHighQuality;

    public DownloadFile(Context context, Track song) {
        this.context = context;
        this.song = song;
        isHighQuality = getIsHighQuality();
        completeFile = FileUtil.getSongFile(context, song);
        partialFile = new File(completeFile.getParent(), FileUtil.getBaseName(completeFile.getName()) +
                "." + ((isHighQuality) ? "high" : "low") + ".partial." + FileUtil.getExtension(completeFile.getName()));
    }

    public Track getSong() {
        return song;
    }

    public synchronized void download() {
        FileUtil.createDirectoryForParent(completeFile);
        failed = false;
        downloadTask = new DownloadTask();
        downloadTask.start();
    }

    public synchronized void cancelDownload() {
        if (downloadTask != null) {
            downloadTask.cancel();
        }
    }

    public File getCompleteFile() {
        return completeFile;
    }

    public File getPartialFile() {
        return partialFile;
    }

//    public boolean isSaved() {
//        return completeFile.exists();
//    }

    public synchronized boolean isCompleteFileAvailable() {
        return completeFile.exists();
    }

    public synchronized boolean isWorkDone() {
        return completeFile.exists();
    }

    public synchronized boolean isDownloading() {
        return downloadTask != null && downloadTask.isRunning();
    }

    public synchronized boolean isDownloadCancelled() {
        return downloadTask != null && downloadTask.isCancelled();
    }

    public boolean isFailed() {
        return failed;
    }

    public void delete() {
        cancelDownload();
        Util.delete(partialFile);
        Util.delete(completeFile);
    }

    public boolean cleanup() {
        boolean ok = true;
        if (completeFile.exists()) {
            ok = Util.delete(partialFile);
        }
        return ok;
    }

    // In support of LRU caching.
    public void updateModificationDate() {
        updateModificationDate(partialFile);
        updateModificationDate(completeFile);
    }

    private void updateModificationDate(File file) {
        if (file.exists()) {
            boolean ok = file.setLastModified(System.currentTimeMillis());
            if (!ok) {
                Log.w(TAG, "Failed to set last-modified date on " + file);
            }
        }
    }

    @Override
    public String toString() {
        return "DownloadFile (" + song + ")";
    }

    private class DownloadTask extends CancellableTask {

        @Override
        public void execute() {

            InputStream in = null;
            FileOutputStream out = null;
            PowerManager.WakeLock wakeLock = null;
            try {

                if (Util.isScreenLitOnDownload(context)) {
                    PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                    wakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, toString());
                    wakeLock.acquire();
                    Log.i(TAG, "Acquired wake lock " + wakeLock);
                }

                if (completeFile.exists()) {
                    Log.i(TAG, completeFile + " already exists. Skipping.");
                    return;
                }

                IMusicService musicService = MusicServiceFactory.getMusicService(context);

                // Attempt partial HTTP GET, appending to the file if it exists.
                HttpResponse response = musicService.getDownloadInputStream(context, song, isHighQuality, DownloadTask.this);
                in = response.getEntity().getContent();

                out = new FileOutputStream(partialFile);
                long n = copy(in, out);
                Log.i(TAG, "Downloaded " + n + " bytes to " + partialFile);
                out.flush();
                out.close();

                if (isCancelled()) {
                    throw new Exception("Download of '" + song + "' was cancelled");
                }

                Util.atomicCopy(partialFile, completeFile);

            } catch (Exception x) {
                Util.close(out);
                Util.delete(completeFile);
                if (!isCancelled()) {
                    failed = true;
                    Log.w(TAG, "Failed to download '" + song + "'.", x);
                }

            } finally {
                Util.close(in);
                Util.close(out);
                if (wakeLock != null) {
                    wakeLock.release();
                    Log.i(TAG, "Released wake lock " + wakeLock);
                }
                new CacheCleaner(context, DownloadServiceImpl.getInstance()).clean();
            }
        }

        @Override
        public String toString() {
            return "DownloadTask (" + song + ")";
        }

        private long copy(final InputStream in, OutputStream out) throws IOException, InterruptedException {

            // Start a thread that will close the input stream if the task is
            // cancelled, thus causing the copy() method to return.
            new Thread() {
                @Override
                public void run() {
                    while (true) {
                        Util.sleepQuietly(3000L);
                        if (isCancelled()) {
                            Util.close(in);
                            return;
                        }
                        if (!isRunning()) {
                            return;
                        }
                    }
                }
            }.start();

            byte[] buffer = new byte[1024 * 16];
            long count = 0;
            int n;
            long lastLog = System.currentTimeMillis();

            while (!isCancelled() && (n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n);
                count += n;

                long now = System.currentTimeMillis();
                if (now - lastLog > 3000L) {  // Only every so often.
                    Log.i(TAG, "Downloaded " + Util.formatBytes(count) + " of " + song);
                    lastLog = now;
                }
            }
            return count;
        }
    }

    private boolean isSongQualitySet = false;
	public boolean getIsHighQuality() {
		if (isSongQualitySet)
			return isHighQuality;
		
		isSongQualitySet = true;
		if (Util.isWifiConnected(context)) {
			return Util.getHighQualityWifi(context);
		}
		return Util.getHighQualityMobile(context);
	}
}
