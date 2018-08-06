package ladylib;

import com.google.common.collect.SetMultimap;
import ladylib.client.ClientHandler;
import ladylib.client.particle.LLParticleManager;
import ladylib.nbt.serialization.internal.DefaultValuesSearch;
import ladylib.registration.BlockRegistrar;
import ladylib.registration.ItemRegistrar;
import ladylib.registration.internal.AutoRegistrar;
import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.*;
import net.minecraftforge.fml.common.discovery.ASMDataTable;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.io.PrintStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

// note: mod information is contained in mcmod.info
@Mod(modid = LadyLib.MOD_ID)
public class LadyLib {
    public static final String MOD_ID = "ladylib";
    public static final String MOD_NAME = "LadyLib";

    public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);
    private static final PrintStream DEBUG_STREAM = new TracingPrintStream(LogManager.getLogger("DEBUG"), System.out);
    private static final Map<String, LLibContainer> allInstances = new HashMap<>();

    /**
     * The mod instance
     */
    public static final LadyLib INSTANCE = new LadyLib();

    /**
     * Checks if the current minecraft instance is running in a development environment. <br>
     * Specifically, checks whether the environment is obfuscated or not.
     * @return true if the current environment is deobfuscated
     */
    public static boolean isDevEnv() {
        return (Boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment");
    }

    /**
     * Prints a message if and only if the game is currently running in a development environment
     * @param message the message to print
     */
    public static void debug(Object message) {
        if (isDevEnv()) {
            // use a dedicated print stream to accurately display the call origin
            DEBUG_STREAM.println(message);
        }
    }

    /**
     * @return LadyLib's custom particle manager
     */
    @SideOnly(Side.CLIENT)
    public static LLParticleManager getParticleManager() {
        return INSTANCE.clientHandler.getParticleManager();
    }


    private AutoRegistrar registrar;

    @SideOnly(Side.CLIENT)
    private ClientHandler clientHandler;

    /**
     * LadyLib pre-initialization
     */
    @Mod.EventHandler
    public void preInit(@Nonnull FMLPreInitializationEvent event) {
        ASMDataTable dataTable = event.getAsmData();
        registrar = new AutoRegistrar(dataTable);
        MinecraftForge.EVENT_BUS.register(registrar);
        MinecraftForge.EVENT_BUS.register(registrar.getItemRegistrar());
        MinecraftForge.EVENT_BUS.register(registrar.getBlockRegistrar());
        if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            clientHandler = new ClientHandler();
            clientHandler.clientInit();
        }
        registrar.autoRegisterTileEntities(dataTable);
        injectContainers(dataTable);
        DefaultValuesSearch.searchDefaultValues(dataTable);
    }

    /**
     * LadyLib initialization
     */
    @Mod.EventHandler
    public void init(@Nonnull FMLInitializationEvent event) {

    }

    /**
     * Injects LLibContainer into their respective instance fields
     */
    private void injectContainers(ASMDataTable asmData) {
        try {
            // hook into forge's already existing mechanism
            Method parseSimpleFieldAnnotation = FMLModContainer.class.getDeclaredMethod("parseSimpleFieldAnnotation", SetMultimap.class, String.class, Function.class);
            parseSimpleFieldAnnotation.setAccessible(true);
            for (ModContainer container : Loader.instance().getModList()) {
                SetMultimap<String, ASMDataTable.ASMData> annotations = asmData.getAnnotationsFor(container);
                if (container instanceof FMLModContainer) {
                    parseSimpleFieldAnnotation.invoke(container, annotations, LLInstance.class.getName(), (Function<ModContainer, Object>) mc -> getContainer(mc.getModId()));
                }
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return the list of every mod wrapper created by LadyLib
     */
    public Collection<LLibContainer> getAllInstances() {
        return allInstances.values();
    }

    /**
     * Gets LadyLib's wrapper container used to provide mod-specific behaviour.
     * @param modid the mod id owning the container
     * @return the mod's container
     */
    public LLibContainer getContainer(String modid) {
        return allInstances.computeIfAbsent(modid, id -> new LLibContainer(Loader.instance().getIndexedModList().get(id)));
    }

    /**
     * Gets LadyLib's {@link ItemRegistrar item registrar}.
     * The item registrar offers various methods to make item registration easier.
     */
    public ItemRegistrar getItemRegistrar() {
        return registrar.getItemRegistrar();
    }

    /**
     * Gets LadyLib's {@link BlockRegistrar block registrar}.
     * The block registrar offers various methods to make block registration easier.
     */
    public BlockRegistrar getBlockRegistrar() {
        return registrar.getBlockRegistrar();
    }

    @Mod.InstanceFactory
    public static LadyLib getModInstance() {
        return INSTANCE;
    }

    /**
     * Populate the annotated field with the LadyLib mod wrapper instance based on the specified ModId.
     * This can be used to retrieve instances of other mods.
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface LLInstance {
        /**
         * The id of the mod which wrapper object is to be injected into this field
         */
        String value() default "";

        /**
         * Optional owner mod id, required if this annotation is on something
         * that is not inside the main class of a mod container.
         */
        String owner() default "";
    }
}
