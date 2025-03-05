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
import org.openelisglobal.notification.valueholder.NotificationConfigOption.NotificationMethod;
import org.openelisglobal.notification.valueholder.NotificationConfigOption.NotificationNature;
import org.openelisglobal.notification.valueholder.NotificationConfigOption.NotificationPersonType;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;

@Entity
@Table(name = "test_notification_config")
public class TestNotificationConfig extends NotificationConfig<Test> {

    private static final long serialVersionUID = -3516692281036957868L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "test_notification_config_generator")
    @SequenceGenerator(name = "test_notification_config_generator", sequenceName = "test_notification_config_seq", allocationSize = 1)
    private Integer id;

    @Valid
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "test_id", referencedColumnName = "id")
    @JsonIgnore
    private Test test;

    @Valid
    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.DETACH, CascadeType.REFRESH })
    @JoinColumn(name = "default_payload_template_id", referencedColumnName = "id")
    @JsonIgnore
    private NotificationPayloadTemplate defaultPayloadTemplate;

    // could implement defaults for individual method types and person types types
    // as well
    // @OneToMany
    // @JoinTable(name = "test_notification_default_method", joinColumns =
    // @JoinColumn(name =
    // "test_notification_config_id"), //
    // inverseJoinColumns = @JoinColumn(name = "notification_payload_template_id"))
    // //
    // @MapKeyColumn(name = "notification_method")
    // private Map<NotificationMethod, NotificationPayloadTemplate>
    // defaultMethodPayloadTemplate;
    //
    // @OneToMany
    // @JoinTable(name = "test_notification_default_person_type", joinColumns =
    // @JoinColumn(name =
    // "test_notification_config_id"), //
    // inverseJoinColumns = @JoinColumn(name = "notification_payload_template_id"))
    // //
    // @MapKeyColumn(name = "notification_person_type")
    // private Map<NotificationPersonType, NotificationPayloadTemplate>
    // defaultPersonPayloadTemplate;

    @Valid
    @OneToMany(cascade = { CascadeType.ALL }, fetch = FetchType.EAGER)
    @JoinTable(name = "test_notification_config_config_option", joinColumns = @JoinColumn(name = "test_notification_config_id"), inverseJoinColumns = @JoinColumn(name = "notification_config_option_id"))
    @JsonIgnore
    private List<NotificationConfigOption> options;

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public Test getTest() {
        return test;
    }

    @JsonGetter
    public String getTestId() {
        return test.getId();
    }

    public void setTestId(String testId) {
        this.test = SpringContext.getBean(TestService.class).get(testId);
    }

    public void setTest(Test test) {
        this.test = test;
    }

    public NotificationPayloadTemplate getDefaultPayloadTemplate() {
        return defaultPayloadTemplate;
    }

    public void setDefaultPayloadTemplate(NotificationPayloadTemplate defaultPayloadTemplate) {
        this.defaultPayloadTemplate = defaultPayloadTemplate;
    }

    @Override
    public List<NotificationConfigOption> getOptions() {
        if (options == null) {
            options = new ArrayList<>();
        }
        return options;
    }

    @Override
    public void setOptions(List<NotificationConfigOption> options) {
        this.options = options;
    }

    @Override
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

    @Override
    public NotificationConfigOption getPatientEmail() {
        return getOptionFor(NotificationNature.RESULT_VALIDATION, NotificationMethod.EMAIL,
                NotificationPersonType.PATIENT);
    }

    @Override
    public NotificationConfigOption getPatientSMS() {
        return getOptionFor(NotificationNature.RESULT_VALIDATION, NotificationMethod.SMS,
                NotificationPersonType.PATIENT);
    }

    @Override
    public NotificationConfigOption getProviderEmail() {
        return getOptionFor(NotificationNature.RESULT_VALIDATION, NotificationMethod.EMAIL,
                NotificationPersonType.PROVIDER);
    }

    @Override
    public NotificationConfigOption getProviderSMS() {
        return getOptionFor(NotificationNature.RESULT_VALIDATION, NotificationMethod.SMS,
                NotificationPersonType.PROVIDER);
    }

    public void setPatientEmail(NotificationConfigOption option) {
        getOptions().add(option);
    }

    public void setPatientSMS(NotificationConfigOption option) {
        getOptions().add(option);
    }

    public void setProviderEmail(NotificationConfigOption option) {
        getOptions().add(option);
    }

    public void setProviderSMS(NotificationConfigOption option) {
        getOptions().add(option);
    }
}
