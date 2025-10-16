package net.denfry.aquaguard.database;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;
import net.denfry.aquaguard.AquaGuard;
import net.minecraft.core.BlockPos;
import org.slf4j.Logger;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DatabaseManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final File logFile;
    private final Gson gson;
    private final List<LogEntry> logs;
    private final Object fileLock = new Object();

    public DatabaseManager(File logFile) {
        this.logFile = logFile;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.logs = new ArrayList<>();
        initialize();
    }

    public void initialize() {
        try {
            if (!logFile.getParentFile().exists() && !logFile.getParentFile().mkdirs()) {
                LOGGER.error("[AquaGuard] Failed to create log directory: {}", logFile.getParentFile().getAbsolutePath());
            }
            if (!logFile.exists()) {
                logFile.createNewFile();
                saveLogs();
                LOGGER.info("[AquaGuard] Created new log file: {}", logFile.getAbsolutePath());
            }
            loadLogs();
            LOGGER.info("[AquaGuard] File-based log storage initialized successfully");
        } catch (Exception e) {
            LOGGER.error("[AquaGuard] Failed to initialize log file", e);
        }
    }

    private void loadLogs() {
        synchronized (fileLock) {
            try (FileReader reader = new FileReader(logFile)) {
                Type logListType = new TypeToken<List<LogEntry>>() {}.getType();
                List<LogEntry> loadedLogs = gson.fromJson(reader, logListType);
                if (loadedLogs != null) {
                    logs.clear();
                    logs.addAll(loadedLogs);
                }
                LOGGER.info("[AquaGuard] Loaded {} log entries from {}", logs.size(), logFile.getAbsolutePath());
            } catch (Exception e) {
                LOGGER.error("[AquaGuard] Failed to load logs from file", e);
            }
        }
    }

    private void saveLogs() {
        synchronized (fileLock) {
            try (FileWriter writer = new FileWriter(logFile)) {
                gson.toJson(logs, writer);
            } catch (IOException e) {
                LOGGER.error("[AquaGuard] Failed to save logs to file", e);
            }
        }
    }

    public synchronized void logAction(LogEntry entry) {
        synchronized (fileLock) {
            long newId = logs.isEmpty() ? 1 : logs.get(logs.size() - 1).getId() + 1;
            LogEntry entryWithId = new LogEntry(
                    newId,
                    entry.getTimestamp(),
                    entry.getPlayerName(),
                    entry.getPlayerUUID(),
                    entry.getActionType(),
                    entry.getDimension(),
                    entry.getPosition(),
                    entry.getBlockStateBefore(),
                    entry.getBlockStateAfter(),
                    entry.getItemData()
            );
            logs.add(entryWithId);
            saveLogs();
        }
    }

    public List<LogEntry> lookupByPosition(BlockPos pos, String dimension, int radius, long timeLimitMs) {
        long timeThreshold = System.currentTimeMillis() - timeLimitMs;
        synchronized (fileLock) {
            return logs.stream()
                    .filter(entry -> entry.getDimension().equals(dimension))
                    .filter(entry -> entry.getTimestamp() > timeThreshold)
                    .filter(entry -> {
                        BlockPos entryPos = entry.getPosition();
                        return entryPos != null &&
                                Math.abs(entryPos.getX() - pos.getX()) <= radius &&
                                Math.abs(entryPos.getY() - pos.getY()) <= radius &&
                                Math.abs(entryPos.getZ() - pos.getZ()) <= radius;
                    })
                    .sorted((e1, e2) -> Long.compare(e2.getTimestamp(), e1.getTimestamp()))
                    .limit(100)
                    .collect(Collectors.toList());
        }
    }

    public List<LogEntry> lookupByPlayer(String playerName, long timeLimitMs) {
        long timeThreshold = System.currentTimeMillis() - timeLimitMs;
        synchronized (fileLock) {
            return logs.stream()
                    .filter(entry -> entry.getPlayerName().equalsIgnoreCase(playerName))
                    .filter(entry -> entry.getTimestamp() > timeThreshold)
                    .sorted((e1, e2) -> Long.compare(e2.getTimestamp(), e1.getTimestamp()))
                    .limit(100)
                    .collect(Collectors.toList());
        }
    }

    public List<LogEntry> lookupAtExactPosition(BlockPos pos, String dimension) {
        synchronized (fileLock) {
            return logs.stream()
                    .filter(entry -> entry.getDimension().equals(dimension))
                    .filter(entry -> {
                        BlockPos entryPos = entry.getPosition();
                        return entryPos != null &&
                                entryPos.getX() == pos.getX() &&
                                entryPos.getY() == pos.getY() &&
                                entryPos.getZ() == pos.getZ();
                    })
                    .sorted((e1, e2) -> Long.compare(e2.getTimestamp(), e1.getTimestamp()))
                    .limit(20)
                    .collect(Collectors.toList());
        }
    }

    public int prune(long timeLimitMs) {
        synchronized (fileLock) {
            long threshold = System.currentTimeMillis() - timeLimitMs;
            int initialSize = logs.size();
            logs.removeIf(entry -> entry.getTimestamp() < threshold);
            saveLogs();
            return initialSize - logs.size();
        }
    }

    public long getLogCount() {
        synchronized (fileLock) {
            return logs.size();
        }
    }

    public long getFileSize() {
        return logFile.length();
    }

    public void close() {
        synchronized (fileLock) {
            saveLogs();
            logs.clear();
            LOGGER.info("[AquaGuard] Log file saved and closed.");
        }
    }
}