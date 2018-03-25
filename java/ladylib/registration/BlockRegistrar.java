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
import java.util.function.Function;

public class BlockRegistrar {

    private final LadyLib ladyLib;
    private ItemRegistrar itemRegistrar;
    /**
     * A map tracking all registered blocks and their associated info
     */
    private Map<Block, BlockInfo> allBlocks = new HashMap<>();

    BlockRegistrar(LadyLib ladyLib, ItemRegistrar itemRegistrar) {
        this.ladyLib = ladyLib;
        this.itemRegistrar = itemRegistrar;
    }

    void addBlock(Block block, AutoRegistryRef ref) {
        addBlock(block, ref.isListed(), ref.isMakeItemBlock(), ref.getOreNames());
    }

    /**
     * Adds a block to be registered during {@link RegistryEvent.Register<Block>}
     *
     * @param block         the block to add
     * @param listed        whether this block's item should appear in the creative and JEI tabs
     * @param makeItemBlock whether this block should get an associated ItemBlock created and registered automatically
     * @param oreNames      ore dictionary names to add to this block
     */
    public void addBlock(Block block, boolean listed, boolean makeItemBlock, String... oreNames) {
        Function<Block, Item> itemGen;
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
     *                          If the result is Items.AIR, no item will be registered for this block
     *                          <b>DO NOT</b> return a null Item
     * @param listed            if false, this block will not appear in the creative and JEI tabs
     * @param oreNames          ore dictionary names to add to this block
     * @return the generated ItemBlock
     */
    @SuppressWarnings({"unchecked", "WeakerAccess", "UnusedReturnValue"})
    public <T extends Item> T addBlock(Block block, Function<Block, T> blockItemFunction, boolean listed, String... oreNames) {
        // adds the block to the list to be registered later
        allBlocks.put(block, new BlockInfo(oreNames));
        if (listed)
            block.setCreativeTab(ladyLib.getCreativeTab());
        // adds the corresponding item to the list of items to be registered as well
        T item = blockItemFunction.apply(block);
        if (item != Items.AIR)
            itemRegistrar.addItem(item, listed);
        // returns the obtained item in case I want to do something with it
        return item;
    }

    /**
     * Needs to be called after the main registrar has discovered all blocks
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public void registerBlocks(RegistryEvent.Register<Block> event) {
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
    public void registerSmartRender(Block block, ModelResourceLocation rl) {
        StateMapperBase ignoreState = new StateMapperBase() {
            @Nonnull
            @Override
            protected ModelResourceLocation getModelResourceLocation(@Nonnull IBlockState iBlockState) {
                return rl;
            }
        };
        ModelLoader.setCustomStateMapper(block, ignoreState);
    }

    class BlockInfo {
        String[] oreNames;

        public BlockInfo(String[] oreNames) {
            this.oreNames = oreNames;
        }
    }

}
