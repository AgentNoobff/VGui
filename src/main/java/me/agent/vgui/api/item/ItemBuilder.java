package me.agent.vgui.api.item;

import com.github.retrooper.packetevents.protocol.component.ComponentTypes;
import com.github.retrooper.packetevents.protocol.component.builtin.item.ItemCustomModelData;
import com.github.retrooper.packetevents.protocol.component.builtin.item.ItemLore;
import com.github.retrooper.packetevents.protocol.component.builtin.item.ItemProfile;
import com.github.retrooper.packetevents.protocol.component.builtin.item.ItemUnbreakable;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.nbt.NBTByte;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.nbt.NBTInt;
import com.github.retrooper.packetevents.protocol.nbt.NBTIntArray;
import com.github.retrooper.packetevents.protocol.nbt.NBTList;
import com.github.retrooper.packetevents.protocol.nbt.NBTShort;
import com.github.retrooper.packetevents.protocol.nbt.NBTString;
import com.github.retrooper.packetevents.protocol.nbt.NBTType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Fluent builder for PacketEvents {@link ItemStack}s that writes <em>both</em> modern
 * item components (1.20.5+) and legacy NBT tags, so items render correctly on every
 * client version from 1.8 to the latest.
 *
 * <pre>{@code
 * ItemStack sword = ItemBuilder.of(ItemTypes.DIAMOND_SWORD)
 *     .name(Component.text("Excalibur", NamedTextColor.GOLD))
 *     .lore(Component.text("Legendary"), Component.text("Click to buy"))
 *     .glow()
 *     .build();
 * }</pre>
 */
public final class ItemBuilder {
    private final ItemStack.Builder itemStack;
    private final LegacyComponentSerializer legacy = LegacyComponentSerializer.legacySection();

    // Legacy NBT accumulated and written at build time
    private NBTCompound displayTag;
    private NBTCompound extraTags;

    private ItemBuilder(ItemType type, int amount) {
        this.itemStack = ItemStack.builder()
                .type(Objects.requireNonNull(type, "type"))
                .amount(clampAmount(amount));
    }

    /** Starts building an item of the given type with amount 1. */
    public static ItemBuilder of(ItemType type) {
        return new ItemBuilder(type, 1);
    }

    /** Starts building an item of the given type and amount (clamped to [1, 99]). */
    public static ItemBuilder of(ItemType type, int amount) {
        return new ItemBuilder(type, amount);
    }

    /** Sets the display name. */
    public ItemBuilder name(Component name) {
        Objects.requireNonNull(name, "name");
        itemStack.component(ComponentTypes.CUSTOM_NAME, name);
        display().setTag("Name", new NBTString(legacy.serialize(name)));
        return this;
    }

    /** Sets the lore lines. */
    public ItemBuilder lore(Component... lore) {
        return lore(Arrays.asList(lore));
    }

    /** Sets the lore lines. */
    public ItemBuilder lore(List<Component> lore) {
        Objects.requireNonNull(lore, "lore");
        itemStack.component(ComponentTypes.LORE, new ItemLore(new ArrayList<>(lore)));
        List<NBTString> legacyLore = new ArrayList<>(lore.size());
        for (Component line : lore) {
            legacyLore.add(new NBTString(legacy.serialize(line)));
        }
        display().setTag("Lore", new NBTList<>(NBTType.STRING, legacyLore));
        return this;
    }

    /** Sets the stack size (clamped to [1, 99]). */
    public ItemBuilder amount(int amount) {
        itemStack.amount(clampAmount(amount));
        return this;
    }

    /** Gives the item an enchantment glint without a visible enchantment. */
    public ItemBuilder glow() {
        return glow(true);
    }

    /** Sets or removes the enchantment glint override. */
    public ItemBuilder glow(boolean glow) {
        itemStack.component(ComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, glow);
        if (glow) {
            // 1.8-1.12 read "ench", 1.13-1.20.4 read "Enchantments"; both hidden via HideFlags.
            NBTCompound ench = new NBTCompound();
            ench.setTag("id", new NBTShort((short) 0));
            ench.setTag("lvl", new NBTShort((short) 1));
            extra().setTag("ench", new NBTList<>(NBTType.COMPOUND, List.of(ench)));
            NBTCompound modernEnch = new NBTCompound();
            modernEnch.setTag("id", new NBTString("minecraft:protection"));
            modernEnch.setTag("lvl", new NBTShort((short) 1));
            extra().setTag("Enchantments", new NBTList<>(NBTType.COMPOUND, List.of(modernEnch)));
            extra().setTag("HideFlags", new NBTInt(1));
        } else if (extraTags != null) {
            extraTags.removeTag("ench");
            extraTags.removeTag("Enchantments");
            Number hideFlags = extraTags.getNumberTagValueOrNull("HideFlags");
            if (hideFlags != null) {
                int remainingFlags = hideFlags.intValue() & ~1;
                if (remainingFlags == 0) {
                    extraTags.removeTag("HideFlags");
                } else {
                    extraTags.setTag("HideFlags", new NBTInt(remainingFlags));
                }
            }
        }
        return this;
    }

    /** Sets custom model data (resource-pack driven models, 1.14+). */
    public ItemBuilder customModelData(int data) {
        itemStack.component(ComponentTypes.CUSTOM_MODEL_DATA_LISTS, new ItemCustomModelData(data));
        extra().setTag("CustomModelData", new NBTInt(data));
        return this;
    }

    /** Marks the item unbreakable (also hides the durability bar). */
    public ItemBuilder unbreakable() {
        itemStack.component(ComponentTypes.UNBREAKABLE_MODERN, new ItemUnbreakable(false));
        extra().setTag("Unbreakable", new NBTByte((byte) 1));
        return this;
    }

    /**
     * Sets a player-head texture from a base64 texture value (the {@code Value} field of
     * a {@code textures} property, as found on skin sites). Only meaningful for
     * {@code PLAYER_HEAD} items. See {@link Skulls#texturedHead(String)} for a shortcut.
     */
    public ItemBuilder skullTexture(String base64Texture) {
        Objects.requireNonNull(base64Texture, "base64Texture");
        UUID id = UUID.nameUUIDFromBytes(base64Texture.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        // Modern profile component (1.20.5+)
        List<ItemProfile.Property> properties = new ArrayList<>(1);
        properties.add(new ItemProfile.Property("textures", base64Texture, null));
        itemStack.component(ComponentTypes.PROFILE, new ItemProfile(null, id, properties));

        // Legacy SkullOwner tag (1.16-1.20.4; older clients fall back to a plain head)
        NBTCompound texture = new NBTCompound();
        texture.setTag("Value", new NBTString(base64Texture));
        NBTCompound propertiesTag = new NBTCompound();
        propertiesTag.setTag("textures", new NBTList<>(NBTType.COMPOUND, List.of(texture)));
        NBTCompound skullOwner = new NBTCompound();
        skullOwner.setTag("Id", new NBTIntArray(uuidToIntArray(id)));
        skullOwner.setTag("Properties", propertiesTag);
        extra().setTag("SkullOwner", skullOwner);
        return this;
    }

    /**
     * Sets a player-head skin by player name. The client resolves the skin itself
     * (works for premium account names).
     */
    public ItemBuilder skullOwner(String playerName) {
        Objects.requireNonNull(playerName, "playerName");
        itemStack.component(ComponentTypes.PROFILE,
                new ItemProfile(playerName, null, new ArrayList<>()));
        extra().setTag("SkullOwner", new NBTString(playerName));
        return this;
    }

    /**
     * Sets legacy item data (durability or metadata) for pre-1.13 clients. For example,
     * {@code 14} makes a {@code STAINED_GLASS_PANE} red. Modern clients ignore it.
     */
    public ItemBuilder legacyData(int legacyData) {
        itemStack.legacyData(legacyData);
        return this;
    }

    /** Escape hatch: mutate the underlying PacketEvents builder directly. */
    public ItemBuilder edit(Consumer<ItemStack.Builder> editor) {
        editor.accept(itemStack);
        return this;
    }

    /** Builds the item stack. The builder can be reused afterwards. */
    public ItemStack build() {
        ItemStack built = itemStack.build().copy();
        NBTCompound nbt = built.getNBT() != null ? built.getNBT().copy() : new NBTCompound();
        if (displayTag != null) {
            nbt.setTag("display", displayTag.copy());
        }
        if (extraTags != null) {
            for (var entry : extraTags.getTags().entrySet()) {
                nbt.setTag(entry.getKey(), entry.getValue().copy());
            }
        }
        built.setNBT(nbt.isEmpty() ? null : nbt);
        return built;
    }

    /** Shortcut for {@code ViewItem.of(build())}. */
    public ViewItem asItem() {
        return ViewItem.of(build());
    }

    /** Shortcut for {@code ViewItem.clickable(build(), onClick)}. */
    public ViewItem onClick(Consumer<me.agent.vgui.api.click.ClickContext> onClick) {
        return ViewItem.clickable(build(), onClick);
    }

    private NBTCompound display() {
        if (displayTag == null) {
            displayTag = new NBTCompound();
        }
        return displayTag;
    }

    private NBTCompound extra() {
        if (extraTags == null) {
            extraTags = new NBTCompound();
        }
        return extraTags;
    }

    private static int clampAmount(int amount) {
        return Math.max(1, Math.min(99, amount));
    }

    static int[] uuidToIntArray(UUID uuid) {
        long most = uuid.getMostSignificantBits();
        long least = uuid.getLeastSignificantBits();
        return new int[]{(int) (most >> 32), (int) most, (int) (least >> 32), (int) least};
    }
}
