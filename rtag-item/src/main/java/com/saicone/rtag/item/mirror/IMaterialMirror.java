package com.saicone.rtag.item.mirror;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.saicone.rtag.Rtag;
import com.saicone.rtag.item.ItemMirror;
import com.saicone.rtag.tag.TagBase;
import com.saicone.rtag.tag.TagCompound;
import com.saicone.rtag.util.ItemMaterialTag;

import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

/**
 * IMaterialMirror to convert item material IDs
 * across version.<br>
 * This is probably the most complex mirror instance
 * in Rtag.
 *
 * @author Rubenicos
 *
 * @see ItemMaterialTag
 */
public class IMaterialMirror implements ItemMirror {

    private final Cache<String, String> cache;
    private final Object defaultMaterial;

    /**
     * Constructs an simple IMaterialMirror with cache duration of 3 hours.
     */
    public IMaterialMirror() {
        this(3, TimeUnit.HOURS, "minecraft:paper");
    }

    /**
     * Constructs an IMaterialMirror with specified paramaters.
     *
     * @param duration        Cache duration.
     * @param unit            Time unit for cache.
     * @param defaultMaterial Default material for incompatible IDs.
     */
    public IMaterialMirror(long duration, TimeUnit unit, String defaultMaterial) {
        cache = CacheBuilder.newBuilder().expireAfterAccess(duration, unit).build();
        Object tag;
        try {
            tag = TagBase.newTag(ItemMaterialTag.SERVER_VALUES.containsKey(defaultMaterial) ? defaultMaterial : "minecraft:paper");
        } catch (Throwable t) {
            tag = null;
        }
        this.defaultMaterial = tag;
    }

    @Override
    public void upgrade(Object compound, String id, int from, int to) {
        resolveMaterial(compound, id, getDamage(compound, null, from), null, from, to);
    }

    @Override
    public void upgrade(Object compound, String id, Object tag, int from, int to) {
        resolveSaved(compound, id, getDamage(compound, tag, from), tag, from, to);
    }

    @Override
    public void downgrade(Object compound, String id, int from, int to) {
        // Compatibility with IPotionMirror
        if (to <= 8 && id.equals("minecraft:potion")) {
            return;
        }
        resolveMaterial(compound, id, getDamage(compound, null, from), null, from, to);
    }

    @Override
    public void downgrade(Object compound, String id, Object tag, int from, int to) {
        // Compatibility with IPotionMirror
        if (to <= 8 && id.equals("minecraft:potion")) {
            return;
        }
        resolveSaved(compound, id, getDamage(compound, tag, from), tag, from, to);
    }

    /**
     * Resolve an ItemStack in case of contains "savedID" inside tag.
     *
     * @param compound Item NBTTagCompound.
     * @param id       ID of the item.
     * @param damage   Damage amount.
     * @param tag      Item tag.
     * @param from     Version specified in compound.
     * @param to       Version to convert.
     */
    public void resolveSaved(Object compound, String id, int damage, Object tag, int from, int to) {
        String savedID = (String) TagBase.getValue(TagCompound.get(tag, "savedID"));
        if (savedID != null) {
            String material = translate(savedID, from, to);
            if (!material.equals("null")) {
                resolveItem(compound, material, tag, from, to);
            }
        } else {
            resolveMaterial(compound, id, damage, tag, from, to);
        }
    }

    /**
     * Resolve material of the item, this method checks if the ID needs
     * to be converted.
     *
     * @param compound Item NBTTagCompound.
     * @param id       ID of the item.
     * @param damage   Damage amount.
     * @param tag      Item tag.
     * @param from     Version specified in compound.
     * @param to       Version to convert.
     */
    public void resolveMaterial(Object compound, String id, int damage, Object tag, int from, int to) {
        String material;
        boolean isEgg;
        if ((isEgg = (from <= 12 && from >= 9) && id.equalsIgnoreCase("minecraft:spawn_egg"))) {
            material = id + "=" + getEggEntity(compound, from);
        } else {
            material = id + (damage > 0 ? ":" + damage : "");
        }

        String newMaterial = translate(material, from, to);
        if (!material.equals(newMaterial)) {
            if (isEgg && (to >= 13 || to <= 8)) {
                TagCompound.remove(compound, "EntityTag");
            }
            if (newMaterial.equals("null")) {
                TagCompound.set(compound, "id", defaultMaterial);
                // Use Rtag, is more easy
                Rtag.INSTANCE.set(compound, material, "tag", "savedID");
                setDamage(compound, tag, 0, from, to);
            } else {
                resolveItem(compound, newMaterial, tag, from, to);
            }
        }
    }

    /**
     * Resolver current item compound with new material to set.
     *
     * @param compound Item NBTTagCompound.
     * @param material Material to set.
     * @param tag      Item tag.
     * @param from     Version specified in compound.
     * @param to       Version to convert.
     */
    public void resolveItem(Object compound, String material, Object tag, int from, int to) {
        String[] split;
        if (material.startsWith("spawn_egg=")) {
            split = material.split("=", 2);
            Rtag.INSTANCE.set(compound, split[1], "EntityTag", "id");
        } else {
            split = material.split(":", 2);
            setDamage(compound, tag, split.length > 1 ? Integer.parseInt(split[1]) : 0, from, to);
        }
        TagCompound.set(compound, "id", TagBase.newTag("minecraft:" + split[0]));
    }

    /**
     * Set item damage depending on item version, this
     * method removes old damage tag if the conversion
     * is across legacy-flat.
     *
     * @param compound Item NBTTagCompound.
     * @param tag      Item tag.
     * @param damage   Damage amount to set.
     * @param from     Version specified in compound
     * @param to       Version to convert.
     */
    public void setDamage(Object compound, Object tag, int damage, int from, int to) {
        if (to >= 13) {
            if (from <= 12) {
                TagCompound.remove(compound, "Damage");
            }
            Rtag.INSTANCE.set(compound, damage, "tag", "Damage");
        } else {
            if (tag != null && from <= 13) {
                TagCompound.remove(tag, "Damage");
            }
            TagCompound.set(compound, "Damage", TagBase.newTag((short) damage));
        }
    }

    /**
     * Get current item damage depending on item version.
     *
     * @param compound Item NBTTagCompound.
     * @param tag      Item tag.
     * @param version  Version of the item.
     * @return         A integer representing item damage.
     */
    public int getDamage(Object compound, Object tag, int version) {
        Object damage = null;
        // On legacy versions "Damage" is outside tag
        if (version <= 12) {
            damage = TagCompound.get(compound, "Damage");
        } else if (tag != null) {
            damage = TagCompound.get(tag, "Damage");
        }
        if ((damage = TagBase.getValue(damage)) != null) {
            // Avoid any rare error
            if (damage instanceof Short) {
                return ((Short) damage).intValue();
            } else if (damage instanceof Integer) {
                return (int) damage;
            } else {
                // WTH happens with damage tag!?
                try {
                    return Integer.parseInt(String.valueOf(damage));
                } catch (NumberFormatException ignored) { }
            }
        }
        return 0;
    }

    /**
     * Get current item entity, method for legacy SPAWN_EGG items.
     *
     * @param compound Item NBTTagCompound.
     * @param version  Item version
     * @return         A string representing entity id.
     */
    public String getEggEntity(Object compound, int version) {
        String entity = Rtag.INSTANCE.get(compound, "EntityTag", "id");
        if (entity != null) {
            return entity;
        }
        // Return another entity to avoid blank SPAWN_EGG
        switch (version) {
            case 12:
                return "pig";
            case 11:
                return "minecraft:pig";
            default:
                return "Pig";
        }
    }

    /**
     * Translate given pair of material and version into
     * current server version.
     *
     * @param material Material to translate.
     * @param from     Version specified in compound
     * @param to       Version to convert.
     * @return         A string representing current server version material.
     */
    public String translate(String material, int from, int to) {
        String mat = cache.getIfPresent(material);
        if (mat == null) {
            if (ItemMaterialTag.SERVER_VALUES.containsKey(material)) {
                cache.put(material, material);
            } else {
                compute(material, ItemMaterialTag.changeNameCase(material.replace("minecraft:", ""), true), from, to);
            }
            mat = cache.getIfPresent(material);
        }
        return mat;
    }

    private void compute(String key, String value, int from, int to) {
        for (ItemMaterialTag tag : ItemMaterialTag.SERVER_VALUES.values()) {
            TreeMap<Integer, String> names = tag.getNames();
            for (Integer tagVersion : names.descendingKeySet()) {
                if (tagVersion <= from) {
                    String tagName = names.get(tagVersion);
                    if (tagName.equals(value)) {
                        cache.put(key, ItemMaterialTag.changeNameCase(names.floorEntry(to).getValue(), false));
                        return;
                    }
                }
            }
        }
        cache.put(key, "null");
    }
}
