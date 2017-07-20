package com.kalyan.videonotes.util;

import android.Manifest;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.RequiresPermission;

/**
 * Created by kalyandechiraju on 20/07/17.
 */

public class ConnectionUtil {
    private static ConnectivityManager mConnectivityManager;

    private static ConnectivityManager getConnectivityManager(Context context) {
        if (mConnectivityManager == null)
            mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return mConnectivityManager;
    }
    /**
     * Shows whether you are connected.
     *
     * @param context Context from which this method is called
     * @return true if it is connected to a network, false otherwise
     */
    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    public static boolean isConnected(Context context) {
        NetworkInfo networkInfo = getConnectivityManager(context).getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isAvailable() && networkInfo.isConnected());
    }

    /**
     * Return a name describe the type of the network
     *
     * @param context Context from which this method is called
     * @return name of the network type "WIFI" or "MOBILE" if is connected, null otherwise
     */
    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    public static String getConnectionType(Context context) {
        return isConnected(context) ? getConnectivityManager(context).getActiveNetworkInfo().getTypeName() : null;
    }
}
