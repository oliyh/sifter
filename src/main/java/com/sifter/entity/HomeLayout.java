package com.sifter.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.List;

// The shape of a Sifter's home, as plain coordinates relative to one
// corner (the foot of the bed) - no blocks, no world, just geometry.
// That's what lets us test the *layout* without starting the game, even
// though building it for real (BuildHomeGoal) very much needs the game.
public class HomeLayout {

	private HomeLayout() {
	}

	public static final BlockPos BED_FOOT = BlockPos.ZERO;
	public static final BlockPos BED_HEAD = BED_FOOT.relative(Direction.SOUTH);
	public static final BlockPos CRAFTING_TABLE = BED_FOOT.relative(Direction.EAST);
	public static final BlockPos FURNACE = CRAFTING_TABLE.relative(Direction.NORTH);
	public static final BlockPos POTTED_SAPLING = FURNACE.above();

	public static List<BlockPos> allOffsets() {
		return List.of(BED_FOOT, BED_HEAD, CRAFTING_TABLE, FURNACE, POTTED_SAPLING);
	}
}