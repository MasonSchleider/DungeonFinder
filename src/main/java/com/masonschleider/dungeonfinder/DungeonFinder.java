package com.masonschleider.dungeonfinder;

import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.MobSpawnerTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.server.ServerChunkProvider;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Set;

@SuppressWarnings("WeakerAccess")
public class DungeonFinder extends Thread {
    private final ServerChunkProvider chunkProvider;
    private final ObserverList observers;
    
    private volatile boolean paramsModified = false;
    private volatile Status status = Status.NEW;
    
    private ChunkPosition searchOrigin;
    private int searchRadius;
    
    public DungeonFinder(BlockPos searchOrigin, int searchRadius, ServerChunkProvider chunkProvider) {
        this.chunkProvider = chunkProvider;
        this.observers = new ObserverList();
        
        setSearchOrigin(searchOrigin);
        setSearchRadius(searchRadius);
    }
    
    public enum Status {
        NEW,
        IDLE,
        RUNNING,
        SUSPENDED,
        TERMINATED
    }
    
    public interface Observer {
        void update(ArrayList<MobSpawnerTileEntity> spawners);
    }
    
    //region Getters & Setters
    public ChunkPosition getSearchOrigin() {
        return this.searchOrigin;
    }
    
    public void setSearchOrigin(BlockPos searchOrigin) {
        setSearchOrigin(new ChunkPosition(searchOrigin));
    }
    
    public synchronized void setSearchOrigin(ChunkPosition searchOrigin) {
        this.searchOrigin = searchOrigin;
        
        if (!EnumSet.of(Status.NEW, Status.SUSPENDED, Status.TERMINATED).contains(this.status)) {
            this.paramsModified = true;
            notify();
        }
    }
    
    public int getSearchRadius() {
        return this.searchRadius;
    }
    
    public synchronized void setSearchRadius(int searchRadius) {
        if (searchRadius < 0)
            throw new IllegalArgumentException("Value must be greater than or equal to 0.");
        
        this.searchRadius = searchRadius;
    
        if (!EnumSet.of(Status.NEW, Status.SUSPENDED, Status.TERMINATED).contains(this.status)) {
            this.paramsModified = true;
            notify();
        }
    }
    
    public Status getStatus() {
        return this.status;
    }
    //endregion
    
    public void addObserver(Observer observer) {
        this.observers.add(observer);
    }
    
    public boolean removeObserver(Observer observer) {
        return this.observers.remove(observer);
    }
    
    public synchronized void reset() {
        this.paramsModified = true;
        notify();
    }
    
    public synchronized void resumeThread() {
        if (this.paramsModified)
            this.status = Status.RUNNING;
        else
            this.status = Status.IDLE;
        notify();
    }
    
    @Override
    public void run() {
        this.status = Status.RUNNING;
        
        while (this.status != Status.TERMINATED) {
            ChunkPosition currentPos = new ChunkPosition(0, 0);
            Direction direction = Direction.NORTH;
            int radius = 0;
    
            ChunkPosition searchOrigin = this.searchOrigin;
            int searchRadius = this.searchRadius;
        
            while (radius <= searchRadius) {
                ArrayList<MobSpawnerTileEntity> spawners = getSpawners(searchOrigin.add(currentPos));
            
                if (spawners.size() > 0)
                    this.observers.update(spawners);
            
                switch (direction) {
                    case NORTH:
                        if (-currentPos.z == radius) {
                            direction = Direction.EAST;
                            radius++;
                        }
                    
                        currentPos = currentPos.north();
                        break;
                    case EAST:
                        currentPos = currentPos.east();
                    
                        if (currentPos.x == radius)
                            direction = Direction.SOUTH;
                        break;
                    case SOUTH:
                        currentPos = currentPos.south();
                    
                        if (currentPos.z == radius)
                            direction = Direction.WEST;
                        break;
                    case WEST:
                        currentPos = currentPos.west();
                    
                        if (-currentPos.x == radius)
                            direction = Direction.NORTH;
                        break;
                }
            }
            
            if (this.status == Status.RUNNING && !this.paramsModified)
                this.status = Status.IDLE;
            
            if (shouldWait()) {
                try {
                    synchronized (this) {
                        while (shouldWait())
                            wait();
                    }
                } catch (InterruptedException e) {
                    this.status = Status.TERMINATED;
                    break;
                }
            }
            
            this.paramsModified = false;
            if (this.status == Status.IDLE)
                this.status = Status.RUNNING;
        }
    }
    
    public synchronized void stopThread() {
        this.status = Status.TERMINATED;
        notify();
    }
    
    public void suspendThread() {
        this.status = Status.SUSPENDED;
    }
    
    private IChunk getChunkAt(ChunkPosition chunkPos) {
        return this.chunkProvider.getChunk(chunkPos.x, chunkPos.z, false);
    }
    
    private ArrayList<MobSpawnerTileEntity> getSpawners(ChunkPosition chunkPos) {
        return getSpawners(getChunkAt(chunkPos));
    }
    
    private ArrayList<MobSpawnerTileEntity> getSpawners(IChunk chunk) {
        ArrayList<MobSpawnerTileEntity> spawners = new ArrayList<>();
        
        if (chunk != null) {
            Set<BlockPos> tileEntityPositions = chunk.getTileEntitiesPos();
    
            for (BlockPos blockPos : tileEntityPositions) {
                TileEntity tileEntity = Minecraft.getInstance().world.getTileEntity(blockPos);
        
                if (tileEntity != null && tileEntity.getType() == TileEntityType.MOB_SPAWNER)
                    spawners.add((MobSpawnerTileEntity) tileEntity);
            }
        }
        
        return spawners;
    }
    
    private boolean shouldWait() {
        return (this.status == Status.IDLE && !this.paramsModified) ||
                this.status == Status.SUSPENDED;
    }
    
    private static class ObserverList extends ArrayList<Observer> {
        void update(ArrayList<MobSpawnerTileEntity> spawners) {
            for (Observer observer : this)
                observer.update(spawners);
        }
    }
}
