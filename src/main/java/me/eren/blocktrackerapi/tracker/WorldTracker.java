package me.eren.blocktrackerapi.tracker;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import me.eren.blocktrackerapi.storage.TrackerStorage;
import me.eren.blocktrackerapi.storage.TrackerStorage.SectionWrite;
import me.eren.blocktrackerapi.util.ChunkKey;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class WorldTracker {

	private final UUID worldId;
	private final int minSection;
	private final int sectionCount;
	private final TrackerStorage storage;
	private final Logger logger;

	private final Long2ObjectMap<ChunkData> chunks = new Long2ObjectOpenHashMap<>();
	private final LongOpenHashSet loadedChunks = new LongOpenHashSet();
	private final LongOpenHashSet modifiedChunks = new LongOpenHashSet();
	private final Long2ObjectMap<LongOpenHashSet> dirtySections = new Long2ObjectOpenHashMap<>();
	private final boolean filterReady;

	public WorldTracker(UUID worldId, int minSection, int sectionCount, TrackerStorage storage, Logger logger) {
		this.worldId = worldId;
		this.minSection = minSection;
		this.sectionCount = sectionCount;
		this.storage = storage;
		this.logger = logger;
		this.filterReady = loadModifiedChunks();
	}

	private boolean loadModifiedChunks() {
		try {
			modifiedChunks.addAll(storage.loadModifiedChunks());
			return true;
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Failed to load modified chunk index for world " + worldId, e);
			return false;
		}
	}

	public UUID worldId() {
		return worldId;
	}

	public void loadChunk(int chunkX, int chunkZ) {
		long key = ChunkKey.of(chunkX, chunkZ);
		if (!loadedChunks.add(key)) {
			return;
		}
		if (filterReady && !modifiedChunks.contains(key)) {
			return;
		}
		try {
			Map<Integer, long[]> saved = storage.loadChunk(chunkX, chunkZ);
			if (saved.isEmpty()) {
				return;
			}
			ChunkData chunk = new ChunkData(sectionCount);
			boolean any = false;
			for (Map.Entry<Integer, long[]> entry : saved.entrySet()) {
				int relative = entry.getKey() - minSection;
				long[] words = entry.getValue();
				if (relative < 0 || relative >= sectionCount || words == null || ChunkData.isZero(words)) {
					continue;
				}
				chunk.setSectionWords(relative, words);
				any = true;
			}
			if (any) {
				chunks.put(key, chunk);
			}
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Failed to load block data for chunk " + chunkX + ", " + chunkZ, e);
		}
	}

	public void unloadChunk(int chunkX, int chunkZ) {
		long key = ChunkKey.of(chunkX, chunkZ);
		if (!loadedChunks.remove(key)) {
			return;
		}
		flushChunk(key);
		chunks.remove(key);
		dirtySections.remove(key);
	}

	public boolean isNatural(int x, int y, int z) {
		ChunkData chunk = chunks.get(ChunkKey.of(x >> 4, z >> 4));
		if (chunk == null) {
			return true;
		}
		int relative = (y >> 4) - minSection;
		if (relative < 0 || relative >= sectionCount) {
			return true;
		}
		return chunk.isNatural(relative, blockIndex(x, y, z));
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

		ChunkData chunk = chunks.get(key);
		if (chunk == null) {
			if (natural) {
				return;
			}
			chunk = new ChunkData(sectionCount);
			chunks.put(key, chunk);
		}

		if (chunk.setNatural(relative, index, natural)) {
			if (natural) {
				if (chunk.isEmpty()) {
					modifiedChunks.remove(key);
				}
			} else {
				modifiedChunks.add(key);
			}
			dirtySections.computeIfAbsent(key, ignored -> new LongOpenHashSet()).add(relative);
		}
	}

	public void saveAll() {
		for (long key : dirtySections.keySet().toLongArray()) {
			flushChunk(key);
		}
		dirtySections.clear();
	}

	public void close() {
		saveAll();
		storage.close();
	}

	private void flushChunk(long key) {
		LongOpenHashSet dirty = dirtySections.get(key);
		if (dirty == null || dirty.isEmpty()) {
			return;
		}
		ChunkData chunk = chunks.get(key);
		List<SectionWrite> writes = new ArrayList<>(dirty.size());
		for (long relative : dirty) {
			long[] words = chunk == null ? null : chunk.sectionWords((int) relative);
			if (words == null || ChunkData.isZero(words)) {
				writes.add(new SectionWrite((int) relative + minSection, null));
			} else {
				writes.add(new SectionWrite((int) relative + minSection, words));
			}
		}

		int chunkX = ChunkKey.x(key);
		int chunkZ = ChunkKey.z(key);
		try {
			storage.writeSections(chunkX, chunkZ, writes);
			if (chunk == null || chunk.isEmpty()) {
				modifiedChunks.remove(key);
			}
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Failed to save block data for chunk " + chunkX + ", " + chunkZ, e);
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
