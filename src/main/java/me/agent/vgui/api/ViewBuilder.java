package me.agent.vgui.api;

import me.agent.vgui.api.click.ClickContext;
import me.agent.vgui.api.contents.Layout;
import me.agent.vgui.api.contents.ViewContents;
import me.agent.vgui.api.context.ViewContext;
import me.agent.vgui.api.item.ViewItem;
import net.kyori.adventure.text.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Fluent builder for views. It builds complete menus without subclassing {@link View}.
 * Obtain via {@link VGui#chest(int)} or {@link VGui#view(ViewType)}.
 *
 * <p>The produced {@link View} is immutable and reusable: open it for any number of
 * players; per-player state lives in each player's {@link ViewContents}.</p>
 */
public final class ViewBuilder {
    private final ViewType type;
    private Component title = Component.text("Menu");
    private String[] layoutRows;
    private final Map<Character, ViewItem> layoutMappings = new HashMap<>();
    private final Map<Integer, ViewItem> items = new LinkedHashMap<>();
    private final List<Update> updates = new ArrayList<>();
    private Consumer<ViewContents> onOpen;
    private Consumer<ClickContext> onClick;
    private CloseHandler onClose;
    private BiConsumer<String, ViewContents> onAnvilInput;
    private boolean cancelClientTransactions = true;
    private long clickCooldownMillis;

    ViewBuilder(ViewType type) {
        this.type = Objects.requireNonNull(type, "type");
    }

    /** Sets the window title. */
    public ViewBuilder title(Component title) {
        this.title = Objects.requireNonNull(title, "title");
        return this;
    }

    /**
     * Sets a character-mask layout. Row count and width must match the view type
     * (e.g. 9 characters per row for chests). Map characters with {@link #map}.
     */
    public ViewBuilder layout(String... rows) {
        this.layoutRows = Objects.requireNonNull(rows, "rows").clone();
        return this;
    }

    /** Maps a layout character to an item ({@code '.'} and {@code ' '} stay empty). */
    public ViewBuilder map(char character, ViewItem item) {
        layoutMappings.put(character, Objects.requireNonNull(item, "item"));
        return this;
    }

    /** Places an item at a flat slot index (applied after the layout). */
    public ViewBuilder item(int slot, ViewItem item) {
        if (slot < 0 || slot >= type.slots()) {
            throw new IllegalArgumentException("Slot " + slot + " out of bounds for " + type + " [0, " + type.slots() + ")");
        }
        items.put(slot, Objects.requireNonNull(item, "item"));
        return this;
    }

    /** Places an item at a row/column position (applied after the layout). */
    public ViewBuilder item(int row, int column, ViewItem item) {
        if (row < 0 || row >= type.rows()) {
            throw new IllegalArgumentException("Row " + row + " out of bounds for " + type);
        }
        if (column < 0 || column >= type.columns()) {
            throw new IllegalArgumentException("Column " + column + " out of bounds for " + type);
        }
        return item(row * type.columns() + column, item);
    }

    /** Runs after the layout and items are applied when the view opens for a player. */
    public ViewBuilder onOpen(Consumer<ViewContents> onOpen) {
        this.onOpen = onOpen;
        return this;
    }

    /** Runs for every click (after any per-item handler). */
    public ViewBuilder onClick(Consumer<ClickContext> onClick) {
        this.onClick = onClick;
        return this;
    }

    /** Runs when the view closes. */
    public ViewBuilder onClose(CloseHandler onClose) {
        this.onClose = onClose;
        return this;
    }

    /** Runs on anvil rename input (only for {@link ViewType#ANVIL} views). */
    public ViewBuilder onAnvilInput(BiConsumer<String, ViewContents> onAnvilInput) {
        this.onAnvilInput = onAnvilInput;
        return this;
    }

    /**
     * Registers a repeating update task started when the view opens and cancelled when
     * it closes. This is useful for animations and live data.
     */
    public ViewBuilder updateEvery(Duration interval, Consumer<ViewContents> task) {
        updates.add(new Update(Objects.requireNonNull(interval, "interval"), Objects.requireNonNull(task, "task")));
        return this;
    }

    /** See {@link View#cancelClientTransactions()}. Default {@code true}. */
    public ViewBuilder cancelClientTransactions(boolean cancel) {
        this.cancelClientTransactions = cancel;
        return this;
    }

    /** See {@link View#clickCooldownMillis()}. Default: disabled. */
    public ViewBuilder clickCooldown(Duration cooldown) {
        Duration value = Objects.requireNonNull(cooldown, "cooldown");
        if (value.isNegative()) {
            throw new IllegalArgumentException("cooldown must not be negative");
        }
        this.clickCooldownMillis = value.toMillis();
        return this;
    }

    /** Builds the immutable, reusable view. */
    public View build() {
        Layout layout = null;
        if (layoutRows != null) {
            layout = Layout.of(layoutRows);
            if (layout.size() != type.slots() || layout.columns() != type.columns()) {
                throw new IllegalArgumentException(
                        "Layout is " + layout.rows() + "x" + layout.columns() + " but " + type + " needs "
                                + type.rows() + "x" + type.columns());
            }
            for (Map.Entry<Character, ViewItem> mapping : layoutMappings.entrySet()) {
                layout.where(mapping.getKey(), mapping.getValue());
            }
        } else if (!layoutMappings.isEmpty()) {
            throw new IllegalStateException("map(...) was called but no layout(...) was set");
        }
        return new BuiltView(this, layout);
    }

    private record Update(Duration interval, Consumer<ViewContents> task) {
    }

    private static final class BuiltView implements View {
        private final ViewType type;
        private final Component title;
        private final Layout layout;
        private final Map<Integer, ViewItem> items;
        private final List<Update> updates;
        private final Consumer<ViewContents> onOpen;
        private final Consumer<ClickContext> onClick;
        private final CloseHandler onClose;
        private final BiConsumer<String, ViewContents> onAnvilInput;
        private final boolean cancelClientTransactions;
        private final long clickCooldownMillis;

        private BuiltView(ViewBuilder builder, Layout layout) {
            this.type = builder.type;
            this.title = builder.title;
            this.layout = layout;
            this.items = Map.copyOf(builder.items);
            this.updates = List.copyOf(builder.updates);
            this.onOpen = builder.onOpen;
            this.onClick = builder.onClick;
            this.onClose = builder.onClose;
            this.onAnvilInput = builder.onAnvilInput;
            this.cancelClientTransactions = builder.cancelClientTransactions;
            this.clickCooldownMillis = builder.clickCooldownMillis;
        }

        @Override
        public Component title() {
            return title;
        }

        @Override
        public ViewType type() {
            return type;
        }

        @Override
        public void onOpen(ViewContents contents) {
            if (layout != null) {
                contents.applyLayout(layout);
            }
            items.forEach(contents::set);
            for (Update update : updates) {
                contents.schedule(update.interval(), update.task());
            }
            if (onOpen != null) {
                onOpen.accept(contents);
            }
        }

        @Override
        public void onClick(ClickContext click) {
            if (onClick != null) {
                onClick.accept(click);
            }
        }

        @Override
        public void onClose(com.velocitypowered.api.proxy.Player player, CloseReason reason, ViewContext context) {
            if (onClose != null) {
                onClose.onClose(player, reason, context);
            }
        }

        @Override
        public void onAnvilInput(String text, ViewContents contents) {
            if (onAnvilInput != null) {
                onAnvilInput.accept(text, contents);
            }
        }

        @Override
        public boolean cancelClientTransactions() {
            return cancelClientTransactions;
        }

        @Override
        public long clickCooldownMillis() {
            return clickCooldownMillis;
        }
    }
}
