package com.example.harsh.mobilep2p.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.example.harsh.mobilep2p.R;
import com.example.harsh.mobilep2p.types.SystemResources;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Harsh on 17-Nov-17.
 */

public class DevicesFragment extends Fragment {

    private static final int TEXTVIEW_SIZE = 12;

    HashMap<String, SystemResources> resourcesMap = new HashMap<>();
    private Map<String, TableRow> tableRowMap = new HashMap<>();
    private String smartHead = "";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        // Do nothing
        return inflater.inflate(R.layout.fragment_devices, container, false);
    }

    public void addDevice(String hostAddress, SystemResources systemResource) {
        resourcesMap.put(hostAddress, systemResource);
        refreshUI();
    }

    public void removeDevice(String hostAddress) {
        resourcesMap.remove(hostAddress);
        refreshUI();
    }

    public void updateSmartHead(String newSmartHead) {
        smartHead = newSmartHead;
        refreshUI();
    }

    private void refreshUI() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TableLayout tableLayout = (TableLayout) getView().findViewById(R.id.tableLayout);
                for (Map.Entry<String, SystemResources> entry : resourcesMap.entrySet()) {
                    addResourcesToTable(entry.getKey(), entry.getValue());
                }
                addSmartHead();
            }
        });
    }

    private void addResourcesToTable(final String hostAddress, final SystemResources resources) {
        if (tableRowMap.get(hostAddress) == null) {
            addTableRow(hostAddress);
        }
        TableRow row = tableRowMap.get(hostAddress);

        row.removeAllViews();

        row.addView(createTextView(hostAddress));
        row.addView(createTextView(resources.getBatteryStatus()));
        row.addView(createTextView(resources.getBatteryLevel()));
        row.addView(createTextView(resources.getTotalMemory()));
    }

    private TextView createTextView(String text) {
        TextView textView = new TextView(getContext());
        textView.setText(text);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXTVIEW_SIZE);
        return textView;
    }

    private void addTableRow(String hostAddress) {
        TableLayout tableLayout = (TableLayout) getView().findViewById(R.id.tableLayout);
        TableRow row = new TableRow(getContext());
        row.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
        tableLayout.addView(row);
        tableRowMap.put(hostAddress, row);
    }

    private void addSmartHead() {
        TextView smartHeadView = (TextView) getView().findViewById(R.id.smartHead);
        smartHeadView.setText(smartHead);
    }
}
