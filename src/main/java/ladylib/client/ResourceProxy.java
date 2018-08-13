package ladylib.client;

import com.google.common.collect.ImmutableSet;
import ladylib.LadyLib;
import net.minecraft.client.resources.AbstractResourcePack;
import net.minecraftforge.fml.common.Loader;

import javax.annotation.Nonnull;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A resource proxy, allowing a mod to programmatically override any resource <br>
 * <p>
 * This class has been adapted from Quark' source code under <a href="https://creativecommons.org/licenses/by-nc-sa/3.0/">
 * Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported License</a>
 *
 * @author Vazkii
 * @see <a href=https://github.com/Vazkii/Quark/blob/master/src/main/java/vazkii/quark/base/client/ResourceProxy.java>Quark's source</a>
 */
public class ResourceProxy extends AbstractResourcePack {

    private static final String BARE_FORMAT = "assets/%s/%s/%s";
    private static final String OVERRIDE_FORMAT = "/assets/%s/%s/overrides/%s";

    private final Map<String, String> overrides = new HashMap<>();
    /**The set of namespaces for which this proxy has overrides*/
    private final ImmutableSet<String> resourceDomains;

    public ResourceProxy(String... resourceDomains) {
        super(Objects.requireNonNull(Loader.instance().activeModContainer()).getSource());
        overrides.put("pack.mcmeta", "/proxypack.mcmeta");
        this.resourceDomains = ImmutableSet.copyOf(resourceDomains);
    }

    /**
     * Adds a resource to be loaded as an override
     * <p>
     * The path to the resource will be constructed as <code>assets/[namespace]/[dir]/overrides/[file]</code>
     *
     * @param owner     the namespace of the override's owner
     * @param namespace the namespace of the resource's original owner
     * @param dir       the path to the parent directory of the resource
     * @param files     the name of one or more files, extension included
     */
    public void addResource(String owner, String namespace, String dir, String... files) {
        for (String file : files) {
            String bare = String.format(BARE_FORMAT, namespace, dir, file);
            String override = String.format(OVERRIDE_FORMAT, owner, dir, file);
            overrides.put(bare, override);
        }
    }

    @Nonnull
    @Override
    public Set<String> getResourceDomains() {
        return resourceDomains;
    }

    @Nonnull
    @Override
    protected InputStream getInputStreamByName(@Nonnull String name) {
        // If the resource is null, it means we have fucked up big time, but it needs to be found immediately
        return Objects.requireNonNull(LadyLib.class.getResourceAsStream(overrides.get(name)));
    }

    @Override
    protected boolean hasResourceName(@Nonnull String name) {
        return overrides.containsKey(name);
    }

    @Nonnull
    @Override
    public String getPackName() {
        return "ladylib-texture-proxy";
    }

}