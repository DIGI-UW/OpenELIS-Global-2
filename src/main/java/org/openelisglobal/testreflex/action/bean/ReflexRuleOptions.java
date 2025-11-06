package org.openelisglobal.testreflex.action.bean;

import java.util.stream.Stream;

public class ReflexRuleOptions {

    public enum GeneralRelationOptions {
        EQUALS("Equals"), NOT_EQUALS("Does not equal"), INSIDE_NORMAL_RANGE("Is inside the normal range"),
        OUTSIDE_NORMAL_RANGE("Is outside the normal range");

        private String displayName;

        private GeneralRelationOptions(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return this.displayName;
        }

        public static Stream<GeneralRelationOptions> stream() {
            return Stream.of(GeneralRelationOptions.values());
        }
    }

    public enum NumericRelationOptions {
        EQUALS("Equals"), NOT_EQUALS("Does not equal"), INSIDE_NORMAL_RANGE("Is inside the normal range"),
        OUTSIDE_NORMAL_RANGE("Is outside the normal range"), LESS_THAN_OR_EQUAL("Is less than or equal to"),
        GREATER_THAN_OR_EQUAL("is greater than or equal to"), LESS_THAN("Is less than "),
        GREATER_THAN("Is greater than"), BETWEEN("Is between ");

        private String displayName;

        private NumericRelationOptions(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return this.displayName;
        }

        public static Stream<NumericRelationOptions> stream() {
            return Stream.of(NumericRelationOptions.values());
        }
    }

    public enum OverallOptions {
        ANY("Any"), ALL("All");

        private String displayName;

        private OverallOptions(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return this.displayName;
        }

        public static Stream<OverallOptions> stream() {
            return Stream.of(OverallOptions.values());
        }
    }
}
