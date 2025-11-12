package com.pbl.quantumleap;

import com.pbl.quantumleap.model.DependencyGraph;
import com.pbl.quantumleap.model.DependencyGraph.ClassNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import lombok.Getter;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

public class TestSelector {

  private final DependencyGraph dependencyGraph;
  private final Map<String, String> sourceToTestMap;

  @Getter
  private Set<String> impactSet = new HashSet<>();
  private Map<String, String> impactPaths = new HashMap<>();

  public TestSelector(DependencyGraph dependencyGraph, Map<String, String> sourceToTestMap) {
    this.dependencyGraph = dependencyGraph;
    this.sourceToTestMap = sourceToTestMap;
  }


  public Set<String> selectTests(Set<String> changedClasses) {
    return selectTestsAndPaths(changedClasses).keySet();
  }
  /**
   * 변경된 클래스 목록을 기반으로 실행해야 할 테스트 클래스 목록을 선별합니다.
   * @param changedClasses 변경된 소스 클래스 이름 Set
   * @return 실행해야 할 테스트 클래스 이름 Set
   */
  public Map<String, List<String>> selectTestsAndPaths(Set<String> changedClasses) {
    System.err.println("--- 테스트 선별 시작 ---");
    System.err.println("변경된 클래스: " + changedClasses);

    // 1. 변경된 클래스에서 시작하여, 영향을 받는 모든 클래스와 그 경로를 찾습니다.
    this.impactPaths = findImpactPaths(changedClasses);
    this.impactSet = this.impactPaths.keySet();
    System.err.println("영향을 받은 클래스: " + this.impactSet);

    // 2. 영향받는 클래스들에 해당하는 테스트 클래스를 찾고, 그 경로를 매핑합니다.
    Map<String, List<String>> testsWithPaths = new HashMap<>();
    for (String impactedClass : this.impactSet) {
      String testClass = sourceToTestMap.get(impactedClass);
      if (testClass != null) {
        // 이 테스트가 왜 선별되었는지 경로를 역추적합니다.
        List<String> path = buildPath(impactedClass);
        testsWithPaths.put(testClass, path);
      }
    }

    System.err.println("실행될 테스트: " + testsWithPaths.keySet());
    return testsWithPaths;
  }

  /**
   * 변경된 클래스들로부터 시작하여, 그래프를 따라가며 영향을 받는 모든 클래스를 찾습니다. (BFS)
   */
  private Map<String, String> findImpactPaths(Set<String> changedClasses) {
    Map<String, String> paths = new HashMap<>();
    Queue<String> queue = new ArrayDeque<>(changedClasses);

    // 시작점(변경된 클래스)은 부모가 null입니다.
    changedClasses.forEach(cls -> paths.put(cls, null));

    while (!queue.isEmpty()) {
      String currentClassName = queue.poll();
      ClassNode currentNode = dependencyGraph.getNode(currentClassName);

      if (currentNode == null) continue;

      for (ClassNode dependentNode : currentNode.getDependents()) {
        String dependentName = dependentNode.getName();
        if (!paths.containsKey(dependentName)) {
          paths.put(dependentName, currentClassName); // 경로 기록(자식, 부모)
          queue.add(dependentName);
        }
      }
    }
    return paths;
  }

  /**
   * <자식, 부모> 맵을 역추적하여, 변경된 클래스로부터 시작하는 의존성 경로 리스트를 생성합니다.
   */
  private List<String> buildPath(String impactedClass) {
    List<String> path = new ArrayList<>();
    String current = impactedClass;
    while (current != null) {
      path.add(current);
      current = this.impactPaths.get(current); // 부모를 찾아 이동
    }
    Collections.reverse(path); // 경로를 A -> B -> C 순으로 뒤집음
    return path;
  }
}
