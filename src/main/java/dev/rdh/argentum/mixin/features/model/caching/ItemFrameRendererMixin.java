package dev.rdh.argentum.mixin.features.model.caching;

import com.gtnewhorizon.gtnhlib.client.renderer.TessellatorManager;
import com.gtnewhorizons.angelica.glsm.GLStateManager;
import com.gtnewhorizons.angelica.rendering.items.BlockRenderListManager;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.rdh.argentum.impl.render.entity.instancing.EntityInstancingRenderer;
import net.minecraft.client.render.vertex.Tesselator;
import net.minecraft.client.render.entity.ItemFrameRenderer;
import net.minecraft.entity.decoration.ItemFrameEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemFrameRenderer.class)
public class ItemFrameRendererMixin {

    @WrapOperation(
            method = "render(Lnet/minecraft/entity/decoration/ItemFrameEntity;DDDFF)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/tileentity/RenderItemFrame;renderFrameItemAsBlock(Lnet/minecraft/entity/item/EntityItemFrame;)V"
            )
    )
    private void argentum$cacheFrame(ItemFrameRenderer renderer, ItemFrameEntity frame, Operation<Void> original) {
        argentum$renderCached(renderer, frame, original, frame.hangingDirection);
    }

    @WrapOperation(
            method = "render(Lnet/minecraft/entity/decoration/ItemFrameEntity;DDDFF)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/tileentity/RenderItemFrame;func_147915_b(Lnet/minecraft/entity/item/EntityItemFrame;)V"
            )
    )
    private void argentum$cacheMapFrame(ItemFrameRenderer renderer, ItemFrameEntity frame, Operation<Void> original) {Expand commentComment on line R36Resolved
        argentum$renderCached(renderer, frame, original, 4 + frame.hangingDirection);
    }

    @Unique
    private static void argentum$renderCached(ItemFrameRenderer renderer, ItemFrameEntity frame,
                                              Operation<Void> original, int keyIndex) {
        if (!EntityInstancingRenderer.recordBlock(model, brightness, red, green, blue)) {
            original.call(renderer, model, brightness, red, green, blue);
        }
        if (GLStateManager.isRecordingDisplayList() || TessellatorManager.isCurrentlyCapturing()
                || TessellatorManager.shouldInterceptDraw(Tesselator.instance)) {
            original.call(renderer, frame);
            return;
        }
        int list = BlockRenderListManager.getItemFrameDisplayList(keyIndex);
        if (list == 0) {
            list = BlockRenderListManager.startCompiling();
            original.call(renderer, frame);
            BlockRenderListManager.endItemFrameCompiling(list, keyIndex);
        }
        GLStateManager.glCallList(list);
    }
}