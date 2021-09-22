package mekanism.common.recipe.impl;

import java.util.function.Consumer;
import mekanism.api.providers.IItemProvider;
import mekanism.common.Mekanism;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.block.basic.BlockFluidTank;
import mekanism.common.recipe.ISubRecipeProvider;
import mekanism.common.recipe.builder.ExtendedShapedRecipeBuilder;
import mekanism.common.recipe.builder.MekDataShapedRecipeBuilder;
import mekanism.common.recipe.pattern.Pattern;
import mekanism.common.recipe.pattern.RecipePattern;
import mekanism.common.recipe.pattern.RecipePattern.TripleLine;
import mekanism.common.registration.impl.BlockRegistryObject;
import mekanism.common.registries.MekanismBlocks;
import mekanism.common.tags.MekanismTags;
import net.minecraft.data.IFinishedRecipe;
import net.minecraft.item.Item;
import net.minecraft.tags.ITag;
import net.minecraftforge.common.Tags;

class FluidTankRecipeProvider implements ISubRecipeProvider {

    private static final RecipePattern FLUID_TANK_PATTERN = RecipePattern.createPattern(
          TripleLine.of(Pattern.ALLOY, Pattern.INGOT, Pattern.ALLOY),
          TripleLine.of(Pattern.INGOT, Pattern.PREVIOUS, Pattern.INGOT),
          TripleLine.of(Pattern.ALLOY, Pattern.INGOT, Pattern.ALLOY));

    @Override
    public void addRecipes(Consumer<IFinishedRecipe> consumer) {
        String basePath = "fluid_tank/";
        //Note: For the basic fluid tank, we have to handle the empty slot differently than batching it against our fluid tank pattern
        ExtendedShapedRecipeBuilder.shapedRecipe(MekanismBlocks.BASIC_FLUID_TANK)
              .pattern(RecipePattern.createPattern(
                    TripleLine.of(Pattern.ALLOY, Pattern.INGOT, Pattern.ALLOY),
                    TripleLine.of(Pattern.INGOT, Pattern.EMPTY, Pattern.INGOT),
                    TripleLine.of(Pattern.ALLOY, Pattern.INGOT, Pattern.ALLOY))
              ).key(Pattern.INGOT, Tags.Items.INGOTS_IRON)
              .key(Pattern.ALLOY, MekanismTags.Items.ALLOYS_BASIC)
              .build(consumer, Mekanism.rl(basePath + "basic"));
        addTieredFluidTank(consumer, basePath, MekanismBlocks.ADVANCED_FLUID_TANK, MekanismBlocks.BASIC_FLUID_TANK, MekanismTags.Items.ALLOYS_INFUSED);
        addTieredFluidTank(consumer, basePath, MekanismBlocks.ELITE_FLUID_TANK, MekanismBlocks.ADVANCED_FLUID_TANK, MekanismTags.Items.ALLOYS_REINFORCED);
        addTieredFluidTank(consumer, basePath, MekanismBlocks.ULTIMATE_FLUID_TANK, MekanismBlocks.ELITE_FLUID_TANK, MekanismTags.Items.ALLOYS_ATOMIC);
    }

    private void addTieredFluidTank(Consumer<IFinishedRecipe> consumer, String basePath, BlockRegistryObject<BlockFluidTank, ?> tank, IItemProvider previousTank,
          ITag<Item> alloyTag) {
        String tierName = Attribute.getBaseTier(tank.getBlock()).getLowerName();
        MekDataShapedRecipeBuilder.shapedRecipe(tank)
              .pattern(FLUID_TANK_PATTERN)
              .key(Pattern.PREVIOUS, previousTank)
              .key(Pattern.INGOT, Tags.Items.INGOTS_IRON)
              .key(Pattern.ALLOY, alloyTag)
              .build(consumer, Mekanism.rl(basePath + tierName));
    }
}