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
public class ServerAddressView extends LinearLayout implements ScanNetworkTask.ScanNetworkCallback, ConnectionHelper {
    private Preferences mPreferences;
    private String mBssId;

    private EditText mServerAddressEditText;
    private TextView mServerName;
    private Spinner mServersSpinner;
    private EditText mUserNameEditText;
    private EditText mPasswordEditText;
    private View mScanResults;
    private View mScanProgress;

    private ScanNetworkTask mScanNetworkTask;

    /** Map server names to IP addresses. */
    private TreeMap<String, String> mDiscoveredServers;

    private ArrayAdapter<String> mServersAdapter;
    private FloatLabelLayout mPasswordLabel;
    private FloatLabelLayout mUserNameLabel;
    private FloatLabelLayout mServerAddressLabel;
    private IconicsImageView togglebtn;
    private RelativeLayout toggle_view;

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
        if (!isInEditMode()) {
            mPreferences = new Preferences(context);
            Preferences.ServerAddress serverAddress = mPreferences.getServerAddress();
            mBssId = serverAddress.bssId;


            toggle_view = (RelativeLayout) findViewById(R.id.toggle_view);


            mServerAddressEditText = (EditText) findViewById(R.id.server_address);
            mServerAddressLabel = (FloatLabelLayout) findViewById(R.id.server_address_label);
            mServerAddressLabel.setOnLongClickListener(new ToggleServerInput());
            mUserNameEditText = (EditText) findViewById(R.id.username);
            mUserNameLabel = (FloatLabelLayout) findViewById(R.id.label_username);
            mPasswordEditText = (EditText) findViewById(R.id.password);
            mPasswordLabel = (FloatLabelLayout) findViewById(R.id.label_password);
            setServerAddress(serverAddress.address);

            // Set up the servers spinner.
            mServersAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item);
            mServersAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            mServerName = (TextView) findViewById(R.id.server_name);
            togglebtn = (IconicsImageView) findViewById(R.id.toggle);
            togglebtn.setOnClickListener(new ToggleServerInput());


            mServersSpinner = (Spinner) findViewById(R.id.found_servers);
            mServersSpinner.setAdapter(mServersAdapter);
            mServersSpinner.setOnItemSelectedListener(new MyOnItemSelectedListener());
//            mServersSpinner.setOnLongClickListener(new ToggleServerInput());

            mScanResults = findViewById(R.id.scan_results);
            mScanProgress = findViewById(R.id.scan_progress);
            mScanProgress.setVisibility(GONE);
            TextView scanDisabledMessage = (TextView) findViewById(R.id.scan_disabled_msg);

            // Only support network scanning on WiFi.
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo ni = connectivityManager.getActiveNetworkInfo();
            boolean isWifi = ni != null && ni.getType() == ConnectivityManager.TYPE_WIFI;
            if (isWifi) {
                scanDisabledMessage.setVisibility(GONE);
                startNetworkScan(context);
                Button scanButton = (Button) findViewById(R.id.scan_button);
                scanButton.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        startNetworkScan(context);
                    }
                });
            } else {
                mScanResults.setVisibility(GONE);
            }
        }
    }

    public void savePreferences() {
        String address = mServerAddressEditText.getText().toString();

        // Append the default port if necessary.
        if (!address.contains(":")) {
            address += ":" + getResources().getInteger(R.integer.DefaultPort);
        }

        Preferences.ServerAddress serverAddress = mPreferences.saveServerAddress(address);

        final String serverName = getServerName(address);
        if (serverName != null) {
            mPreferences.saveServerName(serverAddress, serverName);
        }

        final String userName = mUserNameEditText.getText().toString();
        final String password = mPasswordEditText.getText().toString();
        mPreferences.saveUserCredentials(serverAddress, userName, password);
    }

    public void onDismiss() {
        // Stop scanning
        if (mScanNetworkTask != null) {
            mScanNetworkTask.cancel(true);
        }
    }

    /**
     * Starts scanning for servers.
     */
    void startNetworkScan(Context context) {
        mScanResults.setVisibility(GONE);
        mScanProgress.setVisibility(VISIBLE);
        mScanNetworkTask = new ScanNetworkTask(context, this);
        mScanNetworkTask.execute();
    }

    /**
     * Called when server scanning has finished.
     * @param serverMap Discovered servers, key is the server name, value is the IP address.
     */
    public void onScanFinished(TreeMap<String, String> serverMap) {
        mScanResults.setVisibility(VISIBLE);
        mServerName.setVisibility(GONE);
        mServersSpinner.setVisibility(GONE);
        mScanProgress.setVisibility(GONE);

        if (mScanNetworkTask == null) {
            return;
        }

        mDiscoveredServers = serverMap;
        mDiscoveredServers.put("dummy 1", "192.168.2.10");
        mDiscoveredServers.put("dummy 2", "192.168.2.12");
        mDiscoveredServers.put("dummy 3", "192.168.2.13");
        mDiscoveredServers.put("dummy 4", "192.168.2.14");
        mDiscoveredServers.put("dummy 5", "192.168.2.15");
        mDiscoveredServers.put("Custom", "0");

        mScanNetworkTask = null;

        switch (mDiscoveredServers.size()) {
            case 0:
                // Do nothing, no servers found.
                break;

            case 1:
                // Populate the edit text widget with the address found.
                setServerAddress(mDiscoveredServers.get(mDiscoveredServers.firstKey()));
                mServerName.setVisibility(VISIBLE);
                mServerName.setText(mDiscoveredServers.firstKey());
                break;

            default:
                // Show the spinner so the user can choose a server.
                mServersAdapter.clear();
                for (Entry<String, String> e : mDiscoveredServers.entrySet()) {
                    Log.d("device", e.getKey());
                    mServersAdapter.add(e.getKey());
                }

                int position = getServerPosition(mServerAddressEditText.getText().toString());
                if (position >= 0) mServersSpinner.setSelection(position);
                mServersSpinner.setVisibility(VISIBLE);
                mServersAdapter.notifyDataSetChanged();
        }
    }

    private void setServerAddress(String address) {
        String currentHostPort = mServerAddressEditText.getText().toString();
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

        //TODO-stefan select the correct one in the dropdown
        mServerAddressEditText.setText(serverAddress.address);

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

    private int getServerPosition(String ipPort) {
        if (mDiscoveredServers != null) {
            String host = Util.parseHost(ipPort);
            int position = 0;
            for (Entry<String, String> entry : mDiscoveredServers.entrySet()) {
                if (host.equals(entry.getValue()))
                    return position;
                position++;
            }
        }
        return -1;
    }

    /**
     * Inserts the selected address in to the edit text widget.
     */
    private class MyOnItemSelectedListener implements OnItemSelectedListener {
        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            String serverAddress = mDiscoveredServers.get(parent.getItemAtPosition(pos).toString());
            setServerAddress(serverAddress);
        }

        public void onNothingSelected(AdapterView<?> parent) {
            // Do nothing.
        }
    }

    private class ToggleServerInput implements OnLongClickListener, OnClickListener{
        @Override
        public boolean onLongClick(View v)
        {
            toggle(v);
            return true;
        }

        private void toggle(View v){
            DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
            int dp = Math.round(toggle_view.getHeight() / (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));

            Log.d("height", "NIEUW");
            Log.d("height", String.valueOf(dp));
            Log.d("height", String.valueOf(toggle_view.getMeasuredHeight()));

            if(mServersSpinner.getVisibility() == VISIBLE){
                mServersSpinner.setVisibility(GONE);
                mServerAddressLabel.setVisibility(VISIBLE);
            }else{
                mServerAddressLabel.setVisibility(GONE);
                mServersSpinner.setVisibility(VISIBLE);
            }

        }

        @Override
        public void onClick(View v) {
            toggle(v);
        }
    }

}
