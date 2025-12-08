package org.openelisglobal.externalconnections.controller;

import jakarta.validation.Valid;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.externalconnections.form.ExternalConnectionForm;
import org.openelisglobal.externalconnections.service.ExternalConnectionAuthenticationDataService;
import org.openelisglobal.externalconnections.service.ExternalConnectionContactService;
import org.openelisglobal.externalconnections.service.ExternalConnectionService;
import org.openelisglobal.externalconnections.valueholder.BasicAuthenticationData;
import org.openelisglobal.externalconnections.valueholder.CertificateAuthenticationData;
import org.openelisglobal.externalconnections.valueholder.ExternalConnection;
import org.openelisglobal.externalconnections.valueholder.ExternalConnection.AuthType;
import org.openelisglobal.externalconnections.valueholder.ExternalConnection.ProgrammedConnection;
import org.openelisglobal.externalconnections.valueholder.ExternalConnectionAuthenticationData;
import org.openelisglobal.externalconnections.valueholder.ExternalConnectionContact;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.security.certs.service.TruststoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ExternalConnectionController extends BaseController {

    private static final String[] ALLOWED_FIELDS = new String[] { "externalConnection.id", "externalConnection.active",
            "externalConnection.lastupdated", "externalConnection.programmedConnection",
            "externalConnection.activeAuthenticationType", "externalConnection.uri",
            //
            "externalConnection.descriptionLocalization.lastupdated", "externalConnection.descriptionLocalization.id",
            "externalConnection.descriptionLocalization.localizedValue",
            //
            "externalConnection.nameLocalization.lastupdated", "externalConnection.nameLocalization.id",
            "externalConnection.nameLocalization.localizedValue",
            //
            "externalConnectionContacts*.id", "externalConnectionContacts*.lastupdated",
            //
            "externalConnectionContacts*.person.id", "externalConnectionContacts*.person.lastupdated",
            "externalConnectionContacts*.person.lastName", "externalConnectionContacts*.person.firstName",
            "externalConnectionContacts*.person.primaryPhone", "externalConnectionContacts*.person.email",
            //
            "certificateAuthenticationData.certificate", "certificateAuthenticationData.id",
            "certificateAuthenticationData.lastupdated",
            //
            "basicAuthenticationData.username", "basicAuthenticationData.password", "basicAuthenticationData.id",
            "basicAuthenticationData.lastupdated", };

    @Autowired
    private ExternalConnectionService externalConnectionService;
    @Autowired
    private ExternalConnectionContactService externalConnectionContactService;

    @Autowired
    private ExternalConnectionAuthenticationDataService externalConnectionAuthenticationDataService;

    @Autowired
    private TruststoreService truststoreService;

    @InitBinder
    public void initBinder(WebDataBinder webdataBinder) {
        webdataBinder.setAllowedFields(ALLOWED_FIELDS);
    }

    @GetMapping(value = "/ExternalConnection")
    public ModelAndView viewExternalConnection(
            @RequestParam(value = ID, required = false) Integer externalConnectionId) {
        ExternalConnectionForm form = new ExternalConnectionForm();
        form.setCancelAction("ExternalConnectionsMenu");
        form.setCancelMethod(RequestMethod.GET);

        form.setFormAction("ExternalConnection");
        form.setFormName("ExternalConnectionForm");
        form.setFormMethod(RequestMethod.POST);

        if (null == externalConnectionId || 0 == externalConnectionId) {
            prepareEmptyForm(form);
        } else {
            fillForm(externalConnectionId, form);
        }

        request.getSession().setAttribute(SAVE_DISABLED, TRUE);
        return findForward(FWD_SUCCESS, form);
    }

    @PostMapping(value = "/ExternalConnection")
    public ModelAndView addEditExternalConnection(@Valid @ModelAttribute("form") ExternalConnectionForm form,
            @RequestParam(value = ID, required = false) Integer externalConnectionId)
            throws CertificateException, KeyStoreException, NoSuchAlgorithmException, IOException {

        List<ExternalConnectionContact> externalConnectionContacts = form.getExternalConnectionContacts() == null
                ? new ArrayList<>()
                : form.getExternalConnectionContacts();
        ExternalConnection externalConnection = form.getExternalConnection();
        Map<AuthType, ExternalConnectionAuthenticationData> externalConnectionAuthData = new HashMap<>();
        if (form.getBasicAuthenticationData() != null) {
            externalConnectionAuthData.put(AuthType.BASIC, form.getBasicAuthenticationData());
        }
        if (form.getCertificateAuthenticationData() != null) {
            externalConnectionAuthData.put(AuthType.CERTIFICATE, form.getCertificateAuthenticationData());
            loadCertificateIntoTruststore(form.getExternalConnection().getNameLocalization().getLocalizedValue(),
                    form.getCertificateAuthenticationData().getCertificate());
        }

        if (null == externalConnectionId || 0 == externalConnectionId) {
            externalConnectionService.createNewExternalConnection(externalConnectionAuthData,
                    externalConnectionContacts, externalConnection);
        } else {
            externalConnectionService.updateExternalConnection(externalConnectionAuthData, externalConnectionContacts,
                    externalConnection);
        }
        ConfigurationProperties.loadDBValuesIntoConfiguration();
        return findForward(FWD_SUCCESS_INSERT, form);
    }

    private void loadCertificateIntoTruststore(String connectionName, MultipartFile certificateFile)
            throws CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException {
        if (certificateFile != null && !certificateFile.isEmpty()) {
            final CertificateFactory cf = CertificateFactory.getInstance("X.509");
            final InputStream is = certificateFile.getInputStream();
            final Certificate certificate = cf.generateCertificate(is);
            truststoreService.addTrustedCert(connectionName, certificate);
        }
    }

    private void fillForm(Integer externalConnectionId, @ModelAttribute("form") ExternalConnectionForm form) {
        request.setAttribute(IActionConstants.PAGE_SUBTITLE_KEY,
                MessageUtil.getMessage("externalconnections.edit.title"));
        form.setAuthenticationTypes(Arrays.asList(AuthType.values()));
        form.setProgrammedConnections(Arrays.asList(ProgrammedConnection.values()));

        form.setExternalConnection(externalConnectionService.get(externalConnectionId));
        form.setExternalConnectionContacts(
                externalConnectionContactService.getAllMatching("externalConnection.id", externalConnectionId));
        Map<AuthType, ExternalConnectionAuthenticationData> externalConnectionAuthData = externalConnectionAuthenticationDataService
                .getForExternalConnection(externalConnectionId);
        form.setBasicAuthenticationData((BasicAuthenticationData) externalConnectionAuthData.get(AuthType.BASIC));
        form.setCertificateAuthenticationData(
                (CertificateAuthenticationData) externalConnectionAuthData.get(AuthType.CERTIFICATE));
    }

    private void prepareEmptyForm(ExternalConnectionForm form) {
        request.setAttribute(IActionConstants.PAGE_SUBTITLE_KEY,
                MessageUtil.getMessage("externalconnections.add.title"));
        form.setAuthenticationTypes(Arrays.asList(AuthType.values()));
        form.setProgrammedConnections(Arrays.asList(ProgrammedConnection.values()));

        form.setExternalConnection(new ExternalConnection());
        form.setBasicAuthenticationData(new BasicAuthenticationData());
        form.setCertificateAuthenticationData(new CertificateAuthenticationData());
        form.setExternalConnectionContacts(new ArrayList<>());
    }

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "externalConnectionDefinition";
        } else if (FWD_SUCCESS_INSERT.equals(forward)) {
            return "redirect:/ExternalConnectionsMenu";
        }
        return "PageNotFound";
    }

    @Override
    protected String getPageTitleKey() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected String getPageSubtitleKey() {
        // TODO Auto-generated method stub
        return null;
    }
}
