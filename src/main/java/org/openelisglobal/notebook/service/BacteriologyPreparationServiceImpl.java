package org.openelisglobal.notebook.service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.hibernate.StaleObjectStateException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.notebook.valueholder.NoteBook;
import org.openelisglobal.notebook.valueholder.NoteBookPage;
import org.openelisglobal.notebook.valueholder.NotebookEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of BacteriologyPreparationService. Manages culture media,
 * biochemical media, and antibiotic IQC preparations for bacteriology
 * laboratory workflows.
 *
 * Preparations are stored in the Processing QC page's data field for the
 * notebook associated with the entry.
 */
@Service
public class BacteriologyPreparationServiceImpl implements BacteriologyPreparationService {

    // Processing QC page is at order 4 in Bacteriology workflow
    private static final int PROCESSING_QC_PAGE_ORDER = 4;
    private static final String PROCESSING_QC_PAGE_TITLE = "Processing & Quality Control";
    private static final String CULTURE_MEDIA_KEY = "cultureMediaPreparations";
    private static final String BIOCHEMICAL_MEDIA_KEY = "biochemicalMediaPreparations";
    private static final String ANTIBIOTIC_IQC_KEY = "antibioticIqcPreparations";

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 100;

    @Autowired
    private NotebookEntryService notebookEntryService;

    @Autowired
    private NoteBookPageService noteBookPageService;

    /**
     * Get the Processing QC page ID for the notebook associated with an entry.
     * Returns just the page ID to avoid stale entity issues. IMPORTANT: Uses scalar
     * queries to avoid loading page entities into the Hibernate session, completely
     * preventing stale entity issues.
     */
    private Integer getProcessingQCPageId(Integer entryId) {
        Optional<NotebookEntry> optEntry = notebookEntryService.getMatch("id", entryId);
        if (optEntry.isEmpty()) {
            return null;
        }

        NotebookEntry entry = optEntry.get();
        NoteBook notebook = entry.getNotebook();
        if (notebook == null) {
            return null;
        }

        Integer notebookId = notebook.getId();

        // Use scalar query to get page ID by order (more reliable) - no entity loading
        Integer pageId = noteBookPageService.getPageIdByNotebookIdAndOrder(notebookId, PROCESSING_QC_PAGE_ORDER);
        if (pageId != null) {
            return pageId;
        }

        // Fallback to title pattern matching - also no entity loading
        return noteBookPageService.getPageIdByNotebookIdAndTitlePattern(notebookId, "Processing");
    }

    /**
     * Get a fresh copy of the Processing QC page by ID.
     */
    private NoteBookPage getFreshPage(Integer pageId) {
        return noteBookPageService.get(pageId);
    }

    /**
     * Get preparations list from page data.
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getPreparationsFromPage(NoteBookPage page, String key) {
        if (page == null) {
            return new ArrayList<>();
        }

        Map<String, Object> data = page.getData();
        if (data == null || !data.containsKey(key)) {
            return new ArrayList<>();
        }

        Object preparations = data.get(key);
        if (preparations instanceof List) {
            return (List<Map<String, Object>>) preparations;
        }
        return new ArrayList<>();
    }

    // ==========================================
    // CULTURE MEDIA PREPARATION
    // ==========================================

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getCultureMediaPreparations(Integer entryId) {
        try {
            Integer pageId = getProcessingQCPageId(entryId);
            if (pageId == null) {
                return new ArrayList<>();
            }
            NoteBookPage page = getFreshPage(pageId);
            return getPreparationsFromPage(page, CULTURE_MEDIA_KEY);
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "getCultureMediaPreparations",
                    "Error getting culture media preparations for entry " + entryId + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    @Transactional
    public Map<String, Object> saveCultureMediaPreparation(Integer entryId, Map<String, Object> preparation,
            String userId) {
        Integer pageId = getProcessingQCPageId(entryId);
        if (pageId == null) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "saveCultureMediaPreparation",
                    "Processing QC page not found for entry " + entryId);
            return null;
        }

        // Retry loop to handle optimistic locking conflicts
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                // Fetch fresh page and read preparations atomically
                NoteBookPage freshPage = getFreshPage(pageId);
                List<Map<String, Object>> preparations = new ArrayList<>(
                        getPreparationsFromPage(freshPage, CULTURE_MEDIA_KEY));

                // Generate ID
                int maxId = preparations.stream().mapToInt(p -> ((Number) p.getOrDefault("id", 0)).intValue()).max()
                        .orElse(0);
                preparation.put("id", maxId + 1);
                preparation.put("createdBy", userId);
                preparation.put("createdDate", new Timestamp(System.currentTimeMillis()).toString());

                preparations.add(preparation);

                // Save directly to the fresh page (avoid re-fetch)
                Map<String, Object> data = freshPage.getData();
                if (data == null) {
                    data = new HashMap<>();
                }
                data.put(CULTURE_MEDIA_KEY, preparations);
                freshPage.setData(data);
                freshPage.setLastupdated(new Timestamp(System.currentTimeMillis()));
                freshPage.setSysUserId(userId);
                noteBookPageService.update(freshPage);

                return preparation;
            } catch (ObjectOptimisticLockingFailureException | StaleObjectStateException e) {
                if (attempt < MAX_RETRIES) {
                    LogEvent.logWarn(this.getClass().getSimpleName(), "saveCultureMediaPreparation",
                            "Optimistic lock conflict on attempt " + attempt + ", retrying...");
                    try {
                        Thread.sleep(RETRY_DELAY_MS * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    LogEvent.logError(this.getClass().getSimpleName(), "saveCultureMediaPreparation",
                            "Failed after " + MAX_RETRIES + " attempts due to optimistic locking: " + e.getMessage());
                    return null;
                }
            } catch (Exception e) {
                LogEvent.logError(this.getClass().getSimpleName(), "saveCultureMediaPreparation",
                        "Error saving culture media preparation for entry " + entryId + ": " + e.getMessage());
                return null;
            }
        }
        return null;
    }

    @Override
    @Transactional
    public Map<String, Object> updateCultureMediaPreparation(Integer entryId, Integer preparationId,
            Map<String, Object> preparation, String userId) {
        try {
            Integer pageId = getProcessingQCPageId(entryId);
            if (pageId == null) {
                return null;
            }

            // Fetch fresh page and read preparations atomically
            NoteBookPage freshPage = getFreshPage(pageId);
            List<Map<String, Object>> preparations = new ArrayList<>(
                    getPreparationsFromPage(freshPage, CULTURE_MEDIA_KEY));

            for (int i = 0; i < preparations.size(); i++) {
                Map<String, Object> existing = preparations.get(i);
                if (((Number) existing.get("id")).intValue() == preparationId) {
                    preparation.put("id", preparationId);
                    preparation.put("updatedBy", userId);
                    preparation.put("updatedDate", new Timestamp(System.currentTimeMillis()).toString());
                    preparation.put("createdBy", existing.get("createdBy"));
                    preparation.put("createdDate", existing.get("createdDate"));

                    preparations.set(i, preparation);

                    // Save directly to the fresh page
                    Map<String, Object> data = freshPage.getData();
                    if (data == null) {
                        data = new HashMap<>();
                    }
                    data.put(CULTURE_MEDIA_KEY, preparations);
                    freshPage.setData(data);
                    freshPage.setLastupdated(new Timestamp(System.currentTimeMillis()));
                    freshPage.setSysUserId(userId);
                    noteBookPageService.update(freshPage);

                    return preparation;
                }
            }

            return null;
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "updateCultureMediaPreparation",
                    "Error updating culture media preparation " + preparationId + " for entry " + entryId + ": "
                            + e.getMessage());
            return null;
        }
    }

    @Override
    @Transactional
    public boolean deleteCultureMediaPreparation(Integer entryId, Integer preparationId, String userId) {
        try {
            Integer pageId = getProcessingQCPageId(entryId);
            if (pageId == null) {
                return false;
            }

            // Fetch fresh page and read preparations atomically
            NoteBookPage freshPage = getFreshPage(pageId);
            List<Map<String, Object>> preparations = new ArrayList<>(
                    getPreparationsFromPage(freshPage, CULTURE_MEDIA_KEY));

            boolean removed = preparations.removeIf(p -> ((Number) p.get("id")).intValue() == preparationId);

            if (removed) {
                Map<String, Object> data = freshPage.getData();
                if (data == null) {
                    data = new HashMap<>();
                }
                data.put(CULTURE_MEDIA_KEY, preparations);
                freshPage.setData(data);
                freshPage.setLastupdated(new Timestamp(System.currentTimeMillis()));
                freshPage.setSysUserId(userId);
                noteBookPageService.update(freshPage);
            }

            return removed;
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "deleteCultureMediaPreparation",
                    "Error deleting culture media preparation " + preparationId + " for entry " + entryId + ": "
                            + e.getMessage());
            return false;
        }
    }

    // ==========================================
    // BIOCHEMICAL MEDIA PREPARATION
    // ==========================================

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getBiochemicalMediaPreparations(Integer entryId) {
        try {
            Integer pageId = getProcessingQCPageId(entryId);
            if (pageId == null) {
                return new ArrayList<>();
            }
            NoteBookPage page = getFreshPage(pageId);
            return getPreparationsFromPage(page, BIOCHEMICAL_MEDIA_KEY);
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "getBiochemicalMediaPreparations",
                    "Error getting biochemical media preparations for entry " + entryId + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    @Transactional
    public Map<String, Object> saveBiochemicalMediaPreparation(Integer entryId, Map<String, Object> preparation,
            String userId) {
        Integer pageId = getProcessingQCPageId(entryId);
        if (pageId == null) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "saveBiochemicalMediaPreparation",
                    "Processing QC page not found for entry " + entryId);
            return null;
        }

        // Retry loop to handle optimistic locking conflicts
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                // Fetch fresh page and read preparations atomically
                NoteBookPage freshPage = getFreshPage(pageId);
                List<Map<String, Object>> preparations = new ArrayList<>(
                        getPreparationsFromPage(freshPage, BIOCHEMICAL_MEDIA_KEY));

                // Generate ID
                int maxId = preparations.stream().mapToInt(p -> ((Number) p.getOrDefault("id", 0)).intValue()).max()
                        .orElse(0);
                preparation.put("id", maxId + 1);
                preparation.put("createdBy", userId);
                preparation.put("createdDate", new Timestamp(System.currentTimeMillis()).toString());

                preparations.add(preparation);

                // Save directly to the fresh page
                Map<String, Object> data = freshPage.getData();
                if (data == null) {
                    data = new HashMap<>();
                }
                data.put(BIOCHEMICAL_MEDIA_KEY, preparations);
                freshPage.setData(data);
                freshPage.setLastupdated(new Timestamp(System.currentTimeMillis()));
                freshPage.setSysUserId(userId);
                noteBookPageService.update(freshPage);

                return preparation;
            } catch (ObjectOptimisticLockingFailureException | StaleObjectStateException e) {
                if (attempt < MAX_RETRIES) {
                    LogEvent.logWarn(this.getClass().getSimpleName(), "saveBiochemicalMediaPreparation",
                            "Optimistic lock conflict on attempt " + attempt + ", retrying...");
                    try {
                        Thread.sleep(RETRY_DELAY_MS * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    LogEvent.logError(this.getClass().getSimpleName(), "saveBiochemicalMediaPreparation",
                            "Failed after " + MAX_RETRIES + " attempts due to optimistic locking: " + e.getMessage());
                    return null;
                }
            } catch (Exception e) {
                LogEvent.logError(this.getClass().getSimpleName(), "saveBiochemicalMediaPreparation",
                        "Error saving biochemical media preparation for entry " + entryId + ": " + e.getMessage());
                return null;
            }
        }
        return null;
    }

    @Override
    @Transactional
    public Map<String, Object> updateBiochemicalMediaPreparation(Integer entryId, Integer preparationId,
            Map<String, Object> preparation, String userId) {
        try {
            Integer pageId = getProcessingQCPageId(entryId);
            if (pageId == null) {
                return null;
            }

            // Fetch fresh page and read preparations atomically
            NoteBookPage freshPage = getFreshPage(pageId);
            List<Map<String, Object>> preparations = new ArrayList<>(
                    getPreparationsFromPage(freshPage, BIOCHEMICAL_MEDIA_KEY));

            for (int i = 0; i < preparations.size(); i++) {
                Map<String, Object> existing = preparations.get(i);
                if (((Number) existing.get("id")).intValue() == preparationId) {
                    preparation.put("id", preparationId);
                    preparation.put("updatedBy", userId);
                    preparation.put("updatedDate", new Timestamp(System.currentTimeMillis()).toString());
                    preparation.put("createdBy", existing.get("createdBy"));
                    preparation.put("createdDate", existing.get("createdDate"));

                    preparations.set(i, preparation);

                    // Save directly to the fresh page
                    Map<String, Object> data = freshPage.getData();
                    if (data == null) {
                        data = new HashMap<>();
                    }
                    data.put(BIOCHEMICAL_MEDIA_KEY, preparations);
                    freshPage.setData(data);
                    freshPage.setLastupdated(new Timestamp(System.currentTimeMillis()));
                    freshPage.setSysUserId(userId);
                    noteBookPageService.update(freshPage);

                    return preparation;
                }
            }

            return null;
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "updateBiochemicalMediaPreparation",
                    "Error updating biochemical media preparation " + preparationId + " for entry " + entryId + ": "
                            + e.getMessage());
            return null;
        }
    }

    @Override
    @Transactional
    public boolean deleteBiochemicalMediaPreparation(Integer entryId, Integer preparationId, String userId) {
        try {
            Integer pageId = getProcessingQCPageId(entryId);
            if (pageId == null) {
                return false;
            }

            // Fetch fresh page and read preparations atomically
            NoteBookPage freshPage = getFreshPage(pageId);
            List<Map<String, Object>> preparations = new ArrayList<>(
                    getPreparationsFromPage(freshPage, BIOCHEMICAL_MEDIA_KEY));

            boolean removed = preparations.removeIf(p -> ((Number) p.get("id")).intValue() == preparationId);

            if (removed) {
                Map<String, Object> data = freshPage.getData();
                if (data == null) {
                    data = new HashMap<>();
                }
                data.put(BIOCHEMICAL_MEDIA_KEY, preparations);
                freshPage.setData(data);
                freshPage.setLastupdated(new Timestamp(System.currentTimeMillis()));
                freshPage.setSysUserId(userId);
                noteBookPageService.update(freshPage);
            }

            return removed;
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "deleteBiochemicalMediaPreparation",
                    "Error deleting biochemical media preparation " + preparationId + " for entry " + entryId + ": "
                            + e.getMessage());
            return false;
        }
    }

    // ==========================================
    // ANTIBIOTIC IQC PREPARATION
    // ==========================================

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAntibioticIqcPreparations(Integer entryId) {
        try {
            Integer pageId = getProcessingQCPageId(entryId);
            if (pageId == null) {
                return new ArrayList<>();
            }
            NoteBookPage page = getFreshPage(pageId);
            return getPreparationsFromPage(page, ANTIBIOTIC_IQC_KEY);
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "getAntibioticIqcPreparations",
                    "Error getting antibiotic IQC preparations for entry " + entryId + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    @Transactional
    public Map<String, Object> saveAntibioticIqcPreparation(Integer entryId, Map<String, Object> preparation,
            String userId) {
        Integer pageId = getProcessingQCPageId(entryId);
        if (pageId == null) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "saveAntibioticIqcPreparation",
                    "Processing QC page not found for entry " + entryId);
            return null;
        }

        // Retry loop to handle optimistic locking conflicts
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                // Fetch fresh page and read preparations atomically
                NoteBookPage freshPage = getFreshPage(pageId);
                List<Map<String, Object>> preparations = new ArrayList<>(
                        getPreparationsFromPage(freshPage, ANTIBIOTIC_IQC_KEY));

                // Generate ID
                int maxId = preparations.stream().mapToInt(p -> ((Number) p.getOrDefault("id", 0)).intValue()).max()
                        .orElse(0);
                preparation.put("id", maxId + 1);
                preparation.put("createdBy", userId);
                preparation.put("createdDate", new Timestamp(System.currentTimeMillis()).toString());

                preparations.add(preparation);

                // Save directly to the fresh page
                Map<String, Object> data = freshPage.getData();
                if (data == null) {
                    data = new HashMap<>();
                }
                data.put(ANTIBIOTIC_IQC_KEY, preparations);
                freshPage.setData(data);
                freshPage.setLastupdated(new Timestamp(System.currentTimeMillis()));
                freshPage.setSysUserId(userId);
                noteBookPageService.update(freshPage);

                return preparation;
            } catch (ObjectOptimisticLockingFailureException | StaleObjectStateException e) {
                if (attempt < MAX_RETRIES) {
                    LogEvent.logWarn(this.getClass().getSimpleName(), "saveAntibioticIqcPreparation",
                            "Optimistic lock conflict on attempt " + attempt + ", retrying...");
                    try {
                        Thread.sleep(RETRY_DELAY_MS * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    LogEvent.logError(this.getClass().getSimpleName(), "saveAntibioticIqcPreparation",
                            "Failed after " + MAX_RETRIES + " attempts due to optimistic locking: " + e.getMessage());
                    return null;
                }
            } catch (Exception e) {
                LogEvent.logError(this.getClass().getSimpleName(), "saveAntibioticIqcPreparation",
                        "Error saving antibiotic IQC preparation for entry " + entryId + ": " + e.getMessage());
                return null;
            }
        }
        return null;
    }

    @Override
    @Transactional
    public Map<String, Object> updateAntibioticIqcPreparation(Integer entryId, Integer preparationId,
            Map<String, Object> preparation, String userId) {
        try {
            Integer pageId = getProcessingQCPageId(entryId);
            if (pageId == null) {
                return null;
            }

            // Fetch fresh page and read preparations atomically
            NoteBookPage freshPage = getFreshPage(pageId);
            List<Map<String, Object>> preparations = new ArrayList<>(
                    getPreparationsFromPage(freshPage, ANTIBIOTIC_IQC_KEY));

            for (int i = 0; i < preparations.size(); i++) {
                Map<String, Object> existing = preparations.get(i);
                if (((Number) existing.get("id")).intValue() == preparationId) {
                    preparation.put("id", preparationId);
                    preparation.put("updatedBy", userId);
                    preparation.put("updatedDate", new Timestamp(System.currentTimeMillis()).toString());
                    preparation.put("createdBy", existing.get("createdBy"));
                    preparation.put("createdDate", existing.get("createdDate"));

                    preparations.set(i, preparation);

                    // Save directly to the fresh page
                    Map<String, Object> data = freshPage.getData();
                    if (data == null) {
                        data = new HashMap<>();
                    }
                    data.put(ANTIBIOTIC_IQC_KEY, preparations);
                    freshPage.setData(data);
                    freshPage.setLastupdated(new Timestamp(System.currentTimeMillis()));
                    freshPage.setSysUserId(userId);
                    noteBookPageService.update(freshPage);

                    return preparation;
                }
            }

            return null;
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "updateAntibioticIqcPreparation",
                    "Error updating antibiotic IQC preparation " + preparationId + " for entry " + entryId + ": "
                            + e.getMessage());
            return null;
        }
    }

    @Override
    @Transactional
    public boolean deleteAntibioticIqcPreparation(Integer entryId, Integer preparationId, String userId) {
        try {
            Integer pageId = getProcessingQCPageId(entryId);
            if (pageId == null) {
                return false;
            }

            // Fetch fresh page and read preparations atomically
            NoteBookPage freshPage = getFreshPage(pageId);
            List<Map<String, Object>> preparations = new ArrayList<>(
                    getPreparationsFromPage(freshPage, ANTIBIOTIC_IQC_KEY));

            boolean removed = preparations.removeIf(p -> ((Number) p.get("id")).intValue() == preparationId);

            if (removed) {
                Map<String, Object> data = freshPage.getData();
                if (data == null) {
                    data = new HashMap<>();
                }
                data.put(ANTIBIOTIC_IQC_KEY, preparations);
                freshPage.setData(data);
                freshPage.setLastupdated(new Timestamp(System.currentTimeMillis()));
                freshPage.setSysUserId(userId);
                noteBookPageService.update(freshPage);
            }

            return removed;
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "deleteAntibioticIqcPreparation",
                    "Error deleting antibiotic IQC preparation " + preparationId + " for entry " + entryId + ": "
                            + e.getMessage());
            return false;
        }
    }

    // ==========================================
    // REFERENCE DATA
    // ==========================================

    @Override
    public List<Map<String, String>> getCultureMediaTypes() {
        List<Map<String, String>> types = new ArrayList<>();

        // Common culture media types for bacteriology
        types.add(createOption("BLOOD_AGAR", "Blood Agar (BA)"));
        types.add(createOption("CHOCOLATE_AGAR", "Chocolate Agar (CA)"));
        types.add(createOption("MACCONKEY_AGAR", "MacConkey Agar (MAC)"));
        types.add(createOption("MUELLER_HINTON", "Mueller-Hinton Agar (MHA)"));
        types.add(createOption("MANNITOL_SALT", "Mannitol Salt Agar (MSA)"));
        types.add(createOption("EOSIN_METHYLENE_BLUE", "Eosin Methylene Blue (EMB)"));
        types.add(createOption("XYLOSE_LYSINE", "Xylose Lysine Deoxycholate (XLD)"));
        types.add(createOption("SALMONELLA_SHIGELLA", "Salmonella-Shigella Agar (SS)"));
        types.add(createOption("NUTRIENT_AGAR", "Nutrient Agar"));
        types.add(createOption("NUTRIENT_BROTH", "Nutrient Broth"));
        types.add(createOption("BRAIN_HEART_INFUSION", "Brain Heart Infusion (BHI)"));
        types.add(createOption("THIOGLYCOLLATE", "Thioglycollate Broth"));
        types.add(createOption("SABOURAUD_DEXTROSE", "Sabouraud Dextrose Agar (SDA)"));
        types.add(createOption("CLED_AGAR", "CLED Agar"));
        types.add(createOption("TCBS_AGAR", "TCBS Agar"));

        return types;
    }

    @Override
    public List<Map<String, String>> getBiochemicalTestTypes() {
        List<Map<String, String>> types = new ArrayList<>();

        // Common biochemical test types
        types.add(createOption("TSI", "Triple Sugar Iron (TSI)"));
        types.add(createOption("CITRATE", "Simmons Citrate"));
        types.add(createOption("UREA", "Urea Hydrolysis"));
        types.add(createOption("INDOLE", "Indole Test"));
        types.add(createOption("METHYL_RED", "Methyl Red (MR)"));
        types.add(createOption("VOGES_PROSKAUER", "Voges-Proskauer (VP)"));
        types.add(createOption("MOTILITY", "Motility Test"));
        types.add(createOption("CATALASE", "Catalase Test"));
        types.add(createOption("OXIDASE", "Oxidase Test"));
        types.add(createOption("COAGULASE", "Coagulase Test"));
        types.add(createOption("DNASE", "DNase Test"));
        types.add(createOption("OPTOCHIN", "Optochin Sensitivity"));
        types.add(createOption("BACITRACIN", "Bacitracin Sensitivity"));
        types.add(createOption("BILE_ESCULIN", "Bile Esculin"));
        types.add(createOption("CAMP_TEST", "CAMP Test"));
        types.add(createOption("NOVOBIOCIN", "Novobiocin Sensitivity"));
        types.add(createOption("PHENYLALANINE", "Phenylalanine Deaminase"));
        types.add(createOption("LYSINE", "Lysine Decarboxylase"));
        types.add(createOption("ORNITHINE", "Ornithine Decarboxylase"));

        return types;
    }

    @Override
    public List<Map<String, String>> getAntibioticTypes() {
        List<Map<String, String>> types = new ArrayList<>();

        // Common antibiotics for susceptibility testing
        // Penicillins
        types.add(createOption("PENICILLIN", "Penicillin G (PEN)"));
        types.add(createOption("AMPICILLIN", "Ampicillin (AMP)"));
        types.add(createOption("AMOXICILLIN", "Amoxicillin (AMX)"));
        types.add(createOption("AMOXICILLIN_CLAVULANATE", "Amoxicillin-Clavulanate (AMC)"));
        types.add(createOption("PIPERACILLIN_TAZOBACTAM", "Piperacillin-Tazobactam (TZP)"));

        // Cephalosporins
        types.add(createOption("CEPHALEXIN", "Cephalexin (LEX)"));
        types.add(createOption("CEFAZOLIN", "Cefazolin (CZ)"));
        types.add(createOption("CEFUROXIME", "Cefuroxime (CXM)"));
        types.add(createOption("CEFTRIAXONE", "Ceftriaxone (CRO)"));
        types.add(createOption("CEFTAZIDIME", "Ceftazidime (CAZ)"));
        types.add(createOption("CEFEPIME", "Cefepime (FEP)"));

        // Carbapenems
        types.add(createOption("IMIPENEM", "Imipenem (IPM)"));
        types.add(createOption("MEROPENEM", "Meropenem (MEM)"));
        types.add(createOption("ERTAPENEM", "Ertapenem (ETP)"));

        // Aminoglycosides
        types.add(createOption("GENTAMICIN", "Gentamicin (GEN)"));
        types.add(createOption("AMIKACIN", "Amikacin (AMK)"));
        types.add(createOption("TOBRAMYCIN", "Tobramycin (TOB)"));

        // Fluoroquinolones
        types.add(createOption("CIPROFLOXACIN", "Ciprofloxacin (CIP)"));
        types.add(createOption("LEVOFLOXACIN", "Levofloxacin (LVX)"));
        types.add(createOption("MOXIFLOXACIN", "Moxifloxacin (MXF)"));

        // Macrolides
        types.add(createOption("ERYTHROMYCIN", "Erythromycin (ERY)"));
        types.add(createOption("AZITHROMYCIN", "Azithromycin (AZM)"));
        types.add(createOption("CLARITHROMYCIN", "Clarithromycin (CLR)"));

        // Tetracyclines
        types.add(createOption("TETRACYCLINE", "Tetracycline (TET)"));
        types.add(createOption("DOXYCYCLINE", "Doxycycline (DOX)"));

        // Others
        types.add(createOption("VANCOMYCIN", "Vancomycin (VAN)"));
        types.add(createOption("CLINDAMYCIN", "Clindamycin (CLI)"));
        types.add(createOption("TRIMETHOPRIM_SULFAMETHOXAZOLE", "Trimethoprim-Sulfamethoxazole (SXT)"));
        types.add(createOption("NITROFURANTOIN", "Nitrofurantoin (NIT)"));
        types.add(createOption("CHLORAMPHENICOL", "Chloramphenicol (CHL)"));
        types.add(createOption("COLISTIN", "Colistin (COL)"));

        return types;
    }

    private Map<String, String> createOption(String id, String text) {
        Map<String, String> option = new HashMap<>();
        option.put("id", id);
        option.put("text", text);
        return option;
    }
}
