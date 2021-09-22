package mekanism.generators.common.content.turbine;

import java.util.List;
import mekanism.api.NBTConstants;
import mekanism.common.lib.multiblock.MultiblockCache;
import mekanism.common.tile.TileEntityChemicalTank.GasMode;
import mekanism.common.util.NBTUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;

public class TurbineCache extends MultiblockCache<TurbineMultiblockData> {

    public GasMode dumpMode = GasMode.IDLE;

    @Override
    public void merge(MultiblockCache<TurbineMultiblockData> mergeCache, List<ItemStack> rejectedItems) {
        super.merge(mergeCache, rejectedItems);
        dumpMode = ((TurbineCache) mergeCache).dumpMode;
    }

    @Override
    public void apply(TurbineMultiblockData data) {
        super.apply(data);
        data.dumpMode = dumpMode;
    }

    @Override
    public void sync(TurbineMultiblockData data) {
        super.sync(data);
        dumpMode = data.dumpMode;
    }

    @Override
    public void load(CompoundNBT nbtTags) {
        super.load(nbtTags);
        NBTUtils.setEnumIfPresent(nbtTags, NBTConstants.DUMP_MODE, GasMode::byIndexStatic, mode -> dumpMode = mode);
    }

    @Override
    public void save(CompoundNBT nbtTags) {
        super.save(nbtTags);
        nbtTags.putInt(NBTConstants.DUMP_MODE, dumpMode.ordinal());
    }
}