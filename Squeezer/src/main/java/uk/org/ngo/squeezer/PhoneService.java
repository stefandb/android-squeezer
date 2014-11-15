package uk.org.ngo.squeezer;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.WearableListenerService;

import android.net.Uri;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import uk.org.ngo.squeezer.dialog.AuthenticationDialog;
import uk.org.ngo.squeezer.dialog.EnableWifiDialog;
import uk.org.ngo.squeezer.framework.BaseActivity;
import uk.org.ngo.squeezer.framework.HasUiThread;
import uk.org.ngo.squeezer.model.Player;
import uk.org.ngo.squeezer.model.PlayerState;
import uk.org.ngo.squeezer.model.Song;
import uk.org.ngo.squeezer.service.IServiceCallback;
import uk.org.ngo.squeezer.service.IServiceConnectionCallback;
import uk.org.ngo.squeezer.service.IServiceHandshakeCallback;
import uk.org.ngo.squeezer.service.IServiceMusicChangedCallback;
import uk.org.ngo.squeezer.service.IServicePlayersCallback;
import uk.org.ngo.squeezer.service.IServiceVolumeCallback;
import uk.org.ngo.squeezer.service.ISqueezeService;
import uk.org.ngo.squeezer.service.SqueezeService;

/**
 * Created by Stefan on 14-11-2014.
 */
public class PhoneService  extends WearableListenerService implements
        HasUiThread {
    private static final String START_ACTIVITY_PATH = "/squeezer_current";
    private static final String DATA_ITEM_RECEIVED_PATH = "/data-item-received";
    public static final String DATA_CURRENT_SONG = "/squeezer_current";
    public static final String DATA_ACTION = "/squeezer_action";
    public static final String COUNT_PATH = "/count";
    public static final String IMAGE_PATH = "/image";
    public static final String IMAGE_KEY = "photo";
    private static final String COUNT_KEY = "count";
    private static final int MAX_LOG_TAG_LENGTH = 23;

    @Nullable
    private ISqueezeService mService = null;
    private final Handler uiThreadHandler = new UiThreadHandler(this);
    private boolean mRegisteredCallbacks;
    private final static int UPDATE_TIME = 1;
    private int secondsIn;
    private int secondsTotal;
    private final String TAG = "phoneListenerService";
    private BaseActivity mActivity;
    private boolean mFullHeightLayout;

    private boolean ignoreVolumeChange;
    private VolumePanel mVolumePanel;

    private MenuItem menu_item_connect;
    private MenuItem menu_item_disconnect;
    private MenuItem menu_item_poweron;
    private MenuItem menu_item_poweroff;
    private MenuItem menu_item_players;
    private MenuItem menu_item_playlists;
    private MenuItem menu_item_search;
    private MenuItem menu_item_volume;

    GoogleApiClient mGoogleApiClient;

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d("mobile:squeezer", "ListenerService:onCreate");

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();

        Log.d(TAG, "did bindService; serviceStub = " + mService);
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.d("mobile:squeezer", "ListenerService:onDataChanged");


        LOGD(TAG, "onDataChanged: " + dataEvents);
        final List<DataEvent> events = FreezableUtils.freezeIterable(dataEvents);
        dataEvents.close();
        if(!mGoogleApiClient.isConnected()) {
            ConnectionResult connectionResult = mGoogleApiClient
                    .blockingConnect(30, TimeUnit.SECONDS);
            if (!connectionResult.isSuccess()) {
                Log.e("mobile:squeezer", "DataLayerListenerService failed to connect to GoogleApiClient.");
                return;
            }
        }

        // Loop through the events and send a message back to the node that created the data item.
        for (DataEvent event : events) {
            Uri uri = event.getDataItem().getUri();
            String path = uri.getPath();
            if (COUNT_PATH.equals(path)) {
                // Get the node id of the node that created the data item from the host portion of
                // the uri.
                String nodeId = uri.getHost();
                // Set the data of the message to be the bytes of the Uri.
                byte[] payload = uri.toString().getBytes();

                // Send the rpc
                Wearable.MessageApi.sendMessage(mGoogleApiClient, nodeId, DATA_ITEM_RECEIVED_PATH,
                        payload);
            }else{
                Log.d("mobile:squeezer-data", String.valueOf(event.getType()));
            }
        }
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        LOGD("mobile:squeezer", "onMessageReceived: " + messageEvent);
        Log.d("squeezer message", messageEvent.getData().toString());
        final String message = new String(messageEvent.getData());
        Log.d("mobile:squeezer-message-service", "Message path received on watch is: " + messageEvent.getPath());
        Log.d("mobile:squeezer-message-service", "Message received on watch is: " + message);

        // Check to see if the message is to start an activity

//        if (messageEvent.getPath().equals(START_ACTIVITY_PATH)) {
//            Intent messageIntent = new Intent();
//            messageIntent.setAction(Intent.ACTION_SEND);
//            messageIntent.putExtra("message", message);
//            LocalBroadcastManager.getInstance(this).sendBroadcast(messageIntent);
//        }

        if (messageEvent.getPath().equals("/squeezer_current")) {
//            Log.d("mobile:squeezer", "Message path received on mobile is: " + messageEvent.getPath());
//            Log.d("mobile:squeezer", "Message received on mobile is: " + message.toString());
//            //SEND CURRENT SONG BACK
//
//            JSONObject numberinfo = new JSONObject();
//            try {
//                numberinfo.put("title", "Take me to church");
//                numberinfo.put("artist", "Hozier");
//                numberinfo.put("album", "Hozier");
//                numberinfo.put("status","play");
//
//                numberinfo.put("enable_previous","enabled");
//                numberinfo.put("enable_next","enabled");
//
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
//
//            Log.d("mobile:squeezer", numberinfo.toString());
//            //Requires a new thread to avoid blocking the UI
//            new SendToDataLayerThread(DATA_CURRENT_SONG, numberinfo.toString()).start();

        }else if (messageEvent.getPath().equals("/squeezer_action__")) {

        } else if (messageEvent.getPath().equals("/squeezer_action/next")) {
            if (mService == null) {
                return;
            }
            mService.nextTrack();
        }else if (messageEvent.getPath().equals("/squeezer_action/previous")) {
            if (mService == null) {
                return;
            }
            mService.previousTrack();
        }else if (messageEvent.getPath().equals("/squeezer_action/current")) {
            PlayerState playerState = getPlayerState();
            Song song = playerState.getCurrentSong();
            PlayerState.PlayStatus playStatus = playerState.getPlayStatus();

            JSONObject numberinfo = new JSONObject();
            try {
                numberinfo.put("title", song.getName());
                numberinfo.put("artist", song.getArtist());
                numberinfo.put("album", song.getAlbumName());
                if(playStatus == PlayerState.PlayStatus.play && playStatus != null){
                    //pauze
                    numberinfo.put("status","pause");
                }else if(playStatus != PlayerState.PlayStatus.play && playStatus != null){
                    //play
                    numberinfo.put("status","play");
                }else{
                    //niks
                    numberinfo.put("status","null");
                }

                if (song.isRemote()) {
                    numberinfo.put("btnnext", false);
                    numberinfo.put("btnprevious", false);
                }else{
                    numberinfo.put("btnnext", true);
                    numberinfo.put("btnprevious", true);
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }

            Log.d("mobile:squeezer", numberinfo.toString());
            //Requires a new thread to avoid blocking the UI
            new SendToDataLayerThread(DATA_CURRENT_SONG, numberinfo.toString()).start();
        }else if (messageEvent.getPath().equals("/squeezer_action/play")) {
            //TODO Moet nog geschreven worden

            if (mService == null) {
                return;
            }
            if (isConnected()) {
                Log.v(TAG, "Pause...");
                mService.togglePausePlay();
            } else {
                // When we're not connected, the play/pause
                // button turns into a green connect button.
//                onUserInitiatesConnect();
            }

//            JSONObject numberinfo = new JSONObject();
//            try {
//                numberinfo.put("title", "GET CURRENT");
//                numberinfo.put("artist", "Hozier");
//                numberinfo.put("album", "Hozier");
//                if(message == "play"){
//                    numberinfo.put("status","stop");
//                }else{
//                    numberinfo.put("status","play");
//                }
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
//            Log.d("mobile:squeezer", numberinfo.toString());
//            //Requires a new thread to avoid blocking the UI
//            new SendToDataLayerThread(DATA_CURRENT_SONG, numberinfo.toString()).start();
        }

        Log.d("path", messageEvent.getPath());
        Log.d("mobile:squeezer-data", String.valueOf(messageEvent.getPath()));
        super.onMessageReceived(messageEvent);
    }

    @Override
    public void onPeerConnected(Node peer) {
        LOGD("mobile:squeezer", "onPeerConnected: " + peer);
    }

    @Override
    public void onPeerDisconnected(Node peer) {
        LOGD("mobile:squeezer", "onPeerDisconnected: " + peer);
    }

    public static void LOGD(final String tag, String message) {
//        if (Log.isLoggable(tag, Log.DEBUG)) {
        Log.d(tag, message);
//        }
    }


    class SendToDataLayerThread extends Thread {
        String path;
        String message;

        // Constructor to send a message to the data layer
        SendToDataLayerThread(String p, String msg) {
            path = p;
            message = msg;
        }

        public void run() {
            NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
            Log.d("mobile:squeezer", nodes.getNodes().toString());
            for (Node node : nodes.getNodes()) {
                Log.d("mobile:squeezer-node", node.toString());
                MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(mGoogleApiClient, node.getId(), path, message.getBytes()).await();

                if (result.getStatus().isSuccess()) {
                    Log.d("mobile:squeezer-run", "Message: {" + message + "} sent to: " + node.getDisplayName());
                }
                else {
                    // Log an error
                    Log.d("mobile:squeezer-run", "ERROR: failed to send Message");
                }
            }
        }
    }


    /* ZEKER NODIG */
    private final IServiceMusicChangedCallback musicChangedCallback = new IServiceMusicChangedCallback() {
        @Override
        public void onMusicChanged(final PlayerState playerState) {
            uiThreadHandler.post(new Runnable() {
                public void run() {
                    updateSongInfo(playerState.getCurrentSong(), playerState.getPlayStatus());
                }
            });
        }

        @Override
        public Object getClient() {
            return PhoneService.this;
        }
    };

    private void updateSongInfo(Song song, PlayerState.PlayStatus playStatus) {
        Log.v(TAG, "updateSongInfo " + song);
        if (song != null) {

            JSONObject numberinfo = new JSONObject();
            try {
                numberinfo.put("title", song.getName());
                numberinfo.put("artist", song.getArtist());
                numberinfo.put("album", song.getAlbumName());
                if(playStatus == PlayerState.PlayStatus.play && playStatus != null){
                    //pauze
                    numberinfo.put("status","pause");
                }else if(playStatus != PlayerState.PlayStatus.play && playStatus != null){
                    //play
                    numberinfo.put("status","play");
                }else{
                    numberinfo.put("status","null");
                }

                if (song.isRemote()) {
                    numberinfo.put("btnnext", false);
                    numberinfo.put("btnprevious", false);
                }else{
                    numberinfo.put("btnnext", true);
                    numberinfo.put("btnprevious", true);
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }

            Log.d("mobile:squeezer", numberinfo.toString());
            //Requires a new thread to avoid blocking the UI
            new SendToDataLayerThread(DATA_CURRENT_SONG, numberinfo.toString()).start();
        }
        updateAlbumArt(song);
    }




    /*-----*/

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
                    if (mService != null) {
                        Log.v(TAG, "Initiated connect on WIFI connected");
                        startVisibleConnection();
                    }
                }
            }
        }
    };


    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            Log.v(TAG, "ServiceConnection.onServiceConnected()");
            PhoneService.this.onServiceConnected((ISqueezeService) binder);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }
    };

    protected void onServiceConnected(@NonNull ISqueezeService service) {
        Log.v(TAG, "Service bound");
        mService = service;

        maybeRegisterCallbacks(mService);
        uiThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                updateUIFromServiceState();
            }
        });

        // Assume they want to connect (unless manually disconnected).
        if (!isConnected()) {
            startVisibleConnection();
        }
    }

    private boolean isConnected() {
        if (mService == null) {
            return false;
        }
        return mService.isConnected();
    }

    private void maybeRegisterCallbacks(@NonNull ISqueezeService service) {
        if (!mRegisteredCallbacks) {
            service.registerCallback(serviceCallback);
            service.registerConnectionCallback(connectionCallback);
            service.registerHandshakeCallback(handshakeCallback);
            service.registerMusicChangedCallback(musicChangedCallback);
            service.registerPlayersCallback(playersCallback);
            service.registerVolumeCallback(volumeCallback);
            mRegisteredCallbacks = true;
        }
    }

    private final static class UiThreadHandler extends Handler {

//        final WeakReference<NowPlayingFragment> mFragment;

        public UiThreadHandler(PhoneService fragment) {
//            mFragment = new WeakReference<NowPlayingFragment>(fragment);
        }

        // Normally I'm lazy and just post Runnables to the uiThreadHandler
        // but time updating is special enough (it happens every second) to
        // take care not to allocate so much memory which forces Dalvik to GC
        // all the time.
        @Override
        public void handleMessage(Message message) {
//            if (message.what == UPDATE_TIME) {
//                mFragment.get().updateTimeDisplayTo(mFragment.get().secondsIn,
//                        mFragment.get().secondsTotal);
//            }
        }
    }

    // Should only be called from the UI thread.
    private void updateUIFromServiceState() {
        // Update the UI to reflect connection state. Basically just for
        // the initial display, as changing the prev/next buttons to empty
        // doesn't seem to work in onCreate. (LayoutInflator still running?)
        Log.d(TAG, "updateUIFromServiceState");
        boolean connected = isConnected();
        setConnected(connected, false, false);
        if (connected) {
            PlayerState playerState = getPlayerState();
            playerState.getCurrentSongDuration();
            updateShuffleStatus(playerState.getShuffleStatus());
            updateRepeatStatus(playerState.getRepeatStatus());
        }
    }

//    private boolean isManualDisconnect() {
//        return getActivity() instanceof DisconnectedActivity;
//    }

    public void startVisibleConnection() {
        Log.v(TAG, "startVisibleConnection");
        if (mService == null) {
            return;
        }

        uiThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                Preferences preferences = new Preferences(mActivity);
                String ipPort = preferences.getServerAddress();
                if (ipPort == null) {
                    return;
                }

                if (isConnectInProgress()) {
                    Log.v(TAG, "Connection is already in progress, connecting aborted");
                    return;
                }


                try {
                    connectingDialog = ProgressDialog.show(mActivity,
                            getText(R.string.connecting_text),
                            getString(R.string.connecting_to_text, preferences.getServerName()), true, false);
                    Log.v(TAG, "startConnect, ipPort: " + ipPort);
                    mService.startConnect(ipPort, preferences.getUserName("test"),
                            preferences.getPassword("test1"));
                } catch (IllegalStateException e) {
                    Log.i(TAG, "ProgressDialog.show() was not allowed, connecting aborted: " + e);
                    connectingDialog = null;
                }
            }
        });
    }


    private ProgressDialog connectingDialog = null;


    // Should only be called the UI thread.
    private void setConnected(boolean connected, boolean postConnect, boolean loginFailure) {
        Log.v(TAG, "setConnected(" + connected + ", " + postConnect + ", " + loginFailure + ")");

        // The fragment may no longer be attached to the parent activity.  If so, do nothing.
//        if (!isAdded()) {
//            return;
//        }

        if (postConnect) {
            clearConnectingDialog();
            if (!connected) {
                // TODO: Make this a dialog? Allow the user to correct the
                // server settings here?
                try {
                    Toast.makeText(mActivity, getText(R.string.connection_failed_text),
                            Toast.LENGTH_LONG)
                            .show();
                } catch (IllegalStateException e) {
                    // We are not allowed to show a toast at this point, but
                    // the Toast is not important so we ignore it.
                    Log.i(TAG, "Toast was not allowed: " + e);
                }
            }
        }
        if (loginFailure) {
            Toast.makeText(mActivity, getText(R.string.login_failed_text), Toast.LENGTH_LONG)
                    .show();
            new AuthenticationDialog()
                    .show(mActivity.getSupportFragmentManager(), "AuthenticationDialog");
        }

        if (!connected) {
            updateSongInfo(null, null);

//            playPauseButton.setImageResource(R.drawable.presence_online); // green circle

            if (mFullHeightLayout) {
//                albumArt.setImageResource(R.drawable.icon_album_noart_fullscreen);
//                nextButton.setImageResource(0);
//                prevButton.setImageResource(0);
//                shuffleButton.setImageResource(0);
//                repeatButton.setImageResource(0);
//                updatePlayerDropDown(Collections.<Player>emptyList(), null);
//                artistText.setText(getText(R.string.disconnected_text));
//                currentTime.setText("--:--");
//                totalTime.setText("--:--");
//                seekBar.setEnabled(false);
//                seekBar.setProgress(0);
            } else {
//                albumArt.setImageResource(R.drawable.icon_album_noart);
//                mProgressBar.setEnabled(false);
//                mProgressBar.setProgress(0);
            }
        }
    }

    private void clearConnectingDialog() {
        if (connectingDialog != null && connectingDialog.isShowing()) {
            connectingDialog.dismiss();
        }
        connectingDialog = null;
    }

    private final IServiceCallback serviceCallback = new IServiceCallback() {
        @Override
        public void onPlayStatusChanged(final String playStatusName) {
            uiThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                }
            });
        }

        @Override
        public void onShuffleStatusChanged(final boolean initial, final int shuffleStatusId) {
            uiThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    PlayerState.ShuffleStatus shuffleStatus = PlayerState.ShuffleStatus.valueOf(shuffleStatusId);
                    updateShuffleStatus(shuffleStatus);
                    if (!initial) {
                        Toast.makeText(mActivity,
                                mActivity.getServerString(shuffleStatus.getText()),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        private ProgressDialog connectingDialog = null;

        private void clearConnectingDialog() {
            if (connectingDialog != null && connectingDialog.isShowing()) {
                connectingDialog.dismiss();
            }
            connectingDialog = null;
        }

        private final ServiceConnection serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder binder) {
                Log.v(TAG, "ServiceConnection.onServiceConnected()");
                PhoneService.this.onServiceConnected((ISqueezeService) binder);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                mService = null;
            }
        };




        @Override
        public void onRepeatStatusChanged(final boolean initial, final int repeatStatusId) {
            uiThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    PlayerState.RepeatStatus repeatStatus = PlayerState.RepeatStatus.valueOf(repeatStatusId);
                    updateRepeatStatus(repeatStatus);
                    if (!initial) {
                        Toast.makeText(mActivity, mActivity.getServerString(repeatStatus.getText()),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        @Override
        public void onTimeInSongChange(final int secondsIn, final int secondsTotal) {
            PhoneService.this.secondsIn = secondsIn;
            PhoneService.this.secondsTotal = secondsTotal;
            uiThreadHandler.sendEmptyMessage(UPDATE_TIME);
        }

        @Override
        public void onPowerStatusChanged(final boolean canPowerOn, final boolean canPowerOff) {
            uiThreadHandler.post(new Runnable() {
                public void run() {
                    updatePowerMenuItems(canPowerOn, canPowerOff);
                }
            });
        }

        @Override
        public Object getClient() {
            return PhoneService.this;
        }
    };

    private Song getCurrentSong() {
        PlayerState playerState = getPlayerState();
        return playerState != null ? playerState.getCurrentSong() : null;
    }

    private final IServiceConnectionCallback connectionCallback = new IServiceConnectionCallback() {
        @Override
        public void onConnectionChanged(final boolean isConnected,
                                        final boolean postConnect,
                                        final boolean loginFailed) {
            Log.v(TAG, "Connected == " + isConnected + " (postConnect==" + postConnect + ")");
            uiThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    setConnected(isConnected, postConnect, loginFailed);
                }
            });
        }

        @Override
        public Object getClient() {
            return PhoneService.this;
        }
    };

    private void updateAlbumArt(Song song) {
//        if (song == null || !song.hasArtwork()) {
//            if (mFullHeightLayout) {
//                albumArt.setImageResource(song != null && song.isRemote()
//                        ? R.drawable.icon_iradio_noart_fullscreen
//                        : R.drawable.icon_album_noart_fullscreen);
//            } else {
//                albumArt.setImageResource(song != null && song.isRemote()
//                        ? R.drawable.icon_iradio_noart
//                        : R.drawable.icon_album_noart);
//            }
//            return;
//        }
//
//        // The image fetcher might not be ready yet.
//        if (mImageFetcher == null) {
//            return;
//        }
//
//        mImageFetcher.loadImage(song.getArtworkUrl(mService), albumArt);
    }

    private final IServiceHandshakeCallback handshakeCallback
            = new IServiceHandshakeCallback() {
        @Override
        public void onHandshakeCompleted() {
            uiThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    updatePowerMenuItems(canPowerOn(), canPowerOff());
                }
            });
        }

        @Override
        public Object getClient() {
            return PhoneService.this;
        }
    };


    private final IServicePlayersCallback playersCallback = new IServicePlayersCallback() {
        @Override
        public void onPlayersChanged(final List<Player> players, final Player activePlayer) {
            uiThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    updatePlayerDropDown(players, activePlayer);
                }
            });
        }

        @Override
        public Object getClient() {
            return PhoneService.this;
        }
    };

    private final IServiceVolumeCallback volumeCallback = new IServiceVolumeCallback() {
        @Override
        public void onVolumeChanged(final int newVolume, final Player player) {
            if (!ignoreVolumeChange) {
                mVolumePanel.postVolumeChanged(newVolume, player == null ? "" : player.getName());
            }
        }

        @Override
        public Object getClient() {
            return PhoneService.this;
        }

        @Override
        public boolean wantAllPlayers() {
            return false;
        }
    };

    public void setIgnoreVolumeChange(boolean ignoreVolumeChange) {
        this.ignoreVolumeChange = ignoreVolumeChange;
    }

    public Handler getUIThreadHandler() {
        return uiThreadHandler;
    }

    private void updatePlayerDropDown(@NonNull List<Player> players, @Nullable Player activePlayer) {
//        if (!isAdded()) {
//            return;
//        }

        // Only include players that are connected to the server.
        ArrayList<Player> connectedPlayers = new ArrayList<Player>();
        for (Player player : players) {
            if (player.getConnected()) {
                connectedPlayers.add(player);
            }
        }

        ActionBar actionBar = mActivity.getSupportActionBar();

        // If there are multiple players connected then show a spinner allowing the user to
        // choose between them.
        if (connectedPlayers.size() > 1) {
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
            final ArrayAdapter<Player> playerAdapter = new ArrayAdapter<Player>(
                    mActivity, android.R.layout.simple_spinner_dropdown_item, connectedPlayers) {
                @Override
                public View getDropDownView(int position, View convertView, ViewGroup parent) {
                    return Util.getActionBarSpinnerItemView(mActivity, convertView, parent, getItem(position).getName());
                }

                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    return Util.getActionBarSpinnerItemView(mActivity, convertView, parent, getItem(position).getName());
                }
            };
            actionBar.setListNavigationCallbacks(playerAdapter, new ActionBar.OnNavigationListener() {
                @Override
                public boolean onNavigationItemSelected(int position, long id) {
                    if (!playerAdapter.getItem(position).equals(mService.getActivePlayer())) {
                        Log.i(TAG, "onNavigationItemSelected.setActivePlayer(" + playerAdapter.getItem(position) + ")");
                        mService.setActivePlayer(playerAdapter.getItem(position));
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
    }

    private void updateShuffleStatus(PlayerState.ShuffleStatus shuffleStatus) {
        if (mFullHeightLayout && shuffleStatus != null) {
//            shuffleButton.setImageResource(shuffleStatus.getIcon());
        }
    }

    private void updateRepeatStatus(PlayerState.RepeatStatus repeatStatus) {
        if (mFullHeightLayout && repeatStatus != null) {
//            repeatButton.setImageResource(repeatStatus.getIcon());
        }
    }

    private void updatePowerMenuItems(boolean canPowerOn, boolean canPowerOff) {
        boolean connected = isConnected();

        if (menu_item_poweron != null) {
            if (canPowerOn && connected) {
                Player player = getActivePlayer();
                String playerName = player != null ? player.getName() : "";
                menu_item_poweron.setTitle(getString(R.string.menu_item_poweron, playerName));
                menu_item_poweron.setVisible(true);
            } else {
                menu_item_poweron.setVisible(false);
            }
        }

        if (menu_item_poweroff != null) {
            if (canPowerOff && connected) {
                Player player = getActivePlayer();
                String playerName = player != null ? player.getName() : "";
                menu_item_poweroff.setTitle(getString(R.string.menu_item_poweroff, playerName));
                menu_item_poweroff.setVisible(true);
            } else {
                menu_item_poweroff.setVisible(false);
            }
        }
    }

    private boolean isConnectInProgress() {
        if (mService == null) {
            return false;
        }
        return mService.isConnectInProgress();
    }

    private boolean canPowerOn() {
        if (mService == null) {
            return false;
        }
        return mService.canPowerOn();
    }

    private boolean canPowerOff() {
        if (mService == null) {
            return false;
        }
        return mService.canPowerOff();
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
}
