package com.pbl.quantumleap;

import com.pbl.quantumleap.service.OpenAIService;
import com.pbl.quantumleap.service.QuantumLeapService;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@Command(name = "quantumleap", mixinStandardHelpOptions = true, version = "QuantumLeap 0.1",
    description = "지능적으로 테스트를 선별하여 실행하는 CI 최적화 도구")
public class Main implements Callable<Integer> {

  @Parameters(index = "0", description = "분석할 프로젝트의 루트 경로.")
  private File projectRoot;

  // --- 수정: required=true 제거, 기본값 유지 ---
  // CI에서는 자동으로 값이 주입되고, 로컬에서는 생략 시 기본값(HEAD~1) 사용
  @Option(names = {"--base"}, description = "비교 기준이 될 이전 커밋 해시.", defaultValue = "HEAD~1")
  private String baseCommit;

  // --- 수정: required=true 제거, 기본값 유지 ---
  // CI에서는 자동으로 값이 주입되고, 로컬에서는 생략 시 기본값(HEAD) 사용
  @Option(names = {"--head"}, description = "최신 커밋 해시.", defaultValue = "HEAD")
  private String headCommit;

  @Override
  public Integer call() throws Exception {
    System.err.println("🚀 QuantumLeap 분석기를 시작합니다!");
    Path projectRootPath = projectRoot.toPath();
    System.err.println("분석 대상 프로젝트: " + projectRootPath);
    System.err.println("분석 범위: " + baseCommit + ".." + headCommit);

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

    OpenAIService openAIService = new OpenAIService();
    // 3. QuantumLeapService를 통해 전체 분석 수행
    QuantumLeapService service = new QuantumLeapService(
        sourcePath.toString(),
        testPath.toString(),
        config.getProjectBasePackage(),
        openAIService
    );
    AnalysisResult result = service.analyze(changedClasses);

    // 4. 아키텍처 분석 결과를 로그(stderr)로 출력
    System.err.println("\n========================================");
    System.err.println(" 아키텍처 분석 결과 (규칙 기반)");
    System.err.println("========================================");
    if (result.getCircularDependencies().isEmpty()) {
      System.err.println("✅ 순환 참조가 발견되지 않았습니다.");
    } else {
      result.getCircularDependencies().forEach(cycle ->
          System.err.println("⚠️ 순환 참조 발견: " + String.join(" -> ", cycle))
      );
    }

    // 5. 최종 결과(테스트 목록)만 표준 출력(stdout)으로 출력합니다.
    if (!result.getTestsToRun().isEmpty()) {
      result.getTestsToRun().forEach(System.out::println);
    }

    return 0; // 성공
  }

  // CI 환경용
//  public static void main(String[] args) {
//    int exitCode = new CommandLine(new Main()).execute(args);
//    System.exit(exitCode);
//  }

  public static void main(String[] args) {
    // --- 로컬 IDE에서 바로 실행하기 위한 테스트용 설정 ---
    System.err.println("!!! 로컬 테스트 모드로 실행합니다 !!!");

    Main mainApp = new Main();

    // picocli가 채워줘야 할 값들을 여기서 수동으로 설정합니다.
    // TODO: 아래 경로를 실제 분석할 로컬 프로젝트 경로로 수정하세요.
    mainApp.projectRoot = new File("/Users/junghyeon-u/work/Code/SKB-IPL-API");
    // 분석하고 싶은 Git 변경 범위를 지정합니다. (예: 최근 1개 커밋)
    mainApp.baseCommit = "HEAD~1";
    mainApp.headCommit = "HEAD";

    try {
      // picocli의 execute() 대신, call() 메서드를 직접 호출하여 로직을 실행합니다.
      mainApp.call();
    } catch (Exception e) {
      System.err.println("로컬 테스트 실행 중 오류 발생: " + e.getMessage());
      e.printStackTrace();
    }

  }
}

