package com.shark.androidapng.util;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Shark0 on 2016/9/13.
 */
public class FileUtil {

    private void saveToFile(byte bytes[]) {
        Log.e("APNG", "saveToFile");
        File textFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        File textDir = new File(textFile, "apng");
        if (!textDir.exists()) {
            textDir.mkdirs();
        }
        Log.e("APNG", "saveToFile dir exist: " + textDir.exists());

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String currentTime = sdf.format(new Date());

        String byteFileName = currentTime + ".png";
        File byteFile = new File(textDir, byteFileName);
        if (byteFile.exists()) {
            byteFile.delete();
        }
        try {
            byteFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            FileOutputStream outputStream = new FileOutputStream(byteFile);
            outputStream.write(bytes);
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        String hexTextFileName = currentTime + ".txt";
        String hexText = ByteUtil.bytesToHex(bytes);
        File hexFile = new File(textDir, hexTextFileName);
        if (hexFile.exists()) {
            hexFile.delete();
        }
        try {
            hexFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            FileOutputStream outputStream = new FileOutputStream(hexFile);
            outputStream.write(hexText.getBytes("UTF-8"));
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
