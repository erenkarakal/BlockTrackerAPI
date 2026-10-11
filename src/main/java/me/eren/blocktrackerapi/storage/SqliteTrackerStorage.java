package me.eren.blocktrackerapi.storage;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import me.eren.blocktrackerapi.util.ChunkKey;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public final class SqliteTrackerStorage implements TrackerStorage {

	static {
		try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	private static final String CREATE_TABLE = """
			CREATE TABLE IF NOT EXISTS section_flags (
			    chunk_x   INTEGER NOT NULL,
			    chunk_z   INTEGER NOT NULL,
			    section_y INTEGER NOT NULL,
			    flags     BLOB    NOT NULL,
			    PRIMARY KEY (chunk_x, chunk_z, section_y)
			) WITHOUT ROWID
			""";

	private final File file;
	private final Connection connection;
	private final PreparedStatement selectChunk;
	private final PreparedStatement selectModifiedChunks;
	private final PreparedStatement upsertSection;
	private final PreparedStatement deleteSection;
	private boolean closed;

	public SqliteTrackerStorage(File file, Logger logger) throws SQLException {
		this.file = file;
		this.connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
		try (Statement statement = connection.createStatement()) {
			statement.execute("PRAGMA synchronous = NORMAL");
			statement.execute("PRAGMA busy_timeout = 5000");
			statement.execute("PRAGMA temp_store = MEMORY");
			statement.execute("PRAGMA mmap_size = 268435456");
			statement.execute("PRAGMA cache_size = -65536");
			statement.execute("PRAGMA wal_autocheckpoint = 1000");
			statement.execute("PRAGMA journal_size_limit = 67108864");
			statement.execute(CREATE_TABLE);

			String journalMode;
			try (ResultSet result = statement.executeQuery("PRAGMA journal_mode = WAL")) {
				journalMode = result.next() ? result.getString(1) : "unknown";
			}
			if (!"wal".equalsIgnoreCase(journalMode)) {
				logger.warning("SQLite WAL mode could not be enabled for " + file.getName()
						+ " (journal_mode = " + journalMode + "); performance may suffer.");
			}
		}
		this.selectChunk = connection.prepareStatement(
				"SELECT section_y, flags FROM section_flags WHERE chunk_x = ? AND chunk_z = ?");
		this.selectModifiedChunks = connection.prepareStatement(
				"SELECT DISTINCT chunk_x, chunk_z FROM section_flags");
		this.upsertSection = connection.prepareStatement(
				"INSERT INTO section_flags (chunk_x, chunk_z, section_y, flags) VALUES (?, ?, ?, ?) "
						+ "ON CONFLICT (chunk_x, chunk_z, section_y) DO UPDATE SET flags = excluded.flags");
		this.deleteSection = connection.prepareStatement(
				"DELETE FROM section_flags WHERE chunk_x = ? AND chunk_z = ? AND section_y = ?");
	}

	@Override
	public Map<Integer, long[]> loadChunk(int chunkX, int chunkZ) throws SQLException {
		Map<Integer, long[]> sections = new HashMap<>();
		selectChunk.setInt(1, chunkX);
		selectChunk.setInt(2, chunkZ);
		try (ResultSet result = selectChunk.executeQuery()) {
			while (result.next()) {
				sections.put(result.getInt(1), SectionCodec.decode(result.getBytes(2)));
			}
		}
		return sections;
	}

	@Override
	public LongOpenHashSet loadModifiedChunks() throws SQLException {
		LongOpenHashSet chunks = new LongOpenHashSet();
		try (ResultSet result = selectModifiedChunks.executeQuery()) {
			while (result.next()) {
				chunks.add(ChunkKey.of(result.getInt(1), result.getInt(2)));
			}
		}
		return chunks;
	}

	@Override
	public void writeSections(int chunkX, int chunkZ, List<SectionWrite> sections) throws SQLException {
		boolean autoCommit = connection.getAutoCommit();
		connection.setAutoCommit(false);
		try {
			for (SectionWrite section : sections) {
				if (section.words() == null) {
					deleteSection.setInt(1, chunkX);
					deleteSection.setInt(2, chunkZ);
					deleteSection.setInt(3, section.sectionY());
					deleteSection.addBatch();
				} else {
					upsertSection.setInt(1, chunkX);
					upsertSection.setInt(2, chunkZ);
					upsertSection.setInt(3, section.sectionY());
					upsertSection.setBytes(4, SectionCodec.encode(section.words()));
					upsertSection.addBatch();
				}
			}
			deleteSection.executeBatch();
			upsertSection.executeBatch();
			connection.commit();
		} catch (SQLException e) {
			connection.rollback();
			throw e;
		} finally {
			connection.setAutoCommit(autoCommit);
		}
	}

	@Override
	public void close() {
		closed = true;
		try {
			connection.close();
		} catch (SQLException ignored) {
		}
	}

	@Override
	public String toString() {
		return "SqliteTrackerStorage{file=" + file.getPath() + ", closed=" + closed + "}";
	}

}
