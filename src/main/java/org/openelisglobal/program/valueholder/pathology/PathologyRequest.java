package org.openelisglobal.program.valueholder.pathology;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.program.util.DictionaryEntryNames;

/**
 * A pathologist's request for more bench work on a case: an additional block, a
 * slide cut from a named block, or a stain on a named slide (FR-10).
 *
 * <p>
 * A request used to be a dictionary pick that every save of the case deleted
 * and wrote again, so it had no stable id, no author, no date and no target. It
 * is now raised once, changes status in place, and keeps who raised it and
 * when, what fulfilled it or why it was cancelled. A request on file from
 * before carries none of those, and none is invented for it.
 */
@Entity
@Table(name = "pathology_request")
public class PathologyRequest extends BaseObject<Integer> {

    /** Width of the value column, which holds the instruction. */
    public static final int INSTRUCTION_MAX_LENGTH = 255;

    /** Width of the cancel reason column. */
    public static final int CANCEL_REASON_MAX_LENGTH = 255;

    public enum RequestStatus {
        OPENED("Opened"), COMPLETED("Completed"), CANCELLED("Cancelled");

        private String display;

        RequestStatus(String display) {
            this.display = display;
        }

        public String getDisplay() {
            return display;
        }
    }

    public enum RequestType {
        DICTIONARY("D"), TEXT("T");

        private String code;

        RequestType(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }

        static RequestType fromCode(String code) {
            if (code.equals("D")) {
                return DICTIONARY;
            }
            if (code.equals("T")) {
                return TEXT;
            }
            return null;
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pathology_request_generator")
    @SequenceGenerator(name = "pathology_request_generator", sequenceName = "pathology_request_seq", allocationSize = 1)
    private Integer id;

    @Enumerated(EnumType.STRING)
    private RequestStatus status = RequestStatus.OPENED;

    @Enumerated(EnumType.STRING)
    private RequestType type;

    /**
     * The instruction for a text request, or the dictionary id of the name picked
     * for a request on file.
     */
    private String value;

    /** What the request asks for; null on a request on file. */
    @Enumerated(EnumType.STRING)
    @Column(name = "request_kind", length = 16)
    private RequestKind kind;

    /** The block a slide request is cut from. */
    @Column(name = "target_block_id")
    private Integer targetBlockId;

    /** The slide a stain request is applied to. */
    @Column(name = "target_slide_id")
    private Integer targetSlideId;

    /** Dictionary id of the stain a stain request asks for. */
    @Column(name = "stain_id", precision = 10, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String stainId;

    /** How soon the bench is asked to do it; null on a request on file. */
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", length = 8)
    private RequestPriority priority;

    /** The block that fulfilled a block request, when the bench named one. */
    @Column(name = "result_block_id")
    private Integer resultBlockId;

    /**
     * The slide that fulfilled a slide or stain request, when the bench named one.
     */
    @Column(name = "result_slide_id")
    private Integer resultSlideId;

    /** Why the request was cancelled; cleared when it is reopened. */
    @Column(name = "cancel_reason", length = CANCEL_REASON_MAX_LENGTH)
    private String cancelReason;

    /**
     * When the request was raised, by the server clock; null on a request on file.
     */
    @Column(name = "raised_at")
    private Timestamp raisedAt;

    /**
     * The signed-in user who raised the request, as a system user id; null on a
     * request on file.
     */
    @Column(name = "raised_by", precision = 10, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String raisedBy;

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public RequestType getType() {
        return type;
    }

    public void setType(RequestType type) {
        this.type = type;
    }

    /**
     * What the request asks the bench to do, as the pathologist wrote it or, for a
     * request on file, the name it was picked by. A pick whose value names no
     * dictionary entry is shown as stored, so a value that is not an id cannot fail
     * the read.
     */
    public String instruction(DictionaryService dictionaryService) {
        if (type != RequestType.DICTIONARY || !StringUtils.isNumeric(value)) {
            return value;
        }
        return DictionaryEntryNames.localizedNameOf(dictionaryService, value).orElse(value);
    }

    public RequestKind getKind() {
        return kind;
    }

    public void setKind(RequestKind kind) {
        this.kind = kind;
    }

    public Integer getTargetBlockId() {
        return targetBlockId;
    }

    public void setTargetBlockId(Integer targetBlockId) {
        this.targetBlockId = targetBlockId;
    }

    public Integer getTargetSlideId() {
        return targetSlideId;
    }

    public void setTargetSlideId(Integer targetSlideId) {
        this.targetSlideId = targetSlideId;
    }

    public String getStainId() {
        return stainId;
    }

    public void setStainId(String stainId) {
        this.stainId = stainId;
    }

    public RequestPriority getPriority() {
        return priority;
    }

    public void setPriority(RequestPriority priority) {
        this.priority = priority;
    }

    public Integer getResultBlockId() {
        return resultBlockId;
    }

    public void setResultBlockId(Integer resultBlockId) {
        this.resultBlockId = resultBlockId;
    }

    public Integer getResultSlideId() {
        return resultSlideId;
    }

    public void setResultSlideId(Integer resultSlideId) {
        this.resultSlideId = resultSlideId;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public Timestamp getRaisedAt() {
        return raisedAt;
    }

    public void setRaisedAt(Timestamp raisedAt) {
        this.raisedAt = raisedAt;
    }

    public String getRaisedBy() {
        return raisedBy;
    }

    public void setRaisedBy(String raisedBy) {
        this.raisedBy = raisedBy;
    }
}
