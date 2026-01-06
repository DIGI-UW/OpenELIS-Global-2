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

    private boolean collectionDateCheck;

    private boolean collectedByCheck;

    private boolean testsCheck;

    private boolean patientSexCheck;

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

    public boolean getCollectionDateCheck() {
        return collectionDateCheck;
    }

    public void setCollectionDateCheck(boolean collectionDateCheck) {
        this.collectionDateCheck = collectionDateCheck;
    }

    public boolean getTestsCheck() {
        return testsCheck;
    }

    public void setTestsCheck(boolean testsCheck) {
        this.testsCheck = testsCheck;
    }

    public boolean getPatientSexCheck() {
        return patientSexCheck;
    }

    public void setPatientSexCheck(boolean patientSexCheck) {
        this.patientSexCheck = patientSexCheck;
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

    public boolean getCollectedByCheck() {
        return collectedByCheck;
    }

    public void setCollectedByCheck(boolean collectedByCheck) {
        this.collectedByCheck = collectedByCheck;
    }
}
