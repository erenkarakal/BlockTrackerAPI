package me.eren.blocktrackerapi.tracker;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import me.eren.blocktrackerapi.storage.StorageException;
import me.eren.blocktrackerapi.storage.TrackerStorage;
import me.eren.blocktrackerapi.storage.TrackerStorage.SectionWrite;
import me.eren.blocktrackerapi.util.ChunkKey;
import org.bukkit.Chunk;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class WorldTracker {

	private final World world;
	private final UUID worldId;
	private final int minSection;
	private final int sectionCount;
	private final TrackerStorage storage;
	private final Logger logger;

	private final Long2ObjectMap<ChunkData> chunks = new Long2ObjectOpenHashMap<>();
	private final Long2ObjectMap<Chunk> chunkRefs = new Long2ObjectOpenHashMap<>();
	private final LongOpenHashSet loadedChunks = new LongOpenHashSet();
	private final LongOpenHashSet modifiedChunks = new LongOpenHashSet();
	private final Long2ObjectMap<LongOpenHashSet> dirtySections = new Long2ObjectOpenHashMap<>();
	private final boolean filterReady;

	public WorldTracker(World world, int minSection, int sectionCount, TrackerStorage storage, Logger logger) {
		this.world = world;
		this.worldId = world.getUID();
		this.minSection = minSection;
		this.sectionCount = sectionCount;
		this.storage = storage;
		this.logger = logger;
		this.filterReady = loadModifiedChunks();
	}

	private boolean loadModifiedChunks() {
		LongOpenHashSet index;
		try {
			index = storage.loadModifiedChunks();
		} catch (StorageException e) {
			logger.log(Level.SEVERE, "Failed to load modified chunk index for world " + world.getName(), e);
			return false;
		}
		if (index == null) {
			return false;
		}
		modifiedChunks.addAll(index);
		return true;
	}

	public UUID worldId() {
		return worldId;
	}

	public void loadChunk(Chunk chunk) {
		long key = ChunkKey.of(chunk.getX(), chunk.getZ());
		if (!loadedChunks.add(key)) {
			return;
		}
		chunkRefs.put(key, chunk);
		if (filterReady && !modifiedChunks.contains(key)) {
			return;
		}
		try {
			Map<Integer, long[]> saved = storage.loadChunk(chunk);
			if (saved.isEmpty()) {
				return;
			}
			ChunkData data = new ChunkData(sectionCount);
			boolean any = false;
			for (Map.Entry<Integer, long[]> entry : saved.entrySet()) {
				int relative = entry.getKey() - minSection;
				long[] words = entry.getValue();
				if (relative < 0 || relative >= sectionCount || words == null || ChunkData.isZero(words)) {
					continue;
				}
				data.setSectionWords(relative, words);
				any = true;
			}
			if (any) {
				chunks.put(key, data);
			}
		} catch (StorageException e) {
			logger.log(Level.SEVERE, "Failed to load block data for chunk " + chunk.getX() + ", " + chunk.getZ(), e);
		}
	}

	public void unloadChunk(Chunk chunk) {
		long key = ChunkKey.of(chunk.getX(), chunk.getZ());
		if (!loadedChunks.remove(key)) {
			return;
		}
		flushChunk(key, chunk);
		chunks.remove(key);
		dirtySections.remove(key);
		chunkRefs.remove(key);
	}

	public boolean isNatural(int x, int y, int z) {
		ChunkData data = chunks.get(ChunkKey.of(x >> 4, z >> 4));
		if (data == null) {
			return true;
		}
		int relative = (y >> 4) - minSection;
		if (relative < 0 || relative >= sectionCount) {
			return true;
		}
		return data.isNatural(relative, blockIndex(x, y, z));
	}

	public void setNatural(int x, int y, int z, boolean natural) {
		long key = ChunkKey.of(x >> 4, z >> 4);
		if (!loadedChunks.contains(key)) {
			return;
		}
		int relative = (y >> 4) - minSection;
		if (relative < 0 || relative >= sectionCount) {
			return;
		}
		int index = blockIndex(x, y, z);

		ChunkData data = chunks.get(key);
		if (data == null) {
			if (natural) {
				return;
			}
			data = new ChunkData(sectionCount);
			chunks.put(key, data);
		}

		if (data.setNatural(relative, index, natural)) {
			if (filterReady) {
				if (natural) {
					if (data.isEmpty()) {
						modifiedChunks.remove(key);
					}
				} else {
					modifiedChunks.add(key);
				}
			}
			dirtySections.computeIfAbsent(key, ignored -> new LongOpenHashSet()).add(relative);
		}
	}

	public void saveAll() {
		for (long key : dirtySections.keySet().toLongArray()) {
			Chunk chunk = chunkRefs.get(key);
			if (chunk == null) {
				logger.warning("Skipping unsaved block data for an unloaded chunk in world " + world.getName());
				continue;
			}
			flushChunk(key, chunk);
		}
		dirtySections.clear();
	}

	public void close() {
		saveAll();
		storage.close();
	}

	private void flushChunk(long key, Chunk chunk) {
		LongOpenHashSet dirty = dirtySections.get(key);
		if (dirty == null || dirty.isEmpty()) {
			return;
		}
		ChunkData data = chunks.get(key);
		List<SectionWrite> writes = new ArrayList<>(dirty.size());
		for (long relative : dirty) {
			long[] words = data == null ? null : data.sectionWords((int) relative);
			if (words == null || ChunkData.isZero(words)) {
				writes.add(new SectionWrite((int) relative + minSection, null));
			} else {
				writes.add(new SectionWrite((int) relative + minSection, words));
			}
		}
		try {
			storage.writeSections(chunk, writes);
			if (filterReady && (data == null || data.isEmpty())) {
				modifiedChunks.remove(key);
			}
		} catch (StorageException e) {
			logger.log(Level.SEVERE, "Failed to save block data for chunk "
					+ ChunkKey.x(key) + ", " + ChunkKey.z(key), e);
		}
	}

	private static int blockIndex(int x, int y, int z) {
		return ((y & 15) << 8) | ((z & 15) << 4) | (x & 15);
	}

	@Override
	public String toString() {
		return "WorldTracker{world=" + worldId
				+ ", sections=" + minSection + ".." + (minSection + sectionCount - 1)
				+ ", loaded=" + loadedChunks.size()
				+ ", modified=" + modifiedChunks.size()
				+ ", dirty=" + dirtySections.size()
				+ ", chunks=" + chunks.size()
				+ ", filterReady=" + filterReady
				+ "}";
	}

}
