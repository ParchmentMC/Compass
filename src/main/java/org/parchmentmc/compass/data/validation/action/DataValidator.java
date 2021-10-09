package org.parchmentmc.compass.data.validation.action;

import com.google.common.collect.ImmutableList;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.parchmentmc.compass.util.MappingUtil;
import org.parchmentmc.compass.util.ResultContainer;
import org.parchmentmc.compass.data.validation.ValidationIssue;
import org.parchmentmc.compass.data.validation.Validator;
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

        // reusable lists
        List<ValidationIssue> packageIssues = new ArrayList<>();
        List<ValidationIssue> classIssues = new ArrayList<>();
        List<ValidationIssue> fieldIssues = new ArrayList<>();
        List<ValidationIssue> methodIssues = new ArrayList<>();
        List<ValidationIssue> paramIssues = new ArrayList<>();

        // Packages
        for (MappingDataContainer.PackageData pkgData : data.getPackages()) {
            packageIssues.clear();
            for (Validator validator : validators) {
                validator.validate(packageIssues::add, pkgData);
            }

            if (!packageIssues.isEmpty()) {
                results.addPackage(new ResultContainer.PackageResult<>(pkgData.getName(),
                        ImmutableList.copyOf(packageIssues)));
            }
        }

        final Map<String, ClassMetadata> classMetadataMap = MappingUtil.buildClassMetadataMap(metadata);

        // Classes
        for (MappingDataContainer.ClassData clsData : data.getClasses()) {
            ClassMetadata clsMeta = classMetadataMap.get(clsData.getName());

            classIssues.clear();
            for (Validator validator : validators) {
                validator.validate(classIssues::add, clsData, clsMeta);
            }
            ResultContainer.ClassResult<List<? extends ValidationIssue>> classResult =
                    new ResultContainer.ClassResult<>(clsData.getName(), ImmutableList.copyOf(classIssues));

            // Fields
            for (MappingDataContainer.FieldData fieldData : clsData.getFields()) {
                FieldMetadata fieldMeta = clsMeta != null ? clsMeta.getFields().stream()
                        .filter(s -> s.getName().getMojangName().orElse("").contentEquals(fieldData.getName()))
                        .findFirst().orElse(null) : null;

                fieldIssues.clear();
                for (Validator validator : validators) {
                    validator.validate(fieldIssues::add, clsData, fieldData, clsMeta, fieldMeta);
                }

                if (!fieldIssues.isEmpty()) {
                    classResult.addField(new ResultContainer.FieldResult<>(fieldData.getName(),
                            ImmutableList.copyOf(fieldIssues)));
                }
            }

            // Methods
            for (MappingDataContainer.MethodData methodData : clsData.getMethods()) {
                MethodMetadata methodMeta = clsMeta != null ? clsMeta.getMethods().stream()
                        .filter(s -> s.getName().getMojangName().orElse("").contentEquals(methodData.getName())
                                && s.getDescriptor().getMojangName().orElse("").contentEquals(methodData.getDescriptor()))
                        .findFirst().orElse(null) : null;

                methodIssues.clear();
                for (Validator validator : validators) {
                    validator.validate(methodIssues::add, clsData, methodData, clsMeta, methodMeta);
                }
                ResultContainer.MethodResult<List<? extends ValidationIssue>> methodResult =
                        new ResultContainer.MethodResult<>(methodData.getName(), methodData.getDescriptor(),
                                ImmutableList.copyOf(methodIssues));

                // Method Parameters
                for (MappingDataContainer.ParameterData paramData : methodData.getParameters()) {
                    paramIssues.clear();
                    for (Validator validator : validators) {
                        validator.validate(paramIssues::add, clsData, methodData, paramData, clsMeta, methodMeta);
                    }

                    if (!paramIssues.isEmpty()) {
                        methodResult.addParameter(new ResultContainer.ParameterResult<>(paramData.getIndex(),
                                ImmutableList.copyOf(paramIssues)));
                    }
                }

                if (!methodResult.getData().isEmpty() || !methodResult.isEmpty()) {
                    classResult.addMethod(methodResult);
                }
            }

            if (!classResult.getData().isEmpty() || !classResult.isEmpty()) {
                results.addClass(classResult);
            }
        }

        return results;
    }
}
