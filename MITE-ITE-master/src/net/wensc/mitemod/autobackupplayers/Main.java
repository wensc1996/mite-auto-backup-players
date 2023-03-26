package net.wensc.mitemod.autobackupplayers;


import javax.annotation.Nonnull;

import net.wensc.mitemod.autobackupplayers.trans.InitTrans;
import net.xiaoyu233.fml.AbstractMod;
import net.xiaoyu233.fml.classloading.Mod;
import net.xiaoyu233.fml.config.InjectionConfig;
import net.xiaoyu233.fml.config.InjectionConfig.Builder;
import org.spongepowered.asm.mixin.MixinEnvironment.Phase;
import org.spongepowered.asm.mixin.MixinEnvironment.Side;

@Mod({Side.SERVER})
public class Main extends AbstractMod {
    public Main() {
    }

    public void preInit() {
    }

    @Nonnull
    public InjectionConfig getInjectionConfig() {
        return Builder.of(this.modId(), InitTrans.class.getPackage(), Phase.INIT).build();
    }

    public String modId() {
        return "AutoBackupPlayers";
    }

    public int modVerNum() {
        return 2;
    }

    public String modVerStr() {
        return "0.0.2";
    }
}
