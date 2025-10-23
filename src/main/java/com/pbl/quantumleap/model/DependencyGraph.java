package com.pbl.quantumleap.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Getter
public class DependencyGraph {

  private final Map<String, ClassNode> nodes = new HashMap<>();

  public void addNode(ClassNode node) {
    nodes.put(node.getName(), node);
  }

  public void addDependency(ClassNode from, ClassNode to) {
    from.addDependency(to);
    to.addDependent(from); // 역의존성 추가
  }

  public ClassNode getNode(String className) {
    return nodes.get(className);
  }

  @Getter
  @Setter
  @ToString(of = "name")
  public static class ClassNode {
    private final String name;
    private final String filePath;
    private final String packageName;
    private boolean isEntity = false;
    private boolean isDto = false;
    private boolean isConfig = false;

    private final Set<ClassNode> dependencies = new HashSet<>();
    private final Set<ClassNode> dependents = new HashSet<>();

    public ClassNode(String name, String filePath, String packageName) {
      this.name = name;
      this.filePath = filePath;
      this.packageName = packageName;
    }

    public void addDependency(ClassNode dependency) {
      dependencies.add(dependency);
    }

    public void addDependent(ClassNode dependent) {
      dependents.add(dependent);
    }
  }
}

