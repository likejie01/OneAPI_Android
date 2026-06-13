package center.oneapi.mobile;

import java.util.Locale;

final class ExtensionRef {
    final String id;
    final String kind;
    final String name;
    final String description;
    final String client;

    ExtensionRef(String id, String kind, String name, String description, String client) {
        this.id = id == null ? "" : id;
        this.kind = kind == null || kind.trim().isEmpty() ? "skill" : kind.trim().toLowerCase(Locale.ROOT);
        this.name = name == null ? this.id : name;
        this.description = description == null ? "" : description;
        this.client = client == null ? "" : client;
    }
}
