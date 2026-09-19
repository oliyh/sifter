package com.sifter;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import net.minecraft.network.chat.Component;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

import com.sifter.entity.ModEntities;
import com.sifter.entity.SifterEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// This is the "entrypoint" of our mod - the very first bit of our own code
// that Minecraft runs when it loads. Fabric knows to run it because we told
// it to in fabric.mod.json ("entrypoints" -> "main").
public class Sifter implements ModInitializer {

	public static final String MOD_ID = "sifter";

	// A logger writes lines to the game's console/log file. It's the
	// simplest way to prove our code actually ran.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Sifter mod loaded!");

		// Tell Minecraft the Sifter exists, and what its starting stats
		// are. Referencing ModEntities.SIFTER here is also what triggers
		// its registration (see ModEntities.java).
		FabricDefaultAttributeRegistry.register(ModEntities.SIFTER, SifterEntity.createAttributes());

		// Phase 0.5: register a callback. We don't call this code ourselves -
		// Minecraft calls it automatically, every time a player finishes
		// joining a world.
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			handler.player.sendSystemMessage(Component.literal("Hello from the Sifters!"));
			int b = 8;
			handler.player.sendSystemMessage(Component.literal(String.format("b = %s", b)));
		});
	}
}
