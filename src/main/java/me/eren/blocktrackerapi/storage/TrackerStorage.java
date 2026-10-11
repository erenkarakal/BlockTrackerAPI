package me.eren.blocktrackerapi.storage;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import org.bukkit.Chunk;

import java.util.List;
import java.util.Map;

public interface TrackerStorage extends AutoCloseable {

	Map<Integer, long[]> loadChunk(Chunk chunk);

	/**
	 * @return the modified-chunk index, or {@code null} if this backend has no global index (e.g. PDC).
	 */
	LongOpenHashSet loadModifiedChunks();

	void writeSections(Chunk chunk, List<SectionWrite> sections);

	@Override
	void close();

	record SectionWrite(int sectionY, long[] words) {

		@Override
		public String toString() {
			return words == null
					? "SectionWrite{sectionY=" + sectionY + ", deleted}"
					: "SectionWrite{sectionY=" + sectionY + ", bits=" + SectionCodec.bitCount(words) + "}";
		}

	}

}
