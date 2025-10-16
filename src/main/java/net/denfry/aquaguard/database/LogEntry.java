package net.denfry.aquaguard.database;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.time.Instant;
import java.util.Objects;

public class LogEntry {
    private final long id;
    private final long timestamp;
    private final String playerName;
    private final String playerUUID;
    private final ActionType actionType;
    private final String dimension;
    private final BlockPos position;
    private final String blockStateBefore;
    private final String blockStateAfter;
    private final String itemData;

    public enum ActionType {
        BLOCK_PLACE,
        BLOCK_BREAK,
        CONTAINER_ACCESS,
        CONTAINER_MODIFY,
        ENTITY_KILL,
        PLAYER_DEATH,
        ITEM_PICKUP,
        ITEM_DROP,
        EXPLOSION,
        FLUID_PLACE,
        PLAYER_LOGIN,
        PLAYER_LOGOUT,
        PLAYER_CHAT,
        PLAYER_COMMAND
    }

    public LogEntry(long id,
                    long timestamp,
                    String playerName,
                    String playerUUID,
                    ActionType actionType,
                    String dimension,
                    BlockPos position,
                    String blockStateBefore,
                    String blockStateAfter,
                    String itemData) {
        this.id = id;
        this.timestamp = timestamp;
        this.playerName = playerName;
        this.playerUUID = playerUUID;
        this.actionType = actionType;
        this.dimension = dimension;
        this.position = position;
        this.blockStateBefore = blockStateBefore;
        this.blockStateAfter = blockStateAfter;
        this.itemData = itemData;
    }

    public LogEntry(String playerName,
                    String playerUUID,
                    ActionType actionType,
                    String dimension,
                    BlockPos position,
                    String blockStateBefore,
                    String blockStateAfter,
                    String itemData) {
        this(0,
                Instant.now().toEpochMilli(),
                playerName,
                playerUUID,
                actionType,
                dimension,
                position,
                blockStateBefore,
                blockStateAfter,
                itemData);
    }

    public long getId() { return id; }
    public long getTimestamp() { return timestamp; }
    public String getPlayerName() { return playerName; }
    public String getPlayerUUID() { return playerUUID; }
    public ActionType getActionType() { return actionType; }
    public String getDimension() { return dimension; }
    public BlockPos getPosition() { return position; }
    public String getBlockStateBefore() { return blockStateBefore; }
    public String getBlockStateAfter() { return blockStateAfter; }
    public String getItemData() { return itemData; }

    public static String getDimensionKey(Level level) {
        if (level == null) return "unknown";
        try {
            ResourceLocation loc = level.dimension().location();
            return loc.toString();
        } catch (Exception e) {
            return "unknown";
        }
    }

    @Override
    public String toString() {
        return "LogEntry{" +
                "id=" + id +
                ", timestamp=" + timestamp +
                ", playerName='" + playerName + '\'' +
                ", playerUUID='" + playerUUID + '\'' +
                ", actionType=" + actionType +
                ", dimension='" + dimension + '\'' +
                ", position=" + (position == null ? "null" : position.toShortString()) +
                ", blockStateBefore='" + blockStateBefore + '\'' +
                ", blockStateAfter='" + blockStateAfter + '\'' +
                ", itemData='" + itemData + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LogEntry logEntry)) return false;
        return id == logEntry.id &&
                timestamp == logEntry.timestamp &&
                Objects.equals(playerName, logEntry.playerName) &&
                Objects.equals(playerUUID, logEntry.playerUUID) &&
                actionType == logEntry.actionType &&
                Objects.equals(dimension, logEntry.dimension) &&
                Objects.equals(position, logEntry.position) &&
                Objects.equals(blockStateBefore, logEntry.blockStateBefore) &&
                Objects.equals(blockStateAfter, logEntry.blockStateAfter) &&
                Objects.equals(itemData, logEntry.itemData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, timestamp, playerName, playerUUID, actionType, dimension, position, blockStateBefore, blockStateAfter, itemData);
    }
}