package org.openelisglobal.delivery.daoimpl;

import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.delivery.dao.DeliveryChannelDAO;
import org.openelisglobal.delivery.valueholder.DeliveryChannel;
import org.openelisglobal.delivery.valueholder.DeliveryChannel.Channel;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class DeliveryChannelDAOImpl extends BaseDAOImpl<DeliveryChannel, Long> implements DeliveryChannelDAO {

    public DeliveryChannelDAOImpl() {
        super(DeliveryChannel.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryChannel> findAllOrdered() {
        try {
            return entityManager
                    .createQuery("FROM DeliveryChannel c ORDER BY CASE c.channel WHEN 'EMAIL' THEN 0 WHEN 'SMS' THEN 1"
                            + " ELSE 2 END", DeliveryChannel.class)
                    .getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in DeliveryChannel findAllOrdered()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DeliveryChannel> findByChannel(Channel channel) {
        try {
            TypedQuery<DeliveryChannel> query = entityManager
                    .createQuery("FROM DeliveryChannel c WHERE c.channel = :channel", DeliveryChannel.class);
            query.setParameter("channel", channel);
            query.setMaxResults(1);
            List<DeliveryChannel> results = query.getResultList();
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in DeliveryChannel findByChannel()", e);
        }
    }

    @Override
    public DeliveryChannel save(DeliveryChannel channel) {
        try {
            if (channel.getId() == null) {
                entityManager.persist(channel);
                return channel;
            }
            return entityManager.merge(channel);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in DeliveryChannel save()", e);
        }
    }

    @Override
    public void recordOutcome(Channel channel, boolean success, String error) {
        try {
            String sql = success
                    ? "UPDATE clinlims.delivery_channel SET last_success_at = now(), last_error = NULL"
                            + " WHERE channel = :channel"
                    : "UPDATE clinlims.delivery_channel SET last_failure_at = now(), last_error = "
                            + (error == null ? "NULL" : ":error") + " WHERE channel = :channel";
            var query = entityManager.createNativeQuery(sql).setParameter("channel", channel.name());
            if (!success && error != null) {
                query.setParameter("error", error.substring(0, Math.min(error.length(), 2000)));
            }
            query.executeUpdate();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in DeliveryChannel recordOutcome()", e);
        }
    }
}
