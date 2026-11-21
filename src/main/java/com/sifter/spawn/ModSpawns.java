package com.sifter.spawn;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biomes;

import com.sifter.entity.ModEntities;

// Where, and how often, Sifters appear on their own.
public class ModSpawns {

	// Ancient cities only ever generate inside the Deep Dark biome, so
	// "spawn in the Deep Dark" is the same as "spawn in ancient cities".
	// A weight of 10 is a fairly rare spawn; most animals use 8-12.
	public static final SpawnRule ANCIENT_CITY = new SpawnRule(10, 3, 7);

	public static void register() {
		BiomeModifications.addSpawn(
			BiomeSelectors.includeByKey(Biomes.DEEP_DARK),
			MobCategory.CREATURE,
			ModEntities.SIFTER,
			ANCIENT_CITY.weight(),
			ANCIENT_CITY.minGroupSize(),
			ANCIENT_CITY.maxGroupSize());
	}
}