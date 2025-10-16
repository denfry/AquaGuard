package net.denfry.aquaguard.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.denfry.aquaguard.AquaGuard;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.List;

public class PruneCommand {
    private static final SuggestionProvider<CommandSourceStack> TIME_SUGGESTOR =
            (ctx, builder) -> {
                List<String> times = Arrays.asList("10s", "30s", "1m", "10m", "1h", "12h", "24h", "7d");
                return SharedSuggestionProvider.suggest(times, builder);
            };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ag")
                .requires(source -> source.hasPermission(3))
                .then(Commands.literal("prune")
                        .then(Commands.argument("time", StringArgumentType.string())
                                .suggests(TIME_SUGGESTOR)
                                .executes(PruneCommand::pruneLogs)
                        )
                )
                .then(Commands.literal("purge")
                        .then(Commands.argument("time", StringArgumentType.string())
                                .suggests(TIME_SUGGESTOR)
                                .executes(PruneCommand::pruneLogs)
                        )
                )
        );
    }

    private static int pruneLogs(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String timeStr = StringArgumentType.getString(ctx, "time");
        long timeMs = LookupCommand.parseTime(timeStr); // Reuse parseTime from LookupCommand

        int removed = AquaGuard.getDatabaseManager().prune(timeMs);

        ctx.getSource().sendSuccess(() ->
                Component.literal("§aPrune complete! Removed §f" + removed + "§a old logs older than §e" + timeStr), true);
        return 1;
    }
}