package org.parchmentmc.compass.data.sanitation.impl;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.parchmentmc.compass.data.sanitation.Sanitizer;
import org.parchmentmc.feather.mapping.ImmutableMappingDataContainer.ImmutableMethodData;
import org.parchmentmc.feather.mapping.MappingDataContainer;
import org.parchmentmc.feather.metadata.BouncingTargetMetadata;
import org.parchmentmc.feather.metadata.ClassMetadata;
import org.parchmentmc.feather.metadata.MethodMetadata;
import org.parchmentmc.feather.metadata.Reference;
import org.parchmentmc.feather.metadata.SourceMetadata;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.parchmentmc.feather.mapping.MappingDataContainer.ClassData;
import static org.parchmentmc.feather.mapping.MappingDataContainer.MethodData;

/**
 * Moves data in bouncer methods to their targets.
 */
public class BouncerDataMover extends Sanitizer {
    private final Map<String, MethodData> data = new HashMap<>();
    private final Set<MethodData> bouncersToDelete = new HashSet<>();
    private Pass pass = Pass.COLLECT_BOUNCERS;

    public BouncerDataMover() {
        super("bouncer data mover");
    }

    @Override
    public boolean visit(MappingDataContainer container, @Nullable SourceMetadata metadata) {
        if (pass == Pass.END) { // Reset to beginning, if this somehow gets reused
            pass = Pass.COLLECT_BOUNCERS;
        }
        return metadata != null; // Skip if metadata is not available
    }

    @Override
    public boolean preVisit(DataType type) {
        return DataType.METHODS.test(type);
    }

    @Override
    public Action<MethodData> modifyMethod(ClassData classData, MethodData methodData,
                                           @Nullable ClassMetadata classMetadata, @Nullable MethodMetadata methodMetadata) {
        if ((pass == Pass.COLLECT_BOUNCERS || pass == Pass.REPLACE_AND_DELETE_BOUNCERS)
                && methodMetadata != null && methodMetadata.getBouncingTarget().isPresent()) {
            if (bouncersToDelete.remove(methodData)) {
                return Action.delete();
            }

            final BouncingTargetMetadata targetData = methodMetadata.getBouncingTarget().get();
            if (targetData.getTarget().isPresent()) {
                final Reference targetRef = targetData.getTarget().get();

                String targetRefString = targetRef.getOwner().getMojangName().orElse("") + "#"
                        + targetRef.getName().getMojangName().orElse("") + "#"
                        + targetRef.getDescriptor().getMojangName().orElse("");

                if (pass == Pass.COLLECT_BOUNCERS) {
                    data.put(targetRefString, methodData);
                } else if (pass == Pass.REPLACE_AND_DELETE_BOUNCERS && data.remove(targetRefString) != null) {
                    // The target method did not exist (if it did, its entry would be removed in the map)
                    final String name = targetRef.getName().getMojangName().orElse(null);
                    final String desc = targetRef.getDescriptor().getMojangName().orElse(null);

                    if (name != null && desc != null) {
                        ImmutableMethodData targetMethod =
                                new ImmutableMethodData(name, desc, methodData.getJavadoc(), methodData.getParameters());

                        return Action.replace(targetMethod);
                    }
                }
            }
        }

        if (pass == Pass.APPLY_DATA_TO_EXISTING) {
            String methodRefString = classData.getName() + "#"
                    + methodData.getName() + "#" + methodData.getDescriptor();

            final MethodData bouncerData = data.remove(methodRefString);
            // Only replace if there is no existing data
            if (bouncerData != null) {
                // Always delete the bouncer
                bouncersToDelete.add(bouncerData);
                if (methodData.getParameters().isEmpty() && !bouncerData.getParameters().isEmpty()) {
                    // Replace parameters (and javadocs if non-empty)
                    ImmutableMethodData newMethodData = new ImmutableMethodData(methodData.getName(), methodData.getDescriptor(),
                            methodData.getJavadoc().isEmpty() ? bouncerData.getJavadoc() : methodData.getJavadoc(),
                            bouncerData.getParameters());

                    return Action.replace(newMethodData);
                } else if (methodData.getJavadoc().isEmpty() && !bouncerData.getJavadoc().isEmpty()) {
                    // Modify javadocs
                    return Action.modify(bouncerData, true);
                }
                // Both javadocs and parameters in the target exist, so do nothing
            }
        }

        return Action.skip();
    }

    @Override
    public boolean revisit() {
        pass = Pass.values()[pass.ordinal() + 1];
        if (pass == Pass.END) {
            data.clear();
            bouncersToDelete.clear();
        }
        return pass != Pass.END;
    }

    enum Pass {
        COLLECT_BOUNCERS,
        APPLY_DATA_TO_EXISTING,
        REPLACE_AND_DELETE_BOUNCERS,
        END
    }
}
