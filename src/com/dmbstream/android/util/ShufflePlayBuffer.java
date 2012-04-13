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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.util.Log;
import com.dmbstream.api.Track;

/**
 * @author Sindre Mehus
 * @version $Id: ShufflePlayBuffer.java 1951 2010-11-25 12:55:01Z sindre_mehus $
 */
public class ShufflePlayBuffer {

    private static final String TAG = ShufflePlayBuffer.class.getSimpleName();
    private final ScheduledExecutorService executorService;
    private final List<Track> buffer = new ArrayList<Track>();
    public ShufflePlayBuffer(Context context) {
        executorService = Executors.newSingleThreadScheduledExecutor();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                refill();
            }
        };
        executorService.scheduleWithFixedDelay(runnable, 1, 10, TimeUnit.SECONDS);
    }

    public List<Track> get(int size) {

        List<Track> result = new ArrayList<Track>(size);
        synchronized (buffer) {
            while (!buffer.isEmpty() && result.size() < size) {
                result.add(buffer.remove(buffer.size() - 1));
            }
        }
        Log.i(TAG, "Taking " + result.size() + " songs from shuffle play buffer. " + buffer.size() + " remaining.");
        return result;
    }

    public void shutdown() {
        executorService.shutdown();
    }

    private void refill() {

/*        if (buffer.size() > REFILL_THRESHOLD || (!Util.isNetworkConnected(context) && !Util.isOffline(context))) {
            return;
        }

        try {
            IMusicService service = MusicServiceFactory.getMusicService(context);
            int n = CAPACITY - buffer.size();
            Track songs = service.getRandomSongs(n, context, null);

            synchronized (buffer) {
                buffer.addAll(songs.getChildren());
                Log.i(TAG, "Refilled shuffle play buffer with " + songs.getChildren().size() + " songs.");
            }
        } catch (Exception x) {
            Log.w(TAG, "Failed to refill shuffle play buffer.", x);
        }
*/    }

}
