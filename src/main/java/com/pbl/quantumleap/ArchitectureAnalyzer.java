package com.pbl.quantumleap;

import com.pbl.quantumleap.model.DependencyGraph;
import com.pbl.quantumleap.model.DependencyGraph.ClassNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ArchitectureAnalyzer {

  private final DependencyGraph graph;
  private final Set<String> classesToExclude; // 분석에서 제외할 클래스 목록
  private final List<List<String>> cycles = new ArrayList<>();
  private final Set<ClassNode> visited = new HashSet<>(); // 전체 방문 노드 (Black Set)
  private final Set<ClassNode> recursionStack = new HashSet<>(); // 현재 탐색 경로상 노드 (Gray Set)

  public ArchitectureAnalyzer(DependencyGraph graph, Set<String> classesToExclude) {
    this.graph = graph;
    this.classesToExclude = classesToExclude;
  }

  /**
   * 의존성 그래프에서 모든 순환 참조를 탐지합니다.
   * @return 순환 참조 경로 목록 (e.g., [[A, B, C, A], [D, E, D]])
   */
  public List<List<String>> detectCircularDependencies() {
    System.out.println("\n--- 아키텍처 건전성 분석 시작 ---");
    for (ClassNode node : graph.getNodes().values()) {
      // 제외 목록에 포함된 클래스는 분석을 시작하지 않습니다.
      if (!visited.contains(node) && !classesToExclude.contains(node.getName())) {
        findCycles(node, new ArrayList<>());
      }
    }
    System.out.println("✅ " + cycles.size() + "개의 순환 참조를 발견했습니다.");
    return cycles;
  }

  /**
   * DFS 알고리즘을 사용하여 순환 참조를 찾는 재귀 함수
   * @param currentNode 현재 탐색 중인 노드
   * @param path 현재까지의 탐색 경로
   */
  private void findCycles(ClassNode currentNode, List<String> path) {
    visited.add(currentNode);
    recursionStack.add(currentNode);
    path.add(currentNode.getName());

    for (ClassNode neighbor : currentNode.getDependencies()) {
      // 제외 목록에 포함된 클래스로의 경로는 탐색하지 않습니다.
      if (classesToExclude.contains(neighbor.getName())) {
        continue;
      }

      if (!visited.contains(neighbor)) {
        findCycles(neighbor, path);
      } else if (recursionStack.contains(neighbor)) {
        // 현재 탐색 경로에 이미 있는 노드를 다시 만났다면, 순환 참조 발견!
        int cycleStartIndex = path.indexOf(neighbor.getName());
        List<String> cycle = new ArrayList<>(path.subList(cycleStartIndex, path.size()));
        cycle.add(neighbor.getName()); // 사이클의 마지막을 닫아줌
        cycles.add(cycle);
      }
    }

    // 현재 노드에서 시작하는 모든 경로 탐색이 끝나면 스택에서 제거
    recursionStack.remove(currentNode);
    path.remove(path.size() - 1);
  }
}

