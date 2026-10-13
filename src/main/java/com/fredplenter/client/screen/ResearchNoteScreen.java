package com.fredplenter.client.screen;

import com.fredplenter.network.LearnNotePayload;
import com.fredplenter.network.RequestProgressPayload;
import com.fredplenter.research.ClientArcaneProgress;
import com.fredplenter.research.Research;
import com.fredplenter.research.ResearchRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

public class ResearchNoteScreen extends Screen {

    private final ResourceLocation researchId;
    private final boolean complete;

    public ResearchNoteScreen(ResourceLocation researchId, boolean complete) {
        super(Component.translatable("item.fredplenter.research_note"));
        this.researchId = researchId;
        this.complete = complete;
    }

    @Override
    protected void init() {
        PacketDistributor.sendToServer(new RequestProgressPayload());
        int centerX = width / 2;
        if (complete) {
            addRenderableWidget(Button.builder(Component.translatable("gui.fredplenter.learn"), button -> {
                PacketDistributor.sendToServer(new LearnNotePayload(researchId));
                onClose();
            }).pos(centerX - 80, height / 2 + 62).size(75, 20).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.fredplenter.cancel"), button -> onClose())
                .pos(complete ? centerX + 5 : centerX - 38, height / 2 + 62)
                .size(75, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        int panelWidth = 260;
        int panelHeight = 190;
        int left = (width - panelWidth) / 2;
        int top = (height - panelHeight) / 2;
        graphics.fill(left, top, left + panelWidth, top + panelHeight, 0xFF3A2818);
        graphics.fill(left + 3, top + 3, left + panelWidth - 3, top + panelHeight - 3, 0xFFDCC99A);
        graphics.fill(left + 6, top + 6, left + panelWidth - 6, top + panelHeight - 6, 0xFFE8D8AE);

        Research research = ResearchRegistry.get(researchId);
        int y = top + 18;
        if (research == null) {
            graphics.drawCenteredString(font, Component.translatable("gui.fredplenter.note_unknown"),
                    width / 2, y, 0xFF41301C);
        } else {
            graphics.drawCenteredString(font, Component.translatable(research.getTranslationKey()),
                    width / 2, y, 0xFF41301C);
            y += 16;
            for (var line : font.split(Component.translatable(research.getDescriptionKey()), panelWidth - 36)) {
                graphics.drawCenteredString(font, line, width / 2, y, 0xFF5A4028);
                y += 11;
            }
            y += 8;
            String path = research.puzzle().stream()
                    .map(aspect -> Component.translatable(aspect.getTranslationKey()).getString())
                    .collect(java.util.stream.Collectors.joining(" → "));
            graphics.drawCenteredString(font,
                    Component.translatable("gui.fredplenter.research_path").append(": " + path),
                    width / 2, y, 0xFF41301C);
            y += 16;
            boolean learned = ClientArcaneProgress.hasResearch(researchId);
            graphics.drawCenteredString(font, Component.translatable(learned
                            ? "gui.fredplenter.learned"
                            : complete ? "tooltip.fredplenter.note_ready" : "tooltip.fredplenter.note_unfinished"),
                    width / 2, y, learned ? 0xFF2E6B2E : 0xFF7A4A1E);
            if (!complete && !learned) {
                y += 14;
                for (var line : font.split(Component.translatable("gui.fredplenter.note_howto"),
                        panelWidth - 36)) {
                    graphics.drawCenteredString(font, line, width / 2, y, 0xFF5A4028);
                    y += 11;
                }
            }
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
