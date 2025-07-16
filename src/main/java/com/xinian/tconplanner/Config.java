package com.xinian.tconplanner;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class Config {

    public static final Config CONFIG;
    public static final ForgeConfigSpec SPEC;

    public final ForgeConfigSpec.IntValue buttonX;
    public final ForgeConfigSpec.IntValue buttonY;

    public final ForgeConfigSpec.IntValue importButtonXAnvil;
    public final ForgeConfigSpec.IntValue importButtonYAnvil;

    public final ForgeConfigSpec.IntValue importButtonXStation;
    public final ForgeConfigSpec.IntValue importButtonYStation;
    public final ForgeConfigSpec.EnumValue<ScrollDirectionEnum> scrollDirection;

    public Config(ForgeConfigSpec.Builder builder){
        builder.push("UI Button");
        buttonX = builder.comment("X position of the \"Open Planner\" button. Default: 116").defineInRange("X Position", 116, -300, 300);
        buttonY = builder.comment("Y position of the \"Open Planner\" button. Default: 18").defineInRange("Y Position", 18, -300, 300);
        importButtonXAnvil = builder.comment("X position of the \"Import Tool\" button in the Tinker's Anvil. Default: 34").defineInRange("Import Tool X Position Anvil", 34, -300, 300);
        importButtonYAnvil = builder.comment("Y position of the \"Import Tool\" button in the Tinker's Anvil. Default: 60").defineInRange("Import Tool Y Position Anvil", 60, -300, 300);
        importButtonXStation = builder.comment("X position of the \"Import Tool\" button in the Tinker's Station. Default: 55").defineInRange("Import Tool X Position Station", 55, -300, 300);
        importButtonYStation = builder.comment("Y position of the \"Import Tool\" button in the Tinker's Station. Default: 60").defineInRange("Import Tool Y Position Station", 60, -300, 300);
        builder.pop();
        builder.push("Planner");
        scrollDirection = builder.comment("The scroll direction for paginated lists").defineEnum("Scroll Direction", ScrollDirectionEnum.DOWN);
        builder.pop();
    }

    static {
        Pair<Config, ForgeConfigSpec> commonSpecPair = new ForgeConfigSpec.Builder().configure(Config::new);
        CONFIG = commonSpecPair.getLeft();
        SPEC = commonSpecPair.getRight();
    }

    public enum ScrollDirectionEnum{
        UP(1), DOWN(-1);

        public final int mult;

        ScrollDirectionEnum(int mult){
            this.mult = mult;
        }
    }
}
