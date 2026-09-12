package com.sifter.entity;

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
		this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 5.0F));
		this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
	}

}
