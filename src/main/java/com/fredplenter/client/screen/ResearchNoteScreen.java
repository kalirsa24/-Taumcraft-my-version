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

/**
 * The research note's OWN parchment screen - deliberately not the Codex map.
 * Unfinished: shows the topic and the aspect path to reproduce at the Arcane Table.
 * Complete: shows a Learn button (server re-validates via LearnNotePayload).
 */
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
        int cx = width / 2;
        if (complete) {
            addRenderableWidget(Button.builder(Component.translatable("gui.fredplenter.learn"), b -> {
                PacketDistributor.sendToServer(new LearnNotePayload(researchId));
                onClose();
            }).pos(cx - 80, height / 2 + 62).size(75, 20).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.fredplenter.cancel"), b -> onClose())
                .pos(complete ? cx + 5 : cx - 38, height / 2 + 62).size(75, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g, mouseX, mouseY, partial);
        int w = 260, h = 190, x = (width - w) / 2, y = (height - h) / 2;
        // parchment (own colours, drawn with fills like the rest of this mod)
        g.fill(x, y, x + w, y + h, 0xFF3A2818);
        g.fill(x + 3, y + 3, x + w - 3, y + h - 3, 0xFFDCC99A);
        g.fill(x + 6, y + 6, x + w - 6, y + h - 6, 0xFFE8D8AE);
        Research r = ResearchRegistry.get(researchId);
        int cy = y + 18;
        if (r == null) {
            g.drawCenteredString(font, Component.translatable("gui.fredplenter.note_unknown"), width / 2, cy, 0xFF41301C);
        } else {
            g.drawCenteredString(font, Component.translatable(r.getTranslationKey()), width / 2, cy, 0xFF41301C);
            cy += 16;
            for (var line : font.split(Component.translatable(r.getDescriptionKey()), w - 36)) {
                g.drawCenteredString(font, line, width / 2, cy, 0xFF5A4028);
                cy += 11;
            }
            cy += 8;
            String path = r.puzzle().stream()
                    .map(a -> Component.translatable(a.getTranslationKey()).getString())
                    .collect(java.util.stream.Collectors.joining(" → "));
            g.drawCenteredString(font, Component.translatable("gui.fredplenter.research_path").append(": " + path),
                    width / 2, cy, 0xFF41301C);
            cy += 16;
            boolean learned = ClientArcaneProgress.hasResearch(researchId);
            g.drawCenteredString(font, Component.translatable(learned ? "gui.fredplenter.learned"
                    : complete ? "tooltip.fredplenter.note_ready" : "tooltip.fredplenter.note_unfinished"),
                    width / 2, cy, learned ? 0xFF2E6B2E : 0xFF7A4A1E);
            if (!complete && !learned) {
                cy += 14;
                for (var line : font.split(Component.translatable("gui.fredplenter.note_howto"), w - 36)) {
                    g.drawCenteredString(font, line, width / 2, cy, 0xFF5A4028);
                    cy += 11;
                }
            }
        }
        super.render(g, mouseX, mouseY, partial);
    }

    @Override public boolean isPauseScreen() { return false; }
}
