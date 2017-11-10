package com.example.harsh.mobilep2p.info;

import com.example.harsh.mobilep2p.types.SystemResources;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Harsh on 11/4/2017.
 */

public class ResourcesInfo {

    private List<String> hostAddresses = new ArrayList<>();
    private HashMap<String, SystemResources> resourcesMap = new HashMap<>();

    public List<String> getHostAddresses() {
        return hostAddresses;
    }

    public HashMap<String, SystemResources> getResourcesMap() {
        return resourcesMap;
    }

    public void addHostAddress(final String hostAddress) {
        if (hostAddresses.contains(hostAddress)) {
            return;
        }
        hostAddresses.add(hostAddress);

    }

    public void addResources(String hostAddress, SystemResources resources) {
        resourcesMap.put(hostAddress, resources);
    }

    public String findSmartHead() {
        // TODO: Perform scaling with RAM
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
}
