package com.pbl.quantumleap;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

import com.github.javaparser.ast.body.MethodDeclaration;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TestFinder {

  public Map<String, String> findTests(List<Path> testFiles) {
    System.err.println("\n--- 테스트 클래스 분석 시작 ---");
    Map<String, String> sourceToTestMap = new HashMap<>();
    JavaParser javaParser = new JavaParser();

    for (Path testFile : testFiles) {
      try {
        String code = Files.readString(testFile);
        ParseResult<CompilationUnit> parseResult = javaParser.parse(code);

        if (parseResult.isSuccessful() && parseResult.getResult().isPresent()) {
          CompilationUnit cu = parseResult.getResult().get();
          Optional<ClassOrInterfaceDeclaration> classOpt = cu.findFirst(ClassOrInterfaceDeclaration.class);

          if (classOpt.isPresent()) {
            ClassOrInterfaceDeclaration classDecl = classOpt.get();
            String testClassName = classDecl.getNameAsString();

            String packageName = cu.getPackageDeclaration()
                .map(pd -> pd.getNameAsString() + ".")
                .orElse("");
            String fullTestClassName = packageName + testClassName;
            boolean isSpringBootTest = classDecl.getAnnotations().stream()
                .anyMatch(ann -> ann.getNameAsString().equals("SpringBootTest"));
            boolean hasTestMethod = cu.findAll(MethodDeclaration.class).stream()
                .anyMatch(m -> m.getAnnotations().stream()
                    .anyMatch(a -> a.getNameAsString().equals("Test")));

            if (testClassName.endsWith("Test") && (isSpringBootTest || hasTestMethod)) {
              String sourceClassName = testClassName.substring(0, testClassName.length() - 4);
              sourceToTestMap.put(sourceClassName, fullTestClassName);
              System.err.println("매핑됨: " + sourceClassName + " -> " + fullTestClassName);
              System.out.println(fullTestClassName);
            }
          }
        }
      } catch (IOException e) {
        System.err.println("테스트 파일을 읽는 중 오류가 발생했습니다: " + testFile);
      }
    }
    System.err.println(sourceToTestMap.size() + "개의 테스트 클래스를 소스 클래스와 매핑했습니다.");
    return sourceToTestMap;
  }
}
