package de.tecca.enderborne;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.EndermanEntityRenderer;

public class EnderborneClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Register entity renderer for Trading Enderman - uses vanilla Enderman renderer
        EntityRendererRegistry.register(Enderborne.TRADING_ENDERMAN, EndermanEntityRenderer::new);

        Enderborne.LOGGER.info("Enderborne client initialized - Using vanilla Enderman renderer");
    }
}