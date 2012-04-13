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

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Checkable;
import android.widget.CheckedTextView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.util.WeakHashMap;

import com.dmbstream.android.R;
import com.dmbstream.android.service.DownloadFile;
import com.dmbstream.android.service.DownloadServiceImpl;
import com.dmbstream.android.service.IDownloadService;
import com.dmbstream.api.Track;

/**
 * Used to display songs in a {@code ListView}.
 *
 * @author Sindre Mehus
 */
public class SongView extends LinearLayout implements Checkable {

    private static final String TAG = SongView.class.getSimpleName();
    private static final WeakHashMap<SongView, ?> INSTANCES = new WeakHashMap<SongView, Object>();
    private static Handler handler;

    private CheckedTextView checkedTextView;
    private TextView titleTextView;
    private TextView artistTextView;
    private TextView durationTextView;
    private TextView statusTextView;
    private Track song;

    public SongView(Context context) {
        super(context);
        LayoutInflater.from(context).inflate(R.layout.partial_song_list_item, this, true);

        checkedTextView = (CheckedTextView) findViewById(R.id.song_check);
        titleTextView = (TextView) findViewById(R.id.song_title);
        artistTextView = (TextView) findViewById(R.id.song_artist);
        durationTextView = (TextView) findViewById(R.id.song_duration);
        statusTextView = (TextView) findViewById(R.id.song_status);

        INSTANCES.put(this, null);
        int instanceCount = INSTANCES.size();
        if (instanceCount > 50) {
            Log.w(TAG, instanceCount + " live SongView instances");
        }
        startUpdater();
    }

    public void setSong(Track song, boolean checkable) {
        this.song = song;

        titleTextView.setText(song.title);
        artistTextView.setText(song.artistAbbreviation + " :: " + song.concert);
        durationTextView.setText(Util.formatDuration(song.getDurationAsSeconds()));
        checkedTextView.setVisibility(checkable ? View.VISIBLE : View.GONE);

        update();
    }

    private void update() {
        IDownloadService downloadService = DownloadServiceImpl.getInstance();
        if (downloadService == null) {
            return;
        }

        DownloadFile downloadFile = downloadService.forSong(song);
        File completeFile = downloadFile.getCompleteFile();
        File partialFile = downloadFile.getPartialFile();

        int leftImage = 0;
        int rightImage = 0;

        if (completeFile.exists()) {
            leftImage = R.drawable.saved;
        }

        if (downloadFile.isDownloading() && !downloadFile.isDownloadCancelled() && partialFile.exists()) {
            statusTextView.setText(Util.formatLocalizedBytes(partialFile.length(), getContext()));
            rightImage = R.drawable.downloading;
        } else {
            statusTextView.setText(null);
        }
        statusTextView.setCompoundDrawablesWithIntrinsicBounds(leftImage, 0, rightImage, 0);

        boolean playing = downloadService.getCurrentPlaying() == downloadFile;
        if (playing) {
            titleTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.stat_notify_playing, 0, 0, 0);
        } else {
            titleTextView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        }
    }

    private static synchronized void startUpdater() {
        if (handler != null) {
            return;
        }

        handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                updateAll();
                handler.postDelayed(this, 1000L);
            }
        };
        handler.postDelayed(runnable, 1000L);
    }

    private static void updateAll() {
        try {
            for (SongView view : INSTANCES.keySet()) {
                if (view.isShown()) {
                    view.update();
                }
            }
        } catch (Throwable x) {
            Log.w(TAG, "Error when updating song views.", x);
        }
    }

    @Override
    public void setChecked(boolean b) {
        checkedTextView.setChecked(b);
    }

    @Override
    public boolean isChecked() {
        return checkedTextView.isChecked();
    }

    @Override
    public void toggle() {
        checkedTextView.toggle();
    }
}