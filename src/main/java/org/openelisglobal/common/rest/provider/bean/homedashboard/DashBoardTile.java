package org.openelisglobal.common.rest.provider.bean.homedashboard;

import java.util.stream.Stream;

public class DashBoardTile {

    public enum TileType {
        ORDERS_IN_PROGRESS, ORDERS_READY_FOR_VALIDATION, ORDERS_COMPLETED_TODAY, ORDERS_PATIALLY_COMPLETED_TODAY,
        ORDERS_ENTERED_BY_USER_TODAY, ORDERS_REJECTED_TODAY, UN_PRINTED_RESULTS, INCOMING_ORDERS,
        AVERAGE_TURN_AROUND_TIME, DELAYED_TURN_AROUND, ORDERS_FOR_USER;

        public static Stream<TileType> stream() {
            return Stream.of(TileType.values());
        }
    }
}
