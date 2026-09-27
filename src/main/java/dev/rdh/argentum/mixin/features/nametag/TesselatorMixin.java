package dev.rdh.argentum.mixin.features.nametag;

import dev.rdh.argentum.impl.render.entity.NameTagBatch;
import net.minecraft.client.render.vertex.BufferBuilder;
import net.minecraft.client.render.vertex.Tesselator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Tesselator.class)
public abstract class TesselatorMixin {
    @Shadow
    private BufferBuilder bufferBuilder;

    @Inject(method = "end", at = @At("HEAD"), cancellable = true)
    private void argentum$captureNameTagQuads(CallbackInfo ci) {
        NameTagBatch batch = NameTagBatch.capturing();
        if (batch != null && batch.capture(this.bufferBuilder)) {
            ci.cancel();
        }
    }
}
