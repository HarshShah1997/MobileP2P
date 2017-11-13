package com.example.harsh.mobilep2p.util;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;

import com.example.harsh.mobilep2p.types.FileMetadata;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Created by Harsh on 11/4/2017.
 */

public class DeviceUtils {

    private static final String UPLOAD_DIRECTORY = "/Upload/";
    private static final String TAG = "DeviceUtils";

    /*
    // TODO: Get broadcast address in different way
    public InetAddress getBroadcastAddress(Context context) throws IOException {
        WifiManager wifi = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcp = wifi.getDhcpInfo();
        // handle null somehow

        int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++)
            quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
        return InetAddress.getByAddress(quads);
    }*/

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

    /*public InetAddress getDeviceIPAddress(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
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
            ipAddress = null;
        }
        return ipAddress;
    }*/

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
