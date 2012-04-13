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

import java.util.List;

import com.dmbstream.android.model.PlayerState;
import com.dmbstream.android.model.RepeatMode;
import com.dmbstream.api.Track;

/**
 * @author Sindre Mehus
 * @version $Id$
 */
/**
 * @author Jim
 *
 */
public interface IDownloadService {

    void queue(List<Track> songs, boolean autoplay);

    void setShufflePlayEnabled(boolean enabled);

    boolean isShufflePlayEnabled();

    void shuffle();

    RepeatMode getRepeatMode();

    void setRepeatMode(RepeatMode repeatMode);

    boolean getKeepScreenOn();

    void setKeepScreenOn(boolean screenOn);

    void clear();

    void clearIncomplete();

    int size();

    void remove(DownloadFile downloadFile);

    List<DownloadFile> getDownloads();

    int getCurrentPlayingIndex();

    DownloadFile getCurrentPlaying();

    DownloadFile getCurrentDownloading();

	void togglePlayPause();

    void play(int index);

    void seekTo(int position);

    void previous();

    void next();

    void pause();

    void start();

    void reset();

    PlayerState getPlayerState();

    int getPlayerPosition();

    int getPlayerDuration();

    void delete(List<Track> songs);
    
    /**
     * Get the File object for the specified Track object
     * @param song
     * @return
     */
    DownloadFile forSong(Track song);

    long getDownloadListUpdateRevision();

    void setSuggestedPlaylistName(String name);

    String getSuggestedPlaylistName();
}
