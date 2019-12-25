package com.masonschleider.dungeonfinder;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.event.InputEvent.KeyInputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModEventHandler {
    private static final KeyBinding reset =
            new KeyBinding("key." + Main.MODID + ".reset", GLFW.GLFW_KEY_LEFT_BRACKET, "key.categories." + Main.MODID);
    private static final KeyBinding toggleOverlay =
            new KeyBinding("key." + Main.MODID + ".toggle_overlay", GLFW.GLFW_KEY_RIGHT_BRACKET, "key.categories." + Main.MODID);
    private static final KeyBinding toggleThread =
            new KeyBinding("key." + Main.MODID + ".toggle_thread", GLFW.GLFW_KEY_BACKSLASH, "key.categories." + Main.MODID);
    
    @SubscribeEvent
    public static void onKeyInput(KeyInputEvent event) {
        if (reset.isPressed())
            Main.reset();
        if (toggleOverlay.isPressed())
            Main.toggleOverlay();
        else if (toggleThread.isPressed())
            Main.toggleThread();
    }
    
    static void registerKeyBindings() {
        ClientRegistry.registerKeyBinding(reset);
        ClientRegistry.registerKeyBinding(toggleOverlay);
        ClientRegistry.registerKeyBinding(toggleThread);
    }
}
