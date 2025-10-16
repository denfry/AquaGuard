package net.denfry.aquaguard.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.denfry.aquaguard.AquaGuard;
import net.denfry.aquaguard.Config;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class StatusCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ag")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("status")
                        .executes(StatusCommand::showStatus))
        );
    }

    private static int showStatus(CommandContext<CommandSourceStack> ctx) {
        long logCount = AquaGuard.getDatabaseManager().getLogCount();
        long fileSize = AquaGuard.getDatabaseManager().getFileSize();

        ctx.getSource().sendSuccess(() ->
                Component.literal("§6===== AquaGuard Status ====="), false);
        ctx.getSource().sendSuccess(() ->
                Component.literal("§eTotal Logs: §f" + logCount), false);
        ctx.getSource().sendSuccess(() ->
                Component.literal("§eLog File Size: §f" + formatFileSize(fileSize)), false);
        ctx.getSource().sendSuccess(() ->
                Component.literal("§eConfig Options:"), false);
        ctx.getSource().sendSuccess(() ->
                Component.literal("§7- Block Place Logging: §f" + (Config.LOG_BLOCK_PLACE.get() ? "Enabled" : "Disabled")), false);
        ctx.getSource().sendSuccess(() ->
                Component.literal("§7- Block Break Logging: §f" + (Config.LOG_BLOCK_BREAK.get() ? "Enabled" : "Disabled")), false);
        ctx.getSource().sendSuccess(() ->
                Component.literal("§7- Container Access Logging: §f" + (Config.LOG_CONTAINER_ACCESS.get() ? "Enabled" : "Disabled")), false);
        ctx.getSource().sendSuccess(() ->
                Component.literal("§7- Container Modify Logging: §f" + (Config.LOG_CONTAINER_MODIFY.get() ? "Enabled" : "Disabled")), false);
        ctx.getSource().sendSuccess(() ->
                Component.literal("§7- Entity Kill Logging: §f" + (Config.LOG_ENTITY_KILL.get() ? "Enabled" : "Disabled")), false);
        ctx.getSource().sendSuccess(() ->
                Component.literal("§7- Player Death Logging: §f" + (Config.LOG_PLAYER_DEATH.get() ? "Enabled" : "Disabled")), false);
        ctx.getSource().sendSuccess(() ->
                Component.literal("§7- Item Pickup Logging: §f" + (Config.LOG_ITEM_PICKUP.get() ? "Enabled" : "Disabled")), false);
        ctx.getSource().sendSuccess(() ->
                Component.literal("§7- Item Drop Logging: §f" + (Config.LOG_ITEM_DROP.get() ? "Enabled" : "Disabled")), false);
        ctx.getSource().sendSuccess(() ->
                Component.literal("§7- Explosion Logging: §f" + (Config.LOG_EXPLOSION.get() ? "Enabled" : "Disabled")), false);
        ctx.getSource().sendSuccess(() ->
                Component.literal("§7- Fluid Place Logging: §f" + (Config.LOG_FLUID_PLACE.get() ? "Enabled" : "Disabled")), false);
        ctx.getSource().sendSuccess(() ->
                Component.literal("§7- Player Login Logging: §f" + (Config.LOG_PLAYER_LOGIN.get() ? "Enabled" : "Disabled")), false);
        ctx.getSource().sendSuccess(() ->
                Component.literal("§7- Player Logout Logging: §f" + (Config.LOG_PLAYER_LOGOUT.get() ? "Enabled" : "Disabled")), false);
        ctx.getSource().sendSuccess(() ->
                Component.literal("§7- Player Chat Logging: §f" + (Config.LOG_PLAYER_CHAT.get() ? "Enabled" : "Disabled")), false);
        ctx.getSource().sendSuccess(() ->
                Component.literal("§7- Player Command Logging: §f" + (Config.LOG_PLAYER_COMMAND.get() ? "Enabled" : "Disabled")), false);
        ctx.getSource().sendSuccess(() ->
                Component.literal("§6========================"), false);

        return 1;
    }

    private static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = String.valueOf("KMGTPE".charAt(exp - 1));
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}