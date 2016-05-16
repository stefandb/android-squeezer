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

package uk.org.ngo.squeezer.dialog;

import android.content.Context;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.mikepenz.iconics.view.IconicsImageView;
import com.mikepenz.materialdrawer.adapter.BaseDrawerAdapter;

import java.util.Map.Entry;
import java.util.TreeMap;

import uk.org.ngo.squeezer.Preferences;
import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.Util;
import uk.org.ngo.squeezer.framework.ConnectionHelper;
import uk.org.ngo.squeezer.util.ScanNetworkTask;
import uk.org.ngo.squeezer.widget.FloatLabelLayout;

/**
 * Scans the local network for servers, allow the user to choose one, set it as the preferred server
 * for this network, and optionally enter authentication information.
 * <p>
 * A new network scan can be initiated manually if desired.
 */
public class ServerAddressView extends LinearLayout {
    private Preferences mPreferences;
    private String mBssId;

    private EditText mUserNameEditText;
    private EditText mPasswordEditText;

    private FloatLabelLayout mPasswordLabel;
    private FloatLabelLayout mUserNameLabel;

    private ConnectionHelper ConnectionHelper= null;

    public ServerAddressView(final Context context) {
        super(context);
        initialize(context);
    }

    public ServerAddressView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    private void initialize(final Context context) {
        inflate(context, R.layout.server_address_view, this);

        ConnectionHelper = (ConnectionHelper) new ConnectionHelper(context, getResources());

        if (!isInEditMode()) {
            mUserNameEditText = (EditText) findViewById(R.id.username);
            mUserNameLabel = (FloatLabelLayout) findViewById(R.id.label_username);
            mPasswordEditText = (EditText) findViewById(R.id.password);
            mPasswordLabel = (FloatLabelLayout) findViewById(R.id.label_password);

            //TODO-stefan START
            mPreferences = new Preferences(context);
            Preferences.ServerAddress serverAddress = mPreferences.getServerAddress();
            mBssId = serverAddress.bssId;
            setServerAddress(serverAddress.address);
            //TODO-stefan END
        }
    }

    //TODO-stefan verplaatsen naar disconnectedActivity
    private void setServerAddress(String address) {
        //TODO-stefan haal de huidige speler op voor het IP adres
        String currentHostPort = "192.168.2.22";
        String currentHost = Util.parseHost(currentHostPort);
        int currentPort = Util.parsePort(currentHostPort);

        String host = Util.parseHost(address);
        int port = Util.parsePort(address);

        if (host.equals(currentHost)) {
            port = currentPort;
        }

        Preferences.ServerAddress serverAddress = new Preferences.ServerAddress();
        serverAddress.bssId = mBssId;
        serverAddress.address = host + ":" + port;

        if(mPreferences.getUserName(serverAddress) != null){
            mUserNameEditText.setText(mPreferences.getUserName(serverAddress));
            mUserNameLabel.setVisibility(VISIBLE);
        }else{
            mUserNameLabel.setVisibility(GONE);
        }

        if(mPreferences.getPassword(serverAddress) != null) {
            mPasswordEditText.setText(mPreferences.getPassword(serverAddress));
            mPasswordLabel.setVisibility(VISIBLE);
        }else{
            mPasswordLabel.setVisibility(GONE);
        }
    }

    public void savePreferences(){
        ConnectionHelper.savePreferences("192.168.2.22", mUserNameEditText.getText().toString(), mPasswordEditText.getText().toString());
    }
}
