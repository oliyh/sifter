package com.sifter.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

// This class talks directly to the live world (level, blockPos), so
// unlike SkulkFinder it isn't something we unit test - we check it by
// playing.
public class MineSculkGoal extends MoveToBlockGoal {

	// Ticks (1/20th of a second each) spent standing next to the block
	// before it breaks - a slower dig than a player with a pickaxe.
	private static final int MINE_TICKS = 60;

	private int mining;
	private SifterEntity sifter;

	public MineSculkGoal(SifterEntity sifter, double speedModifier, int searchRange) {
		super(sifter, speedModifier, searchRange);
		this.sifter = sifter;
	}

	@Override
	protected boolean isValidTarget(LevelReader level, BlockPos pos) {
		BlockState currentBlockState = level.getBlockState(pos);
		return currentBlockState.is(Blocks.SCULK) || currentBlockState.is(Blocks.SCULK_VEIN) || currentBlockState.is(Blocks.SCULK_CATALYST);
	}

	@Override
	protected int nextStartTick(PathfinderMob pathfinderMob) {
      return 30;
   	}

	// The goal normally walks the mob to the block above the target, which
	// suits full blocks. A vein has no collision, so the sifter ends up
	// standing inside the vein's own cell - aim for that instead.
	@Override
	protected BlockPos getMoveToTarget() {
		if (mob.level().getBlockState(blockPos).is(Blocks.SCULK_VEIN)) {
			return blockPos;
		}
		return super.getMoveToTarget();
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
			this.sifter.blockMined(mob.level().getBlockState(blockPos));
			mob.level().destroyBlock(blockPos, false, mob);
			mining = 0;
		}
	}
}