package com.sifter.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class SifterEntity extends PathfinderMob {
    
    // The constructor: code that runs once, the moment a new Sifter is
	// created. Minecraft calls this itself - we never call "new
	// SifterEntity(...)" directly.
    public SifterEntity(EntityType<? extends SifterEntity> type, Level level) {
		super(type, level);
	}

    // Whether this Sifter has built its home yet. Lives on the entity (not
	// the goal) because goals are recreated on load, but the entity's saved
	// data survives.
	private boolean homeBuilt;

	public boolean hasBuiltHome() {
		return homeBuilt;
	}

	public void setHomeBuilt(boolean homeBuilt) {
		this.homeBuilt = homeBuilt;
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putBoolean("HomeBuilt", homeBuilt);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		homeBuilt = tag.getBoolean("HomeBuilt");
	}

    // Every mob type must declare its starting stats. 20 health = 10 hearts,
	// same as a player. 0.25 movement speed matches a pig.
	public static AttributeSupplier.Builder createAttributes() {
		return createMobAttributes()
			.add(Attributes.MAX_HEALTH, 20.0)
			.add(Attributes.MOVEMENT_SPEED, 0.35);
	}

    // Called once when the Sifter spawns. We give it a short list of
	// "goals" - behaviours it's allowed to choose between. Lower priority
	// numbers run first.
	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(0, new BuildHomeGoal(this));
		this.goalSelector.addGoal(1, new MineSculkGoal(this, 1.0, 30));
		this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 5.0F));
		this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
	}

}
