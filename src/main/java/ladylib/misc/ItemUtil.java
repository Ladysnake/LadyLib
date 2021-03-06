package ladylib.misc;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatList;
import net.minecraftforge.items.ItemHandlerHelper;
import org.apiguardian.api.API;

import javax.annotation.Nonnull;

import static org.apiguardian.api.API.Status.MAINTAINED;

@API(status = MAINTAINED, since = "2.6.2")
public final class ItemUtil {
    private ItemUtil() { }

    /**
     * Gets a stack's NBT compound. If it doesn't exist, it will be created and attached to the item stack.
     * @param stack the stack for which an NBT compound should be retrieved or created
     * @return the stack's nbt compound
     */
    @Nonnull
    @API(status = MAINTAINED, since = "2.6.2")
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
     * its inventory is full, the result will be dropped at its feet. <br>
     * The old stack will be shrank by the size of the result stack.
     * </p>
     * Note: this method makes no check that the resulting stack is of an appropriate size
     * @param fromStack The stack that is being turned
     * @param bearer The entity holding the stack
     * @param toStack The resulting stack
     * @param affectCreative If false, the transformation will not occur for players in creative mode
     * @return The remainder of the old stack after being converted
     */
    @API(status = MAINTAINED, since = "2.6.2")
    public static ItemStack turnItemIntoAnother(ItemStack fromStack, EntityLivingBase bearer, ItemStack toStack, boolean affectCreative) {
        if (!affectCreative && bearer instanceof EntityPlayer && ((EntityPlayer) bearer).isCreative()) {
            return fromStack;
        }
        fromStack.shrink(toStack.getCount());
        if (bearer instanceof EntityPlayer) {
            StatBase stat = StatList.getObjectUseStats(fromStack.getItem());
            if (stat != null) {
                ((EntityPlayer)bearer).addStat(stat);
            }
        }

        if (fromStack.isEmpty()) {
            return toStack;
        } else {
            if (bearer instanceof EntityPlayer) {
                ItemHandlerHelper.giveItemToPlayer((EntityPlayer) bearer, toStack);
            } else {
                bearer.entityDropItem(toStack, 0);
            }

            return fromStack;
        }
    }
}
