package mekanism.common.item.block.machine;

import java.util.List;
import javax.annotation.Nonnull;
import mekanism.api.text.EnumColor;
import mekanism.client.render.item.ISTERProvider;
import mekanism.common.MekanismLang;
import mekanism.common.block.prefab.BlockTile;
import mekanism.common.content.blocktype.Machine;
import mekanism.common.item.interfaces.IItemSustainedInventory;
import mekanism.common.lib.security.ISecurityItem;
import mekanism.common.tile.machine.TileEntitySolarNeutronActivator;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.SecurityUtils;
import mekanism.common.util.WorldUtils;
import mekanism.common.util.text.BooleanStateDisplay.YesNo;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;

public class ItemBlockSolarNeutronActivator extends ItemBlockMachine implements IItemSustainedInventory, ISecurityItem {

    public ItemBlockSolarNeutronActivator(BlockTile<TileEntitySolarNeutronActivator, Machine<TileEntitySolarNeutronActivator>> block) {
        super(block, ISTERProvider::activator);
    }

    @Override
    public void addDetails(@Nonnull ItemStack stack, World world, @Nonnull List<ITextComponent> tooltip, boolean advanced) {
        SecurityUtils.addSecurityTooltip(stack, tooltip);
        tooltip.add(MekanismLang.HAS_INVENTORY.translateColored(EnumColor.AQUA, EnumColor.GRAY, YesNo.of(hasInventory(stack))));
        MekanismUtils.addUpgradesToTooltip(stack, tooltip);
    }

    @Override
    public boolean placeBlock(@Nonnull BlockItemUseContext context, @Nonnull BlockState state) {
        if (!WorldUtils.isValidReplaceableBlock(context.getWorld(), context.getPos().up())) {
            //If there isn't room then fail
            return false;
        }
        return super.placeBlock(context, state);
    }
}