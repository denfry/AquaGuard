package net.denfry.aquaguard.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.denfry.aquaguard.AquaGuard;
import net.denfry.aquaguard.database.LogEntry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class LookupCommand {
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
    private static final int RESULTS_PER_PAGE = 10;


    private static final SuggestionProvider<CommandSourceStack> PLAYER_SUGGESTOR =
            (ctx, builder) -> {
                MinecraftServer server = ctx.getSource().getServer();
                return SharedSuggestionProvider.suggest(server.getPlayerNames(), builder);
            };

    private static final SuggestionProvider<CommandSourceStack> ACTION_SUGGESTOR =
            (ctx, builder) -> {
                List<String> actions = Arrays.stream(LogEntry.ActionType.values())
                        .map(type -> type.name().toLowerCase())
                        .collect(Collectors.toList());
                return SharedSuggestionProvider.suggest(actions, builder);
            };

    private static final SuggestionProvider<CommandSourceStack> RADIUS_SUGGESTOR =
            (ctx, builder) -> {
                List<String> radii = Arrays.asList("5", "10", "20", "50", "100");
                return SharedSuggestionProvider.suggest(radii, builder);
            };

    private static final SuggestionProvider<CommandSourceStack> TIME_SUGGESTOR =
            (ctx, builder) -> {
                List<String> times = Arrays.asList("10s", "30s", "1m", "10m", "1h", "12h", "24h", "7d");
                return SharedSuggestionProvider.suggest(times, builder);
            };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ag")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("lookup")
                        .then(Commands.literal("user")
                                .then(Commands.argument("player", StringArgumentType.string())
                                        .suggests(PLAYER_SUGGESTOR)
                                        .executes(ctx -> lookupByPlayer(ctx, StringArgumentType.getString(ctx, "player"), "24h"))
                                        .then(Commands.argument("time", StringArgumentType.string())
                                                .suggests(TIME_SUGGESTOR)
                                                .executes(ctx -> lookupByPlayer(ctx,
                                                        StringArgumentType.getString(ctx, "player"),
                                                        StringArgumentType.getString(ctx, "time")))
                                        )
                                )
                        )
                        .then(Commands.literal("radius")
                                .then(Commands.argument("radius", IntegerArgumentType.integer(1, 100))
                                        .suggests(RADIUS_SUGGESTOR)
                                        .executes(ctx -> lookupByRadius(ctx,
                                                IntegerArgumentType.getInteger(ctx, "radius"), "24h"))
                                        .then(Commands.argument("time", StringArgumentType.string())
                                                .suggests(TIME_SUGGESTOR)
                                                .executes(ctx -> lookupByRadius(ctx,
                                                        IntegerArgumentType.getInteger(ctx, "radius"),
                                                        StringArgumentType.getString(ctx, "time")))
                                        )
                                )
                        )
                        .then(Commands.literal("position")
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(ctx -> lookupByPosition(ctx,
                                                BlockPosArgument.getLoadedBlockPos(ctx, "pos"), 5, "24h"))
                                        .then(Commands.argument("time", StringArgumentType.string())
                                                .suggests(TIME_SUGGESTOR)
                                                .executes(ctx -> lookupByPosition(ctx,
                                                        BlockPosArgument.getLoadedBlockPos(ctx, "pos"),
                                                        5,
                                                        StringArgumentType.getString(ctx, "time")))
                                        )
                                )
                        )
                        .then(Commands.literal("action")
                                .then(Commands.argument("action", StringArgumentType.string())
                                        .suggests(ACTION_SUGGESTOR)
                                        .executes(ctx -> lookupByAction(ctx,
                                                StringArgumentType.getString(ctx, "action")))
                                )
                        )
                )
                .then(Commands.literal("l")
                        .then(Commands.literal("u")
                                .then(Commands.argument("player", StringArgumentType.string())
                                        .suggests(PLAYER_SUGGESTOR)
                                        .executes(ctx -> lookupByPlayer(ctx,
                                                StringArgumentType.getString(ctx, "player"), "24h"))
                                        .then(Commands.argument("time", StringArgumentType.string())
                                                .suggests(TIME_SUGGESTOR)
                                                .executes(ctx -> lookupByPlayer(ctx,
                                                        StringArgumentType.getString(ctx, "player"),
                                                        StringArgumentType.getString(ctx, "time")))
                                        )
                                )
                        )
                        .then(Commands.literal("r")
                                .then(Commands.argument("radius", IntegerArgumentType.integer(1, 100))
                                        .suggests(RADIUS_SUGGESTOR)
                                        .executes(ctx -> lookupByRadius(ctx,
                                                IntegerArgumentType.getInteger(ctx, "radius"), "24h"))
                                        .then(Commands.argument("time", StringArgumentType.string())
                                                .suggests(TIME_SUGGESTOR)
                                                .executes(ctx -> lookupByRadius(ctx,
                                                        IntegerArgumentType.getInteger(ctx, "radius"),
                                                        StringArgumentType.getString(ctx, "time")))
                                        )
                                )
                        )
                )
        );
    }

    private static int lookupByPlayer(CommandContext<CommandSourceStack> ctx, String playerName, String timeStr) throws CommandSyntaxException {
        long timeMs = parseTime(timeStr);

        List<LogEntry> logs = AquaGuard.getDatabaseManager().lookupByPlayer(playerName, timeMs);

        if (logs.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("§7No data found for player: §e" + playerName));
            return 0;
        }

        ctx.getSource().sendSuccess(() ->
                Component.literal("§6===== Lookup Results for Player: §e" + playerName + " §6====="), false);
        ctx.getSource().sendSuccess(() ->
                Component.literal("§eFound §f" + logs.size() + "§e entries within §f" + timeStr), false);
        displayLogs(ctx.getSource(), logs, 1);
        return 1;
    }

    private static int lookupByRadius(CommandContext<CommandSourceStack> ctx, int radius, String timeStr) throws CommandSyntaxException {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
            ctx.getSource().sendFailure(Component.literal("§cOnly players can use radius lookup"));
            return 0;
        }

        long timeMs = parseTime(timeStr);

        BlockPos pos = player.blockPosition();
        String dimension = LogEntry.getDimensionKey(player.level());
        List<LogEntry> logs = AquaGuard.getDatabaseManager().lookupByPosition(pos, dimension, radius, timeMs);

        if (logs.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("§7No data found in radius §e" + radius + "§7 blocks"));
            return 0;
        }

        ctx.getSource().sendSuccess(() ->
                Component.literal("§6===== Lookup Results in Radius: §e" + radius + " blocks §6====="), false);
        ctx.getSource().sendSuccess(() ->
                Component.literal("§eCenter: §f" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + " §e(Dimension: §f" + dimension + "§e)"), false);
        ctx.getSource().sendSuccess(() ->
                Component.literal("§eFound §f" + logs.size() + "§e entries within §f" + timeStr), false);
        displayLogs(ctx.getSource(), logs, 1);
        return 1;
    }

    private static int lookupByPosition(CommandContext<CommandSourceStack> ctx, BlockPos pos, int radius, String timeStr) throws CommandSyntaxException {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
            ctx.getSource().sendFailure(Component.literal("§cOnly players can use position lookup"));
            return 0;
        }

        long timeMs = parseTime(timeStr);

        String dimension = LogEntry.getDimensionKey(player.level());
        List<LogEntry> logs = AquaGuard.getDatabaseManager().lookupByPosition(pos, dimension, radius, timeMs);

        if (logs.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("§7No data found at position §e" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()));
            return 0;
        }

        ctx.getSource().sendSuccess(() ->
                Component.literal("§6===== Lookup Results at Position: §e" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + " §6====="), false);
        ctx.getSource().sendSuccess(() ->
                Component.literal("§eFound §f" + logs.size() + "§e entries within §f" + timeStr), false);
        displayLogs(ctx.getSource(), logs, 1);
        return 1;
    }

    private static int lookupByAction(CommandContext<CommandSourceStack> ctx, String action) throws CommandSyntaxException {
        LogEntry.ActionType actionType = parseAction(action);

        List<LogEntry> logs = AquaGuard.getDatabaseManager().lookupByPlayer("", Long.MAX_VALUE)
                .stream()
                .filter(entry -> entry.getActionType() == actionType)
                .collect(Collectors.toList());

        if (logs.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("§7No data found for action: §e" + action));
            return 0;
        }

        ctx.getSource().sendSuccess(() ->
                Component.literal("§6===== Lookup Results for Action: §e" + action.toLowerCase().replace("_", " ") + " §6====="), false);
        ctx.getSource().sendSuccess(() ->
                Component.literal("§eFound §f" + logs.size() + "§e entries"), false);
        displayLogs(ctx.getSource(), logs, 1);
        return 1;
    }

    protected static long parseTime(String timeStr) throws CommandSyntaxException {
        timeStr = timeStr.toLowerCase();
        long multiplier = 1000L; // default seconds to ms
        long value;

        try {
            if (timeStr.endsWith("s")) {
                value = Long.parseLong(timeStr.substring(0, timeStr.length() - 1));
                multiplier = 1000L;
            } else if (timeStr.endsWith("m")) {
                value = Long.parseLong(timeStr.substring(0, timeStr.length() - 1));
                multiplier = 60 * 1000L;
            } else if (timeStr.endsWith("h")) {
                value = Long.parseLong(timeStr.substring(0, timeStr.length() - 1));
                multiplier = 3600 * 1000L;
            } else if (timeStr.endsWith("d")) {
                value = Long.parseLong(timeStr.substring(0, timeStr.length() - 1));
                multiplier = 86400 * 1000L;
            } else {
                value = Long.parseLong(timeStr);
            }
            if (value <= 0) {
                throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.literalIncorrect().create(timeStr);
            }
            return value * multiplier;
        } catch (NumberFormatException e) {
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.literalIncorrect().create(timeStr);
        }
    }

    private static LogEntry.ActionType parseAction(String actionStr) throws CommandSyntaxException {
        try {
            return LogEntry.ActionType.valueOf(actionStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.literalIncorrect().create(actionStr);
        }
    }

    private static void displayLogs(CommandSourceStack source, List<LogEntry> logs, int page) {
        int start = (page - 1) * RESULTS_PER_PAGE;
        int end = Math.min(start + RESULTS_PER_PAGE, logs.size());

        if (start >= logs.size()) {
            source.sendSuccess(() -> Component.literal("§cNo more results to display"), false);
            return;
        }

        source.sendSuccess(() -> Component.literal("§e--- Page " + page + " of " + ((logs.size() + RESULTS_PER_PAGE - 1) / RESULTS_PER_PAGE) + " ---"), false);

        for (int i = start; i < end; i++) {
            LogEntry entry = logs.get(i);
            String timeStr = TIME_FORMATTER.format(Instant.ofEpochMilli(entry.getTimestamp()));
            String actionColor = getActionColor(entry.getActionType());
            String actionName = entry.getActionType().name().toLowerCase().replace("_", " ");
            BlockPos pos = entry.getPosition();

            MutableComponent message = Component.literal(
                    String.format("§7%s §f%s %s%s §7at §f%s",
                            timeStr,
                            entry.getPlayerName(),
                            actionColor,
                            actionName,
                            pos != null ? pos.getX() + ", " + pos.getY() + ", " + pos.getZ() : "N/A")
            );

            String details = getBlockDetails(entry);
            if (!details.isEmpty()) {
                message.append(Component.literal(" §8[Details]").withStyle(style ->
                        style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(details)))));
            }

            if (pos != null) {
                String tpCommand = String.format("/tp @s %d %d %d", pos.getX(), pos.getY(), pos.getZ());
                message.setStyle(message.getStyle()
                        .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, tpCommand))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Component.literal("§eClick to teleport\n§7" + details))));
            }

            source.sendSuccess(() -> message, false);
        }

        if (end < logs.size()) {
            int nextPage = page + 1;
            MutableComponent nextPageMsg = Component.literal("§a[Next Page]")
                    .withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                            "/ag lookup page " + nextPage)));
            source.sendSuccess(() -> nextPageMsg, false);
        }

        source.sendSuccess(() -> Component.literal("§6========================"), false);
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

    private static String getBlockDetails(LogEntry entry) {
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