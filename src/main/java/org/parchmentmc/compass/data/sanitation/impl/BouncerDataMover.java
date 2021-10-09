package org.parchmentmc.compass.data.sanitation.impl;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.parchmentmc.compass.data.sanitation.AbstractSanitizer;
import org.parchmentmc.feather.metadata.BouncingTargetMetadata;
import org.parchmentmc.feather.metadata.ClassMetadata;
import org.parchmentmc.feather.metadata.MethodMetadata;
import org.parchmentmc.feather.metadata.MethodReference;

import java.util.HashMap;
import java.util.Map;

import static org.parchmentmc.feather.mapping.MappingDataContainer.*;

/**
 * Moves data in bouncer methods to their targets.
 */
public class BouncerDataMover extends AbstractSanitizer {
    // First pass is to collect bouncers and delete them, second pass is applying the data from bouncers to their targets
    private boolean finishedCollecting = false;
    private final Map<String, MethodData> data = new HashMap<>();

    public BouncerDataMover() {
        super("bouncer data mover");
    }

    @Override
    public boolean start(boolean isMetadataAvailable) {
        return isMetadataAvailable; // Skip if metadata is not available
    }

    @Override
    public Action<MethodData> sanitize(ClassData classData, MethodData methodData,
                                       @Nullable ClassMetadata classMetadata, @Nullable MethodMetadata methodMetadata) {
        if (!finishedCollecting && methodMetadata != null && methodMetadata.getBouncingTarget().isPresent()) {
            final BouncingTargetMetadata targetData = methodMetadata.getBouncingTarget().get();
            if (targetData.getTarget().isPresent()) {
                final MethodReference targetRef = targetData.getTarget().get();

                String targetRefString = targetRef.getOwner().getMojangName("") + "#"
                        + targetRef.getName().getMojangName("") + "#"
                        + targetRef.getDescriptor().getMojangName("");

                data.put(targetRefString, methodData);
                return Action.delete();
            }
        }

        if (finishedCollecting) {
            String methodRefString = classData.getName() + "#"
                    + methodData.getName() + "#" + methodData.getDescriptor();

            final MethodData bouncerData = data.remove(methodRefString);
            if (bouncerData != null) {
                return Action.modify(bouncerData, true);
            }
        }

        return Action.skip();
    }

    @Override
    public boolean revisit() {
        finishedCollecting = !finishedCollecting;
        if (!finishedCollecting) {
            data.clear();
        }
        return finishedCollecting;
    }
}
