package me.eren.blocktrackerapi.tracker;

import me.eren.blocktrackerapi.api.BlockTracker;
import me.eren.blocktrackerapi.storage.SqliteTrackerStorage;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.io.File;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class BlockTrackerImpl implements BlockTracker {

	private final File dataFolder;
	private final Logger logger;
	private final Map<UUID, WorldTracker> trackers = new HashMap<>();

	public BlockTrackerImpl(File dataFolder, Logger logger) {
		this.dataFolder = dataFolder;
		this.logger = logger;
	}

	public WorldTracker loadWorld(World world) {
		WorldTracker existing = trackers.get(world.getUID());
		if (existing != null) {
			return existing;
		}
		File file = new File(dataFolder, world.getUID() + ".db");
		try {
			SqliteTrackerStorage storage = new SqliteTrackerStorage(file, logger);
			WorldTracker tracker = new WorldTracker(world.getUID(), world.getMinHeight() >> 4,
					(world.getMaxHeight() - world.getMinHeight()) >> 4, storage, logger);
			trackers.put(world.getUID(), tracker);
			return tracker;
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Failed to open block database for world " + world.getName(), e);
			return null;
		}
	}

	public void unloadWorld(World world) {
		WorldTracker tracker = trackers.remove(world.getUID());
		if (tracker != null) {
			tracker.close();
		}
	}

	public void closeAll() {
		for (WorldTracker tracker : trackers.values()) {
			tracker.close();
		}
		trackers.clear();
	}

	public void loadChunk(World world, int chunkX, int chunkZ) {
		WorldTracker tracker = trackers.get(world.getUID());
		if (tracker != null) {
			tracker.loadChunk(chunkX, chunkZ);
		}
	}

	public void unloadChunk(World world, int chunkX, int chunkZ) {
		WorldTracker tracker = trackers.get(world.getUID());
		if (tracker != null) {
			tracker.unloadChunk(chunkX, chunkZ);
		}
	}

	@Override
	public void setNatural(World world, int x, int y, int z, boolean natural) {
		WorldTracker tracker = trackers.get(world.getUID());
		if (tracker != null) {
			tracker.setNatural(x, y, z, natural);
		}
	}

	@Override
	public void setNatural(Block block, boolean natural) {
		setNatural(block.getWorld(), block.getX(), block.getY(), block.getZ(), natural);
	}

	@Override
	public void setNatural(Location location, boolean natural) {
		if (location.getWorld() == null) {
			return;
		}
		setNatural(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), natural);
	}

	@Override
	public boolean isNatural(World world, int x, int y, int z) {
		WorldTracker tracker = trackers.get(world.getUID());
		return tracker == null || tracker.isNatural(x, y, z);
	}

	@Override
	public boolean isNatural(Block block) {
		return isNatural(block.getWorld(), block.getX(), block.getY(), block.getZ());
	}

	@Override
	public boolean isNatural(Location location) {
		if (location.getWorld() == null) {
			return true;
		}
		return isNatural(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
	}

	@Override
	public WorldTracker tracker(World world) {
		return trackers.get(world.getUID());
	}

	@Override
	public String toString() {
		return "BlockTrackerImpl{worlds=" + trackers.size() + "}";
	}

}
