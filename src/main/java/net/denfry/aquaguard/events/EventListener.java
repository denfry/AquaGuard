package net.denfry.aquaguard.events;

import net.denfry.aquaguard.AquaGuard;
import net.denfry.aquaguard.Config;
import net.denfry.aquaguard.database.LogEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = AquaGuard.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EventListener {

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (player == null || player.level().isClientSide() || !Config.LOG_BLOCK_BREAK.get()) return;

        BlockPos pos = event.getPos();
        BlockState state = event.getState();

        LogEntry entry = new LogEntry(
                player.getName().getString(),
                player.getStringUUID(),
                LogEntry.ActionType.BLOCK_BREAK,
                LogEntry.getDimensionKey(player.level()),
                pos,
                state.toString(),
                "minecraft:air",
                null
        );

        AquaGuard.getDatabaseManager().logAction(entry);
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide() || !Config.LOG_BLOCK_PLACE.get()) return;

        BlockPos pos = event.getPos();
        BlockState placedState = event.getPlacedBlock();
        BlockState oldState = event.getBlockSnapshot().getReplacedBlock();

        LogEntry entry = new LogEntry(
                player.getName().getString(),
                player.getStringUUID(),
                LogEntry.ActionType.BLOCK_PLACE,
                LogEntry.getDimensionKey(player.level()),
                pos,
                oldState.toString(),
                placedState.toString(),
                null
        );

        AquaGuard.getDatabaseManager().logAction(entry);
    }

    @SubscribeEvent
    public static void onFluidPlace(BlockEvent.FluidPlaceBlockEvent event) {
        if (!Config.LOG_FLUID_PLACE.get() || event.getLevel().isClientSide()) return;

        String playerName = "Environment";
        String playerUUID = "00000000-0000-0000-0000-000000000000";
        String dimension = LogEntry.getDimensionKey((Level) event.getLevel());

        BlockPos pos = event.getPos();
        BlockState oldState = event.getOriginalState();
        BlockState newState = event.getNewState();

        LogEntry entry = new LogEntry(
                playerName,
                playerUUID,
                LogEntry.ActionType.FLUID_PLACE,
                dimension,
                pos,
                oldState.toString(),
                newState.toString(),
                null
        );

        AquaGuard.getDatabaseManager().logAction(entry);
    }

    @SubscribeEvent
    public static void onContainerOpen(PlayerInteractEvent.RightClickBlock event) {
        if (!Config.LOG_CONTAINER_ACCESS.get()) return;

        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        BlockPos pos = event.getPos();
        BlockState state = player.level().getBlockState(pos);

        boolean isContainer =
                state.getBlock().getMenuProvider(state, player.level(), pos) != null ||
                        player.level().getBlockEntity(pos) instanceof BaseContainerBlockEntity;

        if (!isContainer) return;

        LogEntry entry = new LogEntry(
                player.getName().getString(),
                player.getStringUUID(),
                LogEntry.ActionType.CONTAINER_ACCESS,
                LogEntry.getDimensionKey(player.level()),
                pos,
                state.toString(),
                state.toString(),
                null
        );

        AquaGuard.getDatabaseManager().logAction(entry);
    }

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!Config.LOG_CONTAINER_MODIFY.get()) return;
        if (event.getEntity().level().isClientSide()) return;

        Player player = event.getEntity();
        ItemStack crafted = event.getCrafting();

        LogEntry entry = new LogEntry(
                player.getName().getString(),
                player.getStringUUID(),
                LogEntry.ActionType.CONTAINER_MODIFY,
                LogEntry.getDimensionKey(player.level()),
                player.blockPosition(),
                null,
                null,
                crafted.toString()
        );

        AquaGuard.getDatabaseManager().logAction(entry);
    }

    @SubscribeEvent
    public static void onEntityDeath(LivingDeathEvent event) {
        if (!Config.LOG_ENTITY_KILL.get()) return;

        if (event.getSource().getEntity() instanceof Player killer && !killer.level().isClientSide()) {
            BlockPos pos = event.getEntity().blockPosition();
            String entityTypeId = ForgeRegistries.ENTITY_TYPES.getKey(event.getEntity().getType()).toString();

            LogEntry entry = new LogEntry(
                    killer.getName().getString(),
                    killer.getStringUUID(),
                    LogEntry.ActionType.ENTITY_KILL,
                    LogEntry.getDimensionKey(killer.level()),
                    pos,
                    entityTypeId,
                    null,
                    null
            );
            AquaGuard.getDatabaseManager().logAction(entry);
        }

        if (event.getEntity() instanceof Player player && !player.level().isClientSide()) {
            BlockPos pos = player.blockPosition();

            LogEntry entry = new LogEntry(
                    player.getName().getString(),
                    player.getStringUUID(),
                    LogEntry.ActionType.PLAYER_DEATH,
                    LogEntry.getDimensionKey(player.level()),
                    pos,
                    "player_death",
                    null,
                    event.getSource().getMsgId()
            );
            AquaGuard.getDatabaseManager().logAction(entry);
        }
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (!Config.LOG_EXPLOSION.get() || event.getLevel().isClientSide()) return;

        String playerName = "Environment";
        String playerUUID = "00000000-0000-0000-0000-000000000000";

        if (event.getExplosion().getIndirectSourceEntity() instanceof Player player) {
            playerName = player.getName().getString();
            playerUUID = player.getStringUUID();
        }

        for (BlockPos affectedPos : event.getAffectedBlocks()) {
            BlockState state = event.getLevel().getBlockState(affectedPos);

            LogEntry entry = new LogEntry(
                    playerName,
                    playerUUID,
                    LogEntry.ActionType.EXPLOSION,
                    LogEntry.getDimensionKey(event.getLevel()),
                    affectedPos,
                    state.toString(),
                    "minecraft:air",
                    null
            );

            AquaGuard.getDatabaseManager().logAction(entry);
        }
    }

    @SubscribeEvent
    public static void onItemPickup(PlayerEvent.ItemPickupEvent event) {
        if (!Config.LOG_ITEM_PICKUP.get()) return;
        if (event.getEntity().level().isClientSide()) return;

        Player player = event.getEntity();
        ItemStack stack = event.getStack();
        BlockPos pos = player.blockPosition();

        LogEntry entry = new LogEntry(
                player.getName().getString(),
                player.getStringUUID(),
                LogEntry.ActionType.ITEM_PICKUP,
                LogEntry.getDimensionKey(player.level()),
                pos,
                null,
                null,
                stack.toString()
        );

        AquaGuard.getDatabaseManager().logAction(entry);
    }

    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event) {
        if (!Config.LOG_ITEM_DROP.get()) return;
        if (event.getEntity().level().isClientSide()) return;

        Player player = event.getPlayer();
        ItemEntity itemEntity = event.getEntity();
        ItemStack stack = itemEntity.getItem();
        BlockPos pos = player.blockPosition();

        LogEntry entry = new LogEntry(
                player.getName().getString(),
                player.getStringUUID(),
                LogEntry.ActionType.ITEM_DROP,
                LogEntry.getDimensionKey(player.level()),
                pos,
                null,
                null,
                stack.toString()
        );

        AquaGuard.getDatabaseManager().logAction(entry);
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!Config.LOG_PLAYER_LOGIN.get()) return;
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        LogEntry entry = new LogEntry(
                player.getName().getString(),
                player.getStringUUID(),
                LogEntry.ActionType.PLAYER_LOGIN,
                LogEntry.getDimensionKey(player.level()),
                player.blockPosition(),
                null,
                null,
                "Player logged in"
        );

        AquaGuard.getDatabaseManager().logAction(entry);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!Config.LOG_PLAYER_LOGOUT.get()) return;
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        LogEntry entry = new LogEntry(
                player.getName().getString(),
                player.getStringUUID(),
                LogEntry.ActionType.PLAYER_LOGOUT,
                LogEntry.getDimensionKey(player.level()),
                player.blockPosition(),
                null,
                null,
                "Player logged out"
        );

        AquaGuard.getDatabaseManager().logAction(entry);
    }

    @SubscribeEvent
    public static void onPlayerChat(ServerChatEvent event) {
        if (!Config.LOG_PLAYER_CHAT.get()) return;

        Player player = event.getPlayer();
        String message = event.getMessage().getString();

        LogEntry entry = new LogEntry(
                player.getName().getString(),
                player.getStringUUID(),
                LogEntry.ActionType.PLAYER_CHAT,
                LogEntry.getDimensionKey(player.level()),
                player.blockPosition(),
                null,
                null,
                message
        );

        AquaGuard.getDatabaseManager().logAction(entry);
    }

    @SubscribeEvent
    public static void onPlayerCommand(net.minecraftforge.event.CommandEvent event) {
        if (!Config.LOG_PLAYER_COMMAND.get()) return;

        if (event.getParseResults().getContext().getSource().getEntity() instanceof Player player) {
            String command = event.getParseResults().getReader().getString();

            LogEntry entry = new LogEntry(
                    player.getName().getString(),
                    player.getStringUUID(),
                    LogEntry.ActionType.PLAYER_COMMAND,
                    LogEntry.getDimensionKey(player.level()),
                    player.blockPosition(),
                    null,
                    null,
                    command
            );

            AquaGuard.getDatabaseManager().logAction(entry);
        }
    }
}
