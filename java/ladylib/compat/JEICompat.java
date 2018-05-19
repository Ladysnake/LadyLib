package ladylib.compat;

import ladylib.LadyLib;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.ingredients.IIngredientBlacklist;
import net.minecraft.item.ItemStack;

@JEIPlugin
public class JEICompat implements IModPlugin {
    @Override
    public void register(IModRegistry registry) {
        blacklistInvisibleItems(registry.getJeiHelpers().getIngredientBlacklist());
    }

    private void blacklistInvisibleItems(IIngredientBlacklist blacklist) {
        LadyLib.instance.getItemRegistrar().getInvisibleItems()
                .map(ItemStack::new)
                .forEach(blacklist::addIngredientToBlacklist);
    }
}
