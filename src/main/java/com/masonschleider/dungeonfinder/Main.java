package com.masonschleider.dungeonfinder;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.tileentity.MobSpawnerTileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerChunkProvider;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Objects;

@Mod(Main.MODID)
@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Main {
    public static final String MODID = "dungeonfinder";
    
    private static final Logger LOGGER = LogManager.getLogger(MODID);
    
    private static DungeonFinder dungeonFinder;
    private static boolean overlayHidden = false;
    private static ArrayList<MobSpawnerTileEntity> spawnerList;
    
    public Main() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(Main::clientSetup);
    }
    
    @SuppressWarnings("WeakerAccess")
    static void clientSetup(FMLClientSetupEvent event) {
        spawnerList = new ArrayList<>();
        ModEventHandler.registerKeyBindings();
    }
    
    @SubscribeEvent
    public static void onGuiOpen(GuiOpenEvent event) {
        if (event.getGui() != null)
            return;
        
        int renderDistance = Minecraft.getInstance().gameSettings.renderDistanceChunks;
        if (renderDistance == dungeonFinder.getSearchRadius())
            return;
        
        dungeonFinder.setSearchRadius(renderDistance);
    }
    
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        IntegratedServer server = minecraft.getIntegratedServer();
    
        if (server == null)
            return;
    
        ServerChunkProvider chunkProvider = server.getWorld(DimensionType.OVERWORLD).getChunkProvider();
        DungeonFinder.Observer observer = new DungeonFinder.Observer() {
            @Override
            public void update(ArrayList<MobSpawnerTileEntity> spawners) {
                for (MobSpawnerTileEntity spawner : spawners) {
                    if (!spawnerList.contains(spawner))
                        spawnerList.add(spawner);
                }
                
                BlockPos playerPos = minecraft.player.getPosition();
                spawnerList.sort((o1, o2) -> {
                    Double distanceSq1 = playerPos.distanceSq(o1.getPos());
                    Double distanceSq2 = playerPos.distanceSq(o2.getPos());
                    return distanceSq1.compareTo(distanceSq2);
                });
            }
        };
        BlockPos searchOrigin = event.getPlayer().getPosition();
        int searchRadius = minecraft.gameSettings.renderDistanceChunks;
    
        dungeonFinder = new DungeonFinder(searchOrigin, searchRadius, chunkProvider);
        dungeonFinder.addObserver(observer);
    }
    
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (dungeonFinder == null || event.phase == TickEvent.Phase.START)
            return;
        
        ChunkPosition playerChunkPos = new ChunkPosition(event.player.getPosition());
        if (playerChunkPos.equals(dungeonFinder.getSearchOrigin()))
            return;
        
        dungeonFinder.setSearchOrigin(playerChunkPos);
    }
    
    @SubscribeEvent
    public static void onRenderGameOverlay(RenderGameOverlayEvent.Text event) {
        if (overlayHidden)
            return;
        
        event.getRight().add("DungeonFinder");
        
        switch (dungeonFinder.getStatus()) {
            case NEW:
                event.getRight().add("Status: Not Started");
                break;
            case IDLE:
                event.getRight().add("Status: Idle");
                break;
            case RUNNING:
                event.getRight().add("Status: Searching...");
                break;
            case SUSPENDED:
                event.getRight().add("Status: Paused");
                break;
        }
    
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < spawnerList.size(); i++)
            event.getRight().add(getSpawnerDescription(spawnerList.get(i)));
    }
    
    static void reset() {
        dungeonFinder.reset();
        spawnerList.clear();
    }
    
    static void toggleOverlay() {
        overlayHidden = !overlayHidden;
    }
    
    static void toggleThread() {
        switch (dungeonFinder.getStatus()) {
            case NEW:
                dungeonFinder.start();
                break;
            case IDLE:
            case RUNNING:
                dungeonFinder.suspendThread();
                break;
            case SUSPENDED:
                dungeonFinder.resumeThread();
                break;
        }
    }
    
    private static String getSpawnerDescription(MobSpawnerTileEntity spawner) {
        Entity spawnerEntity = spawner.getSpawnerBaseLogic().getCachedEntity();
        BlockPos spawnerPos = spawner.getPos();
        String[] entityPath = Objects.requireNonNull(spawnerEntity.getEntityString()).split(":");
        
        return entityPath[entityPath.length - 1] + " @ (" +
                spawnerPos.getX() + ", " + spawnerPos.getY() + ", " + spawnerPos.getZ() + ")";
    }
}
