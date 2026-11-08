package com.sifter.item;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;

import com.sifter.Sifter;
import com.sifter.entity.ModEntities;

// Items have a registry of their own, separate from the entity registry
// in ModEntities. Same idea: nothing exists until it's registered.
public class ModItems {

	// Colours are written as 0xRRGGBB - two hex digits each of red, green
	// and blue. The egg is painted in two colours: a background and the
	// spots on top of it.
	public static final Item SIFTER_SPAWN_EGG = Registry.register(
		BuiltInRegistries.ITEM,
		ResourceLocation.fromNamespaceAndPath(Sifter.MOD_ID, "sifter_spawn_egg"),
		new SpawnEggItem(ModEntities.SIFTER, 0xFFFF00, 0x000000, new Item.Properties())
	);

	public static void register() {
		// An event again (like Phase 0.5): when Minecraft builds the
		// "Spawn Eggs" creative tab, it calls this so we can add ours.
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.SPAWN_EGGS)
			.register(entries -> entries.accept(SIFTER_SPAWN_EGG));
	}
}