## 🚀 QuantumLeap: AI 활용 CI 테스트 최적화 도구
느린 CI 빌드는 이제 그만! QuantumLeap는 코드 변경 사항을 지능적으로 분석하여, 꼭 필요한 테스트만 실행시키는 CI 최적화 엔진입니다.

## 🤔 무엇을 해결하나요? (Problem)
프로젝트가 성장할수록 테스트 코드의 수는 기하급수적으로 증가하고, CI 실행 시간은 개발자의 생산성을 저해하는 가장 큰 병목 현상이 됩니다. 코드 한 줄을 수정했을 뿐인데 20~30분이 넘는 전체 테스트를 기다려야 하는 비효율은 개발자의 집중력을 끊고, 빠른 피드백이라는 CI의 본질적인 가치를 퇴색시킵니다.

QuantumLeap는 이 '기다림의 시간'을 '가치 창출의 시간'으로 되돌려주는 것을 목표로 합니다.

## ✨ 주요 기능 (Features)
⚡ 지능형 테스트 선별 (Intelligent Test Selection): JavaParser를 이용한 정적 분석으로 코드의 의존성 그래프를 생성하고, Git 변경 사항을 추적하여 영향을 받는 테스트만을 선별적으로 실행합니다.

🩺 아키텍처 건전성 분석 (Architecture Health Analysis): 의존성 그래프를 분석하여 **순환 참조(Circular Dependency)**와 같이 코드 품질을 저해하는 안티-패턴을 자동으로 감지하고 경고합니다.

🗺️ 인터랙티브 의존성 맵 시각화 (Interactive Dependency Map): 프로젝트 전체의 클래스 의존성 구조를 한눈에 파악할 수 있는 동적인 HTML 리포트("The Atlas")를 제공하여, 코드 변경의 파급 효과를 직관적으로 예측할 수 있도록 돕습니다.

📊 자동화된 PR 리포팅 (Automated PR Reporting): CI 실행 결과를 GitHub Pull Request에 자동으로 코멘트하여, 단축된 시간과 실행된 테스트 목록을 명확하게 피드백합니다.

## ⚙️ 동작 원리 (How it Works)
QuantumLeap는 GitHub Actions 워크플로우의 한 단계로 실행되는 독립적인 분석 애플리케이션입니다.

graph LR
A["fa:fa-user-edit<br/>코드 변경 및 PR"] --> B{"fa:fa-github-alt<br/>CI 환경 진입"};
B --> C["fa:fa-cogs<br/>QuantumLeap<br/>코드 분석 및<br/>테스트 선별"];
C --> D["fa:fa-play-circle<br/>선별된<br/>테스트 실행"];
D --> E["fa:fa-comment-dots<br/>결과 리포트<br/>생성 및 게시"];
E --> F["fa:fa-check-square<br/>프로세스 완료"];

개발자가 Pull Request를 생성하면 CI 워크플로우가 트리거됩니다.

CI 환경 내에서 QuantumLeap.jar가 실행되어 대상 프로젝트 코드를 분석합니다.

분석 결과를 바탕으로 실행할 테스트 목록을 생성합니다.

선별된 테스트만 실행하고, 그 결과를 PR 코멘트로 게시합니다.

## 🚀 시작하기 (Getting Started)
1. QuantumLeap 빌드
   QuantumLeap 프로젝트를 클론하고, 아래 명령어를 실행하여 실행 가능한 JAR 파일을 빌드합니다.

./gradlew build

생성된 QuantumLeap-x.x.x.jar 파일은 향후 GitHub Releases를 통해 배포될 예정입니다.

2. 대상 프로젝트에 설정 파일 추가
   분석하고 싶은 Spring 프로젝트의 루트 디렉토리에 아래 내용으로 quantumleap-config.yml 파일을 생성합니다.

quantumleap-config.yml

# 분석할 프로젝트의 기본 패키지 경로
projectBasePackage: "com.java.yourPorject"

# 소스 코드 디렉토리 (기본값: src/main/java)
sourceDirectory: "src/main/java"

# 테스트 코드 디렉토리 (기본값: src/test/java)
testDirectory: "src/test/java"

3. GitHub Actions 워크플로우 설정
   대상 프로젝트의 .github/workflows/ 디렉토리에 아래 내용으로 ci.yml 파일을 생성합니다.

.github/workflows/ci.yml
```
name: Project CI with QuantumLeap

on:
pull_request:
branches: [ main ]

jobs:
test:
runs-on: ubuntu-latest
steps:
- name: 1. Checkout Target Project Code
uses: actions/checkout@v4
with:
fetch-depth: 0 # Git 변경점 분석을 위해 전체 히스토리 가져오기

      - name: 2. Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: 3. Download QuantumLeap Analyzer
        run: |
          # TODO: 실제 운영 시에는 아래 URL을 실제 릴리즈된 JAR 파일 URL로 변경
          wget [https://github.com/your-github-id/QuantumLeap/releases/download/v0.1.0/QuantumLeap.jar](https://github.com/your-github-id/QuantumLeap/releases/download/v0.1.0/QuantumLeap.jar) -O quantumleap.jar

      - name: 4. Run QuantumLeap Analyzer
        id: analyze
        run: |
          # QuantumLeap를 실행하고, 결과를 GitHub Actions의 출력 변수로 설정
          TESTS_TO_RUN=$(java -jar quantumleap.jar . \
            --base ${{ github.event.before }} \
            --head ${{ github.event.after }})
          
          # 여러 줄의 테스트 목록을 한 줄의 문자열로 변환 (Gradle --tests 옵션 형식)
          TESTS_STRING=$(echo "$TESTS_TO_RUN" | awk '{printf "--tests %s ", $0}' | sed 's/ $//')
          echo "SELECTED_TESTS=${TESTS_STRING}" >> $GITHUB_ENV

      - name: 5. Run Selected Tests
        if: env.SELECTED_TESTS != ''
        run: ./gradlew test ${{ env.SELECTED_TESTS }}
```

## 💻 로컬에서 실행하기 (Running Locally)
로컬에서 테스트 및 디버깅을 위해 다음과 같이 실행할 수 있습니다.

# 1. QuantumLeap 빌드
```./gradlew build```

# 2. 분석 실행 (이전 커밋과 최신 커밋 비교)
```java -jar build/libs/QuantumLeap-0.0.1-SNAPSHOT.jar /path/to/your/target-project --base HEAD~1 --head HEAD```
