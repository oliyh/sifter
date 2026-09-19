package com.sifter.entity;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import com.sifter.Sifter;

// A registry is Minecraft's phone book of every entity/item/block type
// that exists, keyed by name. Something has to be registered here before
// it can ever spawn.
public class ModEntities {

	public static final EntityType<SifterEntity> SIFTER = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		ResourceLocation.fromNamespaceAndPath(Sifter.MOD_ID, "sifter"),
		EntityType.Builder.of(SifterEntity::new, MobCategory.CREATURE)
			.sized(0.8F, 1.7F)
			.build("sifter")
	);
}