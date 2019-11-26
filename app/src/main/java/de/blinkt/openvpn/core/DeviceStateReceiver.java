/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */
package de.blinkt.openvpn.core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.os.Handler;
import android.util.Log;

import de.blinkt.openvpn.core.VpnStatus.ByteCountListener;
import uk.vpn.vpnuk.R;
import uk.vpn.vpnuk.local.Settings;

import static de.blinkt.openvpn.core.OpenVPNManagement.pauseReason;

public class DeviceStateReceiver extends BroadcastReceiver implements ByteCountListener, OpenVPNManagement.PausedStateCallback {
    private final Handler mDisconnectHandler;
    // Data traffic limit in bytes
    private final Settings settings;
    connectState network = connectState.DISCONNECTED;
    connectState userpause = connectState.SHOULDBECONNECTED;
    private OpenVPNManagement mManagement;
    private String lastStateMsg = null;
    private java.lang.Runnable mDelayDisconnectRunnable = new Runnable() {
        @Override
        public void run() {
            if (!(network == connectState.PENDINGDISCONNECT)) return;
            network = connectState.DISCONNECTED;
            if (settings.getReconnect()) {
                mManagement.pause(getPauseReason());
            } else {
                tryToStopVpn();
            }
        }
    };
    private NetworkInfo lastConnectedNetwork;
    private Context context;

    public DeviceStateReceiver(Context context, OpenVPNManagement magnagement, Settings settings) {
        super();
        this.context = context;
        Log.e("network", "create new receiver");
        mManagement = magnagement;
        this.settings = settings;
        mManagement.setPauseCallback(this);
        mDisconnectHandler = new Handler();
    }

    public static boolean equalsObj(Object a, Object b) {
        return (a == null) ? (b == null) : a.equals(b);
    }

    @Override
    public boolean shouldBeRunning() {
        return shouldBeConnected();
    }

    @Override
    public void updateByteCount(long in, long out, long diffIn, long diffOut) {

    }

    public void userPause(boolean pause) {
        if (pause) {
            userpause = connectState.DISCONNECTED;
            // Check if we should disconnect
            mManagement.pause(getPauseReason());
        } else {
            boolean wereConnected = shouldBeConnected();
            userpause = connectState.SHOULDBECONNECTED;
            if (shouldBeConnected() && !wereConnected) mManagement.resume();
            else
                // Update the reason why we currently paused
                mManagement.pause(getPauseReason());
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e("intent", "action " + intent.getAction());
        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
            networkStateChange(context);
        }
    }

    public void networkStateChange(Context context) {
        NetworkInfo networkInfo = getCurrentNetworkInfo(context);
        boolean sendusr1 = true;//false;//prefs.getBoolean("netchangereconnect", true);
        String netstatestring;
        if (networkInfo == null) {
            netstatestring = "not connected";
        } else {
            String subtype = networkInfo.getSubtypeName();
            if (subtype == null) subtype = "";
            String extrainfo = networkInfo.getExtraInfo();
            if (extrainfo == null) extrainfo = "";
            /*
            if(networkInfo.getType()==android.net.ConnectivityManager.TYPE_WIFI) {
				WifiManager wifiMgr = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
				WifiInfo wifiinfo = wifiMgr.getConnectionInfo();
				extrainfo+=wifiinfo.getBSSID();
				subtype += wifiinfo.getNetworkId();
			}*/
            netstatestring = String.format("%2$s %4$s to %1$s %3$s", networkInfo.getTypeName(), networkInfo.getDetailedState(), extrainfo, subtype);
        }
//        Log.e("network", "start ("+ network + ") " + netstatestring);
        if (networkInfo != null && networkInfo.getState() == State.CONNECTED) {
            boolean pendingDisconnect = (network == connectState.PENDINGDISCONNECT);
            network = connectState.SHOULDBECONNECTED;
            boolean sameNetwork;
            sameNetwork = lastConnectedNetwork != null && lastConnectedNetwork.getType() == networkInfo.getType()
                    && equalsObj(lastConnectedNetwork.getExtraInfo(), networkInfo.getExtraInfo());
            /* Same network, connection still 'established' */
//            Log.e("network", "network connected same network = (" + sameNetwork+")");
            if (pendingDisconnect && sameNetwork) {
                Log.e("network", "pending and same");
                mDisconnectHandler.removeCallbacks(mDelayDisconnectRunnable);
                // Reprotect the sockets just be sure
                mManagement.networkChange(true);
            } else {
                /* Different network or connection not established anymore */
//                if (screen == connectState.PENDINGDISCONNECT) screen = connectState.DISCONNECTED;
//                Log.e("network1", "different network should be connected =(" + shouldBeConnected()+")");
//                Log.e("network1", "different network same network= ("+sameNetwork+") pendingDisconnect =(" + pendingDisconnect +")" );
                if (shouldBeConnected()) {
                    mDisconnectHandler.removeCallbacks(mDelayDisconnectRunnable);
                    if (pendingDisconnect || !sameNetwork) {
                        Log.e("network1", "network change " + lastConnectedNetwork);
                        if (settings.getReconnect() || lastConnectedNetwork == null) {
                            mManagement.networkChange(sameNetwork);
                        } else {
                            network = connectState.PENDINGDISCONNECT;
                            boolean isSuccessfull = tryToStopVpn();
                            Log.e("network", "stop succeed " + isSuccessfull + " lsat network " + lastConnectedNetwork);
                        }
                    } else {
                        Log.e("network1", "network resume");
                        mManagement.resume();
                    }
                }
                lastConnectedNetwork = networkInfo;
            }
        } else if (networkInfo == null) {
            Log.e("network", "network null");
            // Not connected, stop openvpn, set last connected network to no network
            if (sendusr1) {
                network = connectState.PENDINGDISCONNECT;
                // Time to wait after network disconnect to pause the VPN
                int DISCONNECT_WAIT = 5;
                mDisconnectHandler.postDelayed(mDelayDisconnectRunnable, DISCONNECT_WAIT * 1000);
            }
        }
        if (!netstatestring.equals(lastStateMsg))
            VpnStatus.logInfo(R.string.netstatus, netstatestring);
        VpnStatus.logDebug(String.format("Debug state info: %s, pause: %s, shouldbeconnected: %s, network: %s ", netstatestring, getPauseReason(), shouldBeConnected(), network));
        lastStateMsg = netstatestring;
    }

    private boolean tryToStopVpn() {
        boolean isSuccessfull = mManagement.stopVPN(false);//
        if (isSuccessfull) {
            OpenVPNService.abortConnectionVPN = true;
            ProfileManager.setConntectedVpnProfileDisconnected(context);
        }
        return isSuccessfull;
    }

    private boolean shouldBeConnected() {
        return (userpause == connectState.SHOULDBECONNECTED && network == connectState.SHOULDBECONNECTED);
    }

    private pauseReason getPauseReason() {
        if (userpause == connectState.DISCONNECTED) return pauseReason.userPause;
        if (network == connectState.DISCONNECTED) return pauseReason.noNetwork;
        return pauseReason.userPause;
    }

    private NetworkInfo getCurrentNetworkInfo(Context context) {
        ConnectivityManager conn = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return conn.getActiveNetworkInfo();
    }

    private enum connectState {
        SHOULDBECONNECTED, PENDINGDISCONNECT, DISCONNECTED
    }
}
