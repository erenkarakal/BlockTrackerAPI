package me.eren.blocktrackerapi.listener;

import me.eren.blocktrackerapi.tracker.BlockTrackerImpl;
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
		tracker.loadChunk(event.getChunk());
	}

	@EventHandler
	public void onChunkUnload(ChunkUnloadEvent event) {
		tracker.unloadChunk(event.getChunk());
	}

}
