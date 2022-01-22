package org.parchmentmc.compass.data.sanitation.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.parchmentmc.compass.data.sanitation.DataSanitizer;
import org.parchmentmc.feather.mapping.MappingDataBuilder;
import org.parchmentmc.feather.mapping.MappingDataBuilder.MutableClassData;
import org.parchmentmc.feather.mapping.MappingDataBuilder.MutableMethodData;
import org.parchmentmc.feather.mapping.MappingDataContainer;
import org.parchmentmc.feather.metadata.BouncingTargetMetadata;
import org.parchmentmc.feather.metadata.BouncingTargetMetadataBuilder;
import org.parchmentmc.feather.metadata.ClassMetadataBuilder;
import org.parchmentmc.feather.metadata.MethodMetadataBuilder;
import org.parchmentmc.feather.metadata.ReferenceBuilder;
import org.parchmentmc.feather.metadata.SourceMetadata;
import org.parchmentmc.feather.metadata.SourceMetadataBuilder;
import org.parchmentmc.feather.named.Named;
import org.parchmentmc.feather.named.NamedBuilder;
import org.parchmentmc.feather.util.Constants.Names;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class BouncerDataMoverTest {
    private static final String CLASS_NAME = "org/example/Test";
    private static final String BOUNCER_NAME = "bouncer";
    private static final String BOUNCER_DESC = "(Ljava/lang/Object;);";
    private static final String TARGET_NAME = "target";
    private static final String TARGET_DESC = "(Lorg/example/Test;)V";

    private static final Named NAMED_CLASS_NAME = NamedBuilder.create(Names.MOJANG, CLASS_NAME);
    private static final Named NAMED_BOUNCER_NAME = NamedBuilder.create(Names.MOJANG, BOUNCER_NAME);
    private static final Named NAMED_BOUNCER_DESC = NamedBuilder.create(Names.MOJANG, BOUNCER_DESC);
    private static final Named NAMED_TARGET_NAME = NamedBuilder.create(Names.MOJANG, TARGET_NAME);
    private static final Named NAMED_TARGET_DESC = NamedBuilder.create(Names.MOJANG, TARGET_DESC);

    private static final BouncingTargetMetadata BOUNCING_TARGET = BouncingTargetMetadataBuilder.create()
            .withOwner(ReferenceBuilder.create().withOwner(NAMED_CLASS_NAME).withName(NAMED_BOUNCER_NAME).withDescriptor(NAMED_BOUNCER_DESC))
            .withTarget(ReferenceBuilder.create().withOwner(NAMED_CLASS_NAME).withName(NAMED_TARGET_NAME).withDescriptor(NAMED_TARGET_DESC));

    private static final SourceMetadata METADATA = SourceMetadataBuilder.create()
            .addClass(ClassMetadataBuilder.create()
                    .withName(NAMED_CLASS_NAME)
                    .addMethod(MethodMetadataBuilder.create()
                            .withName(NAMED_BOUNCER_NAME).withDescriptor(NAMED_BOUNCER_DESC).withBouncingTarget(BOUNCING_TARGET)
                    )
                    .addMethod(MethodMetadataBuilder.create()
                            .withName(NAMED_TARGET_NAME).withDescriptor(NAMED_TARGET_DESC)
                    )
            );

    private MappingDataContainer sanitize(MappingDataBuilder builder) {
        final DataSanitizer sanitizer = new DataSanitizer();
        sanitizer.addSanitizer(new BouncerDataMover());
        return sanitizer.sanitize(builder, METADATA);
    }

    @Test
    @DisplayName("bouncer with javadoc and parameter, and missing target")
    public void test_BouncerJavadocParameters_TargetMissing() {
        final MappingDataBuilder original = new MappingDataBuilder();
        final MutableClassData originalClass = original.createClass(CLASS_NAME);

        final MutableMethodData bouncer = originalClass.createMethod(BOUNCER_NAME, BOUNCER_DESC);
        bouncer.addJavadoc("Boopity boopity boop!")
                .createParameter((byte) 0).setName("boop").setJavadoc("Boop!");

        final MappingDataContainer result = sanitize(original);

        final MappingDataContainer.ClassData classData = result.getClass(CLASS_NAME);
        assertNotNull(classData, "Class could not be found");
        assertNull(classData.getMethod(BOUNCER_NAME, BOUNCER_DESC), "Bouncer still exists");

        final MappingDataContainer.MethodData targetResult = classData.getMethod(TARGET_NAME, TARGET_DESC);
        assertNotNull(targetResult, "Target method does not exist");
        assertEquals(bouncer.getJavadoc(), targetResult.getJavadoc(), "Javadoc of bouncer and target method do not match");
        assertEquals(bouncer.getParameters(), targetResult.getParameters(), "Parameters of bouncer and target method do not match");
    }

    @Test
    @DisplayName("bouncer with javadoc, and target with parameters")
    public void testBouncerJavadoc_TargetParameters() {
        final MappingDataBuilder original = new MappingDataBuilder();
        final MutableClassData originalClass = original.createClass(CLASS_NAME);

        final MutableMethodData bouncer = originalClass.createMethod(BOUNCER_NAME, BOUNCER_DESC);
        bouncer.addJavadoc("Boopity boopity boop!");

        final MutableMethodData target = originalClass.createMethod(TARGET_NAME, TARGET_DESC);
        target.createParameter((byte) 0).setName("boop").setJavadoc("Boop!");

        final MappingDataContainer result = sanitize(original);

        final MappingDataContainer.ClassData classData = result.getClass(CLASS_NAME);
        assertNotNull(classData, "Class could not be found");
        assertNull(classData.getMethod(BOUNCER_NAME, BOUNCER_DESC), "Bouncer still exists");

        final MappingDataContainer.MethodData targetResult = classData.getMethod(TARGET_NAME, TARGET_DESC);
        assertNotNull(targetResult, "Target method does not exist");
        assertEquals(bouncer.getJavadoc(), targetResult.getJavadoc(), "Target method does not have javadoc from bouncer");
        assertEquals(targetResult.getParameters(), target.getParameters(), "Parameters of original and result target do not match");
    }

    @Test
    @DisplayName("bouncer with parameters, and target with javadoc")
    public void testBouncerParameters_TargetJavadoc() {
        final MappingDataBuilder original = new MappingDataBuilder();
        final MutableClassData originalClass = original.createClass(CLASS_NAME);

        final MutableMethodData bouncer = originalClass.createMethod(BOUNCER_NAME, BOUNCER_DESC);
        bouncer.createParameter((byte) 0).setName("boop").setJavadoc("Boop!");

        final MutableMethodData target = originalClass.createMethod(TARGET_NAME, TARGET_DESC)
                .addJavadoc("Boopity boopity boop!");

        final MappingDataContainer result = sanitize(original);

        final MappingDataContainer.ClassData classData = result.getClass(CLASS_NAME);
        assertNotNull(classData, "Class could not be found");
        assertNull(classData.getMethod(BOUNCER_NAME, BOUNCER_DESC), "Bouncer still exists");

        final MappingDataContainer.MethodData targetResult = classData.getMethod(TARGET_NAME, TARGET_DESC);
        assertNotNull(targetResult, "Target method does not exist");
        assertEquals(target.getJavadoc(), targetResult.getJavadoc(), "Javadoc of original and result target do not match");
        assertEquals(bouncer.getParameters(), targetResult.getParameters(), "Target method does not have parameters from bouncer");
    }

    @Test
    @DisplayName("bouncer with parameters and javadoc, and target with parameters and javadoc")
    public void testBouncerJavadocParameters_TargetJavadocParameters() {
        final MappingDataBuilder original = new MappingDataBuilder();
        final MutableClassData originalClass = original.createClass(CLASS_NAME);

        final MutableMethodData bouncer = originalClass.createMethod(BOUNCER_NAME, BOUNCER_DESC);
        bouncer.addJavadoc("Boopity boopity boop!")
                .createParameter((byte) 0).setName("boop").setJavadoc("Boop!");

        final MutableMethodData target = originalClass.createMethod(TARGET_NAME, TARGET_DESC);
        target.addJavadoc("Whoopsie daisy!")
                .createParameter((byte) 1).setName("toor").setJavadoc("Toor!");

        final MappingDataContainer result = sanitize(original);

        final MappingDataContainer.ClassData classData = result.getClass(CLASS_NAME);
        assertNotNull(classData, "Class could not be found");
        assertNull(classData.getMethod(BOUNCER_NAME, BOUNCER_DESC), "Bouncer still exists");

        final MappingDataContainer.MethodData targetResult = classData.getMethod(TARGET_NAME, TARGET_DESC);
        assertNotNull(targetResult, "Target method does not exist");
        assertEquals(target.getJavadoc(), targetResult.getJavadoc(), "Javadoc of original and result target do not match");
        assertEquals(target.getParameters(), targetResult.getParameters(), "Parameters of original and result target do not match");
    }
}
