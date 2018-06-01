package ladylib.client;

import ladylib.client.particle.LLParticleManager;
import ladylib.client.shader.ShaderUtil;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nonnull;

public class ClientHandler implements IClientHandler {
    private LLParticleManager particleManager;

    public ClientHandler() {
        particleManager = new LLParticleManager();
    }

    /**
     * Initializes client-only helpers like {@link ShaderUtil} or {@link LLParticleManager}
     */
    public void clientInit() {
        ShaderUtil.init();
        MinecraftForge.EVENT_BUS.register(particleManager);
    }

    @Nonnull
    public LLParticleManager getParticleManager() {
        return particleManager;
    }
}
