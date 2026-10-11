package me.eren.blocktrackerapi.listener;

import me.eren.blocktrackerapi.tracker.BlockTrackerImpl;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.ArrayList;
import java.util.List;

public final class MechanismListener implements Listener {

	private final BlockTrackerImpl tracker;

	public MechanismListener(BlockTrackerImpl tracker) {
		this.tracker = tracker;
	}

	@EventHandler(ignoreCancelled = true)
	public void onEntityExplode(EntityExplodeEvent event) {
		clear(event.blockList());
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockExplode(BlockExplodeEvent event) {
		clear(event.blockList());
	}

	@EventHandler(ignoreCancelled = true)
	public void onPistonExtend(BlockPistonExtendEvent event) {
		move(event.getBlocks(), event.getDirection());
	}

	@EventHandler(ignoreCancelled = true)
	public void onPistonRetract(BlockPistonRetractEvent event) {
		move(event.getBlocks(), event.getDirection());
	}

	private void clear(List<Block> blocks) {
		for (Block block : blocks) {
			tracker.setNatural(block, true);
		}
	}

	private void move(List<Block> blocks, BlockFace direction) {
		if (blocks.isEmpty()) {
			return;
		}
		List<Block> destinations = new ArrayList<>(blocks.size());
		List<Boolean> natural = new ArrayList<>(blocks.size());
		for (Block source : blocks) {
			destinations.add(source.getRelative(direction));
			natural.add(tracker.isNatural(source));
		}
		for (Block source : blocks) {
			tracker.setNatural(source, true);
		}
		for (int i = 0; i < destinations.size(); i++) {
			tracker.setNatural(destinations.get(i), natural.get(i));
		}
	}

}
