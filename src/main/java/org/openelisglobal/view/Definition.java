package org.openelisglobal.view;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement(name = "definition")
@XmlAccessorType(XmlAccessType.FIELD)
public class Definition {

    @XmlAttribute(name = "name")
    private String name;

    @XmlAttribute(name = "extends")
    private String extension;

    @XmlAttribute(name = "template")
    private String template;

    @XmlElement(name = "put-attribute")
    private List<PutAttribute> putAttributes;

    public Definition() {
    }

    public Definition(String name, String extension, String template, List<PutAttribute> putAttributes) {
        this.name = name;
        this.extension = extension;
        this.template = template;
        this.putAttributes = putAttributes;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getExtends() {
        return extension;
    }

    public void setExtends(String extension) {
        this.extension = extension;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public List<PutAttribute> getPutAttributes() {
        return putAttributes;
    }

    public void setPutAttributes(List<PutAttribute> putAttributes) {
        this.putAttributes = putAttributes;
    }
}
