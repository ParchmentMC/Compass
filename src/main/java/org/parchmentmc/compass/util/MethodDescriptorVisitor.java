package org.parchmentmc.compass.util;

/**
 * Interface for visiting the components of a method descriptor.
 */
@FunctionalInterface
public interface MethodDescriptorVisitor {
    /**
     * Visits the component of a method descriptor. The components of a method descriptor includes the parameters and
     * the return type.
     *
     * <p>There is no guarantee that the parameter index passed to this method is correct. It may be offset from the
     * actual parameter index due to reasons including but not limited to method {@code static}-ness and implicit
     * hidden parameters. It is up to the caller of this visitor whether or not the index is correct.</p>
     *
     * @param position the position of the component in the descriptor
     * @param index    the index of the parameter, or {@code -1} for the return type
     * @param type     the type descriptor of the component
     */
    void visit(byte position, byte index, String type);

    /**
     * Visits the method descriptor with the given visitor.
     *
     * @param startOffset the starting offset for parameter indexes
     * @param descriptor  the method descriptor to be visited
     * @param visitor     the visitor
     * @see #visit(byte, String, MethodDescriptorVisitor)
     */
    static void visit(int startOffset, String descriptor, MethodDescriptorVisitor visitor) {
        visit((byte) startOffset, descriptor, visitor);
    }

    /**
     * Visits the method descriptor with the given visitor.
     *
     * @param startOffset the starting offset for parameter indexes
     * @param descriptor  the method descriptor to be visited
     * @param visitor     the visitor
     */
    static void visit(byte startOffset, String descriptor, MethodDescriptorVisitor visitor) {
        // Assume descriptor is well-formed
        String parameters = descriptor.substring(1, descriptor.indexOf(')'));

        byte position = 0;
        byte index = startOffset;
        int cursor = -1;
        StringBuilder currentParam = new StringBuilder();
        boolean parsingLType = false;
        while (++cursor < parameters.length()) {
            char c = parameters.charAt(cursor);
            currentParam.append(c);

            if (c == '[') {
                // Arrays are attached to other components, so skip to the next one (the type contained in the array)
                continue;
            }

            if (parsingLType) { // Currently parsing an L-type
                if (c == ';') {
                    // End of an L-type; continue to the visit section
                    parsingLType = false;
                } else {
                    // Still parsing an L-type, so continue going down the descriptor we reach its end
                    continue;
                }
            }

            if (c == 'L') {
                // Loop until we reach the end of the L-type
                parsingLType = true;
                continue;
            }

            visitor.visit(position, index, currentParam.toString());
            if (currentParam.length() == 1 && (c == 'D' || c == 'J')) {
                // If a double or long, increment the index twice
                index++;
            }
            index++;

            position++;
            currentParam.setLength(0);
        }
        visitor.visit(position, (byte) -1, descriptor.substring(descriptor.indexOf(')') + 1));
    }
}
