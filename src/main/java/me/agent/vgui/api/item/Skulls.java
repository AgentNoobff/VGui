package me.agent.vgui.api.item;

import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Shortcuts for building player heads.
 */
public final class Skulls {

    private Skulls() {
    }

    /** A player head with the given base64 texture value. */
    public static ItemStack texturedHead(String base64Texture) {
        return builder(base64Texture).build();
    }

    /** An {@link ItemBuilder} pre-configured as a textured player head. */
    public static ItemBuilder builder(String base64Texture) {
        return ItemBuilder.of(ItemTypes.PLAYER_HEAD).skullTexture(base64Texture);
    }

    /** A player head showing the skin of the given (premium) player name. */
    public static ItemStack ownedHead(String playerName) {
        return ItemBuilder.of(ItemTypes.PLAYER_HEAD).skullOwner(playerName).build();
    }

    /**
     * Builds the base64 texture value for a bare skin URL, e.g.
     * {@code http://textures.minecraft.net/texture/<hash>}.
     */
    public static String textureFromUrl(String skinUrl) {
        String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + skinUrl + "\"}}}";
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }
}
