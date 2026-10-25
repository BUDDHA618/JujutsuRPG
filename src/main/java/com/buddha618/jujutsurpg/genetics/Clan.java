package com.buddha618.jujutsurpg.genetics;

/** Stable save IDs. Public definitions and inheritance live in ClanRegistry. */
public record Clan(String id, String displayName) {
    public static final Clan GOJO = new Clan("gojo", "Годжо");
    public static final Clan ZENIN = new Clan("zenin", "Зенин");
    public static final Clan KAMO = new Clan("kamo", "Камо");
    public static final Clan INUMAKI = new Clan("inumaki", "Инумаки");
    public static final Clan FUJIWARA = new Clan("fujiwara", "Фудзивара");
    public static final Clan ABE = new Clan("abe", "Абэ");
    public static final Clan SUGAWARA = new Clan("sugawara", "Линия Сугавары");
    public static final Clan NONE = new Clan("none", "Независимый маг");
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public static Clan[] values() {
        return com.buddha618.jujutsurpg.clans.ClanRegistry.all().stream().map(d -> d.clan()).toArray(Clan[]::new);
    }
}
