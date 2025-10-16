package net.denfry.aquaguard.events;

import net.denfry.aquaguard.AquaGuard;
import net.denfry.aquaguard.commands.InspectCommand;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AquaGuard.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class InspectEventHandler {

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ItemStack heldItem = player.getMainHandItem();

            if (heldItem.hasTag()) {
                CompoundTag tag = heldItem.getTag();
                if (tag != null && tag.getBoolean("AquaGuardInspect")) {
                    event.setCanceled(true);

                    BlockPos pos = event.getPos();
                    InspectCommand.inspectBlock(player, pos);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ItemStack heldItem = player.getMainHandItem();

            if (heldItem.hasTag()) {
                CompoundTag tag = heldItem.getTag();
                if (tag != null && tag.getBoolean("AquaGuardInspect")) {
                    event.setCanceled(true);

                    BlockPos pos = event.getPos();
                    InspectCommand.inspectBlock(player, pos);
                }
            }
        }
    }
}