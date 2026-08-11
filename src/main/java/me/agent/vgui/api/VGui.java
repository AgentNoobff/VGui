package me.agent.vgui.api;

import com.velocitypowered.api.proxy.Player;
import me.agent.vgui.api.contents.ViewContents;
import me.agent.vgui.api.context.ViewContext;
import me.agent.vgui.api.event.VGuiListener;
import net.kyori.adventure.text.Component;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Static entry point to VGui. The instance is installed by the platform bootstrap
 * ({@code VGuiPlatform} when running as a plugin, or {@code VGuiBootstrap} when shaded
 * as a library) during proxy initialization.
 *
 * <pre>{@code
 * View shop = VGui.chest(3)
 *     .title(Component.text("Shop"))
 *     .layout("#########",
 *             "#..a.b..#",
 *             "####c####")
 *     .map('#', ViewItem.of(filler))
 *     .map('a', ViewItem.clickable(sword, click -> buySword(click.player())))
 *     .map('c', ViewItem.closeButton(barrier))
 *     .build();
 *
 * VGui.open(player, shop);
 * }</pre>
 */
public final class VGui {
    private static volatile VGuiService INSTANCE;

    private VGui() {
    }

    /**
     * The service instance.
     *
     * @throws IllegalStateException if VGui has not initialized yet. Declare a
     *                               dependency on {@code vgui} so it loads first.
     */
    public static VGuiService get() {
        VGuiService instance = INSTANCE;
        if (instance == null) {
            throw new IllegalStateException(
                    "VGui is not initialized yet. Declare @Plugin(dependencies = @Dependency(id = \"vgui\")) "
                            + "so your plugin initializes after VGui, or call VGuiBootstrap.init(...) first "
                            + "when shading VGui as a library.");
        }
        return instance;
    }

    /** Installs the service instance. Called by the platform bootstrap; do not call yourself. */
    public static void setInstance(VGuiService service) {
        INSTANCE = service;
    }

    /** Whether the service is initialized and ready. */
    public static boolean isInitialized() {
        return INSTANCE != null;
    }

    /* ------------------------------------------------------------------ builders */

    /** Starts building a chest view with the given number of rows (1-6). */
    public static ViewBuilder chest(int rows) {
        return new ViewBuilder(ViewType.chestRows(rows));
    }

    /** Starts building a view of the given type. */
    public static ViewBuilder view(ViewType type) {
        return new ViewBuilder(type);
    }

    /* ------------------------------------------------------------------ delegates */

    /** @see VGuiService#open(Player, View) */
    public static void open(Player player, View view) {
        get().open(player, view);
    }

    /** @see VGuiService#open(Player, View, ViewContext) */
    public static void open(Player player, View view, ViewContext context) {
        get().open(player, view, context);
    }

    /** @see VGuiService#open(Player, View, Consumer) */
    public static void open(Player player, View view, Consumer<ViewContext> contextBuilder) {
        get().open(player, view, contextBuilder);
    }

    /** @see VGuiService#openReplacing(Player, View) */
    public static void openReplacing(Player player, View view) {
        get().openReplacing(player, view);
    }

    /** @see VGuiService#back(Player) */
    public static boolean back(Player player) {
        return get().back(player);
    }

    /** @see VGuiService#close(Player) */
    public static void close(Player player) {
        get().close(player);
    }

    /** @see VGuiService#currentView(Player) */
    public static View currentView(Player player) {
        return get().currentView(player);
    }

    /** @see VGuiService#currentContext(Player) */
    public static ViewContext currentContext(Player player) {
        return get().currentContext(player);
    }

    /** @see VGuiService#currentContents(Player) */
    public static ViewContents currentContents(Player player) {
        return get().currentContents(player);
    }

    /** @see VGuiService#isViewing(Player) */
    public static boolean isViewing(Player player) {
        return get().isViewing(player);
    }

    /** @see VGuiService#prompt(Player, Component) */
    public static CompletableFuture<String> prompt(Player player, Component title) {
        return get().prompt(player, title);
    }

    /** @see VGuiService#prompt(Player, Component, String) */
    public static CompletableFuture<String> prompt(Player player, Component title, String initialText) {
        return get().prompt(player, title, initialText);
    }

    /** @see VGuiService#addListener(VGuiListener) */
    public static void addListener(VGuiListener listener) {
        get().addListener(listener);
    }

    /** @see VGuiService#removeListener(VGuiListener) */
    public static void removeListener(VGuiListener listener) {
        get().removeListener(listener);
    }
}
