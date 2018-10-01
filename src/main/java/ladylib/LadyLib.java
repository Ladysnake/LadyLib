package ladylib;

import com.google.common.collect.SetMultimap;
import ladylib.client.ClientHandler;
import ladylib.client.LLibClientContainer;
import ladylib.client.internal.ClientHandlerImpl;
import ladylib.client.particle.LLParticleManager;
import ladylib.compat.internal.EnhancedAutomaticEventSubscriber;
import ladylib.misc.PublicApi;
import ladylib.misc.ReflectionFailedException;
import ladylib.nbt.serialization.internal.DefaultValuesSearch;
import ladylib.networking.minecraft.PacketHandler;
import ladylib.registration.BlockRegistrar;
import ladylib.registration.ItemRegistrar;
import ladylib.registration.internal.AutoRegistrar;
import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.*;
import net.minecraftforge.fml.common.discovery.ASMDataTable;
import net.minecraftforge.fml.common.event.*;
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
@Mod(modid = LadyLib.MOD_ID, version = LadyLib.VERSION, dependencies = LadyLib.DEPENDENCIES)
public class LadyLib {
    public static final String MOD_ID = "ladylib";
    public static final String MOD_NAME = "LadyLib";
    public static final String VERSION = "@VERSION@";
    /**
     * <u>Note for modders using dependency extraction:</u> mods containing LadyLib need to declare the dependency on
     * forge 14.23.3.2665+ themselves as older forge versions will not load LadyLib in the first place
     */
    public static final String DEPENDENCIES = "required-after:forge@[14.23.3.2665,);";

    public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);
    private static final PrintStream DEBUG_STREAM = new TracingPrintStream(LogManager.getLogger("DEBUG"), System.out);
    private static final Map<String, LLibContainer> allInstances = new HashMap<>();

    /**The mod instance*/
    public static final LadyLib INSTANCE = new LadyLib();
    /**@deprecated use {@link #INSTANCE} instead*/
    @Deprecated
    public static final LadyLib instance = INSTANCE;


    /**
     * Checks if the current minecraft instance is running in a development environment. <br>
     * Specifically, checks whether the environment is obfuscated or not.
     * @return true if the current environment is deobfuscated
     */
    @PublicApi
    public static boolean isDevEnv() {
        return (Boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment");
    }

    /**
     * Prints a message if and only if the game is currently running in a development environment. <br>
     * This is mostly intended as a replacement for temporary debug statements using {@link PrintStream#println(Object)  System.out.println}
     * @param message the message to print
     */
    @PublicApi
    public static void debug(Object message) {
        if (isDevEnv()) {
            // use a dedicated print stream to accurately display the call origin
            DEBUG_STREAM.println(message);
        }
    }

    /**
     * @return LadyLib's custom particle manager
     * @deprecated use directly {@link LLParticleManager#getInstance()}
     */
    @Deprecated
    @SideOnly(Side.CLIENT)
    public static LLParticleManager getParticleManager() {
        return LLParticleManager.getInstance();
    }


    private AutoRegistrar registrar;
    private ClientHandlerImpl clientHandler;

    /**
     * LadyLib construction
     * This one is only used for time-critical operations
     */
    @Mod.EventHandler
    public void construction(FMLConstructionEvent event) {
        if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            // the resource proxy needs to be registered here to exist when Minecraft looks for resource packs
            ClientHandlerImpl.hookResourceProxy();
        }
    }

    /**
     * LadyLib pre-initialization
     */
    @Mod.EventHandler
    public void preInit(@Nonnull FMLPreInitializationEvent event) {
        ASMDataTable dataTable = event.getAsmData();

        // Init automatic registration
        registrar = new AutoRegistrar(dataTable);
        MinecraftForge.EVENT_BUS.register(registrar);
        MinecraftForge.EVENT_BUS.register(registrar.getItemRegistrar());
        MinecraftForge.EVENT_BUS.register(registrar.getBlockRegistrar());
        registrar.autoRegisterTileEntities(dataTable);

        // Init @EnhancedBusSubscriber
        EnhancedAutomaticEventSubscriber.inject(dataTable);
        EnhancedAutomaticEventSubscriber.redistributeEvent(event);

        // Init shaders and particles
        if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            clientHandler = new ClientHandlerImpl();
            clientHandler.clientInit();
        }

        // Inject LLContainers
        injectContainers(dataTable);
        // Init NBT default values
        DefaultValuesSearch.searchDefaultValues(dataTable);
    }

    /**
     * LadyLib initialization
     */
    @Mod.EventHandler
    public void init(@Nonnull FMLInitializationEvent event) {
        PacketHandler.initPackets();
        EnhancedAutomaticEventSubscriber.redistributeEvent(event);
    }

    /**
     * LadyLib post-initialization
     */
    @Mod.EventHandler
    public void postInit(@Nonnull FMLPostInitializationEvent event) {
        EnhancedAutomaticEventSubscriber.redistributeEvent(event);
    }

    @Mod.EventHandler
    public void serverStarting(@Nonnull FMLServerStartingEvent event) {
        EnhancedAutomaticEventSubscriber.redistributeEvent(event);
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
            throw new ReflectionFailedException("Could not inject LadyLib container instances", e);
        }
    }

    /**
     * @return the list of every mod wrapper created by LadyLib
     */
    @PublicApi
    public Collection<LLibContainer> getAllInstances() {
        return allInstances.values();
    }

    public ClientHandler getClientHandler() {
        return clientHandler;
    }

    /**
     * Gets LadyLib's wrapper container used to provide mod-specific behaviour.
     * @param modId the mod id owning the container
     * @return the mod's container
     */
    @PublicApi
    public LLibContainer getContainer(String modId) {
        return allInstances.computeIfAbsent(modId, this::createContainer);
    }

    private LLibContainer createContainer(String modId) {
        ModContainer owner = Loader.instance().getIndexedModList().get(modId);
        if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            return new LLibClientContainer(owner);
        }
        return new LLibContainer(owner);
    }

    /**
     * Gets LadyLib's {@link ItemRegistrar item registrar}.
     * The item registrar offers various methods to make item registration easier.
     */
    @PublicApi
    public ItemRegistrar getItemRegistrar() {
        return registrar.getItemRegistrar();
    }

    /**
     * Gets LadyLib's {@link BlockRegistrar block registrar}.
     * The block registrar offers various methods to make block registration easier.
     */
    @PublicApi
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
