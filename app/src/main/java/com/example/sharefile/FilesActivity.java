package com.example.sharefile;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FilesActivity extends AppCompatActivity {

    private File[] files;
    private File parentFile;
    private MyAdapter adapter;

    public static void open(Context context, String path) {
        Intent intent = new Intent(context, FilesActivity.class);
        intent.putExtra("path", path);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_files);

        TextView local = findViewById(R.id.local);
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        adapter = new MyAdapter();
        recyclerView.setAdapter(adapter);


        Intent intent = getIntent();
        String path = intent.getStringExtra("path");
        parentFile = new File(path);
        files = parentFile.listFiles();
        adapter.setData(files);
        local.setText(files[0].getParentFile().getAbsolutePath());
    }

    public void onClickClear(View view) {
        FileUtils.deleteFolder(parentFile.getAbsolutePath());
        files = parentFile.listFiles();
        adapter.setData(files);
    }

    private class MyAdapter extends RecyclerView.Adapter {

        List<File> files = new ArrayList();

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_files_item, parent, false);
            return new MyViewHolder2(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            MyViewHolder2 myViewHolder2 = (MyViewHolder2) holder;
            myViewHolder2.setPosition(position);
            File file = files.get(position);
            if (file.isFile()) {
                myViewHolder2.icon_tv.setImageResource(R.mipmap.file);
            } else {
                myViewHolder2.icon_tv.setImageResource(R.mipmap.dir);
            }
            myViewHolder2.filename_tv.setText(file.getName());
            long length = file.length();
            if (length < 1024) {
                myViewHolder2.size_tv.setText(length + "B");
            } else if (length < 1024 * 1024) {
                myViewHolder2.size_tv.setText(length / 1024 + "k");
            } else {
                myViewHolder2.size_tv.setText(length / 1024 / 1024 + "M");
            }

        }

        @Override
        public int getItemCount() {
            return files.size();
        }

        public void setData(File[] files) {
            this.files.clear();
            if (files == null || files.length == 0) {
                notifyDataSetChanged();
                return;
            }
            for (File file : files) {
                this.files.add(file);
            }
            notifyDataSetChanged();
        }

        private class MyViewHolder2 extends RecyclerView.ViewHolder {
            private final ImageView icon_tv;
            private final TextView filename_tv;
            private final TextView size_tv;
            private int position;

            public MyViewHolder2(View view) {
                super(view);
                icon_tv = view.findViewById(R.id.icon);
                filename_tv = view.findViewById(R.id.filename);
                size_tv = view.findViewById(R.id.size);
                view.setOnClickListener(v -> {
                    File file = files.get(position);
                    if (file.getName().endsWith(".apk")) {
                        InstallApkUtils.installApk(v.getContext(), file);
                    } else {
                        Toast.makeText(v.getContext(), "抱歉,不支持打开该格式!!" + file.getName(), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            public void setPosition(int position) {
                this.position = position;
            }
        }
    }
}
