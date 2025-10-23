package com.pbl.quantumleap;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Set;
import java.util.stream.Collectors;

public class GitChangeFinder {

  /**
   * 두 Git 커밋 해시 사이에서 변경된 .java 파일 목록을 반환합니다.
   * @param projectPath Git 저장소의 로컬 경로
   * @param baseCommitHash 비교 기준이 될 이전 커밋 해시
   * @param headCommitHash 최신 커밋 해시
   * @return 변경된 .java 파일의 경로 Set (프로젝트 루트 기준)
   */
  public Set<String> findChangedJavaFiles(String projectPath, String baseCommitHash, String headCommitHash) {
    // 분석 로그는 System.err로 출력합니다.
    System.err.println("\n--- Git 변경점 분석 시작 ---");
    System.err.println("Base: " + baseCommitHash + ", Head: " + headCommitHash);

    // "git diff --name-only [base] [head]" 명령어를 준비합니다.
    ProcessBuilder processBuilder = new ProcessBuilder(
        "git", "diff", "--name-only", baseCommitHash, headCommitHash);

    // 명령어를 실행할 디렉토리를 지정합니다.
    processBuilder.directory(new File(projectPath));

    try {
      Process process = processBuilder.start();
      // 명령어 실행 결과를 읽어옵니다.
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {

        Set<String> changedFiles = reader.lines()
            .filter(line -> line.endsWith(".java")) // .java 파일만 필터링
            .collect(Collectors.toSet());

        int exitCode = process.waitFor(); // 명령어가 끝날 때까지 대기
        if (exitCode != 0) {
          System.err.println("Git diff 명령어 실행 중 오류가 발생했습니다. Exit code: " + exitCode);
          return Set.of();
        }

        System.err.println("✅ " + changedFiles.size() + "개의 변경된 Java 파일을 찾았습니다.");
        return changedFiles;

      }
    } catch (IOException | InterruptedException e) {
      System.err.println("Git 변경점 분석 중 예외가 발생했습니다: " + e.getMessage());
      Thread.currentThread().interrupt(); // InterruptedException 발생 시 스레드 인터럽트 상태 복원
      return Set.of();
    }
  }
}

