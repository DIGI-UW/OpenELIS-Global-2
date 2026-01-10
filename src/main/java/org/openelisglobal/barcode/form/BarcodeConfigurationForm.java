package org.openelisglobal.barcode.form;

import org.hibernate.validator.constraints.Range;
import org.openelisglobal.common.form.BaseForm;
import org.openelisglobal.common.validator.ValidationHelper;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class BarcodeConfigurationForm extends BaseForm {

    @Range(min = 0, max = 2000)
    private int numMaxOrderLabels;

    @Range(min = 0, max = 2000)
    private int numMaxSpecimenLabels;

    @Range(min = 0, max = 2000)
    private int numMaxAliquotLabels;

    @Range(min = 0, max = 2000)
    private int numMaxSlideLabels;

    @Range(min = 0, max = 2000)
    private int numMaxBlockLabels;

    @Range(min = 0, max = 2000)
    private int numMaxFreezerLabels;

    @Range(min = 0, max = 2000)
    private int numDefaultOrderLabels;

    @Range(min = 0, max = 2000)
    private int numDefaultSpecimenLabels;

    @Range(min = 0, max = 2000)
    private int numDefaultAliquotLabels;

    @Range(min = 0, max = 2000)
    private int numDefaultSlideLabels;

    @Range(min = 0, max = 2000)
    private int numDefaultBlockLabels;

    @Range(min = 0, max = 2000)
    private int numDefaultFreezerLabels;

    @Range(min = 0, max = 1000)
    private float heightOrderLabels;

    @Range(min = 0, max = 1000)
    private float widthOrderLabels;

    @Range(min = 0, max = 1000)
    private float heightSpecimenLabels;

    @Range(min = 0, max = 1000)
    private float widthSpecimenLabels;

    @Range(min = 0, max = 1000)
    private float heightBlockLabels;

    @Range(min = 0, max = 1000)
    private float widthBlockLabels;

    @Range(min = 0, max = 1000)
    private float heightSlideLabels;

    @Range(min = 0, max = 1000)
    private float widthSlideLabels;

    @Range(min = 0, max = 1000)
    private float heightFreezerLabels;

    @Range(min = 0, max = 1000)
    private float widthFreezerLabels;

    private boolean orderPatientDobCheck;
    private boolean orderPatientIdCheck;
    private boolean orderPatientNameCheck;
    private boolean orderSiteIdCheck;
    private boolean specimenPatientDobCheck;
    private boolean specimenPatientIdCheck;
    private boolean specimenPatientNameCheck;
    private boolean specimenCollectionDateCheck;
    private boolean specimenCollectedByCheck;
    private boolean specimenTestsCheck;
    private boolean specimenPatientSexCheck;
    private boolean slidePatientIdCheck;
    private boolean slideSlideIdCheck;
    private boolean slideStainTypeCheck;
    private boolean slideBlockIdCheck;
    private boolean slideCaseNumberCheck;
    private boolean blockPatientIdCheck;
    private boolean blockBlockIdCheck;
    private boolean blockSpecimenTypeCheck;
    private boolean blockCaseNumberCheck;
    private boolean freezerPatientIdCheck;
    private boolean freezerStorageLocationCheck;
    private boolean freezerSpecimenTypeCheck;
    private boolean freezerCollectionDateCheck;
    private boolean freezerExpiryDateCheck;

    private boolean prePrintDontUseAltAccession;

    @Pattern(regexp = ValidationHelper.ALPHA_NUM_REGEX)
    @Size(min = 4, max = 4)
    private String prePrintAltAccessionPrefix;

    // for display/validation logic only
    private String sitePrefix;

    public BarcodeConfigurationForm() {
        setFormName("BarcodeConfigurationForm");
    }

    public int getNumMaxOrderLabels() {
        return numMaxOrderLabels;
    }

    public void setNumMaxOrderLabels(int numMaxOrderLabels) {
        this.numMaxOrderLabels = numMaxOrderLabels;
    }

    public int getNumMaxSpecimenLabels() {
        return numMaxSpecimenLabels;
    }

    public void setNumMaxSpecimenLabels(int numMaxSpecimenLabels) {
        this.numMaxSpecimenLabels = numMaxSpecimenLabels;
    }

    public int getNumMaxAliquotLabels() {
        return numMaxAliquotLabels;
    }

    public void setNumMaxAliquotLabels(int numMaxAliquotLabels) {
        this.numMaxAliquotLabels = numMaxAliquotLabels;
    }

    public int getNumMaxSlideLabels() {
        return numMaxSlideLabels;
    }

    public void setNumMaxSlideLabels(int numMaxSlideLabels) {
        this.numMaxSlideLabels = numMaxSlideLabels;
    }

    public int getNumMaxBlockLabels() {
        return numMaxBlockLabels;
    }

    public void setNumMaxBlockLabels(int numMaxBlockLabels) {
        this.numMaxBlockLabels = numMaxBlockLabels;
    }

    public int getNumMaxFreezerLabels() {
        return numMaxFreezerLabels;
    }

    public void setNumMaxFreezerLabels(int numMaxFreezerLabels) {
        this.numMaxFreezerLabels = numMaxFreezerLabels;
    }

    public float getHeightOrderLabels() {
        return heightOrderLabels;
    }

    public void setHeightOrderLabels(float heightOrderLabels) {
        this.heightOrderLabels = heightOrderLabels;
    }

    public float getWidthOrderLabels() {
        return widthOrderLabels;
    }

    public void setWidthOrderLabels(float widthOrderLabels) {
        this.widthOrderLabels = widthOrderLabels;
    }

    public float getHeightSpecimenLabels() {
        return heightSpecimenLabels;
    }

    public void setHeightSpecimenLabels(float heightSpecimenLabels) {
        this.heightSpecimenLabels = heightSpecimenLabels;
    }

    public float getWidthSpecimenLabels() {
        return widthSpecimenLabels;
    }

    public void setWidthSpecimenLabels(float widthSpecimenLabels) {
        this.widthSpecimenLabels = widthSpecimenLabels;
    }

    public float getHeightBlockLabels() {
        return heightBlockLabels;
    }

    public void setHeightBlockLabels(float heightBlockLabels) {
        this.heightBlockLabels = heightBlockLabels;
    }

    public float getWidthBlockLabels() {
        return widthBlockLabels;
    }

    public void setWidthBlockLabels(float widthBlockLabels) {
        this.widthBlockLabels = widthBlockLabels;
    }

    public float getHeightSlideLabels() {
        return heightSlideLabels;
    }

    public void setHeightSlideLabels(float heightSlideLabels) {
        this.heightSlideLabels = heightSlideLabels;
    }

    public float getWidthSlideLabels() {
        return widthSlideLabels;
    }

    public void setWidthSlideLabels(float widthSlideLabels) {
        this.widthSlideLabels = widthSlideLabels;
    }

    public float getHeightFreezerLabels() {
        return heightFreezerLabels;
    }

    public void setHeightFreezerLabels(float heightFreezerLabels) {
        this.heightFreezerLabels = heightFreezerLabels;
    }

    public float getWidthFreezerLabels() {
        return widthFreezerLabels;
    }

    public void setWidthFreezerLabels(float widthFreezerLabels) {
        this.widthFreezerLabels = widthFreezerLabels;
    }

    public boolean getPrePrintDontUseAltAccession() {
        return prePrintDontUseAltAccession;
    }

    public void setPrePrintDontUseAltAccession(boolean prePrintDontUseAltAccession) {
        this.prePrintDontUseAltAccession = prePrintDontUseAltAccession;
    }

    public String getPrePrintAltAccessionPrefix() {
        return prePrintAltAccessionPrefix;
    }

    public void setPrePrintAltAccessionPrefix(String prePrintAltAccessionPrefix) {
        this.prePrintAltAccessionPrefix = prePrintAltAccessionPrefix;
    }

    public String getSitePrefix() {
        return sitePrefix;
    }

    public void setSitePrefix(String sitePrefix) {
        this.sitePrefix = sitePrefix;
    }

    public int getNumDefaultOrderLabels() {
        return numDefaultOrderLabels;
    }

    public void setNumDefaultOrderLabels(int numDefaultOrderLabels) {
        this.numDefaultOrderLabels = numDefaultOrderLabels;
    }

    public int getNumDefaultSpecimenLabels() {
        return numDefaultSpecimenLabels;
    }

    public void setNumDefaultSpecimenLabels(int numDefaultSpecimenLabels) {
        this.numDefaultSpecimenLabels = numDefaultSpecimenLabels;
    }

    public int getNumDefaultAliquotLabels() {
        return numDefaultAliquotLabels;
    }

    public void setNumDefaultAliquotLabels(int numDefaultAliquotLabels) {
        this.numDefaultAliquotLabels = numDefaultAliquotLabels;
    }

    public int getNumDefaultSlideLabels() {
        return numDefaultSlideLabels;
    }

    public void setNumDefaultSlideLabels(int numDefaultSlideLabels) {
        this.numDefaultSlideLabels = numDefaultSlideLabels;
    }

    public int getNumDefaultBlockLabels() {
        return numDefaultBlockLabels;
    }

    public void setNumDefaultBlockLabels(int numDefaultBlockLabels) {
        this.numDefaultBlockLabels = numDefaultBlockLabels;
    }

    public int getNumDefaultFreezerLabels() {
        return numDefaultFreezerLabels;
    }

    public void setNumDefaultFreezerLabels(int numDefaultFreezerLabels) {
        this.numDefaultFreezerLabels = numDefaultFreezerLabels;
    }

    public boolean getOrderPatientDobCheck() {
        return orderPatientDobCheck;
    }

    public void setOrderPatientDobCheck(boolean orderPatientDobCheck) {
        this.orderPatientDobCheck = orderPatientDobCheck;
    }

    public boolean getOrderPatientIdCheck() {
        return orderPatientIdCheck;
    }

    public void setOrderPatientIdCheck(boolean orderPatientIdCheck) {
        this.orderPatientIdCheck = orderPatientIdCheck;
    }

    public boolean getOrderPatientNameCheck() {
        return orderPatientNameCheck;
    }

    public void setOrderPatientNameCheck(boolean orderPatientNameCheck) {
        this.orderPatientNameCheck = orderPatientNameCheck;
    }

    public boolean getOrderSiteIdCheck() {
        return orderSiteIdCheck;
    }

    public void setOrderSiteIdCheck(boolean orderSiteIdCheck) {
        this.orderSiteIdCheck = orderSiteIdCheck;
    }

    public boolean getSpecimenPatientDobCheck() {
        return specimenPatientDobCheck;
    }

    public void setSpecimenPatientDobCheck(boolean specimenPatientDobCheck) {
        this.specimenPatientDobCheck = specimenPatientDobCheck;
    }

    public boolean getSpecimenPatientIdCheck() {
        return specimenPatientIdCheck;
    }

    public void setSpecimenPatientIdCheck(boolean specimenPatientIdCheck) {
        this.specimenPatientIdCheck = specimenPatientIdCheck;
    }

    public boolean getSpecimenPatientNameCheck() {
        return specimenPatientNameCheck;
    }

    public void setSpecimenPatientNameCheck(boolean specimenPatientNameCheck) {
        this.specimenPatientNameCheck = specimenPatientNameCheck;
    }

    public boolean getSpecimenCollectionDateCheck() {
        return specimenCollectionDateCheck;
    }

    public void setSpecimenCollectionDateCheck(boolean specimenCollectionDateCheck) {
        this.specimenCollectionDateCheck = specimenCollectionDateCheck;
    }

    public boolean getSpecimenCollectedByCheck() {
        return specimenCollectedByCheck;
    }

    public void setSpecimenCollectedByCheck(boolean specimenCollectedByCheck) {
        this.specimenCollectedByCheck = specimenCollectedByCheck;
    }

    public boolean getSpecimenTestsCheck() {
        return specimenTestsCheck;
    }

    public void setSpecimenTestsCheck(boolean specimenTestsCheck) {
        this.specimenTestsCheck = specimenTestsCheck;
    }

    public boolean getSpecimenPatientSexCheck() {
        return specimenPatientSexCheck;
    }

    public void setSpecimenPatientSexCheck(boolean specimenPatientSexCheck) {
        this.specimenPatientSexCheck = specimenPatientSexCheck;
    }

    public boolean getSlidePatientIdCheck() {
        return slidePatientIdCheck;
    }

    public void setSlidePatientIdCheck(boolean slidePatientIdCheck) {
        this.slidePatientIdCheck = slidePatientIdCheck;
    }

    public boolean getSlideSlideIdCheck() {
        return slideSlideIdCheck;
    }

    public void setSlideSlideIdCheck(boolean slideSlideIdCheck) {
        this.slideSlideIdCheck = slideSlideIdCheck;
    }

    public boolean getSlideStainTypeCheck() {
        return slideStainTypeCheck;
    }

    public void setSlideStainTypeCheck(boolean slideStainTypeCheck) {
        this.slideStainTypeCheck = slideStainTypeCheck;
    }

    public boolean getSlideBlockIdCheck() {
        return slideBlockIdCheck;
    }

    public void setSlideBlockIdCheck(boolean slideBlockIdCheck) {
        this.slideBlockIdCheck = slideBlockIdCheck;
    }

    public boolean getSlideCaseNumberCheck() {
        return slideCaseNumberCheck;
    }

    public void setSlideCaseNumberCheck(boolean slideCaseNumberCheck) {
        this.slideCaseNumberCheck = slideCaseNumberCheck;
    }

    public boolean getBlockPatientIdCheck() {
        return blockPatientIdCheck;
    }

    public void setBlockPatientIdCheck(boolean blockPatientIdCheck) {
        this.blockPatientIdCheck = blockPatientIdCheck;
    }

    public boolean getBlockBlockIdCheck() {
        return blockBlockIdCheck;
    }

    public void setBlockBlockIdCheck(boolean blockBlockIdCheck) {
        this.blockBlockIdCheck = blockBlockIdCheck;
    }

    public boolean getBlockSpecimenTypeCheck() {
        return blockSpecimenTypeCheck;
    }

    public void setBlockSpecimenTypeCheck(boolean blockSpecimenTypeCheck) {
        this.blockSpecimenTypeCheck = blockSpecimenTypeCheck;
    }

    public boolean getBlockCaseNumberCheck() {
        return blockCaseNumberCheck;
    }

    public void setBlockCaseNumberCheck(boolean blockCaseNumberCheck) {
        this.blockCaseNumberCheck = blockCaseNumberCheck;
    }

    public boolean getFreezerPatientIdCheck() {
        return freezerPatientIdCheck;
    }

    public void setFreezerPatientIdCheck(boolean freezerPatientIdCheck) {
        this.freezerPatientIdCheck = freezerPatientIdCheck;
    }

    public boolean getFreezerStorageLocationCheck() {
        return freezerStorageLocationCheck;
    }

    public void setFreezerStorageLocationCheck(boolean freezerStorageLocationCheck) {
        this.freezerStorageLocationCheck = freezerStorageLocationCheck;
    }

    public boolean getFreezerSpecimenTypeCheck() {
        return freezerSpecimenTypeCheck;
    }

    public void setFreezerSpecimenTypeCheck(boolean freezerSpecimenTypeCheck) {
        this.freezerSpecimenTypeCheck = freezerSpecimenTypeCheck;
    }

    public boolean getFreezerCollectionDateCheck() {
        return freezerCollectionDateCheck;
    }

    public void setFreezerCollectionDateCheck(boolean freezerCollectionDateCheck) {
        this.freezerCollectionDateCheck = freezerCollectionDateCheck;
    }

    public boolean getFreezerExpiryDateCheck() {
        return freezerExpiryDateCheck;
    }

    public void setFreezerExpiryDateCheck(boolean freezerExpiryDateCheck) {
        this.freezerExpiryDateCheck = freezerExpiryDateCheck;
    }
}
