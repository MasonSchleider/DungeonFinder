package com.masonschleider.dungeonfinder;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

@SuppressWarnings("WeakerAccess")
public class ChunkPosition extends ChunkPos {
    public ChunkPosition(int x, int z) {
        super(x, z);
    }
    
    public ChunkPosition(BlockPos blockPos) {
        super(blockPos);
    }
    
    public ChunkPosition add(int x, int z) {
        return new ChunkPosition(this.x + x, this.z + z);
    }
    
    public ChunkPosition add(ChunkPosition chunkPosition) {
        return add(chunkPosition.x, chunkPosition.z);
    }
    
    public ChunkPosition add(BlockPos blockPos) {
        return add(new ChunkPosition(blockPos));
    }
    
    public ChunkPosition east() {
        return east(1);
    }
    
    public ChunkPosition east(int distance) {
        return new ChunkPosition(this.x + distance, this.z);
    }
    
    public ChunkPosition north() {
        return north(1);
    }
    
    public ChunkPosition north(int distance) {
        return new ChunkPosition(this.x, this.z - distance);
    }
    
    public ChunkPosition south() {
        return south(1);
    }
    
    public ChunkPosition south(int distance) {
        return new ChunkPosition(this.x, this.z + distance);
    }
    
    public ChunkPosition west() {
        return west(1);
    }
    
    public ChunkPosition west(int distance) {
        return new ChunkPosition(this.x - distance, this.z);
    }
}
