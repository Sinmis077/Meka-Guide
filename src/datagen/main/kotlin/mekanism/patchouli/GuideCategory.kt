package mekanism.patchouli

import mekanism.patchouli.dsl.IGuideCategory
import java.util.Locale
import mekanism.common.Mekanism
import net.minecraft.util.Util

enum class GuideCategory : IGuideCategory {
    ITEMS,
    ITEMS_METAL_AND_ORE,
    ITEMS_GEAR,
    ITEMS_UPGRADES,
    ITEMS_MODULES,
    BLOCKS,
    BLOCKS_MACHINES,
    BLOCKS_TRANSMITTERS,
    BLOCKS_STORAGE,
    MULTIBLOCKS,
    ORE_PROCESSING,
    CHEMICALS,
    ENTITIES,
    ;

    override val id: String = name.toLowerCase(Locale.ROOT)
    private val translationKey: String = Util.makeTranslationKey("guidebook", Mekanism.rl("category.$id"))
    override val translationKeyName: String = "$translationKey.name"
    override val translationKeyDescription: String = "$translationKey.description"

}
