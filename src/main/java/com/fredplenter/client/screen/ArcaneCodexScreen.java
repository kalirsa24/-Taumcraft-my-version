package com.fredplenter.client.screen;

import com.fredplenter.aspect.Aspect;
import com.fredplenter.research.ClientArcaneProgress;
import com.fredplenter.network.BeginCodexResearchPayload;
import com.fredplenter.research.Research;
import com.fredplenter.research.ResearchRegistry;
import com.fredplenter.item.InkwellItem;
import com.fredplenter.registry.ModItems;
import com.fredplenter.spell.Spell;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public class ArcaneCodexScreen extends Screen {

    private static final int FRAME_MARGIN_X = 34;
    private static final int FRAME_MARGIN_Y = 30;
    private static final int SIDEBAR_WIDTH = 48;
    private static final int NODE_RADIUS = 15;
    private static final List<Category> CATEGORIES = List.of(
            new Category("all", "gui.fredplenter.category_all", "*"),
            new Category("foundations", "gui.fredplenter.category_foundations", "F"),
            new Category("aura", "gui.fredplenter.category_aura", "A"),
            new Category("wand", "gui.fredplenter.category_wand", "W"),
            new Category("craft", "gui.fredplenter.category_craft", "C"));

    private final List<Research> research = new ArrayList<>(ResearchRegistry.all().values());
    private int categoryIndex;
    private EditBox searchBox;
    private Research selectedResearch;
    private Button researchButton;
    private boolean requestPending;
    private long requestStartedAt;
    private int displayedRevision = -1;

    public ArcaneCodexScreen() {
        super(Component.translatable("item.fredplenter.arcane_codex"));
    }

    @Override
    protected void init() {
        requestPending = false;
        int left = frameLeft();
        int top = frameTop();
        searchBox = new EditBox(font, left + 14, top + 8, Math.max(120, width - left * 2 - 230), 16,
                Component.translatable("gui.fredplenter.research_search"));
        searchBox.setMaxLength(48);
        searchBox.setHint(Component.translatable("gui.fredplenter.research_search"));
        addRenderableWidget(searchBox);
        researchButton = Button.builder(Component.translatable("gui.fredplenter.begin_research"),
                        ignored -> beginSelectedResearch())
                .pos(width - frameLeft() - 126, height - frameTop() - 26)
                .size(112, 18)
                .build();
        researchButton.visible = false;
        addRenderableWidget(researchButton);
    }

    @Override
    public void tick() {
        super.tick();
        if (requestPending && System.currentTimeMillis() - requestStartedAt >= 5_000L) {
            requestPending = false;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        drawCosmicBackground(graphics);
        drawBookFrame(graphics);
        drawHeader(graphics);
        drawCategoryRail(graphics);
        drawResearchConnections(graphics);
        drawResearchNodes(graphics, mouseX, mouseY);
        updateResearchButton();
        super.render(graphics, mouseX, mouseY, partialTick);

        Research hovered = findResearchAt(mouseX, mouseY);
        if (hovered != null) {
            drawResearchTooltip(graphics, hovered, mouseX, mouseY);
        }
    }

    private void drawBookFrame(GuiGraphics graphics) {
        int left = frameLeft();
        int top = frameTop();
        int right = width - left;
        int bottom = height - top;
        graphics.fill(left, top, right, bottom, 0xFF100B20);
        graphics.fill(left + 3, top + 3, right - 3, bottom - 3, 0xFF62407D);
        graphics.fill(left + 6, top + 6, right - 6, bottom - 6, 0xFF211533);
        graphics.fill(left + 8, top + 8, right - 8, top + 28, 0xFF38234D);
        graphics.fill(left + 8, top + 29, right - 8, bottom - 8, 0xFF130F24);

        int railRight = left + SIDEBAR_WIDTH;
        graphics.fill(left + 9, top + 30, railRight, bottom - 9, 0xFF1B142C);
        graphics.fill(railRight, top + 30, railRight + 2, bottom - 9, 0xFF9B6FC0);
        graphics.fill(railRight + 2, top + 30, railRight + 4, bottom - 9, 0xFF4A3266);

        for (int x = left + 12; x < right - 12; x += 22) {
            graphics.fill(x, top + 4, x + 1, top + 7, 0xFFD4B3F3);
            graphics.fill(x + 8, bottom - 7, x + 9, bottom - 4, 0xFFD4B3F3);
        }
        for (int y = top + 34; y < bottom - 14; y += 19) {
            graphics.fill(left + 3, y, left + 6, y + 1, 0xFFB485D5);
            graphics.fill(right - 6, y + 7, right - 3, y + 8, 0xFFB485D5);
        }
    }

    private void drawCosmicBackground(GuiGraphics graphics) {
        graphics.fill(0, 0, width, height, 0xFF080614);
        graphics.fill(width / 5, 0, width * 2 / 5, height, 0x220D4E78);
        graphics.fill(width * 3 / 5, 0, width * 4 / 5, height, 0x221E0E49);
        for (int index = 0; index < 96; index++) {
            int x = Math.floorMod(index * 73 + 19, Math.max(1, width));
            int y = Math.floorMod(index * 137 + 41, Math.max(1, height));
            int size = index % 11 == 0 ? 2 : 1;
            int color = index % 3 == 0 ? 0xFFC9B0F2 : 0xFF8D83BB;
            graphics.fill(x, y, x + size, y + size, color);
        }
    }

    private void drawHeader(GuiGraphics graphics) {
        int left = frameLeft();
        int top = frameTop();
        long learned = research.stream().filter(entry -> ClientArcaneProgress.hasResearch(entry.id())).count();
        String summary = Component.translatable("gui.fredplenter.research_summary", learned, research.size()).getString();
        graphics.drawString(font, summary, width - left - font.width(summary) - 13, top + 13,
                0xFFE0C8FF, false);
    }

    private void drawCategoryRail(GuiGraphics graphics) {
        int left = frameLeft();
        int top = frameTop();
        for (int index = 0; index < CATEGORIES.size(); index++) {
            int x = left + 12;
            int y = top + 42 + index * 30;
            boolean selected = categoryIndex == index;
            graphics.fill(x, y, x + 25, y + 24, selected ? 0xFF9A6BC0 : 0xFF38294E);
            graphics.fill(x + 2, y + 2, x + 23, y + 22, selected ? 0xFF4D3567 : 0xFF21182F);
            graphics.drawCenteredString(font, CATEGORIES.get(index).symbol(), x + 12, y + 7,
                    selected ? 0xFFFFE7A8 : 0xFFD0B5E9);
        }
    }

    private void drawResearchConnections(GuiGraphics graphics) {
        List<Research> visible = filteredResearch();
        for (Research entry : visible) {
            Point child = nodePosition(entry);
            for (ResourceLocation prerequisiteId : entry.prerequisite()) {
                Research prerequisite = ResearchRegistry.get(prerequisiteId);
                if (prerequisite == null || !visible.contains(prerequisite)) {
                    continue;
                }
                Point parent = nodePosition(prerequisite);
                boolean unlocked = ClientArcaneProgress.hasResearch(prerequisite.id());
                drawLine(graphics, parent.x(), parent.y(), child.x(), child.y(),
                        unlocked ? 0xFFE3BDFF : 0xFF655A7B);
                if (unlocked) {
                    drawLine(graphics, parent.x(), parent.y() + 1, child.x(), child.y() + 1, 0x668DCEFF);
                }
            }
        }
    }

    private void drawResearchNodes(GuiGraphics graphics, int mouseX, int mouseY) {
        List<Research> visible = filteredResearch();
        for (Research entry : visible) {
            Point point = nodePosition(entry);
            boolean learned = ClientArcaneProgress.hasResearch(entry.id());
            boolean available = ResearchRegistry.isAvailable(entry, ClientArcaneProgress.getCompletedResearch());
            boolean hovered = distanceSquared(mouseX, mouseY, point.x(), point.y()) <= NODE_RADIUS * NODE_RADIUS;
            int fill = learned ? 0xFF276A68 : available ? 0xFF754C9A : 0xFF28243A;
            int edge = hovered ? 0xFFFFE9A9 : learned ? 0xFF91E8D6 : available ? 0xFFE4B8FF : 0xFF766C91;

            graphics.fill(point.x() - NODE_RADIUS - 2, point.y() - NODE_RADIUS - 2,
                    point.x() + NODE_RADIUS + 2, point.y() + NODE_RADIUS + 2, 0xFF090716);
            drawHex(graphics, point.x(), point.y(), NODE_RADIUS + 2, edge);
            drawHex(graphics, point.x(), point.y(), NODE_RADIUS - 2, fill);
            Aspect icon = entry.mapIconAspect();
            drawHex(graphics, point.x(), point.y(), NODE_RADIUS - 2, 0xFF000000 | icon.getColor());
            graphics.drawCenteredString(font, aspectMark(icon), point.x(), point.y() - 4, 0xFFF8EFD9);
            if (hovered) {
                drawHex(graphics, point.x(), point.y(), NODE_RADIUS + 5, 0x99FFF1C2);
            }
        }
    }

    private void drawResearchTooltip(GuiGraphics graphics, Research entry, int mouseX, int mouseY) {
        boolean learned = ClientArcaneProgress.hasResearch(entry.id());
        boolean available = ResearchRegistry.isAvailable(entry, ClientArcaneProgress.getCompletedResearch());
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(entry.getTranslationKey()).withStyle(style -> style.withColor(0xFFFFE77D)));
        lines.add(Component.translatable(ResearchRegistry.requiresCreate(entry) && !ResearchRegistry.isCreateLoaded()
                ? "gui.fredplenter.requires_create"
                : learned ? "gui.fredplenter.learned"
                : available ? "gui.fredplenter.research_available" : "gui.fredplenter.undiscovered"));
        lines.addAll(wrapTooltipText(Component.translatable(entry.getDescriptionKey()), 188));
        lines.add(Component.translatable(entry.method() == Research.Method.PAPER
                ? "gui.fredplenter.research_method_paper"
                : "gui.fredplenter.research_method_aspects"));
        if (entry.method() == Research.Method.ASPECTS) {
            for (com.fredplenter.aspect.AspectStack cost : entry.cost()) {
                lines.add(Component.translatable("gui.fredplenter.aspect_cost_entry",
                        Component.translatable(cost.aspect().getTranslationKey()), cost.amount()));
            }
        }
        for (Spell spell : Spell.values()) {
            if (spell.getResearch().equals(entry.id())) {
                lines.add(Component.translatable("gui.fredplenter.unlocks_spell",
                        Component.translatable(spell.getTranslationKey()))
                        .withStyle(style -> style.withColor(0xFFB8E6FF)));
            }
        }
        lines.add(Component.translatable("gui.fredplenter.prerequisites"));
        if (entry.prerequisite().isEmpty()) {
            lines.add(Component.translatable("gui.fredplenter.no_prerequisites"));
        } else {
            for (ResourceLocation prerequisite : entry.prerequisite()) {
                lines.add(Component.literal("• ").append(Component.translatable(
                        "research." + prerequisite.getNamespace() + "." + prerequisite.getPath() + ".title")));
            }
        }
        if (entry.method() == Research.Method.PAPER) {
            lines.addAll(wrapTooltipText(Component.translatable("gui.fredplenter.research_materials_desc"), 188));
        }
        if (entry.method() == Research.Method.PAPER) {
            String aspectPath = entry.puzzle().stream()
                    .map(aspect -> Component.translatable(aspect.getTranslationKey()).getString())
                    .collect(java.util.stream.Collectors.joining(" → "));
            lines.addAll(wrapTooltipText(
                    Component.translatable("gui.fredplenter.research_path").append(": " + aspectPath), 188));
        }

        int boxWidth = Math.min(210, Math.max(150, lines.stream().mapToInt(font::width).max().orElse(150) + 12));
        int boxHeight = lines.size() * 11 + 8;
        int boxX = Mth.clamp(mouseX + 14, 4, width - boxWidth - 4);
        int boxY = Mth.clamp(mouseY + 12, 4, height - boxHeight - 4);
        graphics.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0xF0100D20);
        graphics.fill(boxX, boxY, boxX + boxWidth, boxY + 1, 0xFFE1B5FF);
        graphics.fill(boxX, boxY + boxHeight - 1, boxX + boxWidth, boxY + boxHeight, 0xFF594172);
        graphics.fill(boxX, boxY, boxX + 1, boxY + boxHeight, 0xFF9B6FC0);
        graphics.fill(boxX + boxWidth - 1, boxY, boxX + boxWidth, boxY + boxHeight, 0xFF9B6FC0);
        for (int index = 0; index < lines.size(); index++) {
            int color = index == 0 ? 0xFFFFE77D : index == 1
                    ? (learned ? 0xFF88E28D : available ? 0xFFD8C18A : 0xFF97909E)
                    : 0xFFE7DDF2;
            graphics.drawString(font, lines.get(index), boxX + 6, boxY + 5 + index * 11, color, false);
        }
    }

    private void updateResearchButton() {
        if (researchButton == null) {
            return;
        }
        researchButton.visible = selectedResearch != null;
        researchButton.active = selectedResearch != null && !requestPending
                && ResearchRegistry.isAvailable(selectedResearch, ClientArcaneProgress.getCompletedResearch())
                && canPayClientSide(selectedResearch);
        if (requestPending && displayedRevision != ClientArcaneProgress.revision()) {
            displayedRevision = ClientArcaneProgress.revision();
            if (ClientArcaneProgress.lastResearchStartId() != null
                    && ClientArcaneProgress.lastResearchStartId().equals(selectedResearch == null
                    ? null : selectedResearch.id())) {
                requestPending = false;
            }
        }
    }

    private boolean canPayClientSide(Research entry) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return false;
        }
        if (entry.method() == Research.Method.PAPER) {
            boolean hasPaper = minecraft.player.getInventory().items.stream()
                    .anyMatch(stack -> stack.is(Items.PAPER));
            boolean hasInkwell = minecraft.player.getInventory().items.stream().anyMatch(InkwellItem::hasInk)
                    || InkwellItem.hasInk(minecraft.player.getOffhandItem());
            return hasPaper && hasInkwell;
        }
        return entry.cost().stream().allMatch(cost ->
                ClientArcaneProgress.getAspectPoints(cost.aspect()) >= cost.amount());
    }

    private void beginSelectedResearch() {
        if (selectedResearch == null || requestPending) {
            return;
        }
        requestPending = true;
        requestStartedAt = System.currentTimeMillis();
        ClientArcaneProgress.clearResearchResult();
        PacketDistributor.sendToServer(new BeginCodexResearchPayload(selectedResearch.id()));
    }

    private List<Component> wrapTooltipText(Component component, int maxWidth) {
        List<Component> wrapped = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : component.getString().split("\\s+")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (!line.isEmpty() && font.width(candidate) > maxWidth) {
                wrapped.add(Component.literal(line.toString()));
                line.setLength(0);
                line.append(word);
            } else {
                if (!line.isEmpty()) {
                    line.append(' ');
                }
                line.append(word);
            }
        }
        if (!line.isEmpty()) {
            wrapped.add(Component.literal(line.toString()));
        }
        return wrapped;
    }

    private List<Research> filteredResearch() {
        String query = searchBox == null ? "" : searchBox.getValue().trim().toLowerCase(java.util.Locale.ROOT);
        return research.stream()
                .filter(entry -> matchesCategory(entry))
                .filter(entry -> query.isEmpty()
                        || Component.translatable(entry.getTranslationKey()).getString()
                        .toLowerCase(java.util.Locale.ROOT).contains(query)
                        || Component.translatable(entry.getDescriptionKey()).getString()
                        .toLowerCase(java.util.Locale.ROOT).contains(query))
                .toList();
    }

    private boolean matchesCategory(Research entry) {
        String selected = CATEGORIES.get(categoryIndex).id();
        return "all".equals(selected) || selected.equals(entry.category());
    }

    private Research findResearchAt(double mouseX, double mouseY) {
        for (Research entry : filteredResearch()) {
            Point point = nodePosition(entry);
            if (distanceSquared(mouseX, mouseY, point.x(), point.y()) <= NODE_RADIUS * NODE_RADIUS) {
                return entry;
            }
        }
        return null;
    }

    private Point nodePosition(Research entry) {
        int contentLeft = frameLeft() + SIDEBAR_WIDTH + 10;
        int contentTop = frameTop() + 35;
        int contentWidth = Math.max(80, width - frameLeft() - contentLeft - 14);
        int contentHeight = Math.max(80, height - frameTop() - contentTop - 18);
        return new Point(contentLeft + Math.round(contentWidth * entry.x()),
                contentTop + Math.round(contentHeight * entry.y()));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int left = frameLeft() + 10;
            int top = frameTop() + 42;
            for (int index = 0; index < CATEGORIES.size(); index++) {
                int y = top + index * 30;
                if (mouseX >= left && mouseX <= left + 30 && mouseY >= y && mouseY <= y + 26) {
                    categoryIndex = index;
                    selectedResearch = null;
                    return true;
                }
            }
            Research clicked = findResearchAt(mouseX, mouseY);
            if (clicked != null) {
                selectedResearch = clicked;
                requestPending = false;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static String aspectMark(Aspect aspect) {
        return switch (aspect) {
            case FLAMMA, AESTUS -> "*";
            case UNDA, VAPOR -> "~";
            case HUMUS, LIMUS -> "+";
            case FLATUS, PROCELLA -> ">";
            case LUX, VITA -> "o";
            case TENEBRAE, MORS -> ".";
            case CINIS, PULVIS -> ":";
            case SOMNUS -> "z";
            case MOTUS -> "^";
        };
    }

    private static void drawLine(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
        for (int step = 0; step <= steps; step++) {
            int x = x1 + (x2 - x1) * step / Math.max(1, steps);
            int y = y1 + (y2 - y1) * step / Math.max(1, steps);
            graphics.fill(x - 1, y - 1, x + 1, y + 1, color);
        }
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

    private static int distanceSquared(double x1, double y1, double x2, double y2) {
        int dx = (int) Math.round(x1 - x2);
        int dy = (int) Math.round(y1 - y2);
        return dx * dx + dy * dy;
    }

    private int frameLeft() {
        return FRAME_MARGIN_X;
    }

    private int frameTop() {
        return FRAME_MARGIN_Y;
    }

    private record Category(String id, String translationKey, String symbol) {
    }

    private record Point(int x, int y) {
    }
}
