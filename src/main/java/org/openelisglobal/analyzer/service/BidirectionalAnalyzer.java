package org.openelisglobal.analyzer.service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public interface BidirectionalAnalyzer {

    List<LISAction> getSupportedLISActions();

    boolean runLISAction(String actionName, Map<String, String> actionParameters);

    public class LISAction {
        private String actionName;
        private Map<Locale, String> displayNames;
        private boolean automaticAction = false;
        private int actionFrequency = 30;
        private TimeUnit frequencyTimeUnit = TimeUnit.SECONDS;

        public String getActionName() {
            return actionName;
        }

        public void setActionName(String actionName) {
            this.actionName = actionName;
        }

        public boolean isAutomaticAction() {
            return automaticAction;
        }

        public void setAutomaticAction(boolean automaticAction) {
            this.automaticAction = automaticAction;
        }

        public int getActionFrequency() {
            return actionFrequency;
        }

        public void setActionFrequency(int actionFrequency) {
            this.actionFrequency = actionFrequency;
        }

        public TimeUnit getFrequencyTimeUnit() {
            return frequencyTimeUnit;
        }

        public void setFrequencyTimeUnit(TimeUnit frequencyTimeUnit) {
            this.frequencyTimeUnit = frequencyTimeUnit;
        }

        public Map<Locale, String> getDisplayNames() {
            return displayNames;
        }

        public void setDisplayNames(Map<Locale, String> displayNames) {
            this.displayNames = displayNames;
        }
    }
}
