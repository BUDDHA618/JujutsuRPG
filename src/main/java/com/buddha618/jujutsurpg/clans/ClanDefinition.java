package com.buddha618.jujutsurpg.clans;

import com.buddha618.jujutsurpg.genetics.*;
import java.util.List;
import java.util.random.RandomGenerator;

/** Public educational data; never contains any player's hidden roll. */
public record ClanDefinition(Clan clan, String group, String description, String strengths, String limits,
                             List<Inheritance> inheritance, List<School> schools, String exercise,
                             List<DrillAction> actions) {
    public record Inheritance(InnateTechnique technique, int percent) {}
    public record School(String id, String title, int level, int reputation, String technique, boolean implemented) {}
    public enum DrillAction { FOCUS, SNEAK, UNARMED, WEAPON, SPRINT, LOOK_UP, EMPTY_OFFHAND, GROUNDED }
    public ClanDefinition {
        inheritance = List.copyOf(inheritance); schools = List.copyOf(schools); actions = List.copyOf(actions);
        if (!clan.id().matches("[a-z0-9_]{1,64}") || schools.size() < 3 || schools.size() > 32 || schools.stream().map(School::id).distinct().count()!=schools.size() || inheritance.stream().anyMatch(i -> i.percent()<0 || i.percent()>100) || actions.isEmpty() || inheritance.stream().mapToInt(Inheritance::percent).sum() > 100)
            throw new IllegalArgumentException("Invalid clan definition " + clan.id());
    }
    public InnateTechnique roll(RandomGenerator random) {
        int roll = random.nextInt(100), bound = 0;
        for (var item : inheritance) { bound += item.percent(); if (roll < bound) return item.technique(); }
        return GeneticsSavedData.independentFallback(InnateTechnique.NONE, random);
    }
    public String heritageDescription() {
        return inheritance.isEmpty() ? "Клановая техника не установлена; самостоятельный пул: 75%."
            : "Возможное наследование: " + String.join(", ", inheritance.stream()
                .map(i -> InnateTechnique.displayName(i.technique().getId())).toList()) + ". Не гарантируется.";
    }
}
