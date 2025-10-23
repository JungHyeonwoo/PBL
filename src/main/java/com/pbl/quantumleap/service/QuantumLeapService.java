package com.pbl.quantumleap.service;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.pbl.quantumleap.*; // Import all classes from the package
import com.pbl.quantumleap.model.DependencyGraph;
import com.pbl.quantumleap.model.DependencyGraph.ClassNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class QuantumLeapService {

  private final String projectPath;
  private final String testPath;
  private final String projectBasePackage;
  private final OpenAIService openAIService; // OpenAI 서비스 필드 추가

  // 생성자 수정: OpenAIService를 주입받도록 변경
  public QuantumLeapService(String projectPath, String testPath, String projectBasePackage, OpenAIService openAIService) {
    this.projectPath = projectPath;
    this.testPath = testPath;
    this.projectBasePackage = projectBasePackage;
    this.openAIService = openAIService; // 주입받은 서비스 저장
  }

  /**
   * 전체 분석을 수행하고, 테스트 목록과 아키텍처 분석 결과를 포함하는 AnalysisResult를 반환합니다.
   * @param changedClasses 변경된 소스 클래스 이름 Set
   * @return 통합 분석 결과 객체
   */
  public AnalysisResult analyze(Set<String> changedClasses) throws IOException {
    ProjectScanner scanner = new ProjectScanner();
    List<Path> javaFiles = scanner.scan(projectPath);

    DependencyGraph dependencyGraph = buildDependencyGraph(javaFiles);
    System.err.println("✅ 의존성 그래프 생성이 완료되었습니다!"); // 로그는 stderr로 출력

    // 생성된 그래프를 JSON 문자열로 변환 (AI 입력용)
    ArchitectureJsonExporter jsonExporter = new ArchitectureJsonExporter();
    String graphJsonString = jsonExporter.getJsonString(dependencyGraph, Set.of()); // AI 분석에는 모든 클래스 포함

    // --- 신규 추가: AI를 이용한 아키텍처 분석 ---
    System.err.println("\n--- AI 아키텍처 건전성 분석 시작 ---");
    String aiPrompt = createAIPrompt(graphJsonString);
    String aiSuggestions = openAIService.callOpenAI(aiPrompt);
    System.err.println("✅ AI 분석 완료.");
    // ------------------------------------------

    // @Entity와 DTO 클래스들을 식별하여 순환 참조 분석에서 제외할 목록 생성
    Set<String> classesToExclude = dependencyGraph.getNodes().values().stream()
        .filter(node -> node.isEntity() || node.isDto())
        .map(ClassNode::getName)
        .collect(Collectors.toSet());
    System.err.println("ℹ️ 순환 참조 분석 제외 클래스: " + classesToExclude);

    // 규칙 기반 아키텍처 분석 수행 (순환 참조)
    ArchitectureAnalyzer architectureAnalyzer = new ArchitectureAnalyzer(dependencyGraph, classesToExclude);
    List<List<String>> cycles = architectureAnalyzer.detectCircularDependencies();

    // 테스트 선별
    TestFinder testFinder = new TestFinder();
    Map<String, String> sourceToTestMap = testFinder.findTests(scanner.scan(testPath));
    TestSelector testSelector = new TestSelector(dependencyGraph, sourceToTestMap);
    Set<String> testsToRun = testSelector.selectTests(changedClasses);

    // 결과 리포팅 (HTML 생성)
    GraphVisualizer visualizer = new GraphVisualizer();
    visualizer.generateInteractiveReport(dependencyGraph, changedClasses, testSelector.getImpactSet(), "build/reports/quantumleap");

    // 최종 결과를 AnalysisResult 객체에 담아 반환 (AI 결과 포함)
    return new AnalysisResult(testsToRun, cycles, aiSuggestions);
  }

  /**
   * OpenAI API에 전송할 프롬프트를 생성합니다.
   * @param graphJson 분석할 의존성 그래프 JSON 문자열
   * @return 생성된 프롬프트 문자열
   */
  private String createAIPrompt(String graphJson) {
    // 프롬프트는 AI가 역할을 이해하고 원하는 결과 형식을 출력하도록 상세하게 작성합니다.
    return String.format("""
            당신은 숙련된 Java Spring 아키텍처 리뷰어입니다.
            주어진 의존성 그래프 JSON 데이터를 분석하여 잠재적인 아키텍처 문제점을 찾아주세요.
            JSON 형식은 다음과 같습니다: {"classes": [{"ClassName": {"depends": ["Dependency1", ...], "dependencies": ["Dependent1", ...]}}, ...]}
            'depends'는 해당 클래스가 의존하는 클래스 목록(상위 구조)이고, 'dependencies'는 해당 클래스를 의존하는 클래스 목록(하위 구조)입니다.

            분석해야 할 주요 항목:
            1.  **높은 결합도(High Coupling):** 특정 클래스가 너무 많은 다른 클래스에 의존하거나, 너무 많은 다른 클래스로부터 의존받는 경우 (예: 10개 이상). '과대 클래스(God Class)' 가능성을 언급해주세요.
            2.  **잠재적 계층 위반:** 일반적으로 Controller는 Service에, Service는 Repository에 의존합니다. 이 패턴을 벗어나는 의심스러운 의존성이 있는지 확인해주세요. (예: Controller가 Repository에 직접 의존)
            3.  **불안정한 의존성:** 변경 가능성이 높은 모듈(예: 외부 API 연동)에 안정적인 핵심 도메인 모듈이 직접 의존하는 경우 위험할 수 있습니다. (추상화 원칙 위배 가능성)

            분석 결과는 마크다운 형식의 리스트로 간결하게 요약해주세요. 각 항목에는 관련된 클래스 이름을 명시하고, 문제점과 가능한 개선 방안을 간단히 제시해주세요. 심각한 문제가 없다면 "특별한 아키텍처 문제점은 발견되지 않았습니다." 라고 답변해주세요.

            분석 대상 JSON 데이터:
            ```json
            %s
            ```
            """, graphJson);
  }

  // ... (buildDependencyGraph, findClassName 메서드는 이전과 동일)
  private DependencyGraph buildDependencyGraph(List<Path> javaFiles) throws IOException {
    DependencyGraph graph = new DependencyGraph();
    Map<String, CompilationUnit> parsedFiles = new HashMap<>();
    Set<String> knownClassNames = new HashSet<>();
    JavaParser javaParser = new JavaParser();

    // 1차 분석: 모든 클래스 이름을 노드로 먼저 등록
    for (Path filePath : javaFiles) {
      String code = Files.readString(filePath);
      ParseResult<CompilationUnit> parseResult = javaParser.parse(code);
      if (parseResult.isSuccessful() && parseResult.getResult().isPresent()) {
        CompilationUnit cu = parseResult.getResult().get();
        cu.findFirst(ClassOrInterfaceDeclaration.class).ifPresent(classDecl -> {
          String className = classDecl.getNameAsString();
          boolean isEntity = classDecl.isAnnotationPresent("Entity");
          boolean isDto = className.endsWith("Request") || className.endsWith("Response") || className.endsWith("Dto");

          parsedFiles.put(className, cu);
          knownClassNames.add(className);
          Optional<String> packageNameOpt = cu.getPackageDeclaration().map(pd -> pd.getName().asString());

          ClassNode newNode = new ClassNode(className, filePath.toString(), packageNameOpt.orElse(""));
          newNode.setEntity(isEntity);
          newNode.setDto(isDto);
          graph.addNode(newNode);
        });
      }
    }
    System.err.println(knownClassNames.size() + "개의 클래스 식별 완료."); // 로그는 stderr로 출력

    // 2차 분석: 의존성(엣지) 연결
    DependencyAnalyzer analyzer = new DependencyAnalyzer(projectBasePackage, knownClassNames);
    for (Map.Entry<String, CompilationUnit> entry : parsedFiles.entrySet()) {
      String className = entry.getKey();
      CompilationUnit cu = entry.getValue();
      ClassNode fromNode = graph.getNode(className);

      Set<String> dependencies = analyzer.analyze(cu);
      for (String dependencyName : dependencies) {
        ClassNode toNode = graph.getNode(dependencyName);
        if (fromNode != null && toNode != null) {
          graph.addDependency(fromNode, toNode);
        }
      }
    }
    return graph;
  }

  private Optional<String> findClassName(CompilationUnit cu) {
    return cu.findFirst(ClassOrInterfaceDeclaration.class)
        .map(ClassOrInterfaceDeclaration::getNameAsString);
  }
}

