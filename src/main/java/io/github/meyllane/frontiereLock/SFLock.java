package io.github.meyllane.frontiereLock;

import dev.jorel.commandapi.CommandAPI;
import io.github.meyllane.frontiereLock.command.FLockCommand;
import io.github.meyllane.frontiereLock.datastruct.LockedBlock;
import io.github.meyllane.frontiereLock.datastruct.LockedBlockRegistry;
import io.github.meyllane.frontiereLock.listener.onPlayerInteractListener;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Chest;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public final class SFLock extends JavaPlugin {
    public static final List<Class<? extends BlockData>> allowedTypes = List.of(Door.class, Chest.class, TrapDoor.class);
    public static File configFile;
    public static YamlConfiguration config;
    public static LockedBlockRegistry lockMap = new LockedBlockRegistry();
    @Override
    public void onEnable() {

        ConfigurationSerialization.registerClass(LockedBlock.class);

        configFile = new File(this.getDataFolder(), "locks.yml");
        if (!configFile.exists()) {
            saveResource("locks.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        //Listener registration
        getServer().getPluginManager().registerEvents(new onPlayerInteractListener(), this);

        CommandAPI.onEnable();

        //Command registration
        FLockCommand.registerCommand();

        //Loading LockedBlocks from configuration
        loadLocks();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public void loadLocks() {
        ConfigurationSection sec = config.getConfigurationSection("locks");

        if (sec == null) return;

        int count = 0;
        for (String key : sec.getKeys(false)) {
            LockedBlock locked = (LockedBlock) config.get("locks." + key);
            lockMap.add(locked.getLoc(), locked);
            count++;
        }

        this.getServer().getLogger().log(Level.INFO, count + " LockedBlocks have been loaded!");
    }

    public static void saveChanges() {
        SFLock.getPlugin(SFLock.class).getServer().getScheduler().runTaskAsynchronously(SFLock.getPlugin(SFLock.class), runner -> {
            try {
                SFLock.config.save(SFLock.configFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
