package com.example.harsh.mobilep2p.util;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Environment;

import com.example.harsh.mobilep2p.types.FileMetadata;

import java.io.File;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Created by Harsh on 11/4/2017.
 */

public class DeviceUtils {

    private static final String UPLOAD_DIRECTORY = "/Upload/";
    private static final String TAG = "DeviceUtils";

    public InetAddress getBroadcastAddress(Context context) {
        InetAddress found_bcast_address = null;
        System.setProperty("java.net.preferIPv4Stack", "true");
        try {
            Enumeration<NetworkInterface> niEnum = NetworkInterface.getNetworkInterfaces();
            while (niEnum.hasMoreElements()) {
                NetworkInterface ni = niEnum.nextElement();
                if (!ni.isLoopback()) {
                    for (InterfaceAddress interfaceAddress : ni.getInterfaceAddresses()) {
                        found_bcast_address = interfaceAddress.getBroadcast();
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return found_bcast_address;
    }

    public InetAddress getDeviceIPAddress(Context context) {
        InetAddress found_bcast_address = null;
        System.setProperty("java.net.preferIPv4Stack", "true");
        try {
            Enumeration<NetworkInterface> niEnum = NetworkInterface.getNetworkInterfaces();
            while (niEnum.hasMoreElements()) {
                NetworkInterface ni = niEnum.nextElement();
                if (!ni.isLoopback()) {
                    for (InterfaceAddress interfaceAddress : ni.getInterfaceAddresses()) {
                        found_bcast_address = interfaceAddress.getAddress();
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return found_bcast_address;
    }

    public List<FileMetadata> getFilesFromDevice() {
        List<FileMetadata> filesList = new ArrayList<>();
        File directory = new File(Environment.getExternalStorageDirectory(), UPLOAD_DIRECTORY);
        directory.setReadable(true);
        File[] files = directory.listFiles();
        for (int i = 0; files != null && i < files.length; i++) {
            FileMetadata fileMetadata = new FileMetadata();
            fileMetadata.setFileName(files[i].getName());
            fileMetadata.setFileSize(files[i].length());
            filesList.add(fileMetadata);
        }
        return filesList;
    }

    public void acquireMultiCastLock(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiManager.MulticastLock mcastLock = wifiManager.createMulticastLock(TAG);
        mcastLock.acquire();
    }
}
