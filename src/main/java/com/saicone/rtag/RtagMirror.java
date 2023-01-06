package com.saicone.rtag;

import com.saicone.rtag.tag.TagBase;
import com.saicone.rtag.tag.TagCompound;
import com.saicone.rtag.tag.TagList;
import com.saicone.rtag.util.EasyLookup;

import java.util.List;
import java.util.Map;

/**
 * <p>RtagMirror class to convert objects.<br>
 * By default it's only compatible with regular Java
 * objects like String, Short, Integer, Double, Float,
 * Long, Byte, Map and List.<br>
 * It also convert Byte, Integer and Long arrays as well.</p>
 *
 * @author Rubenicos
 */
public class RtagMirror {

    protected static final Class<?> TAG_BASE = EasyLookup.classById("NBTBase");
    protected static final Class<?> TAG_COMPOUND = EasyLookup.classById("NBTTagCompound");
    protected static final Class<?> TAG_LIST = EasyLookup.classById("NBTTagList");

    /**
     * RtagMirror public instance only compatible with regular Java objects.
     */
    public static final RtagMirror INSTANCE = new RtagMirror();

    /**
     * Constructs an RtagMirror only compatible with regular Java objects.
     */
    public RtagMirror() {
    }

    @Deprecated
    public RtagMirror(Rtag rtag) {
    }

    @Deprecated
    public Rtag getRtag() {
        if (this instanceof Rtag) {
            return (Rtag) this;
        } else {
            return null;
        }
    }

    @Deprecated
    public void setRtag(Rtag rtag) {
    }

    /**
     * Convert any object to NBTBase tag.
     *
     * @param object Object to convert.
     * @return       Converted NBTBase or null;
     */
    @SuppressWarnings("unchecked")
    public Object newTag(Object object) {
        if (TAG_BASE.isInstance(object)) {
            return object;
        } else if (object instanceof Map) {
            return TagCompound.newTag(this, (Map<String, Object>) object);
        } else if (object instanceof List) {
            return TagList.newTag(this, (List<Object>) object);
        } else {
            return TagBase.newTag(object);
        }
    }

    /**
     * Copy any NBTBase object into new one.
     *
     * @param tag Tag to copy.
     * @return    A NBTBase tag with the same value.
     */
    public Object clone(Object tag) {
        if (TAG_BASE.isInstance(tag)) {
            if (TAG_COMPOUND.isInstance(tag)) {
                return TagCompound.clone(tag);
            } else if (TAG_LIST.isInstance(tag)) {
                return TagList.clone(tag);
            } else {
                return TagBase.clone(tag);
            }
        } else {
            return tag == null ? null : newTag(tag);
        }
    }

    /**
     * Convert any NBTBase tag to regular Java object.
     *
     * @param tag Tag to convert.
     * @return    Converted object.
     */
    public Object getTagValue(Object tag) {
        if (TAG_COMPOUND.isInstance(tag)) {
            return TagCompound.getValue(this, tag);
        } else if (TAG_LIST.isInstance(tag)) {
            return TagList.getValue(this, tag);
        } else {
            return TagBase.getValue(tag);
        }
    }
}
