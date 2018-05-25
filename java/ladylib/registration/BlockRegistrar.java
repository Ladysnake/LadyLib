package ladylib.registration;

import ladylib.LadyLib;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMapperBase;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.oredict.OreDictionary;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * A class providing methods to ease the block registration process. <br/>
 * Also used internally by the automatic registration process.
 */
public class BlockRegistrar {

    private ItemRegistrar itemRegistrar;
    /**
     * A map tracking all registered blocks and their associated info
     */
    private Map<Block, BlockInfo> allBlocks = new HashMap<>();

    /**
     * Utility method to set at the same time an item's unlocalized and registry name. <br/>
     * Unlocalized name will be set to <tt>tile.(modid).(name).name</tt>
     * @param block the block to name
     * @param name the name that will be used as registry name resource path and unlocalized name
     * @return block
     */
    @Nonnull
    public static <T extends Block> T name(@Nonnull T block, @Nonnull String name) {
        block.setRegistryName(name).setUnlocalizedName(block.getRegistryName().getResourceDomain() + "." + name);
        return block;
    }

    public BlockRegistrar(ItemRegistrar itemRegistrar) {
        this.itemRegistrar = itemRegistrar;
    }

    /**
     * Adds a block to be registered during {@link RegistryEvent.Register<Block>}
     *
     * @param block         the block to add
     * @param listed        whether this block's item should appear in the creative and JEI tabs
     * @param makeItemBlock whether this block should get an associated ItemBlock created and registered automatically
     * @param oreNames      ore dictionary names to add to this block
     */
    public void addBlock(@Nonnull Block block, boolean listed, boolean makeItemBlock, @Nonnull String... oreNames) {
        final Function<Block, Item> itemGen;
        if (makeItemBlock)
            // a default function generating a default ItemBlock and giving it the block's own registry name
            itemGen = (b -> new ItemBlock(b).setRegistryName(Objects.requireNonNull(b.getRegistryName())));
        else
            itemGen = b -> Items.AIR;
        addBlock(block, itemGen, listed, oreNames);
    }

    /**
     * Adds a block to be registered during {@link RegistryEvent.Register<Block>}
     *
     * @param block             the block to be registered
     * @param blockItemFunction a function to create an {@link ItemBlock} from the passed block
     *                          If the result is Items.AIR, no item will be registered for this block.
     * @param listed            if false, this block will not appear in the creative and JEI tabs
     * @param oreNames          ore dictionary names to add to this block
     * @return the generated ItemBlock
     */
    @SuppressWarnings({"unchecked", "WeakerAccess"})
    public <T extends Item> T addBlock(@Nonnull Block block, @Nonnull Function<Block, T> blockItemFunction, boolean listed, @Nonnull String... oreNames) {
        // adds the block to the list to be registered later
        allBlocks.put(block, new BlockInfo(oreNames));
        if (listed) {
            block.setCreativeTab(LadyLib.instance.getContainer(block.getRegistryName().getResourceDomain()).getCreativeTab());
        }
        // adds the corresponding item to the list of items to be registered as well
        T item = blockItemFunction.apply(block);
        Objects.requireNonNull(item);
        if (item != Items.AIR)
            itemRegistrar.addItem(item, listed);
        // returns the obtained item in case I want to do something with it
        return item;
    }

    /**
     * Needs to be called after the main registrar has discovered all blocks
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    void registerBlocks(RegistryEvent.Register<Block> event) {
        allBlocks.forEach((block, info) -> {
            event.getRegistry().register(block);
            for (String oreName : info.oreNames)
                OreDictionary.registerOre(oreName, block);
        });
    }

    /**
     * Maps all states of a block to a custom {@link net.minecraft.client.renderer.block.model.IBakedModel}
     *
     * @param block the block to be mapped
     * @param rl    The model resource location for your custom baked model
     */
    @SideOnly(Side.CLIENT)
    @SuppressWarnings("unused")
    public void registerSmartRender(@Nonnull Block block, @Nonnull ModelResourceLocation rl) {
        StateMapperBase ignoreState = new StateMapperBase() {
            @Nonnull
            @Override
            protected ModelResourceLocation getModelResourceLocation(@Nonnull IBlockState iBlockState) {
                return rl;
            }
        };
        ModelLoader.setCustomStateMapper(block, ignoreState);
    }

    public Set<Block> getAllBlocks() {
        return allBlocks.keySet();
    }

    class BlockInfo {
        @Nonnull
        String[] oreNames;

        public BlockInfo(@Nonnull String[] oreNames) {
            this.oreNames = oreNames;
        }
    }

}
