package org.openelisglobal.delivery.service;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.delivery.dao.DeliveryQueueDAO.Claim;
import org.openelisglobal.delivery.dao.DeliveryQueueSearch;
import org.openelisglobal.delivery.dto.SentMessageDTO;
import org.openelisglobal.delivery.dto.SentMessagesPageDTO;
import org.openelisglobal.notification.valueholder.NotificationChannel;
import org.openelisglobal.notification.valueholder.NotificationLog;

/**
 * The persistent outbox: every message is written as a Sent Messages row before
 * anything is attempted, the worker claims due rows and records each attempt,
 * and Resend queues the same rendered text again.
 */
public interface DeliveryOutboxService {

    /** Writes one row with a channel row per requested channel and returns it. */
    NotificationLog enqueue(DeliveryRequest request);

    /**
     * Records a send that already happened synchronously (a channel test): one row
     * with the outcome and its single attempt.
     */
    NotificationLog recordImmediate(DeliveryRequest request, NotificationChannel channel, boolean success, String error,
            String gatewayMessageId);

    /**
     * Queues the same content again to the same recipient, linked to the source.
     */
    NotificationLog resend(Long sourceLogId, String sysUserId);

    List<Claim> claim(String worker, int limit);

    /** Sends one claimed channel row and records the outcome. */
    void attempt(Claim claim, String worker);

    SentMessagesPageDTO list(DeliveryQueueSearch search, int page, int pageSize);

    Optional<SentMessageDTO> detail(Long id);

    /**
     * Whether the laboratory has set this channel up and switched it on, so a
     * product can decide not to queue what could never be sent.
     */
    boolean isChannelSetUp(NotificationChannel channel);
}
