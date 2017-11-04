package com.example.harsh.mobilep2p.activity;

import android.content.Intent;
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

import com.example.harsh.mobilep2p.util.DeviceUtils;
import com.example.harsh.mobilep2p.FileListInfo;
import com.example.harsh.mobilep2p.types.FileMetadata;
import com.example.harsh.mobilep2p.R;
import com.example.harsh.mobilep2p.types.CommandTypes;
import com.example.harsh.mobilep2p.types.IntentConstants;
import com.example.harsh.mobilep2p.types.SystemResources;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int PORT = 6578;
    private static final int BUFF_SIZE = 4096;
    private static final int TEXTVIEW_SIZE = 10;

    private String smartHead = "";
    private List<String> hostAddresses = new ArrayList<>();
    private HashMap<String, SystemResources> resourcesMap = new HashMap<>();
    private List<FileMetadata> deviceFilesList = new ArrayList<>();

    private Gson gson = new Gson();
    private FileListInfo fileListInfo = new FileListInfo();
    private DeviceUtils deviceUtils = new DeviceUtils();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        acquireMulticastLock();
        startReceiveBroadcast();
        deviceFilesList = deviceUtils.getFilesFromDevice();
        announcePresence();
        startElection();
    }

    private void receiveBroadcast() {
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket(PORT, InetAddress.getByName("0.0.0.0"));
            socket.setBroadcast(true);
            while (true) {
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
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, deviceUtils.getBroadcastAddress(MainActivity.this), PORT);
                    socket.send(sendPacket);
                    Log.d(TAG, "Broadcast packet sent to: " + deviceUtils.getBroadcastAddress(MainActivity.this).getHostAddress());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
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
            if (deviceUtils.getDeviceIPAddress(MainActivity.this).getHostAddress().equals(smartHead)) {
                sendBroadcast(CommandTypes.NEW_SMART_HEAD + smartHead);
            }
            sendFilesList(deviceFilesList);
        } else if (data.startsWith(CommandTypes.PRESENT)) {
            addHostAddress(hostAddress);
            addResources(hostAddress, data);
        } else if (data.startsWith(CommandTypes.NEW_SMART_HEAD)) {
            updateSmartHead(data.substring(CommandTypes.NEW_SMART_HEAD.length()));
        } else if (data.startsWith(CommandTypes.FILES_LIST)) {
            updateFilesList(data.substring(CommandTypes.FILES_LIST.length()), hostAddress);
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

    // Broadcasts own files list
    private void sendFilesList(List<FileMetadata> filesList) {
        String json = gson.toJson(filesList);
        String message = CommandTypes.FILES_LIST + json;
        sendBroadcast(message);
    }

    private void updateFilesList(String json, String hostAddress) {
        Type typeListFileMetadata = new TypeToken<ArrayList<FileMetadata>>(){}.getType();
        List<FileMetadata> receivedFilesList = gson.fromJson(json, typeListFileMetadata);
        fileListInfo.addFilesList(receivedFilesList, hostAddress);
        Log.d(TAG, "Files in the network: " + fileListInfo.getFiles());
        refreshFilesListUI();
    }

    private void refreshFilesListUI() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (FileMetadata file : fileListInfo.getFiles()) {
                    addTableRow(file.getFileName(), file.getFileSize());
                }
            }
        });
    }

    private void addTableRow(String fileName, long fileSize) {
        String fileSizeString = getFileSizeString(fileSize);
        TableLayout tableLayout = (TableLayout) findViewById(R.id.filesListLayout);
        TableRow row = new TableRow(MainActivity.this);
        row.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));

        row.addView(createTextView(fileName));
        row.addView(createTextView(fileSizeString));

        tableLayout.addView(row);
    }

    private TextView createTextView(String text) {
        TextView textView = new TextView(MainActivity.this);
        textView.setText(text);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXTVIEW_SIZE);
        return textView;
    }

    private String getFileSizeString(long fileSize) {
        double size = fileSize;
        List<String> fileSizeSuffixes = new ArrayList<>(Arrays.asList("bytes", "KB", "MB", "GB"));
        int suffixPointer = 0;
        while (size > 1024) {
            suffixPointer++;
            size = size / 1024;
        }
        return String.format(Locale.ENGLISH, "%.2f %s", size, fileSizeSuffixes.get(suffixPointer));
    }

    public void sendFile(View view) {
        //TextView sendIPAddressView = (TextView) getViewById
        //String filename = "example";
        //String ipAddress =
    }

    public void receiveFile(View view) {

    }
}