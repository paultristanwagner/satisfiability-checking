package me.paultristanwagner.satchecking.theory.nonlinear;

import java.util.*;

public class Cell {

  private final List<String> variables;
  private final List<Interval> intervals;

  private Cell(List<String> variables, List<Interval> intervals) {
    this.variables = variables;
    this.intervals = intervals;
  }

  public static Cell cell(List<String> variables, List<Interval> intervals) {
    return new Cell(variables, intervals);
  }

  public static Cell cell(List<String> variables, Interval... intervalArray) {
    return new Cell(variables, Arrays.asList(intervalArray));
  }

  public static Cell emptyCell() {
    return new Cell(new ArrayList<>(), new ArrayList<>());
  }

  public Cell extend(String variable, Interval interval) {
    List<String> newVariables = new ArrayList<>(this.variables);
    newVariables.add(variable);

    List<Interval> newIntervals = new ArrayList<>(this.intervals);
    newIntervals.add(interval);

    return cell(newVariables, newIntervals);
  }

  public Map<String, RealAlgebraicNumber> chooseSamplePoint() {
    Map<String, RealAlgebraicNumber> samplePoint = new HashMap<>();
    for (int i = 0; i < intervals.size(); i++) {
      samplePoint.put(variables.get(i), intervals.get(i).chooseSample());
    }

    return samplePoint;
  }

  public List<Interval> getIntervals() {
    return intervals;
  }

  @Override
  public String toString() {
    return "Cell{" + "intervals=" + intervals + '}';
  }
}
