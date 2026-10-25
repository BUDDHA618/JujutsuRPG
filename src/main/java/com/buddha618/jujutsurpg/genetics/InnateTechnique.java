
package com.buddha618.jujutsurpg.genetics;

public enum InnateTechnique {

    NONE("none"),
    CURSED_SPEECH("cursed_speech"),
    LIMITLESS("limitless"),
    TEN_SHADOWS("ten_shadows"),
    PROJECTION_SORCERY("projection_sorcery"),
    BLOOD_MANIPULATION("blood_manipulation"),
    RATIO("ratio"), TOOL_MANIPULATION("tool_manipulation"), BLACK_BIRD("black_bird"),
    BOOGIE_WOOGIE("boogie_woogie"), AUSPICIOUS_BEASTS("auspicious_beasts"),
    INVERSE("inverse"), LOVE_RENDEZVOUS("love_rendezvous"), SKY_MANIPULATION("sky_manipulation");

    public static final java.util.List<InnateTechnique> INDEPENDENT = java.util.List.of(
        RATIO, TOOL_MANIPULATION, BLACK_BIRD, BOOGIE_WOOGIE, AUSPICIOUS_BEASTS,
        INVERSE, LOVE_RENDEZVOUS, SKY_MANIPULATION);

    public static String displayName(String id) {
        return switch (id) {
            case "hidden" -> "Не пробуждена";
            case "cursed_speech" -> "Проклятая речь";
            case "none" -> "Нет";
            case "limitless" -> "Безграничность";
            case "ten_shadows" -> "Десять теней";
            case "projection_sorcery" -> "Проекционная техника";
            case "blood_manipulation" -> "Манипуляция кровью";
            case "ratio" -> "Техника пропорции";
            case "tool_manipulation" -> "Управление инструментом";
            case "black_bird" -> "Управление чёрными птицами";
            case "boogie_woogie" -> "Буги-вуги";
            case "auspicious_beasts" -> "Благоприятные звери";
            case "inverse" -> "Инверсия";
            case "love_rendezvous" -> "Любовное рандеву";
            case "sky_manipulation" -> "Манипуляция небом";
            default -> "Неизвестная техника";
        };
    }

    private final String id;

    InnateTechnique(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
