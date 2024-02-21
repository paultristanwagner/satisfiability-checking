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
}
