package me.eren.blocktrackerapi.storage;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public interface TrackerStorage extends AutoCloseable {

	Map<Integer, long[]> loadChunk(int chunkX, int chunkZ) throws SQLException;

	LongOpenHashSet loadModifiedChunks() throws SQLException;

	void writeSections(int chunkX, int chunkZ, List<SectionWrite> sections) throws SQLException;

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
