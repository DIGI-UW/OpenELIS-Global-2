package org.openelisglobal.view;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement(name = "tiles-definitions")
@XmlAccessorType(XmlAccessType.FIELD)
public class TilesDefinitions {

    @XmlElement(name = "definition")
    private List<Definition> definitions;

    public List<Definition> getDefinitions() {
        return definitions;
    }

    public void setDefinition(List<Definition> definitions) {
        this.definitions = definitions;
    }

    public Definition getDefinitionByName(String name) {
        return definitions.stream().filter(x -> x.getName().equals(name)).findFirst().orElseThrow();
    }
}
