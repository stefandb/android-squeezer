/*
 * Copyright (c) 2013 Google Inc.  All Rights Reserved.
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
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.mikepenz.iconics.view.IconicsImageView;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Map;
import java.util.TreeMap;

import uk.org.ngo.squeezer.dialog.InfoDialog;
import uk.org.ngo.squeezer.dialog.ServerAddressView;
import uk.org.ngo.squeezer.framework.BaseActivity;
import uk.org.ngo.squeezer.framework.ConnectionHelper;
import uk.org.ngo.squeezer.itemlist.SongListActivity;
import uk.org.ngo.squeezer.service.event.HandshakeComplete;
import uk.org.ngo.squeezer.util.ScanNetworkTask;
import uk.org.ngo.squeezer.widget.FloatLabelLayout;

/**
 * An activity for when the user is not connected to a Squeezeserver.
 * <p>
 * Provide a UI for connecting to the configured server, launch HomeActivity when the user
 * connects.
 */
public class DisconnectedActivity extends BaseActivity implements ScanNetworkTask.ScanNetworkCallback{

    private IconicsImageView connectionInfo;
    private int disconnectionReason;
    private Button scanButton;


    public void setDisconnectionReason(int reason) {
        disconnectionReason = reason;
    }

    @IntDef({MANUAL_DISCONNECT, CONNECTION_FAILED, LOGIN_FAILED})
    @Retention(RetentionPolicy.SOURCE)
    public @interface DisconnectionReasons {}
    public static final int MANUAL_DISCONNECT = 0;
    public static final int CONNECTION_FAILED = 1;
    public static final int LOGIN_FAILED = 2;

    private static final String EXTRA_DISCONNECTION_REASON = "reason";

    private ServerAddressView serverAddressView;

    private TextView mHeaderMessage;

    private ConnectionHelper ConnectionHelper= null;
    private EditText mServerAddressEditText;
    private FloatLabelLayout mServerAddressLabel;
    private RelativeLayout toggle_view;
    private IconicsImageView togglebtn;
    private View mScanProgress;

    private Spinner mServersSpinner;
    private ArrayAdapter<String> mServersAdapter;

    private ScanNetworkTask mScanNetworkTask;

    /** Map server names to IP addresses. */
    private TreeMap<String, String> mDiscoveredServers;


    @DisconnectionReasons private int mDisconnectionReason = MANUAL_DISCONNECT;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ConnectionHelper = (ConnectionHelper) new ConnectionHelper(this, getResources());

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            //noinspection ResourceType
            mDisconnectionReason = extras.getInt(EXTRA_DISCONNECTION_REASON);
        }

        //Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.disconnected);

        findViewById(R.id.controls_container).setVisibility(View.GONE);

        mHeaderMessage = (TextView) findViewById(R.id.header_message);
        toggle_view = (RelativeLayout) findViewById(R.id.toggle_view);
        mServerAddressEditText = (EditText) findViewById(R.id.server_address);
        mServerAddressLabel = (FloatLabelLayout) findViewById(R.id.server_address_label);
        togglebtn = (IconicsImageView) findViewById(R.id.toggle);
        mServersSpinner = (Spinner) findViewById(R.id.found_servers);
        mScanProgress = findViewById(R.id.scan_progress);

        TextView scanDisabledMessage = (TextView) findViewById(R.id.scan_disabled_msg);
        scanButton = (Button) findViewById(R.id.scan_button);

        // Set up the servers spinner.
        mServersAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
        mServersAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        mServersSpinner.setAdapter(mServersAdapter);
        mServersSpinner.setOnItemSelectedListener(new MyOnItemSelectedListener());
        mServerAddressLabel.setOnLongClickListener(new ToggleServerInput());
        togglebtn.setOnClickListener(new ToggleServerInput());

        // Only support network scanning on WiFi.
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = connectivityManager.getActiveNetworkInfo();
        boolean isWifi = ni != null && ni.getType() == ConnectivityManager.TYPE_WIFI;
        if (isWifi) {
            startNetworkScan();
            scanButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    startNetworkScan();
                }
            });
        } else {
            scanDisabledMessage.setVisibility(View.VISIBLE);
            scanButton.setVisibility(View.GONE);
            togglebtn.setVisibility(View.GONE);
            mServerAddressLabel.setVisibility(View.VISIBLE);
            mServersSpinner.setVisibility(View.GONE);
            mServerAddressEditText.setText("");
            //TODO-stefan in de resume dit opnieuw controleren
        }


    }

    //TODO-stefan verplaatsen naar onpauze?
    public void onDismiss() {
        // Stop scanning
        if (mScanNetworkTask != null) {
            mScanNetworkTask.cancel(true);
        }
    }


    /**
     * Show this activity.
     * <p>
     * Flags are set to clear the previous activities, as trying to go back while disconnected makes
     * no sense.
     * <p>
     * The pending transition is overridden to animate the activity in place, rather than having it
     * appear to move in from off-screen.
     *
     * @param disconnectionReason identifies why the activity is being shown.
     */
    private static void show(Activity activity, @DisconnectionReasons int disconnectionReason) {
        // If the activity is already running then make sure the header message is appropriate
        // and stop, as there's no need to start another instance of the activity.
        if (activity instanceof DisconnectedActivity) {

            ((DisconnectedActivity) activity).ToggleInformationIcon(disconnectionReason);
            ((DisconnectedActivity) activity).setDisconnectionReason(disconnectionReason);
            return;
        }

        final Intent intent = new Intent(activity, DisconnectedActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        intent.putExtra(EXTRA_DISCONNECTION_REASON, disconnectionReason);
        activity.startActivity(intent);
        activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    public void ToggleInformationIcon(int disconnectionReason){
        int textMessage = (R.string.connecting_text);
        switch (disconnectionReason) {
            case CONNECTION_FAILED:
                textMessage = (R.string.connection_failed_text);
                break;
            case LOGIN_FAILED:
                textMessage = (R.string.login_failed_text);
                break;
        }

        Toast.makeText(getApplicationContext(), textMessage, Toast.LENGTH_SHORT).show();

        if(mDisconnectionReason == CONNECTION_FAILED || mDisconnectionReason == LOGIN_FAILED){
            connectionInfo.setVisibility(View.VISIBLE);
        }
    }

    private View.OnClickListener _InformationOnclickListner(){
        return new IconicsImageView.OnClickListener() {
            @Override
            public void onClick(View v) {
                int textMessage = (R.string.connecting_text);
                connectionInfo.setVisibility(View.VISIBLE);
                switch (disconnectionReason) {
                    case CONNECTION_FAILED:
                        textMessage = (R.string.connection_failed_text);
                        break;
                    case LOGIN_FAILED:
                        textMessage = (R.string.login_failed_text);
                        break;
                }
                InfoDialog.show(getSupportFragmentManager(), textMessage);
            }
        };
    }

    /**
     * Show this activity.
     * @see #show(android.app.Activity)
     */
    public static void show(Activity activity) {
        show(activity, MANUAL_DISCONNECT);
    }

    /**
     * Show this activity on login failure.
     * @see #show(android.app.Activity)
     */
    public static void showLoginFailed(Activity activity) {
        show(activity, LOGIN_FAILED);
    }

    public static void showConnectionFailed(Activity activity) {
        show(activity, CONNECTION_FAILED);
    }

    /**
     * Act on the user requesting a server connection through the activity's UI.
     *
     * @param view The view the user pressed.
     */
    public void onUserInitiatesConnect(View view) {
        ConnectionHelper.savePreferences(mServerAddressEditText.getText().toString());

        NowPlayingFragment fragment = (NowPlayingFragment) getSupportFragmentManager()
                .findFragmentById(R.id.now_playing_fragment);


        fragment.startVisibleConnection();
    }

    public void onEventMainThread(HandshakeComplete event) {
        // The user requested a connection to the server, which succeeded.  There's
        // no prior activity to go to, so launch HomeActivity, with flags to
        // clear other activities so hitting "back" won't show this activity again.
        final Intent intent = new Intent(this, SongListActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        this.startActivity(intent);
        this.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    /**
     * Starts scanning for servers.
     */
    void startNetworkScan() {
        scanButton.setVisibility(View.GONE);
        mScanProgress.setVisibility(View.VISIBLE);
        mScanNetworkTask = new ScanNetworkTask(this, this);
        mScanNetworkTask.execute();
    }


    /**
     * Called when server scanning has finished.
     * @param serverMap Discovered servers, key is the server name, value is the IP address.
     */
    public void onScanFinished(TreeMap<String, String> serverMap) {
        mScanProgress.setVisibility(View.GONE);
        scanButton.setVisibility(View.VISIBLE);

        if (mScanNetworkTask == null) {
            return;
        }

        mDiscoveredServers = serverMap;

        ConnectionHelper.setmDiscoveredServers(mDiscoveredServers);

        mScanNetworkTask = null;

        switch (mDiscoveredServers.size()) {
            case 0:
                mServersSpinner.setVisibility(View.GONE);
                mServerAddressLabel.setVisibility(View.VISIBLE);
                mServerAddressEditText.setText("");
                break;
            default:
                mServersSpinner.setVisibility(View.VISIBLE);
                mServerAddressLabel.setVisibility(View.GONE);

                // Show the spinner so the user can choose a server.
                mServersAdapter.clear();
                for (Map.Entry<String, String> e : mDiscoveredServers.entrySet()) {
                    mServersAdapter.add(e.getKey());
                }

                //TODO-stefan selecteer het laatst geselecteerde item
//                int position = getServerPosition(mServerAddressEditText.getText().toString());
//                if (position >= 0) mServersSpinner.setSelection(position);
                mServersAdapter.notifyDataSetChanged();
            break;
        }
    }

    /**
     * Inserts the selected address in to the edit text widget.
     */
    private class MyOnItemSelectedListener implements AdapterView.OnItemSelectedListener {
        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            String serverAddress = mDiscoveredServers.get(parent.getItemAtPosition(pos).toString());
            mServerAddressEditText.setText(serverAddress);
        }

        public void onNothingSelected(AdapterView<?> parent) {
            // Do nothing.
        }
    }

    private class ToggleServerInput implements View.OnLongClickListener, View.OnClickListener {
        @Override
        public boolean onLongClick(View v)
        {
            toggle(v);
            return true;
        }

        private void toggle(View v){
            DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
            int dp = Math.round(toggle_view.getHeight() / (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));

            Log.d("height", "NIEUW");
            Log.d("height", String.valueOf(dp));
            Log.d("height", String.valueOf(toggle_view.getMeasuredHeight()));

            if(mServersSpinner.getVisibility() == View.VISIBLE){
                mServersSpinner.setVisibility(View.GONE);
                mServerAddressLabel.setVisibility(View.VISIBLE);
            }else{
                mServerAddressLabel.setVisibility(View.GONE);
                mServersSpinner.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onClick(View v) {
            toggle(v);
        }
    }

}
