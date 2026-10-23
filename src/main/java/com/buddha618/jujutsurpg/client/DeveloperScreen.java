package com.buddha618.jujutsurpg.client;

import com.buddha618.jujutsurpg.genetics.Clan;
import com.buddha618.jujutsurpg.genetics.InnateTechnique;
import com.buddha618.jujutsurpg.network.CharacterNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Operator sandbox. Selection is a draft until Apply; server validates every request. */
public final class DeveloperScreen extends Screen {
    private int clan, technique;
    private boolean awakened = true;
    private Boolean infinite;
    public DeveloperScreen() {
        super(Component.literal("Меню разработчика"));
        send("state");
    }
    public void receiveInfinite(boolean enabled) { infinite = enabled; rebuildWidgets(); }
    private void button(String text, int y, Runnable action) {
        addRenderableWidget(Button.builder(Component.literal(text), b -> action.run())
            .bounds(width / 2 - 145, y, 290, 20).build());
    }
    @Override protected void init() {
        int y = Math.max(32, height / 2 - 87);
        button("Клан: " + Clan.values()[clan].getDisplayName(), y, () -> { clan = (clan + 1) % Clan.values().length; rebuildWidgets(); });
        button("Техника: " + InnateTechnique.values()[technique].getId(), y + 24, () -> { technique = (technique + 1) % InnateTechnique.values().length; rebuildWidgets(); });
        button("Пробуждение: " + (awakened ? "да" : "нет"), y + 48, () -> { awakened = !awakened; rebuildWidgets(); });
        button("Применить к своему персонажу", y + 72, () -> send("profile"));
        button("Восстановить ПЭ / снять перегрузку", y + 96, () -> send("restore"));
        button("Бесконечная ПЭ: " + (infinite == null ? "…" : infinite ? "ВКЛ" : "ВЫКЛ"), y + 120, () -> {
            if (infinite != null) { infinite = null; send("infinite"); rebuildWidgets(); }
        });
        button("Назад", y + 144, () -> { minecraft.setScreen(new CharacterScreen()); CharacterNetwork.CHANNEL.sendToServer(new CharacterNetwork.Request()); });
    }
    private void send(String action) {
        CharacterNetwork.CHANNEL.sendToServer(new CharacterNetwork.Developer(action,
            Clan.values()[clan].getId(), InnateTechnique.values()[technique].getId(), awakened));
    }
    @Override public void render(GuiGraphics g, int x, int y, float partial) {
        renderBackground(g);
        g.drawCenteredString(font, title, width / 2, 10, 0xFFD080);
        g.drawCenteredString(font, "DEV: меняет сохранение! Безграничность: Z — Синий, X — Фиолетовый.", width / 2, 22, 0xFFFFFF);
        super.render(g, x, y, partial);
    }
    @Override public boolean isPauseScreen() { return false; }
}
