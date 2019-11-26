package uk.vpn.vpnuk;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import de.blinkt.openvpn.LaunchVPN;
import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.App;
import de.blinkt.openvpn.core.ConfigParser;
import de.blinkt.openvpn.core.ConnectionStatus;
import de.blinkt.openvpn.core.IOpenVPNServiceInternal;
import de.blinkt.openvpn.core.OpenVPNService;
import de.blinkt.openvpn.core.ProfileManager;
import de.blinkt.openvpn.core.VpnStatus;

public class VpnConnector implements VpnStatus.StateListener {
    private Activity activity;
    private ConnectionStateListener listener;
    IOpenVPNServiceInternal mService;


    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.e("vpn1uk", "connected");
            mService = IOpenVPNServiceInternal.Stub.asInterface(service);

        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.e("vpn1uk", "disconnected");
            mService = null;
        }
    };

    public VpnConnector(Activity activity) {
        this.activity = activity;
    }

    public void startVpn(
            String userName,
            String password,
            String ip,
            String socket,
            String port
    ) {
        try {
            ByteArrayInputStream inputStream;
            BufferedReader bufferedReader;
            inputStream =
                    new ByteArrayInputStream(prepareConfig(activity, ip, socket, port));
            bufferedReader =
                    new BufferedReader(new InputStreamReader(inputStream));

            ConfigParser cp = new ConfigParser();
            cp.parseConfig(bufferedReader);

            VpnProfile vp = cp.convertProfile();

            vp.mName = Build.MODEL;
            vp.mUsername = userName;
            vp.mPassword = password;

            ProfileManager pm = ProfileManager.getInstance(activity);
            pm.addProfile(vp);
            pm.saveProfileList(activity);
            pm.saveProfile(activity, vp);
            vp = pm.getProfileByName(Build.MODEL);
            Intent intent = new Intent(activity.getApplicationContext(), LaunchVPN.class);
            intent.putExtra(LaunchVPN.EXTRA_KEY, vp.getUUID().toString());
            intent.setAction(Intent.ACTION_MAIN);
            activity.startActivity(intent);
            App.isStart = false;
        } catch (Exception e) {

        }
    }


    private String getTextFromAsset(Activity context) throws IOException {
        BufferedReader reader = null;
        StringBuilder stringBuilder = new StringBuilder();
        try {
            reader = new BufferedReader(
                    new InputStreamReader(context.getAssets().open("openvpn.txt"), "UTF-8")
            );
            String mLine;
            while ((mLine = reader.readLine()) != null) {
                stringBuilder.append(mLine);
                stringBuilder.append('\n');
                //process line
            }
        } catch (IOException e) {
            //log the exception
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    //log the exception
                }
            }
        }
        return stringBuilder.toString();
    }

    private byte[] prepareConfig(
            Activity context,
            String ip,
            String socket,
            String port) throws IOException {
        return getTextFromAsset(context)
                .replace("<ip>", ip)
                .replace("<port>", port)
                .replace("<socket>", socket)
                .getBytes(Charset.forName("UTF-8"));
    }

    public void startListen(ConnectionStateListener listener) {
        Log.e("vpn1uk", "start");
        this.listener = listener;
        VpnStatus.addStateListener(this);
        Intent intent = new Intent(activity, OpenVPNService.class);
        intent.setAction(OpenVPNService.START_SERVICE);
        activity.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    public void removeListener() {
        Log.e("vpn1uk", "stop");
        disconnectService();
        VpnStatus.removeStateListener(this);
        listener = null;
    }

    private void disconnectService() {
//        if (mService != null) {
            activity.unbindService(mConnection);
//        }
    }

    @Override
    public void updateState(String state, String logmessage, int localizedResId, final ConnectionStatus level) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                listener.onStateChanged(ConnectionState.valueOf(level.name()));
            }
        });
//        Log.e("asdasd", "updateState " + state + " " + level);
    }

    @Override
    public void setConnectedVPN(String uuid) {

    }

    public void stopVpn() {
        OpenVPNService.abortConnectionVPN = true;
        ProfileManager.setConntectedVpnProfileDisconnected(activity);

        if (mService != null) {

            try {
                mService.stopVPN(false);
                ProfileManager pm = ProfileManager.getInstance(activity);
                pm.removeProfile(activity, pm.getProfileByName(Build.MODEL));
            } catch (Exception e) {

            }
        }
    }
}


