package me.eren.blocktrackerapi.util;

public final class ChunkKey {

	private ChunkKey() {
	}

	public static long of(int chunkX, int chunkZ) {
		return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
	}

	public static int x(long key) {
		return (int) (key >> 32);
	}

	public static int z(long key) {
		return (int) key;
	}

}
