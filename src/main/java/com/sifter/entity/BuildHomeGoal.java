package com.sifter.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BedPart;

// Builds the Sifter's home, once, the moment this goal first gets a
// chance to run. Turns the plain coordinates from HomeLayout into real
// blocks - this is the "impure" half that touches the live world, so
// unlike HomeLayout itself, we check this one by playing, not by testing.
public class BuildHomeGoal extends Goal {

	private final SifterEntity sifter;

	public BuildHomeGoal(SifterEntity sifter) {
		this.sifter = sifter;
	}

	@Override
	public boolean canUse() {
		return !sifter.hasBuiltHome();
	}

	@Override
	public void start() {
		BlockPos anchor = sifter.blockPosition();
		Level level = sifter.level();

		level.setBlockAndUpdate(anchor.offset(HomeLayout.BED_FOOT),
			Blocks.BLUE_BED.defaultBlockState()
				.setValue(BedBlock.FACING, Direction.SOUTH)
				.setValue(BedBlock.PART, BedPart.FOOT));

		level.setBlockAndUpdate(anchor.offset(HomeLayout.BED_HEAD),
			Blocks.BLUE_BED.defaultBlockState()
				.setValue(BedBlock.FACING, Direction.SOUTH)
				.setValue(BedBlock.PART, BedPart.HEAD));

		level.setBlockAndUpdate(anchor.offset(HomeLayout.CRAFTING_TABLE), Blocks.CRAFTING_TABLE.defaultBlockState());
		level.setBlockAndUpdate(anchor.offset(HomeLayout.FURNACE), Blocks.FURNACE.defaultBlockState());
		level.setBlockAndUpdate(anchor.offset(HomeLayout.POTTED_SAPLING), Blocks.POTTED_SPRUCE_SAPLING.defaultBlockState());

		sifter.setHomeBuilt(true);
	}
}