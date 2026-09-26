package com.sifter.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

import com.sifter.Sifter;
import com.sifter.entity.SifterAnimation;
import com.sifter.entity.SifterEntity;

// Two stacked boxes, each its own ModelPart so they can move on their
// own. Phase 3 reshapes these into the Sifter's real design - the two
// parts, and the animation below, won't need to change when it does.
public class SifterModel extends EntityModel<SifterEntity> {

	// Every custom model needs a unique name to register itself under -
	// this is that name.
	public static final ModelLayerLocation LAYER =
		new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Sifter.MOD_ID, "sifter"), "main");

	private final ModelPart bottom;
	private final ModelPart top;

	public SifterModel(ModelPart root) {
		this.bottom = root.getChild("bottom");
		this.top = root.getChild("top");
	}

	// Describes the shape once, up front: two boxes, 16 units per side
	// (one Minecraft block each), stacked directly on top of each other.
	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		root.addOrReplaceChild("bottom",
			CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -16.0F, -2.4F, 16.0F, 16.0F, 4.8F),
			PartPose.offset(0.0F, 24.0F, 0.0F));

		root.addOrReplaceChild("top",
			CubeListBuilder.create().texOffs(0, 32).addBox(-6.0F, -12.0F, -2.4F, 16.0F, 16.0F, 4.8F),
			PartPose.offset(0.0F, 10.0F, 0.0F));

		return LayerDefinition.create(mesh, 64, 64);
	}

	// Minecraft calls this itself, every frame, before drawing - we don't
	// have to hook into tick() or the renderer to make this run. ageInTicks
	// is "how far through time are we," the same idea as tickCount +
	// partialTicks from the first draft, just handed to us directly.
	//
	// Both blocks share a period and amplitude - only the phase differs -
	// so the top always follows the same motion as the bottom, just a
	// short delay behind it. That delay is what makes it read as one wave
	// travelling up the body, instead of two blocks wobbling at random.
	@Override
	public void setupAnim(SifterEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
			float netHeadYaw, float headPitch) {
		float period = 20.0F;
		float amplitude = 1.25F;
		float phaseGap = period / 4.0F;

		bottom.x = SifterAnimation.wobbleOffset(ageInTicks, period, amplitude, 0.0F);
		top.x = SifterAnimation.wobbleOffset(ageInTicks, period, amplitude * 2.5F, phaseGap);
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay,
			int color) {
		bottom.render(poseStack, buffer, packedLight, packedOverlay, color);
		top.render(poseStack, buffer, packedLight, packedOverlay, color);
	}
}