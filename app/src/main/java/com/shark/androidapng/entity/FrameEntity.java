package com.shark.androidapng.entity;

/**
 * Created by Shark0 on 2016/9/13.
 */
public class FrameEntity {
    private byte[] fctlBytes;

    private byte[] idatBytes;

    public byte[] getFctlBytes() {
        return fctlBytes;
    }

    public void setFctlBytes(byte[] fctlBytes) {
        this.fctlBytes = fctlBytes;
    }

    public byte[] getIdatBytes() {
        return idatBytes;
    }

    public void setIdatBytes(byte[] idatBytes) {
        this.idatBytes = idatBytes;
    }
}
