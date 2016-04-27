package uk.org.ngo.squeezer.framework;

import java.util.Map;

/**
 * Created by Stefan on 27-4-2016.
 */
public interface ConnectionHelper {
    
    public String getServerName(String ipPort);

    public String getSewszrverName(String ipPort) {
        if (mDiscoveredServers != null)
            for (Map.Entry<String, String> entry : mDiscoveredServers.entrySet())
                if (ipPort.equals(entry.getValue()))
                    return entry.getKey();
        return null;
    }



}
