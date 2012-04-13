// Copyright 2009 Google Inc.
// Copyright 2011 NPR
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.dmbstream.android.view;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.*;
import android.widget.*;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.SlidingDrawer.OnDrawerCloseListener;
import android.widget.SlidingDrawer.OnDrawerOpenListener;

import com.dmbstream.android.R;
import com.dmbstream.android.model.PlayerState;
import com.dmbstream.android.service.DownloadFile;
import com.dmbstream.android.service.DownloadServiceImpl;
import com.dmbstream.android.service.IDownloadService;
import com.dmbstream.android.util.AnalyticsUtil;
import com.dmbstream.android.util.DisplayUtils;
import com.dmbstream.android.util.SongView;
import com.dmbstream.android.util.Util;
import com.dmbstream.android.widget.DragNDropListView;
import com.dmbstream.api.Track;

/*
 * Represents a partial view that contains player functionality and the playlist
 */
public class PlaylistView extends FrameLayout implements 
		OnSeekBarChangeListener, OnDrawerOpenListener, OnDrawerCloseListener {

	private static final String TAG = PlaylistView.class.getSimpleName();

	private final Context context;

	private SlidingDrawer drawer;
	private RelativeLayout handle;

	private RelativeLayout playerContracted;
	private RelativeLayout playerExpanded;

	private TextView itemTitle1;
	private TextView itemTitle2;
	private TextView contractedItemTitle1;
	private TextView contractedItemTitle2;
	private TextView contractedItemTitle3;

	private SeekBar progressBar;
	private TextView positionTextView;
	private TextView durationTextView;

	private boolean playPauseShowsPlay;
	private ImageButton rewindButton;
	private ImageButton rewind30Button;
	private ImageButton playPauseButton;
	private ImageButton fastForwardButton;
	private ImageButton contractedPlayButton;

	private DragNDropListView listView;
	private Button clearPlaylist;

	private boolean enablePlaybackControls;

	private int touchSlop;
	private int startX;
	private int startY;
	private boolean cancelDown;
	
	private ScheduledExecutorService executorService;

	private enum ClickedItem {
		rewind, rewind30, playPause, fastForward, contractedPlay, progressbar
	}

	private ClickedItem clickedItem;

	private DownloadFile currentPlaying;

	private long currentRevision;

	private TextView emptyTextView;

	private boolean hasInit;

	private boolean showingSpinners;

	@SuppressWarnings({ "UnusedDeclaration" })
	public PlaylistView(Context context) {
		super(context);
		this.context = context;
	}

	@SuppressWarnings({ "UnusedDeclaration" })
	public PlaylistView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;
	}

	@SuppressWarnings({ "UnusedDeclaration" })
	public PlaylistView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		this.context = context;
	}

    public IDownloadService getDownloadService() {
        // If service is not available, request it to start and wait for it.
        for (int i = 0; i < 5; i++) {
            IDownloadService downloadService = DownloadServiceImpl.getInstance();
            if (downloadService != null) {
                return downloadService;
            }
            Log.w(TAG, "DownloadService not running. Attempting to start it.");
            context.startService(new Intent(context, DownloadServiceImpl.class));
            Util.sleepQuietly(50L);
        }
        return DownloadServiceImpl.getInstance();
    }

	/**
	 * Returns a pointer to the SlidingDrawer for the player window.
	 * 
	 * @return The player's SlidingDrawer
	 */
	public SlidingDrawer getPlayerDrawer() {
		return drawer;
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		init();
	}

	// Think of this as onCreate
	private void init() {
        Log.d(TAG, "init");
		hasInit = true;
		ViewGroup.inflate(context, R.layout.partial_playlist, this);

		drawer = (SlidingDrawer) findViewById(R.id.drawer);
		drawer.setOnDrawerOpenListener(this);
		drawer.setOnDrawerCloseListener(this);
		touchSlop = ViewConfiguration.getTouchSlop();
		handle = (RelativeLayout) findViewById(R.id.handle);

		playerContracted = (RelativeLayout) findViewById(R.id.player_contracted);
		playerExpanded = (RelativeLayout) findViewById(R.id.player_expanded);

//		playerStatus = (TextView) findViewById(R.id.status);
//		contractedPlayerStatus = (TextView) findViewById(R.id.contracted_status);

		itemTitle1 = (TextView) findViewById(R.id.item_title1);
		itemTitle2 = (TextView) findViewById(R.id.item_title2);
		contractedItemTitle1 = (TextView) findViewById(R.id.contracted_item_title1);
		contractedItemTitle2 = (TextView) findViewById(R.id.contracted_item_title2);
		contractedItemTitle3 = (TextView) findViewById(R.id.contracted_item_title3);

		progressBar = (SeekBar) findViewById(R.id.stream_progress_bar);
		progressBar.setOnSeekBarChangeListener(this);
		positionTextView = (TextView) findViewById(R.id.download_position);
        durationTextView = (TextView) findViewById(R.id.download_duration);

		playPauseShowsPlay = true;
		contractedPlayButton = (ImageButton) findViewById(R.id.contracted_play_pause);
		rewindButton = (ImageButton) findViewById(R.id.stream_rewind_button);
		rewind30Button = (ImageButton) findViewById(R.id.stream_rewind_30_button);
		playPauseButton = (ImageButton) findViewById(R.id.stream_play_pause_button);
		fastForwardButton = (ImageButton) findViewById(R.id.stream_fastforward_button);

		clearPlaylist = (Button) findViewById(R.id.clear_playlist);

        emptyTextView = (TextView) findViewById(R.id.download_empty);
		listView = (DragNDropListView) findViewById(R.id.playlist);
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    			AnalyticsUtil.trackEvent(context, "Player", "Click", "TrackItem", 0);
                play(position);
            }
        });
        
		if (context instanceof Activity) {
			((Activity) context).registerForContextMenu(listView);
		}

		clearPlaylist.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.v(TAG, "clearPlaylist::Click");
    			AnalyticsUtil.trackEvent(context, "Player", "Click", "ClearPlaylist", 0);
				if (getDownloadService() != null) {
					getDownloadService().clear();
				}
			}
		});
	}

	public void onResume() {
        Log.d(TAG, "onResume");
        final Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        update();
                    }
                });
            }
        };

        executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleWithFixedDelay(runnable, 0L, 1000L, TimeUnit.MILLISECONDS);

        if (!hasInit)
        	return;

        onDownloadListChanged();
        onCurrentChanged();
        onProgressChanged();
        scrollToCurrent();

        IDownloadService downloadService = getDownloadService();
        if (context instanceof Activity) {
            if (downloadService != null && downloadService.getKeepScreenOn()) {
                ((Activity)context).getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            } else {
            	((Activity)context).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
		}
	}
	public void onPause() {
        Log.d(TAG, "onPause");
		if (executorService != null) {
	        executorService.shutdown();
			executorService = null;
		}		
	}
	
	public DownloadFile getItemAtPosition(int position) {
		return (DownloadFile) listView.getItemAtPosition(position);
	}

    // Scroll to current playing/downloading.
    private void scrollToCurrent() {
        if (getDownloadService() == null) {
            return;
        }

        if (listView.getAdapter() == null)
        	return;
        
        if (currentPlaying != null) {
	        for (int i = 0; i < listView.getAdapter().getCount(); i++) {
	            if (currentPlaying == listView.getItemAtPosition(i)) {
	            	listView.setSelectionFromTop(i, 40);
	                return;
	            }
	        }
        }
        DownloadFile currentDownloading = getDownloadService().getCurrentDownloading();
        for (int i = 0; i < listView.getAdapter().getCount(); i++) {
            if (currentDownloading == listView.getItemAtPosition(i)) {
            	listView.setSelectionFromTop(i, 40);
                return;
            }
        }
    }

    protected void warnIfNetworkOrStorageUnavailable() {
        if (!Util.isExternalStoragePresent()) {
            Util.toast(context, R.string.select_album_no_sdcard);
        } else if (!Util.isOffline(context) && !Util.isNetworkConnected(context)) {
            Util.toast(context, R.string.select_album_no_network);
        }
    }

	private void update() {
        if (getDownloadService() == null) {
            return;
        }

        if (currentRevision != getDownloadService().getDownloadListUpdateRevision()) {
            onDownloadListChanged();
        }

        if (currentPlaying != getDownloadService().getCurrentPlaying()) {
            onCurrentChanged();
        }

        onProgressChanged();
    }

    private void onDownloadListChanged() {
        IDownloadService downloadService = getDownloadService();
        if (downloadService == null) {
            return;
        }
        if (listView == null)
        	return;

        Log.d(TAG, "onDownloadListChanged");
        
        List<DownloadFile> list = downloadService.getDownloads();

        listView.setAdapter(new SongListAdapter(list));
        emptyTextView.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        currentRevision = downloadService.getDownloadListUpdateRevision();

//        switch (downloadService.getRepeatMode()) {
//            case OFF:
//                repeatButton.setImageResource(R.drawable.media_repeat_off);
//                break;
//            case ALL:
//                repeatButton.setImageResource(R.drawable.media_repeat_all);
//                break;
//            case SINGLE:
//                repeatButton.setImageResource(R.drawable.media_repeat_single);
//                break;
//            default:
//                break;
//        }
    }
 
    private void onProgressChanged() {
        if (getDownloadService() == null) {
            return;
        }

        if (positionTextView == null)
        	return;
        
        if (currentPlaying != null) {

            int millisPlayed = Math.max(0, getDownloadService().getPlayerPosition());
            Integer duration = getDownloadService().getPlayerDuration();
            int millisTotal = duration == null ? 0 : duration;

            positionTextView.setText(Util.formatDuration(millisPlayed / 1000));
            durationTextView.setText(Util.formatDuration(millisTotal / 1000));
            progressBar.setMax(millisTotal == 0 ? 100 : millisTotal); // Work-around for apparent bug.
            progressBar.setProgress(millisPlayed);
//            progressBar.setSlidingEnabled(currentPlaying.isCompleteFileAvailable());
            enablePlaybackControls = true;
            if (showingSpinners && currentPlaying.isCompleteFileAvailable())
            	stopPlaylistSpinners();
        } else {
            positionTextView.setText("0:00");
            durationTextView.setText("-:--");
            progressBar.setProgress(0);
//            progressBar.setSlidingEnabled(false);
            enablePlaybackControls = false;
            if (showingSpinners)
            	stopPlaylistSpinners();
        }

        PlayerState playerState = getDownloadService().getPlayerState();

        switch (playerState) {
            case DOWNLOADING:
            case PREPARING:
                startPlaylistSpinners();
                break;
            case IDLE:
            case PAUSED:
            	if (showingSpinners)
            		stopPlaylistSpinners();
            	if (!playPauseShowsPlay) {
            		playPauseShowsPlay = true;
            		showPlayPause(false);
            	}
            	break;
            case STARTED:
                if (showingSpinners)
                	stopPlaylistSpinners();
                if (playPauseShowsPlay) {
	                playPauseShowsPlay = false;
	                showPlayPause(false);
                }
                break;
        }

//        switch (playerState) {
//            case STARTED:
//                pauseButton.setVisibility(View.VISIBLE);
//                stopButton.setVisibility(View.GONE);
//                startButton.setVisibility(View.GONE);
//                break;
//            case DOWNLOADING:
//            case PREPARING:
//                pauseButton.setVisibility(View.GONE);
//                stopButton.setVisibility(View.VISIBLE);
//                startButton.setVisibility(View.GONE);
//                break;
//            default:
//                pauseButton.setVisibility(View.GONE);
//                stopButton.setVisibility(View.GONE);
//                startButton.setVisibility(View.VISIBLE);
//                break;
//        }
    }

    private class SongListAdapter extends ArrayAdapter<DownloadFile> {
        public SongListAdapter(List<DownloadFile> entries) {
            super(context, android.R.layout.simple_list_item_1, entries);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            SongView view;
            if (convertView != null && convertView instanceof SongView) {
                view = (SongView) convertView;
            } else {
                view = new SongView(context);
            }
            DownloadFile downloadFile = getItem(position);
            view.setSong(downloadFile.getSong(), false);
            return view;
        }
    }
	
    private void onCurrentChanged() {
        if (getDownloadService() == null) {
            return;
        }
        
        if (itemTitle1 == null)
        	return;

        currentPlaying = getDownloadService().getCurrentPlaying();
        if (currentPlaying != null) {
            Track song = currentPlaying.getSong();
            itemTitle1.setText(song.title);            
            itemTitle2.setText(song.artistAbbreviation + " :: " + song.concert);
            contractedItemTitle1.setText(song.title);
            contractedItemTitle2.setText(song.concert);
            contractedItemTitle3.setText(song.artist);
        } else {
            itemTitle1.setText(null);            
            itemTitle2.setText(null);
            contractedItemTitle1.setText(null);
            contractedItemTitle2.setText(null);
            contractedItemTitle3.setText(null);
        }
    }	

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		// Emulator calls detach twice, so clear receiver
		if (executorService != null) {
	        executorService.shutdown();
			executorService = null;
		}
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		seekBar.setProgress(progress);
        if (getDownloadService() == null) {
            return;
        }
		if (fromUser) {
			getDownloadService().seekTo(progress);
		}
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
	}

	@Override
	public void onDrawerOpened() {
		handle.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, DisplayUtils.convertToDIP(context, 150)));
		playerContracted.setVisibility(View.GONE);
		playerExpanded.setVisibility(View.VISIBLE);
		scrollToCurrent();
	}

	@Override
	public void onDrawerClosed() {
		handle.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, DisplayUtils.convertToDIP(context, 95)));
		playerContracted.setVisibility(View.VISIBLE);
		playerExpanded.setVisibility(View.GONE);
	}

	public boolean isExpanded() {
		return drawer.isOpened();
	}

	public void setExpanded(boolean expanded) {
		if (expanded) {
			drawer.animateOpen();
		} else {
			drawer.animateClose();
		}
	}

	private boolean ViewContainsXY(View v, int x, int y) {
		Rect r = new Rect();
		v.getDrawingRect(r);
		offsetDescendantRectToMyCoords(v, r);
		return r.contains(x, y);
	}

	private void closeDrawerIfPastThreshold(int y) {
		Rect r = new Rect();
		drawer.getDrawingRect(r);
		if ((y - startY) > (r.height() / 3)) {
			drawer.close();
		}
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			final int x = (int) event.getX(), y = (int) event.getY();
			if (isExpanded()) {
				if (ViewContainsXY(playPauseButton, x, y)) {
					clickedItem = ClickedItem.playPause;
					return true;
				} else if (ViewContainsXY(rewindButton, x, y)) {
					clickedItem = ClickedItem.rewind;
					return true;
				} else if (ViewContainsXY(rewind30Button, x, y)) {
					clickedItem = ClickedItem.rewind30;
					return true;
				} else if (ViewContainsXY(fastForwardButton, x, y)) {
					clickedItem = ClickedItem.fastForward;
					return true;
				} else if (ViewContainsXY(progressBar, x, y)) {
					clickedItem = ClickedItem.progressbar;
					return true;
				}
			} else {
				if (ViewContainsXY(contractedPlayButton, x, y)) {
					clickedItem = ClickedItem.contractedPlay;
					return true;
				}
			}
		}

		return false;
	}

	private void showPlayPause(boolean showPressed) {
        Log.d(TAG, "showPlayPause: " + showPressed);
		if (playPauseShowsPlay) {
			if (showPressed) {
				playPauseButton.setImageResource(R.drawable.play_button_pressed);
				contractedPlayButton.setImageResource(R.drawable.play_button_pressed);
			} else {
				playPauseButton.setImageResource(R.drawable.play_button_normal);
				contractedPlayButton.setImageResource(R.drawable.play_button_normal);
			}
		} else {
			if (showPressed) {
				playPauseButton.setImageResource(R.drawable.pause_button_pressed);
				contractedPlayButton.setImageResource(R.drawable.pause_button_pressed);
			} else {
				playPauseButton.setImageResource(R.drawable.pause_button_normal);
				contractedPlayButton.setImageResource(R.drawable.pause_button_normal);
			}
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (clickedItem != null) {

			final int x = (int) event.getX(), y = (int) event.getY();

			switch (clickedItem) {

			case playPause:

				switch (event.getAction()) {

					case MotionEvent.ACTION_DOWN:
	
						if (ViewContainsXY(playPauseButton, x, y)) {
							if (enablePlaybackControls) {
								showPlayPause(true);
							}
							startY = y;
							cancelDown = false;
							return true;
						}
						break;
	
					case MotionEvent.ACTION_MOVE:
	
						if (!ViewContainsXY(playPauseButton, x, y)) {
							if (enablePlaybackControls) {
								showPlayPause(false);
							}
							cancelDown = true;
							return true;
						}
						break;
	
					case MotionEvent.ACTION_UP:
	
						if (enablePlaybackControls && !cancelDown) {
					        Log.v(TAG, "playPause: togglePlayPause");
							playPauseShowsPlay = !playPauseShowsPlay;
							if (!currentPlaying.isCompleteFileAvailable() && !playPauseShowsPlay) {
								startPlaylistSpinners();
							}
			    			AnalyticsUtil.trackEvent(context, "Player", "Click", playPauseShowsPlay ? "Play" : "Pause", 0);
	
							showPlayPause(false);
							if (getDownloadService() != null) {
								getDownloadService().togglePlayPause();
							}
						} else {
					        Log.v(TAG, "playPause: toggle drawer");
							closeDrawerIfPastThreshold(y);
						}
						clickedItem = null;
						return true;
				}
				break;

			case rewind:

				switch (event.getAction()) {

					case MotionEvent.ACTION_DOWN:
	
						if (ViewContainsXY(rewindButton, x, y)) {
							if (enablePlaybackControls) {
								rewindButton.setImageResource(R.drawable.rew_pressed);
							}
							startY = y;
							cancelDown = false;
							return true;
						}
						break;
	
					case MotionEvent.ACTION_MOVE:
	
						if (!ViewContainsXY(rewindButton, x, y)) {
							if (enablePlaybackControls) {
								rewindButton.setImageResource(R.drawable.rew_normal);
							}
							cancelDown = true;
							return false;
						}
						break;
	
					case MotionEvent.ACTION_UP:
	
						if (enablePlaybackControls && !cancelDown) {
					        Log.v(TAG, "rewind: previous()");
			    			AnalyticsUtil.trackEvent(context, "Player", "Click", "Previous", 0);
							rewindButton.setImageResource(R.drawable.rew_normal);
							if (getDownloadService() != null) {
								getDownloadService().previous();
							}
						} else {
					        Log.v(TAG, "rewind: toggle drawer");
							closeDrawerIfPastThreshold(y);
						}
						clickedItem = null;
						return true;
				}
				break;

			case rewind30:

				switch (event.getAction()) {

					case MotionEvent.ACTION_DOWN:
	
						if (ViewContainsXY(rewind30Button, x, y)) {
							if (enablePlaybackControls) {
								rewind30Button.setImageResource(R.drawable.rew_30_pressed);
							}
							startY = y;
							cancelDown = false;
							return true;
						}
						break;
	
					case MotionEvent.ACTION_MOVE:
	
						if (!ViewContainsXY(rewind30Button, x, y)) {
							if (enablePlaybackControls) {
								rewind30Button.setImageResource(R.drawable.rew_30_normal);
							}
							cancelDown = true;
							return false;
						}
						break;
	
					case MotionEvent.ACTION_UP:
	
						if (enablePlaybackControls && !cancelDown) {
					        Log.v(TAG, "rewind30: seekTo");
			    			AnalyticsUtil.trackEvent(context, "Player", "Click", "Rewind30", 0);
							rewind30Button.setImageResource(R.drawable.rew_30_normal);
	
							if (getDownloadService() != null) {
								int position = getDownloadService().getPlayerPosition();
								getDownloadService().seekTo(Math.max(position - 30000, 0));
							}
						} else {
					        Log.v(TAG, "rewind30: toggle drawer");
							closeDrawerIfPastThreshold(y);
						}
						clickedItem = null;
						return true;
				}
				break;

			case fastForward:

				switch (event.getAction()) {

					case MotionEvent.ACTION_DOWN:
	
						if (ViewContainsXY(fastForwardButton, x, y)) {
							if (enablePlaybackControls) {
								fastForwardButton.setImageResource(R.drawable.ffwd_pressed);
							}
							startY = y;
							cancelDown = false;
							return true;
						}
						break;
	
					case MotionEvent.ACTION_MOVE:
	
						if (!ViewContainsXY(fastForwardButton, x, y)) {
							if (enablePlaybackControls) {
								fastForwardButton.setImageResource(R.drawable.ffwd_normal);
							}
							cancelDown = true;
							return false;
						}
						break;
	
					case MotionEvent.ACTION_UP:
	
						if (enablePlaybackControls && !cancelDown) {
					        Log.v(TAG, "fastForward: next()");
			    			AnalyticsUtil.trackEvent(context, "Player", "Click", "Next", 0);
							fastForwardButton.setImageResource(R.drawable.ffwd_normal);
							if (getDownloadService() != null) {
								getDownloadService().next();
							}
						} else {
					        Log.v(TAG, "fastForward: toggle drawer");
					        closeDrawerIfPastThreshold(y);
						}
						clickedItem = null;
						return true;
				}

				break;

			case contractedPlay:
				switch (event.getAction()) {

					case MotionEvent.ACTION_DOWN:
	
						if (ViewContainsXY(contractedPlayButton, x, y)) {
							if (enablePlaybackControls) {
								showPlayPause(true);
							}
							startY = y;
							cancelDown = false;
							return true;
						}
						break;
	
					case MotionEvent.ACTION_MOVE:
	
						if (!ViewContainsXY(contractedPlayButton, x, y)) {
							if (enablePlaybackControls) {
								showPlayPause(false);
							}
							cancelDown = true;
							return false;
						}
						break;
	
					case MotionEvent.ACTION_UP:
					        
						if (enablePlaybackControls && !cancelDown) {
					        Log.v(TAG, "contractedPlay: toggle");
							
							playPauseShowsPlay = !playPauseShowsPlay;
							if (!currentPlaying.isCompleteFileAvailable() && !playPauseShowsPlay) {
								startPlaylistSpinners();
							}
			    			AnalyticsUtil.trackEvent(context, "Player", "Click", playPauseShowsPlay ? "Contracted-Play" : "Contracted-Pause", 0);

							showPlayPause(false);
							if (getDownloadService() != null) {
								getDownloadService().togglePlayPause();
							}
						} else {
					        Log.v(TAG, "contractedPlay: toggle drawer");
							Rect r = new Rect();
							drawer.getDrawingRect(r);
							if ((startY - y) > (r.height() / 3)) {
								drawer.open();
							}
						}
						clickedItem = null;
						return true;
				}
				break;

			case progressbar:

				switch (event.getAction()) {
					case MotionEvent.ACTION_DOWN:
	
						if (ViewContainsXY(progressBar, x, y)) {
							startX = x;
							startY = y;
							return true;
						}
						break;
	
					case MotionEvent.ACTION_MOVE:
	
						boolean xMovement = Math.abs(x - startX) >= touchSlop;
						if (enablePlaybackControls && xMovement) {
							progressBar.onTouchEvent(event);
						} else if (Math.abs(y - startY) >= touchSlop && !xMovement) {
							cancelDown = true;
						}
						return true;
	
					case MotionEvent.ACTION_UP:
	
						if (enablePlaybackControls && !cancelDown) {
							progressBar.onTouchEvent(event);
						} else {
							closeDrawerIfPastThreshold(y);
						}
						clickedItem = null;
						return true;
				}
				break;
			}
		}
		return false;
	}

	private void startPlaylistSpinners() {
		Log.v(TAG, "startPlaylistSpinners");
		showingSpinners = true;
		ProgressBar loadingIndicator = (ProgressBar) findViewById(R.id.player_loading_indicator);
		if (loadingIndicator != null) {
			loadingIndicator.setVisibility(View.VISIBLE);
		} else {
			Log.w(TAG, "Can't find loading indicator. Expanded? " + isExpanded());
		}
		loadingIndicator = (ProgressBar) findViewById(R.id.player_loading_indicator_contracted);
		if (loadingIndicator != null) {
			loadingIndicator.setVisibility(View.VISIBLE);
		}
	}

	private void stopPlaylistSpinners() {
		Log.v(TAG, "stopPlaylistSpinners");
		showingSpinners = false;
		ProgressBar loadingIndicator = (ProgressBar) findViewById(R.id.player_loading_indicator);
		if (loadingIndicator != null) {
			loadingIndicator.setVisibility(View.INVISIBLE);
		}
		loadingIndicator = (ProgressBar) findViewById(R.id.player_loading_indicator_contracted);
		if (loadingIndicator != null) {
			loadingIndicator.setVisibility(View.INVISIBLE);
		}
	}

	public void play(int position) {
		warnIfNetworkOrStorageUnavailable();
		getDownloadService().play(position);
		onCurrentChanged();
		onProgressChanged();
	}
	public void remove(DownloadFile track) {
		getDownloadService().remove(track);
		onDownloadListChanged();
	}
}
