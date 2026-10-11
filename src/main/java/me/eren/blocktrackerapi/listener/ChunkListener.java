package me.eren.blocktrackerapi.listener;

import me.eren.blocktrackerapi.tracker.BlockTrackerImpl;
import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

public final class ChunkListener implements Listener {

	private final BlockTrackerImpl tracker;

	public ChunkListener(BlockTrackerImpl tracker) {
		this.tracker = tracker;
	}

	@EventHandler
	public void onChunkLoad(ChunkLoadEvent event) {
		Chunk chunk = event.getChunk();
		tracker.loadChunk(event.getWorld(), chunk.getX(), chunk.getZ());
	}

	@EventHandler
	public void onChunkUnload(ChunkUnloadEvent event) {
		Chunk chunk = event.getChunk();
		tracker.unloadChunk(event.getWorld(), chunk.getX(), chunk.getZ());
	}

}
