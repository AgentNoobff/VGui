# Anvil Input

VGui can collect short text through the anvil rename field. Use the future API for simple prompts or `AnvilInputView` for full callbacks.

## Future API

```java
VGui.prompt(player, Component.text("Enter a nickname"), "Steve")
    .thenAccept(text -> {
        if (text == null) {
            player.sendMessage(Component.text("Prompt cancelled"));
            return;
        }
        saveNicknameAsync(player.getUniqueId(), text);
    });
```

The future completes with the entered text after the output slot is clicked. It completes with null when the player closes without confirming.

The completion may occur on the packet-handling thread. Move blocking validation or persistence to an executor.

## Builder API

```java
AnvilInputView prompt = AnvilInputView.builder()
    .title(Component.text("Choose a nickname"))
    .initialText("Steve")
    .itemType(ItemTypes.NAME_TAG)
    .onType((text, contents) -> validateLive(text, contents))
    .onConfirm((player, text) -> applyNickname(player, text))
    .onCancel(player -> player.sendMessage(Component.text("Cancelled")))
    .build();

VGui.open(player, prompt);
```

`Builder.open(player)` is a shortcut for build and open.

## Builder methods

- `title(Component)` sets the window title. Default: `Enter text`.
- `initialText(String)` pre-fills the rename input. Default: empty.
- `itemType(ItemType)` selects the item in the input and output slots. Default: paper.
- `onType(BiConsumer<String, ViewContents>)` runs after every rename packet.
- `onConfirm(BiConsumer<Player, String>)` runs when the output is clicked.
- `onCancel(Consumer<Player>)` runs when the prompt closes without confirmation.

## Text in context

`AnvilInputView.TEXT` is a typed `ViewKey<String>` containing the latest value:

```java
String current = contents.context().get(AnvilInputView.TEXT);
```

The view initializes it from `initialText` and updates it on each rename packet.

## Confirmation behavior

The output slot is rebuilt for every text change. Clicking it marks the context as confirmed, closes the view, and then invokes `onConfirm`.

The internal confirmed marker prevents `onCancel` from running for the same successful prompt.

## Cancellation behavior

`onCancel` runs for every unconfirmed closure, including replacement with
`CloseReason.SWITCHED`, backend override, disconnect, and shutdown. Consequently,
future-based prompts always complete with `null` when navigation replaces them.

## Validating input

The built-in view does not enforce length, character set, uniqueness, or profanity rules. Validate according to the action being performed.

```java
.onType((text, contents) -> {
    boolean valid = text.length() >= 3
        && text.length() <= 16
        && text.chars().allMatch(ch -> Character.isLetterOrDigit(ch) || ch == '_');

    contents.title(Component.text(valid ? "Click to confirm" : "Invalid nickname"));
})
```

Live validation is only guidance. Repeat authoritative validation inside the confirm path because application state may have changed.

## Custom anvil views

Any `View` with `ViewType.ANVIL` receives `onAnvilInput(text, contents)` for rename packets. Use a custom view when you need different slot behavior or navigation. The three top slots are input 0, secondary input 1, and output 2.

## Limitations

- Anvil prompts are for short client text, not multiline input.
- Client and protocol behavior can vary, so test the exact supported versions.
- Opening an anvil does not provide secure input. Never collect passwords, tokens, or other secrets through a Minecraft client.
