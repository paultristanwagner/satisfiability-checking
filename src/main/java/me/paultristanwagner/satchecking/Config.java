package me.paultristanwagner.satchecking;

import me.paultristanwagner.satchecking.sat.solver.DPLLCDCLSolver;
import me.paultristanwagner.satchecking.sat.solver.DPLLSolver;
import me.paultristanwagner.satchecking.sat.solver.EnumerationSolver;
import me.paultristanwagner.satchecking.sat.solver.SATSolver;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

/**
 * @author Paul Tristan Wagner <paultristanwagner@gmail.com>
 * @version 1.0
 */
public class Config {

  private static final String FILE_NAME = "config.properties";
  private static final String MAXIMUM = "maximum";

  private static Config config;

  public static Config load() {
    if (config == null) {
      File file = new File(FILE_NAME);
      if (!file.exists()) {
        // create and store default config
        Config defaultCfg = Config.defaultConfig();
        config = defaultCfg;
        defaultCfg.save();
      } else {
        try {
          FileReader reader = new FileReader(file);
          Properties properties = new Properties();
          properties.load(reader);
          reader.close();
          config = new Config(properties);
        } catch (IOException e) {
          e.printStackTrace();
          return Config.defaultConfig();
        }
      }
    }

    return config;
  }

  public static Config reload() {
    config = null;
    return Config.load();
  }

  private static Config defaultConfig() {
    Properties defaultProperties = new Properties();
    defaultProperties.setProperty("satSolver", "DPLL+CDCL");
    defaultProperties.setProperty("maxModelCount", MAXIMUM);
    defaultProperties.setProperty("printModels", "true");
    defaultProperties.setProperty("reducedAssignments", "false");
    return new Config(defaultProperties);
  }

  private final Properties properties;

  private Config(Properties properties) {
    this.properties = properties;
  }

  public Properties getProperties() {
    return properties;
  }

  public void save() {
    File file = new File(FILE_NAME);
    try {
      FileWriter fileWriter = new FileWriter(file);
      properties.store(fileWriter, null);
      fileWriter.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public SATSolver getSolver() {
    String solverName = properties.getProperty("satSolver");
    if (solverName.equalsIgnoreCase("Enumeration")) {
      return new EnumerationSolver();
    } else if (solverName.equalsIgnoreCase("DPLL")) {
      return new DPLLSolver();
    } else if (solverName.equalsIgnoreCase("DPLL+CDCL")) {
      return new DPLLCDCLSolver();
    }
    throw new IllegalStateException("Could not load solver from config");
  }

  public boolean printModels() {
    return Boolean.parseBoolean(properties.getProperty("printModels", "true"));
  }

  public boolean reducedAssignments() {
    return Boolean.parseBoolean(properties.getProperty("reducedAssignments", "false"));
  }

  public Long getMaxModelCount() {
    String maxModelCountString = properties.getProperty("maxModelCount", MAXIMUM);
    if (maxModelCountString.equalsIgnoreCase(MAXIMUM)) {
      maxModelCountString = Long.toString(Long.MAX_VALUE);
    }
    return Long.parseLong(maxModelCountString);
  }
}
