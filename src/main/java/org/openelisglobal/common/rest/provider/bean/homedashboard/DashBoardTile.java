package org.openelisglobal.common.rest.provider.bean.homedashboard;

import java.util.stream.Stream;

public class DashBoardTile {

    public enum TileType {
        ORDERS_IN_PROGRESS, ORDERS_READY_FOR_VALIDATION, ORDERS_COMPLETED_TODAY, ORDERS_PARTIALLY_COMPLETED_TODAY,
        ORDERS_ENTERED_BY_USER_TODAY, ORDERS_REJECTED_TODAY, UN_PRINTED_RESULTS, INCOMING_ORDERS,
        AVERAGE_TURN_AROUND_TIME, DELAYED_TURN_AROUND, ORDERS_FOR_USER,
        /**
         * OGC-742 back-compat alias. The original spelling was a typo ({@code PATIALLY}
         * vs {@code PARTIALLY}). Existing deployments that haven't picked up the FE
         * rename keep working by binding to this value, which short-circuits to the
         * partial-today branch in the provider. Schedule removal one or two releases
         * after the FE has been rolled out everywhere.
         */
        @Deprecated
        ORDERS_PATIALLY_COMPLETED_TODAY;

        public static Stream<TileType> stream() {
            return Stream.of(TileType.values());
        }
    }
}
