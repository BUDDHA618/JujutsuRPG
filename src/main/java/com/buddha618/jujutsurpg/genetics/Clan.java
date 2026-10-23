
package com.buddha618.jujutsurpg.genetics;

public enum Clan {

    GOJO("gojo", "Годжо"),
    ZENIN("zenin", "Зенин"),
    KAMO("kamo", "Камо"),
    NONE("none", "Без великого клана");

    private final String id;
    private final String displayName;

    Clan(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }
}
