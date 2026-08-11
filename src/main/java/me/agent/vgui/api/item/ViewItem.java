package me.agent.vgui.api.item;

import com.github.retrooper.packetevents.protocol.item.ItemStack;
import me.agent.vgui.api.View;
import me.agent.vgui.api.click.ClickContext;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * An item shown in a view slot, optionally with a click handler. Immutable; safe to
 * share between slots, views and players.
 */
public final class ViewItem {
    private final ItemStack item;
    private final Consumer<ClickContext> onClick;

    private ViewItem(ItemStack item, Consumer<ClickContext> onClick) {
        this.item = Objects.requireNonNull(item, "item").copy();
        this.onClick = onClick;
    }

    /** A display-only item without a click handler. */
    public static ViewItem of(ItemStack item) {
        return new ViewItem(item, null);
    }

    /** A display-only item built from an {@link ItemBuilder}. */
    public static ViewItem of(ItemBuilder builder) {
        return new ViewItem(builder.build(), null);
    }

    /** An item that runs {@code onClick} when clicked. */
    public static ViewItem clickable(ItemStack item, Consumer<ClickContext> onClick) {
        return new ViewItem(item, Objects.requireNonNull(onClick, "onClick"));
    }

    /** An item built from an {@link ItemBuilder} that runs {@code onClick} when clicked. */
    public static ViewItem clickable(ItemBuilder builder, Consumer<ClickContext> onClick) {
        return clickable(builder.build(), onClick);
    }

    /** An item that closes the view when clicked. */
    public static ViewItem closeButton(ItemStack item) {
        return clickable(item, ClickContext::close);
    }

    /** An item that returns to the previous view (or closes if there is none) when clicked. */
    public static ViewItem backButton(ItemStack item) {
        return clickable(item, click -> {
            if (!click.back()) {
                click.close();
            }
        });
    }

    /** An item that advances the view's {@link me.agent.vgui.api.contents.Pagination} one page. */
    public static ViewItem pageNext(ItemStack item) {
        return clickable(item, click -> click.contents().pagination().next());
    }

    /** An item that goes back one page in the view's {@link me.agent.vgui.api.contents.Pagination}. */
    public static ViewItem pagePrevious(ItemStack item) {
        return clickable(item, click -> click.contents().pagination().previous());
    }

    /** An item that opens another view (pushing back-history) when clicked. */
    public static ViewItem opens(ItemStack item, View view) {
        Objects.requireNonNull(view, "view");
        return clickable(item, click -> click.open(view));
    }

    /** An item that opens a lazily-created view (pushing back-history) when clicked. */
    public static ViewItem opens(ItemStack item, Supplier<View> view) {
        Objects.requireNonNull(view, "view");
        return clickable(item, click -> click.open(view.get()));
    }

    /** A copy of this item with a different click handler ({@code null} removes it). */
    public ViewItem withClick(Consumer<ClickContext> onClick) {
        return new ViewItem(item, onClick);
    }

    /** A copy of this item with a different item stack. */
    public ViewItem withItem(ItemStack item) {
        return new ViewItem(item, onClick);
    }

    /** A defensive copy of the displayed item stack. */
    public ItemStack item() {
        return item.copy();
    }

    /** Whether this item has a click handler. */
    public boolean hasHandler() {
        return onClick != null;
    }

    /** Runs the click handler, if any. */
    public void handle(ClickContext ctx) {
        if (onClick != null) {
            onClick.accept(ctx);
        }
    }
}
