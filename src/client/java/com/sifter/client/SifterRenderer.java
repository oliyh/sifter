package com.sifter.client;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

import com.sifter.entity.SifterEntity;
import com.sifter.Sifter;

// Placeholder look for Phase 1: borrow the vanilla pig's shape, but tint
// every pixel yellow. We'll design the Sifter's real look in Phase 3.
public class SifterRenderer extends MobRenderer<SifterEntity, SifterModel> {

	private static final ResourceLocation TEXTURE =
		ResourceLocation.fromNamespaceAndPath(Sifter.MOD_ID, "textures/entity/sifter.png");

	public SifterRenderer(EntityRendererProvider.Context context) {
		super(context, new SifterModel(context.bakeLayer(SifterModel.LAYER)), 0.5F);
	}

	@Override
	public ResourceLocation getTextureLocation(SifterEntity entity) {
		return TEXTURE;
	}

	@Override
	public void render(SifterEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
			MultiBufferSource buffer, int packedLight) {
		// Multiply every pixel we draw by this colour before drawing it.
		// (1, 1, 1) would mean "no change"; we push red and green down and
		// leave blue at 0, which turns the pig's pinks and browns yellow.
		super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
	}
}