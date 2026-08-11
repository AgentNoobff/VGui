# Examples and Recipes

These examples assume VGui is installed as a standalone plugin and declared as a Velocity dependency.

## Reusable menu field

Build a stateless view once:

```java
private final View mainMenu = createMainMenu();

private View createMainMenu() {
    ViewItem filler = ItemBuilder.of(ItemTypes.BLACK_STAINED_GLASS_PANE)
        .name(Component.text(" "))
        .asItem();

    return VGui.chest(3)
        .title(Component.text("Network Menu"))
        .layout(
            "#########",
            "#.s.q.p.#",
            "####c####")
        .map('#', filler)
        .map('s', serverButton("Survival", survivalView))
        .map('q', serverButton("Queue", queueView))
        .map('p', ViewItem.opens(profileIcon(), () -> buildProfileView()))
        .map('c', ViewItem.closeButton(closeIcon()))
        .build();
}
```

## Open from a Velocity command

```java
public final class MenuCommand implements SimpleCommand {
    private final View menu;

    public MenuCommand(View menu) {
        this.menu = menu;
    }

    @Override
    public void execute(Invocation invocation) {
        if (invocation.source() instanceof Player player) {
            VGui.open(player, menu);
        }
    }
}
```

## Per-player profile item

Render player-specific information in `onOpen`:

```java
View profile = VGui.chest(3)
    .title(Component.text("Profile"))
    .onOpen(contents -> {
        Player player = contents.player();
        contents.fillBorder(filler);
        contents.set(13, Skulls.builder(textureFor(player))
            .name(Component.text(player.getUsername()))
            .lore(Component.text("Connected to " + currentServer(player)))
            .asItem());
    })
    .build();
```

The view remains reusable because the player-specific value is written to the session contents.

## Toggle backed by context

```java
private static final ViewKey<Boolean> ENABLED =
    ViewKey.of("settings:enabled", Boolean.class);

private View settingsView() {
    return VGui.chest(1)
        .title(Component.text("Settings"))
        .onOpen(c -> renderToggle(c, Boolean.TRUE.equals(c.context().get(ENABLED))))
        .item(8, ViewItem.backButton(backIcon()))
        .build();
}

private void renderToggle(ViewContents contents, boolean enabled) {
    contents.set(4, ItemBuilder.of(enabled ? ItemTypes.LIME_DYE : ItemTypes.GRAY_DYE)
        .name(Component.text(enabled ? "Enabled" : "Disabled"))
        .onClick(click -> {
            boolean next = !Boolean.TRUE.equals(click.context().get(ENABLED));
            click.context().set(ENABLED, next);
            renderToggle(click.contents(), next);
        }));
}
```

Open with an initial value:

```java
VGui.open(player, settingsView(), context -> context.set(ENABLED, loadSetting(player)));
```

## Paged player list

```java
private View onlinePlayersView() {
    String[] rows = {
        "#########",
        "#ppppppp#",
        "#ppppppp#",
        "#ppppppp#",
        "<###b###>"
    };
    Layout layout = Layout.of(rows);

    return VGui.chest(5)
        .title(Component.text("Online Players"))
        .layout(rows)
        .map('#', filler)
        .map('<', ViewItem.pagePrevious(previousIcon()))
        .map('>', ViewItem.pageNext(nextIcon()))
        .map('b', ViewItem.backButton(backIcon()))
        .onOpen(contents -> {
            List<ViewItem> players = proxy.getAllPlayers().stream()
                .map(this::playerItem)
                .toList();
            contents.pagination().slots(layout, 'p').items(players);
        })
        .build();
}
```

## Live status menu

```java
View status = VGui.chest(1)
    .title(Component.text("Network Status"))
    .updateEvery(Duration.ofSeconds(1), contents -> {
        StatusSnapshot snapshot = statusCache.current();
        contents.batch(c -> {
            c.set(3, onlineItem(snapshot.onlinePlayers()));
            c.set(4, queueItem(snapshot.queueSize()));
            c.set(5, latencyItem(snapshot.averageLatency()));
        });
    })
    .build();
```

Read cached state in the scheduled callback. Do not make an HTTP request every second from the callback.

## Asynchronous database menu

```java
private static final ViewKey<Long> LOAD_ID =
    ViewKey.of("history:load_id", Long.class);

View history = VGui.chest(6)
    .title(Component.text("Loading..."))
    .onOpen(contents -> {
        long loadId = System.nanoTime();
        contents.context().set(LOAD_ID, loadId);
        contents.set(22, loadingItem());

        CompletableFuture
            .supplyAsync(() -> repository.findHistory(contents.player().getUniqueId()), databaseExecutor)
            .thenAccept(records -> proxy.getScheduler()
                .buildTask(plugin, () -> {
                    if (!contents.isOpen()) {
                        return;
                    }
                    if (!Long.valueOf(loadId).equals(contents.context().get(LOAD_ID))) {
                        return;
                    }
                    contents.batch(c -> renderHistory(c, records));
                    contents.title(Component.text("History"));
                })
                .schedule());
    })
    .build();
```

## Confirmation flow

```java
View confirmation = VGui.chest(1)
    .title(Component.text("Confirm purchase"))
    .item(3, ItemBuilder.of(ItemTypes.LIME_CONCRETE)
        .name(Component.text("Confirm"))
        .onClick(click -> {
            UUID playerId = click.player().getUniqueId();
            click.close();
            purchaseExecutor.execute(() -> completePurchase(playerId));
        }))
    .item(5, ItemBuilder.of(ItemTypes.RED_CONCRETE)
        .name(Component.text("Cancel"))
        .onClick(click -> click.back()))
    .build();
```

## Anvil prompt

```java
VGui.prompt(player, Component.text("Search"))
    .thenAccept(query -> {
        if (query == null || query.isBlank()) {
            return;
        }
        searchExecutor.execute(() -> performSearch(player.getUniqueId(), query));
    });
```

## Global permission gate

```java
VGuiListener gate = new VGuiListener() {
    @Override
    public boolean onClick(ClickContext click) {
        if (click.player().hasPermission("menus.use")) {
            return true;
        }
        click.player().sendMessage(Component.text("You cannot use this menu."));
        return false;
    }
};

VGui.addListener(gate);
```

Register once during plugin initialization and remove it during plugin shutdown.
