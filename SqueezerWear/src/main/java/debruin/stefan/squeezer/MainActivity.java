package debruin.stefan.squeezer;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class MainActivity extends Activity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, DataApi.DataListener, MessageApi.MessageListener,
        NodeApi.NodeListener {


    private GoogleApiClient googleClient;

    private ImageButton mbtnprevious;
    private ImageButton mbtnnext;
    private ImageButton mbtnplay;
    private TextView mTitle;
    private TextView mArtist;

    private int playstatus = 0;
    //0 = er word geen muziek afgespeeld
    //1 = er word wel muziek afgespeeld


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {

                mbtnnext = (ImageButton) stub.findViewById(R.id.btnnext);
                mbtnplay = (ImageButton) stub.findViewById(R.id.btnplay);
                mbtnprevious = (ImageButton) stub.findViewById(R.id.btnprevious);

                mTitle = (TextView) stub.findViewById(R.id.txttitle);
                mArtist = (TextView) stub.findViewById(R.id.txtatrist);

                mbtnprevious.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
//                        mTitle.setText("Live @ ASOT 650 stage");
//                        mArtist.setText("Andrew rayel");

                        try {
                            JSONObject action = new JSONObject();;

                            action.put("action", "previous");

                            Log.d("wear:squeezer-plat-action", action.toString());
                            new SendToDataLayerThread(ListenerService.DATA_ACTION+ "/previous", action.toString()).start();
                            Log.d("wear:squeezer", "vorige");
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Log.d("wear:squeezer", "ERROR");
                        }
                    }
                });

                mbtnplay.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            JSONObject action = new JSONObject();

                            if(playstatus == 1){
                                action.put("action", "stop");
                                mbtnplay.setImageResource(R.drawable.ic_action_pause);
                                playstatus = 0;
                            }else{
                                action.put("action", "play");
                                mbtnplay.setImageResource(R.drawable.ic_action_play);
                                playstatus = 1;
                            }

                            Log.d("wear:squeezer-plat-action", action.toString());
                            new SendToDataLayerThread(ListenerService.DATA_ACTION + "/play", action.toString()).start();
                            Log.d("wear:squeezer", "play");
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Log.d("wear:squeezer", e.toString());
                        }
                    }
                });

                mbtnnext.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            JSONObject action = new JSONObject();;

                            action.put("action", "next");

                            Log.d("wear:squeezer-plat-action", action.toString());
                            new SendToDataLayerThread(ListenerService.DATA_ACTION+ "/next", action.toString()).start();
                            Log.d("wear:squeezer", "volgende");
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Log.d("wear:squeezer", "ERROR");
                        }
                    }
                });

            }
        });

        IntentFilter messageFilter = new IntentFilter(Intent.ACTION_SEND);
        MessageReceiver messageReceiver = new MessageReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, messageFilter);
    }

    private void SetSong(final String title, final JSONObject text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d("wear:squeezer", text.toString());
                JSONObject numberinfo = null;
                try {
                    mTitle.setText(text.getString("title"));
                    mArtist.setText(text.getString("artist"));

                    if(text.getString("status") == "play"){
                        mbtnplay.setBackgroundResource(R.drawable.ic_action_pause);
                        playstatus = 1;
                    }else{
                        mbtnplay.setBackgroundResource(R.drawable.ic_action_play);
                        playstatus = 0;
                    }
//                mTitle.setText(numberinfo.getString("title"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.d("wear:squeezer", "onDataChanged");

        final List<DataEvent> events = FreezableUtils.freezeIterable(dataEvents);
        dataEvents.close();
        for (DataEvent event : events) {
            String path = event.getDataItem().getUri().getPath();
            if (ListenerService.DATA_CURRENT_SONG.equals(path)) {
                Log.d("wear:squeezer", "Data Changed for COUNT_PATH");
                try {
                    SetSong("DataItem Changed", new JSONObject(String.valueOf(event.getDataItem())));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            if (String.valueOf(event.getType()) == "/squeezer_current") {


            } else if (event.getType() == DataEvent.TYPE_DELETED) {
//                generateEvent("DataItem Deleted", event.getDataItem().toString());
            } else {
                Log.d("mobile:squeezer-data", String.valueOf(event.getType()));
//                generateEvent("Unknown data event type", "Type = " + event.getType());
            }
        }
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d("wear:squeezer-function", "onMessageReceived homeactivity");
        Log.d("wear:squeezer-path", messageEvent.getPath());

        if (messageEvent.getPath().equals(ListenerService.DATA_CURRENT_SONG)) {
//            final String message = new String(messageEvent.getData());
            JSONObject message = null;
            try {
                message = new JSONObject(String.valueOf(messageEvent.getData()));
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Log.d("wear:squeezer-message", "Message path received on watch is: " + messageEvent.getPath());
            Log.d("wear:squeezer-message", "Message received on watch is: " + message.toString());
            Log.d("TEST","TEST");
            try {
                Log.d("numbertitle", message.getString("title"));
                mTitle.setText(message.getString("title"));
                mArtist.setText(message.getString("artist"));

                if(message.getString("status") == "play"){
                    mbtnplay.setBackgroundResource(R.drawable.ic_action_pause);
                    playstatus = 1;
                }else{
                    mbtnplay.setBackgroundResource(R.drawable.ic_action_play);
                    playstatus = 0;
                }
//                mTitle.setText(numberinfo.getString("title"));
            } catch (JSONException e) {
                Log.d("wear:squeezer-error", e.toString());
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onPeerConnected(Node node) {

    }

    @Override
    public void onPeerDisconnected(Node node) {

    }

    public class MessageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            JSONObject numberinfo = null;
            try {
                Log.d("wear:squeezer",intent.getStringExtra("message").toString());
                numberinfo = new JSONObject(intent.getStringExtra("message"));

                mTitle.setText(numberinfo.getString("title"));
                mArtist.setText(numberinfo.getString("artist"));

                if(numberinfo.getString("status") == "play"){
                    mbtnplay.setBackgroundResource(R.drawable.ic_action_pause);
                    playstatus = 1;
                }else{
                    mbtnplay.setBackgroundResource(R.drawable.ic_action_play);
                    playstatus = 0;
                }
//                mTitle.setText(numberinfo.getString("title"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        googleClient.connect();

        JSONObject action = new JSONObject();
        try {
            action.put("action", "getcurrent");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.d("squeezer-send-current", action.toString());
        new SendToDataLayerThread(ListenerService.DATA_ACTION+ "/current", action.toString()).start();
        Log.d("wear", "get current");
    }

    // Send a message when the data layer connection is successful.
    @Override
    public void onConnected(Bundle connectionHint) {
//        String message = "Live @ ASOT 650 stage";
//        JSONObject numberinfo = new JSONObject();
//        try {
//            numberinfo.put("title", "Live @ ASOT 650 stage");
//            numberinfo.put("artist", "Andrew Rayel");
//            numberinfo.put("album", "ASOT 650 Miami");
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//
//        //Requires a new thread to avoid blocking the UI
//        new SendToDataLayerThread("/squeezer/current", numberinfo.toString()).start();
    }

    // Disconnect from the data layer when the Activity stops
    @Override
    protected void onStop() {
        if (null != googleClient && googleClient.isConnected()) {
            googleClient.disconnect();
        }
        super.onStop();
    }

    // Placeholders for required connection callbacks
    @Override
    public void onConnectionSuspended(int cause) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
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
            NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(googleClient).await();
            for (Node node : nodes.getNodes()) {
                MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(googleClient, node.getId(), path, message.getBytes()).await();
                if (result.getStatus().isSuccess()) {
                    Log.d("wear:squeezer-run", "Message: {" + message + "} sent to: " + node.getDisplayName());
                }
                else {
                    Log.d("wear:squeezer-run", "ERROR: failed to send Message");
                }
            }
        }
    }
}
