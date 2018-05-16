package ladylib;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.ModContainer;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.function.Supplier;

public class LLModContainer {
    private ModContainer owner;
    private String ownerModId;
    private CreativeTabs creativeTab;

    public LLModContainer(ModContainer owner) {
        this.owner = owner;
        this.ownerModId = owner.getModId();
    }

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

    public void setCreativeTab(CreativeTabs tab) {
        this.creativeTab = tab;
    }

    public CreativeTabs getCreativeTab() {
        return creativeTab;
    }

    @Nonnull
    public String getModId() {
        return Objects.requireNonNull(ownerModId, "The enclosing mod's id was not set before calling the library");
    }
}
