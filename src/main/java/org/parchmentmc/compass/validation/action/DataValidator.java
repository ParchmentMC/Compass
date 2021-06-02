package org.parchmentmc.compass.validation.action;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.parchmentmc.compass.util.ResultContainer;
import org.parchmentmc.compass.validation.ValidationIssue;
import org.parchmentmc.compass.validation.Validator;
import org.parchmentmc.feather.mapping.MappingDataContainer;
import org.parchmentmc.feather.metadata.ClassMetadata;
import org.parchmentmc.feather.metadata.FieldMetadata;
import org.parchmentmc.feather.metadata.MethodMetadata;
import org.parchmentmc.feather.metadata.SourceMetadata;

import java.util.*;

public class DataValidator {
    private final Set<Validator> validators = new LinkedHashSet<>();

    public DataValidator() {
    }

    public void addValidator(Validator validator) {
        validators.add(validator);
    }

    public boolean removeValidator(Validator validator) {
        return validators.remove(validator);
    }

    public Set<Validator> getValidators() {
        return Collections.unmodifiableSet(validators);
    }

    public ResultContainer<List<? extends ValidationIssue>> validate(MappingDataContainer data, @Nullable SourceMetadata metadata) {
        ResultContainer<List<? extends ValidationIssue>> results = new ResultContainer<>();

        // Packages
        for (MappingDataContainer.PackageData pkgData : data.getPackages()) {
            List<ValidationIssue> issues = new ArrayList<>();
            for (Validator validator : validators) {
                issues.addAll(validator.validate(pkgData));
            }
            results.addPackage(new ResultContainer.PackageResult<>(pkgData.getName(), issues));
        }

        // Classes
        for (MappingDataContainer.ClassData clsData : data.getClasses()) {
            ClassMetadata clsMeta = metadata != null ? metadata.getClasses().stream()
                    .filter(s -> s.getName().getMojangName().orElse("").contentEquals(clsData.getName()))
                    .findFirst().orElse(null) : null;

            List<ValidationIssue> classIssues = new ArrayList<>();
            for (Validator validator : validators) {
                classIssues.addAll(validator.validate(clsData, clsMeta));
            }
            ResultContainer.ClassResult<List<? extends ValidationIssue>> classResult =
                    new ResultContainer.ClassResult<>(clsData.getName(), classIssues);
            results.addClass(classResult);

            // Fields
            for (MappingDataContainer.FieldData fieldData : clsData.getFields()) {
                FieldMetadata fieldMeta = clsMeta != null ? clsMeta.getFields().stream()
                        .filter(s -> s.getName().getMojangName().orElse("").contentEquals(fieldData.getName()))
                        .findFirst().orElse(null) : null;

                List<ValidationIssue> fieldIssues = new ArrayList<>();
                for (Validator validator : validators) {
                    fieldIssues.addAll(validator.validate(clsData, fieldData, clsMeta, fieldMeta));
                }
                classResult.addField(new ResultContainer.FieldResult<>(fieldData.getName(), fieldIssues));
            }

            // Methods
            for (MappingDataContainer.MethodData methodData : clsData.getMethods()) {
                MethodMetadata methodMeta = clsMeta != null ? clsMeta.getMethods().stream()
                        .filter(s -> s.getName().getMojangName().orElse("").contentEquals(methodData.getName())
                                && s.getDescriptor().getMojangName().orElse("").contentEquals(methodData.getDescriptor()))
                        .findFirst().orElse(null) : null;

                List<ValidationIssue> methodIssues = new ArrayList<>();
                for (Validator validator : validators) {
                    methodIssues.addAll(validator.validate(clsData, methodData, clsMeta, methodMeta));
                }
                ResultContainer.MethodResult<List<? extends ValidationIssue>> methodResult =
                        new ResultContainer.MethodResult<>(methodData.getName(), methodData.getDescriptor(), methodIssues);
                classResult.addMethod(methodResult);

                // Method Parameters

                for (MappingDataContainer.ParameterData paramData : methodData.getParameters()) {
                    List<ValidationIssue> paramIssues = new ArrayList<>();
                    for (Validator validator : validators) {
                        paramIssues.addAll(validator.validate(clsData, methodData, paramData, clsMeta, methodMeta));
                    }
                    methodResult.addParameter(new ResultContainer.ParameterResult<>(paramData.getIndex(), paramIssues));
                }

            }
        }

        return results;
    }
}
