package me.agent.vgui.core.demo;

import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.sound.Sounds;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import me.agent.vgui.api.VGui;
import me.agent.vgui.api.View;
import me.agent.vgui.api.item.ItemBuilder;
import me.agent.vgui.api.item.ViewItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * {@code /vgui} opens a live tour of layouts, pagination, animation, dynamic updates,
 * navigation history, sounds, and anvil text input. The command also serves as example
 * code for plugin authors.
 */
public final class DemoCommand implements SimpleCommand {

    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player player)) {
            invocation.source().sendMessage(Component.text("Only players can open GUIs.", NamedTextColor.RED));
            return;
        }
        VGui.open(player, mainMenu());
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("vgui.demo");
    }

    private View mainMenu() {
        ViewItem filler = ViewItem.of(ItemBuilder.of(ItemTypes.GRAY_STAINED_GLASS_PANE)
                .name(Component.text(" "))
                .build());

        return VGui.chest(3)
                .title(Component.text("VGui Demo", NamedTextColor.DARK_AQUA))
                .layout(
                        "#########",
                        "#.p.a.i.#",
                        "####c####")
                .map('#', filler)
                .map('p', ViewItem.clickable(
                        ItemBuilder.of(ItemTypes.CHEST)
                                .name(Component.text("Pagination demo", NamedTextColor.GOLD))
                                .lore(Component.text("A paged list of 100 items", NamedTextColor.GRAY))
                                .build(),
                        click -> {
                            click.playSound(Sounds.UI_BUTTON_CLICK);
                            click.open(paginationMenu());
                        }))
                .map('a', ViewItem.clickable(
                        ItemBuilder.of(ItemTypes.NAME_TAG)
                                .name(Component.text("Anvil input demo", NamedTextColor.GOLD))
                                .lore(Component.text("Type text, click to confirm", NamedTextColor.GRAY))
                                .glow()
                                .build(),
                        click -> VGui.prompt(click.player(), Component.text("Enter anything"))
                                .thenAccept(text -> {
                                    if (text != null) {
                                        click.player().sendMessage(Component.text("You typed: " + text, NamedTextColor.GREEN));
                                    } else {
                                        click.player().sendMessage(Component.text("Prompt cancelled.", NamedTextColor.RED));
                                    }
                                })))
                .map('i', ViewItem.clickable(
                        ItemBuilder.of(ItemTypes.CLOCK)
                                .name(Component.text("Animation demo", NamedTextColor.GOLD))
                                .lore(Component.text("A slot updating every second", NamedTextColor.GRAY))
                                .build(),
                        click -> click.open(animationMenu())))
                .map('c', ViewItem.closeButton(ItemBuilder.of(ItemTypes.BARRIER)
                        .name(Component.text("Close", NamedTextColor.RED))
                        .build()))
                .build();
    }

    private View paginationMenu() {
        List<ViewItem> entries = new ArrayList<>(100);
        for (int i = 1; i <= 100; i++) {
            final int number = i;
            entries.add(ViewItem.clickable(
                    ItemBuilder.of(ItemTypes.PAPER)
                            .name(Component.text("Entry #" + number, NamedTextColor.AQUA))
                            .amount(Math.min(64, number))
                            .build(),
                    click -> click.player().sendMessage(Component.text("Clicked entry #" + number))));
        }

        ViewItem filler = ViewItem.of(ItemBuilder.of(ItemTypes.BLACK_STAINED_GLASS_PANE)
                .name(Component.text(" "))
                .build());

        String[] layoutRows = {
                "#########",
                "#·······#",
                "#·······#",
                "#·······#",
                "<###b###>"
        };

        return VGui.chest(5)
                .title(Component.text("Pagination", NamedTextColor.DARK_AQUA))
                .layout(layoutRows)
                .map('#', filler)
                .map('<', ViewItem.pagePrevious(ItemBuilder.of(ItemTypes.ARROW)
                        .name(Component.text("Previous page", NamedTextColor.YELLOW))
                        .build()))
                .map('>', ViewItem.pageNext(ItemBuilder.of(ItemTypes.ARROW)
                        .name(Component.text("Next page", NamedTextColor.YELLOW))
                        .build()))
                .map('b', ViewItem.backButton(ItemBuilder.of(ItemTypes.OAK_DOOR)
                        .name(Component.text("Back", NamedTextColor.RED))
                        .build()))
                .onOpen(contents -> contents.pagination()
                        .slots(me.agent.vgui.api.contents.Layout.of(layoutRows), '·')
                        .items(entries))
                .build();
    }

    private View animationMenu() {
        return VGui.chest(1)
                .title(Component.text("Animation", NamedTextColor.DARK_AQUA))
                .item(8, ViewItem.backButton(ItemBuilder.of(ItemTypes.OAK_DOOR)
                        .name(Component.text("Back", NamedTextColor.RED))
                        .build()))
                .updateEvery(Duration.ofSeconds(1), contents -> {
                    int seconds = (int) ((System.currentTimeMillis() / 1000) % 60);
                    contents.set(4, ViewItem.of(ItemBuilder.of(ItemTypes.CLOCK)
                            .name(Component.text("Seconds: " + seconds, NamedTextColor.GREEN))
                            .amount(Math.max(1, seconds))
                            .build()));
                })
                .build();
    }
}
