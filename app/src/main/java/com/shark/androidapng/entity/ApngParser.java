package com.shark.androidapng.entity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.shark.androidapng.util.ByteUtil;

import java.nio.charset.StandardCharsets;
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

    private final String IHDR_TAG = "IHDR";
    private final String PLTE_TAG = "PLTE_TAG";
    private final String ACTL_TAG = "acTL";
    private final String IDAT_TAG = "IDAT";
    private final String FCTL_TAG = "fcTL";
    private final String FDAT_TAG = "fdAT";
    private final String IEND_TAG = "IEND";

    public static final byte[] IEND_BYTES = new byte[]{
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, //Length
            (byte) 0x49, (byte) 0x45, (byte) 0x4E, (byte) 0x44, //Tag
            (byte) 0xae, (byte) 0x42, (byte) 0x60, ((byte) 0x82)};  //CRC


    private final int CHUNK_DATA_LENGTH_BYTES_LENGTH = 4;
    private final int CHUNK_TAG_BYTES_LENGTH = 4;
    private final int CHUNK_CRC_BYTES_LENGTH = 4;

    private byte[] imageBytes;
    private boolean isApng;

    private ChunkEntity ihdrChunkEntity;

    private ChunkEntity plteChunkEntity;

    private ChunkEntity actlChunkEntity;
    private int frameCount;
    private int repeatCount;

    private List<FrameEntity> frameList = new LinkedList<>();

    private ChunkEntity iendChunkEntity;
    private List<ChunkEntity> chunkList = new LinkedList<>();


    public ApngParser(byte[] imageBytes) {
        this.imageBytes = imageBytes;
        int actlIndex = ByteUtil.indexOf(imageBytes, ACTL_TAG_BYTES);
        isApng = actlIndex != -1;
        Log.e("Apng", "actlIndex: " + actlIndex);
        if(!isApng) {
            return;
        }

        int chunkLengthStartIndex = PNG_TAG_BYTES.length;
        int i = 0;
        while(chunkLengthStartIndex + CHUNK_DATA_LENGTH_BYTES_LENGTH < imageBytes.length) {
            int chunkLengthEndIndex = chunkLengthStartIndex + CHUNK_DATA_LENGTH_BYTES_LENGTH;
            byte[] lengthBytes = ByteUtil.subBytes(imageBytes,
                    chunkLengthStartIndex, chunkLengthEndIndex);
            int length = ByteUtil.bytesToInt(lengthBytes);
//            Log.e("Apng", "i: " + i + ", length: "+ length);

            int tagStartIndex = chunkLengthEndIndex;
            int tagEndIndex = tagStartIndex + CHUNK_TAG_BYTES_LENGTH;
            byte[] tagBytes = ByteUtil.subBytes(imageBytes, tagStartIndex, tagEndIndex);
//            Log.e("Apng", "i: " + i + ", chunkTag: "+ ByteUtil.bytesToHex(tagBytes));
            String tag = new String(tagBytes, StandardCharsets.UTF_8);
//            Log.e("Apng", "i: " + i + ", chunkTag: "+ tag);

            int dataStartIndex = tagEndIndex;
            int dataEndIndex = dataStartIndex + length;
            byte[] dataBytes = ByteUtil.subBytes(imageBytes, dataStartIndex, dataEndIndex);
//            Log.e("Apng", "i: " + i + ", chunkData: "+ ByteUtil.bytesToHex(dataBytes));

            int crcStartIndex = dataEndIndex;
            int crcEndIndex = crcStartIndex + CHUNK_CRC_BYTES_LENGTH;
            byte[] crcBytes = ByteUtil.subBytes(imageBytes, crcStartIndex, crcEndIndex);
//            Log.e("Apng", "i: " + i + ", chunkCrc: "+ ByteUtil.bytesToHex(crcBytes));
            ChunkEntity chunkEntity = new ChunkEntity();
            chunkEntity.setLengthBytes(lengthBytes);
            chunkEntity.setLength(length);
            chunkEntity.setTagBytes(tagBytes);
            chunkEntity.setTag(tag);
            chunkEntity.setDataBytes(dataBytes);
            chunkEntity.setCrcBytes(crcBytes);
            chunkList.add(chunkEntity);

            switch (tag) {
                case IHDR_TAG:
                    ihdrChunkEntity = chunkEntity;
                    break;
                case PLTE_TAG:
                    plteChunkEntity = chunkEntity;
                    break;
                case ACTL_TAG:
                    actlChunkEntity = chunkEntity;
                    byte[] frameCountBytes = ByteUtil.subBytes(actlChunkEntity.getDataBytes(), 0, 4);
//                    Log.e("Apng", "frameCountBytes: " + ByteUtil.bytesToHex(frameCountBytes));
                    frameCount = ByteUtil.bytesToInt(frameCountBytes);
//                    Log.e("Apng", "frameCount: " + frameCount);

                    byte[] repeatCountBytes = ByteUtil.subBytes(actlChunkEntity.getDataBytes(), 4, 8);
//                    Log.e("Apng", "repeatCountBytes: " + ByteUtil.bytesToHex(repeatCountBytes));
                    repeatCount = ByteUtil.bytesToInt(repeatCountBytes);
//                    Log.e("Apng", "repeatCount: " + repeatCount);
                    break;
                case IDAT_TAG:
                    FrameEntity frameEntity = new FrameEntity();
                    frameEntity.setFrameControlChunk(chunkList.get(i - 1));
                    frameEntity.setFrameDataChunk(chunkEntity);
                    frameList.add(frameEntity);
                    break;
                case FDAT_TAG:
                    frameEntity = new FrameEntity();
                    frameEntity.setFrameControlChunk(chunkList.get(i - 1));
                    frameEntity.setFrameDataChunk(chunkEntity);
                    frameList.add(frameEntity);
                    break;
                case IEND_TAG:
                    iendChunkEntity = chunkEntity;
                    break;
            }
            chunkLengthStartIndex = crcEndIndex;
            i = i + 1;
        }
    }

    public Bitmap generateImageDataBitmap(ChunkEntity imageDataChunk) {
        List<ChunkEntity> bitmapChunkList = new LinkedList<>();
        bitmapChunkList.add(ihdrChunkEntity);
        if(plteChunkEntity != null) {
            bitmapChunkList.add(plteChunkEntity);
        }
        bitmapChunkList.add(imageDataChunk);
        bitmapChunkList.add(iendChunkEntity);

        int imageBytesSize = PNG_TAG_BYTES.length;
        for(ChunkEntity chunkEntity: chunkList) {
            imageBytesSize = imageBytesSize + chunkEntity.getLengthBytes().length + chunkEntity.getTag().length() +
                    chunkEntity.getDataBytes().length + chunkEntity.getCrcBytes().length;
        }
        Log.e("Apng", "generateImageDataBitmap  imageBytesSize: " + imageBytesSize);
        byte[] imageBytes = new byte[imageBytesSize];

        for(int i = 0; i < PNG_TAG_BYTES.length; i ++) {
            imageBytes[i ] = PNG_TAG_BYTES[i];
        }
        int startIndex = PNG_TAG_BYTES.length;
        for(ChunkEntity chunkEntity: chunkList) {
            for(int i = 0; i < chunkEntity.getLengthBytes().length; i ++) {
                imageBytes[i + startIndex] = chunkEntity.getLengthBytes()[i];
            }
            startIndex = startIndex + chunkEntity.getLengthBytes().length;
            for(int i = 0; i < chunkEntity.getTagBytes().length; i ++) {
                imageBytes[i + startIndex] = chunkEntity.getTagBytes()[i];
            }

            startIndex = startIndex + chunkEntity.getTagBytes().length;
            for(int i = 0; i < chunkEntity.getDataBytes().length; i ++) {
                imageBytes[i + startIndex] = chunkEntity.getDataBytes()[i];
            }
            startIndex = startIndex + chunkEntity.getDataBytes().length;
            for(int i = 0; i < ihdrChunkEntity.getCrcBytes().length; i ++) {
                imageBytes[i + startIndex] = chunkEntity.getCrcBytes()[i];
            }
            startIndex = startIndex + chunkEntity.getCrcBytes().length;
        }

        Log.e("Apng", "imageBytes: " + ByteUtil.bytesToHex(imageBytes));
        Log.e("Apng", "imageBytes length: " + imageBytes.length);
        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        Log.e("Apng", "isBitmapNull: " + (bitmap == null));

        return bitmap;
    }

    public boolean isApng() {
        return isApng;
    }

    public void setApng(boolean apng) {
        isApng = apng;
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
}
