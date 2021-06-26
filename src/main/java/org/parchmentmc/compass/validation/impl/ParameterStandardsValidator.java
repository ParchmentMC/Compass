package org.parchmentmc.compass.validation.impl;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.parchmentmc.compass.validation.AbstractValidator;
import org.parchmentmc.compass.validation.ValidationIssue;
import org.parchmentmc.feather.metadata.ClassMetadata;
import org.parchmentmc.feather.metadata.MethodMetadata;

import javax.lang.model.SourceVersion;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.parchmentmc.feather.mapping.MappingDataContainer.*;

/**
 * Validates that parameter names follow the current mapping standards.
 *
 * <p>There are currently two standards checked by this validator:</p>
 * <ol>
 *     <li>Parameter names must match the regex <code>{@value #STANDARDS_REGEX}</code>.</li>
 *     <li>Parameter names must neither match any reserved keyword, as defined by &sect;3.9 "Keywords" of the JLS 16,
 *     nor match the boolean literals {@code true} and {@code false}, and the {@code null} literal.</li>
 * </ol>
 *
 * @see <a href="https://docs.oracle.com/javase/specs/jls/se16/html/jls-3.html#jls-3.9">The Java&reg; Language
 * Specification, Java SE 16 Edition, &sect;3.9 "Keywords"</a>
 * @see <a href="https://docs.oracle.com/javase/specs/jls/se16/html/jls-3.html#jls-3.10.3">JLS 16,
 * &sect;3.10.3 "Boolean Literals"</a>
 * @see <a href="https://docs.oracle.com/javase/specs/jls/se16/html/jls-3.html#jls-3.10.8">JLS 16,
 * &sect;3.10.8 "The Null Literal"</a>
 */
public class ParameterStandardsValidator extends AbstractValidator {
    // TODO: move to extension
    public static final String STANDARDS_REGEX = "[a-z][A-Za-z0-9]*";
    private static final Pattern STANDARDS_REGEX_PATTERN = Pattern.compile(STANDARDS_REGEX);

    public ParameterStandardsValidator() {
        super("parameter standards");
    }

    @Override
    public List<? extends ValidationIssue> validate(ClassData classData, MethodData methodData, ParameterData paramData,
                                                    @Nullable ClassMetadata classMetadata,
                                                    @Nullable MethodMetadata methodMetadata) {
        String paramName = paramData.getName();
        if (paramName != null) {
            if (!STANDARDS_REGEX_PATTERN.matcher(paramName).matches()) {
                return singletonList(error("Parameter name '" + paramName + "' does not match regex " + STANDARDS_REGEX_PATTERN.pattern()));
            }
            if (isReserved(paramName.toLowerCase(Locale.ROOT))) {
                return singletonList(error("Parameter name (case-insensitively) matches a reserved keyword: " + paramName));
            }
        }

        return emptyList();
    }

    private static boolean isReserved(CharSequence word) {
        // Java 8 has 50 keywords with two boolean literals and the null literal, and Java 16 has the same.
        // When a new Java version release, please check if there are new keywords which are not present in Java 8;
        // if so, please add an entry here and a comment on what Java version the entry first appeared in.
        return SourceVersion.isKeyword(word);
    }
}
