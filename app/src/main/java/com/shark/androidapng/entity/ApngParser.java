package com.shark.androidapng.entity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.shark.androidapng.util.ByteUtil;

import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.CRC32;

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
    private final String PLTE_TAG = "PLTE";
    private final String ACTL_TAG = "acTL";
    private final String IDAT_TAG = "IDAT";
    private final String FCTL_TAG = "fcTL";
    private final String FDAT_TAG = "fdAT";
    private final String IEND_TAG = "IEND";

    private final int CHUNK_DATA_LENGTH_BYTES_LENGTH = 4;
    private final int CHUNK_TAG_BYTES_LENGTH = 4;
    private final int CHUNK_CRC_BYTES_LENGTH = 4;

    private byte[] imageBytes;
    private Bitmap bitmap;
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
        bitmap =  BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

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
            Log.e("Apng", "i: " + i + ", length: "+ length);

            int tagStartIndex = chunkLengthEndIndex;
            int tagEndIndex = tagStartIndex + CHUNK_TAG_BYTES_LENGTH;
            byte[] tagBytes = ByteUtil.subBytes(imageBytes, tagStartIndex, tagEndIndex);
            Log.e("Apng", "i: " + i + ", chunkTag: "+ ByteUtil.bytesToHex(tagBytes));
            String tag = new String(tagBytes, StandardCharsets.UTF_8);
            Log.e("Apng", "i: " + i + ", chunkTag: "+ tag);

            int dataStartIndex = tagEndIndex;
            int dataEndIndex = dataStartIndex + length;
            byte[] dataBytes = ByteUtil.subBytes(imageBytes, dataStartIndex, dataEndIndex);
            Log.e("Apng", "i: " + i + ", chunkData: "+ ByteUtil.bytesToHex(dataBytes));

            int crcStartIndex = dataEndIndex;
            int crcEndIndex = crcStartIndex + CHUNK_CRC_BYTES_LENGTH;
            byte[] crcBytes = ByteUtil.subBytes(imageBytes, crcStartIndex, crcEndIndex);
            Log.e("Apng", "i: " + i + ", chunkCrc: "+ ByteUtil.bytesToHex(crcBytes));
            ChunkEntity chunkEntity;
            if(tag.equalsIgnoreCase(FCTL_TAG)) {
                chunkEntity = new FctlChunkEntity();
            } else {
                chunkEntity = new ChunkEntity();

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
                    ihdrChunkEntity = chunkEntity;
                    CRC32 crc32 = new CRC32();
                    crc32.update(chunkEntity.getTagBytes(), 0, 4);
                    if(chunkEntity.getLength() > 0) {
                        crc32.update(chunkEntity.getDataBytes(), 0, chunkEntity.getLength());
                    }
                    Log.e("APNG", "IHDR CRC: " + ByteUtil.bytesToHex(ByteUtil.intToBytes((int) crc32.getValue())));
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
                    frameEntity.setFrameControlChunk((FctlChunkEntity) chunkList.get(i - 1));
                    frameEntity.setFrameDataChunk(chunkEntity);
                    frameList.add(frameEntity);
                    crc32 = new CRC32();
                    crc32.update(chunkEntity.getTagBytes(), 0, 4);
                    if(chunkEntity.getLength() > 0) {
                        crc32.update(chunkEntity.getDataBytes(), 0, chunkEntity.getLength());
                    }
                    byte[] idatCrcBytes = ByteUtil.intToBytes((int) crc32.getValue());
                    Log.e("APNG", "IDAT CRC: " + ByteUtil.bytesToHex(idatCrcBytes));
                    break;
                case FDAT_TAG:
                    frameEntity = new FrameEntity();
                    frameEntity.setFrameControlChunk((FctlChunkEntity) chunkList.get(i - 1));
                    Log.e("Apng", "FDat length: " + chunkEntity.getLength());
                    int newLength = chunkEntity.getLength() - 4;
                    Log.e("Apng", "FDat new length: " + newLength);
                    chunkEntity.setLength(newLength);
                    chunkEntity.setLengthBytes(ByteUtil.intToBytes(newLength));
                    chunkEntity.setTag(IDAT_TAG);
                    chunkEntity.setTagBytes(IDAT_TAG_BYTES);
                    chunkEntity.setDataBytes(ByteUtil.subBytes(chunkEntity.getDataBytes(), 4, chunkEntity.getDataBytes().length));
                    Log.e("Apng", "FDat data length: " + chunkEntity.getDataBytes().length);
                    crc32 = new CRC32();
                    crc32.update(chunkEntity.getTagBytes(), 0, 4);
                    if(chunkEntity.getLength() > 0) {
                        crc32.update(chunkEntity.getDataBytes(), 0, chunkEntity.getLength());
                    }
                    byte[] fdatCrcBytes = ByteUtil.intToBytes((int) crc32.getValue());
                    Log.e("APNG", "FDAT CRC: " + ByteUtil.bytesToHex(fdatCrcBytes));
                    chunkEntity.setCrcBytes(fdatCrcBytes);
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
        for(ChunkEntity chunkEntity: bitmapChunkList) {
            imageBytesSize = imageBytesSize + chunkEntity.getLengthBytes().length + chunkEntity.getTag().length() +
                    chunkEntity.getDataBytes().length + chunkEntity.getCrcBytes().length;
        }
        Log.e("Apng", "generateImageDataBitmap  imageBytesSize: " + imageBytesSize);
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

        Log.e("Apng", "imageBytes: " + ByteUtil.bytesToHex(imageBytes));
        Log.e("Apng", "imageBytes length: " + imageBytes.length);
        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        Log.e("Apng", "isBitmapNull: " + (bitmap == null));

        return bitmap;
    }
    public Bitmap generateFrameDataBitmap(FrameEntity frameEntity) {
        FctlChunkEntity fctlChunkEntity = frameEntity.getFrameControlChunk();
        ChunkEntity frameDataChunkEntity = frameEntity.getFrameDataChunk();

        List<ChunkEntity> bitmapChunkList = new LinkedList<>();

        byte[] ihdrDataByte = ihdrChunkEntity.getDataBytes();
        byte[] widthBytes = ByteUtil.intToBytes(fctlChunkEntity.getWidth());
        byte[] heightBytes = ByteUtil.intToBytes(fctlChunkEntity.getHeight());
        for(int i = 0; i < widthBytes.length; i ++) {
            ihdrDataByte[i] = widthBytes[i];
        }
        for(int i = 0; i < heightBytes.length; i ++) {
            ihdrDataByte[i + 4] = heightBytes[i];
        }
        ihdrChunkEntity.setDataBytes(ihdrDataByte);
        CRC32 crc32 = new CRC32();
        crc32.update(ihdrChunkEntity.getTagBytes(), 0, 4);
        if(ihdrChunkEntity.getLength() > 0) {
            crc32.update(ihdrChunkEntity.getDataBytes(), 0, ihdrChunkEntity.getLength());
        }
        byte[] ihdrCrcBytes = ByteUtil.intToBytes((int) crc32.getValue());
        Log.e("APNG", "IHDR CRC: " + ByteUtil.bytesToHex(ihdrCrcBytes));
        ihdrChunkEntity.setCrcBytes(ihdrCrcBytes);

        bitmapChunkList.add(ihdrChunkEntity);
        if(plteChunkEntity != null) {
            bitmapChunkList.add(plteChunkEntity);
        }
        bitmapChunkList.add(frameDataChunkEntity);
        bitmapChunkList.add(iendChunkEntity);

        int imageBytesSize = PNG_TAG_BYTES.length;
        for(ChunkEntity chunkEntity: bitmapChunkList) {
            imageBytesSize = imageBytesSize + chunkEntity.getLengthBytes().length + chunkEntity.getTag().length() +
                    chunkEntity.getDataBytes().length + chunkEntity.getCrcBytes().length;
        }
        Log.e("Apng", "generateImageDataBitmap  imageBytesSize: " + imageBytesSize);
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

        Log.e("Apng", "imageBytes: " + ByteUtil.bytesToHex(imageBytes));
        Log.e("Apng", "imageBytes length: " + imageBytes.length);
        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        Log.e("Apng", "isBitmapNull: " + (bitmap == null));
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

    public ChunkEntity getIhdrChunkEntity() {
        return ihdrChunkEntity;
    }

    public void setIhdrChunkEntity(ChunkEntity ihdrChunkEntity) {
        this.ihdrChunkEntity = ihdrChunkEntity;
    }

    public ChunkEntity getPlteChunkEntity() {
        return plteChunkEntity;
    }

    public void setPlteChunkEntity(ChunkEntity plteChunkEntity) {
        this.plteChunkEntity = plteChunkEntity;
    }

    public ChunkEntity getActlChunkEntity() {
        return actlChunkEntity;
    }

    public void setActlChunkEntity(ChunkEntity actlChunkEntity) {
        this.actlChunkEntity = actlChunkEntity;
    }

    public ChunkEntity getIendChunkEntity() {
        return iendChunkEntity;
    }

    public void setIendChunkEntity(ChunkEntity iendChunkEntity) {
        this.iendChunkEntity = iendChunkEntity;
    }

    public List<ChunkEntity> getChunkList() {
        return chunkList;
    }

    public void setChunkList(List<ChunkEntity> chunkList) {
        this.chunkList = chunkList;
    }
}
