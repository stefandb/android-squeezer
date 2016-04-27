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
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import com.mikepenz.iconics.view.IconicsImageView;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import uk.org.ngo.squeezer.dialog.InfoDialog;
import uk.org.ngo.squeezer.dialog.ServerAddressView;
import uk.org.ngo.squeezer.framework.BaseActivity;
import uk.org.ngo.squeezer.framework.ConnectionHelper;
import uk.org.ngo.squeezer.itemlist.SongListActivity;
import uk.org.ngo.squeezer.service.event.HandshakeComplete;
import uk.org.ngo.squeezer.widget.FloatLabelLayout;

/**
 * An activity for when the user is not connected to a Squeezeserver.
 * <p>
 * Provide a UI for connecting to the configured server, launch HomeActivity when the user
 * connects.
 */
public class DisconnectedActivity extends BaseActivity implements ConnectionHelper {

    private IconicsImageView connectionInfo;
    private int disconnectionReason;
    private Preferences mPreferences;

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

    @DisconnectionReasons private int mDisconnectionReason = MANUAL_DISCONNECT;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            //noinspection ResourceType
            mDisconnectionReason = extras.getInt(EXTRA_DISCONNECTION_REASON);
        }

        //Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.disconnected);

        mPreferences = new Preferences(this);

        findViewById(R.id.controls_container).setVisibility(View.GONE);
        serverAddressView = (ServerAddressView) findViewById(R.id.server_address_view);
        mHeaderMessage = (TextView) findViewById(R.id.header_message);

        mHeaderMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InfoDialog.show(getSupportFragmentManager(), R.string.login_failed_info_text);
            }
        });
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
        savePreferences();
        NowPlayingFragment fragment = (NowPlayingFragment) getSupportFragmentManager()
                .findFragmentById(R.id.now_playing_fragment);
        fragment.startVisibleConnection();
    }

    private void savePreferences(){
        String address = "192.168.2.22";

        // Append the default port if necessary.
        if (!address.contains(":")) {
            address += ":" + getResources().getInteger(R.integer.DefaultPort);
        }

        Preferences.ServerAddress serverAddress = mPreferences.saveServerAddress(address);

        final String serverName = getServerName(address);
        if (serverName != null) {
            mPreferences.saveServerName(serverAddress, serverName);
        }

        final String userName = "";
        final String password = "";
        mPreferences.saveUserCredentials(serverAddress, userName, password);
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
}
