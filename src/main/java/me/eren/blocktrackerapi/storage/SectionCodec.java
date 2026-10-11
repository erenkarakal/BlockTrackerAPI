package me.eren.blocktrackerapi.storage;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class SectionCodec {

	public static final int WORDS = 64;
	public static final int BYTES = WORDS * Long.BYTES;

	private SectionCodec() {
	}

	public static byte[] encode(long[] words) {
		ByteBuffer buffer = ByteBuffer.allocate(BYTES).order(ByteOrder.LITTLE_ENDIAN);
		for (int i = 0; i < WORDS; i++) {
			buffer.putLong(words[i]);
		}
		return buffer.array();
	}

	public static long[] decode(byte[] data) {
		if (data.length != BYTES) {
			throw new IllegalArgumentException("Expected " + BYTES + " bytes but got " + data.length);
		}
		long[] words = new long[WORDS];
		ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
		for (int i = 0; i < WORDS; i++) {
			words[i] = buffer.getLong();
		}
		return words;
	}

	public static int bitCount(long[] words) {
		int count = 0;
		for (long word : words) {
			count += Long.bitCount(word);
		}
		return count;
	}

}
