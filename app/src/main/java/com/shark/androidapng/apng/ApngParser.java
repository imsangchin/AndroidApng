package com.shark.androidapng.apng;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.shark.androidapng.apng.entity.ActlChunkEntity;
import com.shark.androidapng.apng.entity.ChunkEntity;
import com.shark.androidapng.apng.entity.FctlChunkEntity;
import com.shark.androidapng.apng.entity.FrameEntity;
import com.shark.androidapng.apng.entity.IhdrChunkEntity;
import com.shark.androidapng.util.ByteUtil;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.CRC32;

/**
 * Created by Shark0 on 2016/9/13.
 */
public class ApngParser {

    private boolean debug = true;

    public static final byte[] PNG_TAG_BYTES = new byte[]{(byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47, (byte) 0x0D, (byte) 0x0A, (byte) 0x1A, (byte) 0x0A};

    public static final byte[] IHDR_TAG_BYTES = new byte[]{(byte) 0x49, (byte) 0x48, (byte) 0x44, (byte) 0x52};

    public static final byte[] ACTL_TAG_BYTES = new byte[]{(byte) 0x61, (byte) 0x63, (byte) 0x54, (byte) 0x4c};

    public static final byte[] FCTL_TAG_BYTES = new byte[]{(byte) 0x66, (byte) 0x63, (byte) 0x54, (byte) 0x4c};

    public static final byte[] IDAT_TAG_BYTES = new byte[]{(byte) 0x49, (byte) 0x44, (byte) 0x41, (byte) 0x54};

    public static final byte[] FDAT_TAG_BYTES = new byte[]{(byte) 0x66, (byte) 0x64, (byte) 0x41, (byte) 0x54};

    public final String IHDR_TAG = "IHDR";
    public final String PLTE_TAG = "PLTE";
    public final String ACTL_TAG = "acTL";
    public final String IDAT_TAG = "IDAT";
    public final String FCTL_TAG = "fcTL";
    public final String FDAT_TAG = "fdAT";
    public final String IEND_TAG = "IEND";

    private final int CHUNK_DATA_LENGTH_BYTES_LENGTH = 4;
    private final int CHUNK_TAG_BYTES_LENGTH = 4;
    private final int CHUNK_CRC_BYTES_LENGTH = 4;

    private byte[] imageBytes;
    private Bitmap bitmap;
    private boolean isApng;

    private IhdrChunkEntity ihdrChunkEntity;
    private ChunkEntity plteChunkEntity;
    private ActlChunkEntity actlChunkEntity;

    private List<FrameEntity> frameList = new LinkedList<>();
    private List<ChunkEntity> unknowChunkList = new LinkedList<>();

    private ChunkEntity iendChunkEntity;
    private List<ChunkEntity> chunkList = new LinkedList<>();



    public ApngParser(byte[] imageBytes) {
        this.imageBytes = imageBytes;
        bitmap =  BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

        int actlIndex = ByteUtil.indexOf(imageBytes, ACTL_TAG_BYTES);
        isApng = actlIndex != -1;
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
            if(debug) {
                Log.e("Apng", "i: " + i + ", length: "+ length);
            }
            int tagStartIndex = chunkLengthEndIndex;
            int tagEndIndex = tagStartIndex + CHUNK_TAG_BYTES_LENGTH;
            byte[] tagBytes = ByteUtil.subBytes(imageBytes, tagStartIndex, tagEndIndex);
            if(debug) {
                Log.e("Apng", "i: " + i + ", chunkTag: "+ ByteUtil.bytesToHex(tagBytes));
            }
            String tag = new String(tagBytes, StandardCharsets.UTF_8);
            if(debug) {
                Log.e("Apng", "i: " + i + ", chunkTag: "+ tag);
            }
            int dataStartIndex = tagEndIndex;
            int dataEndIndex = dataStartIndex + length;
            byte[] dataBytes = ByteUtil.subBytes(imageBytes, dataStartIndex, dataEndIndex);
            if(debug) {
                Log.e("Apng", "i: " + i + ", chunkData: "+ ByteUtil.bytesToHex(dataBytes));
            }
            int crcStartIndex = dataEndIndex;
            int crcEndIndex = crcStartIndex + CHUNK_CRC_BYTES_LENGTH;
            byte[] crcBytes = ByteUtil.subBytes(imageBytes, crcStartIndex, crcEndIndex);
            if(debug) {
                Log.e("Apng", "i: " + i + ", chunkCrc: "+ ByteUtil.bytesToHex(crcBytes));
            }
            ChunkEntity chunkEntity;
            switch (tag) {
                case ACTL_TAG:
                    chunkEntity = new ActlChunkEntity();
                    break;
                case IHDR_TAG:
                    chunkEntity = new IhdrChunkEntity();
                    break;
                case FCTL_TAG:
                    chunkEntity = new FctlChunkEntity();
                    break;
                default:
                    chunkEntity = new ChunkEntity();
                    break;
            }

            chunkEntity.setLengthBytes(lengthBytes);
            chunkEntity.setLength(length);
            chunkEntity.setTagBytes(tagBytes);
            chunkEntity.setTag(tag);
            chunkEntity.setDataBytes(dataBytes);
            chunkEntity.setCrcBytes(crcBytes);
            chunkList.add(chunkEntity);
            switch (tag) {
                case IHDR_TAG:
                    ihdrChunkEntity = (IhdrChunkEntity) chunkEntity;
                    break;
                case PLTE_TAG:
                    plteChunkEntity = chunkEntity;
                    break;
                case ACTL_TAG:
                    actlChunkEntity = (ActlChunkEntity) chunkEntity;
                    break;
                case IDAT_TAG:
                    FrameEntity frameEntity = new FrameEntity();
                    frameEntity.setFrameControlChunk((FctlChunkEntity) chunkList.get(i - 1));
                    frameEntity.setFrameDataChunk(chunkEntity);
                    frameList.add(frameEntity);
                    break;
                case FDAT_TAG:
                    frameEntity = new FrameEntity();
                    frameEntity.setFrameControlChunk((FctlChunkEntity) chunkList.get(i - 1));
                    int newLength = chunkEntity.getLength() - 4;
                    chunkEntity.setLength(newLength);
                    chunkEntity.setLengthBytes(ByteUtil.intToBytes(newLength));
                    chunkEntity.setTag(IDAT_TAG);
                    chunkEntity.setTagBytes(IDAT_TAG_BYTES);
                    chunkEntity.setDataBytes(ByteUtil.subBytes(chunkEntity.getDataBytes(), 4, chunkEntity.getDataBytes().length));
                    CRC32 crc32 = new CRC32();
                    crc32.update(chunkEntity.getTagBytes(), 0, 4);
                    if(chunkEntity.getLength() > 0) {
                        crc32.update(chunkEntity.getDataBytes(), 0, chunkEntity.getLength());
                    }
                    byte[] fdatCrcBytes = ByteUtil.intToBytes((int) crc32.getValue());
                    chunkEntity.setCrcBytes(fdatCrcBytes);
                    frameEntity.setFrameDataChunk(chunkEntity);
                    frameList.add(frameEntity);
                    break;
                case IEND_TAG:
                    iendChunkEntity = chunkEntity;
                    break;
                default:
                    unknowChunkList.add(chunkEntity);
                    break;
            }
            chunkLengthStartIndex = crcEndIndex;
            i = i + 1;
        }
    }

    public Bitmap generateFrameDataBitmap(FrameEntity frameEntity) {
        FctlChunkEntity fctlChunkEntity = frameEntity.getFrameControlChunk();
        ChunkEntity frameDataChunkEntity = frameEntity.getFrameDataChunk();

        List<ChunkEntity> bitmapChunkList = new LinkedList<>();

        ihdrChunkEntity.setWidth(fctlChunkEntity.getWidth());
        ihdrChunkEntity.setHeight(fctlChunkEntity.getHeight());
        bitmapChunkList.add(ihdrChunkEntity);

        if(plteChunkEntity != null) {
            bitmapChunkList.add(plteChunkEntity);
        }
        bitmapChunkList.addAll(unknowChunkList);
        bitmapChunkList.add(frameDataChunkEntity);
        bitmapChunkList.add(iendChunkEntity);

        int imageBytesSize = PNG_TAG_BYTES.length;
        for(ChunkEntity chunkEntity: bitmapChunkList) {
            imageBytesSize = imageBytesSize + chunkEntity.getLengthBytes().length + chunkEntity.getTag().length() +
                    chunkEntity.getDataBytes().length + chunkEntity.getCrcBytes().length;
        }
        byte[] imageBytes = new byte[imageBytesSize];

        for(int i = 0; i < PNG_TAG_BYTES.length; i ++) {
            imageBytes[i ] = PNG_TAG_BYTES[i];
        }
        int startIndex = PNG_TAG_BYTES.length;
        for(ChunkEntity chunkEntity: bitmapChunkList) {
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
            for(int i = 0; i < chunkEntity.getCrcBytes().length; i ++) {
                imageBytes[i + startIndex] = chunkEntity.getCrcBytes()[i];
            }
            startIndex = startIndex + chunkEntity.getCrcBytes().length;
        }

        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        if(debug) {
            Log.e("Apng", "generateFrameDataBitmap ihdr colour type: " + ihdrChunkEntity.getColourType());
            Log.e("Apng", "generateFrameDataBitmap image bytes: " + ByteUtil.bytesToHex(imageBytes));
            Log.e("Apng", "generateFrameDataBitmap is bitmap: " + (bitmap != null));
        }
        return bitmap;
    }

    public byte[] getImageBytes() {
        return imageBytes;
    }

    public void setImageBytes(byte[] imageBytes) {
        this.imageBytes = imageBytes;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public boolean isApng() {
        return isApng;
    }

    public void setApng(boolean apng) {
        isApng = apng;
    }

    public List<FrameEntity> getFrameList() {
        return frameList;
    }

    public void setFrameList(List<FrameEntity> frameList) {
        this.frameList = frameList;
    }
}
