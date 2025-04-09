package com.example.sharefile;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

public class MyDialog {

    private AlertDialog alertDialog;
    private TextView progressMessage;
    private ProgressBar progressBar;

    public MyDialog(Context context) {
        // 在你的Activity或Fragment中

        // 加载自定义布局
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.progress_dialog_layout, null);

        // 创建AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(dialogView);

        // 显示对话框
        alertDialog = builder.create();
        alertDialog.setCancelable(false); // 禁止取消对话框
//        alertDialog.show();

        // 获取布局中的控件并操作（可选）
        progressMessage = dialogView.findViewById(R.id.progress_message);
        progressBar = dialogView.findViewById(R.id.progress_bar);

        // 在一些后台任务完成后关闭对话框
        // 例如，在异步任务或后台线程中
        // alertDialog.dismiss();
    }

    public void show() {
        alertDialog.show();

    }

    public void dismiss() {
        alertDialog.dismiss();
    }

    public void setProgress(int progress) {
        progressBar.setProgress(progress);
    }

    public void progressMessage(String message) {
        progressMessage.setText(message);
    }

}
