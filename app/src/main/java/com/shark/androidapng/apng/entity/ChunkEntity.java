package com.shark.androidapng.apng.entity;

/**
 * Created by Shark0 on 2016/9/19.
 */
public class ChunkEntity {

    private byte[] lengthBytes;

    private int length;

    private byte[] tagBytes;

    private String tag;

    private byte[] dataBytes;

    private byte[] crcBytes;

    public byte[] getLengthBytes() {
        return lengthBytes;
    }

    public void setLengthBytes(byte[] lengthBytes) {
        this.lengthBytes = lengthBytes;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public byte[] getTagBytes() {
        return tagBytes;
    }

    public void setTagBytes(byte[] tagBytes) {
        this.tagBytes = tagBytes;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public byte[] getDataBytes() {
        return dataBytes;
    }

    public void setDataBytes(byte[] dataBytes) {
        this.dataBytes = dataBytes;
    }

    public byte[] getCrcBytes() {
        return crcBytes;
    }

    public void setCrcBytes(byte[] crcBytes) {
        this.crcBytes = crcBytes;
    }
}
