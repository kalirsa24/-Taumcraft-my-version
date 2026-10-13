package com.fredplenter.client.screen;

import com.fredplenter.aspect.Aspect;
import com.fredplenter.menu.ArcaneTableMenu;
import com.fredplenter.network.CompleteResearchPayload;
import com.fredplenter.network.CombineAspectsPayload;
import com.fredplenter.network.StartResearchPayload;
import com.fredplenter.research.ClientArcaneProgress;
import com.fredplenter.research.Research;
import com.fredplenter.research.ResearchRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public class ArcaneTableScreen extends AbstractContainerScreen<ArcaneTableMenu> {

    private static final int WIDTH = 360;
    private static final int HEIGHT = 320;
    private static final int SIDEBAR_WIDTH = 90;
    private static final int HEX_SIZE = 8;
    private static final int HEX_SPACING = 20;
    private static final List<HexPosition> HONEYCOMB = createHoneycomb();

    private final List<Aspect> selectedPath = new ArrayList<>(3);
    private final List<HexPosition> selectedCells = new ArrayList<>(3);
    private Research selectedResearch;
    private Research activeResearch;
    private static final HexPosition START_CELL = new HexPosition(-2, 0);
    private static final HexPosition END_CELL = new HexPosition(2, 0);
    private final java.util.Map<HexPosition, Aspect> placedTiles = new java.util.HashMap<>();
    private Aspect placementAspect;
    private Aspect combineFirst;
    private Aspect combineSecond;
    private boolean awaitingPuzzleResult;
    private boolean awaitingResearchStart;
    private String puzzleFeedback = "";
    private int displayedRevision = -1;

    public ArcaneTableScreen(ArcaneTableMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = WIDTH;
        imageHeight = HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        rebuildButtons();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (activeResearch == null && menu.getResearchNoteId() == null && selectedResearch != null) {
            selectedResearch = null;
            rebuildButtons();
        }
        if (selectedResearch == null && menu.getResearchNoteId() != null) {
            selectedResearch = ResearchRegistry.get(menu.getResearchNoteId());
            if (selectedResearch != null) {
                rebuildButtons();
            }
        }
        int revision = ClientArcaneProgress.revision();
        if (displayedRevision != revision) {
            displayedRevision = revision;
            if (awaitingPuzzleResult) {
                awaitingPuzzleResult = false;
            }
            if (awaitingResearchStart
                    && activeResearch != null
                    && activeResearch.id().equals(ClientArcaneProgress.lastResearchStartId())) {
                awaitingResearchStart = false;
                if (!ClientArcaneProgress.lastResearchStartAccepted()) {
                    activeResearch = null;
                }
            }
            if (activeResearch != null
                    && activeResearch.id().equals(ClientArcaneProgress.lastResearchResultId())) {
                if (ClientArcaneProgress.lastResearchResultSucceeded()) {
                    activeResearch = null;
                    selectedPath.clear();
                    selectedCells.clear();
                    placedTiles.clear();
                } else {
                    selectedPath.clear();
                    selectedCells.clear();
                    placedTiles.clear();
                    puzzleFeedback = "message.fredplenter.research_failed";
                }
            }
            if (activeResearch != null && ClientArcaneProgress.hasResearch(activeResearch.id())) {
                activeResearch = null;
                selectedPath.clear();
                selectedCells.clear();
            }
            rebuildButtons();
        }
        super.render(graphics, mouseX, mouseY, partialTick);
        renderAspectTooltip(graphics, mouseX, mouseY);
    }

    private void rebuildButtons() {
        clearWidgets();
        if (activeResearch == null) {
            buildResearchButtons();
        } else {
            buildPuzzleButtons();
        }
    }

    private void buildResearchButtons() {
        if (selectedResearch == null) {
            Aspect result = Aspect.combine(combineFirst, combineSecond);
            Button combine = Button.builder(
                            Component.translatable("gui.fredplenter.combine_aspects"),
                            ignored -> {
                                if (combineFirst != null && combineSecond != null) {
                                    PacketDistributor.sendToServer(new CombineAspectsPayload(
                                            combineFirst.getId(), combineSecond.getId()));
                                    combineFirst = null; combineSecond = null;
                                }
                            })
                    .pos(leftPos + SIDEBAR_WIDTH + 10, topPos + 145)
                    .size(workspaceWidth() - 20, 17)
                    .build();
            combine.active = result != null
                    && menu.getAspectPoints(combineFirst) >= 1 && menu.getAspectPoints(combineSecond) >= 1;
            if (result != null) combine.setMessage(Component.translatable("gui.fredplenter.combine_aspects")
                    .append(": " + Component.translatable(result.getTranslationKey()).getString()));
            addRenderableWidget(combine);
        }
        if (selectedResearch != null) {
            Button button = Button.builder(
                            Component.translatable("gui.fredplenter.begin"),
                            ignored -> beginResearch(selectedResearch))
                    .pos(leftPos + SIDEBAR_WIDTH + 10, topPos + 145)
                    .size(workspaceWidth() - 20, 17)
                    .build();
            button.active = menu.canStartResearch(selectedResearch);
            addRenderableWidget(button);
        }
    }

    private void buildPuzzleButtons() {
        int controlsWidth = 62 + 5 + 43;
        int controlsLeft = leftPos + SIDEBAR_WIDTH + 7 + (workspaceWidth() - controlsWidth) / 2;
        addRenderableWidget(Button.builder(
                        Component.translatable("gui.fredplenter.reset_path"),
                        ignored -> {
                            selectedPath.clear();
                            selectedCells.clear();
                            placedTiles.clear();
                            puzzleFeedback = "";
                        })
                .pos(controlsLeft, topPos + 148)
                .size(62, 16)
                .build());
        addRenderableWidget(Button.builder(
                        Component.translatable("gui.fredplenter.cancel"),
                        ignored -> {
                            activeResearch = null;
                            selectedPath.clear();
                            selectedCells.clear();
                            placedTiles.clear();
                            awaitingPuzzleResult = false;
                            awaitingResearchStart = false;
                            puzzleFeedback = "";
                            rebuildButtons();
                        })
                .pos(controlsLeft + 67, topPos + 148)
                .size(43, 16)
                .build());
    }

    private void beginResearch(Research research) {
        PacketDistributor.sendToServer(new StartResearchPayload(research.id()));
        ClientArcaneProgress.clearResearchResult();
        activeResearch = research;
        selectedPath.clear();
        selectedCells.clear();
        placedTiles.clear();
        placementAspect = null;
        awaitingResearchStart = true;
        awaitingPuzzleResult = false;
        puzzleFeedback = "";
        rebuildButtons();
    }

    /**
     * Hex-link puzzle (PNG-2 style): start and end anchors are fixed tiles on the
     * parchment. Pick an aspect on the left, click a hex next to the chain to lay
     * a tile (1 point, charged only on success), click the end anchor to finish.
     * Neighbouring tiles must be linked aspects; the chain must pass through the
     * note's required middle aspect. The server re-validates all of it.
     */
    private void selectHex(HexPosition cell) {
        Aspect start = activeResearch.puzzle().getFirst();
        Aspect end = activeResearch.puzzle().getLast();
        if (cell.equals(START_CELL)) {
            if (selectedPath.isEmpty()) {
                selectedPath.add(start);
                selectedCells.add(cell);
                puzzleFeedback = "gui.fredplenter.place_aspect_first";
            }
            return;
        }
        if (selectedPath.isEmpty()) {
            puzzleFeedback = "gui.fredplenter.start_with_aspect";
            return;
        }
        HexPosition lastCell = selectedCells.getLast();
        Aspect lastAspect = selectedPath.getLast();
        if (placedTiles.containsKey(cell)) {
            if (cell.equals(lastCell)) { // click the tail tile again to take it back, free
                placedTiles.remove(cell);
                selectedPath.removeLast();
                selectedCells.removeLast();
                puzzleFeedback = "";
            }
            return;
        }
        if (cell.equals(END_CELL)) {
            if (!lastCell.isAdjacent(cell)) { puzzleFeedback = "gui.fredplenter.connect_adjacent"; return; }
            if (!selectedPath.contains(activeResearch.puzzle().get(1))) { puzzleFeedback = "gui.fredplenter.need_middle"; return; }
            if (!lastAspect.isLinkedTo(end)) { puzzleFeedback = "gui.fredplenter.tile_not_linked"; return; }
            selectedPath.add(end);
            selectedCells.add(cell);
            String encoded = String.join(",", selectedPath.stream().map(Aspect::getId).toList());
            PacketDistributor.sendToServer(new CompleteResearchPayload(activeResearch.id(), encoded));
            awaitingPuzzleResult = true;
            puzzleFeedback = "gui.fredplenter.research_waiting";
            return;
        }
        if (!lastCell.isAdjacent(cell)) { puzzleFeedback = "gui.fredplenter.connect_adjacent"; return; }
        if (placementAspect == null) { puzzleFeedback = "gui.fredplenter.place_aspect_first"; return; }
        if (!lastAspect.isLinkedTo(placementAspect)) { puzzleFeedback = "gui.fredplenter.tile_not_linked"; return; }
        long alreadyUsed = selectedPath.stream().skip(1).filter(a -> a == placementAspect).count();
        if (menu.getAspectPoints(placementAspect) <= alreadyUsed) { puzzleFeedback = "gui.fredplenter.tile_not_linked"; return; }
        placedTiles.put(cell, placementAspect);
        selectedPath.add(placementAspect);
        selectedCells.add(cell);
        puzzleFeedback = selectedPath.contains(activeResearch.puzzle().get(1))
                ? "gui.fredplenter.click_end_to_finish" : "gui.fredplenter.need_middle";
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (activeResearch == null && button == 0) {
            Aspect clickedAspect = findSidebarAspect(mouseX, mouseY);
            if (clickedAspect != null && ClientArcaneProgress.hasDiscovered(clickedAspect)) {
                if (combineFirst == null || combineFirst == clickedAspect) combineFirst = clickedAspect;
                else if (combineSecond == null || combineSecond == clickedAspect) combineSecond = clickedAspect;
                else { combineFirst = clickedAspect; combineSecond = null; }
                rebuildButtons();
                return true;
            }
        }
        if (activeResearch != null && button == 0) {
            Aspect palette = findSidebarAspect(mouseX, mouseY);
            if (palette != null && ClientArcaneProgress.hasDiscovered(palette)) {
                placementAspect = palette;
                return true;
            }
        }
        if (activeResearch != null && button == 0 && !awaitingResearchStart && !awaitingPuzzleResult) {
            HexPosition cell = findHex(mouseX, mouseY);
            if (cell != null) {
                selectHex(cell);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        drawTableFrame(graphics);
        drawSidePanel(graphics);
        drawWorkspace(graphics);
        drawInventoryArea(graphics);
        drawAspectSidebar(graphics);
        drawTableHelp(graphics);
        if (activeResearch != null) {
            renderResearchPuzzle(graphics, mouseX, mouseY);
            renderPuzzleDetails(graphics);
        } else if (menu.getResearchNoteId() != null) {
            renderNoteDetails(graphics);
        } else {
            renderCombinerHint(graphics);

        }
    }

    private void drawTableFrame(GuiGraphics graphics) {
        int x = leftPos;
        int y = topPos;
        int width = imageWidth;
        int height = imageHeight;
        graphics.fill(x, y, x + width, y + height, 0xFF170E0A);
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, 0xFF60402A);
        graphics.fill(x + 5, y + 5, x + width - 5, y + height - 5, 0xFF342319);
        graphics.fill(x + 7, y + 7, x + width - 7, y + height - 7, 0xFF795638);
        graphics.fill(x + 3, y + 3, x + width - 3, y + 5, 0xFF9B7650);
        graphics.fill(x + 3, y + height - 5, x + width - 3, y + height - 3, 0xFF24170F);
        drawNail(graphics, x + 5, y + 5);
        drawNail(graphics, x + width - 7, y + 5);
        drawNail(graphics, x + 5, y + height - 7);
        drawNail(graphics, x + width - 7, y + height - 7);
    }

    private void drawSidePanel(GuiGraphics graphics) {
        int x = leftPos + 8;
        int y = topPos + 9;
        drawInsetPanel(graphics, x, y, SIDEBAR_WIDTH - 8, 158, 0xFF503823, 0xFF302116);
        graphics.drawCenteredString(font, Component.translatable("gui.fredplenter.aspect_panel"),
                x + (SIDEBAR_WIDTH - 8) / 2, y + 4, 0xFFE6C995);
    }

    private void drawWorkspace(GuiGraphics graphics) {
        int x = leftPos + SIDEBAR_WIDTH + 7;
        int y = topPos + 9;
        drawInsetPanel(graphics, x, y, workspaceWidth(), 158, 0xFFB29361, 0xFFDCC99A);
        drawParchmentMarks(graphics, x + 5, y + 5, workspaceWidth() - 10, 148);
        graphics.fill(x + 4, y + 4, x + workspaceWidth() - 4, y + 5, 0xFF80603F);
        graphics.fill(x + 4, y + 153, x + workspaceWidth() - 4, y + 154, 0xFF80603F);
    }

    private void drawInventoryArea(GuiGraphics graphics) {
        int x = leftPos + 8;
        int y = topPos + 170;
        int width = imageWidth - 16;
        int height = imageHeight - 179;
        drawInsetPanel(graphics, x, y, width, height, 0xFF503823, 0xFF34271D);
        graphics.drawCenteredString(font, Component.translatable("container.inventory"),
                leftPos + imageWidth / 2, topPos + 172, 0xFFD9C39A);
    }

    private void drawInsetPanel(GuiGraphics graphics, int x, int y, int width, int height,
                                int frameColor, int fillColor) {
        graphics.fill(x, y, x + width, y + height, 0xFF24170F);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, frameColor);
        graphics.fill(x + 3, y + 3, x + width - 3, y + height - 3, 0xFF9A754C);
        graphics.fill(x + 4, y + 4, x + width - 4, y + height - 4, fillColor);
    }

    private void drawAspectSidebar(GuiGraphics graphics) {
        int i = 0;
        for (Aspect aspect : Aspect.values()) {
            int x = leftPos + 12 + (i % 2) * 40;
            int y = topPos + 30 + (i / 2) * 12;
            i++;
            boolean known = ClientArcaneProgress.hasDiscovered(aspect);
            int points = known ? menu.getAspectPoints(aspect) : 0;
            boolean picked = aspect == combineFirst || aspect == combineSecond;
            if (picked) graphics.fill(x - 3, y - 3, x + 38, y + 12, 0xFFFFD77A);
            drawAspectIcon(graphics, x, y, aspect, known && points > 0);
            graphics.drawString(font, known ? Integer.toString(points) : "?", x + 14, y + 3,
                    known ? 0xFFFFE1AF : 0xFF8A7A5A, false);
        }
        graphics.drawCenteredString(font, Component.translatable("gui.fredplenter.table_note_slot"),
                leftPos + SIDEBAR_WIDTH / 2, topPos + 134, 0xFFE6C995);
        int slotX = leftPos + ArcaneTableMenu.NOTE_SLOT_X;
        int slotY = topPos + ArcaneTableMenu.NOTE_SLOT_Y;
        graphics.fill(slotX - 3, slotY - 3, slotX + 21, slotY + 21, 0xFF20140D);
        graphics.fill(slotX - 2, slotY - 2, slotX + 20, slotY + 20, 0xFF9A7047);
        graphics.fill(slotX - 1, slotY - 1, slotX + 19, slotY + 19, 0xFF503823);
        graphics.fill(slotX, slotY, slotX + 18, slotY + 18, 0xFF34271D);
    }

    private void drawTableHelp(GuiGraphics graphics) {
        int x = leftPos + 180;
        int y = topPos + 184;
        int width = imageWidth - 190;
        int height = imageHeight - 194;
        drawInsetPanel(graphics, x, y, width, height, 0xFF503823, 0xFF34271D);

        int textX = x + 7;
        int textY = y + 7;
        int textWidth = width - 14;
        graphics.drawString(font, Component.translatable("gui.fredplenter.table_help_title"),
                textX, textY, 0xFFE6C995, false);
        textY += 14;
        textY = drawHelpSection(graphics, "gui.fredplenter.table_help_purpose",
                "gui.fredplenter.table_help_purpose_desc", textX, textY, textWidth);
        textY = drawHelpSection(graphics, "gui.fredplenter.table_help_get",
                "gui.fredplenter.table_help_get_desc", textX, textY + 3, textWidth);
        drawHelpSection(graphics, "gui.fredplenter.table_help_solve",
                "gui.fredplenter.table_help_solve_desc", textX, textY + 3, textWidth);
    }

    private int drawHelpSection(GuiGraphics graphics, String titleKey, String descriptionKey,
                                int x, int y, int width) {
        graphics.drawString(font, Component.translatable(titleKey), x, y, 0xFFE6C995, false);
        y += 10;
        for (net.minecraft.util.FormattedCharSequence line
                : font.split(Component.translatable(descriptionKey), width)) {
            graphics.drawString(font, line, x, y, 0xFFD9C39A, false);
            y += 9;
        }
        return y;
    }

    /** Sidebar geometry shared by drawing, tooltips and clicks: all 16 aspects, 2 columns. */
    private Aspect findSidebarAspect(double mouseX, double mouseY) {
        int i = 0;
        for (Aspect a : Aspect.values()) {
            int x = leftPos + 12 + (i % 2) * 40;
            int y = topPos + 30 + (i / 2) * 12;
            i++;
            if (mouseX >= x - 2 && mouseX < x + 38 && mouseY >= y - 2 && mouseY < y + 12) return a;
        }
        return null;
    }

    private void renderNoteDetails(GuiGraphics graphics) {
        int centerX = leftPos + SIDEBAR_WIDTH + 7 + workspaceWidth() / 2;
        if (selectedResearch == null) return;
        Research r = selectedResearch;
        graphics.drawCenteredString(font, Component.translatable(r.getTranslationKey()), centerX, topPos + 18, 0xFF41301C);
        int y = topPos + 36;
        for (var line : font.split(Component.translatable(r.getDescriptionKey()), workspaceWidth() - 30)) {
            graphics.drawCenteredString(font, line, centerX, y, 0xFF5A4028); y += 11;
        }
        String path = r.puzzle().stream().map(a -> Component.translatable(a.getTranslationKey()).getString())
                .collect(java.util.stream.Collectors.joining(" \u2192 "));
        graphics.drawCenteredString(font, Component.translatable("gui.fredplenter.research_path").append(": " + path),
                centerX, y + 8, 0xFF41301C);
        graphics.drawCenteredString(font, Component.translatable(
                isNoteComplete() ? "tooltip.fredplenter.note_ready" : "gui.fredplenter.note_press_begin"),
                centerX, y + 26, 0xFF5A4028);
    }

    private boolean isNoteComplete() {
        return com.fredplenter.item.ResearchNoteItem.isComplete(menuSlotsNote());
    }
    private net.minecraft.world.item.ItemStack menuSlotsNote() {
        return menu.slots.isEmpty() ? net.minecraft.world.item.ItemStack.EMPTY : menu.slots.get(0).getItem();
    }

    private void renderCombinerHint(GuiGraphics graphics) {
        int centerX = leftPos + SIDEBAR_WIDTH + 7 + workspaceWidth() / 2;
        graphics.drawCenteredString(font, Component.translatable("gui.fredplenter.combine_hint_1"), centerX, topPos + 30, 0xFF41301C);
        graphics.drawCenteredString(font, Component.translatable("gui.fredplenter.combine_hint_2"), centerX, topPos + 44, 0xFF5A4028);
        String sel = (combineFirst == null ? "?" : Component.translatable(combineFirst.getTranslationKey()).getString())
                + " + " + (combineSecond == null ? "?" : Component.translatable(combineSecond.getTranslationKey()).getString());
        Aspect result = Aspect.combine(combineFirst, combineSecond);
        graphics.drawCenteredString(font, sel + " = " + (result == null ? "?" : Component.translatable(result.getTranslationKey()).getString()),
                centerX, topPos + 62, 0xFF41301C);
        // list every known compound recipe using only this mod's own aspect names
        int y = topPos + 80;
        for (Aspect a : Aspect.values()) {
            if (a.isPrimal()) continue;
            var c = a.getComponents();
            String line = Component.translatable(c.get(0).getTranslationKey()).getString() + " + "
                    + Component.translatable(c.get(1).getTranslationKey()).getString() + " = "
                    + (ClientArcaneProgress.hasDiscovered(a) ? Component.translatable(a.getTranslationKey()).getString() : "???");
            graphics.drawCenteredString(font, line, centerX, y, ClientArcaneProgress.hasDiscovered(a) ? 0xFF2E6B2E : 0xFF8A7A5A);
            y += 11;
            if (y > topPos + 140) break;
        }
    }

    private void renderPuzzleDetails(GuiGraphics graphics) {
        int x = leftPos + SIDEBAR_WIDTH + 8;
        int width = imageWidth - SIDEBAR_WIDTH - 16;
        int y = topPos + 126;
        String path = activeResearch.puzzle().stream()
                .map(aspect -> Component.translatable(aspect.getTranslationKey()).getString())
                .collect(java.util.stream.Collectors.joining(" → "));
        graphics.drawWordWrap(font,
                Component.translatable("gui.fredplenter.research_path").append(": " + path),
                x, y, width, 0xFF41301C);
        String feedback = puzzleFeedback.isEmpty() ? "gui.fredplenter.place_aspect_first" : puzzleFeedback;
        if (placementAspect != null) {
            feedback = feedback + " [" + Component.translatable(placementAspect.getTranslationKey()).getString() + "]";
        }
        graphics.drawWordWrap(font, Component.translatable(feedback), x, y + 13, width, 0xFF41301C);
    }

    private void renderResearchPuzzle(GuiGraphics graphics, int mouseX, int mouseY) {
        int areaLeft = leftPos + SIDEBAR_WIDTH + 7;
        int areaTop = topPos + 12;
        int areaRight = areaLeft + imageWidth - SIDEBAR_WIDTH - 16;
        graphics.drawCenteredString(font, Component.translatable(activeResearch.getTranslationKey()),
                (areaLeft + areaRight) / 2, areaTop + 2, 0xFF41301C);

        for (HexPosition cell : HONEYCOMB) {
            int[] position = hexPixel(cell);
            int centerX = position[0];
            int centerY = position[1];
            Aspect aspect = aspectAt(cell);
            boolean isPath = selectedCells.contains(cell);
            boolean hovered = distanceSquared(mouseX, mouseY, centerX, centerY) <= HEX_SIZE * HEX_SIZE;
            int border = hovered ? 0xFFE6C995 : isPath ? 0xFFFFD77A : 0xFF60402A;
            drawHex(graphics, centerX, centerY, HEX_SIZE + 3, 0xFF24170F);
            drawHex(graphics, centerX, centerY, HEX_SIZE + 2, border);
            drawHex(graphics, centerX, centerY, HEX_SIZE, 0xFFB29361);
            if (aspect != null) {
                boolean keyNode = cell.equals(START_CELL) || cell.equals(END_CELL);
                int aspectColor = keyNode ? aspect.getColor() : shade(aspect.getColor(), 0.76f);
                drawHex(graphics, centerX, centerY, HEX_SIZE - 2, aspectColor);
                graphics.drawCenteredString(font, aspectMark(aspect),
                        centerX, centerY - 4, 0xFFF8EFD9);
            } else {
                drawHex(graphics, centerX, centerY, HEX_SIZE - 2, 0xFFBAA77C);
            }
        }

        renderSelectedPath(graphics);
        HexPosition hoveredCell = findHex(mouseX, mouseY);
        if (hoveredCell != null && aspectAt(hoveredCell) != null) {
            graphics.renderTooltip(font, Component.translatable(aspectAt(hoveredCell).getTranslationKey()),
                    mouseX, mouseY);
        }

    }

    private void renderSelectedPath(GuiGraphics graphics) {
        for (int index = 1; index < selectedCells.size(); index++) {
            int[] from = hexPixel(selectedCells.get(index - 1));
            int[] to = hexPixel(selectedCells.get(index));
            drawLine(graphics, from[0], from[1], to[0], to[1], 0xFF263E25);
        }
    }

    private void drawParchmentMarks(GuiGraphics graphics, int x, int y, int width, int height) {
        for (int index = 0; index < 26; index++) {
            int px = x + 8 + Math.floorMod(index * 47 + 13, Math.max(1, width - 16));
            int py = y + 8 + Math.floorMod(index * 31 + 9, Math.max(1, height - 16));
            graphics.fill(px, py, px + 1, py + 1, 0x22735A37);
        }
    }

    private void renderAspectTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        Aspect a = findSidebarAspect(mouseX, mouseY);
        if (a == null) return;
        if (!ClientArcaneProgress.hasDiscovered(a)) {
            graphics.renderTooltip(font, Component.translatable("gui.fredplenter.undiscovered"), mouseX, mouseY);
            return;
        }
        java.util.List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("tooltip.fredplenter.aspect_points",
                Component.translatable(a.getTranslationKey()), menu.getAspectPoints(a)));
        if (!a.isPrimal()) {
            var c = a.getComponents();
            lines.add(Component.translatable("gui.fredplenter.aspect_recipe",
                    Component.translatable(c.get(0).getTranslationKey()),
                    Component.translatable(c.get(1).getTranslationKey())));
        }
        graphics.renderTooltip(font, lines, mouseX, mouseY);
    }

    private void drawAspectIcon(GuiGraphics graphics, int x, int y, Aspect aspect, boolean bright) {
        int color = bright ? aspect.getColor() : shade(aspect.getColor(), 0.35f);
        graphics.fill(x - 2, y - 2, x + 12, y + 12, 0xFF25190F);
        drawHex(graphics, x + 5, y + 5, 6, 0xFF000000 | color);
        graphics.drawCenteredString(font, aspectMark(aspect), x + 5, y + 1, 0xFFF7EBD4);
    }

    private static int shade(int color, float strength) {
        int red = (int) (((color >> 16) & 255) * strength);
        int green = (int) (((color >> 8) & 255) * strength);
        int blue = (int) ((color & 255) * strength);
        return (red << 16) | (green << 8) | blue;
    }

    private static String aspectMark(Aspect aspect) {
        return switch (aspect) {
            case FLAMMA, AESTUS -> "✦";
            case UNDA, VAPOR -> "≈";
            case HUMUS, LIMUS -> "⌁";
            case FLATUS, PROCELLA -> "➤";
            case LUX, VITA -> "✧";
            case TENEBRAE, MORS -> "●";
            case CINIS, PULVIS -> "·";
            case SOMNUS -> "☾";
            case MOTUS -> "↗";
        };
    }

    private static List<HexPosition> createHoneycomb() {
        List<HexPosition> cells = new ArrayList<>();
        for (int q = -2; q <= 2; q++) {
            for (int r = -2; r <= 2; r++) {
                if (Math.abs(q + r) <= 2) {
                    cells.add(new HexPosition(q, r));
                }
            }
        }
        return List.copyOf(cells);
    }

    private Aspect aspectAt(HexPosition position) {
        if (activeResearch == null) return null;
        if (position.equals(START_CELL)) return activeResearch.puzzle().getFirst();
        if (position.equals(END_CELL)) return activeResearch.puzzle().getLast();
        return placedTiles.get(position);
    }

    private int[] hexPixel(HexPosition position) {
        int centerX = leftPos + SIDEBAR_WIDTH + 7
                + (imageWidth - SIDEBAR_WIDTH - 16) / 2
                + position.q() * HEX_SPACING + position.r() * (HEX_SPACING / 2);
        int centerY = topPos + 88 + position.r() * (HEX_SPACING * 3 / 4)
                - position.q() * (HEX_SPACING * 3 / 8);
        return new int[]{centerX, centerY};
    }

    private HexPosition findHex(double mouseX, double mouseY) {
        for (HexPosition cell : HONEYCOMB) {
            int[] position = hexPixel(cell);
            if (distanceSquared(mouseX, mouseY, position[0], position[1]) <= HEX_SIZE * HEX_SIZE) {
                return cell;
            }
        }
        return null;
    }

    private static void drawHex(GuiGraphics graphics, int centerX, int centerY, int radius, int color) {
        int half = Math.max(2, radius / 2);
        graphics.fill(centerX - half, centerY - radius, centerX + half + 1, centerY - radius + 1, color);
        graphics.fill(centerX - radius + 2, centerY - half, centerX + radius - 1, centerY + half + 1, color);
        graphics.fill(centerX - half, centerY + radius - 1, centerX + half + 1, centerY + radius, color);
        for (int offset = 0; offset < radius - half; offset++) {
            graphics.fill(centerX - half - offset, centerY - radius + offset,
                    centerX - half - offset + 1, centerY - radius + offset + 1, color);
            graphics.fill(centerX + half + offset, centerY - radius + offset,
                    centerX + half + offset + 1, centerY - radius + offset + 1, color);
            graphics.fill(centerX - half - offset, centerY + half + offset,
                    centerX - half - offset + 1, centerY + half + offset + 1, color);
            graphics.fill(centerX + half + offset, centerY + half + offset,
                    centerX + half + offset + 1, centerY + half + offset + 1, color);
        }
    }

    private static void drawLine(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
        for (int step = 0; step <= steps; step++) {
            int x = x1 + (x2 - x1) * step / Math.max(1, steps);
            int y = y1 + (y2 - y1) * step / Math.max(1, steps);
            graphics.fill(x - 1, y - 1, x + 1, y + 1, color);
        }
    }

    private void drawNail(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 2, y + 2, 0xFF38271B);
        graphics.fill(x, y, x + 1, y + 1, 0xFFC39B68);
    }

    private int workspaceWidth() {
        return imageWidth - SIDEBAR_WIDTH - 16;
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    private static int distanceSquared(double x1, double y1, double x2, double y2) {
        int dx = (int) Math.round(x1 - x2);
        int dy = (int) Math.round(y1 - y2);
        return dx * dx + dy * dy;
    }

    private record HexPosition(int q, int r) {
        private boolean isAdjacent(HexPosition other) {
            int deltaQ = other.q - q;
            int deltaR = other.r - r;
            return Math.max(Math.abs(deltaQ), Math.max(Math.abs(deltaR), Math.abs(deltaQ + deltaR))) == 1;
        }
    }
}
