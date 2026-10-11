package me.eren.blocktrackerapi.listener;

import me.eren.blocktrackerapi.tracker.BlockTrackerImpl;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

public final class WorldListener implements Listener {

	private final BlockTrackerImpl tracker;

	public WorldListener(BlockTrackerImpl tracker) {
		this.tracker = tracker;
	}

	@EventHandler
	public void onWorldLoad(WorldLoadEvent event) {
		tracker.loadWorld(event.getWorld());
	}

	@EventHandler
	public void onWorldUnload(WorldUnloadEvent event) {
		tracker.unloadWorld(event.getWorld());
	}

}
