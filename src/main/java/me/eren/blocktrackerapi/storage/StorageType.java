package me.eren.blocktrackerapi.storage;

public enum StorageType {

	SQLITE,
	PDC;

	public static StorageType parse(String value) {
		if (value == null) {
			return null;
		}
		for (StorageType type : values()) {
			if (type.name().equalsIgnoreCase(value.trim())) {
				return type;
			}
		}
		return null;
	}

}
