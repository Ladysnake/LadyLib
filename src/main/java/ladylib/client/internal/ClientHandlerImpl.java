package ladylib.client.internal;

import ladylib.client.ClientHandler;
import ladylib.client.ResourceProxy;
import ladylib.client.lighting.FramebufferReplacement;
import ladylib.client.particle.LLParticleManager;
import ladylib.client.shader.ShaderUtil;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nonnull;

public class ClientHandlerImpl implements ClientHandler {
    private LLParticleManager particleManager;
    private static ResourceProxy resourceProxy;

    public static void hookResourceProxy() {
        resourceProxy = new ResourceProxy("minecraft");
        resourceProxy.hook();
    }

    public ClientHandlerImpl() {
        particleManager = new LLParticleManager();
    }

    /**
     * Initializes client-only helpers like {@link ShaderUtil} or {@link LLParticleManager}
     */
    public void clientInit() {
        ShaderUtil.init();
        FramebufferReplacement.replaceMinecraftFramebuffer();
        MinecraftForge.EVENT_BUS.register(particleManager);
    }

    @Nonnull
    @Override
    public LLParticleManager getParticleManager() {
        return particleManager;
    }

    @Override
    public void addResourceOverride(String owner, String dir, String... files) {
        resourceProxy.addResourceOverride(owner, "minecraft", dir, files);
    }
}
