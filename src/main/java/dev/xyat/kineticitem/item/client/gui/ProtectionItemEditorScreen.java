package dev.xyat.kineticitem.item.client.gui;

import dev.xyat.kineticcore.api.client.AdaptiveItemGridRenderer;
import dev.xyat.kineticcore.api.client.GuiRenderUtil;
import dev.xyat.kineticcore.api.client.GuiToastUtil;
import dev.xyat.kineticcore.api.client.ItemSelectorScreen;
import dev.xyat.kineticcore.api.client.ScaledScreen;
import dev.xyat.kineticcore.api.client.gui.GridScrollController;
import dev.xyat.kineticitem.item.network.ItemNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public final class ProtectionItemEditorScreen extends ScaledScreen {
    private static final int PANEL_X = 14;
    private static final int PANEL_Y = 24;
    private static final int PANEL_W = 612;
    private static final int PANEL_H = 296;
    private static final int GRID_X = 26;
    private static final int GRID_Y = 50;
    private static final int SLOT_SIZE = 40;
    private static final int SLOT_GAP = 4;
    private static final int CELL_SIZE = SLOT_SIZE + SLOT_GAP;
    private static final int GRID_COLS = 9;
    private static final int GRID_ROWS = 5;
    private static final int GRID_W = GRID_COLS * CELL_SIZE - SLOT_GAP;
    private static final int GRID_H = GRID_ROWS * CELL_SIZE - SLOT_GAP;
    private static final int SCROLL_X = GRID_X + GRID_W + 5;
    private static final int DETAIL_X = 432;
    private static final int DETAIL_Y = 50;
    private static final int DETAIL_W = 182;
    private static final int DETAIL_H = 216;
    private static final float ITEM_SCALE = 2.25F;

    private final Screen parent;
    private final List<RuleDraft> rules = new ArrayList<>();
    private final GridScrollController gridScroll = new GridScrollController();
    private int selectedIndex = -1;
    private Button fireButton;
    private Button explosionButton;
    private Button glowingButton;
    private Button gravityButton;

    public ProtectionItemEditorScreen(Screen parent, List<String> initialRules) {
        super(Component.translatable("gui.kineticitem.protection_editor.title"));
        this.parent = parent;
        if (initialRules != null) {
            for (String raw : initialRules) {
                RuleDraft rule = RuleDraft.parse(raw);
                if (rule != null) {
                    rules.add(rule);
                }
            }
        }
        if (!rules.isEmpty()) {
            selectedIndex = 0;
        }
        configureResponsiveCanvas(640F, 360F, 6);
    }

    @Override
    protected void initScaled() {
        updateScrollRange();

        addRenderableWidget(Button.builder(
                        Component.translatable("gui.kineticitem.protection_editor.add"),
                        button -> openItemSelector())
                .bounds(26, 328, 104, 20)
                .build());

        addRenderableWidget(Button.builder(
                        Component.translatable("gui.kineticitem.protection_editor.save"),
                        button -> save())
                .bounds(424, 328, 90, 20)
                .build());

        addRenderableWidget(Button.builder(
                        Component.translatable("gui.kineticitem.protection_editor.back"),
                        button -> closeToParent())
                .bounds(524, 328, 90, 20)
                .build());

        fireButton = addRenderableWidget(Button.builder(Component.empty(), button -> toggleFire())
                .bounds(444, 148, 76, 20)
                .build());
        explosionButton = addRenderableWidget(Button.builder(Component.empty(), button -> toggleExplosion())
                .bounds(528, 148, 76, 20)
                .build());
        glowingButton = addRenderableWidget(Button.builder(Component.empty(), button -> toggleGlowing())
                .bounds(444, 176, 76, 20)
                .build());
        gravityButton = addRenderableWidget(Button.builder(Component.empty(), button -> toggleGravity())
                .bounds(528, 176, 76, 20)
                .build());
        refreshRuleButtons();
    }

    private void openItemSelector() {
        if (minecraft == null) {
            return;
        }
        minecraft.setScreen(new ItemSelectorScreen(this, selection -> {
            if (selection == null) {
                return;
            }
            String identifier = selectionIdentifier(selection);
            if (identifier.isBlank()) {
                GuiToastUtil.showToast(Component.translatable("msg.kineticitem.protection_editor.invalid_selection"));
                return;
            }
            for (int i = 0; i < rules.size(); i++) {
                if (rules.get(i).identifier.equals(identifier)) {
                    selectedIndex = i;
                    ensureSelectedVisible();
                    refreshRuleButtons();
                    return;
                }
            }
            rules.add(new RuleDraft(identifier, true, true, false, false));
            selectedIndex = rules.size() - 1;
            updateScrollRange();
            ensureSelectedVisible();
            refreshRuleButtons();
        }));
    }

    private static String selectionIdentifier(ItemSelectorScreen.Selection selection) {
        if (selection.isTag()) {
            return "#" + selection.value().trim();
        }
        if (selection.isMod()) {
            return "@" + selection.value().trim();
        }
        if (!selection.isItem()) {
            return "";
        }
        ItemStack stack = selection.stack();
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null) {
            return "";
        }
        if (stack.getTag() != null && !stack.getTag().isEmpty()) {
            return id + stack.getTag().toString();
        }
        return id.toString();
    }

    private void save() {
        List<String> serialized = new ArrayList<>(rules.size());
        for (RuleDraft rule : rules) {
            serialized.add(rule.serialize());
        }
        ItemNetwork.saveProtectionRules(serialized);
    }

    private void closeToParent() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public void onClose() {
        closeToParent();
    }

    private void toggleFire() {
        RuleDraft rule = selectedRule();
        if (rule == null) return;
        rule.fireImmune = !rule.fireImmune;
        refreshRuleButtons();
    }

    private void toggleExplosion() {
        RuleDraft rule = selectedRule();
        if (rule == null) return;
        rule.explosionImmune = !rule.explosionImmune;
        refreshRuleButtons();
    }

    private void toggleGlowing() {
        RuleDraft rule = selectedRule();
        if (rule == null) return;
        rule.glowing = !rule.glowing;
        refreshRuleButtons();
    }

    private void toggleGravity() {
        RuleDraft rule = selectedRule();
        if (rule == null) return;
        rule.noGravity = !rule.noGravity;
        refreshRuleButtons();
    }

    private void refreshRuleButtons() {
        RuleDraft rule = selectedRule();
        boolean active = rule != null;
        if (fireButton != null) {
            fireButton.active = active;
            fireButton.setMessage(Component.translatable(active && rule.fireImmune
                    ? "gui.kineticitem.protection_editor.fire.on"
                    : "gui.kineticitem.protection_editor.fire.off"));
        }
        if (explosionButton != null) {
            explosionButton.active = active;
            explosionButton.setMessage(Component.translatable(active && rule.explosionImmune
                    ? "gui.kineticitem.protection_editor.explosion.on"
                    : "gui.kineticitem.protection_editor.explosion.off"));
        }
        if (glowingButton != null) {
            glowingButton.active = active;
            glowingButton.setMessage(Component.translatable(active && rule.glowing
                    ? "gui.kineticitem.protection_editor.glowing.on"
                    : "gui.kineticitem.protection_editor.glowing.off"));
        }
        if (gravityButton != null) {
            gravityButton.active = active;
            gravityButton.setMessage(Component.translatable(active && rule.noGravity
                    ? "gui.kineticitem.protection_editor.gravity.off"
                    : "gui.kineticitem.protection_editor.gravity.on"));
        }
    }

    private RuleDraft selectedRule() {
        if (selectedIndex < 0 || selectedIndex >= rules.size()) {
            return null;
        }
        return rules.get(selectedIndex);
    }

    private void updateScrollRange() {
        int rows = (rules.size() + GRID_COLS - 1) / GRID_COLS;
        gridScroll.update(rows, GRID_ROWS);
    }

    private void ensureSelectedVisible() {
        if (selectedIndex < 0) return;
        int row = selectedIndex / GRID_COLS;
        if (row < gridScroll.offset()) {
            gridScroll.setOffset(row);
        } else if (row >= gridScroll.offset() + GRID_ROWS) {
            gridScroll.setOffset(row - GRID_ROWS + 1);
        }
    }

    @Override
    protected void renderScaledBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fillGradient(0, 0, vWidth, vHeight, 0xFF171717, 0xFF0E0E0E);
        GuiRenderUtil.drawPanel(graphics, PANEL_X, PANEL_Y, PANEL_W, PANEL_H, 0xEE1C1C1C, 0xFFAAAAAA);
        GuiRenderUtil.drawPanel(graphics, GRID_X - 5, GRID_Y - 5, GRID_W + 10, GRID_H + 10, 0xEE101010, 0xFF777777);
        GuiRenderUtil.drawPanel(graphics, DETAIL_X, DETAIL_Y, DETAIL_W, DETAIL_H, 0xEE151515, 0xFF777777);
    }

    @Override
    protected void renderScaledForeground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.drawCenteredString(font, title, vWidth / 2, 9, 0xFFFFFF);
        renderGrid(graphics, mouseX, mouseY);
        renderDetails(graphics, mouseX, mouseY);
        graphics.drawString(font, Component.translatable("gui.kineticitem.protection_editor.hint"), 26, 307, 0xFFFFFF, false);
    }

    private void renderGrid(GuiGraphics graphics, int mouseX, int mouseY) {
        int start = gridScroll.offset() * GRID_COLS;
        int end = Math.min(start + GRID_ROWS * GRID_COLS, rules.size());
        for (int index = start; index < end; index++) {
            int local = index - start;
            int col = local % GRID_COLS;
            int row = local / GRID_COLS;
            int x = GRID_X + col * CELL_SIZE;
            int y = GRID_Y + row * CELL_SIZE;
            boolean hovered = contains(mouseX, mouseY, x, y, SLOT_SIZE, SLOT_SIZE);
            ItemStack stack = rules.get(index).preview();
            AdaptiveItemGridRenderer.drawSlot(graphics, stack, x, y, SLOT_SIZE, 4, hovered || index == selectedIndex);
            AdaptiveItemGridRenderer.renderItem(graphics, font, stack, x, y, SLOT_SIZE, ITEM_SCALE, true);
            if (index == selectedIndex) {
                graphics.renderOutline(x, y, SLOT_SIZE, SLOT_SIZE, 0xFFFFFFFF);
            }
        }

        updateScrollRange();
        gridScroll.render(
                graphics,
                mouseX,
                mouseY,
                SCROLL_X,
                GRID_Y,
                6,
                GRID_H,
                18,
                0xFF222222,
                0xFF555555,
                0xFFAAAAAA
        );
    }

    private void renderDetails(GuiGraphics graphics, int mouseX, int mouseY) {
        RuleDraft rule = selectedRule();
        if (rule == null) {
            graphics.drawCenteredString(font, Component.translatable("gui.kineticitem.protection_editor.empty"), DETAIL_X + DETAIL_W / 2, DETAIL_Y + 96, 0xFFFFFF);
            return;
        }

        graphics.drawCenteredString(font, Component.translatable("gui.kineticitem.protection_editor.selected"), DETAIL_X + DETAIL_W / 2, DETAIL_Y + 9, 0xFFFFFF);
        ItemStack stack = rule.preview();
        int previewX = DETAIL_X + (DETAIL_W - 52) / 2;
        int previewY = DETAIL_Y + 27;
        AdaptiveItemGridRenderer.drawSlot(graphics, stack, previewX, previewY, 52, 4, contains(mouseX, mouseY, previewX, previewY, 52, 52));
        AdaptiveItemGridRenderer.renderItem(graphics, font, stack, previewX, previewY, 52, 3.0F, true);

        Component type = Component.translatable(rule.typeKey());
        graphics.drawCenteredString(font, type, DETAIL_X + DETAIL_W / 2, DETAIL_Y + 85, 0xFFFFFF);

        String id = rule.identifier;
        int maxWidth = DETAIL_W - 20;
        if (font.width(id) > maxWidth) {
            id = font.plainSubstrByWidth(id, maxWidth - 8) + "...";
        }
        graphics.drawCenteredString(font, id, DETAIL_X + DETAIL_W / 2, DETAIL_Y + 104, 0xFFFFFF);
    }

    @Override
    protected void renderTooltips(GuiGraphics graphics, int scaledMouseX, int scaledMouseY, int mouseX, int mouseY) {
        int index = gridIndexAt(scaledMouseX, scaledMouseY);
        if (index >= 0) {
            graphics.renderTooltip(font, rules.get(index).preview(), mouseX, mouseY);
            return;
        }
        RuleDraft rule = selectedRule();
        if (rule == null) return;
        int previewX = DETAIL_X + (DETAIL_W - 52) / 2;
        int previewY = DETAIL_Y + 27;
        if (contains(scaledMouseX, scaledMouseY, previewX, previewY, 52, 52)) {
            graphics.renderTooltip(font, rule.preview(), mouseX, mouseY);
        }
    }

    @Override
    protected boolean universalMouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && gridScroll.beginDrag(mouseX, mouseY, SCROLL_X, GRID_Y, 6, GRID_H, 18, 0)) {
            return true;
        }

        int index = gridIndexAt(mouseX, mouseY);
        if (index >= 0) {
            if (button == 1) {
                rules.remove(index);
                if (rules.isEmpty()) {
                    selectedIndex = -1;
                } else if (selectedIndex == index) {
                    selectedIndex = Math.min(index, rules.size() - 1);
                } else if (selectedIndex > index) {
                    selectedIndex--;
                }
                updateScrollRange();
                ensureSelectedVisible();
                refreshRuleButtons();
                return true;
            }
            if (button == 0) {
                selectedIndex = index;
                refreshRuleButtons();
                return true;
            }
        }

        return super.universalMouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected boolean universalMouseReleased(double mouseX, double mouseY, int button) {
        if (gridScroll.release(button)) {
            return true;
        }
        return super.universalMouseReleased(mouseX, mouseY, button);
    }

    @Override
    protected boolean universalMouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (gridScroll.drag(mouseY, GRID_Y, GRID_H, 18)) {
            return true;
        }
        return super.universalMouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    protected boolean universalMouseScrolled(double mouseX, double mouseY, double delta) {
        if (contains(mouseX, mouseY, GRID_X, GRID_Y, GRID_W + 14, GRID_H) && gridScroll.scroll(delta)) {
            return true;
        }
        return super.universalMouseScrolled(mouseX, mouseY, delta);
    }

    private int gridIndexAt(double mouseX, double mouseY) {
        if (!contains(mouseX, mouseY, GRID_X, GRID_Y, GRID_W, GRID_H)) {
            return -1;
        }
        int col = (int) ((mouseX - GRID_X) / CELL_SIZE);
        int row = (int) ((mouseY - GRID_Y) / CELL_SIZE);
        if (col < 0 || col >= GRID_COLS || row < 0 || row >= GRID_ROWS) {
            return -1;
        }
        double localX = mouseX - GRID_X - col * CELL_SIZE;
        double localY = mouseY - GRID_Y - row * CELL_SIZE;
        if (localX >= SLOT_SIZE || localY >= SLOT_SIZE) {
            return -1;
        }
        int index = (gridScroll.offset() + row) * GRID_COLS + col;
        return index >= 0 && index < rules.size() ? index : -1;
    }

    private static boolean contains(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private static final class RuleDraft {
        private final String identifier;
        private boolean fireImmune;
        private boolean explosionImmune;
        private boolean glowing;
        private boolean noGravity;
        private ItemStack cachedPreview;

        private RuleDraft(String identifier, boolean fireImmune, boolean explosionImmune, boolean glowing, boolean noGravity) {
            this.identifier = identifier;
            this.fireImmune = fireImmune;
            this.explosionImmune = explosionImmune;
            this.glowing = glowing;
            this.noGravity = noGravity;
        }

        private static RuleDraft parse(String raw) {
            if (raw == null || raw.isBlank()) return null;
            String[] parts = raw.split(";", 5);
            if (parts.length != 5) return null;
            return new RuleDraft(
                    parts[0].trim(),
                    Boolean.parseBoolean(parts[1].trim()),
                    Boolean.parseBoolean(parts[2].trim()),
                    Boolean.parseBoolean(parts[3].trim()),
                    Boolean.parseBoolean(parts[4].trim())
            );
        }

        private String serialize() {
            return identifier + ";" + fireImmune + ";" + explosionImmune + ";" + glowing + ";" + noGravity;
        }

        private String typeKey() {
            if (identifier.startsWith("@")) {
                return "gui.kineticitem.protection_editor.type.mod";
            }
            if (identifier.startsWith("#")) {
                return "gui.kineticitem.protection_editor.type.tag";
            }
            return "gui.kineticitem.protection_editor.type.item";
        }

        private ItemStack preview() {
            if (cachedPreview == null) {
                cachedPreview = createPreview(identifier);
            }
            return cachedPreview;
        }

        private static ItemStack createPreview(String identifier) {
            if (identifier == null || identifier.isBlank()) {
                return new ItemStack(Items.BARRIER);
            }
            if (identifier.startsWith("@")) {
                String namespace = identifier.substring(1).trim();
                for (Item item : ForgeRegistries.ITEMS.getValues()) {
                    ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
                    if (id != null && id.getNamespace().equals(namespace) && item != Items.AIR) {
                        return new ItemStack(item);
                    }
                }
                return new ItemStack(Items.BARRIER);
            }
            if (identifier.startsWith("#")) {
                ResourceLocation tagId = ResourceLocation.tryParse(identifier.substring(1).trim());
                if (tagId != null) {
                    var tag = ItemTags.create(tagId);
                    for (Item item : ForgeRegistries.ITEMS.getValues()) {
                        if (item == Items.AIR) continue;
                        ItemStack stack = new ItemStack(item);
                        if (stack.is(tag)) {
                            return stack;
                        }
                    }
                }
                return new ItemStack(Items.BARRIER);
            }

            int nbtStart = identifier.indexOf('{');
            String itemId = nbtStart >= 0 ? identifier.substring(0, nbtStart).trim() : identifier.trim();
            ResourceLocation id = ResourceLocation.tryParse(itemId);
            if (id == null) {
                return new ItemStack(Items.BARRIER);
            }
            Item item = ForgeRegistries.ITEMS.getValue(id);
            if (item == null || item == Items.AIR) {
                return new ItemStack(Items.BARRIER);
            }
            ItemStack stack = new ItemStack(item);
            if (nbtStart >= 0) {
                try {
                    stack.setTag(TagParser.parseTag(identifier.substring(nbtStart)));
                } catch (Exception ignored) {
                }
            }
            return stack;
        }
    }
}
