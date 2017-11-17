package com.example.harsh.mobilep2p.fragment;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.example.harsh.mobilep2p.R;
import com.example.harsh.mobilep2p.info.FileStatusInfo;
import com.example.harsh.mobilep2p.types.FileDownloadStatus;
import com.example.harsh.mobilep2p.types.FileMetadata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Created by Harsh on 17-Nov-17.
 */

public class FilesFragment extends Fragment {

    private static final int TEXT_VIEW_SIZE = 16;
    private static final int BORDER_HEIGHT = 1;

    private FileStatusInfo fileStatusInfo = new FileStatusInfo();
    private OnFileDownloadListener mCallback;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_files, container, false);
    }

    public interface OnFileDownloadListener {
         void downloadFile(FileMetadata file);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallback = (OnFileDownloadListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnHeadlineSelectedListener");
        }
    }

    public void refreshFilesListUI(final List<FileMetadata> files) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                LinearLayout linearLayout = (LinearLayout) getView().findViewById(R.id.filesListLayout);
                linearLayout.removeAllViews();
                for (int i = 0; i < files.size(); i++) {
                    FileMetadata file = files.get(i);
                    addRow(file, linearLayout);
                }
            }
        });
    }

    public void updateFileStatus(final FileMetadata file, final String fileStatus) {
        fileStatusInfo.setFileStatus(file, fileStatus);

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final LinearLayout row = fileStatusInfo.getFileRow(file);
                if (row.getChildCount() == 2) {
                    row.removeViewAt(1);
                }

                ImageView imageView = new ImageView(getContext());
                imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);

                if (fileStatus.equals(FileDownloadStatus.SUCCESS)) {
                    imageView.setImageResource(R.mipmap.download_success);
                } else if (fileStatus.equals(FileDownloadStatus.PROGRESS)) {
                    imageView.setImageResource(R.mipmap.download_progress);
                } else if (fileStatus.equals(FileDownloadStatus.FAILED)) {
                    imageView.setImageResource(R.mipmap.download_failed);
                }
                row.addView(imageView);
            }
        });
    }

    public String getFileStatus(FileMetadata file) {
        return fileStatusInfo.getFileStatus(file);
    }

    private void addRow(final FileMetadata file, LinearLayout parent) {
        LinearLayout row = new LinearLayout(getContext());
        row.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        String text = generateText(file.getFileName(), file.getFileSize());
        fileStatusInfo.setFileRow(file, row);
        row.addView(createTextView(text));

        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCallback.downloadFile(file);
            }
        });

        parent.addView(row);
        parent.addView(createEmptyRow());
    }

    private TextView createTextView(String text) {
        TextView textView = new TextView(getContext());
        textView.setText(text);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_VIEW_SIZE);
        textView.setTextColor(Color.GRAY);
        textView.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.9f));
        return textView;
    }

    private LinearLayout createEmptyRow() {
        LinearLayout emptyRow = new LinearLayout(getContext());
        emptyRow.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, BORDER_HEIGHT));
        emptyRow.setBackgroundColor(Color.BLACK);
        emptyRow.addView(createEmptyLine());
        return emptyRow;
    }

    private TextView createEmptyLine() {
        TextView textView = new TextView(getContext());
        textView.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, BORDER_HEIGHT));
        return textView;
    }

    private String generateText(String fileName, long fileSize) {
        String fileSizeString = getFileSizeString(fileSize);
        return fileName + "\n" + fileSizeString;
    }

    private String getFileSizeString(long fileSize) {
        double size = fileSize;
        List<String> fileSizeSuffixes = new ArrayList<>(Arrays.asList("bytes", "KB", "MB", "GB", "TB"));
        int suffixPointer = 0;
        while (size > 1024) {
            suffixPointer++;
            size = size / 1024;
        }
        return String.format(Locale.ENGLISH, "%.2f %s", size, fileSizeSuffixes.get(suffixPointer));
    }
}
