package com.sifter.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;

import com.sifter.entity.ModEntities;

// The client-side counterpart to Sifter.java. This only runs on clients
// (including the client half of singleplayer), never on a dedicated
// server - which is why rendering code lives here instead of there.
public class SifterClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		EntityModelLayerRegistry.registerModelLayer(SifterModel.LAYER, SifterModel::createBodyLayer);
		EntityRendererRegistry.register(ModEntities.SIFTER, SifterRenderer::new);
	}
}
