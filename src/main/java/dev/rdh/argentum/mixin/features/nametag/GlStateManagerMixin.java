package dev.rdh.argentum.mixin.features.nametag;

import dev.rdh.argentum.impl.render.entity.NameTagBatch;
import net.minecraft.client.render.platform.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.FloatBuffer;

@Mixin(GlStateManager.class)
public abstract class GlStateManagerMixin {
    @Inject(method = "matrixMode", at = @At("HEAD"))
    private static void argentum$trackNameTagMatrixMode(int mode, CallbackInfo ci) {
        NameTagBatch batch = NameTagBatch.capturing();
        if (batch != null) batch.matrixMode(mode);
    }

    @Inject(method = "pushMatrix", at = @At("HEAD"))
    private static void argentum$pushNameTagMatrix(CallbackInfo ci) {
        NameTagBatch batch = NameTagBatch.capturing();
        if (batch != null) batch.pushMatrix();
    }

    @Inject(method = "popMatrix", at = @At("HEAD"))
    private static void argentum$popNameTagMatrix(CallbackInfo ci) {
        NameTagBatch batch = NameTagBatch.capturing();
        if (batch != null) batch.popMatrix();
    }

    @Inject(method = "loadIdentity", at = @At("HEAD"))
    private static void argentum$loadNameTagIdentity(CallbackInfo ci) {
        NameTagBatch batch = NameTagBatch.capturing();
        if (batch != null) batch.loadIdentity();
    }

    @Inject(method = "translatef", at = @At("HEAD"))
    private static void argentum$translateNameTag(float x, float y, float z, CallbackInfo ci) {
        NameTagBatch batch = NameTagBatch.capturing();
        if (batch != null) batch.translate(x, y, z);
    }

    @Inject(method = "translated", at = @At("HEAD"))
    private static void argentum$translateNameTag(double x, double y, double z, CallbackInfo ci) {
        NameTagBatch batch = NameTagBatch.capturing();
        if (batch != null) batch.translate((float)x, (float)y, (float)z);
    }

    @Inject(method = "rotatef", at = @At("HEAD"))
    private static void argentum$rotateNameTag(float angle, float x, float y, float z, CallbackInfo ci) {
        NameTagBatch batch = NameTagBatch.capturing();
        if (batch != null) batch.rotate(angle, x, y, z);
    }

    @Inject(method = "scalef", at = @At("HEAD"))
    private static void argentum$scaleNameTag(float x, float y, float z, CallbackInfo ci) {
        NameTagBatch batch = NameTagBatch.capturing();
        if (batch != null) batch.scale(x, y, z);
    }

    @Inject(method = "scaled", at = @At("HEAD"))
    private static void argentum$scaleNameTag(double x, double y, double z, CallbackInfo ci) {
        NameTagBatch batch = NameTagBatch.capturing();
        if (batch != null) batch.scale((float)x, (float)y, (float)z);
    }

    @Inject(method = "multMatrix", at = @At("HEAD"))
    private static void argentum$multiplyNameTag(FloatBuffer matrix, CallbackInfo ci) {
        NameTagBatch batch = NameTagBatch.capturing();
        if (batch != null) batch.multiply(matrix);
    }
}
