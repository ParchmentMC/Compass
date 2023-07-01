package org.parchmentmc.compass.util;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.parchmentmc.feather.mapping.MappingDataContainer;
import org.parchmentmc.feather.metadata.MethodMetadata;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility for calculating and caching parameter indexes from method descriptors.
 *
 * <p>This utility is not thread-safe, as parameter indexes are cached to avoid recalculation whenever possible.
 * Users should create their own instance of this class, or perform external synchronization before calling the methods
 * on this class.</p>
 */
public class DescriptorIndexer {
    private final Map<String, byte[]> cachedDescriptors = new HashMap<>();

    /**
     * Constructs a new instance of {@link DescriptorIndexer}.
     */
    public DescriptorIndexer() {
    }

    /**
     * Calculates the parameter indexes for the given method data and metadata. The returned
     * {@link BitSet} encodes what parameter indexes are valid for the given method's descriptor.
     *
     * <p><strong>This method is not thread-safe.</strong></p>
     *
     * @param methodData     the method data
     * @param methodMetadata metadata for the method, may be {@code null}
     * @return a bit set whose bits correspond to the valid parameter indexes for the given method data
     * @see #getIndexes(String, MethodMetadata)
     */
    public BitSet getIndexes(MappingDataContainer.MethodData methodData, @Nullable MethodMetadata methodMetadata) {
        return getIndexes(methodData.getDescriptor(), methodMetadata);
    }

    /**
     * Calculates the parameter indexes for the given method descriptor and method metadata. The returned
     * {@link BitSet} encodes what parameter indexes are valid for the given method descriptor.
     *
     * <p><strong>This method is not thread-safe.</strong></p>
     *
     * @param methodDescriptor the method descriptor
     * @param methodMetadata   metadata for the method, may be {@code null}
     * @return a bit set whose bits correspond to the valid parameter indexes for the given method descriptor and
     * static-ness
     * @see #getIndexes(String, Boolean)
     */
    public BitSet getIndexes(String methodDescriptor, @Nullable MethodMetadata methodMetadata) {
        return getIndexes(methodDescriptor, methodMetadata != null ? methodMetadata.isStatic() : null);
    }

    /**
     * Calculates the parameter indexes for the given method descriptor and method static-ness. The returned
     * {@link BitSet} encodes what parameter indexes are valid for the given method descriptor.
     *
     * <p>The given nullable {@link Boolean} is used to adjust the parameter indexes to account for the hidden implicit
     * parameter at index {@code 0} for instance (non-static) methods. If the Boolean parameter is {@code null}, then
     * the returned bit set encodes the parameter indexes for both static and instance methods. </p>
     *
     * <p><strong>This method is not thread-safe.</strong> The calculated parameter indexes are cached in a map, with
     * the method descriptor and the static-ness boolean as a combined key.</p>
     *
     * @param methodDescriptor the method descriptor
     * @param isMethodStatic   boolean for whether the method is {@code static}, may be {@code null}
     * @return a bit set whose bits correspond to the valid parameter indexes for the given method descriptor and
     * static-ness
     */
    public BitSet getIndexes(String methodDescriptor, @Nullable Boolean isMethodStatic) {
        String key = methodDescriptor;
        if (isMethodStatic != null && isMethodStatic) {
            key = "static:" + key;
        } else if (isMethodStatic == null) {
            key = "no_meta:" + key;
        }
        if (cachedDescriptors.containsKey(key)) {
            return BitSet.valueOf(cachedDescriptors.get(key));
        }

        final BitSet set = new BitSet();

        if (isMethodStatic != null) {
            // Since we know if the method is static (and therefore the starting index), then we can strictly
            // apply it here
            calculateIndexes(set, methodDescriptor, isMethodStatic ? 0 : 1);
        } else {
            // We don't know if the method is static, so we err on considering if the index fits both a static and
            // non-static method
            calculateIndexes(set, methodDescriptor, 0);
            calculateIndexes(set, methodDescriptor, 1);
        }

        cachedDescriptors.put(key, set.toByteArray());
        return set;
    }

    // Package-private for testing
    /* package-private */
    static void calculateIndexes(BitSet set, String descriptor, int startIndex) {
        MethodDescriptorVisitor.visit(startIndex, descriptor, (position, index, type) -> {
            if (index != -1) set.set(index);
        });
    }
}
