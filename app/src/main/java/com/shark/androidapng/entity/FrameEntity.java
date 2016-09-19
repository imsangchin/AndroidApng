package com.shark.androidapng.entity;

/**
 * Created by Shark0 on 2016/9/13.
 */
public class FrameEntity {
    private ChunkEntity frameControlChunk;

    private ChunkEntity frameDataChunk;

    public ChunkEntity getFrameControlChunk() {
        return frameControlChunk;
    }

    public void setFrameControlChunk(ChunkEntity frameControlChunk) {
        this.frameControlChunk = frameControlChunk;
    }

    public ChunkEntity getFrameDataChunk() {
        return frameDataChunk;
    }

    public void setFrameDataChunk(ChunkEntity frameDataChunk) {
        this.frameDataChunk = frameDataChunk;
    }
}
