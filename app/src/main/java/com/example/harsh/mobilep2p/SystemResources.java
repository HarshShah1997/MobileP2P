package com.example.harsh.mobilep2p;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.app.ActivityManager.MemoryInfo;

import java.io.Serializable;

/**
 * Created by Harsh on 9/7/2017.
 */

public class SystemResources implements Serializable {

    private String batteryStatus;

    private String batteryLevel;

    private String totalMemory;

    private String availableMemory;


    public SystemResources(Context context) {
        batteryStatus = determineBatteryStatus(context);
        batteryLevel = determineBatteryLevel(context);
        totalMemory = determineTotalMemory(context);
        availableMemory = determineAvailableMemory(context);
    }

    private String determineBatteryStatus(Context context) {
        IntentFilter iFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, iFilter);

        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        return convertStatusToString(status);
    }

    private String convertStatusToString(int status) {
        if (status == BatteryManager.BATTERY_STATUS_FULL) {
            return "FULL";
        } else if (status == BatteryManager.BATTERY_STATUS_CHARGING) {
            return "CHARGING";
        } else {
            return "NOT CHARGING";
        }
    }

    private String determineBatteryLevel(Context context) {
        IntentFilter iFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, iFilter);

        int level = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) : -1;
        int scale = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1) : -1;

        float batteryPct = level / (float) scale;

        return String.format("%s", (int)(batteryPct * 100));
    }

    private String determineAvailableMemory(Context context) {
        MemoryInfo memoryInfo = getMemoryInfo(context);
        String availableMemory = String.format("%s", memoryInfo.availMem / 0x100000L);
        return availableMemory;
    }

    private String determineTotalMemory(Context context) {
        MemoryInfo memoryInfo = getMemoryInfo(context);
        String totalMemory = String.format("%s", memoryInfo.totalMem / 0x100000L);
        return totalMemory;
    }

    private MemoryInfo getMemoryInfo(Context context) {
        MemoryInfo memoryInfo = new MemoryInfo();
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(memoryInfo);
        return memoryInfo;
    }

    public String getBatteryStatus() {
        return batteryStatus;
    }

    public String getBatteryLevel() {
        return batteryLevel;
    }

    public String getTotalMemory() {
        return totalMemory;
    }

    public String getAvailableMemory() {
        return availableMemory;
    }
}
