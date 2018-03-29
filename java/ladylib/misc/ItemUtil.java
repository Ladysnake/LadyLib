package ladylib.misc;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.stats.StatList;

import javax.annotation.Nonnull;
import java.util.Objects;

public class ItemUtil {

    @Nonnull
    public static NBTTagCompound getOrCreateCompound(@Nonnull ItemStack stack) {
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) {
            nbt = new NBTTagCompound();
            stack.setTagCompound(nbt);
        }
        return nbt;
    }

    /**
     * Turns an {@link ItemStack} into another inside of an entity's inventory.
     * <p>
     * If the entity is a player, the result will be inserted. If the entity is not a player or
     * its inventory is full, the result will be dropped at its feet. <br/>
     * The old stack will be shrank by the size of the result stack.
     * </p>
     * Note: this method makes no check that the resulting stack is of an appropriate size
     * @param fromStack The stack that is being turned
     * @param bearer The entity holding the stack
     * @param toStack The resulting stack
     * @param affectCreative If false, the transformation will not occur for players in creative mode
     * @return The remainder of the old stack after being converted
     */
    public static ItemStack turnItemIntoAnother(ItemStack fromStack, EntityLivingBase bearer, ItemStack toStack, boolean affectCreative) {
        if (!affectCreative && bearer instanceof EntityPlayer && ((EntityPlayer) bearer).isCreative())
            return fromStack;
        fromStack.shrink(toStack.getCount());
        if (bearer instanceof EntityPlayer)
            ((EntityPlayer)bearer).addStat(Objects.requireNonNull(StatList.getObjectUseStats(fromStack.getItem())));

        if (fromStack.isEmpty()) {
            return toStack;
        } else {
            if (bearer instanceof EntityPlayer) {
                if (!((EntityPlayer)bearer).inventory.addItemStackToInventory(toStack)) {
                    ((EntityPlayer)bearer).dropItem(toStack, false);
                }
            } else {
                bearer.entityDropItem(toStack, 0);
            }

            return fromStack;
        }
    }
}
