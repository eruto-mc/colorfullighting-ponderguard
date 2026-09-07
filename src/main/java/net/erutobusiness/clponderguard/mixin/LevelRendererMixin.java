package net.erutobusiness.clponderguard.mixin;

import net.createmod.ponder.api.level.PonderLevel;
import net.erutobusiness.clponderguard.CLPonderGuard;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 思案画面（Ponder）の中でブロックを焼くとき、Colorful Lighting に触らせない。
 *
 * <p><b>機序</b>: Ponder は場面を本物のワールドではない {@code PonderLevel} の上で描く。
 * Colorful Lighting の光エンジンは {@code LevelMixin.postInit} が
 * {@code this instanceof CLSupportingLevel} のときだけ作るもので、
 * それを実装しているのは {@code ClientLevel} だけ。だから {@code PonderLevel} には
 * エンジンが無いのに、{@code LevelRendererMixin} はそれを確かめずに
 * {@code getEngine().sampleLightColorInt(pos)} を呼び、NullPointerException になる。
 *
 * <p><b>上流にも逃がしは書いてある</b>（{@code CreateCompat} の
 * {@code level instanceof PonderLevel} → まっ白を返す）。ただしその初期化が
 * {@code ModList.isLoaded("create")} の中にあり、<b>Create を入れていないと働かない</b>。
 * Ponder は 1.20.1 では独立して配られておらず、当部では casualswing と
 * effortlessbuilding の jar の中に同梱されているだけなので、逃がしが死んでいた。
 *
 * <p><b>やること</b>: 上流と同じ値（{@link CLPonderGuard#FLAT_WHITE}）を、
 * Colorful Lighting より先に返して終わる。思案画面の中は色付き光を諦める、という
 * 上流の決めと同じ振る舞いになる。
 *
 * <p><b>{@code priority = 500}</b>: 既定は 1000。小さいほど先に当たり、
 * 差し込んだ処理が先に走る。ここで {@code setReturnValue} すると
 * その場で戻るので、Colorful Lighting 側の処理は走らない。
 *
 * <p>⚠ 対象は {@code BlockAndTintGetter} を取る3引数のほう。2引数の同名メソッドと
 * 取り違えないよう、記述子まで書いて指す。
 */
@Mixin(value = LevelRenderer.class, priority = 500)
public class LevelRendererMixin {

    @Inject(method = "getLightColor(Lnet/minecraft/world/level/BlockAndTintGetter;"
                     + "Lnet/minecraft/world/level/block/state/BlockState;"
                     + "Lnet/minecraft/core/BlockPos;)I",
            at = @At("HEAD"), cancellable = true)
    private static void clponderguard$flatLightInPonder(BlockAndTintGetter level,
                                                        BlockState state,
                                                        BlockPos pos,
                                                        CallbackInfoReturnable<Integer> cir) {
        if (level instanceof PonderLevel) {
            cir.setReturnValue(CLPonderGuard.FLAT_WHITE);
        }
    }
}
