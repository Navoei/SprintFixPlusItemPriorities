package me.navoei.sprintfix.mixin.client.ItemPrioritizationMixins;

import me.navoei.sprintfix.client.SprintFixClient;
import me.navoei.sprintfix.util.Feature;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.telemetry.TelemetryProperty;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public abstract class ItemMixin {

    @Inject(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;startUsingItem(Lnet/minecraft/world/InteractionHand;)V", shift = At.Shift.AFTER), cancellable = true)
    protected void injectBlocksAttacksPriority(Level level, Player player, InteractionHand interactionHand, CallbackInfoReturnable<InteractionResult> cir) {
        //Add a check to see if this feature was enabled by the server sending the custom packet.
        player.startUsingItem(interactionHand);
        cir.setReturnValue(InteractionResult.CONSUME);
        if (!SprintFixClient.ENABLED_FEATURES.contains(Feature.FIX_ITEM_PRIORITIES)) return;
        if (interactionHand.equals(InteractionHand.OFF_HAND)) return;
        LocalPlayer localPlayer = Minecraft.getInstance().player;
        if (localPlayer==null) return;
        HitResult hitResult = Minecraft.getInstance().hitResult;
        ItemStack offHandItemStack = localPlayer.getItemInHand(InteractionHand.OFF_HAND);
        ItemStack itemStackInteractionHand = localPlayer.getItemInHand(interactionHand);
        if (!itemStackInteractionHand.is(ItemTags.SWORDS)) return;
        if (!itemStackInteractionHand.has(DataComponents.BLOCKS_ATTACKS)) return;
        if (!offHandItemStack.isEmpty()) {

            if (localPlayer.getCooldowns().isOnCooldown(offHandItemStack)) {
                localPlayer.stopUsingItem();
                player.stopUsingItem();
                localPlayer.interact(player, InteractionHand.OFF_HAND);
                player.interact(player, InteractionHand.OFF_HAND);
                cir.setReturnValue(InteractionResult.PASS);
                return;
            }

            InteractionResult interactionResult = offHandItemStack.use(localPlayer.level(), localPlayer, InteractionHand.OFF_HAND);
            if (interactionResult instanceof InteractionResult.Success) {
                localPlayer.stopUsingItem();
                player.stopUsingItem();
                localPlayer.interact(localPlayer, InteractionHand.OFF_HAND);
                player.interact(player, InteractionHand.OFF_HAND);
                if (offHandItemStack.getUseAnimation().equals(ItemUseAnimation.NONE)) {
                    localPlayer.swing(InteractionHand.OFF_HAND);
                    player.swing(InteractionHand.OFF_HAND);
                }
                cir.setReturnValue(InteractionResult.PASS);
                return;
            }
            if (hitResult!=null && hitResult.getType()== HitResult.Type.BLOCK) {
                if (offHandItemStack.getItem() instanceof BlockItem && !(interactionResult instanceof InteractionResult.Fail) && !hitResult.getType().equals(HitResult.Type.ENTITY)) {
                    localPlayer.stopUsingItem();
                    player.stopUsingItem();
                    localPlayer.interact(localPlayer, InteractionHand.OFF_HAND);
                    player.interact(player, InteractionHand.OFF_HAND);
                    cir.setReturnValue(InteractionResult.PASS);
                    return;
                }
                if (!hitResult.getType().equals(HitResult.Type.ENTITY)) {
                    UseOnContext useOnContext = new UseOnContext(localPlayer, InteractionHand.OFF_HAND, (BlockHitResult) hitResult);
                    int offHandItemCount = localPlayer.getOffhandItem().getCount();
                    interactionResult = localPlayer.getOffhandItem().useOn(useOnContext);
                    localPlayer.getOffhandItem().setCount(offHandItemCount);
                    if (interactionResult instanceof InteractionResult.Success) {
                        localPlayer.stopUsingItem();
                        player.stopUsingItem();
                        localPlayer.interact(localPlayer, InteractionHand.OFF_HAND);
                        player.interact(player, InteractionHand.OFF_HAND);
                        if (interactionResult instanceof InteractionResult.Success) {
                            localPlayer.swing(InteractionHand.OFF_HAND);
                            player.swing(InteractionHand.OFF_HAND);
                        }
                        cir.setReturnValue(InteractionResult.PASS);
                    }
                }
            }
        }
    }

    @Redirect(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;startUsingItem(Lnet/minecraft/world/InteractionHand;)V"))
    protected void redirection(Player instance, InteractionHand interactionHand) {
        if (!SprintFixClient.ENABLED_FEATURES.contains(Feature.FIX_ITEM_PRIORITIES)) {
            instance.startUsingItem(interactionHand);
        }
    }

}

