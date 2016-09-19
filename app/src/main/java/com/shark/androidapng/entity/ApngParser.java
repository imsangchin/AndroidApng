package com.shark.androidapng.entity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.shark.androidapng.util.ByteUtil;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by Shark0 on 2016/9/13.
 */
public class ApngParser {

    public static final byte[] PNG_TAG_BYTES = new byte[]{(byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47, (byte) 0x0D, (byte) 0x0A, (byte) 0x1A, (byte) 0x0A};

    public static final byte[] IHDR_TAG_BYTES = new byte[]{(byte) 0x49, (byte) 0x48, (byte) 0x44, (byte) 0x52};

    public static final byte[] ACTL_TAG_BYTES = new byte[]{(byte) 0x61, (byte) 0x63, (byte) 0x54, (byte) 0x4c};

    public static final byte[] FCTL_TAG_BYTES = new byte[]{(byte) 0x66, (byte) 0x63, (byte) 0x54, (byte) 0x4c};

    public static final byte[] IDAT_TAG_BYTES = new byte[]{(byte) 0x49, (byte) 0x44, (byte) 0x41, (byte) 0x54};

    public static final byte[] FDAT_TAG_BYTES = new byte[]{(byte) 0x66, (byte) 0x64, (byte) 0x41, (byte) 0x54};

    public static final byte[] IEND_TAG_BYTES = new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x49,
            (byte) 0x45, (byte) 0x4E, (byte) 0x44, ((byte) 0xae), (byte) 0x42, (byte) 0x60, ((byte) 0x82)};

    private boolean isApng;

    private byte[] pngAndIhdrBytes;

    private byte[] actlBytes;
    private int frameCount;
    private int repeatCount;

    private List<FrameEntity> frameList = new LinkedList<>();

    public ApngParser(byte[] imageBytes) {
        int actlIndex = ByteUtil.indexOf(imageBytes, ACTL_TAG_BYTES);
        isApng = actlIndex != -1;
        Log.e("Apng", "actlIndex: " + actlIndex);
        if(!isApng) {
            return;
        }

        pngAndIhdrBytes = ByteUtil.subBytes(imageBytes, 0, actlIndex - 4);
        Log.e("Apng", "pngAndIhdrBytes: " + ByteUtil.bytesToHex(pngAndIhdrBytes));

        int fctlIndex = ByteUtil.indexOf(imageBytes, FCTL_TAG_BYTES);
        Log.e("Apng", "fctlIndex: " + fctlIndex);

        actlBytes = ByteUtil.subBytes(imageBytes, actlIndex, fctlIndex - 4);
        Log.e("Apng", "actlBytes: " + ByteUtil.bytesToHex(actlBytes));

        byte[] frameCountBytes = ByteUtil.subBytes(actlBytes, ACTL_TAG_BYTES.length, ACTL_TAG_BYTES.length + 4);
        Log.e("Apng", "frameCountBytes: " + ByteUtil.bytesToHex(frameCountBytes));
        frameCount = ByteUtil.convertFourBytesToInt(frameCountBytes);
        Log.e("Apng", "frameCount: " + frameCount);

        byte[] repeatCountBytes = ByteUtil.subBytes(actlBytes, ACTL_TAG_BYTES.length + 4, ACTL_TAG_BYTES.length + 8);
        Log.e("Apng", "repeatCountBytes: " + ByteUtil.bytesToHex(repeatCountBytes));
        repeatCount = ByteUtil.convertFourBytesToInt(repeatCountBytes);
        Log.e("Apng", "repeatCount: " + repeatCount);

        byte[] framesBytes = ByteUtil.subBytes(imageBytes, ByteUtil.indexOf(imageBytes, FCTL_TAG_BYTES) - 4, ByteUtil.indexOf(imageBytes, IEND_TAG_BYTES));
        //應該用一個For迴圈就解決
        for(int i = 0; i < frameCount; i ++) {
            FrameEntity frameEntity = new FrameEntity();
            if(i == 0) {
                byte[] fctlByte = ByteUtil.subBytes(framesBytes, ByteUtil.indexOf(framesBytes, FCTL_TAG_BYTES), ByteUtil.indexOf(framesBytes, IDAT_TAG_BYTES) - 4);
                Log.e("Apng", "i: " + i + ", frame0ControlInfo: "+ ByteUtil.bytesToHex(fctlByte));

                byte[] nextFctlTagBytes = ByteUtil.combineBytes(FCTL_TAG_BYTES, ByteUtil.convertIntToFourBytes(i + 1));
                Log.e("Apng", "i: " + i + ", frame1ControlTagBytes: "+ ByteUtil.bytesToHex(nextFctlTagBytes));

                byte[] idatBytes =  ByteUtil.subBytes(framesBytes, ByteUtil.indexOf(framesBytes, IDAT_TAG_BYTES) - 4, ByteUtil.indexOf(framesBytes, nextFctlTagBytes) - 4);
                Log.e("Apng", "i: " + i + ", fctl: "+ ByteUtil.bytesToHex(fctlByte));
                Log.e("Apng", "i: " + i + ", idat: "+ ByteUtil.bytesToHex(idatBytes));

                frameEntity.setFctlBytes(fctlByte);
                frameEntity.setIdatBytes(idatBytes);
            } else {
                byte[] fctlTagBytes = ByteUtil.combineBytes(FCTL_TAG_BYTES, ByteUtil.convertIntToFourBytes(((i - 1) * 2 + 1)));
                byte[] fctlBytes;
                byte[] fdatBytes;
//                Log.e("Apng", "i: " + i + ", fctlTagBytes: "+ ByteUtil.bytesToHex(fctlTagBytes));
                if(i == frameCount - 1) {
                    int fctlTagIndex = ByteUtil.indexOf(framesBytes, fctlTagBytes);
                    byte[] frameBytes = ByteUtil.subBytes(framesBytes, fctlTagIndex, (framesBytes.length - 1));
                    fctlBytes = ByteUtil.subBytes(frameBytes, 0, ByteUtil.indexOf(frameBytes, FDAT_TAG_BYTES));
                    fdatBytes = ByteUtil.subBytes(frameBytes, ByteUtil.indexOf(frameBytes, FDAT_TAG_BYTES) - 4, (frameBytes.length - 1));
                } else {
                    byte[] nextFctlTagBytes = ByteUtil.combineBytes(FCTL_TAG_BYTES, ByteUtil.convertIntToFourBytes((i * 2 + 1)));
                    int fctlTagIndex = ByteUtil.indexOf(framesBytes, fctlTagBytes);
                    int nextFctlTagIndex = ByteUtil.indexOf(framesBytes, nextFctlTagBytes);
                    byte[] frameBytes = ByteUtil.subBytes(framesBytes, fctlTagIndex, nextFctlTagIndex);
                    fctlBytes = ByteUtil.subBytes(frameBytes, 0, ByteUtil.indexOf(frameBytes, FDAT_TAG_BYTES) - 4);
                    fdatBytes = ByteUtil.subBytes(frameBytes, ByteUtil.indexOf(frameBytes, FDAT_TAG_BYTES) - 4, (frameBytes.length - 1)- 4);
                }
                Log.e("Apng", "i: " + i + ", fctl: "+ ByteUtil.bytesToHex(fctlBytes));
                Log.e("Apng", "i: " + i + ", fdat: "+ ByteUtil.bytesToHex(fdatBytes));
                frameEntity.setFctlBytes(fctlBytes);
                frameEntity.setIdatBytes(fdatBytes);
            }
            frameList.add(frameEntity);
        }
    }

    public boolean isApng() {
        return isApng;
    }

    public void setApng(boolean apng) {
        isApng = apng;
    }

    public byte[] getPngAndIhdrBytes() {
        return pngAndIhdrBytes;
    }

    public void setPngAndIhdrBytes(byte[] pngAndIhdrBytes) {
        this.pngAndIhdrBytes = pngAndIhdrBytes;
    }

    public byte[] getActlBytes() {
        return actlBytes;
    }

    public void setActlBytes(byte[] actlBytes) {
        this.actlBytes = actlBytes;
    }

    public int getFrameCount() {
        return frameCount;
    }

    public void setFrameCount(int frameCount) {
        this.frameCount = frameCount;
    }

    public int getRepeatCount() {
        return repeatCount;
    }

    public void setRepeatCount(int repeatCount) {
        this.repeatCount = repeatCount;
    }

    public List<FrameEntity> getFrameList() {
        return frameList;
    }

    public void setFrameList(List<FrameEntity> frameList) {
        this.frameList = frameList;
    }

    public Bitmap generateImageBitmap(FrameEntity frameEntity) {
        Log.e("Apng", "generateImageBitmap pngAndIhderBytes.length: " + pngAndIhdrBytes.length);
        Log.e("Apng", "frameEntity.getIdatBytes().length: " + frameEntity.getIdatBytes().length);
        Log.e("Apng", "generateImageBitmap IEND_TAG_BYTES.length: " + IEND_TAG_BYTES.length);

        byte[] imageBytes = new byte[pngAndIhdrBytes.length + frameEntity.getIdatBytes().length + IEND_TAG_BYTES.length];
        for(int i = 0; i < pngAndIhdrBytes.length; i ++) {
            imageBytes[i] = pngAndIhdrBytes[i];
        }
        for(int i = 0; i < frameEntity.getIdatBytes().length; i ++) {
            imageBytes[i + pngAndIhdrBytes.length] = frameEntity.getIdatBytes()[i];
        }
        for(int i = 0; i < IEND_TAG_BYTES.length; i ++) {
            imageBytes[i + pngAndIhdrBytes.length + frameEntity.getIdatBytes().length] = IEND_TAG_BYTES[i];
        }

        Log.e("Apng", "imageBytes: " + ByteUtil.bytesToHex(imageBytes));
        Log.e("Apng", "imageBytes length: " + imageBytes.length);
        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        Log.e("Apng", "isBitmapNull: " + (bitmap == null));

        return bitmap;
    }
}
