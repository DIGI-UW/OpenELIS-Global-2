package org.openelisglobal.notification.valueholder;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.notification.valueholder.NotificationConfigOption.NotificationMethod;
import org.openelisglobal.notification.valueholder.NotificationConfigOption.NotificationNature;
import org.openelisglobal.notification.valueholder.NotificationConfigOption.NotificationPersonType;

@Entity
@Table(name = "analysis_notification_config")
public class AnalysisNotificationConfig extends NotificationConfig<Analysis> {

    private static final long serialVersionUID = -3516692281036957868L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "analysis_notification_config_generator")
    @SequenceGenerator(name = "analysis_notification_config_generator", sequenceName = "analysis_notification_config_seq", allocationSize = 1)
    private Integer id;

    @Valid
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "analysis_id", referencedColumnName = "id")
    @JsonIgnore
    private Analysis analysis;

    @Valid
    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.DETACH, CascadeType.REFRESH })
    @JoinColumn(name = "default_payload_template_id", referencedColumnName = "id")
    @JsonIgnore
    private NotificationPayloadTemplate defaultPayloadTemplate;

    @Valid
    @OneToMany(cascade = { CascadeType.ALL }, fetch = FetchType.EAGER)
    @JoinTable(name = "analysis_notification_config_config_option", joinColumns = @JoinColumn(name = "analysis_notification_config_id"), inverseJoinColumns = @JoinColumn(name = "notification_config_option_id"))
    @JsonIgnore
    private List<NotificationConfigOption> options;

    public AnalysisNotificationConfig() {
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public Analysis getAnalysis() {
        return analysis;
    }

    @JsonGetter
    public String getAnalysisId() {
        return analysis.getId();
    }

    public void setAnalysis(Analysis analysis) {
        this.analysis = analysis;
    }

    public NotificationPayloadTemplate getDefaultPayloadTemplate() {
        return defaultPayloadTemplate;
    }

    public void setDefaultPayloadTemplate(NotificationPayloadTemplate defaultPayloadTemplate) {
        this.defaultPayloadTemplate = defaultPayloadTemplate;
    }

    public List<NotificationConfigOption> getOptions() {
        return options;
    }

    public void setOptions(List<NotificationConfigOption> options) {
        this.options = options;
    }

    public NotificationConfigOption getOptionFor(NotificationNature nature, NotificationMethod methodType,
            NotificationPersonType personType) {
        if (options == null) {
            options = new ArrayList<>();
        }
        return options.stream().filter(opt -> opt.getNotificationMethod().equals(methodType)
                && opt.getNotificationPersonType().equals(personType) && opt.getNotificationNature().equals(nature))
                .findAny().orElseGet(() -> getAndAddNewConfigOption(nature, methodType, personType));
    }

    private NotificationConfigOption getAndAddNewConfigOption(NotificationNature nature, NotificationMethod methodType,
            NotificationPersonType personType) {
        NotificationConfigOption configOption = new NotificationConfigOption(methodType, personType, nature, false);
        options.add(configOption);
        return configOption;
    }

    // used in jsps

    public NotificationConfigOption getPatientEmail() {
        return getOptionFor(NotificationNature.RESULT_VALIDATION, NotificationMethod.EMAIL,
                NotificationPersonType.PATIENT);
    }

    public NotificationConfigOption getPatientSMS() {
        return getOptionFor(NotificationNature.RESULT_VALIDATION, NotificationMethod.SMS,
                NotificationPersonType.PATIENT);
    }

    public NotificationConfigOption getProviderEmail() {
        return getOptionFor(NotificationNature.RESULT_VALIDATION, NotificationMethod.EMAIL,
                NotificationPersonType.PROVIDER);
    }

    public NotificationConfigOption getProviderSMS() {
        return getOptionFor(NotificationNature.RESULT_VALIDATION, NotificationMethod.SMS,
                NotificationPersonType.PROVIDER);
    }
}
