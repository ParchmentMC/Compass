package org.parchmentmc.compass.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.BitSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DescriptorIndexerTest {
    @Test
    @DisplayName("Parameter types except doubles and longs take up one index")
    public void single_parameters_except_doubles_and_longs_take_up_one_index() {
        testIndexes("Z", "x"); // boolean
        testIndexes("B", "x"); // byte
        testIndexes("S", "x"); // short
        testIndexes("I", "x"); // int
        testIndexes("F", "x"); // float
        testIndexes("Ljava/lang/Object;", "x"); // reference

        testIndexes("ZBSIFLjava/lang/Object;", "xxxxxx"); // combined of the above
    }

    @Test
    @DisplayName("doubles and longs take up two indexes (with only the first being set)")
    public void doubles_and_longs_take_up_two_indexes() {
        testIndexes("DIDI", "x xx x"); // double
        testIndexes("JIJI", "x xx x"); // long
    }

    @Test
    @DisplayName("Arrays of any type take up one index")
    public void arrays_of_parameters_except_doubles_and_longs_take_up_one_index() {
        testIndexes("[Z", "x"); // boolean
        testIndexes("[B", "x"); // byte
        testIndexes("[S", "x"); // short
        testIndexes("[I", "x"); // int
        testIndexes("[F", "x"); // float
        testIndexes("[J", "x"); // long
        testIndexes("[D", "x"); // double
        testIndexes("[Ljava/lang/Object;", "x"); // reference

        // combined of all of the above
        testIndexes("[Z[B[S[I[F[J[D[Ljava/lang/Object;", "xxxxxxxx");
    }

    @Test
    @DisplayName("Capital L character in a reference type parses correctly")
    public void capital_l_in_reference_type_parses_correctly() {
        testIndexes("Lorg/example/Label;I", "xx");
    }

    @Test
    @DisplayName("Arrays of any dimension take up one index")
    public void arrays_of_any_dimension_take_up_one_index() {
        // int
        testIndexes("[I", "x");
        testIndexes("[[I", "x");
        testIndexes("[[[[[[[[I", "x");

        // reference
        testIndexes("[Ljava/lang/Object;", "x");
        testIndexes("[[Ljava/lang/Object;", "x");
        testIndexes("[[[[[[[[Ljava/lang/Object;", "x");

        // long
        testIndexes("[J", "x");
        testIndexes("[[J", "x");
        testIndexes("[[[[[[[[J", "x");
    }

    private void testIndexes(String parameters, String expectedIndexes) {
        BitSet set = new BitSet();
        // As the return type doesn't matter for index calculation, just hardcode it to void
        DescriptorIndexer.calculateIndexes(set, "(" + parameters + ")V", 0);
        assertEquals(expectedIndexes.trim(), toStr(set), "Parameter indexes for (" + parameters + ")");
    }

    private String toStr(BitSet set) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < set.length(); i++) {
            builder.append(set.get(i) ? 'x' : ' ');
        }

        return builder.toString();
    }
}
