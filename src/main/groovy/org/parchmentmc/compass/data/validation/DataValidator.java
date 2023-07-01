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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

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
        // Remove all validators which do not wish to visit the data
        // (shouldn't really happen with Validators, but this is the DataVisitor contract)
        validators.removeIf(v -> !v.visit(data, metadata));

        // Shared context
        final Context ctx = new Context(validators.size());

        // ********** Packages ********** //
        // Remove all validators which do not wish to visit the packages
        ctx.preVisit(ctx.packageValidators, validators, DataType.PACKAGES);

        for (MappingDataContainer.PackageData packageData : data.getPackages()) {

            ctx.matching(ctx.packageValidators, ctx.packageIssues,
                    v -> v.visitPackage(packageData),
                    i -> results.addPackage(new ResultContainer.PackageResult<>(packageData.getName(), i)));

            // Finished visiting one package
        }

        ctx.postVisit(ctx.packageValidators, DataType.PACKAGES);
        // All packages have been visited

        // ********** Classes ********** //
        // Remove all validators which do not wish to visit classes (and children)
        ctx.preVisit(ctx.classValidators, validators, DataType.CLASSES);

        final Map<String, ClassMetadata> classMetadataMap = MappingUtil.buildClassMetadataMap(metadata);

        for (MappingDataContainer.ClassData classData : data.getClasses()) {
            final ClassMetadata classMetadata = classMetadataMap.get(classData.getName());

            // Remove all validators which do not wish to visit the children (fields, methods) of this class
            final ResultContainer.ClassResult<List<? extends ValidationIssue>> classResult =
                    ctx.removeMatching(ctx.currentClassValidators, ctx.classValidators, ctx.classIssues,
                            v -> !v.visitClass(classData, classMetadata),
                            i -> new ResultContainer.ClassResult<>(classData.getName(), i));

            // ********** Fields ********** //
            // Remove all validators which do not wish to visit the fields of this class
            ctx.preVisit(ctx.fieldValidators, ctx.currentClassValidators, DataType.FIELDS);

            for (MappingDataContainer.FieldData fieldData : classData.getFields()) {
                final FieldMetadata fieldMetadata = MappingUtil.getFieldMetadata(classMetadata, fieldData.getName());

                ctx.matching(ctx.fieldValidators, ctx.fieldIssues,
                        v -> v.visitField(classData, fieldData, classMetadata, fieldMetadata),
                        i -> classResult.addField(new ResultContainer.FieldResult<>(fieldData.getName(), i)));

                // Finished visiting one field
            }

            ctx.postVisit(ctx.fieldValidators, DataType.FIELDS);
            // All fields have been visited

            // ********** Methods ********** //
            // Remove all validators which do not wish to visit methods (or its parameters)
            ctx.preVisit(ctx.methodValidators, ctx.currentClassValidators, DataType.METHODS);

            for (MappingDataContainer.MethodData methodData : classData.getMethods()) {
                final MethodMetadata methodMetadata = MappingUtil.getMethodMetadata(classMetadata, methodData.getName(),
                        methodData.getDescriptor());

                // Remove all validators which do not wish to visit the parameters of this method
                final ResultContainer.MethodResult<List<? extends ValidationIssue>> methodResult =
                        ctx.removeMatching(ctx.currentMethodValidators, ctx.methodValidators, ctx.methodIssues,
                                v -> !v.visitMethod(classData, methodData, classMetadata, methodMetadata),
                                i -> new ResultContainer.MethodResult<>(methodData.getName(), methodData.getDescriptor(), i));

                // ********** Parameters ********** //
                // Remove all validators which do not wish to visit parameters
                ctx.preVisit(ctx.paramValidators, ctx.currentMethodValidators, DataType.PARAMETERS);

                for (MappingDataContainer.ParameterData paramData : methodData.getParameters()) {

                    ctx.matching(ctx.paramValidators, ctx.paramIssues,
                            v -> v.visitParameter(classData, methodData, paramData, classMetadata, methodMetadata),
                            i -> methodResult.addParameter(new ResultContainer.ParameterResult<>(paramData.getIndex(), i)));

                    // Finished visiting one parameter
                }

                ctx.postVisit(ctx.paramValidators, DataType.PARAMETERS);
                // All parameters of this method have been visited

                // If this method (or its parameter) has issues, add it to the class' result
                if (!methodResult.getData().isEmpty() || !methodResult.isEmpty()) {
                    classResult.addMethod(methodResult);
                }

                // Finished visiting one method
            }

            ctx.postVisit(ctx.methodValidators, DataType.METHODS);
            // All methods of this class have been visited

            // If this class (or its children) has issues, add it to the result
            if (!classResult.getData().isEmpty() || !classResult.isEmpty()) {
                results.addClass(classResult);
            }

            // Finished visiting one class
        }

        ctx.postVisit(ctx.classValidators, DataType.CLASSES);
        // All classes have been visited

        return results;
    }

    private static class Context {
        final List<ValidationIssue> packageIssues = new ArrayList<>();
        final Set<Validator> packageValidators;
        final List<ValidationIssue> classIssues = new ArrayList<>();
        final Set<Validator> classValidators;
        final Set<Validator> currentClassValidators;
        final List<ValidationIssue> fieldIssues = new ArrayList<>();
        final Set<Validator> fieldValidators;
        final List<ValidationIssue> methodIssues = new ArrayList<>();
        final Set<Validator> methodValidators;
        final Set<Validator> currentMethodValidators;
        final List<ValidationIssue> paramIssues = new ArrayList<>();
        final Set<Validator> paramValidators;

        public Context(int validators) {
            this.packageValidators = new LinkedHashSet<>(validators);
            this.classValidators = new LinkedHashSet<>(validators);
            this.currentClassValidators = new LinkedHashSet<>(validators);
            this.fieldValidators = new LinkedHashSet<>(validators);
            this.methodValidators = new LinkedHashSet<>(validators);
            this.currentMethodValidators = new LinkedHashSet<>(validators);
            this.paramValidators = new LinkedHashSet<>(validators);
        }

        public void matching(Set<Validator> workingSet, List<ValidationIssue> issues,
                             Consumer<Validator> validatorConsumer, Consumer<List<? extends ValidationIssue>> issuesConsumer) {
            issues.clear();
            workingSet.forEach(v -> {
                v.issueHandler = issues::add;
                validatorConsumer.accept(v);
                v.issueHandler = null;
            });
            if (!issues.isEmpty()) {
                issuesConsumer.accept(copyOf(issues));
                issues.clear();
            }
        }

        public <C> C removeMatching(Set<Validator> workingSet, Set<Validator> superset, List<ValidationIssue> issues,
                                    Predicate<Validator> validatorPredicate,
                                    Function<List<? extends ValidationIssue>, C> resultCreator) {
            issues.clear();
            workingSet.clear();
            workingSet.addAll(superset);
            workingSet.removeIf(v -> {
                v.issueHandler = issues::add;
                boolean walkChildren = validatorPredicate.test(v);
                v.issueHandler = null;
                return walkChildren;
            });
            C result = resultCreator.apply(copyOf(issues));
            issues.clear();
            return result;
        }

        public void preVisit(Set<Validator> workingSet, Set<Validator> superset, DataType type) {
            workingSet.clear();
            workingSet.addAll(superset);
            workingSet.removeIf(v -> !v.preVisit(type));
        }

        public void postVisit(Set<Validator> workingSet, DataType type) {
            workingSet.forEach(v -> v.postVisit(type));
        }
    }
}
