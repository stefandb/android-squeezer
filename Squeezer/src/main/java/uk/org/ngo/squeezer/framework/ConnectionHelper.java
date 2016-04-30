package uk.org.ngo.squeezer.framework;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

import java.util.Map;
import java.util.TreeMap;

import uk.org.ngo.squeezer.DisconnectedActivity;
import uk.org.ngo.squeezer.Preferences;
import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.Util;

/**
 * Created by Stefan on 27-4-2016.
 */
public class ConnectionHelper {

    private Resources resources= null;
    private Preferences mPreferences;
    private Context mActivity = null;

    private TreeMap<String, String> mDiscoveredServers;

    public ConnectionHelper(Context mActivity, Resources r) {
        resources = r;
        mPreferences = new Preferences(mActivity);
        this.mActivity = mActivity;
    }

    public String getServerName(String ipPort) {
        if (mDiscoveredServers != null)
            for (Map.Entry<String, String> entry : mDiscoveredServers.entrySet())
                if (ipPort.equals(entry.getValue()))
                    return entry.getKey();
        return null;
    }

    public void savePreferences(String address){
        Preferences.ServerAddress serverAddress = mPreferences.getServerAddress();
        String username = "";
        String password = "";
        if(mPreferences.getUserName(serverAddress) != null){
            username = mPreferences.getUserName(serverAddress);
        }

        if(mPreferences.getPassword(serverAddress) != null){
            password = mPreferences.getPassword(serverAddress);
        }

        savePreferences(address, username, password);
    }

    public void savePreferences(String address, String userName, String password){
        if (!address.contains(":")) {
            address += ":" + resources.getInteger(R.integer.DefaultPort);
        }

        Preferences.ServerAddress serverAddress = mPreferences.saveServerAddress(address);

        final String serverName = getServerName(address);
        if (serverName != null) {
            mPreferences.saveServerName(serverAddress, serverName);
        }

        mPreferences.saveUserCredentials(serverAddress, userName, password);
    }

    private int getServerPosition(String ipPort) {
        if (mDiscoveredServers != null) {
            String host = Util.parseHost(ipPort);
            int position = 0;
            for (Map.Entry<String, String> entry : mDiscoveredServers.entrySet()) {
                if (host.equals(entry.getValue()))
                    return position;
                position++;
            }
        }
        return -1;
    }

    public void setmDiscoveredServers(TreeMap<String, String> mDiscoveredServers) {
        this.mDiscoveredServers = mDiscoveredServers;
    }

    public TreeMap<String, String> getmDiscoveredServers() {
        return mDiscoveredServers;
    }
}
