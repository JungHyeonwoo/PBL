package com.pbl.quantumleap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ProjectScanner {

  public List<Path> scan(String rootDir) {
    System.out.println("프로젝트 스캔 시작(대상 디렉토리 : " + rootDir + ")");
    Path startPath = Paths.get(rootDir);

    if (!Files.isDirectory(startPath)) {
      System.err.println("디렉토리가 유효한 디렉토리가 아닙니다.");
      return Collections.emptyList();
    }

    try (Stream<Path> stream = Files.walk(startPath)) {
      List<Path> javaFiles = stream
          .filter(path -> !Files.isDirectory(path))
          .filter(path -> path.toString().endsWith(".java"))
          .collect(Collectors.toList());

      System.out.println(javaFiles.size() + "개의 Java 파일을 찾았습니다.");
      return javaFiles;
    } catch (IOException e) {
      System.err.println("프로젝트 스캔 중 오류가 발생했습니다: " + e.getMessage());
      e.printStackTrace();
      return Collections.emptyList();
    }
  }
}
