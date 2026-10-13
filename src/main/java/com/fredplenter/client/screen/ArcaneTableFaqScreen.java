package com.fredplenter.client.screen;

import com.fredplenter.aspect.Aspect;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class ArcaneTableFaqScreen extends Screen {

    private static final List<Section> SECTIONS = List.of(
            new Section("gui.fredplenter.faq_purpose", "gui.fredplenter.faq_purpose_desc"),
            new Section("gui.fredplenter.faq_get_note", "gui.fredplenter.faq_get_note_desc"),
            new Section("gui.fredplenter.faq_solve", "gui.fredplenter.faq_solve_desc"),
            new Section("gui.fredplenter.faq_example", "gui.fredplenter.faq_example_desc"),
            new Section("gui.fredplenter.faq_combine", "gui.fredplenter.faq_combine_desc"),
            new Section("gui.fredplenter.faq_discover", "gui.fredplenter.faq_discover_desc")
    );

    private final Screen parent;
    private int scrollOffset;
    private int maxScroll;
    private int contentHeight;
    private int bodyHeight;
    private int panelLeft;
    private int panelTop;
    private int panelWidth;
    private int panelHeight;

    public ArcaneTableFaqScreen(Screen parent) {
        super(Component.translatable("gui.fredplenter.faq_title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(420, width - 20);
        panelHeight = Math.min(330, height - 20);
        panelLeft = (width - panelWidth) / 2;
        panelTop = (height - panelHeight) / 2;
        addRenderableWidget(Button.builder(Component.translatable("gui.fredplenter.faq_close"),
                        ignored -> minecraft.setScreen(parent))
                .pos(width / 2 - 42, panelTop + panelHeight - 25)
                .size(84, 18)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        drawPanel(graphics);
        int bodyLeft = panelLeft + 12;
        int bodyTop = panelTop + 34;
        int bodyWidth = panelWidth - 24;
        bodyHeight = panelHeight - 66;
        graphics.enableScissor(bodyLeft, bodyTop, bodyLeft + bodyWidth, bodyTop + bodyHeight);
        int y = bodyTop + 4 - scrollOffset;
        for (int index = 0; index < SECTIONS.size(); index++) {
            Section section = SECTIONS.get(index);
            y = drawSection(graphics, section, bodyLeft, y, bodyWidth);
            if (index == 2) {
                y = drawPathDiagram(graphics, bodyLeft, y + 2, bodyWidth) + 5;
            }
            y += 4;
        }
        graphics.disableScissor();

        contentHeight = y - (bodyTop + 4 - scrollOffset);
        maxScroll = Math.max(0, contentHeight - bodyHeight);
        scrollOffset = Math.min(scrollOffset, maxScroll);
        if (maxScroll > 0) {
            drawScrollbar(graphics, bodyLeft + bodyWidth - 2, bodyTop, bodyHeight);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawPanel(GuiGraphics graphics) {
        graphics.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + panelHeight, 0xFF170E0A);
        graphics.fill(panelLeft + 2, panelTop + 2, panelLeft + panelWidth - 2,
                panelTop + panelHeight - 2, 0xFF60402A);
        graphics.fill(panelLeft + 5, panelTop + 5, panelLeft + panelWidth - 5,
                panelTop + panelHeight - 5, 0xFF342319);
        graphics.fill(panelLeft + 8, panelTop + 8, panelLeft + panelWidth - 8,
                panelTop + panelHeight - 8, 0xFFDCC99A);
        graphics.drawCenteredString(font, Component.translatable("gui.fredplenter.faq_title"),
                width / 2, panelTop + 13, 0xFF41301C);
        graphics.drawCenteredString(font, Component.translatable("gui.fredplenter.faq_scroll_hint"),
                width / 2, panelTop + 23, 0xFF5A4028);
    }

    private int drawSection(GuiGraphics graphics, Section section, int x, int y, int width) {
        graphics.drawString(font, Component.translatable(section.titleKey()), x, y, 0xFF513718, false);
        y += 10;
        for (var line : font.split(Component.translatable(section.bodyKey()), width - 4)) {
            graphics.drawString(font, line, x + 3, y, 0xFF382A1D, false);
            y += 9;
        }
        return y;
    }

    private int drawPathDiagram(GuiGraphics graphics, int x, int y, int width) {
        int centerY = y + 12;
        int[] centers = {x + width / 6, x + width / 2, x + width * 5 / 6};
        graphics.fill(centers[0] + 7, centerY - 1, centers[1] - 7, centerY + 1, 0xFF60402A);
        graphics.fill(centers[1] + 7, centerY - 1, centers[2] - 7, centerY + 1, 0xFF60402A);
        drawAspectBadge(graphics, centers[0], centerY, Aspect.FLATUS, "1");
        drawAspectBadge(graphics, centers[1], centerY, Aspect.PROCELLA, "2");
        drawAspectBadge(graphics, centers[2], centerY, Aspect.UNDA, "3");

        Component example = Component.translatable("gui.fredplenter.faq_diagram_path",
                Component.translatable(Aspect.FLATUS.getTranslationKey()),
                Component.translatable(Aspect.PROCELLA.getTranslationKey()),
                Component.translatable(Aspect.UNDA.getTranslationKey()));
        int labelY = centerY + 11;
        for (var line : font.split(example, width - 4)) {
            graphics.drawString(font, line, x + 2, labelY, 0xFF382A1D, false);
            labelY += 9;
        }
        return labelY;
    }

    private void drawAspectBadge(GuiGraphics graphics, int centerX, int centerY, Aspect aspect, String number) {
        int color = aspect.getColor();
        graphics.fill(centerX - 7, centerY - 7, centerX + 8, centerY + 8, 0xFF38271B);
        graphics.fill(centerX - 5, centerY - 5, centerX + 6, centerY + 6,
                0xFF000000 | color);
        graphics.drawCenteredString(font, number, centerX, centerY - 3, 0xFFFFFFFF);
    }

    private void drawScrollbar(GuiGraphics graphics, int x, int y, int height) {
        graphics.fill(x, y, x + 2, y + height, 0xFF9A754C);
        int thumbHeight = Math.max(12, height * height / Math.max(height, contentHeight));
        int thumbY = y + (height - thumbHeight) * scrollOffset / maxScroll;
        graphics.fill(x, thumbY, x + 2, thumbY + thumbHeight, 0xFF60402A);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (maxScroll > 0) {
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) (scrollY * 16)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record Section(String titleKey, String bodyKey) {
    }
}
