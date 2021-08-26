package org.parchmentmc.compass.util;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.parchmentmc.feather.mapping.MappingDataContainer;
import org.parchmentmc.feather.metadata.MethodMetadata;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

public class DescriptorIndexer {
    private final Map<String, byte[]> cachedDescriptors = new HashMap<>();

    public DescriptorIndexer() {
    }

    public BitSet getIndexes(MappingDataContainer.MethodData methodData, @Nullable MethodMetadata methodMetadata) {
        return getIndexes(methodData.getDescriptor(), methodMetadata);
    }

    public BitSet getIndexes(String methodDescriptor, @Nullable MethodMetadata methodMetadata) {
        String key = methodDescriptor;
        if (methodMetadata != null && methodMetadata.isStatic()) {
            key = "static:" + key;
        } else if (methodMetadata == null) {
            key = "no_meta:" + key;
        }
        if (cachedDescriptors.containsKey(key)) {
            return BitSet.valueOf(cachedDescriptors.get(key));
        }

        final BitSet set = new BitSet();

        if (methodMetadata != null) {
            // Since we know if the method is static (and therefore the starting index), then we can strictly
            // apply it here
            calculateIndexes(set, methodDescriptor, methodMetadata.isStatic() ? 0 : 1);
        } else {
            // We don't know if the method is static, so we err on considering if the index fits both a static and
            // non-static method
            calculateIndexes(set, methodDescriptor, 0);
            calculateIndexes(set, methodDescriptor, 1);
        }

        cachedDescriptors.put(key, set.toByteArray());
        return set;
    }

    public static void calculateIndexes(BitSet set, String descriptor, int startIndex) {
        String parameters = descriptor.substring(1, descriptor.indexOf(')'));

        int index = startIndex;
        int cursor = -1;
        boolean isArray = false;
        while (++cursor < parameters.length()) {
            set.set(index);
            char c = parameters.charAt(cursor);
            switch (c) {
                case 'D':
                case 'J': {
                    if (!isArray)
                        index++; // longs and doubles take up two indexes
                    break;
                }
                case 'L': {
                    do {
                        cursor++;
                    } while (parameters.charAt(cursor) != ';');
                    break;
                }
            }
            if (!isArray)
                index++;
            isArray = c == '[';
        }
    }
}
