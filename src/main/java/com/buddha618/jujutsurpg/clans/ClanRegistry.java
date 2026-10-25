package com.buddha618.jujutsurpg.clans;

import com.buddha618.jujutsurpg.genetics.*;
import java.util.*;
import static com.buddha618.jujutsurpg.clans.ClanDefinition.DrillAction.*;

/** Add a definition here; selection, inheritance, UI and training read the same registry. */
public final class ClanRegistry {
    private static final Map<String, ClanDefinition> DEFINITIONS = new LinkedHashMap<>();
    private static ClanDefinition.School school(String id, String name, int level, int reputation) {
        return new ClanDefinition.School(id, name, level, reputation, "", true);
    }
    private static ClanDefinition.School innate(String id, String name, String technique) {
        return new ClanDefinition.School(id, name, 40, 40, technique, false);
    }
    private static ClanDefinition.Inheritance inherited(InnateTechnique t, int chance) { return new ClanDefinition.Inheritance(t, chance); }
    private static void register(ClanDefinition d) {
        if (DEFINITIONS.putIfAbsent(d.clan().id(), d) != null) throw new IllegalArgumentException("Duplicate clan");
    }
    static {
        register(new ClanDefinition(Clan.GOJO, "Великий клан", "Школа точного управления CE. Выбор даёт право на сезонный отбор Шести глаз, но не сам признак.",
            "Точность и дисциплина контроля.", "Безграничность и Шесть глаз не гарантированы. Нет стартовых бонусов к CE.",
            List.of(inherited(InnateTechnique.LIMITLESS,20)),
            List.of(school("precision","Точный контроль",1,0),school("barrier","Барьерная школа",20,20),innate("space","Врождённая пространственная техника","limitless")),
            "Концентрация по опорным точкам",List.of(FOCUS,SNEAK,FOCUS,EMPTY_OFFHAND)));
        register(new ClanDefinition(Clan.ZENIN,"Великий клан","Боевые дисциплины и работа с оружием. Ритуалы шикигами доступны лишь совместимым наследникам.",
            "Физическая подготовка и позиционирование.","Выбор не гарантирует Десять теней или Небесное ограничение.",
            List.of(inherited(InnateTechnique.TEN_SHADOWS,10),inherited(InnateTechnique.PROJECTION_SORCERY,20)),
            List.of(school("weapon","Оружейная школа",1,0),school("speed","Скоростной бой",20,20),innate("shadows","Наследственная школа","ten_shadows")),
            "Смена боевых стоек",List.of(WEAPON,SPRINT,SNEAK,WEAPON)));
        register(new ClanDefinition(Clan.KAMO,"Великий клан","Школа точности и управления ресурсами. Кровавые приёмы требуют отдельной врождённой техники.",
            "Выдержка и экономное использование энергии.","Кровь не создаётся бесконечно из CE; кровавые приёмы пока закрыты.",
            List.of(inherited(InnateTechnique.BLOOD_MANIPULATION,40)),
            List.of(school("resources","Контроль ресурсов",1,0),school("ranged","Дистанционная школа",20,20),innate("blood","Манипуляция кровью","blood_manipulation")),
            "Контроль циркуляции",List.of(SNEAK,EMPTY_OFFHAND,FOCUS,GROUNDED)));
        register(new ClanDefinition(Clan.INUMAKI,"Известная семья","Точное дозирование воздействия и поддержка. Проклятая речь наследуется отдельно; произвольный чат не является атакой.",
            "Дисциплина и контроль воздействия.","Нет бесплатного контроля целей. Боевые команды речи пока не реализованы.",
            List.of(inherited(InnateTechnique.CURSED_SPEECH,30)),
            List.of(school("discipline","Управление воздействием",1,0),school("support","Поддержка союзников",20,20),innate("speech","Проклятая речь","cursed_speech")),
            "Дисциплина импульса",List.of(UNARMED,SNEAK,EMPTY_OFFHAND,FOCUS)));
        register(new ClanDefinition(Clan.FUJIWARA,"Историческая линия — игровая адаптация","Восстановленная школа разведки и тактики. Уро не делает Манипуляцию небом семейной техникой Фудзивара.",
            "Наблюдение и смена позиции.","Нет гарантированной наследственной техники или бонуса урона группе.",List.of(),
            List.of(school("scout","Разведка",1,0),school("team","Командное взаимодействие",20,20),school("position","Тактическая позиция",40,40)),
            "Маршрут разведчика",List.of(SNEAK,LOOK_UP,SPRINT,FOCUS)));
        register(new ClanDefinition(Clan.ABE,"Историческая линия — игровая адаптация","Авторская школа защитных печатей. Это учебный путь JujutsuRPG, а не объявленная каноном наследственная техника.",
            "Защитная дисциплина и устойчивые позиции.","Не аннулирует чужие техники. Настоящие барьеры пока в разработке.",List.of(),
            List.of(school("guard","Защитные барьеры",1,0),school("seal","Подавление проклятий",20,20),school("protection","Защитная поддержка",40,40)),
            "Четыре опоры защиты",List.of(GROUNDED,SNEAK,FOCUS,GROUNDED)));
        register(new ClanDefinition(Clan.SUGAWARA,"Историческая линия — игровая адаптация","Самостоятельная современная школа этой линии — игровая реконструкция. Изучение общих энергетических дисциплин.",
            "Исследование энергии и общие упражнения.","Не выдаёт Копирование, Рику, Безграничность или Шесть глаз.",List.of(),
            List.of(school("research","Исследование CE",1,0),school("general","Общие техники",20,20),school("advanced_barrier","Барьерные дисциплины",40,40)),
            "Исследование энергетических точек",List.of(LOOK_UP,FOCUS,EMPTY_OFFHAND,SNEAK)));
        register(new ClanDefinition(Clan.NONE,"Независимый путь","Полноценное происхождение без великой семьи. Самостоятельная техника определяется отдельно с шансом75%.",
            "Самостоятельные исследования и общая боевая подготовка.","Нет доступа ко всем закрытым школам семей одновременно.",List.of(),
            List.of(school("field","Универсальная практика",1,0),school("arms","Оружие",20,20),school("discovery","Индивидуальное исследование",40,40)),
            "Полевой маршрут мага",List.of(SPRINT,UNARMED,GROUNDED,FOCUS)));
    }
    public static List<ClanDefinition> all() { return List.copyOf(DEFINITIONS.values()); }
    public static Optional<ClanDefinition> find(String id) { return Optional.ofNullable(DEFINITIONS.get(id)); }
    private ClanRegistry() {}
}
