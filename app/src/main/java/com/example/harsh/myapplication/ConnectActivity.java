package com.example.harsh.myapplication;

import android.app.ActivityManager;
import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.BatteryManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ConnectActivity extends AppCompatActivity {

    private String mServiceName = "TestNsdChat";
    private String SERVICE_TYPE = "_http._tcp.";

    private static final String TAG = "ConnectActivity";
    private static final int PORT = 4446;
    private static final String CHARSET = "UTF-8";

    private String systemInfo = getSystemInfo();

    private List<String> hostAddresses = new ArrayList<>();

    private NsdManager mNsdManager;
    private NsdManager.ResolveListener mResolveListener;
    private NsdManager.DiscoveryListener mDiscoveryListener;
    private NsdManager.RegistrationListener mRegistrationListener;
    private ServerSocket mServerSocket;

    private SocketUtils socketUtils = new SocketUtils();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);

        mNsdManager = (NsdManager) getSystemService(Context.NSD_SERVICE);

        try {
            mServerSocket = new ServerSocket(0);
            int portNo = mServerSocket.getLocalPort();
            registerService(portNo);
            discoverServices();
            waitForClients();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void discoverServices() {
        initializeDiscoveryListener();
        initializeResolveListener();
        mNsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD,
                mDiscoveryListener);
    }

    public void registerService(int port) {
        tearDown();
        initializeRegistrationListener();
        NsdServiceInfo serviceInfo = new NsdServiceInfo();

        serviceInfo.setServiceName(mServiceName);
        serviceInfo.setServiceType(SERVICE_TYPE);
        serviceInfo.setPort(port);

        mNsdManager.registerService(
                serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);
    }

    private void initializeRegistrationListener() {
        mRegistrationListener = new NsdManager.RegistrationListener() {

            @Override
            public void onServiceRegistered(NsdServiceInfo NsdServiceInfo) {
                // Save the service name.  Android may have changed it in order to
                // resolve a conflict, so update the name you initially requested
                // with the name Android actually used.
                mServiceName = NsdServiceInfo.getServiceName();
                Toast.makeText(ConnectActivity.this, "Successfully registered",
                        Toast.LENGTH_LONG).show();
                Log.d(TAG, "Registered name : " + mServiceName);
            }

            @Override
            public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Registration failed!  Put debugging code here to determine why.
            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo arg0) {
                // Service has been unregistered.  This only happens when you call
                // NsdManager.unregisterService() and pass in this listener.
            }

            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Unregistration failed.  Put debugging code here to determine why.
            }
        };
    }

    // Instantiate a new DiscoveryListener
    private void initializeDiscoveryListener() {
        mDiscoveryListener = new NsdManager.DiscoveryListener() {

            //  Called as soon as service discovery begins.
            @Override
            public void onDiscoveryStarted(String regType) {
                Log.d(TAG, "Service discovery started");
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                // A service was found!  Do something with it.
                Log.d(TAG, "Service discovery success" + service);
                if (!service.getServiceType().equals(SERVICE_TYPE)) {
                    // Service type is the string containing the protocol and
                    // transport layer for this service.
                    Log.d(TAG, "Unknown Service Type: " + service.getServiceType());
                } else if (service.getServiceName().equals(mServiceName)) {
                    // The name of the service tells the user what they'd be
                    // connecting to. It could be "Bob's Chat App".
                    Log.d(TAG, "Same machine: " + mServiceName);
                } else if (service.getServiceName().contains("NsdChat")) {
                    mNsdManager.resolveService(service, mResolveListener);
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                // When the network service is no longer available.
                // Internal bookkeeping code goes here.
                Log.e(TAG, "service lost" + service);
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i(TAG, "Discovery stopped: " + serviceType);
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
            }
        };
    }

     private void initializeResolveListener() {
         mResolveListener = new NsdManager.ResolveListener() {

             @Override
             public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                 // Called when the resolve fails.  Use the error code to debug.
                 Log.e(TAG, "Resolve failed" + errorCode);
             }

             @Override
             public void onServiceResolved(NsdServiceInfo serviceInfo) {
                 Log.e(TAG, "Resolve Succeeded. " + serviceInfo);

                 if (serviceInfo.getServiceName().equals(mServiceName)) {
                     Log.d(TAG, "Same IP.");
                     return;
                 }
                 NsdServiceInfo mService = serviceInfo;
                 int port = mService.getPort();
                 InetAddress host = mService.getHost();

                 final String hostName = host.getHostName();

                 handleNewHostAddress(host.getHostAddress());
             }
         };
     }

    @Override
    protected void onDestroy() {
        tearDown();
        super.onDestroy();
    }

    public void tearDown() {
        if (mRegistrationListener != null) {
            try {
                mNsdManager.unregisterService(mRegistrationListener);
            } finally {
            }
            mRegistrationListener = null;
        }
    }

    private void handleNewHostAddress(final String hostAddress) {
        if (hostAddresses.indexOf(hostAddress) == -1) {
            hostAddresses.add(hostAddress);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    TextView textView = (TextView) findViewById(R.id.services);
                    textView.setText(textView.getText() + "\n" + hostAddress);
                }
            });
            sendHello(hostAddress);
        }
    }

    private void sendHello(String hostAddress) {
        Socket socket = null;
        try {
            socket = new Socket(hostAddress, PORT);
            InputStream messageStream = new ByteArrayInputStream(systemInfo.getBytes(Charset.forName(CHARSET)));
            socketUtils.send(socket, messageStream);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeSocket(socket);
        }
    }

    private void waitForClients() {
        new Thread(new IncomingClientsThread()).start();
    }

    private class IncomingClientsThread implements Runnable {
        public void run() {
            ServerSocket serverSocket = null;
            Socket clientSocket = null;
            try {
                serverSocket = new ServerSocket(PORT);
                while (true) {
                    clientSocket = serverSocket.accept();
                    String receivedMessage = readStream(socketUtils.receive(clientSocket));
                    addReceivedMessage(clientSocket.getInetAddress().getHostAddress(), receivedMessage);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void addReceivedMessage(final String hostAddress, final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView messages = (TextView) findViewById(R.id.messages);
                String newMessage = String.format("%s: %s", hostAddress, message);
                messages.setText(messages.getText() + "\n" + newMessage);
            }
        });
    }

    private String readStream(InputStream inputStream) throws IOException {
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String receivedMessage = bufferedReader.readLine();
            return receivedMessage;
        } finally {
            inputStream.close();
        }
    }

    private void closeSocket(Socket socket) {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static long getTotalMemory(Context activity) {
        try {
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            ActivityManager activityManager = (ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE);
            activityManager.getMemoryInfo(mi);
            long availableMegs = mi.totalMem / 1048576L; // in megabyte (mb)

            return availableMegs;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private String getSystemInfo() {
        //String totalMemory = String.valueOf(getTotalMemory(this));
        //return totalMemory;
        //return BatteryManager.EXTRA_LEVEL;
        return "Hello";
    }

}