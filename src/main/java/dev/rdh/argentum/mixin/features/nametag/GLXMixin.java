package dev.rdh.argentum.mixin.features.nametag;

import dev.rdh.argentum.impl.render.entity.NameTagBatch;
import net.minecraft.client.render.platform.GLX;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GLX.class)
public abstract class GLXMixin {
    @Inject(method = "multiTexCoord2f", at = @At("HEAD"))
    private static void argentum$trackNameTagLight(int unit, float u, float v, CallbackInfo ci) {
        if (unit != GLX.GL_TEXTURE1) return;
        NameTagBatch batch = NameTagBatch.active();
        if (batch != null) batch.setLight(u, v);
    }
}
