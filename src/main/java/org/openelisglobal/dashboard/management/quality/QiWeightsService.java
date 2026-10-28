package org.openelisglobal.dashboard.management.quality;

public interface QiWeightsService {

    WeightsView getWeights();

    /** Throws IllegalArgumentException unless {@link WeightsView#isValid()}. */
    void saveWeights(WeightsView weights, String sysUserId);
}
