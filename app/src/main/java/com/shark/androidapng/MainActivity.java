package com.shark.androidapng;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;

import com.shark.androidapng.entity.ApngParser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bindContentView();
    }

    private void bindContentView() {
        bindApngImageView();
    }

    private void bindApngImageView() {
        try {
            InputStream inputStream = getAssets().open("bell.png");
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            int length;
            byte[] buffer = new byte[4096];
            while ((length = inputStream.read(buffer, 0, buffer.length)) != -1) {
                byteArrayOutputStream.write(buffer, 0, length);
            }
            byteArrayOutputStream.flush();
            byte[] originalImageBytes = byteArrayOutputStream.toByteArray();
            Log.e("Mian", "originalImageBytes size: " + originalImageBytes.length);

            ApngParser apngEntity = new ApngParser(originalImageBytes);
            ImageView imageView = (ImageView) findViewById(R.id.activityMain_apngImageView);
            Log.e("Mian", "apng frame list size: " + apngEntity.getFrameList().size());
            imageView.setImageBitmap(apngEntity.generateImageDataBitmap(apngEntity.getFrameList().get(0).getFrameDataChunk()));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
