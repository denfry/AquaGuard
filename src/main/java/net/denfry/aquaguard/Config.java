package net.denfry.aquaguard;

import net.minecraftforge.common.ForgeConfigSpec;

public class Config {

    public static final ForgeConfigSpec SPEC;

    public static ForgeConfigSpec.BooleanValue LOG_BLOCK_PLACE;
    public static ForgeConfigSpec.BooleanValue LOG_BLOCK_BREAK;
    public static ForgeConfigSpec.BooleanValue LOG_CONTAINER_ACCESS;
    public static ForgeConfigSpec.BooleanValue LOG_CONTAINER_MODIFY;
    public static ForgeConfigSpec.BooleanValue LOG_ENTITY_KILL;
    public static ForgeConfigSpec.BooleanValue LOG_PLAYER_DEATH;
    public static ForgeConfigSpec.BooleanValue LOG_ITEM_PICKUP;
    public static ForgeConfigSpec.BooleanValue LOG_ITEM_DROP;
    public static ForgeConfigSpec.BooleanValue LOG_EXPLOSION;
    public static ForgeConfigSpec.BooleanValue LOG_FLUID_PLACE;
    public static ForgeConfigSpec.BooleanValue LOG_PLAYER_LOGIN;
    public static ForgeConfigSpec.BooleanValue LOG_PLAYER_LOGOUT;
    public static ForgeConfigSpec.BooleanValue LOG_PLAYER_CHAT;
    public static ForgeConfigSpec.BooleanValue LOG_PLAYER_COMMAND;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("AquaGuard Logging Settings").push("logging");

        LOG_BLOCK_PLACE = builder
                .comment("Log when players place blocks")
                .define("log_block_place", true);

        LOG_BLOCK_BREAK = builder
                .comment("Log when players break blocks")
                .define("log_block_break", true);

        LOG_CONTAINER_ACCESS = builder
                .comment("Log when players open containers (chests, barrels, etc.)")
                .define("log_container_access", true);

        LOG_CONTAINER_MODIFY = builder
                .comment("Log when players modify containers (crafting, smelting, etc.)")
                .define("log_container_modify", true);

        LOG_ENTITY_KILL = builder
                .comment("Log when players kill entities or mobs")
                .define("log_entity_kill", true);

        LOG_PLAYER_DEATH = builder
                .comment("Log when a player dies")
                .define("log_player_death", true);

        LOG_ITEM_PICKUP = builder
                .comment("Log when players pick up items")
                .define("log_item_pickup", true);

        LOG_ITEM_DROP = builder
                .comment("Log when players drop items")
                .define("log_item_drop", true);

        LOG_EXPLOSION = builder
                .comment("Log when explosions destroy blocks")
                .define("log_explosion", true);

        LOG_FLUID_PLACE = builder
                .comment("Log when fluids are placed or removed (buckets)")
                .define("log_fluid_place", true);

        LOG_PLAYER_LOGIN = builder
                .comment("Log player logins")
                .define("log_player_login", true);

        LOG_PLAYER_LOGOUT = builder
                .comment("Log player logouts")
                .define("log_player_logout", true);

        LOG_PLAYER_CHAT = builder
                .comment("Log player chat messages")
                .define("log_player_chat", true);

        LOG_PLAYER_COMMAND = builder
                .comment("Log player commands")
                .define("log_player_command", true);

        builder.pop();
        SPEC = builder.build();
    }
}