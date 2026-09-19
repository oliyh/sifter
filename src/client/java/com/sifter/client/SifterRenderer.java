package com.sifter.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.PigModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

import com.sifter.entity.SifterEntity;

// Placeholder look for Phase 1: borrow the vanilla pig's shape, but tint
// every pixel yellow. We'll design the Sifter's real look in Phase 3.
public class SifterRenderer extends MobRenderer<SifterEntity, SifterModel> {

	private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/pig/pig.png");

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
		RenderSystem.setShaderColor(1.0F, 0.85F, 0.0F, 1.0F);
		super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
	}
}