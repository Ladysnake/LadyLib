package ladylib;

import com.google.common.base.Strings;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.function.Supplier;

public class LLibContainer {
    private static Map<String, Configuration> configs;

    private ModContainer owner;
    private CreativeTabs creativeTab;

    public LLibContainer(ModContainer owner) {
        this.owner = owner;
    }

    /**
     * This method creates a creative tab for the enclosing mod, sets its icon item
     * to the provided one, and makes it the default.
     *
     * @param icon a supplier for the tab's icon itemstack
     * @return the generated creative tab
     */
    public CreativeTabs makeCreativeTab(Supplier<ItemStack> icon) {
        CreativeTabs ret = new CreativeTabs(owner.getName()) {
            @Nonnull
            @Override
            public ItemStack createIcon() {
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

    /**
     * Gets the specific configuration object used by forge's annotation system,
     * as it will not take newly created config objects into account.
     *
     * @param name the {@link Config#name() name} of the configuration
     * @return the configuration object used by forge for this mod's given config
     */
    @SuppressWarnings("unchecked")
    public Configuration getMainConfiguration(@Nullable String name) {
        if (Strings.isNullOrEmpty(name)) {
            name = getModId();
        }
        File configDir = Loader.instance().getConfigDir();
        File configFile = new File(configDir, name + ".cfg");
        if (configs == null) {
            try {
                Field configsField = ConfigManager.class.getDeclaredField("CONFIGS");
                configsField.setAccessible(true);
                configs = (Map<String, Configuration>) configsField.get(null);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException("Error while attempting to access internal configuration", e);
            }
        }
        return configs.get(configFile.getAbsolutePath());
    }

    @Nonnull
    public String getModId() {
        return owner.getModId();
    }

    /**
     * Adds a resource to be loaded as an override for an existing minecraft resource
     * <p>
     * The path to the resource will be constructed as <code>assets/[modid]/[path]/overrides/[file]</code>,
     * where <code>modid</code> is the mod id of this container's owner.
     * <p>
     * Note: Overrides have minimal priority, only higher than vanilla.
     * Any resourcepack selected by users will function as usual.
     *
     * @param path  the path to the parent directory of the resource
     * @param files the name of one or more files, extension included
     */
    public void addVanillaResourceOverride(String path, String... files) {
        LadyLib.INSTANCE.clientHandler.addResourceOverride(getModId(), path, files);
    }
}
