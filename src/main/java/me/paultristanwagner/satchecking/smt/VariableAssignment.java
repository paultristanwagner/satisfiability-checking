package me.paultristanwagner.satchecking.smt;

import java.util.*;

public class VariableAssignment<O> extends HashMap<String, O> {

  public VariableAssignment() {
  }

  public VariableAssignment(Map<String, O> assignments) {
    this.putAll(assignments);
  }

  public void assign(String variable, O value) {
    this.put(variable, value);
  }

  public O getAssignment(String variable) {
    return this.get(variable);
  }

  public Set<String> getVariables() {
    return this.keySet();
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    List<String> variables = new ArrayList<>(this.keySet());
    variables.sort(String::compareTo);

    for (String variable : variables) {
      O value = this.get(variable);
      builder.append(variable).append("=").append(value).append("; ");
    }

    return builder.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    VariableAssignment<?> that = (VariableAssignment<?>) o;
    return Objects.equals(this.keySet(), that.keySet()) &&
        this.keySet().stream().allMatch(key -> Objects.equals(this.get(key), that.get(key)));
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.keySet().stream().map(key -> Objects.hash(key, this.get(key))).toArray());
  }
}
