package org.openelisglobal.program.valueholder.pathology;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "pathology_request")
public class PathologyRequest extends BaseObject<Integer> {

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

    private String value;

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
}
