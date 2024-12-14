package com.example.sharefile;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

public class MainActivity extends AppCompatActivity {

    private File externalCacheDir;

    private void openSubFile(final SmbFile smbFile) {
        runOnUiThread(() -> {
            URL url = smbFile.getURL();
            String subFile = url.getProtocol() + "://" + url.getAuthority() + url.getPath();
            Intent intent = new Intent(context, MainActivity.class);
            intent.putExtra("fileUrl", subFile);
            intent.putExtra("username", username);
            intent.putExtra("password", password);
            context.startActivity(intent);
        });
    }

    private EditText et_fileUrl;
    private EditText et_username;
    private EditText et_password;
    private RecyclerView recyclerView;
    private MyAdapter adapter;
    private Context context;
    private SharedPreferences sp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;
        sp = getSharedPreferences("sp_filename", Context.MODE_PRIVATE);
        externalCacheDir = getExternalCacheDir();
        TextView files = findViewById(R.id.download_files);
        String[] list = externalCacheDir.list();
        files.setText("已下载文件: " + list.length);

        et_fileUrl = findViewById(R.id.url);
        et_username = findViewById(R.id.username);
        et_password = findViewById(R.id.password);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));

        adapter = new MyAdapter();
        recyclerView.setAdapter(adapter);

        Intent intent = getIntent();
        if (intent != null) {
            String url = intent.getStringExtra("fileUrl");
            String user = intent.getStringExtra("username");
            String pwd = intent.getStringExtra("password");
            if (!TextUtils.isEmpty(url)) {
                fileUrl = url;
                et_fileUrl.setText(fileUrl);
            }
            if (!TextUtils.isEmpty(url)) {
                username = user;
                et_username.setText(username);
            }
            if (!TextUtils.isEmpty(url)) {
                password = pwd;
                et_password.setText(password);
                onClick(null);
            } else {
                fileUrl = sp.getString("fileUrl", "");
                username = sp.getString("username", "");
                password = sp.getString("password", "");
                if (!TextUtils.isEmpty(fileUrl)) et_fileUrl.setText(fileUrl);
                if (!TextUtils.isEmpty(username)) et_username.setText(username);
                if (!TextUtils.isEmpty(password)) et_password.setText(password);
            }
        }


    }

    String fileUrl;
    String username;
    String password;

    public void onClick(View view) {
        fileUrl = et_fileUrl.getText().toString();
        username = et_username.getText().toString();
        password = et_password.getText().toString();
        if (!fileUrl.endsWith("/")) {
            Toast.makeText(context, "文件夹路径要以 '/' 结尾", Toast.LENGTH_SHORT).show();
            return;
        }

        sp.edit().putString("fileUrl", fileUrl).apply();
        sp.edit().putString("username", username).apply();
        sp.edit().putString("password", password).apply();

        new Thread(() -> {
            List<SmbFile> list = new ArrayList<>();
            try {
                list = SmbAccess.accessSharedFolder(fileUrl, username, password);
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show());
            }
            List<SmbFile> finalList = list;
            runOnUiThread(() -> adapter.setData(finalList));
        }).start();

    }

    public void openFiles(View view) {
        FilesActivity.open(context, externalCacheDir.getAbsolutePath());
    }

    private class MyAdapter extends RecyclerView.Adapter {
        List<SmbFile> list;

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_main_item, parent, false);
            return new MyViewHolder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            MyViewHolder myViewHolder = (MyViewHolder) holder;
            SmbFile smbFile = list.get(position);
            myViewHolder.setPosition(position);
            myViewHolder.filename.setText(smbFile.getName());

            new Thread(() -> {
                try {
                    boolean isFile = smbFile.isFile();
                    long length = smbFile.length();

                    runOnUiThread(() -> {
                        float number = length * 1.0f / 1024 / 1024;
                        if (length < 1024) {
                            myViewHolder.size.setText(length + "B");
                        } else if (length < (1024 * 1024)) {
                            myViewHolder.size.setText(length / 1024 + "k");
                        } else {
                            DecimalFormat df = new DecimalFormat("#0.00");
                            String length_formattedNumber = df.format(number);
                            myViewHolder.size.setText(length_formattedNumber + "M");
                        }
                        if (isFile) {
                            myViewHolder.type.setImageResource(R.mipmap.file);
                        } else {
                            myViewHolder.type.setImageResource(R.mipmap.dir);
                            myViewHolder.size.setText(">>>");
                        }
                    });
                } catch (SmbException e) {
                    e.printStackTrace();
                }
            }).start();

        }

        @Override
        public int getItemCount() {
            return list == null ? 0 : list.size();
        }

        public void setData(List<SmbFile> list) {
            this.list = list;
            notifyDataSetChanged();
        }

        private class MyViewHolder extends RecyclerView.ViewHolder {
            private final TextView filename;
            private final TextView size;
            private final ImageView type;
            private int position;

            public MyViewHolder(View view) {
                super(view);
                filename = view.findViewById(R.id.filename);
                type = view.findViewById(R.id.type);
                size = view.findViewById(R.id.size);
                view.setOnClickListener(view1 -> {
                    final SmbFile smbFile = list.get(position);
                    new Thread(() -> {
                        try {
                            if (smbFile.isDirectory()) {
                                openSubFile(smbFile);
                                return;
                            }
                            downloadFile2(smbFile); //多线程下载
                        } catch (SmbException e) {
                            e.printStackTrace();
                        }
                    }).start();
                });
            }

            public void setPosition(int position) {
                this.position = position;
            }
        }
    }

    private void downloadFile2(SmbFile smbFile) {
        runOnUiThread(() -> {
            progressDialog = new MyDialog(context);
            progressDialog.show();
        });


        String temp = externalCacheDir + "/" + "temp/";
        File file = new File(temp);
        if (!file.exists()) file.mkdirs();
        String FINAL_FILE_NAME = externalCacheDir + "/" + smbFile.getName();
        URL url = smbFile.getURL();
        String smbUrl = url.getProtocol() + "://" + url.getAuthority() + url.getPath();
        System.out.println("downloadFile2 " + smbUrl);
        SmbFileMultiThreadedDownload.main(username, password, smbUrl, temp, FINAL_FILE_NAME, new SmbFileMultiThreadedDownload.Callback() {
            @Override
            public void success(String msg) {
                runOnUiThread(() -> {
                    FilesActivity.open(context, externalCacheDir.getAbsolutePath());
                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
                    });
                });
            }

            @Override
            public void fail(String msg) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(context, "下载失败!!! " + msg, Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void progress(float progress) {
                progressDialog.setProgress((int) (progress * 100));
            }
        });
    }

    private MyDialog progressDialog;

    private void downloadFile(final SmbFile smbFile) {
        runOnUiThread(() -> {
            progressDialog = new MyDialog(context);
            progressDialog.show();
        });

        new Thread(() -> {
            System.out.println("File download start....!");
            String localFilePath = externalCacheDir + "/" + smbFile.getName();
            SmbAccess.downloadFile(smbFile, localFilePath, new SmbAccess.Callback() {
                @Override
                public void success() {
                    runOnUiThread(() -> progressDialog.dismiss());
                }

                @Override
                public void fail() {
                    runOnUiThread(() -> progressDialog.dismiss());
                }

                @Override
                public void progress(final float length, final float progess) {
                    runOnUiThread(() -> {
                        int progress = (int) (progess / length * 100);
                        System.out.println("progress=" + progress);
                        progressDialog.setProgress(progress);
                        DecimalFormat df = new DecimalFormat("#0.00");
                        String progess_formattedNumber = df.format(progess / 1024 / 1024);
                        String length_formattedNumber = df.format(length / 1024 / 1024);
                        progressDialog.progressMessage(progess_formattedNumber + "M / " + length_formattedNumber + "M");
                    });
                }
            });
            System.out.println("File downloaded successfully! " + localFilePath);

            System.out.println("File download end....!");
        }).start();

    }


}