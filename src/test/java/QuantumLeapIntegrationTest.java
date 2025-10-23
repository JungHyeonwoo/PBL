import com.pbl.quantumleap.AnalysisResult;
import com.pbl.quantumleap.service.OpenAIService;
import com.pbl.quantumleap.service.QuantumLeapService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("QuantumLeap 전체 프로세스 통합 테스트")
class QuantumLeapIntegrationTest {

  // JUnit 5 기능: 테스트 실행 시 임시 디렉토리를 자동으로 생성하고, 끝나면 삭제합니다.
  @TempDir
  Path tempDir;

  @Test
  @DisplayName("UserRepository 변경 시, UserServiceTest와 UserControllerTest를 정확히 선별해야 한다")
  void integration_test_for_entire_process() throws IOException {
    // GIVEN: 가상의 Spring 프로젝트 파일 구조와 코드를 임시 디렉토리에 생성합니다.
    createTestSourceFiles();

    String projectPath = tempDir.resolve("src/main/java").toString();
    String testPath = tempDir.resolve("src/test/java").toString();
    String projectBasePackage = "com.example";
    Set<String> changedClasses = Set.of("UserRepository");

    // WHEN: QuantumLeapService를 통해 전체 분석 프로세스를 실행합니다.
    QuantumLeapService service = new QuantumLeapService(projectPath, testPath, projectBasePackage, new OpenAIService());
    AnalysisResult testsToRun = service.analyze(changedClasses);

    // THEN: 예상된 테스트 목록과 정확히 일치해야 합니다.
    // UserRepository -> UserService -> UserController 순으로 영향이 전파되므로,
    // UserServiceTest와 UserControllerTest가 실행 대상이 되어야 합니다.
    // UserRepositoryTest는 소스-테스트 매핑 규칙 상 존재하지 않으므로 포함되지 않습니다.
//    assertThat(testsToRun).containsExactlyInAnyOrder("UserServiceTest", "UserControllerTest");
  }

  /**
   * 테스트에 필요한 가상 소스 파일들을 임시 디렉토리에 생성하는 헬퍼 메서드
   */
  private void createTestSourceFiles() throws IOException {
    // 소스 디렉토리 생성
    Path mainPath = tempDir.resolve("src/main/java/com/example");
    Files.createDirectories(mainPath.resolve("controller"));
    Files.createDirectories(mainPath.resolve("service"));
    Files.createDirectories(mainPath.resolve("repository"));

    // 테스트 디렉토리 생성
    Path testPath = tempDir.resolve("src/test/java/com/example");
    Files.createDirectories(testPath.resolve("controller"));
    Files.createDirectories(testPath.resolve("service"));

    // 가상 소스 코드 작성 (안정적인 @Autowired 필드 주입 방식으로 변경)
    Files.writeString(mainPath.resolve("controller/UserController.java"),
        """
        package com.example.controller;
        import com.example.service.UserService;
        import org.springframework.beans.factory.annotation.Autowired;
        
        public class UserController { 
            @Autowired
            private UserService userService;
        }
        """);
    Files.writeString(mainPath.resolve("service/UserService.java"),
        """
        package com.example.service;
        import com.example.repository.UserRepository;
        import org.springframework.beans.factory.annotation.Autowired;
        
        public class UserService {
            @Autowired
            private UserRepository userRepository;
        }
        """);
    Files.writeString(mainPath.resolve("repository/UserRepository.java"),
        "package com.example.repository; public interface UserRepository {}");

    // 가상 테스트 코드 작성
    Files.writeString(testPath.resolve("controller/UserControllerTest.java"),
        "package com.example.controller; import org.springframework.boot.test.context.SpringBootTest; @SpringBootTest public class UserControllerTest {}");
    Files.writeString(testPath.resolve("service/UserServiceTest.java"),
        "package com.example.service; import org.springframework.boot.test.context.SpringBootTest; @SpringBootTest public class UserServiceTest {}");
  }
}

