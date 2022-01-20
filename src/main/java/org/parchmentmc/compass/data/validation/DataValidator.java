package org.parchmentmc.compass.data.validation;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.parchmentmc.compass.data.visitation.DataVisitor.DataType;
import org.parchmentmc.compass.util.MappingUtil;
import org.parchmentmc.compass.util.ResultContainer;
import org.parchmentmc.feather.mapping.MappingDataContainer;
import org.parchmentmc.feather.metadata.ClassMetadata;
import org.parchmentmc.feather.metadata.FieldMetadata;
import org.parchmentmc.feather.metadata.MethodMetadata;
import org.parchmentmc.feather.metadata.SourceMetadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.ImmutableList.copyOf;

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
        final ResultContainer<List<? extends ValidationIssue>> results = new ResultContainer<>();

        final Set<Validator> validators = new LinkedHashSet<>(this.validators);
        validators.removeIf(v -> !v.visit(data, metadata));

        // ********** Packages ********** //
        {
            final List<ValidationIssue> packageIssues = new ArrayList<>();
            final Set<Validator> packageValidators = new LinkedHashSet<>(validators);
            // Remove all validators which do not wish to visit the packages
            packageValidators.removeIf(v -> !v.preVisit(DataType.PACKAGES));

            for (MappingDataContainer.PackageData packageData : data.getPackages()) {
                packageIssues.clear();
                packageValidators.forEach(v -> {
                    v.issueHandler = packageIssues::add;
                    v.visitPackage(packageData);
                    v.issueHandler = null;
                });

                // If this package has results, add it to the result
                if (!packageIssues.isEmpty()) {
                    results.addPackage(new ResultContainer.PackageResult<>(packageData.getName(), copyOf(packageIssues)));
                }

                // Finished visiting one package
            }

            packageValidators.forEach(v -> v.postVisit(DataType.PACKAGES));
            // All packages have been visited
        }

        // ********** Classes ********** //
        {
            final List<ValidationIssue> classIssues = new ArrayList<>();
            final Set<Validator> classValidators = new LinkedHashSet<>(validators);
            // Remove all validators which do not wish to visit classes (and children)
            classValidators.removeIf(v -> !v.preVisit(DataType.CLASSES));

            // Shared lists
            final Set<Validator> currentClassValidators = new LinkedHashSet<>(classValidators.size());
            final List<ValidationIssue> fieldIssues = new ArrayList<>();
            final Set<Validator> fieldValidators = new LinkedHashSet<>(classValidators.size());
            final List<ValidationIssue> methodIssues = new ArrayList<>();
            final Set<Validator> methodValidators = new LinkedHashSet<>(classValidators.size());
            final Set<Validator> currentMethodValidators = new LinkedHashSet<>(classValidators.size());
            final List<ValidationIssue> paramIssues = new ArrayList<>();
            final Set<Validator> paramValidators = new LinkedHashSet<>(classValidators.size());

            final Map<String, ClassMetadata> classMetadataMap = MappingUtil.buildClassMetadataMap(metadata);

            for (MappingDataContainer.ClassData classData : data.getClasses()) {
                final ClassMetadata classMetadata = classMetadataMap.get(classData.getName());
                classIssues.clear();

                currentClassValidators.clear();
                currentClassValidators.addAll(classValidators);
                // Remove all validators which do not wish to visit the children (fields, methods) of this class
                currentClassValidators.removeIf(v -> {
                    v.issueHandler = classIssues::add;
                    boolean visitChildren = v.visitClass(classData, classMetadata);
                    v.issueHandler = null;
                    return !visitChildren;
                });

                final ResultContainer.ClassResult<List<? extends ValidationIssue>> classResult =
                    new ResultContainer.ClassResult<>(classData.getName(), copyOf(classIssues));
                // ********** Fields ********** //
                {
                    fieldIssues.clear();
                    fieldValidators.clear();
                    fieldValidators.addAll(currentClassValidators);
                    // Remove all validators which do not wish to visit the fields of this class
                    fieldValidators.removeIf(v -> !v.preVisit(DataType.FIELDS));

                    for (MappingDataContainer.FieldData fieldData : classData.getFields()) {
                        final FieldMetadata fieldMetadata = getFieldMetadata(classMetadata, fieldData.getName());
                        fieldIssues.clear();

                        fieldValidators.forEach(v -> {
                            v.issueHandler = fieldIssues::add;
                            v.visitField(classData, fieldData, classMetadata, fieldMetadata);
                            v.issueHandler = null;
                        });

                        // If this field has issues, add it to the class' result
                        if (!fieldIssues.isEmpty()) {
                            classResult.addField(new ResultContainer.FieldResult<>(fieldData.getName(), copyOf(fieldIssues)));
                        }

                        // Finished visiting one field
                    }

                    fieldValidators.forEach(v -> v.postVisit(DataType.FIELDS));
                    // All fields have been visited
                }

                // ********** Methods ********** //
                {
                    methodIssues.clear();
                    methodValidators.clear();
                    methodValidators.addAll(currentClassValidators);
                    // Remove all validators which do not wish to visit methods (or its parameters)
                    methodValidators.removeIf(v -> !v.preVisit(DataType.METHODS));

                    for (MappingDataContainer.MethodData methodData : classData.getMethods()) {
                        final MethodMetadata methodMetadata = getMethodMetadata(classMetadata, methodData.getName(),
                            methodData.getDescriptor());
                        methodIssues.clear();

                        currentMethodValidators.clear();
                        currentMethodValidators.addAll(methodValidators);
                        // Remove all validators which do not wish to visit the parameters of this method
                        currentMethodValidators.removeIf(v -> {
                            v.issueHandler = methodIssues::add;
                            boolean visitChildren = v.visitMethod(classData, methodData, classMetadata, methodMetadata);
                            v.issueHandler = null;
                            return !visitChildren;
                        });

                        ResultContainer.MethodResult<List<? extends ValidationIssue>> methodResult =
                            new ResultContainer.MethodResult<>(methodData.getName(), methodData.getDescriptor(),
                                copyOf(methodIssues));
                        // ********** Parameters ********** //
                        {
                            paramIssues.clear();
                            paramValidators.clear();
                            paramValidators.addAll(currentMethodValidators);
                            // Remove all validators which do not wish to visit parameters
                            paramValidators.removeIf(v -> !v.preVisit(DataType.PARAMETERS));

                            for (MappingDataContainer.ParameterData paramData : methodData.getParameters()) {
                                paramIssues.clear();

                                paramValidators.forEach(v -> {
                                    v.issueHandler = paramIssues::add;
                                    v.visitParameter(classData, methodData, paramData, classMetadata, methodMetadata);
                                    v.issueHandler = null;
                                });

                                // If this field has issues, add it to the method' result
                                if (!paramIssues.isEmpty()) {
                                    methodResult.addParameter(new ResultContainer.ParameterResult<>(paramData.getIndex(),
                                        copyOf(paramIssues)));
                                }

                                // Finished visiting one parameter
                            }

                            paramValidators.forEach(v -> v.postVisit(DataType.PARAMETERS));
                            // All parameters of this method have been visited
                        }

                        // If this method (or its parameter) has issues, add it to the class' result
                        if (!methodResult.getData().isEmpty() || !methodResult.isEmpty()) {
                            classResult.addMethod(methodResult);
                        }

                        // Finished visiting one method
                    }

                    methodValidators.forEach(v -> v.postVisit(DataType.METHODS));
                    // All methods of this class have been visited
                }

                // If this class (or its children) has issues, add it to the result
                if (!classResult.getData().isEmpty() || !classResult.isEmpty()) {
                    results.addClass(classResult);
                }

                // Finished visiting one class
            }

            classValidators.forEach(v -> v.postVisit(DataType.CLASSES));
            // All classes have been visited
        }

        return results;
    }

    @Nullable
    private static FieldMetadata getFieldMetadata(@Nullable ClassMetadata classMeta, String fieldName) {
        if (classMeta == null) return null;

        return classMeta.getFields().stream()
            .filter(s -> s.getName().getMojangName().orElse("").contentEquals(fieldName))
            .findFirst().orElse(null);
    }

    @Nullable
    private static MethodMetadata getMethodMetadata(@Nullable ClassMetadata classMeta, String methodName, String methodDesc) {
        if (classMeta == null) return null;

        return classMeta.getMethods().stream()
            .filter(s -> s.getName().getMojangName().orElse("").contentEquals(methodName)
                && s.getDescriptor().getMojangName().orElse("").contentEquals(methodDesc))
            .findFirst().orElse(null);
    }
}
