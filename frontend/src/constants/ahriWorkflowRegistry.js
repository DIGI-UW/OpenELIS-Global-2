/** Auto-synced from volume/configuration/backend/workflow-registry/ahri-workflows.csv */
export const NOTEBOOK_STAGE_ACTIONS = {
  VIEW: "VIEW",
  EDIT: "EDIT",
  COMPLETE: "COMPLETE",
};

const REGISTRY_BY_WORKFLOW_TYPE = {
  "biorepository": [
    {
      "stageOrder": 1,
      "stageId": "intake",
      "pageKey": "intake",
      "stageTitle": "Sample Intake & Registration",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Sample Collector",
        "Laboratory Technician"
      ]
    },
    {
      "stageOrder": 2,
      "stageId": "storage_assign",
      "pageKey": "storage_assign",
      "stageTitle": "Storage Assignment",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Laboratory Technician",
        "Lab Manager"
      ]
    },
    {
      "stageOrder": 3,
      "stageId": "monitoring",
      "pageKey": "monitoring",
      "stageTitle": "Ongoing Storage and Monitoring",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Laboratory Technician",
        "Lab Manager"
      ]
    },
    {
      "stageOrder": 4,
      "stageId": "retention",
      "pageKey": "retention",
      "stageTitle": "Retention & Disposal",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Lab Manager"
      ]
    },
    {
      "stageOrder": 5,
      "stageId": "request",
      "pageKey": "request",
      "stageTitle": "Sample Request & Retrieval",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Lab Manager",
        "Laboratory Technician"
      ]
    },
    {
      "stageOrder": 6,
      "stageId": "qc",
      "pageKey": "qc",
      "stageTitle": "QC Inspection",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Laboratory Technician",
        "Lab Manager"
      ]
    },
    {
      "stageOrder": 7,
      "stageId": "reporting",
      "pageKey": "reporting",
      "stageTitle": "Reporting & Audit",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Lab Manager",
        "Senior Researcher"
      ]
    }
  ],
  "immunology": [
    {
      "stageOrder": 1,
      "stageId": "reception",
      "pageKey": "reception",
      "stageTitle": "Sample Reception",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Sample Collector",
        "Laboratory Technician"
      ]
    },
    {
      "stageOrder": 2,
      "stageId": "processing",
      "pageKey": "processing",
      "stageTitle": "Initial Processing",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Laboratory Technician",
        "Junior Researcher",
        "Senior Researcher"
      ]
    },
    {
      "stageOrder": 3,
      "stageId": "assays",
      "pageKey": "assays",
      "stageTitle": "Assays",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Laboratory Technician",
        "Junior Researcher",
        "Senior Researcher"
      ]
    },
    {
      "stageOrder": 4,
      "stageId": "child_samples",
      "pageKey": "child_samples",
      "stageTitle": "Child Samples",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Laboratory Technician",
        "Junior Researcher",
        "Senior Researcher"
      ]
    },
    {
      "stageOrder": 5,
      "stageId": "prep",
      "pageKey": "prep",
      "stageTitle": "Prep",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Laboratory Technician",
        "Junior Researcher",
        "Senior Researcher"
      ]
    },
    {
      "stageOrder": 6,
      "stageId": "analysis",
      "pageKey": "analysis",
      "stageTitle": "Analysis",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Junior Researcher",
        "Senior Researcher",
        "Laboratory Technician"
      ]
    },
    {
      "stageOrder": 7,
      "stageId": "storage",
      "pageKey": "storage",
      "stageTitle": "Storage",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Laboratory Technician",
        "Lab Manager"
      ]
    },
    {
      "stageOrder": 8,
      "stageId": "results",
      "pageKey": "results",
      "stageTitle": "Results",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Laboratory Technician",
        "Lab Manager"
      ]
    },
    {
      "stageOrder": 9,
      "stageId": "archive",
      "pageKey": "archive",
      "stageTitle": "Archive",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Lab Manager"
      ]
    },
    {
      "stageOrder": 10,
      "stageId": "reporting",
      "pageKey": "reporting",
      "stageTitle": "Reporting & REDCap",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Lab Manager",
        "Senior Researcher"
      ]
    }
  ],
  "bacteriology": [
    {
      "stageOrder": 1,
      "stageId": "reception",
      "pageKey": "reception",
      "stageTitle": "Sample Reception",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Sample Collector",
        "Laboratory Technician"
      ]
    },
    {
      "stageOrder": 2,
      "stageId": "lab_reception",
      "pageKey": "lab_reception",
      "stageTitle": "Laboratory Reception & Verification",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Sample Collector",
        "Laboratory Technician"
      ]
    },
    {
      "stageOrder": 3,
      "stageId": "isolate",
      "pageKey": "isolate",
      "stageTitle": "Isolate Creation",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Laboratory Technician",
        "Junior Researcher",
        "Senior Researcher"
      ]
    },
    {
      "stageOrder": 4,
      "stageId": "temp_storage",
      "pageKey": "temp_storage",
      "stageTitle": "Temporary Storage Assignment",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Laboratory Technician",
        "Lab Manager"
      ]
    },
    {
      "stageOrder": 5,
      "stageId": "processing_qc",
      "pageKey": "processing_qc",
      "stageTitle": "Processing & Quality Control",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Laboratory Technician",
        "Lab Manager"
      ]
    },
    {
      "stageOrder": 6,
      "stageId": "test_execution",
      "pageKey": "test_execution",
      "stageTitle": "Assay/Test Execution",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Laboratory Technician",
        "Junior Researcher",
        "Senior Researcher"
      ]
    },
    {
      "stageOrder": 7,
      "stageId": "post_storage",
      "pageKey": "post_storage",
      "stageTitle": "Post-Analysis Storage",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Laboratory Technician",
        "Lab Manager"
      ]
    },
    {
      "stageOrder": 8,
      "stageId": "disposal",
      "pageKey": "disposal",
      "stageTitle": "Sample Retrieval, Archival & Disposal",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Lab Manager"
      ]
    },
    {
      "stageOrder": 9,
      "stageId": "reporting",
      "pageKey": "reporting",
      "stageTitle": "Reporting & Data Export",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Lab Manager",
        "Senior Researcher"
      ]
    }
  ],
  "mntd": [
    {
      "stageOrder": 1,
      "stageId": "intake",
      "pageKey": "intake",
      "stageTitle": "Sample Intake / Sample Creation",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Sample Collector",
        "Laboratory Technician"
      ]
    },
    {
      "stageOrder": 2,
      "stageId": "lab_reception",
      "pageKey": "lab_reception",
      "stageTitle": "Laboratory Reception & Verification",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Sample Collector",
        "Laboratory Technician"
      ]
    },
    {
      "stageOrder": 3,
      "stageId": "temp_storage",
      "pageKey": "temp_storage",
      "stageTitle": "Temporary Storage Assignment",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Laboratory Technician",
        "Lab Manager"
      ]
    },
    {
      "stageOrder": 4,
      "stageId": "prep",
      "pageKey": "prep",
      "stageTitle": "Sample Processing Preparation",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Laboratory Technician",
        "Junior Researcher",
        "Senior Researcher"
      ]
    },
    {
      "stageOrder": 5,
      "stageId": "aliquot",
      "pageKey": "aliquot",
      "stageTitle": "Aliquoting / Bulk Sample Import",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Laboratory Technician",
        "Junior Researcher",
        "Senior Researcher"
      ]
    },
    {
      "stageOrder": 6,
      "stageId": "processing_qc",
      "pageKey": "processing_qc",
      "stageTitle": "Processing & Quality Control",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Laboratory Technician",
        "Lab Manager"
      ]
    },
    {
      "stageOrder": 7,
      "stageId": "scheduling",
      "pageKey": "scheduling",
      "stageTitle": "Test Assignment & Machine Scheduling",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Laboratory Technician",
        "Junior Researcher",
        "Senior Researcher"
      ]
    },
    {
      "stageOrder": 8,
      "stageId": "execution",
      "pageKey": "execution",
      "stageTitle": "Test Execution & Raw Data Capture",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Laboratory Technician",
        "Junior Researcher",
        "Senior Researcher"
      ]
    },
    {
      "stageOrder": 9,
      "stageId": "archiving",
      "pageKey": "archiving",
      "stageTitle": "Sample Archiving",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Lab Manager"
      ]
    },
    {
      "stageOrder": 10,
      "stageId": "analysis",
      "pageKey": "analysis",
      "stageTitle": "Data Analysis & Export",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Lab Manager",
        "Senior Researcher"
      ]
    }
  ],
  "tuberculosis": [
    {
      "stageOrder": 1,
      "stageId": "accession",
      "pageKey": "accession",
      "stageTitle": "Sample Accession & Registration",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Sample Collector",
        "Laboratory Technician"
      ]
    },
    {
      "stageOrder": 2,
      "stageId": "qc",
      "pageKey": "qc",
      "stageTitle": "Raw Sample Quality Check (QC)",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Laboratory Technician",
        "Lab Manager"
      ]
    },
    {
      "stageOrder": 3,
      "stageId": "processing",
      "pageKey": "processing",
      "stageTitle": "Initial Sample Processing",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Laboratory Technician",
        "Junior Researcher",
        "Senior Researcher"
      ]
    },
    {
      "stageOrder": 4,
      "stageId": "incubation",
      "pageKey": "incubation",
      "stageTitle": "Incubation & Monitoring",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Laboratory Technician",
        "Junior Researcher",
        "Senior Researcher"
      ]
    },
    {
      "stageOrder": 5,
      "stageId": "test",
      "pageKey": "test",
      "stageTitle": "Test Execution",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Laboratory Technician",
        "Junior Researcher",
        "Senior Researcher"
      ]
    },
    {
      "stageOrder": 6,
      "stageId": "storage",
      "pageKey": "storage",
      "stageTitle": "Isolate Storage",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Laboratory Technician",
        "Lab Manager"
      ]
    },
    {
      "stageOrder": 7,
      "stageId": "disposal",
      "pageKey": "disposal",
      "stageTitle": "Disposal & Archiving",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Lab Manager"
      ]
    },
    {
      "stageOrder": 8,
      "stageId": "reporting",
      "pageKey": "reporting",
      "stageTitle": "Reporting",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Lab Manager",
        "Senior Researcher"
      ]
    }
  ],
  "pharmaceutical": [
    {
      "stageOrder": 1,
      "stageId": "creation",
      "pageKey": "creation",
      "stageTitle": "Sample Creation & Full Metadata Capture",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Sample Collector",
        "Laboratory Technician"
      ]
    },
    {
      "stageOrder": 2,
      "stageId": "qc",
      "pageKey": "qc",
      "stageTitle": "Raw Sample Quality Check (QC)",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Laboratory Technician",
        "Lab Manager"
      ]
    },
    {
      "stageOrder": 3,
      "stageId": "processing",
      "pageKey": "processing",
      "stageTitle": "Sample Processing & Aliquoting",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Laboratory Technician",
        "Junior Researcher",
        "Senior Researcher"
      ]
    },
    {
      "stageOrder": 4,
      "stageId": "assay",
      "pageKey": "assay",
      "stageTitle": "Assay & Test Execution",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Laboratory Technician",
        "Junior Researcher",
        "Senior Researcher"
      ]
    },
    {
      "stageOrder": 5,
      "stageId": "storage",
      "pageKey": "storage",
      "stageTitle": "Storage & Inventory Management",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Laboratory Technician",
        "Lab Manager"
      ]
    },
    {
      "stageOrder": 6,
      "stageId": "reporting",
      "pageKey": "reporting",
      "stageTitle": "Reporting & Performance Monitoring",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Lab Manager",
        "Senior Researcher"
      ]
    },
    {
      "stageOrder": 7,
      "stageId": "disposal",
      "pageKey": "disposal",
      "stageTitle": "Disposal & Archiving",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Lab Manager"
      ]
    }
  ],
  "traditional_medicine": [
    {
      "stageOrder": 1,
      "stageId": "intake",
      "pageKey": "intake",
      "stageTitle": "Sample Intake, Registration & Authentication",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Sample Collector",
        "Laboratory Technician"
      ]
    },
    {
      "stageOrder": 2,
      "stageId": "herbarium",
      "pageKey": "herbarium",
      "stageTitle": "Sample Storage & Herbarium Placement",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Laboratory Technician",
        "Lab Manager"
      ]
    },
    {
      "stageOrder": 3,
      "stageId": "prep",
      "pageKey": "prep",
      "stageTitle": "Sample Preparation for Analysis",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Laboratory Technician",
        "Junior Researcher",
        "Senior Researcher"
      ]
    },
    {
      "stageOrder": 4,
      "stageId": "extraction",
      "pageKey": "extraction",
      "stageTitle": "Extraction, Filtration & Concentration",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Laboratory Technician",
        "Junior Researcher",
        "Senior Researcher"
      ]
    },
    {
      "stageOrder": 5,
      "stageId": "analytical",
      "pageKey": "analytical",
      "stageTitle": "Analytical Pathway",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Junior Researcher",
        "Senior Researcher",
        "Laboratory Technician"
      ]
    },
    {
      "stageOrder": 6,
      "stageId": "product_dev",
      "pageKey": "product_dev",
      "stageTitle": "Product Development & Testing",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Junior Researcher",
        "Senior Researcher",
        "Laboratory Technician"
      ]
    },
    {
      "stageOrder": 7,
      "stageId": "formulation",
      "pageKey": "formulation",
      "stageTitle": "Formulation of Medical Product",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Laboratory Technician",
        "Junior Researcher",
        "Senior Researcher"
      ]
    },
    {
      "stageOrder": 8,
      "stageId": "archive",
      "pageKey": "archive",
      "stageTitle": "Storage, Reporting & Archival",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Lab Manager",
        "Senior Researcher"
      ]
    }
  ],
  "bioanalytical": [
    {
      "stageOrder": 1,
      "stageId": "reception",
      "pageKey": "reception",
      "stageTitle": "Sample Reception & Registration",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Sample Collector",
        "Laboratory Technician"
      ]
    },
    {
      "stageOrder": 2,
      "stageId": "assignment",
      "pageKey": "assignment",
      "stageTitle": "Test Assignment & Preparation",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Laboratory Technician",
        "Junior Researcher",
        "Senior Researcher"
      ]
    },
    {
      "stageOrder": 3,
      "stageId": "analysis",
      "pageKey": "analysis",
      "stageTitle": "Conduct Analysis / Test",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Laboratory Technician",
        "Junior Researcher",
        "Senior Researcher"
      ]
    },
    {
      "stageOrder": 4,
      "stageId": "reporting",
      "pageKey": "reporting",
      "stageTitle": "Reporting & Release",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Lab Manager",
        "Senior Researcher"
      ]
    },
    {
      "stageOrder": 5,
      "stageId": "post_test",
      "pageKey": "post_test",
      "stageTitle": "Post-Test Sample & Data Handling",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Laboratory Technician",
        "Lab Manager"
      ]
    }
  ],
  "bioequivalence": [
    {
      "stageOrder": 1,
      "stageId": "reception",
      "pageKey": "reception",
      "stageTitle": "Sample Reception & Registration",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Sample Collector",
        "Laboratory Technician"
      ]
    },
    {
      "stageOrder": 2,
      "stageId": "assignment",
      "pageKey": "assignment",
      "stageTitle": "Test Assignment & Preparation",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Laboratory Technician",
        "Junior Researcher",
        "Senior Researcher"
      ]
    },
    {
      "stageOrder": 3,
      "stageId": "analysis",
      "pageKey": "analysis",
      "stageTitle": "Conduct Analysis / Test",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Laboratory Technician",
        "Junior Researcher",
        "Senior Researcher"
      ]
    },
    {
      "stageOrder": 4,
      "stageId": "reporting",
      "pageKey": "reporting",
      "stageTitle": "Reporting & Release",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Lab Manager",
        "Senior Researcher"
      ]
    },
    {
      "stageOrder": 5,
      "stageId": "post_test",
      "pageKey": "post_test",
      "stageTitle": "Post-Test Sample & Data Handling",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Laboratory Technician",
        "Lab Manager"
      ]
    }
  ],
  "pathology": [
    {
      "stageOrder": 1,
      "stageId": "creation",
      "pageKey": "creation",
      "stageTitle": "Sample Creation and Metadata Capture",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Sample Collector",
        "Laboratory Technician"
      ]
    },
    {
      "stageOrder": 2,
      "stageId": "qc",
      "pageKey": "qc",
      "stageTitle": "Sample Quality Control",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Laboratory Technician",
        "Lab Manager"
      ]
    },
    {
      "stageOrder": 3,
      "stageId": "gross",
      "pageKey": "gross",
      "stageTitle": "Gross Examination",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Laboratory Technician",
        "Junior Researcher",
        "Senior Researcher"
      ]
    },
    {
      "stageOrder": 4,
      "stageId": "cassette",
      "pageKey": "cassette",
      "stageTitle": "Cassette Setup",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Laboratory Technician",
        "Junior Researcher",
        "Senior Researcher"
      ]
    },
    {
      "stageOrder": 5,
      "stageId": "processing",
      "pageKey": "processing",
      "stageTitle": "Sample Processing",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Laboratory Technician",
        "Junior Researcher",
        "Senior Researcher"
      ]
    },
    {
      "stageOrder": 6,
      "stageId": "block",
      "pageKey": "block",
      "stageTitle": "Block Creation",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Laboratory Technician",
        "Junior Researcher",
        "Senior Researcher"
      ]
    },
    {
      "stageOrder": 7,
      "stageId": "slide_prep",
      "pageKey": "slide_prep",
      "stageTitle": "Slide Preparation",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Laboratory Technician",
        "Junior Researcher",
        "Senior Researcher"
      ]
    },
    {
      "stageOrder": 8,
      "stageId": "staining",
      "pageKey": "staining",
      "stageTitle": "Slide Staining",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Laboratory Technician",
        "Junior Researcher",
        "Senior Researcher"
      ]
    },
    {
      "stageOrder": 9,
      "stageId": "microscopy",
      "pageKey": "microscopy",
      "stageTitle": "Microscopy and Diagnosis",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Junior Researcher",
        "Senior Researcher",
        "Laboratory Technician"
      ]
    },
    {
      "stageOrder": 10,
      "stageId": "report_print",
      "pageKey": "report_print",
      "stageTitle": "Individual Patient Report Preview and Print",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Lab Manager",
        "Senior Researcher"
      ]
    },
    {
      "stageOrder": 11,
      "stageId": "storage",
      "pageKey": "storage",
      "stageTitle": "Storage and Inventory Management",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Laboratory Technician",
        "Lab Manager"
      ]
    },
    {
      "stageOrder": 12,
      "stageId": "performance",
      "pageKey": "performance",
      "stageTitle": "Reporting and Performance Monitoring",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Lab Manager",
        "Senior Researcher"
      ]
    },
    {
      "stageOrder": 13,
      "stageId": "disposal",
      "pageKey": "disposal",
      "stageTitle": "Disposal and Archiving",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Lab Manager"
      ]
    }
  ],
  "medlab": [
    {
      "stageOrder": 1,
      "stageId": "patient",
      "pageKey": "patient",
      "stageTitle": "Patient Registration",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Sample Collector",
        "Laboratory Technician"
      ]
    },
    {
      "stageOrder": 2,
      "stageId": "collection",
      "pageKey": "collection",
      "stageTitle": "Sample Collection",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Sample Collector",
        "Laboratory Technician"
      ]
    },
    {
      "stageOrder": 3,
      "stageId": "transport",
      "pageKey": "transport",
      "stageTitle": "Transport & Packaging",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Laboratory Technician",
        "Junior Researcher",
        "Senior Researcher"
      ]
    },
    {
      "stageOrder": 4,
      "stageId": "receipt",
      "pageKey": "receipt",
      "stageTitle": "Sample Receipt & Quality Assessment",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Laboratory Technician",
        "Lab Manager"
      ]
    },
    {
      "stageOrder": 5,
      "stageId": "routing",
      "pageKey": "routing",
      "stageTitle": "Sample Routing",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Laboratory Technician",
        "Junior Researcher",
        "Senior Researcher"
      ]
    },
    {
      "stageOrder": 6,
      "stageId": "processing",
      "pageKey": "processing",
      "stageTitle": "Sample Processing",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Laboratory Technician",
        "Junior Researcher",
        "Senior Researcher"
      ]
    },
    {
      "stageOrder": 7,
      "stageId": "testing",
      "pageKey": "testing",
      "stageTitle": "Testing & Analyzer",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Laboratory Technician",
        "Junior Researcher",
        "Senior Researcher"
      ]
    },
    {
      "stageOrder": 8,
      "stageId": "results",
      "pageKey": "results",
      "stageTitle": "Result Entry",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Laboratory Technician",
        "Lab Manager"
      ]
    },
    {
      "stageOrder": 9,
      "stageId": "validation",
      "pageKey": "validation",
      "stageTitle": "Validation, Reporting & Performance Monitoring",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Lab Manager",
        "Senior Researcher"
      ]
    },
    {
      "stageOrder": 10,
      "stageId": "disposal",
      "pageKey": "disposal",
      "stageTitle": "Disposal, Archiving & Accreditation",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Lab Manager"
      ]
    }
  ],
  "virology": [
    {
      "stageOrder": 1,
      "stageId": "intake",
      "pageKey": "intake",
      "stageTitle": "Sample Intake & Registration",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Sample Collector",
        "Laboratory Technician"
      ]
    },
    {
      "stageOrder": 2,
      "stageId": "extraction",
      "pageKey": "extraction",
      "stageTitle": "DNA/RNA Extraction",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Laboratory Technician",
        "Junior Researcher",
        "Senior Researcher"
      ]
    },
    {
      "stageOrder": 3,
      "stageId": "qc",
      "pageKey": "qc",
      "stageTitle": "Quality & Quantity Assessment",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Laboratory Technician",
        "Lab Manager"
      ]
    },
    {
      "stageOrder": 4,
      "stageId": "pcr",
      "pageKey": "pcr",
      "stageTitle": "PCR Amplification",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Laboratory Technician",
        "Junior Researcher",
        "Senior Researcher"
      ]
    },
    {
      "stageOrder": 5,
      "stageId": "gel",
      "pageKey": "gel",
      "stageTitle": "Gel Electrophoresis",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Laboratory Technician",
        "Junior Researcher",
        "Senior Researcher"
      ]
    },
    {
      "stageOrder": 6,
      "stageId": "library",
      "pageKey": "library",
      "stageTitle": "Library Preparation",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Laboratory Technician",
        "Junior Researcher",
        "Senior Researcher"
      ]
    },
    {
      "stageOrder": 7,
      "stageId": "bioanalyzer",
      "pageKey": "bioanalyzer",
      "stageTitle": "Bioanalyzer QC",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Laboratory Technician",
        "Lab Manager"
      ]
    },
    {
      "stageOrder": 8,
      "stageId": "sequencing",
      "pageKey": "sequencing",
      "stageTitle": "Sequencing",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Laboratory Technician",
        "Junior Researcher",
        "Senior Researcher"
      ]
    },
    {
      "stageOrder": 9,
      "stageId": "bioinformatics",
      "pageKey": "bioinformatics",
      "stageTitle": "Bioinformatics Analysis & Data Submission",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Junior Researcher",
        "Senior Researcher",
        "Laboratory Technician"
      ]
    },
    {
      "stageOrder": 10,
      "stageId": "storage",
      "pageKey": "storage",
      "stageTitle": "Storage & Environmental Monitoring",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Laboratory Technician",
        "Lab Manager"
      ]
    }
  ],
  "viral_vaccine": [
    {
      "stageOrder": 1,
      "stageId": "intake",
      "pageKey": "intake",
      "stageTitle": "Sample Intake and Registration",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Sample Collector",
        "Laboratory Technician"
      ]
    },
    {
      "stageOrder": 2,
      "stageId": "media_prep",
      "pageKey": "media_prep",
      "stageTitle": "Media Preparation",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Laboratory Technician",
        "Junior Researcher",
        "Senior Researcher"
      ]
    },
    {
      "stageOrder": 3,
      "stageId": "cell_culture",
      "pageKey": "cell_culture",
      "stageTitle": "Cell Culture",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Laboratory Technician",
        "Junior Researcher",
        "Senior Researcher"
      ]
    },
    {
      "stageOrder": 4,
      "stageId": "quality_control",
      "pageKey": "quality_control",
      "stageTitle": "Quality Control",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Laboratory Technician",
        "Lab Manager"
      ]
    },
    {
      "stageOrder": 5,
      "stageId": "virus_culture",
      "pageKey": "virus_culture",
      "stageTitle": "Virus Culture",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Laboratory Technician",
        "Junior Researcher",
        "Senior Researcher"
      ]
    },
    {
      "stageOrder": 6,
      "stageId": "dark_room_imaging",
      "pageKey": "dark_room_imaging",
      "stageTitle": "Dark Room Imaging",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Laboratory Technician",
        "Junior Researcher",
        "Senior Researcher"
      ]
    },
    {
      "stageOrder": 7,
      "stageId": "formulation",
      "pageKey": "formulation",
      "stageTitle": "Formulation",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Laboratory Technician",
        "Junior Researcher",
        "Senior Researcher"
      ]
    },
    {
      "stageOrder": 8,
      "stageId": "feeding",
      "pageKey": "feeding",
      "stageTitle": "Feeding",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Laboratory Technician",
        "Junior Researcher",
        "Senior Researcher"
      ]
    },
    {
      "stageOrder": 9,
      "stageId": "packaging",
      "pageKey": "packaging",
      "stageTitle": "Packaging",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Laboratory Technician",
        "Junior Researcher",
        "Senior Researcher"
      ]
    },
    {
      "stageOrder": 10,
      "stageId": "virus_isolation",
      "pageKey": "virus_isolation",
      "stageTitle": "Virus Isolation",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Laboratory Technician",
        "Junior Researcher",
        "Senior Researcher"
      ]
    },
    {
      "stageOrder": 11,
      "stageId": "titer_measurement",
      "pageKey": "titer_measurement",
      "stageTitle": "Titer Measurement",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Laboratory Technician",
        "Junior Researcher",
        "Senior Researcher"
      ]
    },
    {
      "stageOrder": 12,
      "stageId": "genome_sequencing",
      "pageKey": "genome_sequencing",
      "stageTitle": "Genome Sequencing",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Laboratory Technician",
        "Junior Researcher",
        "Senior Researcher"
      ]
    },
    {
      "stageOrder": 13,
      "stageId": "seed_virus_production",
      "pageKey": "seed_virus_production",
      "stageTitle": "Seed Virus Production",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Junior Researcher",
        "Senior Researcher",
        "Lab Manager"
      ]
    },
    {
      "stageOrder": 14,
      "stageId": "preclinical_clinical_trials",
      "pageKey": "preclinical_clinical_trials",
      "stageTitle": "Preclinical & Clinical Trials",
      "requiredActions": [
        "VIEW",
        "EDIT",
        "COMPLETE"
      ],
      "allowedPersonas": [
        "Senior Researcher",
        "Lab Manager"
      ]
    }
  ]
};

export function normalizeWorkflowType(workflowType) {
  if (!workflowType) return "";
  return String(workflowType).trim().toLowerCase().replace(/\s+/g, "_");
}

export function getRegistryStages(workflowType) {
  const key = normalizeWorkflowType(workflowType);
  return REGISTRY_BY_WORKFLOW_TYPE[key] || [];
}

export function resolvePageKey(page) {
  if (!page) return "";
  if (page.pageId) return String(page.pageId).trim();
  if (page.pageKey) return String(page.pageKey).trim();
  const order = page?.pageOrder ?? page?.order ?? 0;
  return order > 0 ? `stage-${order}` : "";
}

export function findRegistryStage(workflowType, page) {
  const key = resolvePageKey(page);
  const order = page?.pageOrder ?? page?.order ?? 0;
  const stages = getRegistryStages(workflowType);
  return (
    stages.find((s) => s.pageKey === key || s.stageId === key) ||
    stages.find((s) => s.stageOrder === order) ||
    null
  );
}

export function isActionPermitted(workflowType, page, action) {
  const stage = findRegistryStage(workflowType, page);
  if (!stage) return false;
  const actions = stage.requiredActions || [];
  return actions.includes(action);
}

export function resolvePageAllowedRoles(workflowType, page, action = null) {
  const explicit = page?.allowedRoles
    ? (Array.isArray(page.allowedRoles) ? page.allowedRoles : Array.from(page.allowedRoles))
    : [];
  if (explicit.length > 0) {
    return explicit;
  }
  const stage = findRegistryStage(workflowType, page);
  if (!stage) return [];
  if (action && !isActionPermitted(workflowType, page, action)) {
    return [];
  }
  return stage.allowedPersonas || [];
}

export function enrichPagesWithRegistryRoles(workflowType, pages) {
  if (!pages || !Array.isArray(pages)) return [];
  return pages.map((page) => {
    const allowedRoles = resolvePageAllowedRoles(workflowType, page);
    return { ...page, pageKey: resolvePageKey(page), allowedRoles };
  });
}

export { REGISTRY_BY_WORKFLOW_TYPE };
