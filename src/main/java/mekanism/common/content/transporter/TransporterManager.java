package mekanism.common.content.transporter;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.Map;
import java.util.Set;
import mekanism.api.Coord4D;
import mekanism.common.content.transporter.TransporterStack.Path;
import mekanism.common.lib.inventory.TransitRequest;
import mekanism.common.lib.inventory.TransitRequest.ItemData;
import mekanism.common.lib.inventory.TransitRequest.TransitResponse;
import mekanism.common.util.InventoryUtils;
import mekanism.common.util.StackUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;

public class TransporterManager {

    private TransporterManager() {
    }

    private static final Map<Coord4D, Set<TransporterStack>> flowingStacks = new Object2ObjectOpenHashMap<>();

    public static void reset() {
        flowingStacks.clear();
    }

    public static void add(World world, TransporterStack stack) {
        flowingStacks.computeIfAbsent(new Coord4D(stack.getDest(), world), k -> new ObjectOpenHashSet<>()).add(stack);
    }

    public static void remove(World world, TransporterStack stack) {
        if (stack.hasPath() && stack.getPathType() != Path.NONE) {
            flowingStacks.get(new Coord4D(stack.getDest(), world)).remove(stack);
        }
    }

    public static boolean didEmit(ItemStack stack, ItemStack returned) {
        return returned.isEmpty() || returned.getCount() < stack.getCount();
    }

    public static ItemStack getToUse(ItemStack stack, ItemStack returned) {
        return returned.isEmpty() ? stack : StackUtils.size(stack, stack.getCount() - returned.getCount());
    }

    /**
     * Simulates inserting multiple items into an item handler without modifying the contents of the handler itself.
     *
     * @param handler       Item handler to insert into
     * @param inventoryInfo Contents of the destination handler including any changes we make due to simulation
     * @param stack         The itemstack to insert (not modified by the method so does not need to be copied)
     * @param count         The amount of the stack we want to insert, may be more or less than the size of the stack itself
     * @param inFlight      {@code true} for if the stack is currently in flight, {@code false} otherwise. We perform slightly different checks accuracy wise for already
     *                      in flight stacks
     *
     * @return The amount that is left over from trying to insert into the destination.
     */
    private static int simulateInsert(IItemHandler handler, InventoryInfo inventoryInfo, ItemStack stack, int count, boolean inFlight) {
        int maxStackSize = stack.getMaxStackSize();
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            if (count == 0) {
                // Nothing more to insert
                break;
            }
            int max = handler.getSlotLimit(slot);
            //If no items are allowed in the slot, pass it up before checking anything about the items
            if (max == 0) {
                continue;
            }

            // Make sure that the item is valid for the handler
            if (!handler.isItemValid(slot, stack)) {
                continue;
            }

            // Simulate the insert; note that we can't depend solely on the "normal" simulate, since it would only tell us about
            // _this_ stack, not the cumulative set of stacks. Use our best guess about stacking/maxes to figure out
            // how the inventory would look after the insertion

            // Number of items in the destination
            int destCount = inventoryInfo.stackSizes.getInt(slot);

            int mergedCount = count + destCount;
            int toAccept = count;
            boolean needsSimulation = false;
            if (destCount > 0) {
                if (!InventoryUtils.areItemsStackable(inventoryInfo.inventory.get(slot), stack) || destCount >= max) {
                    //If the destination isn't empty and not stackable or it is currently full, move along
                    continue;
                } else if (max > maxStackSize && mergedCount > maxStackSize) {
                    //If we have items in the destination, and the max amount is larger than
                    // the max size of the stack, and the total number of items will also be larger
                    // than the max stack size, we need to simulate to see how much we are actually
                    // able to insert
                    needsSimulation = true;
                    //If the stack's actual size is less than or equal to the max stack size
                    // then we need to increase the size by one for purposes of properly
                    // being able to simulate what the "limit" of the slot is rather
                    // than sending one extra item to the slot only for it to fail
                    //Note: Some things such as IInventory to IItemHandler wrappers
                    // don't actually have any restrictions on stack size, so even doing this
                    // does not help stopping the one extra item from being sent due to the fact
                    // that it allows inserting larger than the max stack size if it is already
                    // packages in an amount that is larger than a single stack
                    //Note: Because we check the size of the stack against the max stack size
                    // even if there are multiple slots that need this, we only end up copying
                    // our stack a single time to resize it. We do however make sure to update
                    // the toAccept value again if it is needed.
                    if (count <= maxStackSize) {
                        if (stack.getCount() <= maxStackSize) {
                            stack = StackUtils.size(stack, maxStackSize + 1);
                        }
                        //Update our amount that we expect to accept from simulation to represent the amount we actually
                        // are trying to insert this way if we can't accept it all then we know that the slot actually
                        // has a lower limit than it returned for getSlotLimit
                        toAccept = stack.getCount();
                    } else if (stack.getCount() <= maxStackSize) {
                        //Note: If we have more we are trying to insert than the max stack size, just take the number we are trying to insert
                        // so that we have an accurate amount for checking the real slot stack size
                        stack = StackUtils.size(stack, count);
                    }
                } else if (!inFlight) {
                    //Otherwise if we are not in flight yet, we should simulate before we actually start sending the item
                    // in case it isn't currently accepting new items even though it is not full
                    // For in flight items we follow our own logic for calculating insertions so that we are not having to
                    // query potentially expensive simulation options as often
                    needsSimulation = true;
                }
            } else {
                // If the item stack is empty, we need to do a simulated insert since we can't tell if the stack
                // in question would be allowed in this slot. Otherwise, we depend on areItemsStackable to keep us
                // out of trouble
                needsSimulation = true;
            }
            if (needsSimulation) {
                ItemStack simulatedRemainder = handler.insertItem(slot, stack, true);
                int accepted = stack.getCount() - simulatedRemainder.getCount();
                if (accepted == 0) {
                    // Insert will fail; bail
                    continue;
                } else if (accepted < toAccept) {
                    //If we accepted less than the amount we expected to, the slot actually has a lower limit
                    // so we mark the amount we accepted plus the amount already in the slot as the slot's
                    // actual limit
                    //Note: We are not able to just use inventoryInfo.inventory.get(slot) as we want to go based
                    // on the actual amount stored, and the inventory info may have gotten updated with a stack
                    // that we inserted and isn't actually the source stack. If eventually this ends up showing
                    // from profiling to be a hotspot, then we can adjust it by also keeping track in inventoryInfo
                    // of if the slot is the source or is just "type information" that we set
                    max = handler.getStackInSlot(slot).getCount() + accepted;
                }
                if (destCount == 0) {
                    //If we actually are going to insert it, because there are currently no items
                    // in the destination, we set the item to the one we are sending so that we can compare
                    // it with InventoryUtils.areItemsStackable. This makes it so that we do not send multiple
                    // items of different types to the same slot just because they are not there yet
                    inventoryInfo.inventory.set(slot, StackUtils.size(stack, 1));
                }
            }
            if (mergedCount > max) {
                // Not all the items will fit; put max in and save leftovers
                inventoryInfo.stackSizes.set(slot, max);
                count = mergedCount - max;
            } else {
                // All items will fit; set the destination count as the new combined amount
                inventoryInfo.stackSizes.set(slot, mergedCount);
                return 0;
            }
        }
        return count;
    }

    /**
     * Gets the {@link TransitResponse} of what items we expect to be able to get used/inserted into the item handler at a given position.
     *
     * @param position Position of the target
     * @param side     Side of the target we are connecting to
     * @param handler  The item handler the target has
     * @param request  Transit request
     *
     * @return {@link TransitResponse} of expected items to use
     */
    public static TransitResponse getPredictedInsert(Coord4D position, Direction side, IItemHandler handler, TransitRequest request) {
        InventoryInfo inventoryInfo = new InventoryInfo(handler);
        //Before we see if this item can fit in the destination, we must first check the stacks that are
        // en-route. Note that we also have to simulate the current inventory after each stack; we'll keep
        // track of the initial size of the inventory and then simulate each in-flight addition. If any
        // in-flight stack can't be inserted, then we can fail fast.
        //Note: that stackSizes for inventoryInfo is updated each time
        Set<TransporterStack> transporterStacks = flowingStacks.get(position);
        if (transporterStacks != null) {
            for (TransporterStack stack : transporterStacks) {
                if (stack != null && stack.getPathType() != Path.NONE) {
                    //We start by simulating inserting the stack into the handler, regardless of if we
                    // are interacting with the same side of the target as the stack's path is taking.
                    // This is so that in cases where the item handler is shared (chests) or some of
                    // the slots of the item handler may be shared (our machines with multiple sides
                    // set to the same side config are "different" because of the side proxies) then
                    // we want to make sure we try to insert the in-flight stacks anyways so that if
                    // the slot is the same we fill it.
                    int numLeftOver = simulateInsert(handler, inventoryInfo, stack.itemStack, stack.itemStack.getCount(), true);
                    if (numLeftOver > 0) {
                        if (numLeftOver == stack.itemStack.getCount()) {
                            //If none of the stack was able to be inserted, check if we are attempting to insert it
                            // into the same side as the side we are predicting that we can insert into.
                            if (side != stack.getSideOfDest()) {
                                //If we are not, then assume that the destination does not contain the slot that the in-flight
                                // stack is en-route to at all, so don't exit early just because of failing to insert it into
                                // the destination, and instead continue checking and then simulate/check our TransitRequest
                                continue;
                            }
                        }
                        // Failed to successfully insert this in-flight item; there's no room for anyone else
                        return request.getEmptyResponse();
                    }
                }
            }
        }

        //Now for each of the items in the request, simulate the insert, using the state from all the in-flight
        // items to ensure we have an accurate model of what will happen in future. We try each stack in the
        // request; it might be possible to not send the first item, but the second could work, etc.
        for (ItemData data : request.getItemData()) {
            //Create a sending ItemStack with the hashed item type and total item count within the request
            ItemStack stack = data.getStack();
            int numToSend = data.getTotalCount();
            //Directly pass the stack AND the actual amount we want, so that it does not need to copy the stack if there is no room
            int numLeftOver = simulateInsert(handler, inventoryInfo, stack, numToSend, false);
            //If leftovers is unchanged from the simulation, there's no room at all; move on to the next stack
            if (numLeftOver == numToSend) {
                continue;
            }
            //Otherwise, construct the appropriately size stack to send and return that
            return request.createResponse(StackUtils.size(stack, numToSend - numLeftOver), data);
        }
        return request.getEmptyResponse();
    }

    /**
     * Information about the inventory, keeps track of the size of a stack a slot will have, and a cache of what {@link IItemHandler#getStackInSlot(int)} returns (as it
     * has to call it anyways to get the stack size). This cache allows potentially expensive {@link IItemHandler#getStackInSlot(int)} implementations to only have to be
     * called once instead of potentially many times.
     */
    private static class InventoryInfo {

        private final NonNullList<ItemStack> inventory;
        private final IntList stackSizes = new IntArrayList();

        public InventoryInfo(IItemHandler handler) {
            int slots = handler.getSlots();
            inventory = NonNullList.withSize(slots, ItemStack.EMPTY);
            for (int i = 0; i < slots; i++) {
                ItemStack stack = handler.getStackInSlot(i);
                inventory.set(i, stack);
                stackSizes.add(stack.getCount());
            }
        }
    }
}