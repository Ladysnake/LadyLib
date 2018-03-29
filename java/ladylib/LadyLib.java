package ladylib;

import ladylib.client.ClientHandler;
import ladylib.client.IClientHandler;
import ladylib.registration.AutoRegistrar;
import ladylib.registration.BlockRegistrar;
import ladylib.registration.ItemRegistrar;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.*;
import java.util.function.Supplier;

public class LadyLib {

    public static Logger LOGGER = LogManager.getLogger("LadyLib");
    private static final Map<String, LadyLib> allInstances = new HashMap<>();

    private ModContainer owner;
    private String ownerModId;
    private CreativeTabs creativeTab;
    private AutoRegistrar registrar;
    private File configFolder;

    @SideOnly(Side.CLIENT)
    private ClientHandler clientHandler;

    /**
     * Creates and initializes an instance of this class.
     * Simply calling this is enough to have annotation magic work on your mod.
     * @param event a pre initialization event to get setup information
     * @return an instance of this class, to further interact with the library
     */
    public static LadyLib initLib(FMLPreInitializationEvent event) {
        LadyLib ret = new LadyLib();
        ret.preInit(event);
        allInstances.put(ret.ownerModId, ret);
        return ret;
    }

    public static boolean isDevEnv() {
        return (Boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment");
    }

    public static Collection<LadyLib> getAllInstances() {
        return allInstances.values();
    }

    private LadyLib() {}

    /**
     * Call this during {@link net.minecraftforge.fml.common.event.FMLPreInitializationEvent}
     */
    private void preInit(@Nonnull FMLPreInitializationEvent event) {
        // automatically gets the calling mod container
        owner = Loader.instance().activeModContainer();
        if (owner == null)
            throw new IllegalStateException("LadyLib initialization was done at the wrong time");
        ownerModId = owner.getModId();
        LOGGER = LogManager.getLogger(owner.getName() + ":lib");
        registrar = new AutoRegistrar(this, event.getAsmData());
        MinecraftForge.EVENT_BUS.register(registrar);
        MinecraftForge.EVENT_BUS.register(registrar.getItemRegistrar());
        MinecraftForge.EVENT_BUS.register(registrar.getBlockRegistrar());
        if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            clientHandler = new ClientHandler();
            clientHandler.clientInit();
        }
        registrar.autoRegisterTileEntities(this, event.getAsmData());
        configFolder = event.getModConfigurationDirectory();
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

    public File getConfigFolder() {
        return configFolder;
    }

    public void setCreativeTab(CreativeTabs tab) {
        this.creativeTab = tab;
    }

    public CreativeTabs getCreativeTab() {
        return creativeTab;
    }

    @Nonnull
    public ItemRegistrar getItemRegistrar() {
        return registrar.getItemRegistrar();
    }

    @Nonnull
    public BlockRegistrar getBlockRegistrar() {
        return registrar.getBlockRegistrar();
    }

    @Nonnull
    public String getModId() {
        return Objects.requireNonNull(ownerModId, "The enclosing mod's id was not set before calling the library");
    }

    @SideOnly(Side.CLIENT)
    public IClientHandler getClientHandler() {
        return clientHandler;
    }
}
