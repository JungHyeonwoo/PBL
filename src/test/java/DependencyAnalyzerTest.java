//import com.github.javaparser.JavaParser;
//import com.github.javaparser.ParseResult;
//import com.github.javaparser.ast.CompilationUnit;
//import com.pbl.quantumleap.DependencyAnalyzer;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//
//import java.util.Set;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//class DependencyAnalyzerTest {
//
//  private DependencyAnalyzer analyzer;
//  private final JavaParser javaParser = new JavaParser();
//
//  @BeforeEach
//  void setUp() {
//    // 테스트를 실행하기 전, 프로젝트에 존재하는 모든 클래스 목록을 미리 정의합니다.
//    Set<String> knownProjectClasses = Set.of("UserController", "UserService", "UserRepository");
//    // 분석기를 '알려진 클래스 목록'과 함께 초기화합니다.
//    analyzer = new DependencyAnalyzer("com.example", knownProjectClasses);
//  }
//
//  @Test
//  @DisplayName("생성자 주입과 Import를 통해 의존성을 정확히 분석해야 한다")
//  void analyze_should_find_dependencies_from_constructor_and_import() {
//    // GIVEN: UserService에 의존하는 UserController 코드
//    String userControllerCode = """
//                package com.example.controller;
//                import com.example.service.UserService;
//
//                public class UserController {
//                    private final UserService userService;
//
//                    public UserController(UserService userService) {
//                        this.userService = userService;
//                    }
//                }
//                """;
//    CompilationUnit cu = parseCode(userControllerCode);
//
//    // WHEN: 의존성을 분석하면
//    Set<String> dependencies = analyzer.analyze(cu);
//
//    // THEN: UserService를 의존성으로 찾아내야 한다.
//    assertThat(dependencies).containsExactly("UserService");
//  }
//
//  @Test
//  @DisplayName("필드 주입(@Autowired)을 통해 의존성을 정확히 분석해야 한다")
//  void analyze_should_find_dependencies_from_field_injection() {
//    String userServiceCode = """
//                package com.example.service;
//                import com.example.repository.UserRepository;
//                import org.springframework.beans.factory.annotation.Autowired;
//
//                public class UserService {
//                    @Autowired
//                    private UserRepository userRepository;
//                }
//                """;
//    CompilationUnit cu = parseCode(userServiceCode);
//
//    // WHEN: 의존성을 분석하면
//    Set<String> dependencies = analyzer.analyze(cu);
//
//    // THEN: UserRepository를 의존성으로 찾아내야 한다.
//    assertThat(dependencies).containsExactly("UserRepository");
//  }
//
//  @Test
//  @DisplayName("의존성이 없는 클래스는 빈 Set을 반환해야 한다")
//  void analyze_should_return_empty_set_for_no_dependencies() {
//    // GIVEN: 의존성이 없는 UserRepository 코드
//    String userRepositoryCode = """
//                package com.example.repository;
//
//                public interface UserRepository {
//                    // ...
//                }
//                """;
//    CompilationUnit cu = parseCode(userRepositoryCode);
//
//    // WHEN: 의존성을 분석하면
//    Set<String> dependencies = analyzer.analyze(cu);
//
//    // THEN: 의존성 목록은 비어있어야 한다.
//    assertThat(dependencies).isEmpty();
//  }
//
//  /**
//   * 테스트용 헬퍼 메서드: 문자열 코드를 파싱하여 CompilationUnit 객체를 반환합니다.
//   * @param code 파싱할 Java 코드 문자열
//   * @return 파싱된 CompilationUnit 객체
//   */
//  private CompilationUnit parseCode(String code) {
//    ParseResult<CompilationUnit> parseResult = javaParser.parse(code);
//    return parseResult.getResult().orElseThrow(() -> new RuntimeException("코드 파싱에 실패했습니다."));
//  }
//}
//
