package com.example.harsh.mobilep2p;

import android.content.Intent;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int PORT = 6578;
    private static final int BUFF_SIZE = 4096;
    private static final String UPLOAD_DIRECTORY = "/Upload";

    private String smartHead = "";
    private List<String> hostAddresses = new ArrayList<>();
    private HashMap<String, SystemResources> resourcesMap = new HashMap<>();
    private List<FileMetadata> filesList = new ArrayList<>();

    private Gson gson = new Gson();
    private HeadInfoUtils headInfoUtils = new HeadInfoUtils();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        acquireMulticastLock();
        startReceiveBroadcast();
        getFilesFromDevice();
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
            if (getDeviceIPAddress().getHostAddress().equals(smartHead)) {
                sendBroadcast(CommandTypes.NEW_SMART_HEAD + smartHead);
            }
            sendFilesList(filesList);
        } else if (data.startsWith(CommandTypes.PRESENT)) {
            addHostAddress(hostAddress);
            addResources(hostAddress, data);
        } else if (data.startsWith(CommandTypes.NEW_SMART_HEAD)) {
            updateSmartHead(data.substring(CommandTypes.NEW_SMART_HEAD.length()));
        } else if (data.startsWith(CommandTypes.FILES_LIST)) {
            updateFilesList(data.substring(CommandTypes.FILES_LIST.length()));
            headInfoUtils.addFilesList(filesList, hostAddress);
        } else if (data.startsWith((CommandTypes.TRANSFER_FILES_LIST))) {
            String oldSmartHead = data.substring(CommandTypes.TRANSFER_FILES_LIST.length());
            transferFilesList(oldSmartHead);
        } else if (data.startsWith(CommandTypes.HEAD_INFO)) {
            if (getDeviceIPAddress().getHostAddress().equals(smartHead)) {
                String message = data.substring(CommandTypes.HEAD_INFO.length());
                updateHeadInfo(message);
            }
        }
    }

    private void addHostAddress(final String hostAddress) {
        if (hostAddresses.contains(hostAddress)) {
            return;
        }
        hostAddresses.add(hostAddress);

    }

    private void addResources(String hostAddress, String data) {
        String json = data.substring(CommandTypes.PRESENT.length());
        Log.i(TAG, "JSON String: " + json);
        SystemResources resources = gson.fromJson(json, SystemResources.class);
        resourcesMap.put(hostAddress, resources);
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
                String oldSmartHead = smartHead;
                sendBroadcast(CommandTypes.NEW_SMART_HEAD + newSmartHead);
                //if (!oldSmartHead.equals("") && !oldSmartHead.equals(smartHead)) {
                //    broadcastTransferFilesList(oldSmartHead, smartHead);
                //}
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
    }

    public void showDevices(View view) {
        Intent intent = new Intent(this, DevicesListActivity.class);
        intent.putExtra(IntentConstants.RESOURCES_MAP, resourcesMap);
        intent.putExtra(IntentConstants.SMART_HEAD, smartHead);
        startActivity(intent);
    }

    private void getFilesFromDevice() {
        String path = Environment.getExternalStorageDirectory().toString() + UPLOAD_DIRECTORY;
        File directory = new File(path);
        File[] files = directory.listFiles();
        for (int i = 0; files != null && i < files.length; i++)
        {
            FileMetadata fileMetadata = new FileMetadata();
            Log.d(TAG, "Filename:" + files[i].getName());
            fileMetadata.setFilename(files[i].getName());
            fileMetadata.setFilesize(files[i].length());
            filesList.add(fileMetadata);
        }
    }

    // Broadcasts own files list
    private void sendFilesList(List<FileMetadata> filesList) {
        String json = gson.toJson(filesList);
        String message = CommandTypes.FILES_LIST + json;
        sendBroadcast(message);
    }

    private void updateFilesList(String json) {
        Type typeListFileMetadata = new TypeToken<ArrayList<FileMetadata>>(){}.getType();
        List<FileMetadata> receivedFilesList = gson.fromJson(json, typeListFileMetadata);
        for (FileMetadata receivedFile : receivedFilesList) {
            if (!filesList.contains(receivedFile)) {
                filesList.add(receivedFile);
            }
        }
        for (FileMetadata fileMetadata : filesList) {
            Log.d(TAG, "Network Filename:" + fileMetadata.getFilename());
        }
    }

    private void broadcastTransferFilesList(String oldSmartHead, String newSmartHead) {
        sendBroadcast(CommandTypes.TRANSFER_FILES_LIST + oldSmartHead);
    }

    private void transferFilesList(String oldSmartHead) {
        if (getDeviceIPAddress().getHostAddress().equals(oldSmartHead)) {
            String json = gson.toJson(headInfoUtils);
            String message = CommandTypes.HEAD_INFO + json;
            sendBroadcast(message);
            headInfoUtils.clear();
        }
    }

    // If device is a smart head, it will update its headinfo object
    private void updateHeadInfo(String json) {
        headInfoUtils = gson.fromJson(json, HeadInfoUtils.class);
        showMessageAsToast("Transfer file list successful");
        Log.d(TAG, "Transfer file list successful");
        Log.d(TAG, "Files:" + headInfoUtils.getFiles());
        Log.d(TAG, "Locations:" + headInfoUtils.getFileLocations());
        Log.d(TAG, "Node Contains:" + headInfoUtils.getNodesContent());
        showMessageAsToast("Files:" + headInfoUtils.getFiles());
    }

    public void sendFile(View view) {
        //TextView sendIPAddressView = (TextView) getViewById
        //String filename = "example";
        //String ipAddress =
    }

    public void receiveFile(View view) {

    }
}