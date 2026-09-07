package net.erutobusiness.clponderguard.mixin;

import net.createmod.ponder.api.level.PonderLevel;
import net.erutobusiness.clponderguard.CLPonderGuard;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 思案画面（Ponder）の中で実体を描くとき、Colorful Lighting に触らせない。
 *
 * <p>ブロック側（{@link LevelRendererMixin}）と同じ機序。⚠ <b>ただしこちらは上流に
 * 逃がしが1つも書かれていない</b>——{@code CreateCompat} が持っているのは
 * {@code LevelRenderer.getLightColor} の分岐だけで、
 * {@code EntityRenderer.getPackedLightCoords} には {@code PonderLevel} の判定が無い。
 * だから <b>Create を入れている人にも当たる</b>（上流
 * https://github.com/Camawama/colorful-lighting-sodium/issues/47 のコメント
 * 「Create crashes when using ponder really often with this mod」）。
 *
 * <p>実測（2026-09-07・当部の構成）: ブロック側だけ直すと、場面に実体が出た瞬間に
 * {@code PonderLevel.renderEntity} → {@code sampleTrilinearLightColor} で
 * 同じ NullPointerException が出る。
 */
@Mixin(value = EntityRenderer.class, priority = 500)
public class EntityRendererMixin {

    @Inject(method = "getPackedLightCoords", at = @At("HEAD"), cancellable = true)
    private void clponderguard$flatLightInPonder(Entity entity,
                                                 float partialTick,
                                                 CallbackInfoReturnable<Integer> cir) {
        if (entity.level() instanceof PonderLevel && CLPonderGuard.colorfulLightingLoaded()) {
            cir.setReturnValue(CLPonderGuard.FLAT_WHITE);
        }
    }
}
