package com.example.harsh.mobilep2p.activity;

import android.content.Intent;
import android.graphics.Color;
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

import com.example.harsh.mobilep2p.info.ResourcesInfo;
import com.example.harsh.mobilep2p.util.DeviceUtils;
import com.example.harsh.mobilep2p.info.FileListInfo;
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
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int PORT = 6578;
    private static final int BUFF_SIZE = 4096;
    private static final int TEXT_VIEW_SIZE = 16;
    private static final int BORDER_HEIGHT = 1;

    private String smartHead = "";
    private List<FileMetadata> deviceFilesList = new ArrayList<>();

    private Gson gson = new Gson();
    private FileListInfo fileListInfo = new FileListInfo();
    private ResourcesInfo resourcesInfo = new ResourcesInfo();
    private DeviceUtils deviceUtils = new DeviceUtils();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        acquireMultiCastLock();
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
            handleNewDevice(hostAddress);
        } else if (data.startsWith(CommandTypes.PRESENT)) {
            resourcesInfo.addHostAddress(hostAddress);
            String json = data.substring(CommandTypes.PRESENT.length());
            SystemResources resources = gson.fromJson(json, SystemResources.class);
            resourcesInfo.addResources(hostAddress, resources);
        } else if (data.startsWith(CommandTypes.NEW_SMART_HEAD)) {
            updateSmartHead(data.substring(CommandTypes.NEW_SMART_HEAD.length()));
        } else if (data.startsWith(CommandTypes.FILES_LIST)) {
            updateFilesList(data.substring(CommandTypes.FILES_LIST.length()), hostAddress);
        }
    }

    private void sendPresence() {
        String message = CommandTypes.PRESENT;
        SystemResources resources = new SystemResources(MainActivity.this);
        String json = gson.toJson(resources);
        message += json;
        sendBroadcast(message);
    }

    private void acquireMultiCastLock() {
        WifiManager wifiManager = (WifiManager) MainActivity.this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiManager.MulticastLock mcastLock = wifiManager.createMulticastLock(TAG);
        mcastLock.acquire();
    }

    private void startElection() {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                String newSmartHead = resourcesInfo.findSmartHead();
                sendBroadcast(CommandTypes.NEW_SMART_HEAD + newSmartHead);
            }
        }, 5000);
    }

    private void updateSmartHead(String newSmartHead) {
        smartHead = newSmartHead;
    }

    public void showDevices(View view) {
        Intent intent = new Intent(this, DevicesListActivity.class);
        intent.putExtra(IntentConstants.RESOURCES_MAP, resourcesInfo.getResourcesMap());
        intent.putExtra(IntentConstants.SMART_HEAD, smartHead);
        startActivity(intent);
    }

    private void handleNewDevice(String hostAddress) {
        resourcesInfo.addHostAddress(hostAddress);
        sendPresence();
        if (isSmartHead()) {
            sendBroadcast(CommandTypes.NEW_SMART_HEAD + smartHead);
        }
        sendFilesList(deviceFilesList);
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
                TableLayout tableLayout = (TableLayout) findViewById(R.id.filesListLayout);
                tableLayout.removeAllViews();
                for (FileMetadata file : fileListInfo.getFiles()) {
                    addTableRow(file.getFileName(), file.getFileSize());
                }
            }
        });
    }

    private void addTableRow(final String fileName, final long fileSize) {
        String fileSizeString = getFileSizeString(fileSize);
        TableLayout tableLayout = (TableLayout) findViewById(R.id.filesListLayout);
        TableRow row = new TableRow(MainActivity.this);
        row.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));

        row.addView(createTextView(fileName + "\n" + fileSizeString));

        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                downloadFile(fileName, fileSize);
            }
        });

        TableRow emptyRow = createEmptyRow();

        tableLayout.addView(row);
        tableLayout.addView(emptyRow);
    }

    private TextView createTextView(String text) {
        TextView textView = new TextView(MainActivity.this);
        textView.setText(text);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_VIEW_SIZE);
        textView.setTextColor(Color.GRAY);
        return textView;
    }

    private TableRow createEmptyRow() {
        TableRow emptyRow = new TableRow(MainActivity.this);
        emptyRow.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, BORDER_HEIGHT));
        emptyRow.setBackgroundColor(Color.BLACK);
        emptyRow.addView(createEmptyLine());
        return emptyRow;
    }

    private TextView createEmptyLine() {
        TextView textView = new TextView(MainActivity.this);
        textView.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, BORDER_HEIGHT));
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

    private boolean isSmartHead() {
        if (deviceUtils.getDeviceIPAddress(MainActivity.this).getHostAddress().equals(smartHead)) {
            return true;
        } else {
            return false;
        }
    }

    private void getFilesFromDevice() {
        deviceFilesList = deviceUtils.getFilesFromDevice();
        Log.d(TAG, "Device files: " + deviceFilesList);
    }

    private void downloadFile(String fileName, long fileSize) {
        List<String> nodes = fileListInfo.getNodesContainingFile(fileName, fileSize);
        Log.d(TAG, "Download requested: " + fileName + " Locations: " + nodes);
        List<Long> fileOffsets = new ArrayList<>();
        List<Long> fileSizes = new ArrayList<>();
        int noOfNodes = nodes.size();

        long chunkSize = fileSize / noOfNodes;
        long startOffset = 0;

        for (int i = 0; i < noOfNodes - 1; i++) {
            fileOffsets.add(startOffset);
            fileSizes.add(chunkSize);
            startOffset += chunkSize;
        }
        fileOffsets.add(startOffset);
        fileSizes.add(chunkSize + (fileSize % noOfNodes));
        Log.d(TAG, "Offsets:" + fileOffsets + " Sizes:" + fileSizes);
    }

    public void sendFile(View view) {
        //TextView sendIPAddressView = (TextView) getViewById
        //String filename = "example";
        //String ipAddress =
    }

    public void receiveFile(View view) {

    }
}