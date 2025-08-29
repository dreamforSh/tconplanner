package com.xinian.tconplanner;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkConstants;
import com.xinian.tconplanner.data.PlannerData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

@Mod(TConPlanner.MODID)
public class TConPlanner {
    public static final Logger LOGGER = LogManager.getLogger();
    public static final String MODID = "tconplanner";

    public static PlannerData DATA;

    public TConPlanner() {
        ModLoadingContext mlctx = ModLoadingContext.get();
        mlctx.registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (a, b) -> true));
        mlctx.registerConfig(ModConfig.Type.CLIENT, Config.SPEC);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setupClient);
    }

    private void setupClient(final FMLClientSetupEvent event) {
        File gameDir = Minecraft.getInstance().gameDirectory;
        File folder = new File(gameDir, MODID);
        DATA = new PlannerData(folder);
    }


}
