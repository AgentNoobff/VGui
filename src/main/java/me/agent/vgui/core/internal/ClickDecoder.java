package me.agent.vgui.core.internal;

import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow.WindowClickType;
import me.agent.vgui.api.click.ClickType;

/**
 * Decodes raw (mode, button, slot) click packet data into a {@link ClickType}.
 * Pure decoding logic. See the vanilla protocol's "Click Container" documentation.
 */
final class ClickDecoder {

    private ClickDecoder() {
    }

    static ClickType decode(WindowClickType mode, int button, int slot) {
        if (mode == null) {
            return ClickType.UNKNOWN;
        }
        switch (mode) {
            case PICKUP:
                if (slot == -999) {
                    return button == 0 ? ClickType.LEFT_OUTSIDE
                            : button == 1 ? ClickType.RIGHT_OUTSIDE : ClickType.UNKNOWN;
                }
                return button == 0 ? ClickType.LEFT
                        : button == 1 ? ClickType.RIGHT : ClickType.UNKNOWN;
            case QUICK_MOVE:
                return button == 0 ? ClickType.SHIFT_LEFT
                        : button == 1 ? ClickType.SHIFT_RIGHT : ClickType.UNKNOWN;
            case SWAP:
                if (button == 40) {
                    return ClickType.OFFHAND_SWAP;
                }
                return button >= 0 && button <= 8 ? ClickType.NUMBER_KEY : ClickType.UNKNOWN;
            case CLONE:
                return button == 2 ? ClickType.MIDDLE : ClickType.UNKNOWN;
            case THROW:
                return button == 0 ? ClickType.DROP
                        : button == 1 ? ClickType.CTRL_DROP : ClickType.UNKNOWN;
            case QUICK_CRAFT:
                switch (button) {
                    case 0:
                    case 4:
                    case 8:
                        return ClickType.DRAG_START;
                    case 1:
                    case 5:
                    case 9:
                        return ClickType.DRAG_ADD;
                    case 2:
                    case 6:
                    case 10:
                        return ClickType.DRAG_END;
                    default:
                        return ClickType.UNKNOWN;
                }
            case PICKUP_ALL:
                return button == 0 ? ClickType.DOUBLE_CLICK : ClickType.UNKNOWN;
            default:
                return ClickType.UNKNOWN;
        }
    }

    /** Rejects impossible mode/button/slot combinations before handler dispatch. */
    static boolean isValid(WindowClickType mode, int button, int slot, int totalSlots) {
        if (totalSlots < 0) {
            return false;
        }
        ClickType type = decode(mode, button, slot);
        if (type == ClickType.UNKNOWN) {
            return false;
        }
        boolean inside = slot >= 0 && slot < totalSlots;
        if (type.isOutside()) {
            return slot == -999;
        }
        if (type == ClickType.DRAG_START || type == ClickType.DRAG_END) {
            return slot == -999;
        }
        return inside;
    }

    /** The hotbar key in [0, 8] for NUMBER_KEY clicks, otherwise -1. */
    static int hotbarKey(WindowClickType mode, int button) {
        if (mode == WindowClickType.SWAP && button >= 0 && button <= 8) {
            return button;
        }
        return -1;
    }
}
