package me.eren.blocktrackerapi.storage;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public final class PdcTrackerStorage implements TrackerStorage {

	private static final String PREFIX = "sec_";

	private final Plugin plugin;
	private final String namespace;
	private final Logger logger;
	private final Map<Integer, NamespacedKey> keyCache = new ConcurrentHashMap<>();

	public PdcTrackerStorage(Plugin plugin, Logger logger) {
		this.plugin = plugin;
		this.namespace = plugin.getName().toLowerCase(Locale.ROOT);
		this.logger = logger;
	}

	private NamespacedKey key(int sectionY) {
		return keyCache.computeIfAbsent(sectionY, y -> new NamespacedKey(plugin, PREFIX + y));
	}

	@Override
	public Map<Integer, long[]> loadChunk(Chunk chunk) {
		Map<Integer, long[]> sections = new HashMap<>();
		PersistentDataContainer container = chunk.getPersistentDataContainer();
		for (NamespacedKey key : container.getKeys()) {
			if (!namespace.equals(key.getNamespace()) || !key.getKey().startsWith(PREFIX)) {
				continue;
			}
			int sectionY;
			try {
				sectionY = Integer.parseInt(key.getKey().substring(PREFIX.length()));
			} catch (NumberFormatException e) {
				continue;
			}
			byte[] data = container.get(key, PersistentDataType.BYTE_ARRAY);
			if (data != null) {
				sections.put(sectionY, SectionCodec.decode(data));
			}
		}
		return sections;
	}

	@Override
	public LongOpenHashSet loadModifiedChunks() {
		return null;
	}

	@Override
	public void writeSections(Chunk chunk, List<SectionWrite> sections) {
		if (chunk == null) {
			logger.warning("Cannot write block data: chunk is not loaded");
			return;
		}
		PersistentDataContainer container = chunk.getPersistentDataContainer();
		for (SectionWrite section : sections) {
			NamespacedKey key = key(section.sectionY());
			if (section.words() == null) {
				container.remove(key);
			} else {
				container.set(key, PersistentDataType.BYTE_ARRAY, SectionCodec.encode(section.words()));
			}
		}
	}

	@Override
	public void close() {
	}

	@Override
	public String toString() {
		return "PdcTrackerStorage{namespace=" + namespace + "}";
	}

}
