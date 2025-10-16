package net.denfry.aquaguard;

import com.mojang.logging.LogUtils;
import net.denfry.aquaguard.commands.InspectCommand;
import net.denfry.aquaguard.commands.LookupCommand;
import net.denfry.aquaguard.commands.RollbackCommand;
import net.denfry.aquaguard.commands.PruneCommand;
import net.denfry.aquaguard.commands.StatusCommand;
import net.denfry.aquaguard.database.DatabaseManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.File;

@Mod(AquaGuard.MODID)
public class AquaGuard {

    public static final String MODID = "aquaguard";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static DatabaseManager databaseManager;

    public AquaGuard() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        MinecraftForge.EVENT_BUS.register(this);

        LOGGER.info("[AquaGuard] Mod constructor called");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("[AquaGuard] Initializing...");

        File configDir = FMLPaths.CONFIGDIR.get().toFile();
        File aquaguardDir = new File(configDir, "aquaguard");
        if (!aquaguardDir.exists() && !aquaguardDir.mkdirs()) {
            LOGGER.error("[AquaGuard] Failed to create config directory: {}", aquaguardDir.getAbsolutePath());
        }

        File dbFile = new File(aquaguardDir, "aquaguard_logs.json");
        try {
            databaseManager = new DatabaseManager(dbFile);
            databaseManager.initialize();
            LOGGER.info("[AquaGuard] Database initialized at {}", dbFile.getAbsolutePath());
        } catch (Exception e) {
            LOGGER.error("[AquaGuard] Failed to initialize database!", e);
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("[AquaGuard] Server is starting...");
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        LOGGER.info("[AquaGuard] AquaGuard is now active and logging!");
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        LOGGER.info("[AquaGuard] Server stopping, closing database...");
        if (databaseManager != null) {
            try {
                databaseManager.close();
                LOGGER.info("[AquaGuard] Database closed successfully.");
            } catch (Exception e) {
                LOGGER.error("[AquaGuard] Error while closing database!", e);
            }
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        try {
            InspectCommand.register(event.getDispatcher());
            LookupCommand.register(event.getDispatcher());
            RollbackCommand.register(event.getDispatcher());
            PruneCommand.register(event.getDispatcher());
            StatusCommand.register(event.getDispatcher());
            LOGGER.info("[AquaGuard] Commands registered successfully.");
        } catch (Exception e) {
            LOGGER.error("[AquaGuard] Failed to register commands!", e);
        }
    }

    public static DatabaseManager getDatabaseManager() {
        if (databaseManager == null) {
            LOGGER.warn("[AquaGuard] DatabaseManager accessed before initialization!");
        }
        return databaseManager;
    }
}