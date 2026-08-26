package dev.xyat.kineticitem.item.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.xyat.kineticcore.api.client.AdaptiveItemGridRenderer;
import dev.xyat.kineticcore.api.client.AdvancedSearchUtil;
import dev.xyat.kineticcore.api.client.GuiRenderUtil;
import dev.xyat.kineticcore.api.client.GuiToastUtil;
import dev.xyat.kineticcore.api.client.ItemCache;
import dev.xyat.kineticcore.api.client.ScaledScreen;
import dev.xyat.kineticitem.item.config.BanItemConfig;
import dev.xyat.kineticitem.item.network.ItemNetwork;
import dev.xyat.kineticitem.item.util.ItemBanControl;
import dev.xyat.kineticcore.api.client.gui.NbtEditorScreen;
import dev.xyat.kineticcore.api.client.gui.GridScrollController;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class MergeItemScreen extends ScaledScreen {
    private EditBox searchBox;
    private EditBox leftSearchBox;
    private Button addBtn, saveBtn, closeBtn, tagFilterBtn;
    private final GridScrollController leftScroll = new GridScrollController();
    private final GridScrollController rightScroll = new GridScrollController();
    private int totalRightH = 0;
    private String selectedTarget = null;
    private boolean isCreatingRule = false;
    private boolean targetTagFilterActive = false;
    private final Set<String> expandedTargets = new HashSet<>();

    private final List<ItemCache.CachedItem> allItemsCache;
    private final Map<String, List<String>> tempRules = new HashMap<>();
    private final List<LeftEntry> leftEntries = new ArrayList<>();
    private List<ItemCache.CachedItem> rightDisplayList = new ArrayList<>();

    private static final int SLOT_SIZE = 30;
    private static final int SCROLL_TRACK_COLOR = 0x802A5C73;
    private static final int SCROLL_THUMB_COLOR = 0xFF55DDFE;
    private static final int SCROLL_THUMB_DRAG_COLOR = 0xFF9AEFFF;
    private static String rememberedLeftSearch = "";
    private static String rememberedRightSearch = "";
    private int leftX, leftY, leftW, leftH;
    private int rightX, rightY, rightW, rightH;
    private int gridCols;
    private int gridAreaH;
    private boolean compactLayout;
    private int rightInfoY;

    public MergeItemScreen() {
        super(Component.translatable("gui.kineticitem.banitem.merge_overview_title"));
        configureResponsiveFluid(
                640f,
                360f,
                4
        );

        this.allItemsCache = ItemSearchCache.getAllItems();
        if (BanItemConfig.data != null && BanItemConfig.data.mergedItems != null) {
            for (Map.Entry<String, List<String>> entry : BanItemConfig.data.mergedItems.entrySet()) {
                this.tempRules.put(entry.getKey(), new ArrayList<>(entry.getValue()));
            }
        }
    }

    @Override
    protected void initScaled() {
        int sidePadding =
                switch (layoutLevel()) {
                    case LARGE -> 14;
                    case NORMAL -> 10;
                    case SMALL -> 8;
                    case COMPACT -> 6;
                };

        int gap = 6;
        int buttonHeight = 20;

        compactLayout =
                isPortraitLayout()
                        || isCompactLayout()
                        || vWidth < 540;

        if (compactLayout) {
            initCompactLayout(sidePadding, gap, buttonHeight);
        } else {
            initWideLayout(sidePadding, gap, buttonHeight);
        }

        updateLeftEntries();
        updateRightPanel();
    }

    private void initWideLayout(int sidePadding, int gap, int buttonHeight) {
        int searchY = 5;
        int panelY = 30;
        leftW = Math.max(120, Math.min(170, vWidth / 4));
        leftX = sidePadding;
        leftY = panelY;
        leftH = Math.max(80, vHeight - panelY - 8);
        rightX = leftX + leftW + gap;
        rightY = panelY;
        rightW = Math.max(SLOT_SIZE + 10, vWidth - sidePadding - rightX);
        rightH = leftH;
        gridCols = Math.max(1, (rightW - 10) / SLOT_SIZE);
        gridAreaH = rightH;

        leftSearchBox = createLeftSearchBox(leftX, searchY, leftW);

        int buttonWidth = 60;
        int closeX = rightX + rightW - buttonWidth;
        int saveX = closeX - gap - buttonWidth;
        int addX = saveX - gap - buttonWidth;
        int rightSearchWidth = Math.max(80, Math.min(150, addX - gap - rightX));

        searchBox = createRightSearchBox(rightX, searchY, rightSearchWidth);
        tagFilterBtn = createTagFilterButton(rightX + rightSearchWidth + 4, searchY, 76);
        addBtn = createAddButton(addX, searchY, buttonWidth, buttonHeight);
        saveBtn = createSaveButton(saveX, searchY, buttonWidth, buttonHeight);
        closeBtn = createCloseButton(closeX, searchY, buttonWidth, buttonHeight);
        rightInfoY = 11;
    }

    private void initCompactLayout(int sidePadding, int gap, int buttonHeight) {
        int contentW = Math.max(120, vWidth - sidePadding * 2);
        leftX = sidePadding;
        leftW = contentW;
        leftSearchBox = createLeftSearchBox(leftX, 5, leftW);

        int buttonY = 30;
        int buttonWidth = Math.max(42, (contentW - gap * 2) / 3);
        addBtn = createAddButton(leftX, buttonY, buttonWidth, buttonHeight);
        saveBtn = createSaveButton(leftX + buttonWidth + gap, buttonY, buttonWidth, buttonHeight);
        closeBtn = createCloseButton(leftX + (buttonWidth + gap) * 2, buttonY, buttonWidth, buttonHeight);

        leftY = 56;
        int minimumRightHeight = SLOT_SIZE * 4;
        int reservedForRight = 20 + 4 + font.lineHeight + 4 + minimumRightHeight + 8;
        int availableForLeft = vHeight - leftY - reservedForRight;
        leftH = Math.max(56, Math.min(120, availableForLeft));

        int rightSearchY = leftY + leftH + 4;
        rightX = sidePadding;
        rightW = contentW;
        int tagWidth = 76;
        int rightSearchWidth = Math.max(80, rightW - tagWidth - 4);
        searchBox = createRightSearchBox(rightX, rightSearchY, rightSearchWidth);
        tagFilterBtn = createTagFilterButton(rightX + rightSearchWidth + 4, rightSearchY, tagWidth);
        rightInfoY = rightSearchY + 25;
        rightY = rightInfoY + font.lineHeight + 4;
        rightH = Math.max(SLOT_SIZE * 2, vHeight - rightY - 8);
        gridCols = Math.max(1, (rightW - 10) / SLOT_SIZE);
        gridAreaH = rightH;
    }

    private EditBox createLeftSearchBox(int x, int y, int width) {
        EditBox box = new EditBox(font, x, y, width, 20, Component.empty());
        box.setResponder(query -> {
            rememberedLeftSearch = query == null ? "" : query;
            updateLeftEntries();
        });
        box.setValue(rememberedLeftSearch);
        addRenderableWidget(box);
        return box;
    }

    private EditBox createRightSearchBox(int x, int y, int width) {
        EditBox box = new EditBox(font, x, y, width, 20, Component.empty());
        box.setResponder(query -> {
            rememberedRightSearch = query == null ? "" : query;
            updateRightPanel();
        });
        box.setValue(rememberedRightSearch);
        addRenderableWidget(box);
        return box;
    }

    private Button createTagFilterButton(int x, int y, int width) {
        Button button = Button.builder(
                Component.translatable("gui.kineticitem.banitem.merge_tag_filter"),
                ignored -> {
                    targetTagFilterActive = !targetTagFilterActive;
                    updateRightPanel();
                }
        ).bounds(x, y, width, 20).build();
        button.visible = false;
        addRenderableWidget(button);
        return button;
    }

    private Button createAddButton(int x, int y, int width, int height) {
        Button button = Button.builder(
                Component.translatable("gui.kineticitem.banitem.merge_add"),
                ignored -> {
                    isCreatingRule = true;
                    selectedTarget = null;
                    targetTagFilterActive = false;
                    updateLeftEntries();
                    updateRightPanel();
                }
        ).bounds(x, y, width, height).build();
        addRenderableWidget(button);
        return button;
    }

    private Button createSaveButton(int x, int y, int width, int height) {
        Button button = Button.builder(
                Component.translatable("gui.kineticitem.banitem.btn.save"),
                ignored -> {
                    BanItemConfig.Data data = new BanItemConfig.Data();
                    data.bannedItems = new ArrayList<>(BanItemConfig.data.bannedItems);
                    data.mergedItems.putAll(buildMergedRulesForSave());
                    ItemNetwork.CHANNEL.sendToServer(
                            new ItemNetwork.SaveBanConfigPacket(BanItemConfig.GSON.toJson(data))
                    );
                    GuiToastUtil.showToast(
                            "banitem_save_success",
                            Component.translatable("gui.kineticitem.banitem.save_success")
                    );
                }
        ).bounds(x, y, width, height).build();
        addRenderableWidget(button);
        return button;
    }

    private Button createCloseButton(int x, int y, int width, int height) {
        Button button = Button.builder(
                Component.translatable("gui.kineticitem.banitem.btn.close"),
                ignored -> {
                    if (minecraft != null) minecraft.setScreen(null);
                }
        ).bounds(x, y, width, height).build();
        addRenderableWidget(button);
        return button;
    }

    private String getSearchDataForId(String idStr) {
        return ItemSearchCache.getSearchDataForId(idStr);
    }

    private void updateLeftEntries() {
        leftEntries.clear();
        String leftQuery = leftSearchBox != null ? leftSearchBox.getValue().toLowerCase(Locale.ROOT).trim() : "";

        for (Map.Entry<String, List<String>> entry : tempRules.entrySet()) {
            String target = entry.getKey();

            boolean targetMatches = leftQuery.isEmpty() || AdvancedSearchUtil.match(getSearchDataForId(target), leftQuery);
            boolean sourceMatches = false;

            if (!leftQuery.isEmpty()) {
                for (String src : entry.getValue()) {
                    if (AdvancedSearchUtil.match(getSearchDataForId(src), leftQuery)) {
                        sourceMatches = true;
                        break;
                    }
                }
            }

            if (targetMatches || sourceMatches) {
                leftEntries.add(new TargetEntry(target, entry.getValue().size()));
                if (expandedTargets.contains(target) || !leftQuery.isEmpty()) {
                    for (String source : entry.getValue()) {
                        if (leftQuery.isEmpty() || targetMatches || AdvancedSearchUtil.match(getSearchDataForId(source), leftQuery)) {
                            leftEntries.add(new SourceEntry(target, source));
                        }
                    }
                }
            }
        }
        int totalLeftH = 0;
        for (LeftEntry e : leftEntries) { e.h = (e instanceof TargetEntry) ? 22 : 12; totalLeftH += e.h + 1; }
        leftScroll.update(totalLeftH, leftH);
    }

    private void updateRightPanel() {
        if (searchBox == null) return;
        searchBox.visible = (isCreatingRule || selectedTarget != null);
        searchBox.active = searchBox.visible;
        updateTagFilterButton();

        if (!searchBox.visible) {
            rightDisplayList = new ArrayList<>();
            totalRightH = 0;
            rightScroll.reset();
            rightScroll.update(0, gridAreaH);
            return;
        }

        String query = searchBox.getValue().toLowerCase(Locale.ROOT).trim();
        Set<String> excluded = buildExcludedIdentifiers();
        Set<ItemUnificationHelper.MergeGroup> targetGroups = targetTagFilterActive && selectedTarget != null ? ItemSearchCache.getUnificationGroupsForId(selectedTarget) : Collections.emptySet();
        int sourceHash = 31 * ItemSearchCache.getAllItemsHash() + ItemSearchCache.hashStrings(excluded);
        if (targetTagFilterActive) sourceHash = 31 * sourceHash + hashGroups(targetGroups);

        rightDisplayList = new ArrayList<>(ItemSearchCache.searchItems(targetTagFilterActive ? "merge_right_unify" : "merge_right", allItemsCache, query, c -> {
            if (excluded.contains(c.idStr) || excluded.contains(getBaseIdentifier(c.idStr))) return false;
            return !targetTagFilterActive || ItemSearchCache.hasAnyUnificationGroup(c.stack, targetGroups);
        }, sourceHash));

        int totalRightRows = (int) Math.ceil((double) rightDisplayList.size() / gridCols);
        totalRightH = totalRightRows * SLOT_SIZE;
        rightScroll.update(totalRightH, gridAreaH);
    }

    private void updateTagFilterButton() {
        if (tagFilterBtn == null) return;
        boolean show = selectedTarget != null && !isCreatingRule;
        Set<ItemUnificationHelper.MergeGroup> groups = show ? ItemSearchCache.getUnificationGroupsForId(selectedTarget) : Collections.emptySet();
        tagFilterBtn.visible = show;
        tagFilterBtn.active = show && !groups.isEmpty();
        if (!tagFilterBtn.active) targetTagFilterActive = false;
        tagFilterBtn.setMessage(Component.translatable(targetTagFilterActive ? "gui.kineticitem.banitem.merge_tag_filter_on" : "gui.kineticitem.banitem.merge_tag_filter"));
    }

    private Set<String> buildExcludedIdentifiers() {
        Set<String> excluded = new HashSet<>(BanItemConfig.data.bannedItems);
        for (String banned : BanItemConfig.data.bannedItems) addIdentifierExclusion(excluded, banned);
        addRuleMapExclusions(excluded, tempRules);
        return excluded;
    }

    private void addRuleMapExclusions(Set<String> excluded, Map<String, List<String>> rules) {
        if (rules == null || rules.isEmpty()) return;
        for (Map.Entry<String, List<String>> entry : rules.entrySet()) {
            addIdentifierExclusion(excluded, entry.getKey());
            if (entry.getValue() != null) {
                for (String source : entry.getValue()) addIdentifierExclusion(excluded, source);
            }
        }
    }

    private void addIdentifierExclusion(Set<String> excluded, String idStr) {
        if (idStr == null || idStr.isEmpty()) return;
        excluded.add(idStr);
        excluded.add(getBaseIdentifier(idStr));
    }

    private String getBaseIdentifier(String idStr) {
        if (idStr == null) return "";
        int bracket = idStr.indexOf('{');
        return bracket == -1 ? idStr : idStr.substring(0, bracket);
    }

    private Map<String, List<String>> buildMergedRulesForSave() {
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : tempRules.entrySet()) {
            List<String> sources = new ArrayList<>(new LinkedHashSet<>(entry.getValue()));
            sources.removeIf(s -> s == null || s.isEmpty() || getBaseIdentifier(s).equals(getBaseIdentifier(entry.getKey())));
            if (sources.isEmpty()) continue;

            String targetWithTags = buildTargetWithMergedTags(entry.getKey());
            List<String> targetSources = result.computeIfAbsent(targetWithTags, k -> new ArrayList<>());
            for (String source : sources) {
                if (!targetSources.contains(source)) targetSources.add(source);
            }
        }
        return result;
    }

    private String buildTargetWithMergedTags(String targetId) {
        return targetId;
    }

    private int hashGroups(Set<ItemUnificationHelper.MergeGroup> groups) {
        if (groups == null || groups.isEmpty()) return 0;
        int hash = 1;
        for (ItemUnificationHelper.MergeGroup group : groups) hash = 31 * hash + group.key().hashCode();
        return hash;
    }


    private void openNbtEditor(String idStr, int context, String targetParent, ItemStack fallbackStack) {
        String baseId;
        String initNbt;
        int bracket = idStr.indexOf('{');
        if (bracket == -1) {
            baseId = idStr;
            initNbt = (fallbackStack != null && fallbackStack.hasTag() && fallbackStack.getTag() != null) ? fallbackStack.getTag().toString() : "";
        } else {
            baseId = idStr.substring(0, bracket);
            initNbt = idStr.substring(bracket);
        }

        if (this.minecraft != null) {
            this.minecraft.setScreen(new NbtEditorScreen(initNbt, (savedNbt) -> {
                String newIdStr = baseId + savedNbt;
                switch (context) {
                    case 0:
                        tempRules.putIfAbsent(newIdStr, new ArrayList<>());
                        selectedTarget = newIdStr; isCreatingRule = false; targetTagFilterActive = false; expandedTargets.add(newIdStr);
                        break;
                    case 1:
                        tempRules.get(selectedTarget).add(newIdStr);
                        break;
                    case 2:
                        List<String> src = tempRules.remove(idStr);
                        if (src == null) src = new ArrayList<>();
                        tempRules.put(newIdStr, src);
                        if (idStr.equals(selectedTarget)) selectedTarget = newIdStr;
                        targetTagFilterActive = false; expandedTargets.remove(idStr); expandedTargets.add(newIdStr);
                        break;
                    case 3:
                        List<String> pSrc = tempRules.get(targetParent);
                        if (pSrc != null) { pSrc.remove(idStr); pSrc.add(newIdStr); }
                        break;
                }
                updateLeftEntries(); updateRightPanel();
            }, this));
        }
    }

    @Override
    protected void renderScaledBackground(@NotNull GuiGraphics g, int smx, int smy, float pt) {
        g.fill(0, 0, vWidth, vHeight, 0xFF303030);
        g.fillGradient(0, 0, vWidth, vHeight, 0xFF222222, 0xFF111111);
        GuiRenderUtil.drawPanel(g, leftX, leftY, leftW, leftH, 0xFF1C1C1C, 0xFF555555);
        GuiRenderUtil.drawPanel(g, rightX, rightY, rightW, rightH, 0xFF1C1C1C, 0xFF555555);
    }

    @Override
    protected void renderScaledForeground(@NotNull GuiGraphics g, int smx, int smy, float pt) {
        int curY = leftY - leftScroll.offset();
        enableVirtualScissor(g, leftX, leftY, leftX + leftW, leftY + leftH);
        for (LeftEntry e : leftEntries) {
            if (curY + e.h >= leftY && curY <= leftY + leftH) {
                e.x = leftX + 2;
                e.y = curY; e.w = leftW - 4; e.render(g, smx, smy);
            }
            curY += e.h + 1;
        }
        g.disableScissor();

        leftScroll.render(
                g,
                smx,
                smy,
                leftX + leftW + 2,
                leftY,
                6,
                leftH,
                20,
                SCROLL_TRACK_COLOR,
                SCROLL_THUMB_COLOR,
                SCROLL_THUMB_DRAG_COLOR
        );

        if (isCreatingRule || selectedTarget != null) {
            int countX = getRightCountX();
            Component countText = Component.literal(String.valueOf(rightDisplayList.size())).withStyle(ChatFormatting.GREEN)
                    .append(Component.literal(" / ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(String.valueOf(allItemsCache.size())).withStyle(ChatFormatting.YELLOW));
            g.drawString(font, countText, countX, rightInfoY, 0xFFFFFF, false);

            int gridX = rightX + 2;
            int gridY = rightY;
            enableVirtualScissor(g, rightX, gridY, rightX + rightW, gridY + gridAreaH);
            for (int i = 0; i < rightDisplayList.size(); i++) {
                int col = i % gridCols;
                int row = i / gridCols;
                int rX = gridX + col * SLOT_SIZE;
                int rY = gridY + row * SLOT_SIZE - rightScroll.offset();
                if (rY + SLOT_SIZE > gridY && rY < gridY + gridAreaH) {
                    ItemStack stack = rightDisplayList.get(i).stack;
                    boolean hovered = smx >= rX && smx < rX + SLOT_SIZE
                            && smy >= rY && smy < rY + SLOT_SIZE;
                    AdaptiveItemGridRenderer.drawSlot(
                            g,
                            rX,
                            rY,
                            SLOT_SIZE,
                            4,
                            hovered
                    );

                    ItemBanControl.withSkip(() -> {
                        AdaptiveItemGridRenderer.renderItem(
                                g,
                                font,
                                stack,
                                rX,
                                rY,
                                SLOT_SIZE,
                                1.5F,
                                false
                        );
                        return null;
                    });
                }
            }
            g.disableScissor();

            rightScroll.render(
                    g,
                    smx,
                    smy,
                    rightX + rightW - 6,
                    gridY,
                    6,
                    gridAreaH,
                    20,
                    SCROLL_TRACK_COLOR,
                    SCROLL_THUMB_COLOR,
                    SCROLL_THUMB_DRAG_COLOR
            );
        }

        if (leftSearchBox != null && leftSearchBox.getValue().isEmpty() && !leftSearchBox.isFocused()) {
            g.drawString(font, Component.translatable("gui.kineticitem.banitem.search.hint"), leftSearchBox.getX() + 6, leftSearchBox.getY() + 6, 0x888888, false);
        }
        if (searchBox != null && searchBox.visible && searchBox.getValue().isEmpty() && !searchBox.isFocused()) {
            g.drawString(font, Component.translatable("gui.kineticitem.banitem.search.hint"), searchBox.getX() + 6, searchBox.getY() + 6, 0x888888, false);
        }
    }

    private int getRightCountX() {
        if (compactLayout) return rightX;
        if (tagFilterBtn != null && tagFilterBtn.visible) {
            return tagFilterBtn.getX() + tagFilterBtn.getWidth() + 8;
        }
        return searchBox.getX() + searchBox.getWidth() + 10;
    }

    private boolean isHoveringButton(Button btn, double mx, double my) { return btn != null && btn.visible && mx >= btn.getX() && mx < btn.getX() + btn.getWidth() && my >= btn.getY() && my < btn.getY() + btn.getHeight();
    }

    @Override
    protected void renderTooltips(@NotNull GuiGraphics g, int smx, int smy, int mx, int my) {
        int tooltipY = smy < leftY ? my + 15 : my;

        if (isHoveringButton(addBtn, smx, smy)) { g.renderTooltip(font, Component.translatable("gui.kineticitem.banitem.tooltip.btn.add"), mx, tooltipY); return;
        }
        if (isHoveringButton(saveBtn, smx, smy)) { g.renderTooltip(font, Component.translatable("gui.kineticitem.banitem.tooltip.btn.save"), mx, tooltipY); return;
        }
        if (isHoveringButton(closeBtn, smx, smy)) { g.renderTooltip(font, Component.translatable("gui.kineticitem.banitem.tooltip.btn.close"), mx, tooltipY); return;
        }
        if (isHoveringButton(tagFilterBtn, smx, smy)) {
            Set<ItemUnificationHelper.MergeGroup> groups = selectedTarget == null ? Collections.emptySet() : ItemSearchCache.getUnificationGroupsForId(selectedTarget);
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.translatable("gui.kineticitem.banitem.tooltip.merge_tag_filter.title"));
            tooltip.add(Component.translatable(
                    groups.isEmpty() ? "gui.kineticitem.banitem.tooltip.merge_tag_filter.none" : "gui.kineticitem.banitem.tooltip.merge_tag_filter.desc",
                    Component.literal(String.valueOf(groups.size())).withStyle(ChatFormatting.AQUA)
            ));
            g.renderComponentTooltip(font, tooltip, mx, tooltipY);
            return;
        }

        if (isCreatingRule || selectedTarget != null) {
            int countX = getRightCountX();
            String rawStr = rightDisplayList.size() + " / " + allItemsCache.size();
            if (smx >= countX && smx < countX + font.width(rawStr) && smy >= rightInfoY && smy < rightInfoY + font.lineHeight) {
                List<Component> tooltip = new ArrayList<>();
                tooltip.add(Component.translatable("gui.kineticitem.banitem.tooltip.count.title"));
                tooltip.add(Component.translatable("gui.kineticitem.banitem.tooltip.count.merge.desc"));
                g.renderComponentTooltip(font, tooltip, mx, tooltipY);
                return;
            }
        }

        if (smx >= leftX && smx < leftX + leftW && smy >= leftY && smy < leftY + leftH) {
            int currentY = leftY - leftScroll.offset();
            for (LeftEntry entry : leftEntries) {
                if (currentY + entry.h >= leftY && currentY <= leftY + leftH) {
                    if (smx >= entry.x && smx < entry.x + entry.w && smy >= currentY && smy < currentY + entry.h) { entry.renderTooltip(g, mx, my);
                        break; }
                }
                currentY += entry.h + 1;
            }
        } else if ((isCreatingRule || selectedTarget != null) && smx >= rightX && smx < rightX + rightW && smy >= rightY && smy < rightY + rightH) {
            int gridX = rightX + 2, gridY = rightY;
            if (smy >= gridY + gridAreaH) return;
            int col = (smx - gridX) / SLOT_SIZE, row = (smy - gridY + rightScroll.offset()) / SLOT_SIZE, idx = row * gridCols + col;
            if (col >= 0 && col < gridCols && idx >= 0 && idx < rightDisplayList.size()) {
                ItemCache.CachedItem ci = rightDisplayList.get(idx);
                List<Component> tt = new ArrayList<>();
                tt.add(ItemCacheHudRenderer.getDisplayNameCustom(ci.stack));
                tt.add(Component.literal(ci.idStr));
                tt.add(Component.empty());
                tt.add(Component.translatable(isCreatingRule ? "gui.kineticitem.banitem.tooltip.set_target" : "gui.kineticitem.banitem.tooltip.add_source"));
                g.renderComponentTooltip(font, tt, mx, my);
            }
        }
    }

    @Override
    protected boolean universalMouseClicked(double smx, double smy, int btn) {
        if (btn == 0
                && leftScroll.beginDrag(
                        smx,
                        smy,
                        leftX + leftW + 2,
                        leftY,
                        6,
                        leftH,
                        20,
                        0
                )) {
            return true;
        }

        int gridY = rightY;

        if (btn == 0
                && rightScroll.beginDrag(
                        smx,
                        smy,
                        rightX + rightW - 6,
                        gridY,
                        6,
                        gridAreaH,
                        20,
                        0
                )) {
            return true;
        }

        if (smx >= leftX && smx < leftX + leftW && smy >= leftY && smy < leftY + leftH) {
            int currentY = leftY - leftScroll.offset();
            for (LeftEntry entry : leftEntries) {
                if (currentY + entry.h >= leftY && currentY <= leftY + leftH) {
                    if (smx >= entry.x && smx < entry.x + entry.w && smy >= currentY && smy < currentY + entry.h) {
                        if (entry.mouseClicked(smx, smy, btn)) return true;
                    }
                }
                currentY += entry.h + 1;
            }
        } else if ((isCreatingRule || selectedTarget != null) && smx >= rightX && smx < rightX + rightW && smy >= rightY && smy < rightY + rightH) {
            int gridX = rightX + 2, col = (int) ((smx - gridX) / SLOT_SIZE), row = (int) ((smy - gridY + rightScroll.offset()) / SLOT_SIZE), idx = row * gridCols + col;
            if (col >= 0 && col < gridCols && idx >= 0 && idx < rightDisplayList.size()) {
                if (btn == 0) {
                    String id = rightDisplayList.get(idx).idStr;
                    if (isCreatingRule) { tempRules.putIfAbsent(id, new ArrayList<>()); selectedTarget = id; isCreatingRule = false; targetTagFilterActive = false; expandedTargets.add(id);
                    }
                    else { tempRules.get(selectedTarget).add(id);
                    }
                    updateLeftEntries(); updateRightPanel(); return true;
                }
            }
        }
        return super.universalMouseClicked(smx, smy, btn);
    }

    @Override
    protected boolean universalMouseReleased(double smx, double smy, int btn) {
        boolean released =
                leftScroll.release(btn)
                        | rightScroll.release(btn);

        return released
                || super.universalMouseReleased(smx, smy, btn);
    }

    @Override
    protected boolean universalMouseDragged(double smx, double smy, int btn, double dx, double dy) {
        if (leftScroll.drag(
                smy,
                leftY,
                leftH,
                20
        )) {
            return true;
        }

        if (rightScroll.drag(
                smy,
                rightY,
                gridAreaH,
                20
        )) {
            return true;
        }

        return super.universalMouseDragged(smx, smy, btn, dx, dy);
    }

    @Override
    protected boolean universalMouseScrolled(double smx, double smy, double d) {
        if (smx >= leftX
                && smx <= leftX + leftW
                && smy >= leftY
                && smy <= leftY + leftH
                && leftScroll.scroll(d, 10)) {
            return true;
        }

        if (smx >= rightX
                && smx <= rightX + rightW
                && smy >= rightY
                && smy <= rightY + rightH
                && rightScroll.scroll(d, SLOT_SIZE)) {
            return true;
        }

        return super.universalMouseScrolled(smx, smy, d);
    }

    abstract static class LeftEntry { int x, y, w, h;
        abstract void render(GuiGraphics g, int mx, int my); abstract boolean mouseClicked(double mx, double my, int btn);
        abstract void renderTooltip(GuiGraphics g, int mx, int my); }

    class TargetEntry extends LeftEntry {
        String id;
        ItemStack stack; int count;
        TargetEntry(String id, int count) { this.id = id; this.stack = BanItemConfig.parseItemStack(id); this.count = count;
        }
        void render(GuiGraphics g, int mx, int my) {
            boolean selected = id.equals(selectedTarget), hover = mx >= x && mx < x + w && my >= y && my < y + h;
            g.fill(x, y, x + w, y + h, selected ? 0xFF4444AA : (hover ? 0xFF333333 : 0xFF222222));
            AdaptiveItemGridRenderer.drawSlot(g, stack, x, y + 1, 20, 4, hover);
            RenderSystem.enableDepthTest();
            ItemBanControl.withSkip(() -> { g.renderItem(stack, x + 2, y + 3); return null; }); RenderSystem.disableDepthTest();
            g.drawString(font, ItemCacheHudRenderer.getDisplayNameCustom(stack), x + 24, y + 7, 0xFFFFFF, false);
            g.drawString(font, Component.translatable("gui.kineticitem.common.count_parentheses", Component.literal(String.valueOf(count)).withStyle(ChatFormatting.YELLOW)), x + w - 24, y + 7, 0xFFFFFF, false);
            g.drawString(font, Component.translatable(expandedTargets.contains(id) ? "gui.kineticitem.common.collapse" : "gui.kineticitem.common.expand"), x + w - 36, y + 7, 0xFFFFFF, false);
        }
        boolean mouseClicked(double mx, double my, int btn) {
            if (Screen.hasShiftDown() && btn == 0) { openNbtEditor(id, 2, "", stack);
                return true; }
            if (btn == 0) { if (expandedTargets.contains(id)) expandedTargets.remove(id);
            else expandedTargets.add(id); selectedTarget = id; isCreatingRule = false; targetTagFilterActive = false; updateLeftEntries(); updateRightPanel(); return true;
            }
            else if (btn == 1) { tempRules.remove(id);
                if (id.equals(selectedTarget)) { selectedTarget = null; targetTagFilterActive = false; } expandedTargets.remove(id); updateLeftEntries(); updateRightPanel(); return true;
            }
            return false;
        }
        void renderTooltip(GuiGraphics g, int mx, int my) {
            List<Component> tt = new ArrayList<>();
            tt.add(ItemCacheHudRenderer.getDisplayNameCustom(stack)); tt.add(Component.literal(id));
            tt.add(Component.translatable("gui.kineticitem.banitem.tooltip.shift_edit_nbt"));
            tt.add(Component.translatable("gui.kineticitem.banitem.tooltip.target_del"));
            g.renderComponentTooltip(font, tt, mx, my);
        }
    }

    class SourceEntry extends LeftEntry {
        String targetId, sourceId;
        ItemStack stack;
        SourceEntry(String t, String s) { targetId = t; sourceId = s; stack = BanItemConfig.parseItemStack(s);
        }
        void render(GuiGraphics g, int mx, int my) {
            boolean hover = mx >= x && mx < x + w && my >= y && my < y + h;
            g.fill(x, y, x + w, y + h, hover ? 0xFF2A2A3D : 0xFF14141E);
            AdaptiveItemGridRenderer.drawSlot(g, stack, x + 10, y, 12, 3, hover);
            RenderSystem.enableDepthTest();
            ItemBanControl.withSkip(() -> { g.pose().pushPose(); g.pose().translate(x + 12, y + 2, 0); g.pose().scale(0.5f, 0.5f, 1.0f); g.renderItem(stack, 0, 0); g.pose().popPose(); return null; });
            RenderSystem.disableDepthTest();
            g.pose().pushPose(); g.pose().translate(x + 24, y + 2.5f, 0); g.pose().scale(0.8f, 0.8f, 1.0f); g.drawString(font, ItemCacheHudRenderer.getDisplayNameCustom(stack), 0, 0, 0xFFAAAAAA, false); g.pose().popPose();
        }
        boolean mouseClicked(double mx, double my, int btn) {
            if (Screen.hasShiftDown() && btn == 0) { openNbtEditor(sourceId, 3, targetId, stack);
                return true; }
            if (btn == 1) { tempRules.get(targetId).remove(sourceId);
                updateLeftEntries(); updateRightPanel(); return true; }
            return false;
        }
        void renderTooltip(GuiGraphics g, int mx, int my) {
            List<Component> tt = new ArrayList<>();
            tt.add(ItemCacheHudRenderer.getDisplayNameCustom(stack)); tt.add(Component.literal(sourceId));
            tt.add(Component.translatable("gui.kineticitem.banitem.tooltip.shift_edit_nbt"));
            tt.add(Component.translatable("gui.kineticitem.banitem.tooltip.source_del"));
            g.renderComponentTooltip(font, tt, mx, my);
        }
    }
}
