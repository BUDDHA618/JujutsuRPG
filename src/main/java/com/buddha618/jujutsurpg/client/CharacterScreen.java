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
        int top = Math.max(8, height / 2 - 125);
        addRenderableWidget(Button.builder(Component.literal("Усилить силу"), button -> reinforce(false))
            .bounds(width / 2 - 148, top + 185, 144, 20)
            .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("50% текущей ПЭ • Сила I • 10 секунд"))).build());
        addRenderableWidget(Button.builder(Component.literal("Усилить защиту"), button -> reinforce(true))
            .bounds(width / 2 + 4, top + 185, 144, 20)
            .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("50% текущей ПЭ • Сопротивление I • 10 секунд"))).build());
        if (net.minecraft.client.Minecraft.getInstance().player != null
                && net.minecraft.client.Minecraft.getInstance().player.hasPermissions(2)) {
            addRenderableWidget(Button.builder(Component.literal("DEV"), button ->
                net.minecraft.client.Minecraft.getInstance().setScreen(new DeveloperScreen()))
                .bounds(width / 2 + 94, Math.min(height - 26, top + 210), 54, 20).build());
        }
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
            String technique = switch (profile.technique()) {
                case "limitless" -> "Безграничность";
                case "ten_shadows" -> "Десять теней";
                case "projection_sorcery" -> "Проекционная магия";
                case "blood_manipulation" -> "Управление кровью";
                case "hidden" -> "Скрыта до пробуждения";
                default -> "Нет";
            };
            String[] rows = {"Клан: " + profile.clan(), "Техника: " + technique,
                "Генетический признак: в разработке", EnergyHud.description(), "Уровень: —", "Ранг: —"};
            for (int i = 0; i < rows.length; i++) {
                int y = top + 47 + i * 22;
                graphics.fill(left + 12, y - 5, right - 12, y + 17, 0xFF22283E);
                graphics.drawString(font, rows[i], left + 20, y, i < 2 ? 0xFFFFFF : 0xA4ABC2, false);
            }
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    private void reinforce(boolean defense) {
        CharacterNetwork.CHANNEL.sendToServer(new CharacterNetwork.Reinforce(defense));
        onClose();
    }
    @Override public boolean isPauseScreen() { return false; }
}



