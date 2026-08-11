package me.agent.vgui.api.item;

import com.github.retrooper.packetevents.protocol.component.ComponentTypes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import me.agent.vgui.testutil.TestPacketEvents;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemBuilderTest {

    @BeforeAll
    static void setup() {
        TestPacketEvents.ensureInitialized();
    }

    @Test
    void buildsTypedStackWithClampedAmount() {
        ItemStack stack = ItemBuilder.of(ItemTypes.DIAMOND, 200).build();
        assertEquals(ItemTypes.DIAMOND, stack.getType());
        assertEquals(99, stack.getAmount());
        assertEquals(1, ItemBuilder.of(ItemTypes.DIAMOND, -5).build().getAmount());
    }

    @Test
    void nameWritesLegacyDisplayTag() {
        ItemStack stack = ItemBuilder.of(ItemTypes.PAPER)
                .name(Component.text("Hello"))
                .build();
        assertNotNull(stack.getNBT());
        assertNotNull(stack.getNBT().getCompoundTagOrNull("display"));
        assertEquals("Hello",
                stack.getNBT().getCompoundTagOrNull("display").getStringTagValueOrNull("Name"));
    }

    @Test
    void loreWritesAllLines() {
        ItemStack stack = ItemBuilder.of(ItemTypes.PAPER)
                .lore(Component.text("line one"), Component.text("line two"))
                .build();
        var lore = stack.getNBT().getCompoundTagOrNull("display").getStringListTagOrNull("Lore");
        assertNotNull(lore);
        assertEquals(2, lore.size());
    }

    @Test
    void glowWritesLegacyEnchantTags() {
        ItemStack stack = ItemBuilder.of(ItemTypes.PAPER).glow().build();
        assertNotNull(stack.getNBT().getTagOrNull("ench"));
        assertNotNull(stack.getNBT().getTagOrNull("Enchantments"));
        assertNotNull(stack.getNBT().getTagOrNull("HideFlags"));
    }

    @Test
    void disablingGlowRemovesLegacyEnchantTags() {
        ItemBuilder builder = ItemBuilder.of(ItemTypes.PAPER).glow();
        builder.build();

        ItemStack stack = builder.glow(false).build();

        assertNull(stack.getNBT());
        assertFalse(stack.getComponent(ComponentTypes.ENCHANTMENT_GLINT_OVERRIDE).orElse(true));
    }

    @Test
    void writesModernCustomModelDataAndUnbreakableComponents() {
        ItemStack stack = ItemBuilder.of(ItemTypes.DIAMOND_SWORD)
                .customModelData(42)
                .unbreakable()
                .build();

        assertEquals(42, stack.getComponent(ComponentTypes.CUSTOM_MODEL_DATA_LISTS)
                .orElseThrow().getLegacyId());
        assertFalse(stack.getComponent(ComponentTypes.UNBREAKABLE_MODERN)
                .orElseThrow().isShowInTooltip());
        assertEquals(42, stack.getNBT().getNumberTagValueOrNull("CustomModelData").intValue());
        assertEquals(1, stack.getNBT().getNumberTagValueOrNull("Unbreakable").intValue());
    }

    @Test
    void builderReuseCannotMutatePreviouslyBuiltStack() {
        ItemBuilder builder = ItemBuilder.of(ItemTypes.PAPER).name(Component.text("first"));
        ItemStack first = builder.build();

        builder.name(Component.text("second"));

        assertEquals("first", first.getNBT().getCompoundTagOrNull("display")
                .getStringTagValueOrNull("Name"));
    }

    @Test
    void viewItemDefensivelyCopiesInputAndOutputStacks() {
        ItemStack source = ItemBuilder.of(ItemTypes.PAPER).build();
        ViewItem item = ViewItem.of(source);
        source.setAmount(7);

        ItemStack exposed = item.item();
        exposed.setAmount(9);

        assertEquals(1, item.item().getAmount());
    }

    @Test
    void plainItemHasNoStrayTags() {
        ItemStack stack = ItemBuilder.of(ItemTypes.PAPER).build();
        assertTrue(stack.getNBT() == null || stack.getNBT().getTags().isEmpty());
        assertNull(ItemBuilder.of(ItemTypes.PAPER).build().getNBT() != null
                ? ItemBuilder.of(ItemTypes.PAPER).build().getNBT().getTagOrNull("display")
                : null);
    }

    @Test
    void skullTextureWritesSkullOwner() {
        ItemStack stack = ItemBuilder.of(ItemTypes.PLAYER_HEAD)
                .skullTexture("dGVzdA==")
                .build();
        assertNotNull(stack.getNBT().getCompoundTagOrNull("SkullOwner"));
    }

    @Test
    void uuidToIntArrayRoundtrip() {
        java.util.UUID uuid = new java.util.UUID(0x0123456789ABCDEFL, 0xFEDCBA9876543210L);
        int[] parts = ItemBuilder.uuidToIntArray(uuid);
        assertEquals(0x01234567, parts[0]);
        assertEquals(0x89ABCDEF, parts[1]);
        assertEquals(0xFEDCBA98, parts[2]);
        assertEquals(0x76543210, parts[3]);
    }
}
