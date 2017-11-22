package com.example.harsh.mobilep2p.info;

import android.os.BatteryManager;

import com.example.harsh.mobilep2p.types.SystemResources;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Harsh on 11/4/2017.
 */

public class ResourcesInfo {

    private static final int MAX_AVAILABLE_MEMORY = 4000;

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

    public void removeHostAddress(String hostAddress) {
        hostAddresses.remove(hostAddress);
        resourcesMap.remove(hostAddress);
    }

    public void addResources(String hostAddress, SystemResources resources) {
        resourcesMap.put(hostAddress, resources);
    }

    public String findSmartHead() {
        String maxHostCharging = "";
        double maxWeightCharging = 0;
        String maxHostNotCharging = "";
        double maxWeightNotCharging = 0;

        for (String hostAddress : hostAddresses) {
            SystemResources resources = resourcesMap.get(hostAddress);
            double batteryLevel = getBatteryLevel(resources.getBatteryLevel());
            double ram = getRamLevel(resources.getAvailableMemory());
            if (resources.getBatteryStatus().equals(BatteryManager.BATTERY_STATUS_CHARGING) && batteryLevel > 30) {
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

    public List<Double> findWeights(List<String> nodes) {
        List<Double> weights = new ArrayList<>();
        double sum = 0;
        for (String node : nodes) {
            SystemResources systemResources = resourcesMap.get(node);
            double weight = 0.75 * getBatteryLevel(systemResources.getBatteryLevel()) + 0.25 * getRamLevel(systemResources.getAvailableMemory());
            weights.add(weight);
            sum += weight;
        }
        for (int i = 0; i < weights.size(); i++) {
            weights.set(i, weights.get(i) / sum);
        }
        return weights;
    }

    private double getBatteryLevel(String batteryPercentage) {
        return Double.parseDouble("0." + batteryPercentage);
    }

    private double getRamLevel(String availableMemory) {
        return Double.parseDouble(availableMemory) / MAX_AVAILABLE_MEMORY;
    }
}
