package mekanism.common.recipe.impl;

import java.util.function.Consumer;
import mekanism.common.Mekanism;
import mekanism.common.item.ItemTierInstaller;
import mekanism.common.recipe.ISubRecipeProvider;
import mekanism.common.recipe.builder.ExtendedShapedRecipeBuilder;
import mekanism.common.recipe.pattern.Pattern;
import mekanism.common.registration.impl.ItemRegistryObject;
import mekanism.common.registries.MekanismItems;
import mekanism.common.resource.PrimaryResource;
import mekanism.common.resource.ResourceType;
import mekanism.common.tags.MekanismTags;
import net.minecraft.data.IFinishedRecipe;
import net.minecraft.item.Item;
import net.minecraft.tags.ITag;
import net.minecraft.tags.ItemTags;
import net.minecraftforge.common.Tags;

class TierInstallerRecipeProvider implements ISubRecipeProvider {

    @Override
    public void addRecipes(Consumer<IFinishedRecipe> consumer) {
        String basePath = "tier_installer/";
        addTierInstallerRecipe(consumer, basePath, MekanismItems.BASIC_TIER_INSTALLER, Tags.Items.INGOTS_IRON, MekanismTags.Items.ALLOYS_BASIC, MekanismTags.Items.CIRCUITS_BASIC);
        addTierInstallerRecipe(consumer, basePath, MekanismItems.ADVANCED_TIER_INSTALLER, MekanismTags.Items.PROCESSED_RESOURCES.get(ResourceType.INGOT, PrimaryResource.OSMIUM), MekanismTags.Items.ALLOYS_INFUSED, MekanismTags.Items.CIRCUITS_ADVANCED);
        addTierInstallerRecipe(consumer, basePath, MekanismItems.ELITE_TIER_INSTALLER, Tags.Items.INGOTS_GOLD, MekanismTags.Items.ALLOYS_REINFORCED, MekanismTags.Items.CIRCUITS_ELITE);
        addTierInstallerRecipe(consumer, basePath, MekanismItems.ULTIMATE_TIER_INSTALLER, Tags.Items.GEMS_DIAMOND, MekanismTags.Items.ALLOYS_ATOMIC, MekanismTags.Items.CIRCUITS_ULTIMATE);
    }

    private void addTierInstallerRecipe(Consumer<IFinishedRecipe> consumer, String basePath, ItemRegistryObject<ItemTierInstaller> tierInstaller, ITag<Item> ingotTag,
          ITag<Item> alloyTag, ITag<Item> circuitTag) {
        ExtendedShapedRecipeBuilder.shapedRecipe(tierInstaller)
              .pattern(MekanismRecipeProvider.TIER_PATTERN)
              .key(Pattern.PREVIOUS, ItemTags.PLANKS)
              .key(Pattern.CIRCUIT, circuitTag)
              .key(Pattern.INGOT, ingotTag)
              .key(Pattern.ALLOY, alloyTag)
              .build(consumer, Mekanism.rl(basePath + tierInstaller.getItem().getToTier().getLowerName()));
    }
}