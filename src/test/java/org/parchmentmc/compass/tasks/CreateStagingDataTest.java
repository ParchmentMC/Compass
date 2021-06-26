package org.parchmentmc.compass.tasks;

import org.junit.jupiter.api.Test;
import org.parchmentmc.compass.tasks.CreateStagingData.InputMode;
import org.parchmentmc.feather.mapping.MappingDataBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CreateStagingDataTest {
    public static final MappingDataBuilder base = new MappingDataBuilder();
    public static final MappingDataBuilder input = new MappingDataBuilder();

    static {
        base.createClass("com/example/TestApp")
                .createMethod("main", "([Ljava/lang/String;)V")
                .addJavadoc("Main method of the application")
                .createParameter((byte) 0).setName("args").setJavadoc("The arguments");
        input.createClass("com/example/TestApp")
                .addJavadoc("A class of the app")
                .createMethod("main", "([Ljava/lang/String;)V")
                .addJavadoc("A main method.")
                .createParameter((byte) 0).setName("args");
    }

    MappingDataBuilder applyInput(MappingDataBuilder input, InputMode mode) {
        final MappingDataBuilder baseCopy = MappingDataBuilder.copyOf(CreateStagingDataTest.base);
        CreateStagingData.apply(baseCopy, input, mode);
        return baseCopy;
    }

    @Test
    public void apply_method_does_not_copy_members_not_present_in_base() {
        MappingDataBuilder input = new MappingDataBuilder();
        input.createClass("com/example/TestApp")
                .createField("nonExistentField", "J");
        input.getOrCreateClass("com/example/TestApp")
                .createMethod("nonExistentMethod", "(I)J")
                .createParameter((byte) 0).setName("nonExistentField");
        input.createClass("com/example/NonExistent")
                .createField("aFloat", "F");
        input.getOrCreateClass("com/example/NonExistent")
                .createMethod("method", "(J)V")
                .createParameter((byte) 0).setName("arg");

        assertEquals(base, applyInput(input, InputMode.OVERWRITE));
        assertEquals(base, applyInput(input, InputMode.OVERRIDE));
        assertEquals(base, applyInput(input, InputMode.ADDITIVE));
    }

    @Test
    public void additive_input_mode() {
        MappingDataBuilder expected = new MappingDataBuilder();
        expected.createClass("com/example/TestApp")
                .addJavadoc("A class of the app") // Added by input
                .createMethod("main", "([Ljava/lang/String;)V")
                .addJavadoc("Main method of the application") // Not overriden by inputs
                .createParameter((byte) 0)
                .setName("args")
                .addJavadoc("The arguments"); // Remained from base

        assertEquals(expected, applyInput(input, InputMode.ADDITIVE));
    }

    @Test
    public void override_input_mode() {
        MappingDataBuilder expected = new MappingDataBuilder();
        expected.createClass("com/example/TestApp")
                .addJavadoc("A class of the app") // Added by input
                .createMethod("main", "([Ljava/lang/String;)V")
                .addJavadoc("A main method") // Overriden by input
                .createParameter((byte) 0)
                .setName("args")
                .addJavadoc("The arguments"); // Remained from base

        assertEquals(expected, applyInput(input, InputMode.OVERRIDE));
    }

    @Test
    public void overwrite_input_mode() {
        // Since both have the same elements (1 class -> 1 method -> 1 param), and OVERWRITE means 'the data from the
        // inputs if there's an element, even if data is empty), then we can pass in the input tree as the expected data
        assertEquals(input, applyInput(input, InputMode.OVERWRITE));
    }
}
