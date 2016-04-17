/*
 * Copyright (c) 2012 Google Inc.  All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.org.ngo.squeezer;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.drawable.StateListDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.mikepenz.fontawesome_typeface_library.FontAwesome;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.Iconics;
import com.mikepenz.iconics.IconicsDrawable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import uk.org.ngo.squeezer.dialog.AboutDialog;
import uk.org.ngo.squeezer.dialog.EnableWifiDialog;
import uk.org.ngo.squeezer.framework.BaseActivity;
import uk.org.ngo.squeezer.itemlist.AlarmsActivity;
import uk.org.ngo.squeezer.itemlist.AlbumListActivity;
import uk.org.ngo.squeezer.itemlist.CurrentPlaylistActivity;
import uk.org.ngo.squeezer.itemlist.PlayerListActivity;
import uk.org.ngo.squeezer.itemlist.SongListActivity;
import uk.org.ngo.squeezer.model.Artist;
import uk.org.ngo.squeezer.model.Player;
import uk.org.ngo.squeezer.model.PlayerState;
import uk.org.ngo.squeezer.model.PlayerState.RepeatStatus;
import uk.org.ngo.squeezer.model.PlayerState.ShuffleStatus;
import uk.org.ngo.squeezer.model.Song;
import uk.org.ngo.squeezer.service.ConnectionState;
import uk.org.ngo.squeezer.service.ISqueezeService;
import uk.org.ngo.squeezer.service.ServerString;
import uk.org.ngo.squeezer.service.SqueezeService;
import uk.org.ngo.squeezer.service.event.ConnectionChanged;
import uk.org.ngo.squeezer.service.event.HandshakeComplete;
import uk.org.ngo.squeezer.service.event.MusicChanged;
import uk.org.ngo.squeezer.service.event.PlayStatusChanged;
import uk.org.ngo.squeezer.service.event.PlayersChanged;
import uk.org.ngo.squeezer.service.event.PowerStatusChanged;
import uk.org.ngo.squeezer.service.event.RepeatStatusChanged;
import uk.org.ngo.squeezer.service.event.ShuffleStatusChanged;
import uk.org.ngo.squeezer.service.event.SongTimeChanged;
import uk.org.ngo.squeezer.util.ImageFetcher;

public class NowPlayingFragment extends Fragment implements View.OnCreateContextMenuListener {

    private final String TAG = "NowPlayingFragment";

    private BaseActivity mActivity;

    @Nullable
    private ISqueezeService mService = null;

    private TextView albumText;

    private TextView artistText;

    private TextView trackText;

//    private ImageView btnContextMenu;

    private TextView currentTime;

    private TextView totalTime;

    private ImageButton playPauseButton;

    private ImageButton nextButton;

    private ImageButton prevButton;

    private ImageButton shuffleButton;

    private ImageButton repeatButton;

    private ImageView albumArt;

    private View ControllersView;

    /** In full-screen mode, shows the current progress through the track. */
    private SeekBar seekBar;

    /** In mini-mode, shows the current progress through the track. */
    private ProgressBar mProgressBar;

    // Updating the seekbar
    private boolean updateSeekBar = true;

    private int fontColorCode;


    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager connMgr = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            if (networkInfo.isConnected()) {
                Log.v(TAG, "Received WIFI connected broadcast");
                if (!isConnected()) {
                    // Requires a serviceStub. Else we'll do this on the service
                    // connection callback.
                    if (mService != null && !isManualDisconnect()) {
                        Log.v(TAG, "Initiated connect on WIFI connected");
                        startVisibleConnection();
                    }
                }
            }
        }
    };

    /** Dialog displayed while connecting to the server. */
    private ProgressDialog connectingDialog = null;
    private View pageView;

    /**
     * Shows the "connecting" dialog if it's not already showing.
     */
    @UiThread
    private void showConnectingDialog() {
        if (connectingDialog == null || !connectingDialog.isShowing()) {
            Preferences preferences = new Preferences(mActivity);
            Preferences.ServerAddress serverAddress = preferences.getServerAddress();

            connectingDialog = ProgressDialog.show(mActivity,
                    getText(R.string.connecting_text),
                    getString(R.string.connecting_to_text, preferences.getServerName(serverAddress)),
                    true, false);
        }
    }

    /**
     * Dismisses the "connecting" dialog if it's showing.
     */
    @UiThread
    private void dismissConnectingDialog() {
        if (connectingDialog != null && connectingDialog.isShowing()) {
            connectingDialog.dismiss();
        }
        connectingDialog = null;
    }


    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            Log.v(TAG, "ServiceConnection.onServiceConnected()");
            NowPlayingFragment.this.onServiceConnected((ISqueezeService) binder);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }
    };

    private boolean mFullHeightLayout;

    /**
     * Called before onAttach. Pull out the layout spec to figure out which layout to use later.
     */
    @Override
    public void onInflate(Activity activity, AttributeSet attrs, Bundle savedInstanceState) {
        super.onInflate(activity, attrs, savedInstanceState);

        int layout_height = attrs.getAttributeUnsignedIntValue(
                "http://schemas.android.com/apk/res/android",
                "layout_height", 0);

        mFullHeightLayout = (layout_height == ViewGroup.LayoutParams.FILL_PARENT);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = (BaseActivity) activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        mActivity.bindService(new Intent(mActivity, SqueezeService.class), serviceConnection,
                Context.BIND_AUTO_CREATE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v;
        if(getActivity().getLocalClassName().toLowerCase().contains("nowplayingactivity")){
            mFullHeightLayout = true;

        }

        fontColorCode = Color.parseColor("#aa000000");
        if (mFullHeightLayout) {
            fontColorCode = Color.parseColor("#aaffffff");
            v = inflater.inflate(R.layout.now_playing_fragment_full, container, false);
            pageView = v.findViewById(R.id.pageView);
            artistText = (TextView) v.findViewById(R.id.artistname);
            nextButton = (ImageButton) v.findViewById(R.id.next);
            prevButton = (ImageButton) v.findViewById(R.id.prev);
            shuffleButton = (ImageButton) v.findViewById(R.id.shuffle);
            repeatButton = (ImageButton) v.findViewById(R.id.repeat);
            currentTime = (TextView) v.findViewById(R.id.currenttime);
            totalTime = (TextView) v.findViewById(R.id.totaltime);
            seekBar = (SeekBar) v.findViewById(R.id.seekbar);
            ControllersView = v.findViewById(R.id.controllers);

//            btnContextMenu = (ImageView) v.findViewById(R.id.context_menu);
//            btnContextMenu.setOnCreateContextMenuListener(this);
//            btnContextMenu.setOnClickListener(new OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    v.showContextMenu();
//                }
//            });

        } else {
            v = inflater.inflate(R.layout.now_playing_fragment_mini, container, false);

            mProgressBar = (ProgressBar) v.findViewById(R.id.progressbar);
        }

        albumArt = (ImageView) v.findViewById(R.id.album);
        if (mFullHeightLayout) {
            albumArt.setOnCreateContextMenuListener(this);
//            albumArt.setOnClickListener(new OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    v.showContextMenu();
//                }
//            });
        }
        trackText = (TextView) v.findViewById(R.id.trackname);
        albumText = (TextView) v.findViewById(R.id.albumname);
        playPauseButton = (ImageButton) v.findViewById(R.id.pause);

        // Marquee effect on TextViews only works if they're focused.
        trackText.requestFocus();

        playPauseButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mService == null) {
                    return;
                }
                if (isConnected()) {
                    Log.v(TAG, "Pause...");
                    mService.togglePausePlay();
                } else {
                    // When we're not connected, the play/pause
                    // button turns into a green connect button.
                    onUserInitiatesConnect();
                }
            }
        });

        if (mFullHeightLayout) {
            /*
             * TODO: Simplify these following the notes at
             * http://developer.android.com/resources/articles/ui-1.6.html.
             * Maybe. because the TextView resources don't support the
             * android:onClick attribute.
             */
            nextButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mService == null) {
                        return;
                    }
                    mService.nextTrack();
                }
            });

            prevButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mService == null) {
                        return;
                    }
                    mService.previousTrack();
                }
            });

            shuffleButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mService == null) {
                        return;
                    }
                    mService.toggleShuffle();
                }
            });

            repeatButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mService == null) {
                        return;
                    }
                    mService.toggleRepeat();
                }
            });

            seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
                Song seekingSong;

                // Update the time indicator to reflect the dragged thumb
                // position.
                @Override
                public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                    if (fromUser) {
                        currentTime.setText(Util.formatElapsedTime(progress));
                    }
                }

                // Disable updates when user drags the thumb.
                @Override
                public void onStartTrackingTouch(SeekBar s) {
                    seekingSong = getCurrentSong();
                    updateSeekBar = false;
                }

                // Re-enable updates. If the current song is the same as when
                // we started seeking then jump to the new point in the track,
                // otherwise ignore the seek.
                @Override
                public void onStopTrackingTouch(SeekBar s) {
                    Song thisSong = getCurrentSong();

                    updateSeekBar = true;

                    if (seekingSong == thisSong) {
                        setSecondsElapsed(s.getProgress());
                    }
                }
            });
        } else {
            // Clicking on the layout goes to NowPlayingActivity.
            v.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    NowPlayingActivity.show(mActivity);
                }
            });
        }

        return v;
    }

    @UiThread
    private void updatePlayPauseIcon(@PlayerState.PlayState String playStatus) {
        if(!PlayerState.PLAY_STATE_PLAY.equals(playStatus)){
            StateListDrawable iconStateListDrawable = new StateListDrawable();
            iconStateListDrawable.addState(new int[]{android.R.attr.state_pressed}, new IconicsDrawable(mActivity, GoogleMaterial.Icon.gmd_play_arrow).sizeDp(30).color(Color.parseColor("#aaCC4C1A")));
            iconStateListDrawable.addState(new int[]{}, new IconicsDrawable(mActivity, GoogleMaterial.Icon.gmd_play_arrow).sizeDp(30).color(fontColorCode));
            playPauseButton.setImageDrawable(iconStateListDrawable);
        }else{
            StateListDrawable iconStateListDrawable = new StateListDrawable();
            iconStateListDrawable.addState(new int[]{android.R.attr.state_pressed}, new IconicsDrawable(mActivity, GoogleMaterial.Icon.gmd_pause).sizeDp(30).color(Color.parseColor("#aaCC4C1A")));
            iconStateListDrawable.addState(new int[]{}, new IconicsDrawable(mActivity, GoogleMaterial.Icon.gmd_pause).sizeDp(30).color(fontColorCode));
            playPauseButton.setImageDrawable(iconStateListDrawable);
        }

    }

    @UiThread
    private void updateShuffleStatus(ShuffleStatus shuffleStatus) {
        if (mFullHeightLayout && shuffleStatus != null) {

            StateListDrawable iconStateListDrawable = new StateListDrawable();
            iconStateListDrawable.addState(new int[]{android.R.attr.state_pressed}, new IconicsDrawable(mActivity, shuffleStatus.getFontIcon()).sizeDp(30).color(shuffleStatus.getColor()).contourWidthDp(1));
            iconStateListDrawable.addState(new int[]{}, new IconicsDrawable(mActivity, shuffleStatus.getFontIcon()).sizeDp(30).color(shuffleStatus.getColor()).contourWidthDp(2));
            shuffleButton.setImageDrawable(iconStateListDrawable);
        }
    }

    @UiThread
    private void updateRepeatStatus(RepeatStatus repeatStatus) {
        if (mFullHeightLayout && repeatStatus != null) {
            repeatButton.setImageResource(
                    mActivity.getAttributeValue(repeatStatus.getIcon()));

            StateListDrawable iconStateListDrawable = new StateListDrawable();
            iconStateListDrawable.addState(new int[]{android.R.attr.state_pressed}, new IconicsDrawable(mActivity, repeatStatus.getFontIcon()).sizeDp(30).color(repeatStatus.getColor()).contourWidthDp(1));
            iconStateListDrawable.addState(new int[]{}, new IconicsDrawable(mActivity, repeatStatus.getFontIcon()).sizeDp(30).color(repeatStatus.getColor()).contourWidthDp(2));
            repeatButton.setImageDrawable(iconStateListDrawable);

        }
    }

    /**
     * Manages the list of connected players in the action bar.
     *
     * @param players A list of players to show. May be empty (use {@code
     * Collections.&lt;Player>emptyList()}) but not null.
     * @param activePlayer The currently active player. May be null.
     */
    @UiThread
    private void updatePlayerDropDown(@NonNull Collection<Player> players,
            @Nullable Player activePlayer) {
        if (!isAdded()) {
            return;
        }

        // Only include players that are connected to the server.
        ArrayList<Player> connectedPlayers = new ArrayList<Player>();
        for (Player player : players) {
            if (player.getConnected()) {
                connectedPlayers.add(player);
            }
        }

        /**
        ActionBar actionBar = mActivity.getSupportActionBar();

        // If there are multiple players connected then show a spinner allowing the user to
        // choose between them.
        if (connectedPlayers.size() > 1) {
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
            final ArrayAdapter<Player> playerAdapter = new ArrayAdapter<Player>(
                    actionBar.getThemedContext(), android.R.layout.simple_spinner_dropdown_item,
                    connectedPlayers) {
                @Override
                public View getDropDownView(int position, View convertView, ViewGroup parent) {
                    return Util.getActionBarSpinnerItemView(getContext(), convertView, parent,
                            getItem(position).getName());
                }

                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    return Util.getActionBarSpinnerItemView(getContext(), convertView, parent,
                            getItem(position).getName());
                }
            };
            actionBar.setListNavigationCallbacks(playerAdapter,
                    new ActionBar.OnNavigationListener() {
                        @Override
                        public boolean onNavigationItemSelected(int position, long id) {
                            if (!playerAdapter.getItem(position)
                                    .equals(mService.getActivePlayer())) {
                                Log.i(TAG,
                                        "onNavigationItemSelected.setActivePlayer(" + playerAdapter
                                                .getItem(position) + ")");
                                mService.setActivePlayer(playerAdapter.getItem(position));
                                updateUiFromPlayerState(mService.getActivePlayerState());
                            }
                            return true;
                        }
                    });
            if (activePlayer != null) {
                actionBar.setSelectedNavigationItem(playerAdapter.getPosition(activePlayer));
            }
        } else {
            // 0 or 1 players, disable the spinner, and either show the sole player in the
            // action bar, or the app name if there are no players.
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);

            if (connectedPlayers.size() == 1) {
                actionBar.setTitle(connectedPlayers.get(0).getName());
            } else {
                // TODO: Alert the user if there are no connected players.
                actionBar.setTitle(R.string.app_name);
            }
        }
         */
    }

    protected void onServiceConnected(@NonNull ISqueezeService service) {
        Log.v(TAG, "Service bound");
        mService = service;

        maybeRegisterCallbacks(mService);

        // Assume they want to connect (unless manually disconnected).
        if (!isConnected() && !isManualDisconnect()) {
            startVisibleConnection();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume...");

        // Start it and have it run forever (until it shuts itself down).
        // This is required so swapping out the activity (and unbinding the
        // service connection in onDestroy) doesn't cause the service to be
        // killed due to zero refcount.  This is our signal that we want
        // it running in the background.
        mActivity.startService(new Intent(mActivity, SqueezeService.class));

        if (mService != null) {
            maybeRegisterCallbacks(mService);
        }

        if (new Preferences(mActivity).isAutoConnect()) {
            mActivity.registerReceiver(broadcastReceiver, new IntentFilter(
                    ConnectivityManager.CONNECTIVITY_ACTION));
        }
    }

    /**
     * Keep track of whether callbacks have been registered
     */
    private boolean mRegisteredCallbacks;

    /**
     * This is called when the service is first connected, and whenever the activity is resumed.
     */
    private void maybeRegisterCallbacks(@NonNull ISqueezeService service) {
        if (!mRegisteredCallbacks) {
            service.getEventBus().registerSticky(this);

            mRegisteredCallbacks = true;
        }
    }

    @UiThread
    private void updateTimeDisplayTo(int secondsIn, int secondsTotal) {
        if (mFullHeightLayout) {
            if (updateSeekBar) {
                if (seekBar.getMax() != secondsTotal) {
                    seekBar.setMax(secondsTotal);
                    totalTime.setText(Util.formatElapsedTime(secondsTotal));
                }
                seekBar.setProgress(secondsIn);
                currentTime.setText(Util.formatElapsedTime(secondsIn));
            }
        } else {
            if (mProgressBar.getMax() != secondsTotal) {
                mProgressBar.setMax(secondsTotal);
            }
            mProgressBar.setProgress(secondsIn);
        }
    }

    /**
     * Update the UI based on the player state. Call this when the active player
     * changes.
     *
     * @param playerState the player state to reflect in the UI.
     */
    @UiThread
    private void updateUiFromPlayerState(@NonNull PlayerState playerState) {
        updateSongInfo(playerState);

        updatePlayPauseIcon(playerState.getPlayStatus());
        updateShuffleStatus(playerState.getShuffleStatus());
        updateRepeatStatus(playerState.getRepeatStatus());
        mActivity.updatePowerMenuItems(canPowerOn(), canPowerOff());
    }

    /**
     * Update the UI when the song changes, either because the track has changed, or the
     * active player has changed.
     *
     * @param playerState the player state for the song.
     */
    @UiThread
    private void updateSongInfo(@NonNull PlayerState playerState) {
        updateTimeDisplayTo(playerState.getCurrentTimeSecond(),
                playerState.getCurrentSongDuration());

        Song song = playerState.getCurrentSong();

        if (song != null) {
            albumText.setText(song.getAlbumName());
            trackText.setText(song.getName());
            if (mFullHeightLayout) {
                artistText.setText(song.getArtist());
                totalTime.setText(Util.formatElapsedTime(song.getDuration()));
                if (song.isRemote()) {
                    if (song.getButtons().length() == 0) {
                        nextButton.setEnabled(false);
                        Util.setAlpha(nextButton, 0.25f);
                    } else {
                        // TODO: figure out how to parse the buttons HASH;
                        // for now just assume the next button is enabled
                        nextButton.setEnabled(true);
                        Util.setAlpha(nextButton, 1.0f);
                    }
                    prevButton.setEnabled(false);
                    Util.setAlpha(prevButton, 0.25f);
                    albumArt.setClickable(false);
//                    btnContextMenu.setVisibility(View.GONE);
                } else {
                    albumArt.setClickable(true);
                    nextButton.setEnabled(true);
                    Util.setAlpha(nextButton, 1.0f);
                    prevButton.setEnabled(true);
                    Util.setAlpha(prevButton, 1.0f);
//                    btnContextMenu.setVisibility(View.VISIBLE);
                }
            }
        } else {
            albumText.setText("");
            trackText.setText("");
            if (mFullHeightLayout) {
                artistText.setText("");
                albumArt.setClickable(false);
//                btnContextMenu.setVisibility(View.GONE);
            }
        }

        if (song == null || !song.hasArtwork()) {
            if (mFullHeightLayout) {
                albumArt.setImageResource(song != null && song.isRemote()
                        ? R.drawable.icon_iradio_noart_fullscreen
                        : R.drawable.icon_album_noart_fullscreen);
            } else {
                albumArt.setImageResource(song != null && song.isRemote()
                        ? R.drawable.icon_iradio_noart
                        : R.drawable.icon_album_noart);
            }
            return;
        }

        ImageFetcher.getInstance(mActivity).loadImage(song.getArtworkUrl(), albumArt);
    }

    private boolean setSecondsElapsed(int seconds) {
        return mService != null && mService.setSecondsElapsed(seconds);
    }

    private PlayerState getPlayerState() {
        if (mService == null) {
            return null;
        }
        return mService.getPlayerState();
    }

    private Player getActivePlayer() {
        if (mService == null) {
            return null;
        }
        return mService.getActivePlayer();
    }

    private Song getCurrentSong() {
        PlayerState playerState = getPlayerState();
        return playerState != null ? playerState.getCurrentSong() : null;
    }

    private boolean isConnected() {
        return mService != null && mService.isConnected();
    }

    private boolean isConnectInProgress() {
        return mService != null && mService.isConnectInProgress();
    }

    private boolean canPowerOn() {
        return mService != null && mService.canPowerOn();
    }

    private boolean canPowerOff() {
        return mService != null && mService.canPowerOff();
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause...");

        dismissConnectingDialog();

        if (new Preferences(mActivity).isAutoConnect()) {
            mActivity.unregisterReceiver(broadcastReceiver);
        }

        if (mRegisteredCallbacks) {
            mService.cancelSubscriptions(this);
            mService.getEventBus().unregister(this);
            mRegisteredCallbacks = false;
        }

        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mService != null) {
            mActivity.unbindService(serviceConnection);
        }
    }

    /**
     * Builds a context menu suitable for the currently playing song.
     * <p>
     * Takes the general song context menu, and disables items that make no sense for the song that
     * is currently playing.
     * <p>
     * {@inheritDoc}
     *
     * @param menu
     * @param v
     * @param menuInfo
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.songcontextmenu, menu);

        menu.findItem(R.id.play_now).setVisible(false);
        menu.findItem(R.id.play_next).setVisible(false);
        menu.findItem(R.id.add_to_playlist).setVisible(false);

        menu.findItem(R.id.view_this_album).setVisible(true);
        menu.findItem(R.id.view_albums_by_song).setVisible(true);
        menu.findItem(R.id.view_songs_by_artist).setVisible(true);
    }

    /**
     * Handles clicks on the context menu.
     * <p>
     * {@inheritDoc}
     *
     * @param item
     *
     * @return
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        Song song = getCurrentSong();
        if (song == null || song.isRemote()) {
            return false;
        }

        // Note: Very similar to code in SongView:doItemContext().  Refactor?
        switch (item.getItemId()) {
            case R.id.download:
                mActivity.downloadItem(song);
                return true;

            case R.id.view_this_album:
                SongListActivity.show(getActivity(), song.getAlbum());
                return true;

            case R.id.view_albums_by_song:
                AlbumListActivity.show(getActivity(),
                        new Artist(song.getArtistId(), song.getArtist()));
                return true;

            case R.id.view_songs_by_artist:
                SongListActivity.show(getActivity(),
                        new Artist(song.getArtistId(), song.getArtist()));
                return true;

            default:
                throw new IllegalStateException("Unknown menu ID.");
        }
    }


    /**
     * @see android.support.v4.app.Fragment#onCreateOptionsMenu(android.view.Menu,
     * android.view.MenuInflater)
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
//        MenuInflater i = mActivity.getMenuInflater();
//        i.inflate(R.menu.now_playing_fragment, menu);
//
//        menu_item_connect = menu.findItem(R.id.menu_item_connect);
//        menu_item_disconnect = menu.findItem(R.id.menu_item_disconnect);
//        menu_item_poweron = menu.findItem(R.id.menu_item_poweron);
//        menu_item_poweroff = menu.findItem(R.id.menu_item_poweroff);
//        menu_item_playlist = menu.findItem(R.id.menu_item_playlist);
//        menu_item_search = menu.findItem(R.id.menu_item_search);
//
//        mActivity.setSearchMenuItem(menu.findItem(R.id.menu_item_search));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_connect:
                onUserInitiatesConnect();
                return true;
//            case R.id.menu_item_settings:
//                SettingsActivity.show(mActivity);
//                return true;
//            case R.id.menu_item_players:
//                PlayerListActivity.show(mActivity);
//                return true;
//            case R.id.menu_item_alarm:
//                AlarmsActivity.show(mActivity);
//                return true;
//            case R.id.menu_item_about:
//                new AboutDialog().show(getFragmentManager(), "AboutDialog");
//                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Has the user manually disconnected from the server?
     *
     * @return true if they have, false otherwise.
     */
    private boolean isManualDisconnect() {
        return getActivity() instanceof DisconnectedActivity;
    }

    private void onUserInitiatesConnect() {
        if (mService == null) {
            Log.e(TAG, "serviceStub is null.");
            return;
        }
        startVisibleConnection();
    }

    public void startVisibleConnection() {
        Log.v(TAG, "startVisibleConnection");
        if (mService == null) {
            return;
        }

        Preferences preferences = new Preferences(mActivity);

        // If we are configured to automatically connect on Wi-Fi availability
        // we will also give the user the opportunity to enable Wi-Fi
        if (preferences.isAutoConnect()) {
            WifiManager wifiManager = (WifiManager) mActivity
                    .getSystemService(Context.WIFI_SERVICE);
            if (!wifiManager.isWifiEnabled()) {
                FragmentManager fragmentManager = getFragmentManager();
                if (fragmentManager != null) {
                    EnableWifiDialog.show(getFragmentManager());
                } else {
                    Log.i(getTag(), "fragment manager is null so we can't show EnableWifiDialog");
                }
                return;
                // When a Wi-Fi connection is made this method will be called again by the
                // broadcastReceiver
            }
        }

        Preferences.ServerAddress serverAddress = preferences.getServerAddress();
        String ipPort = serverAddress.address;
        if (ipPort == null) {
            // Set up a server connection, if it is not present
            DisconnectedActivity.show(mActivity);
            return;
        }

        if (isConnectInProgress()) {
            Log.v(TAG, "Connection is already in progress, connecting aborted");
            return;
        }
        Log.v(TAG, "startConnect, ipPort: " + ipPort);
        mService.startConnect(ipPort, preferences.getUserName(serverAddress, "test"),
                preferences.getPassword(serverAddress, "test1"));
    }

    @MainThread
    public void onEventMainThread(ConnectionChanged event) {
        Log.d(TAG, "ConnectionChanged: " + event);

        // The fragment may no longer be attached to the parent activity.  If so, do nothing.
        if (!isAdded()) {
            return;
        }

        // Handle any of the reasons for disconnection, clear the dialog and show the
        // DisconnectedActivity.
        if (event.connectionState == ConnectionState.DISCONNECTED) {
            dismissConnectingDialog();
            DisconnectedActivity.show(mActivity);
            return;
        }

        if (event.connectionState == ConnectionState.CONNECTION_FAILED) {
            dismissConnectingDialog();
            DisconnectedActivity.showConnectionFailed(mActivity);
            return;
        }

        if (event.connectionState == ConnectionState.LOGIN_FAILED) {
            dismissConnectingDialog();
            DisconnectedActivity.showLoginFailed(mActivity);
            return;
        }

        // Any other event means that a connection is in progress, make sure the dialog is showing.
        showConnectingDialog();

        // Ensure that option menu item state is adjusted as appropriate.
        getActivity().supportInvalidateOptionsMenu();

        playPauseButton.setImageResource(
                mActivity.getAttributeValue(R.attr.ic_action_av_connect));

        StateListDrawable iconStateListDrawable = new StateListDrawable();
        iconStateListDrawable.addState(new int[]{android.R.attr.state_pressed}, new IconicsDrawable(mActivity, GoogleMaterial.Icon.gmd_play_arrow).sizeDp(30).color(Color.parseColor("#aaCC4C1A")).contourWidthDp(1));
        iconStateListDrawable.addState(new int[]{}, new IconicsDrawable(mActivity, GoogleMaterial.Icon.gmd_play_arrow).sizeDp(30).color(fontColorCode).contourWidthDp(2));
        playPauseButton.setImageDrawable(iconStateListDrawable);

        if (mFullHeightLayout) {
            nextButton.setEnabled(false);
            prevButton.setEnabled(false);
            shuffleButton.setEnabled(false);
            repeatButton.setEnabled(false);

            albumArt.setImageResource(R.drawable.icon_album_noart_fullscreen);
            nextButton.setImageResource(0);
            prevButton.setImageResource(0);
            shuffleButton.setImageResource(0);
            repeatButton.setImageResource(0);
            updatePlayerDropDown(Collections.<Player>emptyList(), null);
            artistText.setText(getText(R.string.disconnected_text));
            currentTime.setText("--:--");
            totalTime.setText("--:--");
            seekBar.setEnabled(false);
            seekBar.setProgress(0);
        } else {
            albumArt.setImageResource(R.drawable.icon_album_noart);
            mProgressBar.setEnabled(false);
            mProgressBar.setProgress(0);
        }
     }

    @MainThread
    public void onEventMainThread(HandshakeComplete event) {
        // Event might arrive before this fragment has connected to the service (e.g.,
        // the activity connected before this fragment did).
        // XXX: Verify that this is possible, since the fragment can't register for events
        // until it's connected to the service.
        if (mService == null) {
            return;
        }

        Log.d(TAG, "Handshake complete");

        dismissConnectingDialog();

        if (mFullHeightLayout) {
            nextButton.setEnabled(true);
            prevButton.setEnabled(true);
            shuffleButton.setEnabled(true);
            repeatButton.setEnabled(true);

            StateListDrawable iconStateListDrawablePrevious = new StateListDrawable();
            iconStateListDrawablePrevious.addState(new int[]{android.R.attr.state_pressed}, new IconicsDrawable(mActivity, GoogleMaterial.Icon.gmd_skip_previous).sizeDp(30).color(Color.parseColor("#aaCC4C1A")).contourWidthDp(1));
            iconStateListDrawablePrevious.addState(new int[]{}, new IconicsDrawable(mActivity, GoogleMaterial.Icon.gmd_skip_previous).sizeDp(30).color(fontColorCode).contourWidthDp(2));
            prevButton.setImageDrawable(iconStateListDrawablePrevious);

            StateListDrawable iconStateListDrawableNext = new StateListDrawable();
            iconStateListDrawableNext.addState(new int[]{android.R.attr.state_pressed}, new IconicsDrawable(mActivity, GoogleMaterial.Icon.gmd_skip_next).sizeDp(30).color(Color.parseColor("#aaCC4C1A")).contourWidthDp(1));
            iconStateListDrawableNext.addState(new int[]{}, new IconicsDrawable(mActivity, GoogleMaterial.Icon.gmd_skip_next).sizeDp(30).color(fontColorCode).contourWidthDp(2));
            nextButton.setImageDrawable(iconStateListDrawableNext);

            seekBar.setEnabled(true);
        } else {
            mProgressBar.setEnabled(true);
        }

        PlayerState playerState = getPlayerState();

        // May be no players connected.
        // TODO: These views should be cleared if there's no player connected.
        if (playerState == null)
            return;

        updateUiFromPlayerState(playerState);
    }

    @MainThread
    public void onEventMainThread(MusicChanged event) {
        if (event.player.equals(mService.getActivePlayer())) {
            updateSongInfo(event.playerState);
        }
    }

    @MainThread
    public void onEventMainThread(PlayersChanged event) {
        updatePlayerDropDown(event.players.values(), mService.getActivePlayer());
        updateUiFromPlayerState(mService.getActivePlayerState());
    }

    @MainThread
    public void onEventMainThread(PlayStatusChanged event) {
        updatePlayPauseIcon(event.playStatus);
    }

    @MainThread
    public void onEventMainThread(RepeatStatusChanged event) {
        if (event.player.equals(mService.getActivePlayer())) {
            updateRepeatStatus(event.repeatStatus);
            if (!event.initial) {
                Snackbar snackbar = Snackbar.make(pageView, mActivity.getServerString(event.repeatStatus.getText()), Snackbar.LENGTH_LONG);
                snackbar.show();
            }
        }
    }

    @MainThread
    public void onEventMainThread(ShuffleStatusChanged event) {
        if (event.player.equals(mService.getActivePlayer())) {
            updateShuffleStatus(event.shuffleStatus);
            if (!event.initial) {
                Snackbar snackbar = Snackbar.make(pageView, mActivity.getServerString(event.shuffleStatus.getText()), Snackbar.LENGTH_LONG);
                snackbar.show();
            }
        }
    }

    @MainThread
    public void onEventMainThread(SongTimeChanged event) {
        if (event.player.equals(mService.getActivePlayer())) {
            updateTimeDisplayTo(event.currentPosition, event.duration);
        }
    }
}
