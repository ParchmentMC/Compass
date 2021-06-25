package org.parchmentmc.compass.tasks;

import org.gradle.api.DefaultTask;
import org.gradle.api.Named;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.logging.Logger;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.VerificationTask;
import org.parchmentmc.compass.CompassPlugin;
import org.parchmentmc.compass.storage.io.MappingIOFormat;
import org.parchmentmc.compass.util.ResultContainer;
import org.parchmentmc.compass.util.download.BlackstoneDownloader;
import org.parchmentmc.compass.validation.ValidationIssue;
import org.parchmentmc.compass.validation.action.DataValidator;
import org.parchmentmc.compass.validation.impl.*;
import org.parchmentmc.feather.mapping.MappingDataContainer;
import org.parchmentmc.feather.metadata.SourceMetadata;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public abstract class ValidateMappingData extends DefaultTask implements VerificationTask {
    private static final Marker VALIDATION = MarkerFactory.getMarker("compass/task/validation");

    private boolean ignoreFailures = false;

    @InputDirectory
    public abstract DirectoryProperty getInput();

    @Input
    public abstract Property<MappingIOFormat> getInputFormat();

    @TaskAction
    public void validate() throws IOException {
        File input = getInput().get().getAsFile();

        CompassPlugin plugin = getProject().getPlugins().getPlugin(CompassPlugin.class);
        BlackstoneDownloader blackstoneDownloader = plugin.getBlackstoneDownloader();

        final SourceMetadata metadata = blackstoneDownloader.retrieveMetadata();

        MappingDataContainer data = getInputFormat().get().read(input);

        final DataValidator validator = new DataValidator();
        validator.addValidator(new BridgeValidator());
        validator.addValidator(new ClassInitValidator());
        validator.addValidator(new EnumValuesValidator());
        validator.addValidator(new LambdaValidator());
        validator.addValidator(new ParameterStandardsValidator());
        validator.addValidator(new SyntheticValidator());
        validator.addValidator(new MethodStandardsValidator());
        validator.addValidator(new ParameterConflictsValidator());

        final Logger logger = getProject().getLogger();

        logger.lifecycle("Validators in use: {}", validator.getValidators().stream().map(Named::getName).collect(Collectors.toSet()));
        if (metadata != null) {
            logger.lifecycle("Blackstone metadata is loaded");
        }
        logger.lifecycle("Validating mapping data from '{}'", input.getAbsolutePath());

        final ResultContainer<List<? extends ValidationIssue>> results = validator.validate(data, metadata);

        if (results.isEmpty()) {
            logger.lifecycle("No validation issues found.");
            return;
        }

        IssueCount count = new IssueCount();

        logger.warn(VALIDATION, "Found validation issues in {} packages and {} classes", results.getPackages().size(),
                results.getClasses().size());

        for (ResultContainer.PackageResult<List<? extends ValidationIssue>> packageResult : results.getPackages()) {
            final List<? extends ValidationIssue> issues = packageResult.getData();
            logger.warn(VALIDATION, "Package: {}", packageResult.getName());
            logIssue(logger, issues, count, "");
        }

        for (ResultContainer.ClassResult<List<? extends ValidationIssue>> classResult : results.getClasses()) {
            final List<? extends ValidationIssue> issues = classResult.getData();
            logger.warn(VALIDATION, "Class: {}", classResult.getName());
            logIssue(logger, issues, count, "");

            for (ResultContainer.FieldResult<List<? extends ValidationIssue>> fieldResult : classResult.getFields()) {
                final List<? extends ValidationIssue> fieldIssues = fieldResult.getData();
                logger.warn(VALIDATION, "    Field: {}", fieldResult.getName());
                logIssue(logger, fieldIssues, count, "    ");
            }

            for (ResultContainer.MethodResult<List<? extends ValidationIssue>> methodResult : classResult.getMethods()) {
                final List<? extends ValidationIssue> methodIssues = methodResult.getData();
                logger.warn(VALIDATION, "    Method: {}{}", methodResult.getName(),
                        methodResult.getDescriptor());
                logIssue(logger, methodIssues, count, "    ");

                for (ResultContainer.ParameterResult<List<? extends ValidationIssue>> paramResult : methodResult.getParameters()) {
                    final List<? extends ValidationIssue> paramIssues = paramResult.getData();
                    logger.warn(VALIDATION, "        Parameter at index {}", paramResult.getIndex());
                    logIssue(logger, paramIssues, count, "        ");
                }
            }
        }

        logger.warn("Found {} validation warnings and {} validation errors", count.warnings, count.errors);
        throw new ValidationFailedException("Found " + count.warnings + " validation warnings and "
                + count.errors + " validation errors");
    }

    private void logIssue(Logger logger, List<? extends ValidationIssue> issues, IssueCount count, String prefix) {
        for (ValidationIssue issue : issues) {
            if (issue instanceof ValidationIssue.ValidationWarning) {
                logger.warn(prefix + " - <!> {}: {}", issue.getValidatorName(), issue.getMessage());
                count.warnings++;
            } else if (issue instanceof ValidationIssue.ValidationError) {
                logger.error(prefix + " - (X) {}: {}", issue.getValidatorName(), issue.getMessage());
                count.errors++;
            }
        }
    }

    @Override
    public void setIgnoreFailures(boolean ignoreFailures) {
        this.ignoreFailures = ignoreFailures;
    }

    @Override
    @Input
    public boolean getIgnoreFailures() {
        return ignoreFailures;
    }

    static class IssueCount {
        int warnings = 0;
        int errors = 0;
    }

    static class ValidationFailedException extends RuntimeException {
        public ValidationFailedException(String message) {
            super(message);
        }
    }
}
