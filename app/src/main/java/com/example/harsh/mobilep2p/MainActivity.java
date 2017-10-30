package com.example.harsh.mobilep2p;

import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.content.Context;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import org.w3c.dom.Text;

import java.io.IOException;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int PORT = 6578;
    private static final int BUFF_SIZE = 4096;
    private static final int TEXTVIEW_SIZE = 8;

    private String smartHead = "";
    private List<String> hostAddresses = new ArrayList<>();
    private Map<String, TableRow> tableRowMap = new HashMap<>();
    private Map<String, SystemResources> resourcesMap = new HashMap<>();

    private Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        acquireMulticastLock();
        startReceiveBroadcast();
        announcePresence();
        startElection();
    }

    private void receiveBroadcast() {
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket(PORT, InetAddress.getByName("0.0.0.0"));
            socket.setBroadcast(true);
            while (true) {
                Log.d(TAG, "Ready to receive packets");

                byte[] recvBuf = new byte[BUFF_SIZE];
                DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
                socket.receive(packet);
                processIncomingPacket(packet);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendBroadcast(final String message) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    DatagramSocket socket = new DatagramSocket();
                    socket.setBroadcast(true);
                    byte[] sendData = message.getBytes();
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, getBroadcastAddress(), PORT);
                    socket.send(sendPacket);
                    Log.d(TAG, "Broadcast packet sent to: " + getBroadcastAddress().getHostAddress());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private InetAddress getBroadcastAddress() throws IOException {
        WifiManager wifi = (WifiManager) MainActivity.this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcp = wifi.getDhcpInfo();
        // handle null somehow

        int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++)
            quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
        return InetAddress.getByAddress(quads);
    }

    private InetAddress getDeviceIPAddress() {
        WifiManager wifiManager = (WifiManager) MainActivity.this.getApplicationContext().getSystemService(WIFI_SERVICE);
        int ipAddressInt = wifiManager.getConnectionInfo().getIpAddress();

        // Convert little-endian to big-endianif needed
        if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
            ipAddressInt = Integer.reverseBytes(ipAddressInt);
        }

        byte[] ipByteArray = BigInteger.valueOf(ipAddressInt).toByteArray();

        InetAddress ipAddress;
        try {
            ipAddress = InetAddress.getByAddress(ipByteArray);
        } catch (UnknownHostException ex) {
            Log.e(TAG, "Unable to get host address.");
            ipAddress = null;
        }
        return ipAddress;
    }

    private void showMessageAsToast(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void announcePresence() {
        sendBroadcast(CommandTypes.NEW);
    }

    private void startReceiveBroadcast() {
        new Thread(new Runnable() {
            public void run() {
                receiveBroadcast();
            }
        }).start();
    }

    private void processIncomingPacket(DatagramPacket packet) {
        String data = new String(packet.getData()).trim();
        String hostAddress = packet.getAddress().getHostAddress();
        Log.i(TAG, String.format("Packet received from: %s Data: %s", hostAddress, data));

        if (data.equals(CommandTypes.NEW)) {
            addHostAddress(hostAddress);
            sendPresence();
        } else if (data.startsWith(CommandTypes.PRESENT)) {
            addHostAddress(hostAddress);
            addResources(hostAddress, data);
        } else if (data.startsWith(CommandTypes.NEW_SMART_HEAD)) {
            updateSmartHead(data.substring(CommandTypes.NEW_SMART_HEAD.length()));
        }
    }

    private void addHostAddress(final String hostAddress) {
        if (hostAddresses.contains(hostAddress)) {
            return;
        }
        hostAddresses.add(hostAddress);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TableLayout tableLayout = (TableLayout) findViewById(R.id.tableLayout);
                TableRow row = new TableRow(MainActivity.this);
                row.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));

                TextView ipAddrView = new TextView(MainActivity.this);
                ipAddrView.setText(hostAddress);
                ipAddrView.setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXTVIEW_SIZE);

                row.addView(ipAddrView);
                tableLayout.addView(row);

                tableRowMap.put(hostAddress, row);
            }
        });
    }

    private void addResources(String hostAddress, String data) {
        String json = data.substring(CommandTypes.PRESENT.length());
        Log.i(TAG, "JSON String: " + json);
        SystemResources resources = gson.fromJson(json, SystemResources.class);
        addResourcesToTable(hostAddress, resources);
        resourcesMap.put(hostAddress, resources);
    }

    private void addResourcesToTable(final String hostAddress, final SystemResources resources) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TableRow row = tableRowMap.get(hostAddress);

                row.removeAllViews();

                row.addView(createTextView(hostAddress));
                row.addView(createTextView(resources.getBatteryStatus()));
                row.addView(createTextView(resources.getBatteryLevel()));
                row.addView(createTextView(resources.getTotalMemory()));
            }
        });
    }

    private TextView createTextView(String text) {
        TextView textView = new TextView(MainActivity.this);
        textView.setText(text);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXTVIEW_SIZE);
        return textView;
    }

    private void sendPresence() {
        String message = CommandTypes.PRESENT;
        SystemResources resources = new SystemResources(MainActivity.this);
        String json = gson.toJson(resources);
        message += json;
        sendBroadcast(message);
    }

    private void acquireMulticastLock() {
        WifiManager wifiManager = (WifiManager) MainActivity.this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiManager.MulticastLock mcastLock = wifiManager.createMulticastLock(TAG);
        mcastLock.acquire();
    }

    private void startElection() {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                String newSmartHead = findSmartHead();
                if (newSmartHead != smartHead) {
                    sendBroadcast(CommandTypes.NEW_SMART_HEAD + newSmartHead);
                }
            }
        }, 5000);
    }

    private String findSmartHead() {
        String maxHostCharging = "";
        double maxWeightCharging = 0;
        String maxHostNotCharging = "";
        double maxWeightNotCharging = 0;

        for (String hostAddress : hostAddresses) {
            SystemResources resources = resourcesMap.get(hostAddress);
            int batteryLevel = Integer.parseInt(resources.getBatteryLevel());
            int ram = Integer.parseInt(resources.getAvailableMemory());
            if (resources.getBatteryStatus().equals("CHARGING") && batteryLevel > 30) {
                double currWeightCharging = 0.75 * batteryLevel + 0.25 * ram;
                if (currWeightCharging > maxWeightCharging) {
                    maxWeightCharging = currWeightCharging;
                    maxHostCharging = hostAddress;
                }
            } else {
                double currWeightNotCharging = 0.75 * batteryLevel + 0.25 * ram;
                if (currWeightNotCharging > maxWeightNotCharging) {
                    maxWeightNotCharging = currWeightNotCharging;
                    maxHostNotCharging = hostAddress;
                }
            }
        }
        if (maxWeightCharging > 0) {
            return maxHostCharging;
        } else {
            return maxHostNotCharging;
        }
    }

    private void updateSmartHead(final String newSmartHead) {
        smartHead = newSmartHead;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView smartHeadView = (TextView) findViewById(R.id.smartHead);
                smartHeadView.setText(newSmartHead);
            }
        });
    }

    public void sendFile(View view) {
        //TextView sendIPAddressView = (TextView) getViewById
        //String filename = "example";
        //String ipAddress =
    }

    public void receiveFile(View view) {

    }
}