package com.storm.util;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;

public class Keybinds {

    public static KeyMapping debugKey;
    public static KeyMapping stopKey;

    public static void register() {
        debugKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.stormclient.debug", InputConstants.Type.KEYBOARD, InputConstants.KEY_O, KeyMapping.Category.MISC));
        stopKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.stormclient.stop", InputConstants.Type.KEYBOARD, InputConstants.KEY_K, KeyMapping.Category.MOVEMENT));


        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (debugKey.consumeClick()) {
                PathDebug.enabled = !PathDebug.enabled;
                if (mc.player != null) {
                    mc.player.sendOverlayMessage(Component.literal("Path debug " + (PathDebug.enabled ? "on" : "off")));
                }
            }
            while (stopKey.consumeClick()) {
                PathFollower.enabled = false;
                PathFollower.stop();
            }
        });


    }
}
