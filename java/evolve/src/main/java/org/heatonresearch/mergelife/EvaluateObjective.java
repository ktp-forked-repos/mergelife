package org.heatonresearch.mergelife;

import java.util.Random;

public interface EvaluateObjective {
    double calculateObjective(String ruleText, Random random);
}
