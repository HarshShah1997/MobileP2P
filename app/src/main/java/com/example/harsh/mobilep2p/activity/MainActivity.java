package com.example.harsh.mobilep2p.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.example.harsh.mobilep2p.ViewPagerAdapter;
import com.example.harsh.mobilep2p.fragment.DevicesFragment;
import com.example.harsh.mobilep2p.fragment.FilesFragment;
import com.example.harsh.mobilep2p.info.FileStatusInfo;
import com.example.harsh.mobilep2p.info.ResourcesInfo;
import com.example.harsh.mobilep2p.types.FileDownloadStatus;
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
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements FilesFragment.OnFileDownloadListener {

    private static final String TAG = "MainActivity";
    private static final int BROADCAST_PORT = 6578;
    private static final int FILE_TRANSFER_PORT = 6579;
    private static final int BUFF_SIZE = 4096;

    private String smartHead = "";
    private List<FileMetadata> deviceFilesList = new ArrayList<>();
    private ServerSocket serverSocket;

    private Gson gson = new Gson();
    private FileListInfo fileListInfo = new FileListInfo();
    private ResourcesInfo resourcesInfo = new ResourcesInfo();
    private DeviceUtils deviceUtils = new DeviceUtils();
    private FileTransferUtils fileTransferUtils = new FileTransferUtils();

    private FilesFragment filesFragment;
    private DevicesFragment devicesFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Initializing viewPager
        final ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);

        //Initializing the tablayout
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tablayout);
        tabLayout.setupWithViewPager(viewPager);

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                viewPager.setCurrentItem(position, false);

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        setupViewPager(viewPager);
    }

    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        filesFragment = new FilesFragment();
        devicesFragment = new DevicesFragment();
        adapter.addFragment(filesFragment, "Files");
        adapter.addFragment(devicesFragment, "Devices");
        viewPager.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        init();
    }

    private void init() {
        deviceUtils.acquireMultiCastLock(MainActivity.this);
        startReceiveBroadcast();
        getFilesFromDevice();
        announcePresence();
        startElection();
        startServerSocket();
    }

    private void startReceiveBroadcast() {
        new Thread(new Runnable() {
            public void run() {
                receiveBroadcast();
            }
        }).start();
    }

    private void receiveBroadcast() {
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket(BROADCAST_PORT, InetAddress.getByName("0.0.0.0"));
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

    private void announcePresence() {
        sendBroadcast(CommandTypes.NEW);
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

    private void sendBroadcast(final String message) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    DatagramSocket socket = new DatagramSocket();
                    socket.setBroadcast(true);
                    byte[] sendData = message.getBytes();
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, deviceUtils.getBroadcastAddress(MainActivity.this), BROADCAST_PORT);
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
        } else if (data.startsWith(CommandTypes.QUIT)) {
            removeNode(hostAddress);
        }
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

    private void sendPresence() {
        String message = CommandTypes.PRESENT;
        SystemResources resources = new SystemResources(MainActivity.this);
        String json = gson.toJson(resources);
        message += json;
        sendBroadcast(message);
    }

    private void handlePresentCommand(String json, String hostAddress) {
        resourcesInfo.addHostAddress(hostAddress);
        SystemResources resources = gson.fromJson(json, SystemResources.class);
        resourcesInfo.addResources(hostAddress, resources);
        devicesFragment.addDevice(hostAddress, resources);
    }

    private void updateSmartHead(String newSmartHead) {
        smartHead = newSmartHead;
        devicesFragment.updateSmartHead(smartHead);
    }

    private void updateFilesList(String json, String hostAddress) {
        Type typeListFileMetadata = new TypeToken<ArrayList<FileMetadata>>() {
        }.getType();
        List<FileMetadata> receivedFilesList = gson.fromJson(json, typeListFileMetadata);
        fileListInfo.addFilesList(receivedFilesList, hostAddress);
        Log.d(TAG, "Files in the network: " + fileListInfo.getFiles());
        filesFragment.refreshFilesListUI(fileListInfo.getFiles());
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

    private void removeNode(String node) {
        resourcesInfo.removeHostAddress(node);
        fileListInfo.removeNode(node);
        filesFragment.refreshFilesListUI(fileListInfo.getFiles());
    }


    private boolean isSmartHead() {
        return deviceUtils.getDeviceIPAddress(MainActivity.this).getHostAddress().equals(smartHead);
    }

    private void getFilesFromDevice() {
        deviceFilesList = deviceUtils.getFilesFromDevice();
        Log.d(TAG, "Device files: " + deviceFilesList);
    }

    @Override
    public void downloadFile(final FileMetadata file) {
        new AlertDialog.Builder(this)
                .setMessage("Confirm download?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        startDownload(file);
                    }
                })
                .setNegativeButton(android.R.string.no, null).show();
    }

    private void startDownload(final FileMetadata file) {
        final String fileName = file.getFileName();
        final long fileSize = file.getFileSize();
        filesFragment.updateFileStatus(file, FileDownloadStatus.PROGRESS);

        new Thread(new Runnable() {
            @Override
            public void run() {
                List<String> nodes = fileListInfo.getNodesContainingFile(fileName, fileSize);
                Log.d(TAG, "Download requested: " + fileName + " " + fileSize + " Locations: " + nodes);
                List<TransferRequest> transferRequests = generateTransferRequests(fileName, fileSize, nodes);

                List<Thread> threads = new ArrayList<>();
                for (final TransferRequest transferRequest : transferRequests) {
                    Thread thread = new Thread(new Runnable() {
                        public void run() {
                            try {
                                fileTransferUtils.receiveFile(transferRequest, serverSocket);
                            } catch (IOException | ArrayIndexOutOfBoundsException e) {
                                filesFragment.updateFileStatus(file, FileDownloadStatus.FAILED);
                                Log.e(TAG, e.getMessage());
                            }
                        }
                    });
                    threads.add(thread);
                    thread.start();
                    sendDownloadRequest(transferRequest);
                }
                joinThreads(threads);
                if ((filesFragment.getFileStatus(file)).equals(FileDownloadStatus.PROGRESS)) {
                    filesFragment.updateFileStatus(file, FileDownloadStatus.SUCCESS);
                }

            }
        }).start();
    }

    private List<TransferRequest> generateTransferRequests(String fileName, long fileSize, List<String> nodes) {
        List<TransferRequest> transferRequests = new ArrayList<>();
        int noOfNodes = nodes.size();
        long startOffset = 0;

        List<Double> weights = resourcesInfo.findWeights(nodes);
        long chunkSize = 0;
        Log.d(TAG, "Weights:" + weights);
        for (int i = 0; i < noOfNodes; i++) {
            TransferRequest transferRequest = new TransferRequest();
            transferRequest.setFileName(fileName);
            transferRequest.setToIPAddress(deviceUtils.getDeviceIPAddress(MainActivity.this).getHostAddress());
            transferRequest.setStartOffset(startOffset);

            chunkSize = (long) (fileSize * weights.get(i));
            transferRequest.setSize(chunkSize);
            transferRequest.setFromIPAddress(nodes.get(i));
            transferRequests.add(transferRequest);
            startOffset += chunkSize;
        }
        transferRequests.get(noOfNodes - 1).setSize(chunkSize + fileSize - startOffset);
        return transferRequests;
    }

    private void sendDownloadRequest(TransferRequest transferRequest) {
        String json = gson.toJson(transferRequest);
        String message = CommandTypes.SEND_FILE + json;
        sendBroadcast(message);
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

    private void closeSocket(DatagramSocket socket) {
        if (socket != null) {
            socket.close();
        }
    }

    private void startServerSocket() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    serverSocket = new ServerSocket(FILE_TRANSFER_PORT);
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage());
                }
            }
        }).start();
    }

    @Override
    protected void onStop() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                sendBroadcast(CommandTypes.QUIT);
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage());
                }
            }
        }).start();
        super.onStop();
    }
}