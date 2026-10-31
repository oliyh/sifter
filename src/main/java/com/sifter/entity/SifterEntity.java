package com.sifter.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.npc.Npc;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;

public class SifterEntity extends PathfinderMob implements Npc, Merchant {
    
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
	// Merchant requires us to keep track of these two bits of state
	// ourselves - who's currently trading with us, and our trade list.
	private Player tradingPlayer;
	private final MerchantOffers offers = new MerchantOffers();
	private int villagerXp;

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

	// Right-click handling: if a player (not a spectator) right-clicks us,
	// open the trading screen instead of the usual "attack" interaction.
	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (!this.level().isClientSide && !player.isSpectator()) {
			this.setTradingPlayer(player);
			this.openTradingScreen(player, this.getDisplayName(), 1);
		}
		return InteractionResult.sidedSuccess(this.level().isClientSide);
	}

		// --- Merchant interface: the bits Minecraft needs to run a trade ---

	@Override
	public void setTradingPlayer(Player player) {
		this.tradingPlayer = player;
	}

	@Override
	public Player getTradingPlayer() {
		return this.tradingPlayer;
	}

	@Override
	public MerchantOffers getOffers() {
		if (this.offers.isEmpty()) {
			// TradePricing decides *what* the trades are (tested in
			// TradePricingTest). Here we turn that plain data into the
			// real Items a trade needs - which only works with the game
			// actually running.
			this.offers.add(new MerchantOffer(
				new ItemCost(Items.EMERALD, TradePricing.OFFERS.get(0).emeraldCost()),
				new ItemStack(Items.SCULK),
				TradePricing.OFFERS.get(0).maxUses(),
				5,
				0.05F));
			this.offers.add(new MerchantOffer(
				new ItemCost(Items.EMERALD, TradePricing.OFFERS.get(1).emeraldCost()),
				new ItemStack(Items.SCULK_CATALYST),
				TradePricing.OFFERS.get(1).maxUses(),
				10,
				0.05F));
			this.offers.add(new MerchantOffer(
				new ItemCost(Items.EMERALD, TradePricing.OFFERS.get(2).emeraldCost()),
				new ItemStack(Items.SCULK_VEIN),
				TradePricing.OFFERS.get(2).maxUses(),
				15,
				0.05F));
		}
		return this.offers;
	}

	@Override
	public void overrideOffers(MerchantOffers offers) {
		this.offers.clear();
		this.offers.addAll(offers);
	}

	@Override
	public void notifyTrade(MerchantOffer offer) {
		offer.increaseUses();
		this.playSound(this.getNotifyTradeSound(), 1.0F, 1.0F);
	}

	@Override
	public void notifyTradeUpdated(ItemStack stack) {
		// Nothing extra to do when the player is just holding an item up
		// to a trade slot - only a completed trade (notifyTrade) matters
		// to us.
	}

	@Override
	public int getVillagerXp() {
		return this.villagerXp;
	}

	@Override
	public void overrideXp(int xp) {
		this.villagerXp = xp;
	}

	@Override
	public boolean showProgressBar() {
		return true;
	}

	@Override
	public SoundEvent getNotifyTradeSound() {
		return SoundEvents.SPIDER_AMBIENT;
	}

	@Override
	public boolean isClientSide() {
		return this.level().isClientSide();
	}

}
