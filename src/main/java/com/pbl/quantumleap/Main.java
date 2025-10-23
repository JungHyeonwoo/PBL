package com.pbl.quantumleap;

import com.pbl.quantumleap.service.OpenAIService;
import com.pbl.quantumleap.service.QuantumLeapService;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.Callable;

@Command(name = "quantumleap", mixinStandardHelpOptions = true, version = "QuantumLeap 0.1",
    description = "지능적으로 테스트를 선별하여 실행하는 CI 최적화 도구")
public class Main implements Callable<Integer> {

  @Parameters(index = "0", description = "분석할 프로젝트의 루트 경로.")
  private File projectRoot;

  @Option(names = {"--base"}, description = "비교 기준이 될 이전 커밋 해시.", defaultValue = "HEAD~1")
  private String baseCommit;

  @Option(names = {"--head"}, description = "최신 커밋 해시.", defaultValue = "HEAD")
  private String headCommit;

  @Override
  public Integer call() throws Exception {
    System.err.println("🚀 QuantumLeap 분석기를 시작합니다!");
    Path projectRootPath = projectRoot.toPath();
    System.err.println("분석 대상 프로젝트: " + projectRootPath);

    // 1. Git 변경점 분석
    GitChangeFinder gitChangeFinder = new GitChangeFinder();
    Set<String> changedFilePaths = gitChangeFinder.findChangedJavaFiles(projectRootPath.toString(), baseCommit, headCommit);

    Set<String> changedClasses = changedFilePaths.stream()
        .map(path -> Paths.get(path).getFileName().toString().replace(".java", ""))
        .collect(Collectors.toSet());

    if (changedClasses.isEmpty()) {
      System.err.println("분석할 Java 변경점이 없습니다. 테스트를 건너뜁니다.");
      return 0;
    }

    // 2. 설정 파일 로드 및 경로 계산
    ConfigLoader configLoader = new ConfigLoader();
    Configuration config = configLoader.loadConfig(projectRootPath);
    Path sourcePath = projectRootPath.resolve(config.getSourceDirectory());
    Path testPath = projectRootPath.resolve(config.getTestDirectory());

    // 3. QuantumLeapService를 통해 전체 분석 수행
    QuantumLeapService service = new QuantumLeapService(
        sourcePath.toString(),
        testPath.toString(),
        config.getProjectBasePackage(),
        new OpenAIService()
    );
    AnalysisResult result = service.analyze(changedClasses);

    // 4. 아키텍처 분석 결과를 로그(stderr)로 출력
    System.err.println("\n========================================");
    System.err.println(" 아키텍처 분석 결과");
    System.err.println("========================================");
    if (result.getCircularDependencies().isEmpty()) {
      System.err.println("✅ 순환 참조가 발견되지 않았습니다.");
    } else {
      result.getCircularDependencies().forEach(cycle ->
          System.err.println("⚠️ 순환 참조 발견: " + String.join(" -> ", cycle))
      );
    }

    // 5. 최종 결과(테스트 목록)만 표준 출력(stdout)으로 출력
    if (!result.getTestsToRun().isEmpty()) {
      result.getTestsToRun().forEach(System.out::println);
    }

    return 0;
  }

  public static void main(String[] args) {
    int exitCode = new CommandLine(new Main()).execute(args);
    System.exit(exitCode);
  }
}