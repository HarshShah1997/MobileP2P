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
import java.net.UnknownHostException;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Harsh on 11/4/2017.
 */

public class DeviceUtils {

    private static final String UPLOAD_DIRECTORY = "/Upload";

    public InetAddress getBroadcastAddress(Context context) throws IOException {
        WifiManager wifi = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcp = wifi.getDhcpInfo();
        // handle null somehow

        int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++)
            quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
        return InetAddress.getByAddress(quads);
    }

    public InetAddress getDeviceIPAddress(Context context) {
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
    }

    public List<FileMetadata> getFilesFromDevice() {
        String path = Environment.getExternalStorageDirectory().toString() + UPLOAD_DIRECTORY;
        List<FileMetadata> filesList = new ArrayList<>();
        File directory = new File(path);
        File[] files = directory.listFiles();
        for (int i = 0; files != null && i < files.length; i++)
        {
            FileMetadata fileMetadata = new FileMetadata();
            fileMetadata.setFileName(files[i].getName());
            fileMetadata.setFileSize(files[i].length());
            filesList.add(fileMetadata);
        }
        return filesList;
    }
}
