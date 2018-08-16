package ladysnake.milksnake;

import ladylib.client.ResourceProxy;

public class MSResourceProxy extends ResourceProxy {
    private static final String OVERRIDE_FORMAT = "/assets/%s/%s/overrides/%s/%s";

    public MSResourceProxy(String... resourceDomains) {
        super(resourceDomains);
    }

    @Override
    protected String getOverrideLocation(String owner, String namespace, String dir, String file) {
        return String.format(OVERRIDE_FORMAT, owner, dir, MilkSnakeConfig.flavour, file);
    }
}
