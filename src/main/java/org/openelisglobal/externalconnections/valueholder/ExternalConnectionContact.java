package org.openelisglobal.externalconnections.valueholder;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.person.valueholder.Person;

@Entity
@Table(name = "external_connection_contact")
public class ExternalConnectionContact extends BaseObject<Integer> {

    private static final long serialVersionUID = -1942060576531727396L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "external_connection_contact_generator")
    @SequenceGenerator(name = "external_connection_contact_generator", sequenceName = "external_connection_contact_seq", allocationSize = 1)
    private Integer id;

    @Valid
    @ManyToOne
    @JoinColumn(name = "external_connection_id", referencedColumnName = "id")
    private ExternalConnection externalConnection;

    @Valid
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "person_id", referencedColumnName = "id")
    private Person person;

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public ExternalConnection getExternalConnection() {
        return externalConnection;
    }

    public void setExternalConnection(ExternalConnection externalConnection) {
        this.externalConnection = externalConnection;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }
}
