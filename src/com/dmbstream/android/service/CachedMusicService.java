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

import org.apache.http.HttpResponse;

import android.content.Context;
import com.dmbstream.android.util.CancellableTask;
import com.dmbstream.android.util.LRUCache;
import com.dmbstream.android.util.TimeLimitedCache;
import com.dmbstream.api.Track;

public class CachedMusicService implements IMusicService {

    private static final int MUSIC_DIR_CACHE_SIZE = 20;
    private static final int TTL_MUSIC_DIR = 5 * 60; // Five minutes

    private final IMusicService musicService;
    private final LRUCache<String, TimeLimitedCache<Track>> cachedMusicDirectories;
    private String restUrl;

    public CachedMusicService(IMusicService musicService) {
        this.musicService = musicService;
        cachedMusicDirectories = new LRUCache<String, TimeLimitedCache<Track>>(MUSIC_DIR_CACHE_SIZE);
    }

    @Override
    public HttpResponse getDownloadInputStream(Context context, Track song, boolean isHighQuality, CancellableTask task) throws Exception {
        return musicService.getDownloadInputStream(context, song, isHighQuality, task);
    }
}
