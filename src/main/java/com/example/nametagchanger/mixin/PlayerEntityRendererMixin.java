package com.example.nametagchanger.mixin;

import com.example.nametagchanger.NametagConfig;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Mixes into the generic EntityRenderer (rather than PlayerEntityRenderer
 * specifically) because renderLabelIfPresent is declared there. We only
 * act when the entity being rendered is actually a player.
 *
 * This purely swaps the Text that gets drawn above the entity's head;
 * it does not touch the player's actual name/profile, the tab list,
 * chat, or anything sent to the server. It is 100% client-side and
 * cosmetic, visible only to the user running this mod.
 */
@Mixin(EntityRenderer.class)
public abstract class PlayerEntityRendererMixin {

    @ModifyVariable(method = "renderLabelIfPresent", at = @At("HEAD"), argsOnly = true)
    private Text nametagchanger$modifyLabel(Text text, Entity entity) {
        if (entity instanceof AbstractClientPlayerEntity player) {
            String override = NametagConfig.getOverride(player.getUuid());
            if (override != null && !override.isEmpty()) {
                return Text.literal(override);
            }
        }
        return text;
    }
}
