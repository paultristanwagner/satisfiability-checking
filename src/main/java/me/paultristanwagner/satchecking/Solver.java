package me.paultristanwagner.satchecking;

/**
 * @author Paul Tristan Wagner <paultristanwagner@gmail.com>
 * @version 1.0
 */
public interface Solver {

    void load(CNF cnf);

    Assignment nextModel();

}
