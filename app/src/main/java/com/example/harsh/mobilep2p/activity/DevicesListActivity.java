package com.example.harsh.mobilep2p.activity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.TypedValue;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.example.harsh.mobilep2p.R;
import com.example.harsh.mobilep2p.types.IntentConstants;
import com.example.harsh.mobilep2p.types.SystemResources;

import java.util.HashMap;
import java.util.Map;

public class DevicesListActivity extends AppCompatActivity {

    private static final int TEXTVIEW_SIZE = 8;

    private Map<String, TableRow> tableRowMap = new HashMap<>();
    private String smartHead = "";
    private HashMap<String, SystemResources> resourcesMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_devices_list);

        Intent intent = getIntent();
        smartHead = intent.getStringExtra(IntentConstants.SMART_HEAD);
        resourcesMap = (HashMap<String, SystemResources>) intent.getSerializableExtra(IntentConstants.RESOURCES_MAP);

        for (Map.Entry<String, SystemResources> entry : resourcesMap.entrySet()) {
            addResourcesToTable(entry.getKey(), entry.getValue());
        }
        addSmartHead();
    }

    private void addResourcesToTable(final String hostAddress, final SystemResources resources) {
        addTableRow(hostAddress);
        TableRow row = tableRowMap.get(hostAddress);

        row.removeAllViews();

        row.addView(createTextView(hostAddress));
        row.addView(createTextView(resources.getBatteryStatus()));
        row.addView(createTextView(resources.getBatteryLevel()));
        row.addView(createTextView(resources.getTotalMemory()));
    }

    private TextView createTextView(String text) {
        TextView textView = new TextView(DevicesListActivity.this);
        textView.setText(text);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXTVIEW_SIZE);
        return textView;
    }

    private void addTableRow(String hostAddress) {
        TableLayout tableLayout = (TableLayout) findViewById(R.id.tableLayout);
        TableRow row = new TableRow(DevicesListActivity.this);
        row.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
        tableLayout.addView(row);
        tableRowMap.put(hostAddress, row);
    }

    private void addSmartHead() {
        TextView smartHeadView = (TextView) findViewById(R.id.smartHead);
        smartHeadView.setText(smartHead);
    }
}
