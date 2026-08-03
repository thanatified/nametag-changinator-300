package com.example.nametagchanger.mixin;

import com.example.nametagchanger.NametagConfig;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * As of the 1.21.2+ entity-render-state refactor, renderLabelIfPresent no
 * longer receives the live Entity - it receives a per-frame snapshot
 * (PlayerEntityRenderState) that already carries the player's username in
 * its public `name` field. We use that to look up an override and swap
 * the Text that gets drawn above the player's head.
 *
 * 100% client-side and cosmetic: nothing here touches the real player
 * profile, the tab list, chat, or anything sent to the server.
 */
@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {

    @ModifyVariable(method = "renderLabelIfPresent", at = @At("HEAD"), argsOnly = true)
    private Text nametagchanger$modifyLabel(Text text, PlayerEntityRenderState state) {
        String override = NametagConfig.getOverride(state.name);
        if (override != null && !override.isEmpty()) {
            return Text.literal(override);
        }
        return text;
    }
}
