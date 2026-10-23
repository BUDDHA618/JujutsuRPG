
package com.buddha618.jujutsurpg.genetics;

public enum InnateTechnique {

    NONE("none"),
    LIMITLESS("limitless"),
    TEN_SHADOWS("ten_shadows"),
    PROJECTION_SORCERY("projection_sorcery"),
    BLOOD_MANIPULATION("blood_manipulation");

    private final String id;

    InnateTechnique(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
