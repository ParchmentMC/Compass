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

            switch (c) {
                case '[': {
                    // Arrays are attached to other components, so skip to the next one (the type contained in the array)
                    break;
                }
                case 'L': {
                    if (!parsingLType) {
                        // Loop until we reach the end of the L-type
                        parsingLType = true;
                        break;
                    }
                }
                case ';': {
                    if (parsingLType) {
                        // End of an L-type
                        parsingLType = false;
                    }
                }
                default: {
                    if (parsingLType) {
                        // Still parsing an L-type, so skip visiting until we reach its end (see the case for semicolon)
                        break;
                    }
                    visitor.visit(position, index, currentParam.toString());
                    if (currentParam.length() == 1) {
                        switch (currentParam.charAt(0)) {
                            case 'D':
                            case 'J':
                                index++;
                        }
                    }
                    index++;
                    position++;
                    currentParam.setLength(0);
                }
            }
        }
        visitor.visit(position, (byte) -1, descriptor.substring(descriptor.indexOf(')') + 1));
    }
}
