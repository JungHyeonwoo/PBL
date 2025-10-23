package com.pbl.quantumleap;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.ImportDeclaration;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DependencyAnalyzer {

  private final String projectBasePackage;
  private final Set<String> knownProjectClasses;

  public DependencyAnalyzer(String projectBasePackage, Set<String> knownProjectClasses) {
    this.projectBasePackage = projectBasePackage;
    this.knownProjectClasses = knownProjectClasses;
  }

  public Set<String> analyze(CompilationUnit cu) {
    Set<String> dependenciesFromImports = findDependenciesFromImports(cu);
    Set<String> dependenciesFromFields = findDependenciesFromFields(cu);
    Set<String> dependenciesFromConstructors = findDependenciesFromConstructors(cu);
    Set<String> dependenciesFromLombok = findDependenciesFromLombok(cu);

    return Stream.of(dependenciesFromImports, dependenciesFromFields, dependenciesFromConstructors, dependenciesFromLombok)
        .flatMap(Set::stream)
        .collect(Collectors.toSet());
  }

  // ... (findDependenciesFromImports, findDependenciesFromFields, findDependenciesFromConstructors는 이전과 동일)

  private Set<String> findDependenciesFromImports(CompilationUnit cu) {
    return cu.getImports().stream()
        .filter(imp -> !imp.isStatic())
        .map(ImportDeclaration::getNameAsString)
        .filter(name -> name.startsWith(projectBasePackage))
        .map(this::getClassNameFromFqcn)
        .collect(Collectors.toSet());
  }

  private Set<String> findDependenciesFromFields(CompilationUnit cu) {
    Set<String> dependencies = new HashSet<>();
    cu.findAll(FieldDeclaration.class).forEach(field -> {
      boolean hasInjectionAnnotation = field.getAnnotations().stream()
          .anyMatch(ann -> {
            String annName = ann.getNameAsString();
            return annName.equals("Autowired") || annName.equals("Inject");
          });

      if (hasInjectionAnnotation) {
        field.getVariables().forEach(variable -> {
          String fieldType = variable.getType().asString();
          if (fieldType.contains("<")) {
            fieldType = fieldType.substring(fieldType.indexOf('<') + 1, fieldType.indexOf('>'));
          }
          if (knownProjectClasses.contains(fieldType)) {
            dependencies.add(fieldType);
          }
        });
      }
    });
    return dependencies;
  }

  private Set<String> findDependenciesFromConstructors(CompilationUnit cu) {
    Set<String> dependencies = new HashSet<>();
    cu.findAll(com.github.javaparser.ast.body.ConstructorDeclaration.class).forEach(constructor -> {
      for (Parameter parameter : constructor.getParameters()) {
        String paramType = parameter.getType().asString();
        if (knownProjectClasses.contains(paramType)) {
          dependencies.add(paramType);
        }
      }
    });
    return dependencies;
  }


  /**
   * Lombok의 @RequiredArgsConstructor와 @AllArgsConstructor 어노테이션을 분석하여 의존성을 찾습니다.
   */
  private Set<String> findDependenciesFromLombok(CompilationUnit cu) {
    Set<String> dependencies = new HashSet<>();
    cu.findFirst(ClassOrInterfaceDeclaration.class).ifPresent(classOrInterface -> {
      boolean hasRequiredArgsConstructor = classOrInterface.isAnnotationPresent("RequiredArgsConstructor");
      boolean hasAllArgsConstructor = classOrInterface.isAnnotationPresent("AllArgsConstructor");

      if (hasAllArgsConstructor) {
        // @AllArgsConstructor는 모든 필드를 대상으로 생성자를 만듭니다.
        classOrInterface.getFields().forEach(field -> addDependencyFromField(field, dependencies));
      } else if (hasRequiredArgsConstructor) {
        // @RequiredArgsConstructor는 final 필드를 대상으로 생성자를 만듭니다.
        classOrInterface.getFields().stream()
            .filter(FieldDeclaration::isFinal)
            .forEach(field -> addDependencyFromField(field, dependencies));
      }
    });
    return dependencies;
  }

  /**
   * 필드 선언에서 의존성을 추출하여 목록에 추가하는 헬퍼 메서드
   */
  private void addDependencyFromField(FieldDeclaration field, Set<String> dependencies) {
    field.getVariables().forEach(variable -> {
      String fieldType = variable.getType().asString();
      if (knownProjectClasses.contains(fieldType)) {
        dependencies.add(fieldType);
      }
    });
  }

  private String getClassNameFromFqcn(String fqcn) {
    return fqcn.substring(fqcn.lastIndexOf('.') + 1);
  }
}

