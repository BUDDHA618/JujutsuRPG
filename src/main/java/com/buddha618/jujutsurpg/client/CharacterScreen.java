package com.buddha618.jujutsurpg.client;

import com.buddha618.jujutsurpg.network.CharacterNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public final class CharacterScreen extends Screen {
    CharacterNetwork.Profile profile;
    CharacterNetwork.Energy energy;
    private int refreshTicks;
    @Override public void tick() {
        if (++refreshTicks % 20 == 0) CharacterNetwork.CHANNEL.sendToServer(new CharacterNetwork.Request());
    }
    public CharacterScreen() { super(Component.literal("Характеристики мага")); }
    @Override protected void init() {
        int top = Math.max(8, height / 2 - 125);
        String[] modes = {"Баланс", "Атака", "Защита"};
        for (int i = 0; i < modes.length; i++) {
            final int mode = i + 1;
            addRenderableWidget(Button.builder(Component.literal(modes[i]), button -> reinforce(mode))
                .bounds(width / 2 - 148 + i * 100, top + 185, 96, 20)
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("Постоянный расход ПЭ. Повторное нажатие выключает."))).build());
        }
        if (net.minecraft.client.Minecraft.getInstance().player != null
                && net.minecraft.client.Minecraft.getInstance().player.hasPermissions(2)) {
            addRenderableWidget(Button.builder(Component.literal("DEV"), button ->
                net.minecraft.client.Minecraft.getInstance().setScreen(new DeveloperScreen()))
                .bounds(width / 2 + 94, Math.min(height - 26, top + 210), 54, 20).build());
        }
        addRenderableWidget(Button.builder(Component.literal("Школа клана"), button -> {
            minecraft.setScreen(new ClanScreen());
            com.buddha618.jujutsurpg.clans.ClanNetwork.CHANNEL.sendToServer(new com.buddha618.jujutsurpg.clans.ClanNetwork.Request());
        }).bounds(width / 2 - 148, Math.min(height - 26, top + 210), 84, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Закрыть"), button -> onClose())
            .bounds(width / 2 - 60, Math.min(height - 26, top + 210), 120, 20).build());
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int left = Math.max(8, width / 2 - 160), top = Math.max(8, height / 2 - 125);
        int right = width - left;
        graphics.fill(left, top, right, top + 234, 0xF015192B);
        graphics.fill(left, top, left + 3, top + 234, 0xFF9D80FF);
        graphics.drawCenteredString(font, title, width / 2, top + 16, 0xD4C4FF);
        if (profile == null) {
            graphics.drawCenteredString(font, "Ожидание профиля с сервера…", width / 2, top + 65, 0xBBBBCC);
        } else {
            String technique = com.buddha618.jujutsurpg.genetics.InnateTechnique.displayName(profile.technique());
            String[] rows = {"Клан: " + profile.clan(), "Техника: " + technique,
                "Шесть глаз (этот сезон): " + (profile.sixEyes() ? "да" : "нет"), EnergyHud.description(),
                energy == null ? "Ожидание энергетики…" : String.format(java.util.Locale.ROOT,
                    "Выход: %.1f/с • Контроль: %.1f", energy.output(), energy.control()),
                energy == null ? "" : String.format(java.util.Locale.ROOT, "Потери: %.0f%% • Реген.: %.1f/с", energy.loss()*100, energy.regeneration()),
                energy == null ? "" : String.format(java.util.Locale.ROOT, "Напряжение: %.1f • %s", energy.strain(), energy.overloaded()?"Перегрузка":"Норма"),
                ClanClient.view == null ? "Клановое обучение: «Школа клана»" : "Мастерство клана: " + ClanClient.view.level() + " • Репутация: " + ClanClient.view.reputation()};
            for (int i = 0; i < rows.length; i++) {
                int y = top + 47 + i * 17;
                graphics.fill(left + 12, y - 5, right - 12, y + 12, 0xFF22283E);
                graphics.drawString(font, rows[i], left + 20, y, i < 2 ? 0xFFFFFF : 0xA4ABC2, false);
            }
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    private void reinforce(int mode) {
        CharacterNetwork.CHANNEL.sendToServer(new CharacterNetwork.Reinforce(mode));
        onClose();
    }
    @Override public boolean isPauseScreen() { return false; }
}



