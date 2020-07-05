package com.denizenscript.denizen.utilities.nbt;

import com.denizenscript.denizen.nms.NMSHandler;
import com.denizenscript.denizen.nms.NMSVersion;
import com.denizenscript.denizen.nms.util.jnbt.*;
import com.denizenscript.denizen.objects.properties.entity.EntityDisabledSlots.Action;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class CustomNBT {

    public static final String KEY_DENIZEN = "Denizen NBT";
    public static final String KEY_ATTRIBUTES = "AttributeModifiers";
    public static final String KEY_CAN_PLACE_ON = "CanPlaceOn";
    public static final String KEY_CAN_DESTROY = "CanDestroy";
    public static final String KEY_DISABLED_SLOTS = "DisabledSlots";

    public static MapOfEnchantments getEnchantments(ItemStack item) {
        return new MapOfEnchantments(item);
    }

    private static final Map<EquipmentSlot, Integer> slotMap;

    static {
        slotMap = new HashMap<>();
        slotMap.put(EquipmentSlot.HAND, 0);
        slotMap.put(EquipmentSlot.FEET, 1);
        slotMap.put(EquipmentSlot.LEGS, 2);
        slotMap.put(EquipmentSlot.CHEST, 3);
        slotMap.put(EquipmentSlot.HEAD, 4);
        slotMap.put(EquipmentSlot.OFF_HAND, 5);
    }

    /*
     * Some static methods for dealing with Minecraft NBT data, which is used to store
     * custom NBT.
     */

    public static class AttributeReturn {
        public String attr;
        public String slot;
        public int op;
        public double amt;
        public long uuidMost;
        public long uuidLeast;
    }

    public static List<AttributeReturn> getAttributes(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return null;
        }
        CompoundTag compoundTag = NMSHandler.getItemHelper().getNbtData(itemStack);

        List<CompoundTag> attribs = new ArrayList<>();
        if (compoundTag.getValue().containsKey(KEY_ATTRIBUTES)) {
            List<Tag> temp = (List<Tag>) compoundTag.getValue().get(KEY_ATTRIBUTES).getValue();
            for (Tag tag : temp) {
                attribs.add((CompoundTag) tag);
            }
        }

        List<AttributeReturn> attrs = new ArrayList<>();

        for (int i = 0; i < attribs.size(); i++) {
            CompoundTag ct = attribs.get(i);
            AttributeReturn atr = new AttributeReturn();
            atr.attr = (String) ct.getValue().get(NMSHandler.getVersion().isAtLeast(NMSVersion.v1_16) ? "Name" : "AttributeName").getValue();
            atr.slot = ct.getValue().get("Slot") == null ? "mainhand" : (String) ct.getValue().get("Slot").getValue();
            atr.op = (Integer) ct.getValue().get("Operation").getValue();
            Tag t = ct.getValue().get("Amount");
            if (t instanceof IntTag) {
                atr.amt = (Integer) t.getValue();
            }
            else if (t instanceof LongTag) {
                atr.amt = (Long) t.getValue();
            }
            else if (t instanceof DoubleTag) {
                atr.amt = (Double) t.getValue();
            }
            else {
                /// ????
                atr.amt = 0;
            }
            if (NMSHandler.getVersion().isAtLeast(NMSVersion.v1_16) && ct.getValue().containsKey("UUID")) {
                UUID id = NMSHandler.getItemHelper().convertNbtToUuid((IntArrayTag) ct.getValue().get("UUID"));
                atr.uuidLeast = id.getLeastSignificantBits();
                atr.uuidMost = id.getMostSignificantBits();
            }
            else if (ct.getValue().containsKey("UUIDMost")) {
                t = ct.getValue().get("UUIDMost");
                if (t instanceof LongTag) {
                    atr.uuidMost = (Long) t.getValue();
                }
                else if (t instanceof IntTag) {
                    atr.uuidMost = (Integer) t.getValue();
                }
                t = ct.getValue().get("UUIDLeast");
                if (t instanceof LongTag) {
                    atr.uuidLeast = (Long) t.getValue();
                }
                else if (t instanceof IntTag) {
                    atr.uuidLeast = (Integer) t.getValue();
                }
            }
            attrs.add(atr);
        }

        return attrs;
    }

    public static long uuidChoice(ItemStack its) {
        String mat = CoreUtilities.toLowerCase(its.getType().name());
        if (mat.contains("boots")) {
            return 1000;
        }
        else if (mat.contains("legging")) {
            return 100000;
        }
        else if (mat.contains("helmet")) {
            return 10000000;
        }
        else if (mat.contains("chestp")) {
            return 1000000000;
        }
        else {
            return 1;
        }
    }

    public static ItemStack addAttribute(ItemStack itemStack, String attr, String slot, int op, double amt) {
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return null;
        }
        CompoundTag compoundTag = NMSHandler.getItemHelper().getNbtData(itemStack);

        List<CompoundTag> attribs = new ArrayList<>();
        if (compoundTag.getValue().containsKey(KEY_ATTRIBUTES)) {
            List<Tag> temp = (List<Tag>) compoundTag.getValue().get(KEY_ATTRIBUTES).getValue();
            for (Tag tag : temp) {
                attribs.add((CompoundTag) tag);
            }
        }

        HashMap<String, Tag> tmap = new HashMap<>();

        tmap.put("AttributeName", new StringTag(attr));
        tmap.put("Name", new StringTag(attr));
        tmap.put("Slot", new StringTag(slot));
        tmap.put("Operation", new IntTag(op));
        tmap.put("Amount", new DoubleTag(amt));

        long uuidhelp = uuidChoice(itemStack);
        UUID fullUuid = new UUID(uuidhelp + 88512 + attribs.size(), uuidhelp * 2 + 1250025L + attribs.size());

        if (NMSHandler.getVersion().isAtLeast(NMSVersion.v1_16)) {
            tmap.put("UUID", NMSHandler.getItemHelper().convertUuidToNbt(fullUuid));
        }
        else {
            tmap.put("UUIDMost", new LongTag(fullUuid.getMostSignificantBits()));
            tmap.put("UUIDLeast", new LongTag(fullUuid.getLeastSignificantBits()));
        }

        CompoundTag ct = NMSHandler.getInstance().createCompoundTag(tmap);
        attribs.add(ct);
        JNBTListTag lt = new JNBTListTag(CompoundTag.class, attribs);
        compoundTag = compoundTag.createBuilder().put(KEY_ATTRIBUTES, lt).build();

        return NMSHandler.getItemHelper().setNbtData(itemStack, compoundTag);
    }

    public static List<Material> getNBTMaterials(ItemStack itemStack, String key) {
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return null;
        }

        CompoundTag compoundTag = NMSHandler.getItemHelper().getNbtData(itemStack);

        List<Material> materials = new ArrayList<>();
        if (compoundTag.getValue().containsKey(key)) {
            List<StringTag> temp = (List<StringTag>) compoundTag.getValue().get(key).getValue();
            for (StringTag tag : temp) {
                materials.add(NMSHandler.getItemHelper().getMaterialFromInternalName(tag.getValue()));
            }
        }

        return materials;
    }

    public static ItemStack setNBTMaterials(ItemStack itemStack, String key, List<Material> materials) {
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return null;
        }

        CompoundTag compoundTag = NMSHandler.getItemHelper().getNbtData(itemStack);
        compoundTag = compoundTag.createBuilder().remove(key).build();

        if (materials.isEmpty()) {
            return NMSHandler.getItemHelper().setNbtData(itemStack, compoundTag);
        }

        List<StringTag> internalMaterials = new ArrayList<>();
        for (Material material : materials) {
            internalMaterials.add(new StringTag(NMSHandler.getItemHelper().getInternalNameFromMaterial(material)));
        }

        JNBTListTag lt = new JNBTListTag(StringTag.class, internalMaterials);
        compoundTag = compoundTag.createBuilder().put(key, lt).build();

        return NMSHandler.getItemHelper().setNbtData(itemStack, compoundTag);
    }

    public static ItemStack addCustomNBT(ItemStack itemStack, String key, String value, String basekey) {
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return null;
        }
        CompoundTag compoundTag = NMSHandler.getItemHelper().getNbtData(itemStack);

        CompoundTag denizenTag;
        if (compoundTag.getValue().containsKey(basekey)) {
            denizenTag = (CompoundTag) compoundTag.getValue().get(basekey);
        }
        else {
            denizenTag = NMSHandler.getInstance().createCompoundTag(new HashMap<>());
        }

        // Add custom NBT
        denizenTag = denizenTag.createBuilder().putString(CoreUtilities.toLowerCase(key), value).build();

        compoundTag = compoundTag.createBuilder().put(basekey, denizenTag).build();

        // Write tag back
        return NMSHandler.getItemHelper().setNbtData(itemStack, compoundTag);
    }

    public static ItemStack clearNBT(ItemStack itemStack, String key) {
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return null;
        }
        CompoundTag compoundTag = NMSHandler.getItemHelper().getNbtData(itemStack);

        compoundTag = compoundTag.createBuilder().remove(key).build();

        // Write tag back
        return NMSHandler.getItemHelper().setNbtData(itemStack, compoundTag);
    }

    public static ItemStack removeCustomNBT(ItemStack itemStack, String key, String basekey) {
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return null;
        }
        CompoundTag compoundTag = NMSHandler.getItemHelper().getNbtData(itemStack);

        CompoundTag denizenTag;
        if (compoundTag.getValue().containsKey(basekey)) {
            denizenTag = (CompoundTag) compoundTag.getValue().get(basekey);
        }
        else {
            return itemStack;
        }

        // Remove custom NBT
        denizenTag = denizenTag.createBuilder().remove(CoreUtilities.toLowerCase(key)).build();

        compoundTag = compoundTag.createBuilder().put(basekey, denizenTag).build();

        // Write tag back
        return NMSHandler.getItemHelper().setNbtData(itemStack, compoundTag);
    }

    public static boolean hasCustomNBT(ItemStack itemStack, String key, String basekey) {
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return false;
        }
        CompoundTag compoundTag = NMSHandler.getItemHelper().getNbtData(itemStack);

        CompoundTag denizenTag;
        if (compoundTag.getValue().containsKey(basekey)) {
            denizenTag = (CompoundTag) compoundTag.getValue().get(basekey);
        }
        else {
            return false;
        }

        return denizenTag.getValue().containsKey(CoreUtilities.toLowerCase(key));
    }

    public static String getCustomNBT(ItemStack itemStack, String key, String basekey) {
        if (itemStack == null || itemStack.getType() == Material.AIR || key == null) {
            return null;
        }
        CompoundTag compoundTag = NMSHandler.getItemHelper().getNbtData(itemStack);

        if (compoundTag.getValue().containsKey(basekey)) {
            CompoundTag denizenTag = (CompoundTag) compoundTag.getValue().get(basekey);
            String lowerKey = CoreUtilities.toLowerCase(key);
            if (denizenTag.containsKey(lowerKey)) {
                return denizenTag.getString(lowerKey);
            }
        }

        return null;
    }

    public static List<String> listNBT(ItemStack itemStack, String basekey) {
        List<String> nbt = new ArrayList<>();
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return nbt;
        }
        CompoundTag compoundTag = NMSHandler.getItemHelper().getNbtData(itemStack);

        if (compoundTag.getValue().containsKey(basekey)) {
            CompoundTag denizenTag = (CompoundTag) compoundTag.getValue().get(basekey);
            nbt.addAll(denizenTag.getValue().keySet());
        }

        return nbt;
    }

    public static void addCustomNBT(Entity entity, String key, String value) {
        if (entity == null) {
            return;
        }
        CompoundTag compoundTag = NMSHandler.getEntityHelper().getNbtData(entity);

        // Add custom NBT
        compoundTag = compoundTag.createBuilder().putString(key, value).build();

        // Write tag back
        NMSHandler.getEntityHelper().setNbtData(entity, compoundTag);
    }

    public static void addCustomNBT(Entity entity, String key, int value) {
        if (entity == null) {
            return;
        }
        CompoundTag compoundTag = NMSHandler.getEntityHelper().getNbtData(entity);

        // Add custom NBT
        compoundTag = compoundTag.createBuilder().putInt(key, value).build();

        // Write tag back
        NMSHandler.getEntityHelper().setNbtData(entity, compoundTag);
    }

    public static void removeCustomNBT(Entity entity, String key) {
        if (entity == null) {
            return;
        }
        CompoundTag compoundTag = NMSHandler.getEntityHelper().getNbtData(entity);

        // Remove custom NBT
        compoundTag = compoundTag.createBuilder().remove(key).build();

        // Write tag back
        NMSHandler.getEntityHelper().setNbtData(entity, compoundTag);
    }

    public static boolean hasCustomNBT(Entity entity, String key) {
        if (entity == null) {
            return false;
        }
        CompoundTag compoundTag = NMSHandler.getEntityHelper().getNbtData(entity);

        // Check for key
        return compoundTag.getValue().containsKey(key);
    }

    public static String getCustomNBT(Entity entity, String key) {
        if (entity == null) {
            return null;
        }
        CompoundTag compoundTag = NMSHandler.getEntityHelper().getNbtData(entity);

        // Return contents of the tag
        return compoundTag.getString(key);
    }

    public static int getCustomIntNBT(Entity entity, String key) {
        if (entity == null) {
            return 0;
        }
        CompoundTag compoundTag = NMSHandler.getEntityHelper().getNbtData(entity);

        // Return contents of the tag
        return compoundTag.getInt(key);
    }

    public static void setDisabledSlots(Entity entity, Map<EquipmentSlot, Set<Action>> map) {
        int sum = 0;
        for (Map.Entry<EquipmentSlot, Set<Action>> entry : map.entrySet()) {
            if (!slotMap.containsKey(entry.getKey())) {
                continue;
            }
            for (Action action : entry.getValue()) {
                sum += 1 << (slotMap.get(entry.getKey()) + action.getId());
            }
        }
        addCustomNBT(entity, KEY_DISABLED_SLOTS, sum);
    }

    public static Map<EquipmentSlot, Set<Action>> getDisabledSlots(Entity entity) {
        if (entity == null) {
            return null;
        }

        Map<EquipmentSlot, Set<Action>> map = new HashMap<>();
        CompoundTag compoundTag = NMSHandler.getEntityHelper().getNbtData(entity);
        int disabledSlots = compoundTag.getInt(KEY_DISABLED_SLOTS);

        if (disabledSlots == 0) {
            return map;
        }

        slotLoop:
        for (EquipmentSlot slot : slotMap.keySet()) {
            for (Action action : Action.values()) {
                int matchedSlot = disabledSlots & 1 << slotMap.get(slot) + action.getId();

                if (matchedSlot != 0) {
                    Set<Action> set = map.get(slot);
                    if (set == null) {
                        set = new HashSet<>();
                        map.put(slot, set);
                    }
                    set.add(action);

                    disabledSlots -= matchedSlot;
                    if (disabledSlots == 0) {
                        break slotLoop;
                    }
                }
            }
        }

        return map;
    }
}

