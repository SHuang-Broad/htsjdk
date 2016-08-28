/*
* Copyright (c) 2012 The Broad Institute
* 
* Permission is hereby granted, free of charge, to any person
* obtaining a copy of this software and associated documentation
* files (the "Software"), to deal in the Software without
* restriction, including without limitation the rights to use,
* copy, modify, merge, publish, distribute, sublicense, and/or sell
* copies of the Software, and to permit persons to whom the
* Software is furnished to do so, subject to the following
* conditions:
* 
* The above copyright notice and this permission notice shall be
* included in all copies or substantial portions of the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
* EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
* OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
* NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
* HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
* WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
* FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
* THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

package htsjdk.variant.variantcontext;


import htsjdk.variant.vcf.VCFConstants;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Common utility routines for VariantContext and Genotype
 *
 * @author depristo
 */
// TODO: should this class be package access?
public final class CommonInfo implements Serializable {
    public static final long serialVersionUID = 1L;

    public static final double NO_LOG10_PERROR = 1.0;

    private static final Set<String> NO_FILTERS = Collections.emptySet();
    private static final Map<String, Object> NO_ATTRIBUTES = Collections.unmodifiableMap(new HashMap<String, Object>());

    private double log10PError = NO_LOG10_PERROR;
    private String name = null;
    private Set<String> filters = null;
    private Map<String, Object> attributes = NO_ATTRIBUTES;

    // TODO: why the asymmetric argument checks between filters and attributes? shouldn't an exception be thrown with null attributes?
    /**
     * @throws IllegalArgumentException if  1) {@code log10PError} is positive and at the same time not {@link #NO_LOG10_PERROR}
     *                                      2) the log10PError contained in this instance is either NaN or {@link Double#isInfinite()}
     */
    public CommonInfo(final String name, final double log10PError, final Set<String> filters, final Map<String, Object> attributes) {
        this.name = name;
        setLog10PError(log10PError);
        this.filters = filters;
        if ( attributes != null && ! attributes.isEmpty() ) {
            this.attributes = attributes;
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if ( name == null ) throw new IllegalArgumentException("Name cannot be null " + this);
        this.name = name;
    }

    // ---------------------------------------------------------------------------------------------------------
    //
    // Filter
    //
    // ---------------------------------------------------------------------------------------------------------

    // TODO: should this be deprecated and be replaced with getFilters() which never returns null?
    //       this may change how related functions and structs/fields are initialized
    public Set<String> getFiltersMaybeNull() {
        return filters;
    }

    /**
     * @return  Un-modifiable view of filters associated with this information.
     *          Never {@code null}, but may be {@link #NO_FILTERS}.
     */
    public Set<String> getFilters() {
        return filters == null ? NO_FILTERS : Collections.unmodifiableSet(filters);
    }

    public boolean filtersWereApplied() {
        return filters != null;
    }

    public boolean isFiltered() {
        return filters != null && !filters.isEmpty();
    }

    public boolean isNotFiltered() {
        return ! isFiltered();
    }

    /**
     * @throws IllegalArgumentException when {@code filter} is {@code null}
     */
    public void addFilter(final String filter) {
        if ( filter == null ) throw new IllegalArgumentException("BUG: Attempting to add null filter " + this);
        // TODO: should this throw or simply warn?
        if ( getFilters().contains(filter) ) throw new IllegalArgumentException("BUG: Attempting to add duplicate filter " + filter + " at " + this);
        if ( filters == null ) // TODO: is this comment a warning sign?: immutable -> mutable
            filters = new HashSet<>();

        filters.add(filter);
    }

    /**
     * @throws IllegalArgumentException when {@code filters} is {@code null} or when filters to be added are already put in
     */
    public void addFilters(final Collection<String> filters) {
        if ( filters == null ) throw new IllegalArgumentException("BUG: Attempting to add null filters at" + this);
        for ( final String f : filters )
            addFilter(f);
    }

    // ---------------------------------------------------------------------------------------------------------
    //
    // Working with log error rates
    //
    // ---------------------------------------------------------------------------------------------------------

    public boolean hasLog10PError() {
        return getLog10PError() != NO_LOG10_PERROR;
    }

    /**
     * @return the -1 * log10-based error estimate
     */
    public double getLog10PError() { return log10PError; }

    /**
     * Floating-point arithmetic allows signed zeros such as +0.0 and -0.0.
     * Adding the constant 0.0 to the result ensures that the returned value is never -0.0
     * since (-0.0) + 0.0 = 0.0.
     *
     * When this is set to '0.0', the resulting VCF would be 0 instead of -0.
     *
     * @return double - Phred scaled quality score
     */
    public double getPhredScaledQual() { return (getLog10PError() * -10) + 0.0; }

    /**
     * TODO: why checks this instance's log10PError while assigning? And why IllegalArgumentException?
     * @param log10PError
     * @throws IllegalArgumentException if  1) {@code log10PError} is positive and at the same time not {@link #NO_LOG10_PERROR}
     *                                      2) the log10PError contained in this instance is either NaN or {@link Double#isInfinite()}
     */
    public void setLog10PError(final double log10PError) {
        if ( log10PError > 0 && log10PError != NO_LOG10_PERROR)
            throw new IllegalArgumentException("BUG: log10PError cannot be > 0 : " + this.log10PError);
        if ( Double.isInfinite(this.log10PError) )
            throw new IllegalArgumentException("BUG: log10PError should not be Infinity");
        if ( Double.isNaN(this.log10PError) )
            throw new IllegalArgumentException("BUG: log10PError should not be NaN");
        this.log10PError = log10PError;
    }

    // ---------------------------------------------------------------------------------------------------------
    //
    // Working with attributes
    //
    // ---------------------------------------------------------------------------------------------------------

    // TODO: should the following be
    // if(attributes!=NO_ATTRIBUTES) attributes = new HashMap<>();
    public void clearAttributes() {
        attributes = new HashMap<>();
    }

    // TODO for a todo: is the following todo still worth it? should some enums in Genotype and VariantContext be moved here?
    // todo -- define common attributes as enum

    /**
     *
     * @param map
     */
    public void setAttributes(final Map<String, ?> map) {
        clearAttributes();
        putAttributes(map);
    }

    public void putAttribute(final String key, final Object value) {
        putAttribute(key, value, false);
    }

    /**
     * @throws IllegalArgumentException if {@code allowOverwrites} is {@code false} and the {@code key} is already associated with a value
     */
    public void putAttribute(final String key, final Object value, final boolean allowOverwrites) {
        if ( ! allowOverwrites && hasAttribute(key) )
            throw new IllegalStateException("Attempting to overwrite key->value binding: key = " + key + " this = " + this);

        if ( attributes == NO_ATTRIBUTES ) // immutable -> mutable
            attributes = new HashMap<>();

        attributes.put(key, value);
    }

    // TODO: edge case "bug": if attributes was no_attributes before, calling this method would change that, though nothing is removed.
    public void removeAttribute(final String key) {
        if ( attributes == NO_ATTRIBUTES ) // immutable -> mutable
            attributes = new HashMap<>();
        attributes.remove(key);
    }

    // TODO: shouldn't exception be thrown here with null input?
    /**
     * @throws IllegalArgumentException if the input map has any non-empty intersection with the attribute map currently hold by this instance
     */
    public void putAttributes(final Map<String, ?> map) {
        if ( map != null ) {
            // for efficiency, we can skip the validation if the map is empty
            if (attributes.isEmpty()) {
                if ( attributes == NO_ATTRIBUTES ) // immutable -> mutable
                    attributes = new HashMap<>();
                attributes.putAll(map);
            } else {
                for ( Map.Entry<String, ?> elt : map.entrySet() ) {
                    putAttribute(elt.getKey(), elt.getValue(), false);
                }
            }
        }
    }

    public boolean hasAttribute(final String key) {
        return attributes.containsKey(key);
    }

    public int getNumAttributes() {
        return attributes.size();
    }

    /**
     * @return an un-modifiable view of attribute map
     */
    public Map<String, Object> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    /**
     * @param key    the attribute key
     *
     * @return the attribute value for the given key (or null if not set)
     */
    public Object getAttribute(final String key) {
        return attributes.get(key);
    }

    public Object getAttribute(final String key, final Object defaultValue) {
        if ( hasAttribute(key) )
            return attributes.get(key);
        else
            return defaultValue;
    }

    /**
     * @return  an empty list if key was not found
     *          a singleton list if there's only one value
     *          a list if the value is a list of array
     */
    @SuppressWarnings("unchecked")
    public List<Object> getAttributeAsList(final String key) {
        final Object o = getAttribute(key);
        if ( o == null ) return Collections.emptyList();
        if ( o instanceof List ) return (List<Object>)o;
        if ( o.getClass().isArray() ) return Arrays.asList((Object[])o);
        return Collections.singletonList(o);
    }

    /**
     * @return {@code defaultValue} if {@code key} map to {@code null},
     *          otherwise the normal behavior as defined by the associated value object's toString() method
     */
    public String getAttributeAsString(final String key, final String defaultValue) {
        Object x = getAttribute(key);
        if ( x == null ) return defaultValue;
        if ( x instanceof String ) return (String)x; // TODO: String.valueOf() throws?
        return String.valueOf(x); // throws an exception if this isn't a string
    }

    /**
     * @return {@code defaultValue} if {@code key} map to {@code null}, otherwise the normal behavior.
     * @throws ClassCastException if the associated value cannot be casted to a {@link String}.
     * @throws NumberFormatException if the associated value cannot be parsed by {@link Integer#valueOf(String)}.
     */
    public int getAttributeAsInt(final String key, final int defaultValue) {
        Object x = getAttribute(key);
        if ( x == null || x == VCFConstants.MISSING_VALUE_v4 ) return defaultValue;
        if ( x instanceof Integer ) return (Integer)x;
        return Integer.valueOf((String)x); // throws an exception if this isn't a string
    }

    /**
     * @return {@code defaultValue} if {@code key} map to {@code null}, otherwise the normal behavior.
     * @throws ClassCastException if the associated value cannot be casted to a {@link String}.
     * @throws NumberFormatException if the associated value cannot be parsed by {@link Double#valueOf(String)}.
     */
    public double getAttributeAsDouble(final String key, final double defaultValue) {
        Object x = getAttribute(key);
        if ( x == null ) return defaultValue;
        if ( x instanceof Double ) return (Double)x;
        if ( x instanceof Integer ) return (Integer)x;
        return Double.valueOf((String)x); // throws an exception if this isn't a string
    }

    /**
     * @return {@code defaultValue} if {@code key} map to {@code null}, otherwise the normal behavior.
     * @throws ClassCastException if the associated value cannot be casted to a {@link String}.
     */
    public boolean getAttributeAsBoolean(final String key, final boolean defaultValue) {
        Object x = getAttribute(key);
        if ( x == null ) return defaultValue;
        if ( x instanceof Boolean ) return (Boolean)x;
        return Boolean.valueOf((String)x); // throws an exception if this isn't a string
    }

//    public String getAttributeAsString(String key)      { return (String.valueOf(getExtendedAttribute(key))); } // **NOTE**: will turn a null Object into the String "null"
//    public int getAttributeAsInt(String key)            { Object x = getExtendedAttribute(key); return x instanceof Integer ? (Integer)x : Integer.valueOf((String)x); }
//    public double getAttributeAsDouble(String key)      { Object x = getExtendedAttribute(key); return x instanceof Double ? (Double)x : Double.valueOf((String)x); }
//    public boolean getAttributeAsBoolean(String key)      { Object x = getExtendedAttribute(key); return x instanceof Boolean ? (Boolean)x : Boolean.valueOf((String)x); }
//    public Integer getAttributeAsIntegerNoException(String key)  { try {return getAttributeAsInt(key);} catch (Exception e) {return null;} }
//    public Double getAttributeAsDoubleNoException(String key)    { try {return getAttributeAsDouble(key);} catch (Exception e) {return null;} }
//    public String getAttributeAsStringNoException(String key)    { if (getExtendedAttribute(key) == null) return null; return getAttributeAsString(key); }
//    public Boolean getAttributeAsBooleanNoException(String key)  { try {return getAttributeAsBoolean(key);} catch (Exception e) {return null;} }
}
