package com.sifter.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;

// This class talks directly to the live world (level, blockPos), so
// unlike SkulkFinder it isn't something we unit test - we check it by
// playing.
public class MineSculkGoal extends MoveToBlockGoal {

	// Ticks (1/20th of a second each) spent standing next to the block
	// before it breaks - a slower dig than a player with a pickaxe.
	private static final int MINE_TICKS = 60;

	private int mining;

	public MineSculkGoal(SifterEntity sifter, double speedModifier, int searchRange) {
		super(sifter, speedModifier, searchRange);
	}

	@Override
	protected boolean isValidTarget(LevelReader level, BlockPos pos) {
		return level.getBlockState(pos).is(Blocks.SCULK);
	}

	@Override
	protected int nextStartTick(PathfinderMob pathfinderMob) {
      return 30;
   	}

	@Override
	public void tick() {
		super.tick();

		if (!isReachedTarget()) {
			mining = 0;
			return;
		}

		mining++;
		if (mining >= MINE_TICKS) {
			mob.level().removeBlock(blockPos, false);
			mining = 0;
		}
	}
}