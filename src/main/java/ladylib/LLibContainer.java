package ladylib;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.ModContainer;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.function.Supplier;

public class LLibContainer {
    private ModContainer owner;
    private CreativeTabs creativeTab;

    public LLibContainer(ModContainer owner) {
        this.owner = owner;
    }

    /**
     * This method creates a creative tab for the enclosing mod, sets its icon item
     * to the provided one, and makes it the default.
     * @param icon a supplier for the tab's icon itemstack
     * @return the generated creative tab
     */
    public CreativeTabs makeCreativeTab(Supplier<ItemStack> icon) {
        CreativeTabs ret = new CreativeTabs(owner.getName()) {
            @Nonnull
            @Override
            public ItemStack getTabIconItem() {
                return icon.get();
            }
        };
        setCreativeTab(ret);
        return ret;
    }

    /**
     * This method sets the given tab as the default tab used for registration. <br/>
     * Automatically registered items will be assigned to this tab by default.
     *
     * @param tab the mod's default creative tab
     * @see ladylib.registration.AutoRegister
     * @see ladylib.registration.AutoRegister.Unlisted
     */
    public void setCreativeTab(CreativeTabs tab) {
        this.creativeTab = tab;
    }

    public CreativeTabs getCreativeTab() {
        return creativeTab;
    }

    @Nonnull
    public String getModId() {
        return owner.getModId();
    }
}
