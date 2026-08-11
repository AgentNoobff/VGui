package me.agent.vgui.api.input;

import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.velocitypowered.api.proxy.Player;
import me.agent.vgui.api.CloseReason;
import me.agent.vgui.api.View;
import me.agent.vgui.api.ViewType;
import me.agent.vgui.api.click.ClickContext;
import me.agent.vgui.api.contents.ViewContents;
import me.agent.vgui.api.context.ViewContext;
import me.agent.vgui.api.context.ViewKey;
import me.agent.vgui.api.item.ItemBuilder;
import me.agent.vgui.api.item.ViewItem;
import net.kyori.adventure.text.Component;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * A text-input prompt rendered as an anvil window: the player types in the rename
 * field and clicks the output slot to confirm.
 *
 * <pre>{@code
 * AnvilInputView.builder()
 *     .title(Component.text("Choose a nickname"))
 *     .initialText("Steve")
 *     .onConfirm((player, text) -> setNickname(player, text))
 *     .open(player);
 * }</pre>
 *
 * <p>For a future-based one-liner use {@link me.agent.vgui.api.VGui#prompt}.</p>
 */
public final class AnvilInputView implements View {

    /** Context key holding the text currently typed into the rename field. */
    public static final ViewKey<String> TEXT = ViewKey.of("vgui:anvil_text", String.class);

    private static final ViewKey<Boolean> CONFIRMED = ViewKey.of("vgui:anvil_confirmed", Boolean.class);

    private final Component title;
    private final String initialText;
    private final ItemType itemType;
    private final BiConsumer<Player, String> onConfirm;
    private final Consumer<Player> onCancel;
    private final BiConsumer<String, ViewContents> onType;

    private AnvilInputView(Builder builder) {
        this.title = builder.title;
        this.initialText = builder.initialText;
        this.itemType = builder.itemType;
        this.onConfirm = builder.onConfirm;
        this.onCancel = builder.onCancel;
        this.onType = builder.onType;
    }

    /** Starts building an anvil input prompt. */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Component title() {
        return title;
    }

    @Override
    public ViewType type() {
        return ViewType.ANVIL;
    }

    @Override
    public void onOpen(ViewContents contents) {
        contents.context().set(TEXT, initialText);
        contents.set(0, ViewItem.of(ItemBuilder.of(itemType)
                .name(Component.text(initialText))
                .build()));
        contents.set(2, confirmItem(initialText));
    }

    @Override
    public void onAnvilInput(String text, ViewContents contents) {
        contents.context().set(TEXT, text);
        contents.set(2, confirmItem(text));
        if (onType != null) {
            onType.accept(text, contents);
        }
    }

    @Override
    public void onClose(Player player, CloseReason reason, ViewContext context) {
        if (onCancel != null && !Boolean.TRUE.equals(context.get(CONFIRMED))) {
            onCancel.accept(player);
        }
    }

    private ViewItem confirmItem(String text) {
        ItemStack item = ItemBuilder.of(itemType)
                .name(Component.text(text))
                .lore(Component.text("Click to confirm"))
                .glow()
                .build();
        return ViewItem.clickable(item, this::confirm);
    }

    private void confirm(ClickContext click) {
        String text = click.context().get(TEXT);
        click.context().set(CONFIRMED, Boolean.TRUE);
        click.close();
        if (onConfirm != null) {
            onConfirm.accept(click.player(), text != null ? text : initialText);
        }
    }

    /** Builder for {@link AnvilInputView}. */
    public static final class Builder {
        private Component title = Component.text("Enter text");
        private String initialText = "";
        private ItemType itemType = ItemTypes.PAPER;
        private BiConsumer<Player, String> onConfirm;
        private Consumer<Player> onCancel;
        private BiConsumer<String, ViewContents> onType;

        private Builder() {
        }

        /** Window title. */
        public Builder title(Component title) {
            this.title = Objects.requireNonNull(title, "title");
            return this;
        }

        /** Text pre-filled in the rename field. */
        public Builder initialText(String initialText) {
            this.initialText = Objects.requireNonNull(initialText, "initialText");
            return this;
        }

        /** Item type shown in the input and output slots (default paper). */
        public Builder itemType(ItemType itemType) {
            this.itemType = Objects.requireNonNull(itemType, "itemType");
            return this;
        }

        /** Called when the player clicks the output slot to confirm. */
        public Builder onConfirm(BiConsumer<Player, String> onConfirm) {
            this.onConfirm = onConfirm;
            return this;
        }

        /** Called when the player closes the prompt without confirming. */
        public Builder onCancel(Consumer<Player> onCancel) {
            this.onCancel = onCancel;
            return this;
        }

        /** Called on every keystroke with the full current text. */
        public Builder onType(BiConsumer<String, ViewContents> onType) {
            this.onType = onType;
            return this;
        }

        /** Builds the view. */
        public AnvilInputView build() {
            return new AnvilInputView(this);
        }

        /** Builds the view and opens it for a player. */
        public void open(Player player) {
            me.agent.vgui.api.VGui.open(player, build());
        }
    }
}
