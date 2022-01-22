package org.parchmentmc.compass.data.sanitation.impl;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.parchmentmc.compass.data.sanitation.Sanitizer;
import org.parchmentmc.feather.mapping.MappingDataContainer;
import org.parchmentmc.feather.metadata.BouncingTargetMetadata;
import org.parchmentmc.feather.metadata.ClassMetadata;
import org.parchmentmc.feather.metadata.MethodMetadata;
import org.parchmentmc.feather.metadata.Reference;
import org.parchmentmc.feather.metadata.SourceMetadata;

import java.util.HashMap;
import java.util.Map;

import static org.parchmentmc.feather.mapping.MappingDataContainer.ClassData;
import static org.parchmentmc.feather.mapping.MappingDataContainer.MethodData;

/**
 * Moves data in bouncer methods to their targets.
 */
public class BouncerDataMover extends Sanitizer {
    // First pass is to collect bouncers and delete them, second pass is applying the data from bouncers to their targets
    private boolean finishedCollecting = false;
    private final Map<String, MethodData> data = new HashMap<>();

    public BouncerDataMover() {
        super("bouncer data mover");
    }

    @Override
    public boolean visit(MappingDataContainer container, @Nullable SourceMetadata metadata) {
        return metadata != null; // Skip if metadata is not available
    }

    @Override
    public boolean preVisit(DataType type) {
        return DataType.METHODS.test(type);
    }

    @Override
    public Action<MethodData> modifyMethod(ClassData classData, MethodData methodData,
                                       @Nullable ClassMetadata classMetadata, @Nullable MethodMetadata methodMetadata) {
        if (!finishedCollecting && methodMetadata != null && methodMetadata.getBouncingTarget().isPresent()) {
            final BouncingTargetMetadata targetData = methodMetadata.getBouncingTarget().get();
            if (targetData.getTarget().isPresent()) {
                final Reference targetRef = targetData.getTarget().get();

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
