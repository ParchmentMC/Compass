package org.parchmentmc.compass.tasks;

import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.parchmentmc.compass.util.MappingUtil;
import org.parchmentmc.feather.mapping.MappingDataBuilder;
import org.parchmentmc.feather.mapping.MappingDataContainer;
import org.parchmentmc.feather.metadata.ClassMetadata;
import org.parchmentmc.feather.metadata.MethodMetadata;
import org.parchmentmc.feather.metadata.SourceMetadata;

import java.io.IOException;
import java.util.Map;

public abstract class GenerateSanitizedExport extends GenerateExport {
    public GenerateSanitizedExport() {
        getParameterPrefix().convention("p");
        getSkipLambdaParameters().convention(Boolean.TRUE);
        getSkipAnonymousClassParameters().convention(Boolean.TRUE);
        getIntermediate().convention("official"); // Required for us to work.
    }

    @Override
    protected MappingDataContainer modifyData(MappingDataContainer container) throws IOException {
        final String paramPrefix = getParameterPrefix().get();
        final MappingDataBuilder builder = MappingDataBuilder.copyOf(container);

        final SourceMetadata metadata = getSourceMetadata();

        final boolean skipLambdas = getSkipLambdaParameters().get();
        final boolean skipAnonClasses = getSkipAnonymousClassParameters().get();

        final Map<String, ClassMetadata> classMetadataMap = MappingUtil.buildClassMetadataMap(metadata);

        // Cascade parent methods first separately so that prefixes don't get applied multiple times
        builder.getClasses().forEach(clsData -> cascadeParentMethods(builder, classMetadataMap, clsData, classMetadataMap.get(clsData.getName())));

        builder.getClasses().forEach(clsData -> copyRecordData(clsData, classMetadataMap.get(clsData.getName())));

        builder.getClasses().forEach(clsData -> {
            final ClassMetadata clsMeta = classMetadataMap.get(clsData.getName());

            boolean anonClass = withinAnonymousClass(clsData.getName());

            clsData.getMethods().forEach(methodData -> {
                final MethodMetadata methodMeta = MappingUtil.getMethodMetadata(clsMeta, methodData.getName(), methodData.getDescriptor());

                // Simple heuristic; if it starts with `lambda$`, it's a lambda.
                boolean lambda = (methodMeta != null && methodMeta.isLambda())
                        || (methodMeta == null && methodData.getName().startsWith("lambda$"));

                methodData.getParameters().forEach(paramData -> {
                    if (paramData.getName() != null) {
                        if ((skipAnonClasses && anonClass) || (skipLambdas && lambda)) {
                            paramData.setName(null);
                        } else {
                            paramData.setName(paramPrefix + capitalize(paramData.getName()));
                        }
                    }
                });
            });
        });

        return builder;
    }

    @Input
    public abstract Property<String> getParameterPrefix();

    @Input
    public abstract Property<Boolean> getSkipLambdaParameters();

    @Input
    public abstract Property<Boolean> getSkipAnonymousClassParameters();

    private static String capitalize(String input) {
        return Character.toTitleCase(input.charAt(0)) + input.substring(1);
    }

    private static boolean withinAnonymousClass(String className) {
        for (String name : className.split("\\$")) {
            /*
             * Anonymous classes have a simple heuristic for detection
             * According to JLS, class names must be defined by `[letter][letter or digit]*` identifiers
             * So, it stands to reason that if a class name starts with a digit, then it is an anonymous (or at least
             * synthetic) class.
             */
            int firstChar = name.codePointAt(0);
            // See Character#isJavaIdentifierPart
            if (Character.isDigit(firstChar)) {
                return true;
            }
        }
        return false;
    }
}
