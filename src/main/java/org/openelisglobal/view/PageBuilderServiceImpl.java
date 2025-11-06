package org.openelisglobal.view;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;
import org.apache.commons.validator.GenericValidator;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

@Service
public class PageBuilderServiceImpl implements PageBuilderService {

    private TilesDefinitions definitionsByName;

    @PostConstruct
    public void readDefinitions() throws JAXBException, ParserConfigurationException, SAXException, IOException {
        JAXBContext jaxbContext = JAXBContext.newInstance(TilesDefinitions.class);

        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        spf.setFeature("http://xml.org/sax/features/validation", false);

        XMLReader xmlReader = spf.newSAXParser().getXMLReader();

        ClassPathResource resource = new ClassPathResource("tiles/tiles-defs.xml");
        SAXSource source = new SAXSource(xmlReader, new InputSource(resource.getInputStream()));

        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

        definitionsByName = (TilesDefinitions) unmarshaller.unmarshal(source);
    }

    @Override
    public String setupJSPPage(String viewName, HttpServletRequest request) throws ViewConfigurationException {
        Definition definition = definitionsByName.getDefinitionByName(viewName);
        String template;
        if (definitionExtends(definition)) {
            template = setupJSPPage(definition.getExtends(), request);
        } else {
            if (GenericValidator.isBlankOrNull(definition.getTemplate())) {
                throw new ViewConfigurationException("definition doesn't extend another or have a template");
            }
            template = definition.getTemplate();
        }
        for (PutAttribute i : definition.getPutAttributes()) {
            if (i.getValue().endsWith(".jsp")) {
                request.setAttribute(i.getName(), i.getValue());
            } else {
                Definition subDefinition = definitionsByName.getDefinitionByName(i.getValue());
                request.setAttribute(i.getName(), setupJSPPage(subDefinition.getName(), request));
            }
        }
        if (definitionTemplated(definition)) {
            return definition.getTemplate();
        } else {
            return template;
        }
    }

    private boolean definitionTemplated(Definition definition) {
        return !GenericValidator.isBlankOrNull(definition.getTemplate());
    }

    private boolean definitionExtends(Definition definition) {
        return !GenericValidator.isBlankOrNull(definition.getExtends());
    }
}
