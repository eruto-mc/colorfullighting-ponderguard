package net.erutobusiness.clponderguard;

import net.minecraftforge.fml.common.Mod;

@Mod(CLPonderGuard.MODID)
public class CLPonderGuard {

    public static final String MODID = "clponderguard";

    /**
     * Colorful Lighting の packed light で「まっ白・空の明かり 0」を表す値。
     *
     * <p>Colorful Lighting は明かりの int の意味そのものを変えていて、
     * {@code PackedLightData.packData(sky4, r8, g8, b8)} は
     * {@code r | (g << 8) | (sky << 16) | (b << 20) | (15 << 28)} と詰める
     * （2.7.0 の bytecode で確認）。
     *
     * <p>この値は上流自身が思案画面へ返しているもの
     * （{@code CreateCompat.colorfullighting$getLightColor} の
     * {@code packData(0, 225, 225, 225)}）と同じ。
     * 逆に解くと red=225 / green=225 / sky=0 / blue=225 / alpha=15 になる。
     *
     * <p><b>バニラの明かりの値を返してはいけない</b>。Colorful Lighting が入っている間は
     * この int の色のビットを読む側が居るので、バニラの詰め方を返すと色が化ける
     * （上流 issue #11「思案画面が赤く染まる」がその症状）。
     */
    public static final int FLAT_WHITE = 0xFE10E1E1;

    public CLPonderGuard() {
    }
}
