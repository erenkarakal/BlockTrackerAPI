package me.eren.blocktrackerapi.listener;

import me.eren.blocktrackerapi.tracker.BlockTrackerImpl;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public final class BlockListener implements Listener {

	private final BlockTrackerImpl tracker;

	public BlockListener(BlockTrackerImpl tracker) {
		this.tracker = tracker;
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockPlace(BlockPlaceEvent event) {
		tracker.setNatural(event.getBlockPlaced(), false);
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event) {
		tracker.setNatural(event.getBlock(), true);
	}

}
