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
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
public class RollbackCommand {
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
    private static final SuggestionProvider<CommandSourceStack> TIME_SUGGESTOR =
            (ctx, builder) -> {
                List<String> times = Arrays.asList("10s", "30s", "1m", "10m", "1h", "12h", "24h", "7d");
                return SharedSuggestionProvider.suggest(times, builder);
            };
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ag")
                .requires(source -> source.hasPermission(3))
                .then(Commands.literal("rollback")
                        .then(Commands.literal("user")
                                .then(Commands.argument("player", StringArgumentType.string())
                                        .suggests(PLAYER_SUGGESTOR)
                                        .executes(ctx -> rollbackByPlayer(ctx,
                                                StringArgumentType.getString(ctx, "player"),
                                                "24h", null))
                                        .then(Commands.argument("time", StringArgumentType.string())
                                                .suggests(TIME_SUGGESTOR)
                                                .executes(ctx -> rollbackByPlayer(ctx,
                                                        StringArgumentType.getString(ctx, "player"),
                                                        StringArgumentType.getString(ctx, "time"),
                                                        null))
                                                .then(Commands.argument("action", StringArgumentType.string())
                                                        .suggests(ACTION_SUGGESTOR)
                                                        .executes(ctx -> rollbackByPlayer(ctx,
                                                                StringArgumentType.getString(ctx, "player"),
                                                                StringArgumentType.getString(ctx, "time"),
                                                                StringArgumentType.getString(ctx, "action")))
                                                )
                                        )
                                )
                        )
                        .then(Commands.literal("radius")
                                .then(Commands.argument("radius", IntegerArgumentType.integer(1, 100))
                                        .then(Commands.argument("time", StringArgumentType.string())
                                                .suggests(TIME_SUGGESTOR)
                                                .executes(ctx -> rollbackByRadius(ctx,
                                                        IntegerArgumentType.getInteger(ctx, "radius"),
                                                        StringArgumentType.getString(ctx, "time"),
                                                        null))
                                                .then(Commands.argument("action", StringArgumentType.string())
                                                        .suggests(ACTION_SUGGESTOR)
                                                        .executes(ctx -> rollbackByRadius(ctx,
                                                                IntegerArgumentType.getInteger(ctx, "radius"),
                                                                StringArgumentType.getString(ctx, "time"),
                                                                StringArgumentType.getString(ctx, "action")))
                                                )
                                        )
                                )
                        )
                )
                .then(Commands.literal("restore")
                        .then(Commands.literal("user")
                                .then(Commands.argument("player", StringArgumentType.string())
                                        .suggests(PLAYER_SUGGESTOR)
                                        .executes(ctx -> restoreByPlayer(ctx,
                                                StringArgumentType.getString(ctx, "player"),
                                                "24h", null))
                                        .then(Commands.argument("time", StringArgumentType.string())
                                                .suggests(TIME_SUGGESTOR)
                                                .executes(ctx -> restoreByPlayer(ctx,
                                                        StringArgumentType.getString(ctx, "player"),
                                                        StringArgumentType.getString(ctx, "time"),
                                                        null))
                                                .then(Commands.argument("action", StringArgumentType.string())
                                                        .suggests(ACTION_SUGGESTOR)
                                                        .executes(ctx -> restoreByPlayer(ctx,
                                                                StringArgumentType.getString(ctx, "player"),
                                                                StringArgumentType.getString(ctx, "time"),
                                                                StringArgumentType.getString(ctx, "action")))
                                                )
                                        )
                                )
                        )
                        .then(Commands.literal("radius")
                                .then(Commands.argument("radius", IntegerArgumentType.integer(1, 100))
                                        .then(Commands.argument("time", StringArgumentType.string())
                                                .suggests(TIME_SUGGESTOR)
                                                .executes(ctx -> restoreByRadius(ctx,
                                                        IntegerArgumentType.getInteger(ctx, "radius"),
                                                        StringArgumentType.getString(ctx, "time"),
                                                        null))
                                                .then(Commands.argument("action", StringArgumentType.string())
                                                        .suggests(ACTION_SUGGESTOR)
                                                        .executes(ctx -> restoreByRadius(ctx,
                                                                IntegerArgumentType.getInteger(ctx, "radius"),
                                                                StringArgumentType.getString(ctx, "time"),
                                                                StringArgumentType.getString(ctx, "action")))
                                                )
                                        )
                                )
                        )
                )
        );
    }
    private static int rollbackByPlayer(CommandContext<CommandSourceStack> ctx, String playerName, String timeStr, String action) throws CommandSyntaxException {
        long timeMs = LookupCommand.parseTime(timeStr);
        LogEntry.ActionType actionType = parseAction(action);
        List<LogEntry> logs = AquaGuard.getDatabaseManager().lookupByPlayer(playerName, timeMs);
        if (actionType != null) {
            logs = logs.stream().filter(entry -> entry.getActionType() == actionType).collect(Collectors.toList());
        }
        if (logs.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("§cNo logs found for player §e" + playerName));
            return 0;
        }
        int count = performRollback(ctx.getSource(), logs);
        ctx.getSource().sendSuccess(() -> Component.literal("§aRollback complete! Restored §f" + count + "§a blocks for §e" + playerName), true);
        return count;
    }
    private static int rollbackByRadius(CommandContext<CommandSourceStack> ctx, int radius, String timeStr, String action) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        BlockPos pos = player.blockPosition();
        String dimension = LogEntry.getDimensionKey(player.level());
        long timeMs = LookupCommand.parseTime(timeStr);
        LogEntry.ActionType actionType = parseAction(action);
        List<LogEntry> logs = AquaGuard.getDatabaseManager().lookupByPosition(pos, dimension, radius, timeMs);
        if (actionType != null) {
            logs = logs.stream().filter(entry -> entry.getActionType() == actionType).collect(Collectors.toList());
        }
        if (logs.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("§cNo logs found within radius §e" + radius));
            return 0;
        }
        int count = performRollback(ctx.getSource(), logs);
        ctx.getSource().sendSuccess(() -> Component.literal("§aRollback complete! Restored §f" + count + "§a blocks within §e" + radius + "§a blocks"), true);
        return count;
    }
    private static int restoreByPlayer(CommandContext<CommandSourceStack> ctx, String playerName, String timeStr, String action) throws CommandSyntaxException {
        long timeMs = LookupCommand.parseTime(timeStr);
        LogEntry.ActionType actionType = parseAction(action);
        List<LogEntry> logs = AquaGuard.getDatabaseManager().lookupByPlayer(playerName, timeMs);
        if (actionType != null) {
            logs = logs.stream().filter(entry -> entry.getActionType() == actionType).collect(Collectors.toList());
        }
        if (logs.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("§cNo logs found for player §e" + playerName));
            return 0;
        }
        int count = performRestore(ctx.getSource(), logs);
        ctx.getSource().sendSuccess(() -> Component.literal("§aRestore complete! Restored §f" + count + "§a blocks for §e" + playerName), true);
        return count;
    }
    private static int restoreByRadius(CommandContext<CommandSourceStack> ctx, int radius, String timeStr, String action) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        BlockPos pos = player.blockPosition();
        String dimension = LogEntry.getDimensionKey(player.level());
        long timeMs = LookupCommand.parseTime(timeStr);
        LogEntry.ActionType actionType = parseAction(action);
        List<LogEntry> logs = AquaGuard.getDatabaseManager().lookupByPosition(pos, dimension, radius, timeMs);
        if (actionType != null) {
            logs = logs.stream().filter(entry -> entry.getActionType() == actionType).collect(Collectors.toList());
        }
        if (logs.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("§cNo logs found within radius §e" + radius));
            return 0;
        }
        int count = performRestore(ctx.getSource(), logs);
        ctx.getSource().sendSuccess(() -> Component.literal("§aRestore complete! Restored §f" + count + "§a blocks within §e" + radius + "§a blocks"), true);
        return count;
    }
    private static LogEntry.ActionType parseAction(String actionStr) throws CommandSyntaxException {
        if (actionStr == null) return null;
        try {
            return LogEntry.ActionType.valueOf(actionStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.literalIncorrect().create(actionStr);
        }
    }
    private static int performRollback(CommandSourceStack source, List<LogEntry> logs) {
        ServerLevel level = source.getLevel();
        Map<BlockPos, LogEntry> latestChanges = new HashMap<>();
        int skipped = 0;
        for (int i = logs.size() - 1; i >= 0; i--) {
            LogEntry entry = logs.get(i);
            if (!latestChanges.containsKey(entry.getPosition()) &&
                    (entry.getActionType() == LogEntry.ActionType.BLOCK_PLACE ||
                            entry.getActionType() == LogEntry.ActionType.BLOCK_BREAK ||
                            entry.getActionType() == LogEntry.ActionType.EXPLOSION ||
                            entry.getActionType() == LogEntry.ActionType.FLUID_PLACE)) {
                latestChanges.put(entry.getPosition(), entry);
            } else {
                skipped++;
            }
        }
        int count = 0;
        for (LogEntry entry : latestChanges.values()) {
            BlockPos pos = entry.getPosition();
            if (pos == null) continue;
            String blockStateStr = (entry.getActionType() == LogEntry.ActionType.BLOCK_PLACE ||
                    entry.getActionType() == LogEntry.ActionType.FLUID_PLACE) ?
                    entry.getBlockStateBefore() : entry.getBlockStateAfter();
            BlockState state = parseBlockState(level, blockStateStr);
            if (state != null) {
                level.setBlock(pos, state, 3);
                count++;
            }
        }
        if (skipped > 0) {
            int finalSkipped = skipped;
            source.sendSuccess(() ->
                    Component.literal("§7Skipped §f" + finalSkipped + "§7 non-unique or irrelevant entries"), false);
        }
        return count;
    }
    private static int performRestore(CommandSourceStack source, List<LogEntry> logs) {
        ServerLevel level = source.getLevel();
        Map<BlockPos, LogEntry> latestChanges = new HashMap<>();
        int skipped = 0;
        for (LogEntry entry : logs) {
            if (!latestChanges.containsKey(entry.getPosition()) &&
                    (entry.getActionType() == LogEntry.ActionType.BLOCK_PLACE ||
                            entry.getActionType() == LogEntry.ActionType.BLOCK_BREAK ||
                            entry.getActionType() == LogEntry.ActionType.EXPLOSION ||
                            entry.getActionType() == LogEntry.ActionType.FLUID_PLACE)) {
                latestChanges.put(entry.getPosition(), entry);
            } else {
                skipped++;
            }
        }
        int count = 0;
        for (LogEntry entry : latestChanges.values()) {
            BlockPos pos = entry.getPosition();
            if (pos == null) continue;
            String blockStateStr = (entry.getActionType() == LogEntry.ActionType.BLOCK_BREAK ||
                    entry.getActionType() == LogEntry.ActionType.EXPLOSION) ?
                    entry.getBlockStateBefore() : entry.getBlockStateAfter();
            BlockState state = parseBlockState(level, blockStateStr);
            if (state != null) {
                level.setBlock(pos, state, 3);
                count++;
            }
        }
        if (skipped > 0) {
            int finalSkipped = skipped;
            source.sendSuccess(() ->
                    Component.literal("§7Skipped §f" + finalSkipped + "§7 non-unique or irrelevant entries"), false);
        }
        return count;
    }
    private static BlockState parseBlockState(ServerLevel level, String blockStateStr) {
        if (blockStateStr == null || blockStateStr.equals("minecraft:air")) {
            return Blocks.AIR.defaultBlockState();
        }
        try {
            String fullStateStr = getFullBlockStateString(blockStateStr);
            BlockStateParser.BlockResult result = BlockStateParser.parseForBlock(
                    level.registryAccess().lookupOrThrow(Registries.BLOCK),
                    fullStateStr,
                    false
            );
            return result.blockState();
        } catch (CommandSyntaxException e) {
            AquaGuard.LOGGER.warn("[AquaGuard] Failed to parse block state: {}", blockStateStr);
            return null;
        } catch (Exception e) {
            AquaGuard.LOGGER.error("[AquaGuard] Error restoring block: {}", blockStateStr, e);
            return null;
        }
    }
    private static String getFullBlockStateString(String blockState) {
        if (blockState.startsWith("Block{")) {
            blockState = blockState.substring(6, blockState.length() - 1);
        }
        return blockState;
    }
}