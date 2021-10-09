package org.parchmentmc.compass.tasks;

import org.gradle.api.DefaultTask;
import org.gradle.api.Named;
import org.gradle.api.NamedDomainObjectList;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.logging.Logger;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.VerificationTask;
import org.parchmentmc.compass.CompassPlugin;
import org.parchmentmc.compass.data.validation.ValidationIssue;
import org.parchmentmc.compass.data.validation.Validator;
import org.parchmentmc.compass.data.validation.action.DataValidator;
import org.parchmentmc.compass.data.validation.impl.*;
import org.parchmentmc.compass.storage.io.MappingIOFormat;
import org.parchmentmc.compass.util.ResultContainer;
import org.parchmentmc.compass.util.download.BlackstoneDownloader;
import org.parchmentmc.feather.mapping.MappingDataContainer;
import org.parchmentmc.feather.metadata.SourceMetadata;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public abstract class ValidateData extends DefaultTask implements VerificationTask {
    private static final Marker VALIDATION = MarkerFactory.getMarker("compass/task/validation");

    private final NamedDomainObjectList<Validator> validators;
    private boolean ignoreFailures = false;

    @InputDirectory
    public abstract DirectoryProperty getInput();

    @Input
    public abstract Property<MappingIOFormat> getInputFormat();

    @Input
    public NamedDomainObjectList<Validator> getValidators() {
        return validators;
    }

    @Inject
    public ValidateData(ObjectFactory objectFactory) {
        validators = objectFactory.namedDomainObjectList(Validator.class);

        validators.add(new BridgeValidator());
        validators.add(new ClassInitValidator());
        validators.add(new EnumValidator());
        validators.add(new LambdaValidator());
        validators.add(new ParameterStandardsValidator());
        validators.add(new SyntheticValidator());
        validators.add(new MethodStandardsValidator());
        validators.add(new ParameterConflictsValidator());
        validators.add(new ParameterIndexValidator());
    }

    @TaskAction
    public void validate() throws IOException {
        File input = getInput().get().getAsFile();

        CompassPlugin plugin = getProject().getPlugins().getPlugin(CompassPlugin.class);
        BlackstoneDownloader blackstoneDownloader = plugin.getBlackstoneDownloader();

        final SourceMetadata metadata = blackstoneDownloader.retrieveMetadata();

        MappingDataContainer data = getInputFormat().get().read(input);

        final DataValidator validator = new DataValidator();
        getValidators().forEach(validator::addValidator);

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
        logger.warn(VALIDATION, "( <!> means validation warning, (X) means validation error )");

        for (ResultContainer.PackageResult<List<? extends ValidationIssue>> packageResult : results.getPackages()) {
            logIssue(logger, count, packageResult.getData(),
                    "package \"" + packageResult.getName() + "\"");
            // error [package "{}"]: ...
        }

        for (ResultContainer.ClassResult<List<? extends ValidationIssue>> classResult : results.getClasses()) {
            logIssue(logger, count, classResult.getData(),
                    "class \"" + classResult.getName() + "\"");
            // error [class "{}"]: ...

            for (ResultContainer.FieldResult<List<? extends ValidationIssue>> fieldResult : classResult.getFields()) {
                logIssue(logger, count, fieldResult.getData(),
                        "field \"" + fieldResult.getName() + "\" of \"" + classResult.getName() + "\"");
                // error [field "{}" of "{}"]: ...
            }

            for (ResultContainer.MethodResult<List<? extends ValidationIssue>> methodResult : classResult.getMethods()) {
                logIssue(logger, count, methodResult.getData(),
                        "method \"" + methodResult.getName() + methodResult.getDescriptor() + "\" of \""
                                + classResult.getName() + "\"");
                // error [method "{}" of "{}"]: ...

                for (ResultContainer.ParameterResult<List<? extends ValidationIssue>> paramResult : methodResult.getParameters()) {
                    logIssue(logger, count, paramResult.getData(),
                            "parameter #" + paramResult.getIndex() + " of \"" + methodResult.getName()
                                    + methodResult.getDescriptor() + "\" of \"" + classResult.getName() + "\"");
                    // error [parameter #{} of "{}" of "{}"]: ...
                }
            }
        }

        logger.warn("Found {} validation warnings and {} validation errors", count.warnings, count.errors);

        if (count.errors > 0) {
            if (!ignoreFailures) {
                throw new ValidationFailedException("Found " + count.warnings + " validation warnings and "
                        + count.errors + " validation errors");
            } else {
                logger.warn("Ignoring failures.");
            }
        }
    }

    private static final String ISSUE_LOG_TEMPLATE = "{} from '{}' [{}]: {}";

    private void logIssue(Logger logger, IssueCount count, List<? extends ValidationIssue> issues, String type) {
        for (ValidationIssue issue : issues) {
            if (issue instanceof ValidationIssue.ValidationWarning) {
                logger.warn(VALIDATION, ISSUE_LOG_TEMPLATE, "warning", issue.getValidatorName(), type, issue.getMessage());
                count.warnings++;
            } else if (issue instanceof ValidationIssue.ValidationError) {
                logger.error(VALIDATION, ISSUE_LOG_TEMPLATE, "error", issue.getValidatorName(), type, issue.getMessage());
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
