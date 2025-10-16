package net.denfry.aquaguard.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.denfry.aquaguard.AquaGuard;
import net.denfry.aquaguard.database.LogEntry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.CompoundTag;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class InspectCommand {
    private static final Map<UUID, Boolean> inspectMode = new HashMap<>();
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ag")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("inspect")
                        .executes(InspectCommand::toggleInspect))
                .then(Commands.literal("i")
                        .executes(InspectCommand::toggleInspect))
        );
    }

    private static int toggleInspect(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("§cOnly players can use this command"));
            return 0;
        }

        UUID playerUUID = player.getUUID();
        boolean currentMode = inspectMode.getOrDefault(playerUUID, false);
        inspectMode.put(playerUUID, !currentMode);

        if (!currentMode) {
            ItemStack stick = new ItemStack(Items.STICK);
            CompoundTag tag = new CompoundTag();
            tag.putBoolean("AquaGuardInspect", true);
            stick.setTag(tag);
            stick.setHoverName(Component.literal("§6Inspector Tool"));

            player.setItemInHand(InteractionHand.MAIN_HAND, stick);
            context.getSource().sendSuccess(() ->
                    Component.literal("§aInspector enabled. Left-click or right-click blocks to inspect."), false);
        } else {
            player.getMainHandItem().setCount(0);
            context.getSource().sendSuccess(() ->
                    Component.literal("§cInspector disabled."), false);
        }

        return 1;
    }

    public static boolean isInspectMode(UUID playerUUID) {
        return inspectMode.getOrDefault(playerUUID, false);
    }

    public static void inspectBlock(ServerPlayer player, BlockPos pos) {
        String dimension = LogEntry.getDimensionKey(player.level());
        List<LogEntry> logs = AquaGuard.getDatabaseManager().lookupAtExactPosition(pos, dimension);

        if (logs.isEmpty()) {
            player.sendSystemMessage(Component.literal("§7No logged data for block at §e" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()));
            return;
        }

        player.sendSystemMessage(Component.literal("§6===== Block Inspector: §e" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + " §6====="));
        player.sendSystemMessage(Component.literal("§eDimension: §f" + dimension));
        player.sendSystemMessage(Component.literal("§eHistory (newest first, max 10 entries):"));

        int count = 0;
        for (LogEntry entry : logs) {
            if (count >= 10) break;

            String timeStr = TIME_FORMATTER.format(Instant.ofEpochMilli(entry.getTimestamp()));
            String actionColor = getActionColor(entry.getActionType());
            String actionName = entry.getActionType().name().toLowerCase().replace("_", " ");

            String details = getDetails(entry);
            Component message = Component.literal(
                    String.format("§7%s §f%s %s%s", timeStr, entry.getPlayerName(), actionColor, actionName)
            );

            if (!details.isEmpty()) {
                message = message.copy().append(Component.literal(" §8[Details]").withStyle(style ->
                        style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(details)))));
            }

            player.sendSystemMessage(message);
            count++;
        }

        player.sendSystemMessage(Component.literal("§6========================"));
    }

    private static String getActionColor(LogEntry.ActionType type) {
        return switch (type) {
            case BLOCK_PLACE -> "§a";
            case BLOCK_BREAK -> "§c";
            case CONTAINER_ACCESS -> "§e";
            case CONTAINER_MODIFY -> "§b";
            case EXPLOSION -> "§4";
            case ENTITY_KILL -> "§5";
            case PLAYER_DEATH -> "§4";
            case ITEM_PICKUP -> "§b";
            case ITEM_DROP -> "§d";
            case FLUID_PLACE -> "§9";
            case PLAYER_LOGIN -> "§a";
            case PLAYER_LOGOUT -> "§c";
            case PLAYER_CHAT -> "§7";
            case PLAYER_COMMAND -> "§6";
        };
    }

    private static String getDetails(LogEntry entry) {
        StringBuilder details = new StringBuilder();
        if (entry.getBlockStateBefore() != null && !entry.getBlockStateBefore().equals("minecraft:air")) {
            details.append("§7Before: §f").append(simplifyBlockName(entry.getBlockStateBefore())).append("\n");
        }
        if (entry.getBlockStateAfter() != null && !entry.getBlockStateAfter().equals("minecraft:air")) {
            details.append("§7After: §f").append(simplifyBlockName(entry.getBlockStateAfter())).append("\n");
        }
        if (entry.getItemData() != null) {
            details.append("§7Data: §f").append(entry.getItemData());
        }
        return details.toString();
    }

    private static String simplifyBlockName(String blockState) {
        if (blockState.startsWith("Block{")) {
            blockState = blockState.substring(6, blockState.length() - 1);
        }
        if (blockState.contains("[")) {
            blockState = blockState.substring(0, blockState.indexOf("["));
        }
        return blockState;
    }
}