package me.eren.blocktrackerapi;

import me.eren.blocktrackerapi.api.BlockTracker;
import me.eren.blocktrackerapi.listener.BlockListener;
import me.eren.blocktrackerapi.listener.ChunkListener;
import me.eren.blocktrackerapi.listener.MechanismListener;
import me.eren.blocktrackerapi.listener.WorldListener;
import me.eren.blocktrackerapi.tracker.BlockTrackerImpl;
import me.eren.blocktrackerapi.tracker.WorldTracker;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

public final class BlockTrackerAPI extends JavaPlugin {

	private static BlockTracker tracker;

	private BlockTrackerImpl impl;

	public static BlockTracker get() {
		return tracker;
	}

	@Override
	public void onEnable() {
		if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
			getLogger().severe("Could not create plugin data folder");
			return;
		}

		impl = new BlockTrackerImpl(getDataFolder(), getLogger());
		tracker = impl;

		for (World world : Bukkit.getWorlds()) {
			WorldTracker worldTracker = impl.loadWorld(world);
			if (worldTracker == null) {
				continue;
			}
			for (Chunk chunk : world.getLoadedChunks()) {
				worldTracker.loadChunk(chunk.getX(), chunk.getZ());
			}
		}

		getServer().getPluginManager().registerEvents(new WorldListener(impl), this);
		getServer().getPluginManager().registerEvents(new ChunkListener(impl), this);
		getServer().getPluginManager().registerEvents(new BlockListener(impl), this);
		getServer().getPluginManager().registerEvents(new MechanismListener(impl), this);
	}

	@Override
	public void onDisable() {
		if (impl != null) {
			impl.closeAll();
			impl = null;
		}
		tracker = null;
	}

}
