package com.buddha618.jujutsurpg.genetics;

public enum GeneticTrait {
    NONE("none", "Нет"),
    SIX_EYES("six_eyes", "Шесть глаз"),
    HEAVENLY_RESTRICTION("heavenly_restriction", "Небесное ограничение");

    private final String id;
    private final String displayName;

    GeneticTrait(String id, String displayName) {
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
