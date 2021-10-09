package org.parchmentmc.compass.data.validation.impl;

import com.google.common.base.Preconditions;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.parchmentmc.compass.data.validation.AbstractValidator;
import org.parchmentmc.compass.data.validation.ValidationIssue;
import org.parchmentmc.feather.metadata.ClassMetadata;
import org.parchmentmc.feather.metadata.MethodMetadata;

import javax.lang.model.SourceVersion;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static org.parchmentmc.feather.mapping.MappingDataContainer.*;

/**
 * Validates that parameter names follow the current mapping standards.
 *
 * <p>There are currently two standards checked by this validator:</p>
 * <ol>
 *     <li>Parameter names must match the configured regex, which defaults to <code>{@value #DEFAULT_STANDARDS_REGEX}</code>.</li>
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
    public static final String DEFAULT_STANDARDS_REGEX = "[a-z][A-Za-z0-9]*";
    private static final Pattern STANDARDS_REGEX_PATTERN = Pattern.compile(DEFAULT_STANDARDS_REGEX);
    private Pattern regexPattern = STANDARDS_REGEX_PATTERN;

    public ParameterStandardsValidator() {
        super("parameter standards");
    }

    public String getRegex() {
        return regexPattern.pattern();
    }

    public void setRegex(String regex) {
        Preconditions.checkNotNull(regex, "Regex must not be null");
        this.regexPattern = Pattern.compile(regex);
    }

    @Override
    public void validate(Consumer<? super ValidationIssue> issueHandler, ClassData classData, MethodData methodData,
                         ParameterData paramData, @Nullable ClassMetadata classMetadata,
                         @Nullable MethodMetadata methodMetadata) {
        String paramName = paramData.getName();
        if (paramName != null) {
            if (!regexPattern.matcher(paramName).matches()) {
                issueHandler.accept(error("Parameter name '" + paramName + "' does not match regex "
                        + regexPattern.pattern()));
            }
            if (isReserved(paramName.toLowerCase(Locale.ROOT))) {
                issueHandler.accept(error("Parameter name (case-insensitively) matches a reserved keyword: " + paramName));
            }
        }
    }

    private static boolean isReserved(CharSequence word) {
        // Java 8 has 50 keywords with two boolean literals and the null literal, and Java 16 has the same.
        // When a new Java version is released, please check if there are new keywords which are not present in Java 8;
        // if so, please add an case for it here and a comment on what Java version the entry first appeared in.
        return SourceVersion.isKeyword(word);
    }
}
