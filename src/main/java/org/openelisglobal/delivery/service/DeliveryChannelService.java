package org.openelisglobal.delivery.service;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.delivery.dto.DeliveryChannelDTO;
import org.openelisglobal.delivery.valueholder.DeliveryChannel.Channel;

public interface DeliveryChannelService {

    /** The outcome of a test send: whether it went and what to tell the user. */
    record TestOutcome(boolean success, String message, Long sentMessageId) {
    }

    List<DeliveryChannelDTO> list();

    Optional<DeliveryChannelDTO> get(Channel channel);

    /** Stores the editable fields; a blank secret keeps the stored one. */
    DeliveryChannelDTO save(Channel channel, DeliveryChannelDTO wanted, String sysUserId);

    /**
     * Sends one test message to a typed address and records it under product Test.
     */
    TestOutcome sendTest(Channel channel, String address, String sysUserId);
}
