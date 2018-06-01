package ladylib.client;

import ladylib.client.particle.LLParticleManager;

import javax.annotation.Nonnull;

public interface IClientHandler {
    @Nonnull
    LLParticleManager getParticleManager();
}
