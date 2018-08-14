package ladylib.client.shader;

import net.minecraft.util.ResourceLocation;

public class ShaderPair {
    private final ResourceLocation fragment;
    private final ResourceLocation vertex;

    ShaderPair(ResourceLocation fragment, ResourceLocation vertex) {
        this.fragment = fragment;
        this.vertex = vertex;
    }

    public ResourceLocation getFragment() {
        return fragment;
    }

    public ResourceLocation getVertex() {
        return vertex;
    }
}
