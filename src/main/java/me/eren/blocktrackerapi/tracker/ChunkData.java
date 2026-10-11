package me.eren.blocktrackerapi.tracker;

import me.eren.blocktrackerapi.storage.SectionCodec;

public final class ChunkData {

	private final long[][] sections;

	public ChunkData(int sectionCount) {
		this.sections = new long[sectionCount][];
	}

	public int sectionCount() {
		return sections.length;
	}

	public long[] sectionWords(int sectionIndex) {
		return sections[sectionIndex];
	}

	public void setSectionWords(int sectionIndex, long[] words) {
		sections[sectionIndex] = words;
	}

	/**
	 * @return {@code true} when the block is natural (bit 0), {@code false} when it was placed by a player (bit 1)
	 */
	public boolean isNatural(int sectionIndex, int blockIndex) {
		long[] words = sections[sectionIndex];
		if (words == null) {
			return true;
		}
		return (words[blockIndex >>> 6] & (1L << (blockIndex & 63))) == 0;
	}

	/**
	 * @return {@code true} if the stored bit actually changed
	 */
	public boolean setNatural(int sectionIndex, int blockIndex, boolean natural) {
		long[] words = sections[sectionIndex];
		int wordIndex = blockIndex >>> 6;
		long mask = 1L << (blockIndex & 63);

		if (natural) {
			if (words == null || (words[wordIndex] & mask) == 0) {
				return false;
			}
			words[wordIndex] &= ~mask;
			if (isZero(words)) {
				sections[sectionIndex] = null;
			}
			return true;
		}

		if (words == null) {
			words = new long[SectionCodec.WORDS];
			sections[sectionIndex] = words;
		} else if ((words[wordIndex] & mask) != 0) {
			return false;
		}
		words[wordIndex] |= mask;
		return true;
	}

	public boolean isEmpty() {
		for (long[] words : sections) {
			if (words != null) {
				return false;
			}
		}
		return true;
	}

	public static boolean isZero(long[] words) {
		for (long word : words) {
			if (word != 0) {
				return false;
			}
		}
		return true;
	}

	@Override
	public String toString() {
		int populated = 0;
		int bits = 0;
		for (long[] words : sections) {
			if (words != null) {
				populated++;
				bits += SectionCodec.bitCount(words);
			}
		}
		return "ChunkData{sections=" + sections.length + ", populated=" + populated + ", bits=" + bits + "}";
	}

}
