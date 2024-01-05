package me.paultristanwagner.satchecking.smt;

import java.util.*;

public class VariableAssignment<O> {

  private final Map<String, O> assignments = new HashMap<>();

  public void assign(String variable, O value) {
    assignments.put(variable, value);
  }

  public O getAssignment(String variable) {
    return assignments.get(variable);
  }

  public Set<String> getVariables() {
    return assignments.keySet();
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    List<String> variables = new ArrayList<>(assignments.keySet());
    variables.sort(String::compareTo);

    for (String variable : variables) {
      O value = assignments.get(variable);
      builder.append(variable).append("=").append(value).append("; ");
    }

    return builder.toString();
  }
}
