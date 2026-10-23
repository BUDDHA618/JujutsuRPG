package com.buddha618.jujutsurpg.client;

import com.buddha618.jujutsurpg.network.CharacterNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public final class CharacterScreen extends Screen {
    CharacterNetwork.Profile profile;
    public CharacterScreen() { super(Component.literal("Характеристики мага")); }
    @Override protected void init() {
        addRenderableWidget(Button.builder(Component.literal("Закрыть"), button -> onClose())
            .bounds(width / 2 - 60, Math.min(height - 26, height / 2 + 110), 120, 20).build());
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int left = Math.max(8, width / 2 - 160), top = Math.max(8, height / 2 - 125);
        int right = width - left;
        graphics.fill(left, top, right, top + 225, 0xF015192B);
        graphics.fill(left, top, left + 3, top + 225, 0xFF9D80FF);
        graphics.drawCenteredString(font, title, width / 2, top + 16, 0xD4C4FF);
        if (profile == null) {
            graphics.drawCenteredString(font, "Ожидание профиля с сервера…", width / 2, top + 65, 0xBBBBCC);
        } else {
            String technique = switch (profile.technique()) {
                case "limitless" -> "Безграничность";
                case "ten_shadows" -> "Десять теней";
                case "projection_sorcery" -> "Проекционная магия";
                case "blood_manipulation" -> "Управление кровью";
                default -> "Нет";
            };
            String[] rows = {"Клан: " + profile.clan(), "Техника: " + technique,
                "Генетический признак: в разработке", "Энергия: —", "Уровень: —", "Ранг: —"};
            for (int i = 0; i < rows.length; i++) {
                int y = top + 47 + i * 27;
                graphics.fill(left + 12, y - 5, right - 12, y + 17, 0xFF22283E);
                graphics.drawString(font, rows[i], left + 20, y, i < 2 ? 0xFFFFFF : 0xA4ABC2, false);
            }
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    @Override public boolean isPauseScreen() { return false; }
}
