package org.openelisglobal.delivery.dao;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.delivery.valueholder.DeliveryChannel;
import org.openelisglobal.delivery.valueholder.DeliveryChannel.Channel;

public interface DeliveryChannelDAO extends BaseDAO<DeliveryChannel, Long> {

    List<DeliveryChannel> findAllOrdered();

    Optional<DeliveryChannel> findByChannel(Channel channel);

    DeliveryChannel save(DeliveryChannel channel);

    /** Records the outcome of a send without touching the configured fields. */
    void recordOutcome(Channel channel, boolean success, String error);
}
