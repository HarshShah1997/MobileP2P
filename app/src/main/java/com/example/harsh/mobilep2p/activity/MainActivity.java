package com.example.harsh.mobilep2p.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.example.harsh.mobilep2p.info.ResourcesInfo;
import com.example.harsh.mobilep2p.types.TransferRequest;
import com.example.harsh.mobilep2p.util.DeviceUtils;
import com.example.harsh.mobilep2p.info.FileListInfo;
import com.example.harsh.mobilep2p.types.FileMetadata;
import com.example.harsh.mobilep2p.R;
import com.example.harsh.mobilep2p.types.CommandTypes;
import com.example.harsh.mobilep2p.types.IntentConstants;
import com.example.harsh.mobilep2p.types.SystemResources;
import com.example.harsh.mobilep2p.util.FileTransferUtils;
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
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int PORT = 6578;
    private static final int BUFF_SIZE = 4096;
    private static final int TEXT_VIEW_SIZE = 16;
    private static final int BORDER_HEIGHT = 1;
    private static final String STATUS_SUCCESS = "success";
    private static final String STATUS_FAILED = "failed";
    private static final String STATUS_PROGRESS = "progress";

    private String smartHead = "";
    private List<FileMetadata> deviceFilesList = new ArrayList<>();
    private Map<String, String> fileStatuses = new HashMap<>();
    private Map<String, TableRow> tableRowMap = new HashMap<>();

    private Gson gson = new Gson();
    private FileListInfo fileListInfo = new FileListInfo();
    private ResourcesInfo resourcesInfo = new ResourcesInfo();
    private DeviceUtils deviceUtils = new DeviceUtils();
    private FileTransferUtils fileTransferUtils = new FileTransferUtils();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        deviceUtils.acquireMultiCastLock(MainActivity.this);
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
                byte[] receiveBuffer = new byte[BUFF_SIZE];
                DatagramPacket packet = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                socket.receive(packet);
                processIncomingPacket(packet);
            }
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        } finally {
            closeSocket(socket);
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
            handlePresentCommand(data.substring(CommandTypes.PRESENT.length()), hostAddress);
        } else if (data.startsWith(CommandTypes.NEW_SMART_HEAD)) {
            updateSmartHead(data.substring(CommandTypes.NEW_SMART_HEAD.length()));
        } else if (data.startsWith(CommandTypes.FILES_LIST)) {
            updateFilesList(data.substring(CommandTypes.FILES_LIST.length()), hostAddress);
        } else if (data.startsWith(CommandTypes.SEND_FILE)) {
            sendFile(data.substring(CommandTypes.SEND_FILE.length()));
        }
    }

    private void sendPresence() {
        String message = CommandTypes.PRESENT;
        SystemResources resources = new SystemResources(MainActivity.this);
        String json = gson.toJson(resources);
        message += json;
        sendBroadcast(message);
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

    private void handlePresentCommand(String json, String hostAddress) {
        resourcesInfo.addHostAddress(hostAddress);
        SystemResources resources = gson.fromJson(json, SystemResources.class);
        resourcesInfo.addResources(hostAddress, resources);
    }

    // Broadcasts own files list
    private void sendFilesList(List<FileMetadata> filesList) {
        String json = gson.toJson(filesList);
        String message = CommandTypes.FILES_LIST + json;
        sendBroadcast(message);
    }

    private void updateFilesList(String json, String hostAddress) {
        Type typeListFileMetadata = new TypeToken<ArrayList<FileMetadata>>() {
        }.getType();
        List<FileMetadata> receivedFilesList = gson.fromJson(json, typeListFileMetadata);
        fileListInfo.addFilesList(receivedFilesList, hostAddress);
        Log.d(TAG, "Files in the network: " + fileListInfo.getFiles());
        refreshFilesListUI();
    }

    private void sendFile(String json) {
        final TransferRequest transferRequest = gson.fromJson(json, TransferRequest.class);
        if (transferRequest.getFromIPAddress().equals(deviceUtils.getDeviceIPAddress(MainActivity.this).getHostAddress())) {
            Log.d(TAG, "Sending file:" + transferRequest.getFileName() + " Size:" + transferRequest.getSize() + " to: " + transferRequest.getToIPAddress());
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        fileTransferUtils.sendFile(transferRequest);
                        showMessageAsToast("Transfer successful");
                    } catch (IOException e) {
                        showMessageAsToast("Transfer failed");
                        Log.e(TAG, e.getMessage());
                    }
                }
            }).start();
        }
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
        TableLayout tableLayout = (TableLayout) findViewById(R.id.filesListLayout);
        TableRow row = new TableRow(MainActivity.this);
        row.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));

        String text = generateText(fileName, fileSize);
        row.addView(createTextView(text));
        tableRowMap.put(text, row);

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
        return deviceUtils.getDeviceIPAddress(MainActivity.this).getHostAddress().equals(smartHead);
    }

    private void getFilesFromDevice() {
        deviceFilesList = deviceUtils.getFilesFromDevice();
        Log.d(TAG, "Device files: " + deviceFilesList);
    }

    private void downloadFile(final String fileName, final long fileSize) {
        new AlertDialog.Builder(this)
                .setMessage("Confirm download?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        startDownload(fileName, fileSize);
                    }
                })
                .setNegativeButton(android.R.string.no, null).show();
    }

    private void startDownload(final String fileName, final long fileSize) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                fileStatuses.put(generateText(fileName, fileSize), STATUS_PROGRESS);
                showStatus(fileName, fileSize);
                List<String> nodes = fileListInfo.getNodesContainingFile(fileName, fileSize);
                Log.d(TAG, "Download requested: " + fileName + " Locations: " + nodes);
                List<TransferRequest> transferRequests = generateTransferRequests(fileName, fileSize, nodes);

                List<Thread> threads = new ArrayList<>();
                //fileStatuses.put(generateText(fileName, fileSize), STATUS_SUCCESS);
                for (final TransferRequest transferRequest : transferRequests) {
                    Log.d(TAG, "FileName:" + transferRequest.getFileName() + " From:" + transferRequest.getFromIPAddress() +
                            " To:" + transferRequest.getToIPAddress() + " Offset:" + transferRequest.getStartOffset() + " Size:" + transferRequest.getSize());
                    Thread thread = new Thread(new Runnable() {
                        public void run() {
                            try {
                                fileTransferUtils.receiveFile(transferRequest);
                            } catch (IOException e) {
                                fileStatuses.put(generateText(fileName, fileSize), STATUS_FAILED);
                                Log.e(TAG, e.getMessage());
                            }
                        }
                    });
                    threads.add(thread);
                    thread.start();
                    sendDownloadRequest(transferRequest);
                }
                joinThreads(threads);
                if (fileStatuses.get(generateText(fileName, fileSize)).equals(STATUS_PROGRESS)) {
                    fileStatuses.put(generateText(fileName, fileSize), STATUS_SUCCESS);
                }
                showStatus(fileName, fileSize);
            }
        }).start();
    }

    private void showStatus(final String fileName, final long fileSize) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String text = generateText(fileName, fileSize);
                String fileStatus = fileStatuses.get(text);
                TableRow row = tableRowMap.get(text);
                row.removeAllViews();
                ImageView imageView = new ImageView(MainActivity.this);
                imageView.setLayoutParams(new TableRow.LayoutParams(60, 60));
                imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                row.addView(createTextView(text));

                if (fileStatus.equals(STATUS_SUCCESS)) {
                    Log.d(TAG, "Download succeeded");
                    imageView.setImageResource(R.mipmap.download_success);
                } else if (fileStatus.equals(STATUS_PROGRESS)) {
                    imageView.setImageResource(R.mipmap.download_progress);
                } else if (fileStatus.equals(STATUS_FAILED)) {
                    imageView.setImageResource(R.mipmap.download_failed);
                }
                row.addView(imageView);
            }
        });
    }

    private List<TransferRequest> generateTransferRequests(String fileName, long fileSize, List<String> nodes) {
        // TODO: Perform weighted division
        List<TransferRequest> transferRequests = new ArrayList<>();
        int noOfNodes = nodes.size();

        long chunkSize = fileSize / noOfNodes;
        long startOffset = 0;

        TransferRequest transferRequest = new TransferRequest();
        transferRequest.setFileName(fileName);
        transferRequest.setToIPAddress(deviceUtils.getDeviceIPAddress(MainActivity.this).getHostAddress());
        for (int i = 0; i < noOfNodes; i++) {
            transferRequest.setStartOffset(startOffset);
            transferRequest.setSize(chunkSize);
            transferRequest.setFromIPAddress(nodes.get(i));
            transferRequests.add(transferRequest);
            startOffset += chunkSize;
        }
        transferRequests.get(noOfNodes - 1).setSize(chunkSize + (fileSize % noOfNodes));
        return transferRequests;
    }

    private void joinThreads(List<Thread> threads) {
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }

    private void sendDownloadRequest(TransferRequest transferRequest) {
        String json = gson.toJson(transferRequest);
        String message = CommandTypes.SEND_FILE + json;
        sendBroadcast(message);
    }

    private String generateText(String fileName, long fileSize) {
        String fileSizeString = getFileSizeString(fileSize);
        return fileName + "\n" + fileSizeString;
    }

    private void closeSocket(DatagramSocket socket) {
        if (socket != null) {
            socket.close();
        }
    }
}