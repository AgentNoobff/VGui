package me.agent.vgui.core.internal;

import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerOpenWindow;
import me.agent.vgui.api.ViewType;
import net.kyori.adventure.text.Component;

/**
 * Version-aware window opener. Populates BOTH modern (1.14+) menu-type ids and legacy
 * (&lt;= 1.13.x) type strings so PacketEvents can write whichever path the client's
 * protocol version needs.
 */
public final class WindowTypeResolver {

    private WindowTypeResolver() {
    }

    /** Sends the OPEN_WINDOW packet for the given view type, bypassing our own listeners. */
    public static void openWindow(User user, int windowId, ViewType type, Component title) {
        if (title == null) {
            title = Component.text("Menu");
        }
        WrapperPlayServerOpenWindow open = new WrapperPlayServerOpenWindow(
                windowId,
                modernTypeId(type, user.getClientVersion()),
                title,
                legacySlots(type),
                true, // useProvidedWindowTitle (<= 1.7 path only; harmless otherwise)
                0     // horseId (unused)
        );
        open.setLegacyType(legacyType(type));
        user.sendPacketSilently(open);
    }

    /**
     * Modern (1.14+) menu-type registry id. 1.20.3 inserted {@code crafter_3x3} into the
     * registry, shifting every id after {@code generic_3x3} up by one.
     */
    static int modernTypeId(ViewType type, ClientVersion version) {
        boolean shifted = version != null && version.isNewerThanOrEquals(ClientVersion.V_1_20_3);
        switch (type) {
            case HOPPER:
                return shifted ? 16 : 15;
            case DISPENSER:
                return 6;
            case ANVIL:
                return shifted ? 8 : 7;
            default:
                return type.rows() - 1; // generic_9x1..generic_9x6 -> 0..5 on every version
        }
    }

    /** Legacy (&lt;= 1.13.x) window type string. */
    static String legacyType(ViewType type) {
        switch (type) {
            case HOPPER:
                return "minecraft:hopper";
            case DISPENSER:
                return "minecraft:dispenser";
            case ANVIL:
                return "minecraft:anvil";
            default:
                return "minecraft:container";
        }
    }

    /** Legacy slot-count field (only meaningful for generic containers). */
    static int legacySlots(ViewType type) {
        return type.isChest() ? type.slots() : 0;
    }
}
