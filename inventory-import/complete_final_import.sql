-- Complete MNTD Inventory Import - Final Version with Correct Schema
BEGIN TRANSACTION;

-- Insert Catalog Items
WITH catalog_data AS (
    SELECT 'MgCl2 10xPCR rxn buffer', 'Sequencing primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'KB extender', 'Sequencing primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pfcrt-Rv ', 'Sequencing primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pfcrt-Fw', 'Sequencing primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'K13-Fw', 'Sequencing primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'K13-Rv', 'Sequencing primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pf mdr1-Rv', 'Sequencing primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pfcrt-Fw ', 'Sequencing primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pfdhps-Rv', 'Sequencing primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pfdhps-Fw', 'Sequencing primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pfcrt-F1', 'Sequencing primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pfcrt-R1 sequence', 'Sequencing primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pfdhps-F', 'Sequencing primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pfdhps-R', 'Sequencing primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'plasmepsin 3R', 'Sequencing primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'plasmepsin 2R', 'Sequencing primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'plasmepsin 2F', 'Sequencing primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'plasmepsin 3F', 'Sequencing primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'plasmepsin 3P', 'Sequencing primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'plasmepsin 2P', 'Sequencing primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pf 18s fwd', 'Sequencing primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pf 18s rev', 'Sequencing primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Beta tubulin F', 'Sequencing primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'beta tubulin R', 'Sequencing primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'beta tublin P', 'Sequencing primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'cyt B-F1', 'Sequencing primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'cyt B-R1', 'Sequencing primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'mitochndorion -R1', 'Sequencing primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'mitochndorion -F1', 'Sequencing primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pf k13-F1', 'Sequencing primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pf k13-R1', 'Sequencing primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'pfmdr1 F', 'Sequencing primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'pfmdr1 R', 'Sequencing primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'FastDigest Alul enzyme with buffer ', 'Enzymes', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'SmartCut enzyme with buffer ', 'Enzymes', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'mbo II 5000 U/ml', 'Enzymes', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Nla III  10,000 U/ml', 'Enzymes', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Ase I  10,000 U/ml', 'Enzymes', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Fok I 5,000 U/ml', 'Enzymes', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Fok I 5,000 U/ml (cutsmart)', 'Enzymes', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Dra I  20,000 U/ml', 'Enzymes', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'ApoI 10,000 U/ml', 'Enzymes', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Dde I   10,000 U/ml', 'Enzymes', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Eco RV 20,000 U/ml', 'Enzymes', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'NEBuffer  3.1  10x conc.', 'Enzymes', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'NEBuffer  4  10x conc.', 'Enzymes', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Afa I  1,000 U/ml', 'Enzymes', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pvu II 15U/ml', 'Enzymes', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT '10x M buffer', 'Enzymes', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'cutsmart buffer  10x conc.', 'Enzymes', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Qiagen rnase free water', 'Enzymes', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT '1KB + DNA ladder', 'Enzymes', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT '0.1% BSA ', 'Enzymes', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'pscam II (alpha DNA)', 'Enzymes', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'T2 (alpha DNA)', 'Enzymes', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'ILRI 161108 R1 66.1 nmol', 'Enzymes', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'ILRI 161108 R2 60.3 nmol', 'Enzymes', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'ILRI 161108 R3 60.5 nmol', 'Enzymes', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'ILRI 161108 F3 45.1 nmol', 'Enzymes', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'ILRI 161108 F2 45.1 nmol', 'Enzymes', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'loading dye', 'Enzymes', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Eurofins genomics 2R', 'Enzymes', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Eurofins genomics 1R', 'Enzymes', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Eurofins genomics pair 1R', 'Enzymes', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Eurofins genomics pair 2F', 'Enzymes', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Eurofins genomics pair 2R', 'Enzymes', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Eurofins genomics 2F', 'Enzymes', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Eurofins genomics 1F', 'Enzymes', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Eurofins genomics pair 1F', 'Enzymes', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Hae III', 'Enzymes', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Hha I', 'Enzymes', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT '10x React 2', 'Enzymes', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'quanti Tect probe PCR mastermix', 'Enzymes', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT '10x buffer T', 'Enzymes', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Hha1 use with react 2', 'Enzymes', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT '10x react I', 'Enzymes', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT '10x PCR buffer (-mgcl2)', 'Enzymes', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'HuPoR (alpha DNA)(5'' GGACTTCGTTTGTACCCGTTG )', 'Enzymes', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'b- actin R (5'' CGTCATACTCCTGCTTGCTGATCCACATCTGC)', 'Enzymes', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'LaeV5f (5'' CGTGATGTGCCCGAGTGCA)', 'Enzymes', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'cpb EF rev', 'Enzymes', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'cpb EFfwd', 'Enzymes', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'K26 fwd', 'Enzymes', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'k26 rev', 'Enzymes', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'List R', 'Enzymes', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT '10xFast DigestGreen Buffer', 'Enzymes', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT '10xFast Digest Buffer (colorless)', 'Enzymes', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Fast Digest Buffer Alul', 'Enzymes', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'turbo DNase buffer', ' RNA extruction Magmax kit', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'RNA binding bead', ' RNA extruction Magmax kit', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Elution buffer', ' RNA extruction Magmax kit', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'elution buffer  (opened)', ' RNA extruction Magmax kit', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'turbo DNase buffer (opened)', ' RNA extruction Magmax kit', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'G-OFprimer (Glurp) 2.5 nMol', 'Genotyping Pf', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'OuterG4 for GLURP FW', 'Genotyping Pf', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'G-OR 2.5 nMol', 'Genotyping Pf', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'OuterG5 for GLURP RV ', 'Genotyping Pf', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Nested S4 for MSP-2 FW', 'Genotyping Pf', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Nested S1 for MSP-2 FW', 'Genotyping Pf', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'MSP-2-S2 FW', 'Genotyping Pf', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'MSP-2-S3 RV', 'Genotyping Pf', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'MSP-2-S1 tail fw', 'Genotyping Pf', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'MSP2-FC27 RV (FAM dye labeled)', 'Genotyping Pf', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'MSP2-3D7-RV (VIC dye labeled)', 'Genotyping Pf', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'M1-OR 2.5 nMol (MSP1M1OR)', 'Genotyping Pf', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'MSP1 M1 RV 2.5 nMol', 'Genotyping Pf', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'MSP1M1-FW 2.5 nMol', 'Genotyping Pf', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'MSP1M1-OF 2.5 nMol', 'Genotyping Pf', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'MSP1M1-KF 2.5 nMol', 'Genotyping Pf', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'MSP1M1-KR 2.5 nMol', 'Genotyping Pf', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Nested N1 for MSP1 FW', 'Genotyping Pf', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Nested N2 for MSP1 RV', 'Genotyping Pf', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'N2RPfmdr86', 'Genotyping Pf', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'N2RPfcrt', 'Genotyping Pf', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'N1FPfcrt', 'Genotyping Pf', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'N1FPfmdr1034', 'Genotyping Pf', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT '96 DNA multi-sample kit 96 rxn', 'Mag Max ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT '96 DNA multi-sample kit', 'Mag Max ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT '96 DNA multi-sample kit 96rxn', 'Mag Max ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT '96 Blood RNA isolation kit (TURBO DNase & lysis binding inhancer)', 'Mag Max ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'RNase A enzyme 1mg/ml', 'Mag Max ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'proteinase K 100 mg/dl', 'Mag Max ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Mag max RNA isolation kit (used)', 'Mag Max ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Dna /Rna index', 'Sequensing ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Dna prep PCR BUFFER', 'Sequensing ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Nextseq Accessory box v2', 'Sequensing ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Platinium Taq Dna polymerase', 'Sequensing ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pf 18s probe', 'Sequensing ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pv 18s probe', 'Sequensing ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'dNTPs mix 10mM', 'Sequensing ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'KAPA libarary Quant kit', 'Sequensing ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'nextseq 500/550 cartirage v2', 'Sequensing ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'nextseq 500/550 cartirage v3', 'Sequensing ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Iseq 100 reagent cartridge', 'Sequensing ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'HF DNA polymerase', 'Sequensing ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Dna /Rna index set B', 'Sequensing ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Dna /Rna index set A', 'Sequensing ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Dna /Rna index set D', 'Sequensing ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'resuspension buffer', 'Sequensing ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Qubit RNA IQ assay kit', 'Sequensing ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Tagmentation buffer 1', 'Sequensing ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'enhanced PCR mix', 'Sequensing ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'phix control V3', 'Sequensing ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Cubit dsDNA HS Assay kit ', 'Sequensing ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'AMPure XP', 'Sequensing ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Nuclease free water', 'Sequensing ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT '1Kb DNA ladder ', 'Sequensing ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Bio analyzer-High sensitivity DNA reagents ', 'Sequensing ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'DNA prep tagmentation beads', 'Sequensing ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'sample purification beads', 'Sequensing ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'DNA prep beads+buffers', 'Sequensing ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'nextseq 500/550(mid output flow cell cartridge v2,5)', 'Sequensing ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Taqman universal pcr mastermix', 'Sequensing ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Taqman fast advansed mastermix', 'Sequensing ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Proteinase K', 'Sequensing ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'i1 flow cell', 'Sequensing ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'tagment stop buffer', 'Sequensing ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'qubit dsDNA HS standard #2', 'Sequensing ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'mgcl2', 'Sequensing ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'kb Extender', 'Sequensing ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'turbo DNase buffer', 'Sequensing ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'RNA binding bead', 'Sequensing ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Elution buffer', 'Sequensing ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'elution buffer  (opened)', 'Sequensing ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'corning pipet tips 0.1-10 ul ', 'Sequensing ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'collection tubes 2 ml', 'Sequensing ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'QIAamp mini spin colomn', 'Sequensing ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Qiagen Exraction kit', 'Sequensing ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'tris-hydro chloride 1M', 'Sequensing ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'tagmentation wash buffer', 'Sequensing ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Nextseq500/550 buffer cartrige V2', 'Sequensing ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'PCR tubes 12 strips of 8 tubes ', 'Sequensing ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Qiagen proteinase K', 'Sequensing ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Qubit Assay tubes', 'Sequensing ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'PCR Amplitube caps', 'Sequensing ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Amplitubes PCR rxn strips', 'Sequensing ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'PCR plate 96 well', 'Sequensing ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'sample (DNA elution) storage tube', 'Sequensing ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Quick gel extraction & PCR Purification combo kit', 'Sequensing ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Bio analyzer-High sensitivity DNA chips', 'Sequensing ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Bio analyzer-syringe kit', 'Sequensing ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Top vision Agrose', 'Sequensing ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Qubit 1x ds DNA HS assay kit', 'Sequensing ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Qubit 1x dsL DNA BR assay standard is ', 'Sequensing ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'AmpureXP', 'Sequensing ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Hot start MasterMix, high fidelity', 'Sequensing ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Q5 Blood Direct MasterMix', 'Sequensing ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'cpbF (alpha DNA)', 'leshimaniasis', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'cpbEF-F  (alpha DNA)', 'leshimaniasis', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'B4 (alpha DNA)', 'leshimaniasis', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'K26 f 15pM', 'leshimaniasis', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'T2 (alpha DNA)', 'leshimaniasis', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'seqcpbr2 (apha DNA)', 'leshimaniasis', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Cpbef frd (alpha DNA)', 'leshimaniasis', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'seqcpbf1 (alpha DNA)', 'leshimaniasis', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'cpbEFrev (alpha DNA)', 'leshimaniasis', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'cpbEF R  (alpha DNA)', 'leshimaniasis', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'dNTPs 10mM', 'leshimaniasis', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'vsp15pM', 'leshimaniasis', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'one-4 phor-all buffer + 10x conc.', 'leshimaniasis', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'taq DNA poly', 'leshimaniasis', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'k26r (alpha DNA)', 'leshimaniasis', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'LITSR (alpha DNA)', 'leshimaniasis', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'L%.8s  (alpha DNA)', 'leshimaniasis', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Laev 10 R', 'leshimaniasis', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'cpb TAG', 'leshimaniasis', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'cpbr (alpha DNA)', 'leshimaniasis', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'cpb ATG (alpha DNA)', 'leshimaniasis', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'big dye terminator v 3.1 cycle sequencing RR-24', 'leshimaniasis', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Ilri-161108F1', 'leshimaniasis', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'pgem3Zf(+)', 'leshimaniasis', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'm13(-21)primer', 'leshimaniasis', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT '202GA-Rv', 'G6PD Primer', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT '202GA-Fw', 'G6PD Primer', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Tailing Segment FW', 'RFLP drug Resistance primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Tailing Segment RV', 'RFLP drug Resistance primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'hrp2 one-step FW', 'RFLP drug Resistance primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'hrp2 one-step RV', 'RFLP drug Resistance primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'N1FPfcrt FW', 'RFLP drug Resistance primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'N1FPfcrt RV', 'RFLP drug Resistance primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'N2FPfcrt FW', 'RFLP drug Resistance primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'N2FPfcrt RV', 'RFLP drug Resistance primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'N1-Mdr1-86Y Fw', 'RFLP drug Resistance primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'N1-Mdr1-86Y Rv', 'RFLP drug Resistance primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'N2-Mdr1-86Y FW', 'RFLP drug Resistance primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'N2-Mdr1-86Y Rv', 'RFLP drug Resistance primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'N2-mdr1-86Y FW', 'RFLP drug Resistance primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'N2-mdr1-86Y RV', 'RFLP drug Resistance primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'M1-OF', 'RFLP drug Resistance primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'M1-OR', 'RFLP drug Resistance primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'RNaseP-F', 'RFLP drug Resistance primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'RNaseP-R', 'RFLP drug Resistance primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'msp-1(N)-FW', 'RFLP drug Resistance primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'msp-1(N)-RV', 'RFLP drug Resistance primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'msp-1(N1)-FW', 'RFLP drug Resistance primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'msp-1(N1)-RV', 'RFLP drug Resistance primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'msp-outer-F', 'RFLP drug Resistance primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'msp-outer-R', 'RFLP drug Resistance primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT '3D7/ICfamily(N-2)-FW', 'RFLP drug Resistance primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'msp2-S2-fw', 'RFLP drug Resistance primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'msp2-S3-rev', 'RFLP drug Resistance primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'msp-2(N1)-FW', 'RFLP drug Resistance primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'msp-2(N1)-Rv', 'RFLP drug Resistance primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'msp-2(N1)-RV', 'RFLP drug Resistance primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'msp2-S2-F', 'RFLP drug Resistance primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'msp2-S3-R', 'RFLP drug Resistance primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'msp2-S1Tail-fw', 'RFLP drug Resistance primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'msp2-S1Tail-f', 'RFLP drug Resistance primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'FC27family(N2)-FW', 'RFLP drug Resistance primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'FC27family(N2)-RV', 'RFLP drug Resistance primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'msp1-k1-F', 'RFLP drug Resistance primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'msp1-k1-R', 'RFLP drug Resistance primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'msp1-Mad20-F', 'RFLP drug Resistance primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'msp1-Mad20-R', 'RFLP drug Resistance primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Fc27', 'RFLP drug Resistance primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT '3D7', 'RFLP drug Resistance primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'msp2-3D7-N5-RV-probe', 'RFLP drug Resistance primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'msp2-FC27-MS-RV-probe', 'RFLP drug Resistance primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Nuclase free water', 'unknowns ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'NOT I', 'unknowns ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'NE buffer 3.1  ', 'unknowns ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'PhHv10^3', 'unknowns ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'PhHv-26267s ', 'unknowns ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'probephhv-305TQ-cy5', 'unknowns ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'phhv-337as', 'unknowns ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'ENDFORWARD (Genosys 7112-012)', 'unknowns ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'ENDREVERSE (gENOSYS 7112-013)', 'unknowns ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT '563CT- Fw', 'unknowns ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'DNase I, RNase free', 'unknowns ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT '10X reaction buffer with MgCl2 for DNase I', 'unknowns ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'MnCl2', 'unknowns ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'reaction buffer with out MnCl2 for DNase I', 'unknowns ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'EDTA', 'unknowns ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'cell proliferatio kit I (MTT)', 'unknowns ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'RQ1R  DNase, stop solution', 'unknowns ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'RQ1 DNase, 10x rxn buffer', 'unknowns ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'RQ1R  RNase freeDNase', 'unknowns ', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'PV25probe', 'plasmid and probe', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'PV_DBP_probe', 'plasmid and probe', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'CCp4 probe', 'plasmid and probe', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'pfMGET', 'plasmid and probe', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Ps18S', 'plasmid and probe', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'QIAcuity probe PCR kit', 'Digital PCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Probe PCR Kit 25ml', 'Digital PCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Probe PCR Kit 50ml', 'Digital PCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Probe PCR Kit 15ml', 'Digital PCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'varATS ', 'Digital PCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT '2E12F1', 'Parasite PvPf Genotyping Primer', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT '2E12R1', 'Parasite PvPf Genotyping Primer', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT '2E12F', 'Parasite PvPf Genotyping Primer', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT '2E12R', 'Parasite PvPf Genotyping Primer', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT '2E2F', 'Parasite PvPf Genotyping Primer', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT '2E2R1', 'Parasite PvPf Genotyping Primer', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT '2E2F1', 'Parasite PvPf Genotyping Primer', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT '2E2R', 'Parasite PvPf Genotyping Primer', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'BRAVO_F', 'Parasite PvPf Genotyping Primer', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'BRAVO_R', 'Parasite PvPf Genotyping Primer', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'R1(2)', 'Parasite PvPf Genotyping Primer', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'F1 (M1-F1)', 'Parasite PvPf Genotyping Primer', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'R2', 'Parasite PvPf Genotyping Primer', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'F2', 'Parasite PvPf Genotyping Primer', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Out PCR barcoding fw', 'Parasite PvPf Genotyping Primer', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Out PCR barcoding rv', 'Parasite PvPf Genotyping Primer', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'M1-R1', 'Parasite PvPf Genotyping Primer', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'M1-IR', 'Parasite PvPf Genotyping Primer', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Tailing Segment FW', 'Parasite PvPf Genotyping Primer', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Tailing Segment RV', 'Parasite PvPf Genotyping Primer', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'hrp2 one-step FW', 'Parasite PvPf Genotyping Primer', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'hrp2 one-step RV', 'Parasite PvPf Genotyping Primer', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'N1FPfcrt FW', 'Parasite PvPf Genotyping Primer', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'N1FPfcrt RV', 'Parasite PvPf Genotyping Primer', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'N2FPfcrt FW', 'Parasite PvPf Genotyping Primer', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'N2FPfcrt RV', 'Parasite PvPf Genotyping Primer', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'N1-Mdr1-86Y Fw', 'Parasite PvPf Genotyping Primer', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'N1-Mdr1-86Y Rv', 'Parasite PvPf Genotyping Primer', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'N2-Mdr1-86Y FW', 'Parasite PvPf Genotyping Primer', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'N2-Mdr1-86Y Rv', 'Parasite PvPf Genotyping Primer', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'N2-mdr1-86Y FW', 'Parasite PvPf Genotyping Primer', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'N2-mdr1-86Y RV', 'Parasite PvPf Genotyping Primer', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'M1-OF', 'Parasite PvPf Genotyping Primer', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'M1-OR', 'Parasite PvPf Genotyping Primer', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pfmdr11246-B', 'Parasite PvPf Genotyping Primer', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pfmdr11246-A', 'Parasite PvPf Genotyping Primer', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pfmdr1_RV', 'Parasite PvPf Genotyping Primer', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pfmdr1_FW', 'Parasite PvPf Genotyping Primer', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pf--tubulin_FW', 'Parasite PvPf Genotyping Primer', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pf--tubulin_RV', 'Parasite PvPf Genotyping Primer', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pfplasmepsin2__FW', 'Parasite PvPf Genotyping Primer', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pfplasmepsin2__RV', 'Parasite PvPf Genotyping Primer', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pfmdr1246-D1', 'Parasite PvPf Genotyping Primer', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pfmdr1246-D2', 'Parasite PvPf Genotyping Primer', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pfmdr1246-A', 'Parasite PvPf Genotyping Primer', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pfmdr1246-B', 'Parasite PvPf Genotyping Primer', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'RV11', 'Parasite PvPf Genotyping Primer', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'RV12', 'Parasite PvPf Genotyping Primer', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT '8633F', 'Parasite PvPf Genotyping Primer', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT '9211R', 'Parasite PvPf Genotyping Primer', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT '8945F', 'Parasite PvPf Genotyping Primer', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT '9577R', 'Parasite PvPf Genotyping Primer', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT '8669F', 'Parasite PvPf Genotyping Primer', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT '9541R', 'Parasite PvPf Genotyping Primer', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'RV12-2', 'Parasite PvPf Genotyping Primer', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pv210-Pc', 'Immunoassay', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'pf-pc2', 'Immunoassay', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'pv-210-6', 'Immunoassay', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'pf-pc5', 'Immunoassay', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'pf-pc1', 'Immunoassay', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'pv-210-5', 'Immunoassay', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'pf-pc4', 'Immunoassay', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'pv-210-2', 'Immunoassay', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'pv-210-1', 'Immunoassay', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'pf-pc6', 'Immunoassay', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'pv-210 pc ELISA(stock)', 'Immunoassay', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'pv-247 pc ELISA(stock)', 'Immunoassay', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pf-PC', 'Immunoassay', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'PV-210-3', 'Immunoassay', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pf-PC3', 'Immunoassay', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'pf-210-4', 'Immunoassay', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'New Naive-serum', 'Immunoassay', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'conjugate m Ab pf', 'Immunoassay', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'pf-pc csp', 'Immunoassay', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'pv hrp swp', 'Immunoassay', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'pv-210-pc ELISA(stock)', 'Immunoassay', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'pv247-pc', 'Immunoassay', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'pv capture AB', 'Immunoassay', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'pf capture AB', 'Immunoassay', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'pv-210-1 m ab', 'Immunoassay', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'pv-210 2nd mab', 'Immunoassay', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'pv-219 pc ELISA', 'Immunoassay', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'CCP4-RV', 'Recieved from CDC', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'BCFB FY2021', 'Recieved from CDC', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'PfMGET-FW', 'Recieved from CDC', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'PfMGET-RV', 'Recieved from CDC', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'rPLU-5', 'Recieved from CDC', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pv18s-RV', 'Recieved from CDC', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'rVIV-1', 'Recieved from CDC', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pv525-RV', 'Recieved from CDC', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'rPLU-6', 'Recieved from CDC', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'PV525-FW', 'Recieved from CDC', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pf 18s-FW', 'Recieved from CDC', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'rFAL-2', 'Recieved from CDC', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'PV18S probe', 'Recieved from CDC', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'rVIV-2', 'Recieved from CDC', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'rFAL-1', 'Recieved from CDC', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'PfMEGT probe', 'Recieved from CDC', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pf 18s turbo probe', 'Recieved from CDC', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'CCP4 probe', 'Recieved from CDC', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'PF18S RV', 'Recieved from CDC', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'PVS25 Probe', 'Recieved from CDC', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'PV18S Probe', 'Recieved from CDC', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'E-COLI', 'Recieved from CDC', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'New N-serum', 'Recieved from CDC', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'E-COLI Lysate', 'Recieved from CDC', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'WHO PF standard(+ve control)', 'Recieved from CDC', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'PF +VE control(cp3 NEAT)', 'Recieved from CDC', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'cp3(1:1)(50%)', 'Recieved from CDC', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Et-pf pooled serum', 'Recieved from CDC', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'E-COLI Lysate(5.2mg/ml)', 'Recieved from CDC', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'CP3 1:10', 'Recieved from CDC', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'S1PV +VE control', 'Recieved from CDC', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Hot start Taq 2x Master Mix 500rxn', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Dream Taq Master Mix 2x', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'PCR buffer+green+white+mgcl', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'DNA ladder 100bp with buffer', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Invitrogen DNA ladder 50bp with buffer', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'GoTaq nPCR componenet ', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'GoTaq 100nM dNTPs set', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Bio-dNTP Mix ', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'MBL-dNTP Mix ', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Thermoscintific- dNTP Mix ', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'GoTaq dNTP set', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT '100bp DNA ladder ', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'PCR Nucleotide Mix', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'rplu5-100mM', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'rplu6-100mM', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'rrplu1-100mM', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'rplu3-100mM', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'rplu4-100mM', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'rviv1-100mM', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'rviv2-100mM', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'rfal2-100mM', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'rfal1-100mM', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'rmal1-100mM', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'rmal2-100mM', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'rova1-100mM', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'rova2-100mM', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pf - Rv (rFAL2)', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pf-fw (rFAL1)', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pv Rv (rVIV 2)', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pv Fw (rVIV 1)', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'rPLU 6 forward', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'rPLU 5 reverse', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'dCTP-100mM', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'dTTP-100mM', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'dATP-100mM', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'dGTP-100mM', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'dNTP Mix -2.5mM', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'dNTP MIX -10mM', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'dATP-100mM   100umol', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'dCTP-100mM  100umol', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'dGTP-100mM  100umol', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'dTTP-100mM  100umol', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'dNTP MIX with dUTP-12.5mM', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'dNTP MIX with dTTP-10mM', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'dNTP mix 10mM', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'AF1III ', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'APoI', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'N1FPfcrt-100mM', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'N2FPfcrt-100mM', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'N1FPfcrt RW-100mM', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'N1FPfcrt FW-100mM', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'N2FPfcrt RW-100mM', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'N2FPfcrt FW-100mM', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'green Go Taq buffer', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'MgCl2 -25mM', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'colorless Go Taq buffer', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'GO Taq G2 flexi DNA polymerases 5 u/ul', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'HOT Start Taq 2X master mix (500 rxn / vial )', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'dNTP set 4x25 umol', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'PCR nucleotide mix 10mM', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'flexi DNA polymerase', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'GO Taq G2 flexi DNA polymerases 5u/ul', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Go Taq flexi DNA polymerase', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'NPCR components (Go Taq)', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Flexi Go Taq DNA polymerase', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'F-Rv+Fw', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'O-Rv+Fw', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'M-Rv+Fw', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'V-Rv+Fw', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'K-Rv+Fw', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'P-Rv+Fw', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'rCutSmart Buffer', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'gel loading dye purple', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'NEBuffer', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'HhaI', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'EcoRI-HF', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'BgI-II', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Dde-I', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pst-I', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'BIO Taq DNA polymerase', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'NH4 reaction buffer, MgCl2 free', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'MgCl2 ', 'nPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'RQ1 RNase-Free DNase', 'Purification (DNARNA)', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'DNase I, RNase-free', 'Purification (DNARNA)', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'RNase inhibitor', 'Purification (DNARNA)', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'High capacity cDNA Reverse T kit', 'RT-qPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'One step RT-qPCR kit 2500rxn', 'RT-qPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'One step RT-qPCR kit 500rxn', 'RT-qPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'One step RT-SuperMix kit 25rxn', 'RT-qPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Luna warm start RT enzyme mix, 20x conc', 'RT-qPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Luna universal probe 1 step reaction mix 2x conc', 'RT-qPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'GO Taq PCR master mix 2X c0nc', 'RT-qPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'MQ', 'RT-qPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Luna One step RT-qPCR kit 2,500 rxn', 'RT-qPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'High capacity cDNA rt kit', 'RT-qPCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'UD2-R_2_P.PIGNATELL', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'St-F_2_P.PIGNATELL', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'U5.8S-F_2_P.PIGNATELL', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'ITS2-steph-R_P.PIGNATELL', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'ITS2A_P.PIGNATELL', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'ITS2B_mod2_P.PIGNATELL', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'OVM_115402_C8', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'OVM_115394_C4', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pfr364af_fw', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pfr364af_rb', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pvdhfrv_fw', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pvdhfrv_rv', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Hrp2 exon 2 fwd', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Hrp2 exon 2 rev', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Hrp3 rev ', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Hrp3 fwd', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Trna rev', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Trna fwd', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Hrp2 exon 2 probe', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Hrp2 probe', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Hrp3 probe', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Trna probe', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pvmsp2_n_rev', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pvmsp2_p_fwd', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pvmsp2_n_fwd', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pvmsp2_p_rev', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pvmsp1f3_p_rev', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pvmsp1f3_n_fwd', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pvmsp1f3_p_fwd', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pvmsp1f3_n_rev', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Msp2_s1 tail_fwd', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Msp2_fc27_m5_rev', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Msp2_3d7_n5_rev', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pfmsp2_s2_fw', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pfmsp2_s3_rv', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'ed protein (hyp8) rv', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT '563ct_rv', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'rPLU3', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'rPLU4', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'TARE_2_fw', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'TARE_2_rv', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'RESA rv', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'RESA fw', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pfs25_fw', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'rPLU1', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT '376AG fw', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT '376AG rv', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pv18s_ fw', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pv18s_ rv', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pmal_qPCR_ fw', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pmal_qPCR_ rv', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pova_qPCR_fw', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pova_qPCR_rv', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'PgMET_fw', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'PgMET_rw', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pvs25 fw', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pvs25 rev', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'QMAL fw', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'QMAL rv', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pfs230p_fw', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pfs230p_rv', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'varATS_fw', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'varATS_rv', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pf18S RV 100uM', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'PV18S FW MPX 100uM', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pf18S probe 100uM', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pv18S MPX 100uM', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'PV18S RV 100uM  ', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pf18s FW 100uM', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'PV25 probe 5.3 nMol', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'PfMDR1 probe 5.1 nMol', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Beta TT probe 5.2 nMol', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pf plasmepsin2 probe 5.3 nMol', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Viv f(shako) ', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Fal-F(shako)', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pf18S shoko probe 5.3 nMOL', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pv18s SHOKO probe ', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Plasmo rev 100UM Shoko (for Pv- Pf mpx)', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pv 18S REV', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pv-25 Fw', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pv-25 Rev', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pv-25 Rv', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pvs25 Fw', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pv-18s new short', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pf-18s- DNA-Rv', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pf-18s- DNA-Fw', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'StD2', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'GATA-1F', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'GATA-1R', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'hrp2-Exon-FW', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'tRNA-FW', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'hrp3-FW', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'uq-R', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'UD2-R', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'St-F', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'StD2-R', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'us-85-F', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'st-F', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'tRNA-Rv', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'hrp2-Exon2-RV', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'HumTuBB-R', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Uq-R', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'U5.8S-F', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pfldh-F', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pfldh-R', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'HumTubBB-F', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'stq-F', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'stq-R', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'uq-F', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pf Hrp3-R2', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Hrp3-Rv', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'PfHrp2-F1', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'PfHrp2-R3', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'PfHrp2-F2', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'PfHrp3-R1', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'PfHrp2-R2', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pf Hrp3-F1', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pf Hrp3-P1', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'PfHrp2-R1', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'PfHrp3-F2', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'PfHrp2-F3', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pfhrp3_F2', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pfhrp3_R2', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'GATA-MT-probe', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'GATA-WT-probe', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT '2-probe', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'tRNA-probe', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'hrp3-probe', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pfhrp2-probe', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pfidh-probe', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pfhrp3-probe', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Stq-P probe', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Uq-P probe', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'HumanTuBB-P probe', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pfhrp3_probe', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pf18s-Fw (Nij)', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pv18s-Fw', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pv18s-fw', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pf18s-Rv (Nij)', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pv18s-rv', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'PfMGET-fw', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'PfMGET-Rv', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'PfMGET- FW', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'PfMGET- Rev multiplex', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pf18s-RNA-fw', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pv18s -fw', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pv18s-Rv', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pf18s-RNA-RV', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pfs25_FW', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pfs25_Rv', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'SBP1-RV', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'SBP1-Fw', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pfs25-Rv', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pf18s-DNA-RV(Swit)', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pf18s_Nji_RV', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pf18s-Rv', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pf18s-DNA-FW(Swit)', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pf18s_Nji_FW', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pf18s Rev', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pf18s for', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pv-18s Fw', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pv-18s Rv', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'CCp4 Rev', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'CCp4 Fw', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pfs25-FW', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'PfMGET probe (FAM)', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'CCP4 probe TEXAS-RED)', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pvs25 probe (FAM)', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pv18s probe (HEX)', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pv18s probe (FAM)', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'ITS2B-mod2, 20.5nmol', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'PAR, 24.7nmol', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'UN, 19.8nmol', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Stq-F, 21.2nmol', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'LESS, 22.8', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'RIV,23.0 nmol', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'UD2, 22.3nmol', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'VAN, 22.5nmol', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'FUN,19.9nmol', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'ITS2A, 19.5nmol', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'ITS2-steph-R, 22.2nmol', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT '02Ga FW 100uM', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT '63CT FW 100 uM', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT '02GA RV 100 uM', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT '63CT RV 100 uM', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT '76AG FW 100uM', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT '76AG RV 100uM', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'N1RPFMDR1034 100UM', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'N2FPFMDR1034 100UM', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'N1FPFMDR1034', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'N2RPFMDR1034', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'N1FPFMDR86 100UM', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'N1RPFMDR86 100UM', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'N2FPFMDR86 100UM', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'N2RPFMDR86 100UM', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Ovis F 134.9 nMol (Sheep)', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Can 2F 125 nMol', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'ME (1) 143.5 nMol', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'CAN 2R (2) 161.6 nMol', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'UN (1) 139.8 nMol', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Ovis 2R (2) 140.4 nMol (sheep)', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Hmn F (2) 126.3 nMol', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'AR (1) 130.4 nMol', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Sus F 145.9 nMol', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Bos 2F (2) 149nMol', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Bos 2R 122.2 nMol', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Hmn R (1) 145.5 nMol', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Sus R (2) 147.5 nMol', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'ITS 2A(2) 125.7nMol', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'ITS2-Steph-R(2) 143.2 nMol', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'GA 170.3 nMol', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Hco2 198 (1) 113.7 nMol', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'LCO 1490(2) 99.7 nMol', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'S200x6.1-F 132.3 nMol', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Cap3R 126.8 nMol', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'plsR (revers primer)(1) 644.4 nMol', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'S200X6.1-R(2) 157.2 nMol', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'PlsF(forward primer)(1) 738.6 nMol', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Cap3F 136.8 nMol', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'QD(1) 134.9 nMol', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'N1RPFmdr86 41.9.nMol', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'N2RPVmdr1 76.4.nMol', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'N1RPFcrt 41.1 nMol', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'N1FPFmdr86 47.0.nMol', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'N2RPFmdr1034 43.9 nMol', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'N2FPFmdr 1034 61.5 nMol', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'N2 FPFmdr86 39.1nMol', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'N2FPFcrt 40.6nMol', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'N1RPVmdr1 77.7nMol', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'N1FPVmdr1 75.5nMol', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'N1RPFmdr1034 61.2 nMol', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'N2FPVmdr1 60.9nMol', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'MDR3 (sense) for PFMDR1', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'MDR4 (anti-sense) for PFMDR1', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'MDR3 (sence) ', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'PV-25 FW', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'PV-25 RV', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'CCp4 FW', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'PF MGET ', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'CCp4RV', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'CCp4 RV', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pf MGET RV', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'CCp4 probe ', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pf MGET probe', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'M1-RR primer 2.5 nMol', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'M1-RF primer 2.5 nMol', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'MGET FW 50uM', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'MGET RV 50uM', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'CCp4 forward reverse 50uM', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'CCp4 forward  50uM', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'CCp4 reverse 50uM', 'Primers', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'HumTuBB RV 100mM', 'Conventional PCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'HumTuBB FW 100mM', 'Conventional PCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'COX1 Plasmo F ', 'Conventional PCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'COX1 Plasmo R', 'Conventional PCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Cow 121 F', 'Conventional PCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Dog 368 F', 'Conventional PCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Human 741 F', 'Conventional PCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Pig 573 F', 'Conventional PCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Got 894 F', 'Conventional PCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'UnRev1025', 'Conventional PCR', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT '100 bp ladder (ready made)', 'DNA ladder', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Ultera low range DNAladder, 0.5ug/ul', 'DNA ladder', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Trakit cyan/yellow loading buffer, 6x', 'DNA ladder', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'Ultera low range DNAladder, 0.5ug/ul (used)', 'DNA ladder', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT '100bp DNA ladder', 'DNA ladder', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'blue/orange loading day 6X conc', 'DNA ladder', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'gel loading dye purple 6X conc', 'DNA ladder', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'gel loading dye Blue/orange 6X conc', 'DNA ladder', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT '100bp DNA lader with loadind dye(Blue/orange 6x)', 'DNA ladder', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'cyan/orange loading buffer', 'DNA ladder', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT '50bp DNA ladder', 'DNA ladder', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT '5x DNA loading buffer (blue)', 'DNA ladder', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'blue/orange 6x oading dye', 'DNA ladder', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'ultra low rabge DNA ladder', 'DNA ladder', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT '1kb + DNA lader', 'DNA ladder', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'hyper ladder 100bp 100 lans', 'DNA ladder', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'primer IPCF -2.5 mM', 'Mosquito Id', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'primer west -26uM', 'Mosquito Id', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'primer WT (west) 25uM', 'Mosquito Id', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'primer WT (east) ', 'Mosquito Id', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'primer east ', 'Mosquito Id', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'primer ALT rev ', 'Mosquito Id', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'An.gambiae RSP-ST', 'Mosquito Id', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'An.coluzzii AKDR', 'Mosquito Id', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'ITS2A (1) ', 'Mosquito Id', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'ITS2-Steph-R(1) ', 'Mosquito Id', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'QD', 'Mosquito Id', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'UN', 'Mosquito Id', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'GA', 'Mosquito Id', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'AR', 'Mosquito Id', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'BW', 'Mosquito Id', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'QDA', 'Mosquito Id', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'ME', 'Mosquito Id', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'GA RV', 'Mosquito Id', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'AR RV', 'Mosquito Id', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'QDA RV', 'Mosquito Id', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'ME RV', 'Mosquito Id', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'QD RV', 'Mosquito Id', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'UN FW', 'Mosquito Id', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'IMP-S1', 'Mosquito Id', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'QD-3T', 'Mosquito Id', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'IMP-UN', 'Mosquito Id', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'IMP-M1', 'Mosquito Id', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'AR-3T', 'Mosquito Id', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'GA-3T', 'Mosquito Id', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'ME-3T', 'Mosquito Id', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'An. arabiensis Dong 5 F211', 'Mosquito Id', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'An. merus, Maf F185', 'Mosquito Id', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
        UNION ALL
SELECT 'An. quadriannus, Sangwe, F180', 'Mosquito Id', 'Malaria and Neglected Tropical Disease (MNTD) Laboratory'
)
INSERT INTO clinlims.inventory_item (
    id, fhir_uuid, name, description, project_name, item_type, units, is_active, 
    stability_after_opening, concentration, storage_requirements, last_updated, version
)
SELECT
    nextval('clinlims.inventory_item_seq'),
    gen_random_uuid(),
    cd.name,
    cd.name || ' (' || cd.category || ')',
    cd.project_name,
    'REAGENT',
    'unit',
    'Y',
    30,
    'Standard concentration',
    '2-8°C',
    NOW(),
    1
FROM catalog_data cd(name, category, project_name);

-- Insert Lots
WITH lot_data AS (
    SELECT 'MgCl2 10xPCR rxn buffer', 'Ref Y02028', 1.0, '300ul', '2026-12-31'
        UNION ALL
SELECT 'MgCl2 10xPCR rxn buffer', 'Ref Y02028', 1.0, '1.2ml', '2026-12-31'
        UNION ALL
SELECT 'KB extender', 'Ref 100017874', 1.0, '1.3ml', '2026-12-31'
        UNION ALL
SELECT 'Pfcrt-Rv ', 'Code:3550)', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pfcrt-Fw', 'Code:3551)', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'K13-Fw', 'Code:3549)', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'K13-Rv', 'Code:3552', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pf mdr1-Rv', 'Code:3554', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pf mdr1-Rv', 'Code:3553', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pfcrt-Fw ', 'Code:198', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pfcrt-Rv ', 'Code:199', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pfdhps-Rv', 'Code:3560', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pfdhps-Fw', 'Code: 3559', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pfcrt-F1', 'LOT-435b5e0b', 1.0, 'Received reconstitute', '2026-12-31'
        UNION ALL
SELECT 'Pfcrt-R1 sequence', 'LOT-7760bb9f', 1.0, 'Received reconstitute', '2026-12-31'
        UNION ALL
SELECT 'Pfdhps-F', 'LOT-a72c0040', 1.0, 'Received reconstitute', '2026-12-31'
        UNION ALL
SELECT 'Pfdhps-R', 'LOT-a4391fc1', 1.0, 'Received reconstitute', '2026-12-31'
        UNION ALL
SELECT 'plasmepsin 3R', 'LOT-7dfe07cb', 1.0, '', '2021-12-20'
        UNION ALL
SELECT 'plasmepsin 2R', 'LOT-5aaaec75', 1.0, '', '2021-12-21'
        UNION ALL
SELECT 'plasmepsin 2F', 'LOT-1f875cbd', 1.0, '', '2021-12-22'
        UNION ALL
SELECT 'plasmepsin 3F', 'LOT-04483e47', 1.0, '', '2021-12-23'
        UNION ALL
SELECT 'plasmepsin 3P', 'LOT-0597c2b4', 1.0, '', '2021-12-24'
        UNION ALL
SELECT 'plasmepsin 2P', 'LOT-152ecb26', 1.0, '', '2022-01-22'
        UNION ALL
SELECT 'Pf 18s fwd', 'LOT-b9c413fa', 2.0, '', '2021-12-20'
        UNION ALL
SELECT 'Pf 18s rev', 'LOT-c4b31fd1', 2.0, '', '2021-12-21'
        UNION ALL
SELECT 'Beta tubulin F', 'LOT-7afb0b62', 1.0, '', '2021-12-22'
        UNION ALL
SELECT 'beta tubulin R', 'LOT-b8755ae4', 1.0, '', '2022-01-21'
        UNION ALL
SELECT 'beta tublin P', 'LOT-0b3865f0', 1.0, '', '2022-01-22'
        UNION ALL
SELECT 'cyt B-F1', 'LOT-1e232e16', 1.0, '', '2022-01-23'
        UNION ALL
SELECT 'cyt B-R1', 'LOT-b72003d4', 1.0, '', '2021-12-22'
        UNION ALL
SELECT 'mitochndorion -R1', 'LOT-1584a4ad', 1.0, '', '2021-12-23'
        UNION ALL
SELECT 'mitochndorion -F1', 'LOT-ff22ede7', 1.0, '', '2021-12-24'
        UNION ALL
SELECT 'Pf k13-F1', 'LOT-fa60e7b5', 1.0, '', '2021-12-25'
        UNION ALL
SELECT 'Pf k13-R1', 'LOT-fc3d3e5c', 1.0, '', '2021-12-26'
        UNION ALL
SELECT 'pfmdr1 F', 'LOT-f7cbb7ab', 2.0, '', '2021-12-27'
        UNION ALL
SELECT 'pfmdr1 R', 'LOT-b9c00e48', 2.0, '', '2021-12-28'
        UNION ALL
SELECT 'FastDigest Alul enzyme with buffer ', 'LOT-dad6d7aa', 1.0, '', '2020-12-02'
        UNION ALL
SELECT 'SmartCut enzyme with buffer ', 'LOT-babb7cee', 1.0, '', '2018-11-01'
        UNION ALL
SELECT 'mbo II 5000 U/ml', 'LOT-0d3fb70a', 1.0, '0.3', '2017-05-31'
        UNION ALL
SELECT 'Nla III  10,000 U/ml', 'LOT-2ab430f5', 2.0, '0.25', '2017-07-30'
        UNION ALL
SELECT 'Ase I  10,000 U/ml', 'LOT-036df5df', 1.0, '0.2', '2017-04-30'
        UNION ALL
SELECT 'Fok I 5,000 U/ml', 'LOT-d326a6f9', 1.0, '1.0', '2018-01-30'
        UNION ALL
SELECT 'Fok I 5,000 U/ml (cutsmart)', 'LOT-b450484b', 1.0, '1.0', '2018-11-30'
        UNION ALL
SELECT 'Dra I  20,000 U/ml', 'LOT-40a69f19', 1.0, '0.1', '2017-02-01'
        UNION ALL
SELECT 'ApoI 10,000 U/ml', 'LOT-8c7e750f', 1.0, '0.1', '2017-03-01'
        UNION ALL
SELECT 'Dde I   10,000 U/ml', 'LOT-a1259a94', 1.0, '0.1', '2017-03-02'
        UNION ALL
SELECT 'Eco RV 20,000 U/ml', 'LOT-b41b4d19', 1.0, '0.2', '2016-08-01'
        UNION ALL
SELECT 'NEBuffer  3.1  10x conc.', 'LOT-2dad79b3', 5.0, '1.25', '2018-10-01'
        UNION ALL
SELECT 'NEBuffer  4  10x conc.', 'LOT-ccd60e9a', 2.0, '5.0', '2015-03-01'
        UNION ALL
SELECT 'Afa I  1,000 U/ml', 'LOT-8f88099a', 1.0, '', '2011-11-01'
        UNION ALL
SELECT 'Pvu II 15U/ml', 'LOT-cd9c2815', 1.0, '3000.0', '2007-11-01'
        UNION ALL
SELECT '10x M buffer', 'LOT-ede478df', 3.0, '1.0', '2026-12-31'
        UNION ALL
SELECT 'cutsmart buffer  10x conc.', 'LOT-ea744455', 3.0, '1.25', '2018-07-01'
        UNION ALL
SELECT 'Qiagen rnase free water', 'LOT-111faed1', 4.0, '1.9', '2026-12-31'
        UNION ALL
SELECT '1KB + DNA ladder', 'LOT-ac74ac46', 1.0, '', '2026-12-31'
        UNION ALL
SELECT '0.1% BSA ', 'LOT-9cdd1374', 1.0, '1.0', '2026-12-31'
        UNION ALL
SELECT 'pscam II (alpha DNA)', 'LOT-3abaaca3', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'T2 (alpha DNA)', 'LOT-b4661886', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'ILRI 161108 R1 66.1 nmol', 'LOT-168d801f', 1.0, '6060.9', '2026-12-31'
        UNION ALL
SELECT 'ILRI 161108 R2 60.3 nmol', 'LOT-a701d3cc', 1.0, '603.1', '2026-12-31'
        UNION ALL
SELECT 'ILRI 161108 R3 60.5 nmol', 'LOT-b553bc45', 1.0, '604.6', '2026-12-31'
        UNION ALL
SELECT 'ILRI 161108 F3 45.1 nmol', 'LOT-122408f3', 1.0, '451.1', '2026-12-31'
        UNION ALL
SELECT 'ILRI 161108 F2 45.1 nmol', 'LOT-c5312343', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'loading dye', 'LOT-3339854e', 1.0, 'working', '2026-12-31'
        UNION ALL
SELECT 'Eurofins genomics 2R', 'LOT-349b199f', 1.0, '', '2017-09-12'
        UNION ALL
SELECT 'Eurofins genomics 1R', 'LOT-52431305', 1.0, '', '2017-09-12'
        UNION ALL
SELECT 'Eurofins genomics pair 1R', 'LOT-6576fc40', 1.0, '', '2017-07-07'
        UNION ALL
SELECT 'Eurofins genomics pair 2F', 'LOT-8579e4bc', 1.0, '', '2017-07-08'
        UNION ALL
SELECT 'Eurofins genomics pair 2R', 'LOT-8273cef7', 1.0, '', '2017-07-07'
        UNION ALL
SELECT 'Eurofins genomics 2F', 'LOT-1aacb0d9', 1.0, '', '2017-09-12'
        UNION ALL
SELECT 'Eurofins genomics 1F', 'LOT-4046f91c', 1.0, '', '2017-09-13'
        UNION ALL
SELECT 'Eurofins genomics pair 1F', 'LOT-99d2a201', 1.0, '', '2017-07-07'
        UNION ALL
SELECT '10x M buffer', 'LOT-a5546c37', 5.0, '1.0', '2026-12-31'
        UNION ALL
SELECT 'Hae III', 'LOT-f8ad3cb7', 9.0, '10.0', '2007-10-01'
        UNION ALL
SELECT 'Hha I', 'LOT-4642e11d', 3.0, '10.0', '2008-03-01'
        UNION ALL
SELECT '10x React 2', 'LOT-3ae8f47f', 7.0, '1.0', '2026-12-31'
        UNION ALL
SELECT 'quanti Tect probe PCR mastermix', 'LOT-33a251c0', 2.0, '1.7', '2026-12-31'
        UNION ALL
SELECT '10x buffer T', 'LOT-72537cd0', 1.0, '1.0', '2026-12-31'
        UNION ALL
SELECT 'Hae III', 'LOT-6f7982ee', 1.0, '10.0', '2024-11-01'
        UNION ALL
SELECT 'Hha I', 'LOT-ba687cb6', 1.0, '0.1', '2014-10-01'
        UNION ALL
SELECT 'Hha1 use with react 2', 'LOT-dd3676a3', 1.0, '10.0', '2010-02-28'
        UNION ALL
SELECT '10x react I', 'LOT-cc19456f', 1.0, '1.0', '2026-12-31'
        UNION ALL
SELECT '10x PCR buffer (-mgcl2)', 'LOT-8a357e56', 1.0, '1.25', '2026-12-31'
        UNION ALL
SELECT 'HuPoR (alpha DNA)(5'' GGACTTCGTTTGTACCCGTTG )', 'LOT-d2054fd3', 2.0, '', '2026-12-31'
        UNION ALL
SELECT 'b- actin R (5'' CGTCATACTCCTGCTTGCTGATCCACATCTGC)', 'LOT-3e24db92', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'LaeV5f (5'' CGTGATGTGCCCGAGTGCA)', 'LOT-0106d854', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'cpb EF rev', 'LOT-796a7e6c', 1.0, 'stock ', '2026-12-31'
        UNION ALL
SELECT 'cpb EFfwd', 'LOT-973fa075', 1.0, 'stock ', '2026-12-31'
        UNION ALL
SELECT 'K26 fwd', 'LOT-91fb8e7f', 1.0, 'stock ', '2026-12-31'
        UNION ALL
SELECT 'k26 rev', 'LOT-46623ff2', 1.0, 'stock ', '2026-12-31'
        UNION ALL
SELECT 'List R', 'LOT-833e41cf', 1.0, 'working', '2026-12-31'
        UNION ALL
SELECT '10xFast DigestGreen Buffer', 'LOT-aa4832dc', 1.0, '1.0', '2020-12-01'
        UNION ALL
SELECT '10xFast Digest Buffer (colorless)', 'LOT-1a4b55aa', 1.0, '1.0', '2020-12-02'
        UNION ALL
SELECT 'Fast Digest Buffer Alul', 'LOT-9f61ada6', 1.0, '100.0', '2020-12-03'
        UNION ALL
SELECT 'turbo DNase buffer', '402/4G', 18.0, '6.0', '2026-12-31'
        UNION ALL
SELECT 'RNA binding bead', 'lot 01226797', 16.0, '1.1', '2026-12-31'
        UNION ALL
SELECT 'Elution buffer', '9918G6', 19.0, '9.0', '2026-12-31'
        UNION ALL
SELECT 'elution buffer  (opened)', '9918G7', 12.0, '9.0', '2026-12-31'
        UNION ALL
SELECT 'turbo DNase buffer (opened)', '402/4G', 13.0, '6.0', '2026-12-31'
        UNION ALL
SELECT 'G-OFprimer (Glurp) 2.5 nMol', 'LOT-95244e43', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'OuterG4 for GLURP FW', 'LOT-d0080262', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'G-OR 2.5 nMol', 'LOT-f9cb70c5', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'OuterG5 for GLURP RV ', 'LOT-690cfdd5', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'Nested S4 for MSP-2 FW', 'LOT-a84a9286', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'Nested S1 for MSP-2 FW', 'LOT-38913855', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'MSP-2-S2 FW', 'LOT-70f04a16', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'MSP-2-S3 RV', 'LOT-27da3f36', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'MSP-2-S1 tail fw', 'LOT-5b1bb1e3', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'MSP2-FC27 RV (FAM dye labeled)', 'LOT-483fc567', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'MSP2-3D7-RV (VIC dye labeled)', 'LOT-ac53d7c0', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'M1-OR 2.5 nMol (MSP1M1OR)', 'LOT-3435a184', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'MSP1 M1 RV 2.5 nMol', 'LOT-74a8dd3f', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'MSP1M1-FW 2.5 nMol', 'LOT-ab1aa12d', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'MSP1M1-OF 2.5 nMol', 'LOT-938f4cb4', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'MSP1M1-KF 2.5 nMol', 'LOT-4e21d5ee', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'MSP1M1-KR 2.5 nMol', 'LOT-0b1b1422', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'Nested N1 for MSP1 FW', 'LOT-752b01c1', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'Nested N2 for MSP1 RV', 'LOT-e61bc48c', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'N2RPfmdr86', 'LOT-d7ba7b69', 1.0, 'Lyophilized', '2026-12-31'
        UNION ALL
SELECT 'N2RPfcrt', 'LOT-fe5a4e63', 1.0, 'Lyophilized', '2026-12-31'
        UNION ALL
SELECT 'N1FPfcrt', 'LOT-4b5906fd', 1.0, 'Lyophilized', '2026-12-31'
        UNION ALL
SELECT 'N1FPfmdr1034', 'LOT-087a66e9', 1.0, 'Lyophilized', '2026-12-31'
        UNION ALL
SELECT '96 DNA multi-sample kit 96 rxn', 'PARA Freezer 04', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT '96 DNA multi-sample kit', 'PARA Freezer 04', 1.0, '', '2026-12-31'
        UNION ALL
SELECT '96 DNA multi-sample kit 96rxn', 'PARA Freezer 04', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT '96 Blood RNA isolation kit (TURBO DNase & lysis binding inhancer)', 'PARA Freezer 04', 1.0, 'TURBO DNase 110 ul & lysis binding Enhancer 1.1 ml', '2026-12-31'
        UNION ALL
SELECT 'RNase A enzyme 1mg/ml', 'PARA Freezer 04', 1.0, '530.0', '2026-12-31'
        UNION ALL
SELECT 'proteinase K 100 mg/dl', 'PARA Freezer 04', 1.0, 'used', '2026-12-31'
        UNION ALL
SELECT 'Mag max RNA isolation kit (used)', 'PARA Freezer 04', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Dna /Rna index', '20026930.0', 1.0, '', '2024-02-28'
        UNION ALL
SELECT 'Dna /Rna index', '20026121.0', 2.0, '', '2024-03-27'
        UNION ALL
SELECT 'Dna /Rna index', '20026933.0', 1.0, '', '2023-12-11'
        UNION ALL
SELECT 'Dna /Rna index', '20026934.0', 1.0, '', '2024-02-06'
        UNION ALL
SELECT 'Dna prep PCR BUFFER', '20015829.0', 4.0, '', '2023-06-06'
        UNION ALL
SELECT 'Nextseq Accessory box v2', '15058251.0', 2.0, '', '2023-09-10'
        UNION ALL
SELECT 'Platinium Taq Dna polymerase', '10966034.0', 7.0, '', '2023-09-13'
        UNION ALL
SELECT 'Pf 18s probe', '230975330.0', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pv 18s probe', '230975334.0', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'dNTPs mix 10mM', 'R0193', 4.0, '', '2024-02-01'
        UNION ALL
SELECT 'KAPA libarary Quant kit', '7960140001.0', 2.0, '', '2024-08-03'
        UNION ALL
SELECT 'nextseq 500/550 cartirage v2', '15057939.0', 1.0, '', '2023-03-04'
        UNION ALL
SELECT 'nextseq 500/550 cartirage v3', '15057939.0', 2.0, '', '2023-09-30'
        UNION ALL
SELECT 'Iseq 100 reagent cartridge', '20009584.0', 3.0, '', '2023-05-13'
        UNION ALL
SELECT 'HF DNA polymerase', 'M0530L', 2.0, '500.0', '2023-10-01'
        UNION ALL
SELECT 'KAPA libarary Quant kit', '7960140001.0', 2.0, '', '2024-10-13'
        UNION ALL
SELECT 'Dna /Rna index set B', '20025080.0', 1.0, '', '2023-12-14'
        UNION ALL
SELECT 'Dna /Rna index set A', '20025019.0', 1.5, '', '2023-12-16'
        UNION ALL
SELECT 'Dna /Rna index set D', '20025082.0', 1.0, '', '2023-12-12'
        UNION ALL
SELECT 'resuspension buffer', '15026770.0', 3.0, '20.0', '2022-10-04'
        UNION ALL
SELECT 'resuspension buffer', '15026770.0', 1.0, '20.0', '2023-01-04'
        UNION ALL
SELECT 'Qubit RNA IQ assay kit', 'Q33222', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'dNTPs mix 10mM', 'R0192', 8.0, '1.0', '2024-02-01'
        UNION ALL
SELECT 'Nextseq Accessory box v2', '15058251.0', 3.0, '', '2023-02-18'
        UNION ALL
SELECT 'Platinium Taq Dna polymerase', '10966034.0', 10.0, '600.0', '2024-01-19'
        UNION ALL
SELECT 'Tagmentation buffer 1', '20015171.0', 16.0, '0.29', '2023-04-04'
        UNION ALL
SELECT 'enhanced PCR mix', '20015172.0', 16.0, '0.58', '2023-03-22'
        UNION ALL
SELECT 'phix control V3', 'LOT-d110f238', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Cubit dsDNA HS Assay kit ', 'Q32854', 4.0, '500.0', '2026-12-31'
        UNION ALL
SELECT 'AMPure XP', 'A63881', 1.0, '60.0', '2023-06-01'
        UNION ALL
SELECT 'Nuclease free water', 'P119E', 7.0, '13.0', '2026-12-31'
        UNION ALL
SELECT '1Kb DNA ladder ', 'SM0313', 4.0, '50.0', '2025-11-01'
        UNION ALL
SELECT 'Bio analyzer-High sensitivity DNA reagents ', 'lot 2006', 1.0, '', '2023-02-03'
        UNION ALL
SELECT 'Bio analyzer-High sensitivity DNA reagents ', 'lot 2135', 2.0, '', '2022-08-30'
        UNION ALL
SELECT 'DNA prep tagmentation beads', '20015880.0', 1.0, '96.0', '2022-10-07'
        UNION ALL
SELECT 'DNA prep tagmentation beads', '20015880.0', 5.0, '96.0', '2023-03-22'
        UNION ALL
SELECT 'sample purification beads', '15052080.0', 4.0, '13.0', '2023-03-07'
        UNION ALL
SELECT 'DNA prep beads+buffers', '20015828.0', 2.0, '96.0', '2023-03-21'
        UNION ALL
SELECT 'DNA prep beads+buffers', '20015828.0', 3.0, '96.0', '2023-05-02'
        UNION ALL
SELECT 'AMPure XP', 'A63881', 1.0, '60.0', '2023-09-02'
        UNION ALL
SELECT 'nextseq 500/550(mid output flow cell cartridge v2,5)', '20022409.0', 2.0, '', '2024-10-04'
        UNION ALL
SELECT 'nextseq 500/550(mid output flow cell cartridge v2,5)', '20022409.0', 2.0, '', '2024-02-28'
        UNION ALL
SELECT 'Taqman universal pcr mastermix', '4304437.0', 5.0, '5.0', '2023-03-17'
        UNION ALL
SELECT 'Taqman fast advansed mastermix', '4444557.0', 44.0, '5.0', '2023-05-31'
        UNION ALL
SELECT 'Taqman fast advansed mastermix', '4444557.0', 15.0, '5.0', '2023-01-31'
        UNION ALL
SELECT 'Proteinase K', 'MB-11201-0100', 2.0, '100.0', '2026-12-31'
        UNION ALL
SELECT 'i1 flow cell', '20642290.0', 3.0, '', '2023-03-27'
        UNION ALL
SELECT 'tagment stop buffer', '20636831.0', 16.0, '1.24', '2023-03-18'
        UNION ALL
SELECT 'qubit dsDNA HS standard #2', 'Q32854', 3.0, '5.0', '2026-12-31'
        UNION ALL
SELECT 'mgcl2', '2465152.0', 1.0, '1.25', '2022-03-04'
        UNION ALL
SELECT 'mgcl2', '2417588.0', 1.0, '1.25', '2021-09-28'
        UNION ALL
SELECT 'kb Extender', '2352302.0', 1.0, '1.3', '2021-06-23'
        UNION ALL
SELECT 'turbo DNase buffer', '402/4G', 18.0, '6.0', '2026-12-31'
        UNION ALL
SELECT 'RNA binding bead', 'lot 01226797', 16.0, '1.1', '2026-12-31'
        UNION ALL
SELECT 'Elution buffer', '9918G6', 19.0, '9.0', '2026-12-31'
        UNION ALL
SELECT 'elution buffer  (opened)', '9918G7', 12.0, '9.0', '2026-12-31'
        UNION ALL
SELECT 'turbo DNase buffer', '402/4G', 13.0, '6.0', '2026-12-31'
        UNION ALL
SELECT 'corning pipet tips 0.1-10 ul ', 'lot 274110', 10.0, '100.0', '2024-10-04'
        UNION ALL
SELECT 'collection tubes 2 ml', 'lot 172016263', 19.0, '50.0', '2026-12-31'
        UNION ALL
SELECT 'QIAamp mini spin colomn', 'lot 172015984', 50.0, '10.0', '2026-12-31'
        UNION ALL
SELECT 'Qiagen Exraction kit', 'LOT-d86f0e5e', 2.0, '', '2026-12-31'
        UNION ALL
SELECT 'tris-hydro chloride 1M', 'BP1756-100', 1.0, '100.0', '2026-12-31'
        UNION ALL
SELECT 'tagmentation wash buffer', '20015079.0', 4.0, '41.0', '2022-10-03'
        UNION ALL
SELECT 'Nextseq500/550 buffer cartrige V2', '15057941.0', 2.0, '', '2023-08-05'
        UNION ALL
SELECT 'PCR tubes 12 strips of 8 tubes ', 'BR781316-120 EA', 5.0, '10.0', '2026-12-31'
        UNION ALL
SELECT 'Qiagen proteinase K', '19133.0', 12.0, '10.0', '2026-12-31'
        UNION ALL
SELECT 'Qubit Assay tubes', 'Q32856', 9.0, '500.0', '2026-12-31'
        UNION ALL
SELECT 'PCR Amplitube caps', 'lot 30519006', 1.0, '125.0', '2026-12-31'
        UNION ALL
SELECT 'Amplitubes PCR rxn strips', 'T320-2R', 3.0, '125.0', '2026-12-31'
        UNION ALL
SELECT 'PCR plate 96 well', '17820008.0', 2.0, '10.0', '2026-12-31'
        UNION ALL
SELECT 'sample (DNA elution) storage tube', 'LOT-81e2420d', 1.0, '25.0', '2026-12-31'
        UNION ALL
SELECT 'Quick gel extraction & PCR Purification combo kit', 'K220001', 3.0, '50.0', '2025-10-31'
        UNION ALL
SELECT 'Bio analyzer-High sensitivity DNA chips', 'Lot: AR01BK50', 1.0, '', '2023-04-01'
        UNION ALL
SELECT 'Bio analyzer-syringe kit', 'G293868706', 2.0, '', '2026-12-31'
        UNION ALL
SELECT 'Top vision Agrose', 'R0492', 3.0, '500.0', '2026-03-01'
        UNION ALL
SELECT 'Qubit 1x ds DNA HS assay kit', 'LOT-59816228', 12.0, '', '2026-12-31'
        UNION ALL
SELECT 'Qubit 1x dsL DNA BR assay standard is ', 'LOT-4a93c85f', 12.0, '', '2026-12-31'
        UNION ALL
SELECT 'AmpureXP', 'LOT-3a13eaf9', 6.0, '', '2026-12-31'
        UNION ALL
SELECT 'Hot start MasterMix, high fidelity', 'LOT-134fe31d', 15.0, '', '2026-12-31'
        UNION ALL
SELECT 'Q5 Blood Direct MasterMix', 'LOT-afe04112', 12.0, '', '2026-12-31'
        UNION ALL
SELECT 'cpbF (alpha DNA)', 'LOT-82c0208e', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'cpbEF-F  (alpha DNA)', 'LOT-78f2917d', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'B4 (alpha DNA)', 'LOT-b42a97d8', 3.0, '', '2026-12-31'
        UNION ALL
SELECT 'K26 f 15pM', 'LOT-a5081181', 3.0, '', '2026-12-31'
        UNION ALL
SELECT 'T2 (alpha DNA)', 'LOT-78f96656', 3.0, '', '2026-12-31'
        UNION ALL
SELECT 'seqcpbr2 (apha DNA)', 'LOT-6b76a2a7', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Cpbef frd (alpha DNA)', 'LOT-b6a79645', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'seqcpbf1 (alpha DNA)', 'LOT-a1cf81ff', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'cpbEFrev (alpha DNA)', 'LOT-9b6e1cde', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'cpbEF R  (alpha DNA)', 'LOT-969a7c77', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'dNTPs 10mM', 'LOT-00a62824', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'vsp15pM', 'LOT-fc93a0da', 3.0, '', '2026-12-31'
        UNION ALL
SELECT 'one-4 phor-all buffer + 10x conc.', 'LOT-713a6c2e', 1.0, '1.0', '2026-12-31'
        UNION ALL
SELECT 'taq DNA poly', 'LOT-00f5c796', 1.0, '250.0', '2026-12-31'
        UNION ALL
SELECT 'k26r (alpha DNA)', 'LOT-4a0f97bc', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'LITSR (alpha DNA)', 'LOT-e01f01ed', 4.0, '', '2026-12-31'
        UNION ALL
SELECT 'L%.8s  (alpha DNA)', 'LOT-d74eb295', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Laev 10 R', 'LOT-27596c14', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'cpb TAG', 'LOT-e2562115', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'cpbr (alpha DNA)', 'LOT-9832d807', 1.0, '150.0', '2026-12-31'
        UNION ALL
SELECT 'cpb ATG (alpha DNA)', 'LOT-1fcb8304', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'big dye terminator v 3.1 cycle sequencing RR-24', 'LOT-0e881373', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Ilri-161108F1', 'LOT-70cf3296', 1.0, '604.2', '2026-12-31'
        UNION ALL
SELECT 'pgem3Zf(+)', 'LOT-e8492ef0', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'm13(-21)primer', 'LOT-d016086d', 1.0, '', '2026-12-31'
        UNION ALL
SELECT '202GA-Rv', 'LOT-aef875c3', 1.0, '', '2026-12-31'
        UNION ALL
SELECT '202GA-Fw', 'LOT-861551bc', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Tailing Segment FW', 'LOT-5ebafcf5', 1.0, '14.6', '2026-12-31'
        UNION ALL
SELECT 'Tailing Segment RV', 'LOT-4d2034b6', 1.0, '18.9', '2026-12-31'
        UNION ALL
SELECT 'Tailing Segment FW', 'LOT-6f108b9e', 1.0, '14.6', '2026-12-31'
        UNION ALL
SELECT 'Tailing Segment RV', 'LOT-fef0346b', 1.0, '18.9', '2026-12-31'
        UNION ALL
SELECT 'hrp2 one-step FW', 'LOT-918b0b10', 1.0, '14.6', '2026-12-31'
        UNION ALL
SELECT 'hrp2 one-step RV', 'LOT-91df7c98', 1.0, '20.0', '2026-12-31'
        UNION ALL
SELECT 'hrp2 one-step FW', 'LOT-a566c6a7', 1.0, '14.6', '2026-12-31'
        UNION ALL
SELECT 'hrp2 one-step RV', 'LOT-e7fbf084', 1.0, '20.0', '2026-12-31'
        UNION ALL
SELECT 'N1FPfcrt FW', 'LOT-ff793e2c', 1.0, '11.8', '2026-12-31'
        UNION ALL
SELECT 'N1FPfcrt RV', 'LOT-4b8d0256', 1.0, '7.2', '2026-12-31'
        UNION ALL
SELECT 'N1FPfcrt FW', 'LOT-218988de', 1.0, '11.8', '2026-12-31'
        UNION ALL
SELECT 'N1FPfcrt RV', 'LOT-1923136d', 1.0, '7.2', '2026-12-31'
        UNION ALL
SELECT 'N2FPfcrt FW', 'LOT-95cf4f2f', 1.0, '13.9', '2026-12-31'
        UNION ALL
SELECT 'N2FPfcrt RV', 'LOT-12b51a08', 1.0, '12.4', '2026-12-31'
        UNION ALL
SELECT 'N2FPfcrt FW', 'LOT-23a848a1', 1.0, '13.9', '2026-12-31'
        UNION ALL
SELECT 'N2FPfcrt RV', 'LOT-b0690fbc', 1.0, '12.4', '2026-12-31'
        UNION ALL
SELECT 'N1-Mdr1-86Y Fw', 'LOT-d95f224b', 1.0, '12.3', '2026-12-31'
        UNION ALL
SELECT 'N1-Mdr1-86Y Rv', 'LOT-59f5ec2b', 1.0, '12.9', '2026-12-31'
        UNION ALL
SELECT 'N1-Mdr1-86Y Fw', 'LOT-aefb2313', 1.0, '12.3', '2026-12-31'
        UNION ALL
SELECT 'N1-Mdr1-86Y Rv', 'LOT-c8df3c30', 1.0, '12.9', '2026-12-31'
        UNION ALL
SELECT 'N2-Mdr1-86Y FW', 'LOT-fb5290b3', 1.0, '16.8', '2026-12-31'
        UNION ALL
SELECT 'N2-Mdr1-86Y Rv', 'LOT-acc1e21c', 1.0, '17.3', '2026-12-31'
        UNION ALL
SELECT 'N2-Mdr1-86Y FW', 'LOT-3742b820', 1.0, '16.8', '2026-12-31'
        UNION ALL
SELECT 'N2-Mdr1-86Y Rv', 'LOT-5a2bef57', 1.0, '17.3', '2026-12-31'
        UNION ALL
SELECT 'N1FPfcrt FW', 'LOT-d76dc157', 1.0, '12.3', '2026-12-31'
        UNION ALL
SELECT 'N1FPfcrt RV', 'LOT-b38721fe', 1.0, '14.1', '2026-12-31'
        UNION ALL
SELECT 'N2FPfcrt FW', 'LOT-ed5affdb', 1.0, '15.4', '2026-12-31'
        UNION ALL
SELECT 'N2FPfcrt RV', 'LOT-8e1185c3', 1.0, '14.0', '2026-12-31'
        UNION ALL
SELECT 'N1-Mdr1-86Y Fw', 'LOT-22680609', 1.0, '11.4', '2026-12-31'
        UNION ALL
SELECT 'N1-Mdr1-86Y Rv', 'LOT-e4e19155', 1.0, '12.1', '2026-12-31'
        UNION ALL
SELECT 'N1-Mdr1-86Y Fw', 'LOT-9130e0fc', 1.0, '12.4', '2026-12-31'
        UNION ALL
SELECT 'N1-Mdr1-86Y Rv', 'LOT-a993cc87', 1.0, '12.1', '2026-12-31'
        UNION ALL
SELECT 'N2-mdr1-86Y FW', 'LOT-94623201', 1.0, '8.6', '2026-12-31'
        UNION ALL
SELECT 'N2-mdr1-86Y RV', 'LOT-25f43104', 1.0, '10.4', '2026-12-31'
        UNION ALL
SELECT 'N2-mdr1-86Y FW', 'LOT-4dccc150', 1.0, '8.6', '2026-12-31'
        UNION ALL
SELECT 'N2-mdr1-86Y RV', 'LOT-376d02ab', 1.0, '10.4', '2026-12-31'
        UNION ALL
SELECT 'M1-OF', 'LOT-dacc2806', 1.0, '9.6', '2026-12-31'
        UNION ALL
SELECT 'M1-OR', 'LOT-9641b118', 1.0, '11.1', '2026-12-31'
        UNION ALL
SELECT 'M1-OF', 'LOT-8e611989', 1.0, '9.6', '2026-12-31'
        UNION ALL
SELECT 'M1-OR', 'LOT-f6d8a807', 1.0, '11.1', '2026-12-31'
        UNION ALL
SELECT 'RNaseP-F', 'LOT-dadb44ea', 1.0, '62.1', '2026-12-31'
        UNION ALL
SELECT 'RNaseP-R', 'LOT-df287895', 1.0, '65.4', '2026-12-31'
        UNION ALL
SELECT 'msp-1(N)-FW', 'LOT-1cbe37f9', 1.0, '25.3', '2026-12-31'
        UNION ALL
SELECT 'msp-1(N)-RV', 'LOT-f6eebf1b', 1.0, '10.2', '2026-12-31'
        UNION ALL
SELECT 'msp-1(N1)-FW', 'LOT-4b67f7cf', 1.0, '25.3', '2026-12-31'
        UNION ALL
SELECT 'msp-1(N1)-RV', 'LOT-bc2d68dd', 1.0, '10.2', '2026-12-31'
        UNION ALL
SELECT 'msp-outer-F', 'LOT-4639ada0', 1.0, '60.6', '2026-12-31'
        UNION ALL
SELECT 'msp-outer-R', 'LOT-785a0123', 1.0, '59.7', '2026-12-31'
        UNION ALL
SELECT '3D7/ICfamily(N-2)-FW', 'LOT-45113687', 1.0, '25.9', '2026-12-31'
        UNION ALL
SELECT '3D7/ICfamily(N-2)-FW', 'LOT-1d09bd75', 1.0, '25.9', '2026-12-31'
        UNION ALL
SELECT 'msp2-S2-fw', 'LOT-127b8298', 1.0, '26.5', '2026-12-31'
        UNION ALL
SELECT 'msp2-S3-rev', 'LOT-91b4732a', 1.0, '24.2', '2026-12-31'
        UNION ALL
SELECT 'msp-2(N1)-FW', 'LOT-df6932cc', 1.0, '9.3', '2026-12-31'
        UNION ALL
SELECT 'msp-2(N1)-Rv', 'LOT-b6293077', 1.0, '28.4', '2026-12-31'
        UNION ALL
SELECT 'msp2-S2-fw', 'LOT-2699cdc9', 1.0, '26.5', '2026-12-31'
        UNION ALL
SELECT 'msp2-S3-rev', 'LOT-f661b359', 1.0, '24.2', '2026-12-31'
        UNION ALL
SELECT 'msp-2(N1)-FW', 'LOT-07ea0445', 1.0, '9.3', '2026-12-31'
        UNION ALL
SELECT 'msp-2(N1)-RV', 'LOT-f43eaf26', 1.0, '28.4', '2026-12-31'
        UNION ALL
SELECT 'msp2-S2-F', 'LOT-104f9d43', 1.0, '57.5', '2026-12-31'
        UNION ALL
SELECT 'msp2-S3-R', 'LOT-db39acd3', 1.0, '56.0', '2026-12-31'
        UNION ALL
SELECT 'msp2-S1Tail-fw', 'LOT-ea059c9e', 1.0, '18.9', '2026-12-31'
        UNION ALL
SELECT 'msp2-S1Tail-fw', 'LOT-2e164d12', 1.0, '18.9', '2026-12-31'
        UNION ALL
SELECT 'msp2-S1Tail-f', 'LOT-3c89c579', 1.0, '53.8', '2026-12-31'
        UNION ALL
SELECT 'FC27family(N2)-FW', 'LOT-e1a8b184', 1.0, '25.9', '2026-12-31'
        UNION ALL
SELECT 'FC27family(N2)-RV', 'LOT-18aa9bfa', 1.0, '11.8', '2026-12-31'
        UNION ALL
SELECT 'FC27family(N2)-FW', 'LOT-a96f4de6', 1.0, '25.9', '2026-12-31'
        UNION ALL
SELECT 'FC27family(N2)-RV', 'LOT-8a7f064b', 1.0, '11.8', '2026-12-31'
        UNION ALL
SELECT 'msp1-k1-F', 'LOT-728e4d4a', 1.0, '38.8', '2026-12-31'
        UNION ALL
SELECT 'msp1-k1-R', 'LOT-2a88242e', 1.0, '56.6', '2026-12-31'
        UNION ALL
SELECT 'msp1-Mad20-F', 'LOT-f898d13c', 1.0, '49.6', '2026-12-31'
        UNION ALL
SELECT 'msp1-Mad20-R', 'LOT-fee67d99', 1.0, '51.5', '2026-12-31'
        UNION ALL
SELECT 'Fc27', 'LOT-b3c8c92e', 1.0, '57.8', '2026-12-31'
        UNION ALL
SELECT '3D7', 'LOT-6c5ae543', 1.0, '57.4', '2026-12-31'
        UNION ALL
SELECT 'msp2-3D7-N5-RV-probe', 'LOT-63de57dc', 1.0, '20.9', '2026-12-31'
        UNION ALL
SELECT 'msp2-FC27-MS-RV-probe', 'LOT-1e08f44f', 1.0, '20.1', '2026-12-31'
        UNION ALL
SELECT 'Nuclase free water', 'LOT-73516da0', 6.0, '1.5', '2026-12-31'
        UNION ALL
SELECT 'NOT I', 'LOT-47c3e7fa', 1.0, '0.05', '2022-09-01'
        UNION ALL
SELECT 'NE buffer 3.1  ', 'LOT-796e3037', 1.0, '1.25', '2024-01-01'
        UNION ALL
SELECT 'PhHv10^3', 'LOT-29f4ffa6', 1.0, '80.0', '2026-12-31'
        UNION ALL
SELECT 'PhHv-26267s ', 'LOT-6dc27be7', 1.0, '232.0', '2026-12-31'
        UNION ALL
SELECT 'probephhv-305TQ-cy5', 'LOT-7b2da903', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'phhv-337as', 'LOT-d485ee2a', 1.0, '210.0', '2011-12-01'
        UNION ALL
SELECT 'ENDFORWARD (Genosys 7112-012)', 'LOT-01eaab8a', 1.0, 'Lyophilized', '2026-12-31'
        UNION ALL
SELECT 'ENDREVERSE (gENOSYS 7112-013)', 'LOT-edf02506', 1.0, 'Lyophilized', '2026-12-31'
        UNION ALL
SELECT '563CT- Fw', 'LOT-b31b5e76', 1.0, 'Lyophilized', '2026-12-31'
        UNION ALL
SELECT 'DNase I, RNase free', 'LOT-b12ee09f', 13.0, '', '2025-06-30'
        UNION ALL
SELECT '10X reaction buffer with MgCl2 for DNase I', 'LOT-5a95231a', 13.0, '1.25', '2026-12-31'
        UNION ALL
SELECT 'MnCl2', 'LOT-582a29ad', 13.0, '1.0', '2026-12-31'
        UNION ALL
SELECT 'reaction buffer with out MnCl2 for DNase I', 'LOT-24eb3ad9', 13.0, '1.0', '2026-12-31'
        UNION ALL
SELECT 'EDTA', 'LOT-5a0a6c53', 13.0, '1.0', '2026-12-31'
        UNION ALL
SELECT 'cell proliferatio kit I (MTT)', 'LOT-42da4d25', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'cell proliferatio kit I (MTT)', 'LOT-52581023', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'RQ1R  DNase, stop solution', 'LOT-4a5b9289', 1.0, '1.0', '2016-04-30'
        UNION ALL
SELECT 'RQ1 DNase, 10x rxn buffer', 'LOT-0c4ccdde', 1.0, '1.0', '2016-04-30'
        UNION ALL
SELECT 'RQ1R  RNase freeDNase', 'LOT-2e87478b', 1.0, '1.0', '2016-04-30'
        UNION ALL
SELECT 'PV25probe', 'LOT-d3a0f45f', 1.0, '20.4', '2026-12-31'
        UNION ALL
SELECT 'PV_DBP_probe', 'LOT-bbed8251', 1.0, '20.0', '2026-12-31'
        UNION ALL
SELECT 'CCp4 probe', 'LOT-ad1a7679', 1.0, '20.0', '2026-12-31'
        UNION ALL
SELECT 'pfMGET', 'LOT-6841d935', 1.0, '20.7', '2026-12-31'
        UNION ALL
SELECT 'Ps18S', 'LOT-fabf933a', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'QIAcuity probe PCR kit', 'LOT-5b40e5c4', 15.0, '1.0', '2024-09-17'
        UNION ALL
SELECT 'Probe PCR Kit 25ml', 'LOT-bb86d496', 1.0, '25.0', '2026-12-31'
        UNION ALL
SELECT 'Probe PCR Kit 50ml', 'LOT-b5cbf3f7', 10.0, '50.0', '2026-12-31'
        UNION ALL
SELECT 'Probe PCR Kit 15ml', 'LOT-c896ff20', 1.0, '15.0', '2026-12-31'
        UNION ALL
SELECT 'varATS ', 'LOT-d23666ec', 18.0, 'stock', '2026-12-31'
        UNION ALL
SELECT '2E12F1', 'LOT-664f2bde', 1.0, '15.5', '2026-12-31'
        UNION ALL
SELECT '2E12R1', 'LOT-935f5e73', 1.0, '13.2', '2026-12-31'
        UNION ALL
SELECT '2E12F1', 'LOT-9f7e2cc1', 1.0, '15.5', '2026-12-31'
        UNION ALL
SELECT '2E12R1', 'LOT-a20f29ce', 1.0, '13.2', '2026-12-31'
        UNION ALL
SELECT '2E12F', 'LOT-3280fa5a', 1.0, '20.4', '2026-12-31'
        UNION ALL
SELECT '2E12R', 'LOT-c5e8af5c', 1.0, '13.6', '2026-12-31'
        UNION ALL
SELECT '2E12F', 'LOT-aa4f8eae', 1.0, '20.4', '2026-12-31'
        UNION ALL
SELECT '2E12R', 'LOT-eb5497b5', 1.0, '13.6', '2026-12-31'
        UNION ALL
SELECT '2E2F', 'LOT-16e700c3', 1.0, '14.9', '2026-12-31'
        UNION ALL
SELECT '2E2R1', 'LOT-91df6804', 1.0, '16.6', '2026-12-31'
        UNION ALL
SELECT '2E2F', 'LOT-237a0c61', 1.0, '14.9', '2026-12-31'
        UNION ALL
SELECT '2E2R1', 'LOT-d7d2c70c', 1.0, '16.6', '2026-12-31'
        UNION ALL
SELECT '2E2F1', 'LOT-42755adb', 1.0, '12.0', '2026-12-31'
        UNION ALL
SELECT '2E2R', 'LOT-a76fb37d', 1.0, '13.8', '2026-12-31'
        UNION ALL
SELECT '2E2F1', 'LOT-07358a08', 1.0, '12.0', '2026-12-31'
        UNION ALL
SELECT '2E2R', 'LOT-3085fde7', 1.0, '13.8', '2026-12-31'
        UNION ALL
SELECT 'BRAVO_F', 'LOT-580e3cac', 1.0, '14.6', '2026-12-31'
        UNION ALL
SELECT 'BRAVO_R', 'LOT-c4e26805', 1.0, '17.9', '2026-12-31'
        UNION ALL
SELECT 'BRAVO_F', 'LOT-f4d97bfa', 1.0, '14.6', '2026-12-31'
        UNION ALL
SELECT 'BRAVO_R', 'LOT-693e8c58', 1.0, '17.9', '2026-12-31'
        UNION ALL
SELECT 'R1(2)', 'LOT-3ee76e08', 1.0, '19.0', '2026-12-31'
        UNION ALL
SELECT 'F1 (M1-F1)', 'LOT-541ccb7e', 1.0, '16.2', '2026-12-31'
        UNION ALL
SELECT 'R1(2)', 'LOT-4d7fb331', 1.0, '19.0', '2026-12-31'
        UNION ALL
SELECT 'F1 (M1-F1)', 'LOT-09240e20', 1.0, '16.2', '2026-12-31'
        UNION ALL
SELECT 'R2', 'LOT-e5725454', 1.0, '11.6', '2026-12-31'
        UNION ALL
SELECT 'F2', 'LOT-f031a6b9', 1.0, '14.4', '2026-12-31'
        UNION ALL
SELECT 'R2', 'LOT-c683669c', 1.0, '11.6', '2026-12-31'
        UNION ALL
SELECT 'F2', 'LOT-b3de15cd', 1.0, '14.4', '2026-12-31'
        UNION ALL
SELECT 'Out PCR barcoding fw', 'LOT-3a4bea57', 1.0, '15.3', '2026-12-31'
        UNION ALL
SELECT 'Out PCR barcoding rv', 'LOT-3c7aee16', 1.0, '15.9', '2026-12-31'
        UNION ALL
SELECT 'Out PCR barcoding fw', 'LOT-e055ef30', 1.0, '15.3', '2026-12-31'
        UNION ALL
SELECT 'Out PCR barcoding rv', 'LOT-707f24a5', 1.0, '15.9', '2026-12-31'
        UNION ALL
SELECT 'M1-R1', 'LOT-4a01911e', 1.0, '16.9', '2026-12-31'
        UNION ALL
SELECT 'M1-IR', 'LOT-c859d01b', 1.0, '16.9', '2026-12-31'
        UNION ALL
SELECT 'Tailing Segment FW', 'LOT-865a7416', 1.0, '14.6', '2026-12-31'
        UNION ALL
SELECT 'Tailing Segment RV', 'LOT-2bbc182a', 1.0, '18.9', '2026-12-31'
        UNION ALL
SELECT 'Tailing Segment FW', 'LOT-1878115a', 1.0, '14.6', '2026-12-31'
        UNION ALL
SELECT 'Tailing Segment RV', 'LOT-5a8f5d0e', 1.0, '18.9', '2026-12-31'
        UNION ALL
SELECT 'hrp2 one-step FW', 'LOT-bbf0a520', 1.0, '14.6', '2026-12-31'
        UNION ALL
SELECT 'hrp2 one-step RV', 'LOT-6de4bbec', 1.0, '20.0', '2026-12-31'
        UNION ALL
SELECT 'hrp2 one-step FW', 'LOT-ccf3aa9d', 1.0, '14.6', '2026-12-31'
        UNION ALL
SELECT 'hrp2 one-step RV', 'LOT-be45530d', 1.0, '20.0', '2026-12-31'
        UNION ALL
SELECT 'N1FPfcrt FW', 'LOT-8cd0e913', 1.0, '11.8', '2026-12-31'
        UNION ALL
SELECT 'N1FPfcrt RV', 'LOT-d61be401', 1.0, '7.2', '2026-12-31'
        UNION ALL
SELECT 'N1FPfcrt FW', 'LOT-071ac58e', 1.0, '11.8', '2026-12-31'
        UNION ALL
SELECT 'N1FPfcrt RV', 'LOT-7e3f76f2', 1.0, '7.2', '2026-12-31'
        UNION ALL
SELECT 'N2FPfcrt FW', 'LOT-f6a2e7e2', 1.0, '13.9', '2026-12-31'
        UNION ALL
SELECT 'N2FPfcrt RV', 'LOT-6f8fd980', 1.0, '12.4', '2026-12-31'
        UNION ALL
SELECT 'N2FPfcrt FW', 'LOT-61be611b', 1.0, '13.9', '2026-12-31'
        UNION ALL
SELECT 'N2FPfcrt RV', 'LOT-66964dc2', 1.0, '12.4', '2026-12-31'
        UNION ALL
SELECT 'N1-Mdr1-86Y Fw', 'LOT-5ff71ad3', 1.0, '12.3', '2026-12-31'
        UNION ALL
SELECT 'N1-Mdr1-86Y Rv', 'LOT-b3a56d5c', 1.0, '12.9', '2026-12-31'
        UNION ALL
SELECT 'N1-Mdr1-86Y Fw', 'LOT-58528d9d', 1.0, '12.3', '2026-12-31'
        UNION ALL
SELECT 'N1-Mdr1-86Y Rv', 'LOT-88a790e2', 1.0, '12.9', '2026-12-31'
        UNION ALL
SELECT 'N2-Mdr1-86Y FW', 'LOT-4cdccd66', 1.0, '16.8', '2026-12-31'
        UNION ALL
SELECT 'N2-Mdr1-86Y Rv', 'LOT-12d3eb3d', 1.0, '17.3', '2026-12-31'
        UNION ALL
SELECT 'N2-Mdr1-86Y FW', 'LOT-467e1865', 1.0, '16.8', '2026-12-31'
        UNION ALL
SELECT 'N2-Mdr1-86Y Rv', 'LOT-b2296502', 1.0, '17.3', '2026-12-31'
        UNION ALL
SELECT 'N1FPfcrt FW', 'LOT-6a6ec30d', 1.0, '12.3', '2026-12-31'
        UNION ALL
SELECT 'N1FPfcrt RV', 'LOT-82aafe65', 1.0, '14.1', '2026-12-31'
        UNION ALL
SELECT 'N2FPfcrt FW', 'LOT-6140d473', 1.0, '15.4', '2026-12-31'
        UNION ALL
SELECT 'N2FPfcrt RV', 'LOT-70cc3620', 1.0, '14.0', '2026-12-31'
        UNION ALL
SELECT 'N1-Mdr1-86Y Fw', 'LOT-d04f6efd', 1.0, '11.4', '2026-12-31'
        UNION ALL
SELECT 'N1-Mdr1-86Y Rv', 'LOT-83b0b49d', 1.0, '12.1', '2026-12-31'
        UNION ALL
SELECT 'N1-Mdr1-86Y Fw', 'LOT-dd960167', 1.0, '12.4', '2026-12-31'
        UNION ALL
SELECT 'N1-Mdr1-86Y Rv', 'LOT-ff72ae1c', 1.0, '12.1', '2026-12-31'
        UNION ALL
SELECT 'N2-mdr1-86Y FW', 'LOT-a9ef6b3e', 1.0, '8.6', '2026-12-31'
        UNION ALL
SELECT 'N2-mdr1-86Y RV', 'LOT-2d2005a9', 1.0, '10.4', '2026-12-31'
        UNION ALL
SELECT 'N2-mdr1-86Y FW', 'LOT-32597837', 1.0, '8.6', '2026-12-31'
        UNION ALL
SELECT 'N2-mdr1-86Y RV', 'LOT-7c238175', 1.0, '10.4', '2026-12-31'
        UNION ALL
SELECT 'M1-OF', 'LOT-c0b55773', 1.0, '9.6', '2026-12-31'
        UNION ALL
SELECT 'M1-OR', 'LOT-c6d9a34e', 1.0, '11.1', '2026-12-31'
        UNION ALL
SELECT 'M1-OF', 'LOT-e0b3d685', 1.0, '9.6', '2026-12-31'
        UNION ALL
SELECT 'M1-OR', 'LOT-28a0f150', 1.0, '11.1', '2026-12-31'
        UNION ALL
SELECT 'Pfmdr11246-B', 'LOT-fcc291c3', 1.0, '26.3', '2026-12-31'
        UNION ALL
SELECT 'Pfmdr11246-A', 'LOT-917d0017', 1.0, '24.7', '2026-12-31'
        UNION ALL
SELECT 'Pfmdr1_RV', 'LOT-b74dfc67', 1.0, '40.2', '2026-12-31'
        UNION ALL
SELECT 'Pfmdr1_FW', 'LOT-de32ceef', 1.0, '50.6', '2026-12-31'
        UNION ALL
SELECT 'Pf--tubulin_FW', 'LOT-073c07b2', 1.0, '54.9', '2026-12-31'
        UNION ALL
SELECT 'Pf--tubulin_RV', 'LOT-6dbc78fa', 1.0, '45.3', '2026-12-31'
        UNION ALL
SELECT 'Pfplasmepsin2__FW', 'LOT-9c284b28', 1.0, '63.4', '2026-12-31'
        UNION ALL
SELECT 'Pfplasmepsin2__RV', 'LOT-67d27907', 1.0, '55.3', '2026-12-31'
        UNION ALL
SELECT 'Pfmdr1246-D1', 'LOT-faba8468', 1.0, '22.2', '2026-12-31'
        UNION ALL
SELECT 'Pfmdr1246-D2', 'LOT-1205ccaa', 1.0, '25.6', '2026-12-31'
        UNION ALL
SELECT 'Pfmdr1246-A', 'LOT-7188f948', 1.0, '24.7', '2026-12-31'
        UNION ALL
SELECT 'Pfmdr1246-B', 'LOT-8cb4d5ee', 1.0, '26.3', '2026-12-31'
        UNION ALL
SELECT 'Pfmdr1246-D1', 'LOT-bfaa17dd', 1.0, '22.2', '2026-12-31'
        UNION ALL
SELECT 'Pfmdr1246-D2', 'LOT-3dd3e576', 1.0, '25.6', '2026-12-31'
        UNION ALL
SELECT 'RV11', 'LOT-7dd364e7', 1.0, '52.7', '2026-12-31'
        UNION ALL
SELECT 'RV12', 'LOT-5654319d', 1.0, '53.9', '2026-12-31'
        UNION ALL
SELECT '8633F', 'LOT-2fef18ac', 1.0, '55.9', '2026-12-31'
        UNION ALL
SELECT '9211R', 'LOT-aa7410a2', 1.0, '62.5', '2026-12-31'
        UNION ALL
SELECT '8945F', 'LOT-1da5ace7', 1.0, '69.7', '2026-12-31'
        UNION ALL
SELECT '9577R', 'LOT-b8eed9d5', 1.0, '59.5', '2026-12-31'
        UNION ALL
SELECT '8669F', 'LOT-71426843', 1.0, '65.0', '2026-12-31'
        UNION ALL
SELECT '9541R', 'LOT-81881ca5', 1.0, '59.4', '2026-12-31'
        UNION ALL
SELECT 'RV12-2', 'LOT-5b9016ec', 1.0, '63.9', '2026-12-31'
        UNION ALL
SELECT 'Pv210-Pc', 'LOT-8e5369ee', 3.0, '9.1', '2026-12-31'
        UNION ALL
SELECT 'pf-pc2', 'LOT-cbca1c29', 1.0, '0.25', '2026-12-31'
        UNION ALL
SELECT 'pv-210-6', 'LOT-6c5e6cbb', 1.0, '0.5', '2026-12-31'
        UNION ALL
SELECT 'pf-pc5', 'LOT-f7be2704', 1.0, '0.25', '2026-12-31'
        UNION ALL
SELECT 'pf-pc1', 'LOT-758b06ff', 1.0, '0.25', '2026-12-31'
        UNION ALL
SELECT 'pv-210-5', 'LOT-8f663c89', 1.0, '0.3', '2026-12-31'
        UNION ALL
SELECT 'pf-pc4', 'LOT-47fb2d3f', 1.0, '0.3', '2026-12-31'
        UNION ALL
SELECT 'pv-210-2', 'LOT-9207a99c', 1.0, '0.25', '2026-12-31'
        UNION ALL
SELECT 'pv-210-1', 'LOT-d5457bf0', 1.0, '0.25', '2026-12-31'
        UNION ALL
SELECT 'pf-pc6', 'LOT-642eb800', 1.0, '0.5', '2026-12-31'
        UNION ALL
SELECT 'pv-210 pc ELISA(stock)', 'LOT-1cb1a466', 1.0, '0.25', '2026-12-31'
        UNION ALL
SELECT 'pv-247 pc ELISA(stock)', 'LOT-b507ca45', 1.0, '0.25', '2026-12-31'
        UNION ALL
SELECT 'Pf-PC', 'LOT-f1be4b30', 2.0, '0.1', '2026-12-31'
        UNION ALL
SELECT 'PV-210-3', 'LOT-64e33729', 1.0, '0.25', '2026-12-31'
        UNION ALL
SELECT 'Pf-PC3', 'LOT-97575c4e', 1.0, '0.25', '2026-12-31'
        UNION ALL
SELECT 'pf-210-4', 'LOT-00765d84', 1.0, '0.25', '2026-12-31'
        UNION ALL
SELECT 'New Naive-serum', 'LOT-112029e5', 1.0, '1.5', '2026-12-31'
        UNION ALL
SELECT 'conjugate m Ab pf', 'LOT-1cbc989c', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'pf-pc csp', 'LOT-6aceffb4', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'pv hrp swp', 'LOT-1163d2f6', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'pv-210-pc ELISA(stock)', 'LOT-5568e8c8', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'pv247-pc', 'LOT-83201d02', 1.0, '4.6', '2026-12-31'
        UNION ALL
SELECT 'pv capture AB', 'LOT-44bb9c62', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'pf capture AB', 'LOT-a432057c', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'pv-210-1 m ab', 'LOT-611aee9e', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'pv-210 2nd mab', 'LOT-f2f68053', 2.0, '', '2026-12-31'
        UNION ALL
SELECT 'pv-219 pc ELISA', 'LOT-7a5e7e14', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'CCP4-RV', 'LOT-fff17fd6', 1.0, '500.0', '2026-12-31'
        UNION ALL
SELECT 'BCFB FY2021', 'LOT-0c18768e', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'PfMGET-FW', 'LOT-89778b5b', 1.0, '1000.0', '2026-12-31'
        UNION ALL
SELECT 'PfMGET-RV', 'LOT-0bda0b33', 1.0, '500.0', '2026-12-31'
        UNION ALL
SELECT 'rPLU-5', 'LOT-7446ecab', 1.0, '1000.0', '2026-12-31'
        UNION ALL
SELECT 'Pv18s-RV', 'LOT-8cf5cb61', 1.0, '500.0', '2026-12-31'
        UNION ALL
SELECT 'rVIV-1', 'LOT-4fcb1d99', 1.0, '300.0', '2026-12-31'
        UNION ALL
SELECT 'Pv525-RV', 'LOT-667ee854', 1.0, '500.0', '2026-12-31'
        UNION ALL
SELECT 'rPLU-6', 'LOT-946e43ad', 1.0, '1000.0', '2026-12-31'
        UNION ALL
SELECT 'PV525-FW', 'LOT-95211254', 1.0, '500.0', '2026-12-31'
        UNION ALL
SELECT 'Pf 18s-FW', 'LOT-a8835c8c', 1.0, '300.0', '2026-12-31'
        UNION ALL
SELECT 'rFAL-2', 'LOT-8cc5615a', 1.0, '300.0', '2026-12-31'
        UNION ALL
SELECT 'PV18S probe', 'LOT-beb01c90', 1.0, '300.0', '2026-12-31'
        UNION ALL
SELECT 'rVIV-2', 'LOT-49d6586c', 1.0, '300.0', '2026-12-31'
        UNION ALL
SELECT 'rFAL-1', 'LOT-00950410', 1.0, '300.0', '2026-12-31'
        UNION ALL
SELECT 'PfMEGT probe', 'LOT-86d9694e', 1.0, '7443.9', '2026-12-31'
        UNION ALL
SELECT 'Pf 18s turbo probe', 'LOT-878e2a2e', 1.0, '900.0', '2026-12-31'
        UNION ALL
SELECT 'CCP4 probe', 'LOT-c1b94870', 1.0, '900.0', '2026-12-31'
        UNION ALL
SELECT 'PF18S RV', 'LOT-00c93f63', 1.0, '500.0', '2026-12-31'
        UNION ALL
SELECT 'PVS25 Probe', 'LOT-5c6ddb86', 1.0, '600.0', '2026-12-31'
        UNION ALL
SELECT 'PV18S Probe', 'LOT-9e1e4fcc', 1.0, '300.0', '2026-12-31'
        UNION ALL
SELECT 'E-COLI', 'LOT-8e4d427c', 5.0, '~1', '2026-12-31'
        UNION ALL
SELECT 'New N-serum', 'LOT-b9ef72f9', 6.0, '~2', '2026-12-31'
        UNION ALL
SELECT 'E-COLI Lysate', 'LOT-51397ea3', 4.0, '8.64', '2026-12-31'
        UNION ALL
SELECT 'WHO PF standard(+ve control)', 'LOT-e5ce626f', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'PF +VE control(cp3 NEAT)', 'LOT-3f6fd995', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'cp3(1:1)(50%)', 'LOT-42b0c7f6', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Et-pf pooled serum', 'LOT-c60a91be', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'E-COLI Lysate(5.2mg/ml)', 'LOT-0c58472d', 1.0, '1.6', '2026-12-31'
        UNION ALL
SELECT 'CP3 1:10', 'LOT-036c892b', 1.0, '0.5', '2026-12-31'
        UNION ALL
SELECT 'S1PV +VE control', 'LOT-dd8fd2ae', 1.0, '~2,5', '2026-12-31'
        UNION ALL
SELECT 'Hot start Taq 2x Master Mix 500rxn', 'LOT-f862d45c', 1.0, '', '2024-08-01'
        UNION ALL
SELECT 'Dream Taq Master Mix 2x', 'LOT-5480c59f', 5.0, '', '2026-12-31'
        UNION ALL
SELECT 'PCR buffer+green+white+mgcl', 'LOT-10c2d922', 1.0, '2ml', '2019-09-14'
        UNION ALL
SELECT 'PCR buffer+green+white+mgcl', 'LOT-834abb1f', 1.0, '2ml', '2021-10-15'
        UNION ALL
SELECT 'PCR buffer+green+white+mgcl', 'LOT-bd75a4dc', 1.0, '2ml', '2026-12-31'
        UNION ALL
SELECT 'DNA ladder 100bp with buffer', 'LOT-cd88ab75', 2.0, '', '2026-12-31'
        UNION ALL
SELECT 'Invitrogen DNA ladder 50bp with buffer', 'LOT-c541e21c', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'GoTaq nPCR componenet ', 'LOT-11552df2', 40.0, '', '2024-11-04'
        UNION ALL
SELECT 'GoTaq nPCR componenet ', 'LOT-2630b647', 20.0, '', '2026-12-31'
        UNION ALL
SELECT 'GoTaq nPCR componenet ', 'LOT-7c8297e9', 40.0, '', '2024-04-11'
        UNION ALL
SELECT 'GoTaq nPCR componenet ', 'LOT-bd182b9b', 3.0, '', '2021-10-15'
        UNION ALL
SELECT 'GoTaq nPCR componenet ', 'LOT-6b70fb43', 18.0, '', '2022-12-11'
        UNION ALL
SELECT 'GoTaq 100nM dNTPs set', 'LOT-eefb819d', 11.0, '', '2024-01-08'
        UNION ALL
SELECT 'Bio-dNTP Mix ', 'LOT-61175f7a', 1.0, '', '2023-02-01'
        UNION ALL
SELECT 'MBL-dNTP Mix ', 'LOT-2905f420', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Thermoscintific- dNTP Mix ', 'LOT-1165d903', 1.0, '', '2024-05-01'
        UNION ALL
SELECT 'GoTaq dNTP set', 'LOT-634dfc80', 1.0, '', '2026-12-31'
        UNION ALL
SELECT '100bp DNA ladder ', 'LOT-ec461797', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'PCR Nucleotide Mix', 'LOT-d8ad0b5e', 1.0, '', '2024-04-01'
        UNION ALL
SELECT 'rplu5-100mM', 'LOT-e0d3082c', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'rplu5-100mM', 'LOT-83ccb79a', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'rplu6-100mM', 'LOT-49da199e', 4.0, '', '2026-12-31'
        UNION ALL
SELECT 'rplu6-100mM', 'LOT-dfde270a', 2.0, '', '2026-12-31'
        UNION ALL
SELECT 'rrplu1-100mM', 'LOT-81bea63f', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'rplu3-100mM', 'LOT-0543cf17', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'rplu3-100mM', 'LOT-c5c92e3d', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'rplu4-100mM', 'LOT-1a1791c8', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'rviv1-100mM', 'LOT-a610a486', 4.0, '', '2026-12-31'
        UNION ALL
SELECT 'rviv1-100mM', 'LOT-9ffe5b51', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'rviv2-100mM', 'LOT-05fc941b', 4.0, '', '2026-12-31'
        UNION ALL
SELECT 'rviv2-100mM', 'LOT-46fbc391', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'rfal2-100mM', 'LOT-5d08d31a', 4.0, '', '2026-12-31'
        UNION ALL
SELECT 'rfal2-100mM', 'LOT-e56a4093', 2.0, '', '2026-12-31'
        UNION ALL
SELECT 'rfal1-100mM', 'LOT-382d8009', 4.0, '', '2026-12-31'
        UNION ALL
SELECT 'rfal1-100mM', 'LOT-dc0b0ef7', 2.0, '', '2026-12-31'
        UNION ALL
SELECT 'rmal1-100mM', 'LOT-b2ee15b9', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'rmal2-100mM', 'LOT-afb0b271', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'rova1-100mM', 'LOT-673fe63b', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'rova2-100mM', 'LOT-2fb2edc0', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pf - Rv (rFAL2)', 'LOT-65490365', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pf-fw (rFAL1)', 'LOT-c86e180c', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pv Rv (rVIV 2)', 'LOT-bdb1d294', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pv Fw (rVIV 1)', 'LOT-736f0a1c', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'rPLU 6 forward', 'LOT-e7e14fe4', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'rPLU 5 reverse', 'LOT-690df071', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'dCTP-100mM', 'LOT-ad2df871', 2.0, '1.0', '2023-04-09'
        UNION ALL
SELECT 'dTTP-100mM', 'LOT-e9f80de1', 2.0, '1.0', '2023-04-09'
        UNION ALL
SELECT 'dATP-100mM', 'LOT-88c3fae6', 2.0, '1.0', '2023-04-09'
        UNION ALL
SELECT 'dGTP-100mM', 'LOT-c05b5f88', 2.0, '1.0', '2023-04-13'
        UNION ALL
SELECT 'dTTP-100mM', 'LOT-d3acd8af', 4.0, '0.4&1', '2022-02-17'
        UNION ALL
SELECT 'dGTP-100mM', 'LOT-3da85e55', 4.0, '0.4&1', '2022-04-28'
        UNION ALL
SELECT 'dATP-100mM', 'LOT-668e74b9', 4.0, '0.4&1', '2022-05-27'
        UNION ALL
SELECT 'dCTP-100mM', 'LOT-743ceb84', 4.0, '0.4&1', '2022-02-17'
        UNION ALL
SELECT 'dATP-100mM', 'LOT-d7fa874e', 11.0, '0.4&1', '2024-09-19'
        UNION ALL
SELECT 'dGTP-100mM', 'LOT-7b70640d', 11.0, '0.4&1', '2024-09-26'
        UNION ALL
SELECT 'dCTP-100mM', 'LOT-c7b3ffe5', 11.0, '0.4&1', '2024-09-19'
        UNION ALL
SELECT 'dTTP-100mM', 'LOT-e71b40a5', 11.0, '0.4&1', '2024-08-01'
        UNION ALL
SELECT 'dNTP Mix -2.5mM', 'LOT-d6804eff', 13.0, '1.0', '2026-12-31'
        UNION ALL
SELECT 'dATP-100mM', 'LOT-cd6ae41d', 2.0, '1.0', '2022-05-27'
        UNION ALL
SELECT 'dCTP-100mM', 'LOT-c926a44a', 2.0, '1.0', '2022-02-17'
        UNION ALL
SELECT 'dTTP-100mM', 'LOT-bc946897', 2.0, '1.0', '2022-02-17'
        UNION ALL
SELECT 'dGTP-100mM', 'LOT-a7851ada', 2.0, '1.0', '2022-04-28'
        UNION ALL
SELECT 'dNTP MIX -10mM', 'LOT-cdc26174', 1.0, '1.0', '2022-05-12'
        UNION ALL
SELECT 'dATP-100mM   100umol', 'LOT-b09943e8', 1.0, '1.0', '2018-06-01'
        UNION ALL
SELECT 'dCTP-100mM  100umol', 'LOT-c01cb94f', 1.0, '1.0', '2018-06-01'
        UNION ALL
SELECT 'dGTP-100mM  100umol', 'LOT-2c0b5766', 2.0, '1.0', '2018-06-01'
        UNION ALL
SELECT 'dTTP-100mM  100umol', 'LOT-5e6995b7', 1.0, '1.0', '2018-06-01'
        UNION ALL
SELECT 'dNTP MIX with dUTP-12.5mM', 'LOT-78885649', 1.0, '1.0', '2026-12-31'
        UNION ALL
SELECT 'dNTP MIX with dTTP-10mM', 'LOT-c466398a', 1.0, '1.0', '2026-12-31'
        UNION ALL
SELECT 'dNTP mix 10mM', 'LOT-7f0bbe06', 10.0, '1.0', '2023-02-01'
        UNION ALL
SELECT 'AF1III ', 'LOT-66a2b6bf', 1.0, '0.025', '2024-01-01'
        UNION ALL
SELECT 'AF1III ', 'LOT-a20bc765', 1.0, '0.025', '2024-01-01'
        UNION ALL
SELECT 'APoI', 'LOT-3c8a6897', 1.0, '0.1', '2018-07-01'
        UNION ALL
SELECT 'N1FPfcrt-100mM', 'LOT-93684ff8', 2.0, '', '2026-12-31'
        UNION ALL
SELECT 'N2FPfcrt-100mM', 'LOT-8930ed3f', 2.0, '', '2026-12-31'
        UNION ALL
SELECT 'N1FPfcrt RW-100mM', 'LOT-1ee7c59f', 1.0, '', '2022-12-01'
        UNION ALL
SELECT 'N1FPfcrt FW-100mM', 'LOT-44444b94', 1.0, '', '2022-12-01'
        UNION ALL
SELECT 'N2FPfcrt RW-100mM', 'LOT-049e6c1a', 1.0, '', '2022-12-01'
        UNION ALL
SELECT 'N2FPfcrt FW-100mM', 'LOT-c1865e0b', 1.0, '', '2022-12-01'
        UNION ALL
SELECT 'green Go Taq buffer', 'LOT-6d43fdbf', 15.0, '1.0', '2026-12-31'
        UNION ALL
SELECT 'MgCl2 -25mM', 'LOT-7b70f640', 16.0, '1.2', '2026-12-31'
        UNION ALL
SELECT 'colorless Go Taq buffer', 'LOT-7edcdef1', 25.0, '1.0', '2026-12-31'
        UNION ALL
SELECT 'colorless Go Taq buffer', 'LOT-335bd97a', 68.0, '1.0', '2022-11-22'
        UNION ALL
SELECT 'colorless Go Taq buffer', 'LOT-328376e9', 33.0, '1.0', '2026-12-31'
        UNION ALL
SELECT 'MgCl2 -25mM', 'LOT-96955c9b', 17.0, '1.2', '2026-12-31'
        UNION ALL
SELECT 'green Go Taq buffer', 'LOT-b303968a', 23.0, '1.0', '2026-12-31'
        UNION ALL
SELECT 'green Go Taq buffer', 'LOT-52363449', 69.0, '1.0', '2022-11-12'
        UNION ALL
SELECT 'MgCl2 -25mM', 'LOT-26a7bb93', 54.0, '1.2', '2022-11-12'
        UNION ALL
SELECT 'green Go Taq buffer', 'LOT-23e1eac5', 59.0, '1.0', '2024-04-11'
        UNION ALL
SELECT 'green Go Taq buffer', 'LOT-ef620856', 56.0, '1.0', '2024-04-11'
        UNION ALL
SELECT 'green Go Taq buffer', 'LOT-e63fc8d4', 44.0, '1.0', '2024-04-11'
        UNION ALL
SELECT 'colorless Go Taq buffer', 'LOT-8b02ab1e', 40.0, '1.0', '2024-04-11'
        UNION ALL
SELECT 'colorless Go Taq buffer', 'LOT-6d9096bd', 53.0, '1.0', '2024-04-11'
        UNION ALL
SELECT 'colorless Go Taq buffer', 'LOT-71db905b', 58.0, '1.0', '2024-04-11'
        UNION ALL
SELECT 'MgCl2 -25mM', 'LOT-162c6a67', 43.0, '1.2', '2024-04-11'
        UNION ALL
SELECT 'MgCl2 -25mM', 'LOT-759d2f26', 48.0, '1.2', '2024-04-11'
        UNION ALL
SELECT 'MgCl2 -25mM', 'LOT-060672e0', 27.0, '1.2', '2024-04-11'
        UNION ALL
SELECT 'GO Taq G2 flexi DNA polymerases 5 u/ul', 'LOT-9b42824b', 40.0, '500.0', '2024-12-12'
        UNION ALL
SELECT 'HOT Start Taq 2X master mix (500 rxn / vial )', 'LOT-b9f4b0f4', 2.0, '50.0', '2024-08-31'
        UNION ALL
SELECT 'dNTP set 4x25 umol', 'LOT-e851af49', 4.0, '1.0', '2024-05-31'
        UNION ALL
SELECT 'PCR nucleotide mix 10mM', 'LOT-c1f1b693', 1.0, '200.0', '2024-01-04'
        UNION ALL
SELECT 'flexi DNA polymerase', 'LOT-0667425c', 12.0, '500.0', '2020-09-18'
        UNION ALL
SELECT 'GO Taq G2 flexi DNA polymerases 5u/ul', 'LOT-cd3e0d72', 18.0, '500.0', '2022-11-12'
        UNION ALL
SELECT 'Go Taq flexi DNA polymerase', 'LOT-5975b7af', 3.0, '500.0', '2021-10-15'
        UNION ALL
SELECT 'MgCl2 -25mM', 'LOT-2b65967b', 15.0, '1.2', '2021-10-15'
        UNION ALL
SELECT 'green Go Taq buffer', 'LOT-feb425f4', 20.0, '1.0', '2021-10-15'
        UNION ALL
SELECT 'colorless Go Taq buffer', 'LOT-bb3907fa', 20.0, '1.0', '2021-10-15'
        UNION ALL
SELECT 'NPCR components (Go Taq)', 'LOT-eceb991c', 13.0, '', '2020-09-18'
        UNION ALL
SELECT 'Flexi Go Taq DNA polymerase', 'LOT-ae1db1af', 1.0, '', '2024-04-11'
        UNION ALL
SELECT 'F-Rv+Fw', 'LOT-2778cde1', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'O-Rv+Fw', 'LOT-82a585eb', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'M-Rv+Fw', 'LOT-58c37993', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'V-Rv+Fw', 'LOT-7470ce73', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'K-Rv+Fw', 'LOT-d3f4f99c', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'P-Rv+Fw', 'LOT-c7819e2b', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'rCutSmart Buffer', 'LOT-f33bac92', 1.0, '1.25', '2025-04-01'
        UNION ALL
SELECT 'rCutSmart Buffer', 'LOT-dc23d31f', 1.0, '1.25', '2025-02-01'
        UNION ALL
SELECT 'rCutSmart Buffer', 'LOT-c7ae7ea4', 1.0, '1.25', '2024-07-01'
        UNION ALL
SELECT 'rCutSmart Buffer', 'LOT-46adf2a7', 1.0, '1.25', '2025-04-01'
        UNION ALL
SELECT 'gel loading dye purple', 'LOT-deeb0c5c', 1.0, '0.5', '2024-12-01'
        UNION ALL
SELECT 'gel loading dye purple', 'LOT-a63ed7eb', 1.0, '0.5', '2024-09-01'
        UNION ALL
SELECT 'gel loading dye purple', 'LOT-5533add5', 1.0, '0.5', '2024-03-01'
        UNION ALL
SELECT 'gel loading dye purple', 'LOT-b24bd129', 1.0, '0.5', '2024-05-01'
        UNION ALL
SELECT 'NEBuffer', 'LOT-10e6ddf5', 1.0, '1.25', '2024-12-01'
        UNION ALL
SELECT 'NEBuffer', 'LOT-5b4015cd', 2.0, '1.25', '2024-10-01'
        UNION ALL
SELECT 'HhaI', 'LOT-bc0d81a1', 1.0, '0.1', '2024-02-01'
        UNION ALL
SELECT 'EcoRI-HF', 'LOT-61f999db', 2.0, '1.25', '2024-03-01'
        UNION ALL
SELECT 'BgI-II', 'LOT-558a04e1', 1.0, '0.2', '2023-08-01'
        UNION ALL
SELECT 'Dde-I', 'LOT-415ce261', 1.0, '0.1', '2023-07-01'
        UNION ALL
SELECT 'Pst-I', 'LOT-e4e1772a', 1.0, '0.5', '2023-11-01'
        UNION ALL
SELECT 'BIO Taq DNA polymerase', 'LOT-7d7670c8', 20.0, '100.0', '2023-12-30'
        UNION ALL
SELECT 'NH4 reaction buffer, MgCl2 free', 'LOT-8b21fd22', 40.0, '1.2', '2023-12-30'
        UNION ALL
SELECT 'MgCl2 ', 'LOT-92d436b7', 20.0, '1.2', '2023-12-30'
        UNION ALL
SELECT 'RQ1 RNase-Free DNase', 'LOT-f3925152', 1.0, '', '2016-04-30'
        UNION ALL
SELECT 'DNase I, RNase-free', 'LOT-60da205b', 15.0, '', '2025-06-01'
        UNION ALL
SELECT 'RNase inhibitor', 'LOT-66a3a7f1', 5.0, '', '2019-07-01'
        UNION ALL
SELECT 'RNase inhibitor', 'LOT-806b0e1f', 1.0, '', '2017-08-01'
        UNION ALL
SELECT 'High capacity cDNA Reverse T kit', 'LOT-5453371c', 1.0, '', '2015-06-23'
        UNION ALL
SELECT 'One step RT-qPCR kit 2500rxn', 'LOT-767bc47e', 1.0, '', '2022-04-01'
        UNION ALL
SELECT 'One step RT-qPCR kit 500rxn', 'LOT-09952dc2', 1.0, '', '2019-08-01'
        UNION ALL
SELECT 'One step RT-SuperMix kit 25rxn', 'LOT-5e09d2c2', 1.0, '', '2020-02-01'
        UNION ALL
SELECT 'Luna warm start RT enzyme mix, 20x conc', 'LOT-44472b7b', 1.0, 'used', '2019-08-31'
        UNION ALL
SELECT 'Luna universal probe 1 step reaction mix 2x conc', 'LOT-21be3872', 1.0, '1.0', '2020-01-30'
        UNION ALL
SELECT 'GO Taq PCR master mix 2X c0nc', 'LOT-668f7796', 2.0, 'used', '2020-05-28'
        UNION ALL
SELECT 'MQ', 'LOT-d8969f40', 1.0, '1.5', '2020-11-30'
        UNION ALL
SELECT 'Luna One step RT-qPCR kit 2,500 rxn', 'LOT-192ec1c7', 2.0, '', '2021-04-30'
        UNION ALL
SELECT 'Luna One step RT-qPCR kit 2,500 rxn', 'LOT-d5eaf3de', 2.0, '', '2022-04-30'
        UNION ALL
SELECT 'Luna One step RT-qPCR kit 2,500 rxn', 'LOT-3a0e2d10', 6.0, '', '2024-01-30'
        UNION ALL
SELECT 'Luna One step RT-qPCR kit 2,500 rxn', 'LOT-1e6a3a8e', 6.0, '', '2024-01-30'
        UNION ALL
SELECT 'High capacity cDNA rt kit', '4.0', 1.0, '1.0', '2023-01-31'
        UNION ALL
SELECT 'UD2-R_2_P.PIGNATELL', 'LOT-8d46ec61', 1.0, '', '2022-08-09'
        UNION ALL
SELECT 'St-F_2_P.PIGNATELL', 'LOT-7ef250ea', 1.0, '', '2022-08-09'
        UNION ALL
SELECT 'U5.8S-F_2_P.PIGNATELL', 'LOT-384811b9', 1.0, '', '2022-08-09'
        UNION ALL
SELECT 'ITS2-steph-R_P.PIGNATELL', 'LOT-c5e10045', 1.0, '', '2021-07-26'
        UNION ALL
SELECT 'ITS2A_P.PIGNATELL', 'LOT-0c5a573c', 1.0, '', '2021-07-26'
        UNION ALL
SELECT 'ITS2B_mod2_P.PIGNATELL', 'LOT-b018cca9', 1.0, '', '2021-07-26'
        UNION ALL
SELECT 'OVM_115402_C8', 'LOT-b2772c81', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'OVM_115394_C4', 'LOT-87356df8', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pfr364af_fw', 'LOT-ee9e14c2', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pfr364af_rb', 'LOT-450e4e9a', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pvdhfrv_fw', 'LOT-0f6099fd', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pvdhfrv_rv', 'LOT-dd371dbf', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Hrp2 exon 2 fwd', 'LOT-13cc5fdc', 1.0, '', '2022-01-31'
        UNION ALL
SELECT 'Hrp2 exon 2 rev', 'LOT-5b79e0e7', 1.0, '', '2022-01-31'
        UNION ALL
SELECT 'Hrp3 rev ', 'LOT-bcb42c71', 1.0, '', '2022-01-31'
        UNION ALL
SELECT 'Hrp3 fwd', 'LOT-06678907', 1.0, '', '2022-01-31'
        UNION ALL
SELECT 'Trna rev', 'LOT-7c193ed8', 1.0, '', '2022-01-31'
        UNION ALL
SELECT 'Trna fwd', 'LOT-47a2e7c3', 1.0, '', '2022-01-31'
        UNION ALL
SELECT 'Hrp2 exon 2 probe', 'LOT-69f9e062', 1.0, '', '2022-01-29'
        UNION ALL
SELECT 'Hrp2 probe', 'LOT-e2bc2d85', 1.0, '', '2022-02-17'
        UNION ALL
SELECT 'Hrp3 probe', 'LOT-3247717a', 1.0, '', '2022-02-17'
        UNION ALL
SELECT 'Trna probe', 'LOT-eefd67f1', 1.0, '', '2022-02-02'
        UNION ALL
SELECT 'Pvmsp2_n_rev', 'LOT-af419b22', 1.0, '', '2022-02-05'
        UNION ALL
SELECT 'Pvmsp2_p_fwd', 'LOT-12090479', 1.0, '', '2022-02-05'
        UNION ALL
SELECT 'Pvmsp2_n_fwd', 'LOT-dda527be', 1.0, '', '2022-01-28'
        UNION ALL
SELECT 'Pvmsp2_p_rev', 'LOT-82a4ccad', 1.0, '', '2022-02-05'
        UNION ALL
SELECT 'Pvmsp1f3_p_rev', 'LOT-c75a0aef', 1.0, '', '2022-02-09'
        UNION ALL
SELECT 'Pvmsp1f3_n_fwd', 'LOT-a3985dc0', 1.0, '', '2022-01-28'
        UNION ALL
SELECT 'Pvmsp1f3_p_fwd', 'LOT-48a90796', 1.0, '', '2022-02-09'
        UNION ALL
SELECT 'Pvmsp1f3_n_rev', 'LOT-bcbdbd66', 1.0, '', '2022-02-05'
        UNION ALL
SELECT 'Msp2_s1 tail_fwd', 'LOT-959ad871', 1.0, '', '2022-02-05'
        UNION ALL
SELECT 'Msp2_fc27_m5_rev', 'LOT-d8e6a19c', 1.0, '', '2022-01-28'
        UNION ALL
SELECT 'Msp2_3d7_n5_rev', 'LOT-e8c6a3dc', 1.0, '', '2022-01-28'
        UNION ALL
SELECT 'Pfmsp2_s2_fw', 'LOT-57239617', 1.0, '', '2022-02-05'
        UNION ALL
SELECT 'Pfmsp2_s3_rv', 'LOT-b390e400', 1.0, '', '2022-02-05'
        UNION ALL
SELECT 'ed protein (hyp8) rv', 'LOT-bc3749e1', 1.0, '', '2026-12-31'
        UNION ALL
SELECT '563ct_rv', 'LOT-661d9d5e', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'rPLU3', 'LOT-b83073e7', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'rPLU4', 'LOT-1cef7b2b', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'TARE_2_fw', 'LOT-ba46af4b', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'TARE_2_rv', 'LOT-24378a7f', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'RESA rv', 'LOT-431b345d', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'RESA fw', 'LOT-c82d1aa5', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pfs25_fw', 'LOT-4c6fc1a1', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'rPLU1', 'LOT-2f26c6fc', 1.0, '', '2026-12-31'
        UNION ALL
SELECT '376AG fw', 'LOT-08e5742c', 1.0, '', '2026-12-31'
        UNION ALL
SELECT '376AG rv', 'LOT-153ce589', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pv18s_ fw', 'LOT-7a158c34', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pv18s_ rv', 'LOT-7ede5ec0', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pmal_qPCR_ fw', 'LOT-66ba2347', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pmal_qPCR_ rv', 'LOT-727d2fb3', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pova_qPCR_fw', 'LOT-3fb62d5e', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pova_qPCR_rv', 'LOT-d4ca0791', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'PgMET_fw', 'LOT-d779498b', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'PgMET_rw', 'LOT-dfd77b58', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pvs25 fw', 'LOT-aae17666', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pvs25 rev', 'LOT-90cf80a7', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'QMAL fw', 'LOT-99309c5c', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'QMAL rv', 'LOT-7ee6d1c0', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'QMAL fw', 'LOT-1ef63bcb', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'QMAL rv', 'LOT-a01af190', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pfs230p_fw', 'LOT-2ddbabf7', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pfs230p_rv', 'LOT-db01d4c2', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'varATS_fw', 'LOT-09e421d0', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'varATS_rv', 'LOT-eea7f7d6', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'varATS_fw', 'LOT-20e2327c', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'varATS_rv', 'LOT-62ad2061', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pf18S RV 100uM', 'LOT-8b431aab', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'PV18S FW MPX 100uM', 'LOT-b648dada', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'Pf18S probe 100uM', 'LOT-abbd215c', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'Pv18S MPX 100uM', 'LOT-87535308', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'PV18S RV 100uM  ', 'LOT-624f05ac', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'Pf18s FW 100uM', 'LOT-a5636c45', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'PV25 probe 5.3 nMol', 'LOT-70aa71fd', 1.0, 'lyophilized ', '2026-12-31'
        UNION ALL
SELECT 'PfMDR1 probe 5.1 nMol', 'LOT-e2faf25d', 1.0, 'lyophilized ', '2026-12-31'
        UNION ALL
SELECT 'Beta TT probe 5.2 nMol', 'LOT-f379849d', 1.0, 'lyophilized ', '2026-12-31'
        UNION ALL
SELECT 'Pf plasmepsin2 probe 5.3 nMol', 'LOT-1730c396', 1.0, 'lyophilized ', '2026-12-31'
        UNION ALL
SELECT 'Viv f(shako) ', 'LOT-67f544bb', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'Fal-F(shako)', 'LOT-71d64c0a', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'Pf18S shoko probe 5.3 nMOL', 'LOT-7f6414d6', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'Pv18s SHOKO probe ', 'LOT-47e235b2', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'Plasmo rev 100UM Shoko (for Pv- Pf mpx)', 'LOT-1a7e4b49', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'Pv 18S REV', 'LOT-1d47fdc3', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'Pv-25 Fw', 'LOT-631c0b0c', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pv-25 Rev', 'LOT-62936f36', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pv-25 Rv', 'LOT-caa817fe', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pvs25 Fw', 'LOT-a9965484', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pv-18s new short', 'LOT-5370b857', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pf-18s- DNA-Rv', 'LOT-84a09e67', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pf-18s- DNA-Fw', 'LOT-a821b9ab', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'StD2', 'LOT-1febbef3', 1.0, '300ul', '2026-12-31'
        UNION ALL
SELECT 'GATA-1F', 'LOT-dd1386a4', 1.0, '1000ul', '2026-12-31'
        UNION ALL
SELECT 'GATA-1R', 'LOT-89b5124f', 1.0, '1000ul', '2026-12-31'
        UNION ALL
SELECT 'hrp2-Exon-FW', 'LOT-4bff0706', 1.0, '600ul', '2026-12-31'
        UNION ALL
SELECT 'tRNA-FW', 'LOT-be0a65ef', 1.0, '600ul', '2026-12-31'
        UNION ALL
SELECT 'hrp3-FW', 'LOT-fb18b8a3', 1.0, '600ul', '2026-12-31'
        UNION ALL
SELECT 'uq-R', 'LOT-af987127', 1.0, '600ul', '2026-12-31'
        UNION ALL
SELECT 'UD2-R', 'LOT-a7797cf6', 1.0, '600ul', '2026-12-31'
        UNION ALL
SELECT 'St-F', 'LOT-0cca2f72', 1.0, '600ul', '2026-12-31'
        UNION ALL
SELECT 'StD2-R', 'LOT-8c437143', 1.0, '600ul', '2026-12-31'
        UNION ALL
SELECT 'us-85-F', 'LOT-6fd9217f', 1.0, '600ul', '2026-12-31'
        UNION ALL
SELECT 'st-F', 'LOT-5c655bbc', 1.0, '600ul', '2026-12-31'
        UNION ALL
SELECT 'UD2-R', 'LOT-1ea59ec1', 1.0, '600ul', '2026-12-31'
        UNION ALL
SELECT 'tRNA-Rv', 'LOT-88ff41c0', 1.0, '600ul', '2026-12-31'
        UNION ALL
SELECT 'hrp2-Exon2-RV', 'LOT-9482cdca', 1.0, '600ul', '2026-12-31'
        UNION ALL
SELECT 'HumTuBB-R', 'LOT-1ada10b8', 1.0, '600ul', '2026-12-31'
        UNION ALL
SELECT 'Uq-R', 'LOT-480f59a1', 1.0, '600ul', '2026-12-31'
        UNION ALL
SELECT 'U5.8S-F', 'LOT-a7bfb759', 1.0, '600ul', '2026-12-31'
        UNION ALL
SELECT 'Pfldh-F', 'LOT-2e342064', 1.0, '600ul', '2026-12-31'
        UNION ALL
SELECT 'Pfldh-R', 'LOT-5e4008eb', 1.0, '1200ul', '2026-12-31'
        UNION ALL
SELECT 'HumTubBB-F', 'LOT-a0f680d5', 1.0, '900ul', '2026-12-31'
        UNION ALL
SELECT 'stq-F', 'LOT-6c329712', 1.0, '1200.0', '2026-12-31'
        UNION ALL
SELECT 'stq-R', 'LOT-66bf3fb8', 1.0, '900ul', '2026-12-31'
        UNION ALL
SELECT 'uq-F', 'LOT-bd498472', 1.0, '1200.0', '2026-12-31'
        UNION ALL
SELECT 'Pf Hrp3-R2', 'LOT-014becfb', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Hrp3-Rv', 'LOT-8997c9a2', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'PfHrp2-F1', 'LOT-a1bb593a', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'PfHrp2-R3', 'LOT-24aec177', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'PfHrp2-F2', 'LOT-09221fab', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'PfHrp3-R1', 'LOT-0274d07c', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'PfHrp2-R2', 'LOT-66fb5290', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pf Hrp3-F1', 'LOT-3203a9c4', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pf Hrp3-P1', 'LOT-096b58b6', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'PfHrp2-R1', 'LOT-0d0e45d8', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'PfHrp3-F2', 'LOT-ef2da4f5', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'PfHrp2-F3', 'LOT-9db489d2', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pfhrp3_F2', 'LOT-a38d8da2', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pfhrp3_R2', 'LOT-15c00761', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'GATA-MT-probe', 'LOT-afbfc1bd', 1.0, '300ul', '2026-12-31'
        UNION ALL
SELECT 'GATA-WT-probe', 'LOT-92ab5b06', 1.0, '600ul', '2026-12-31'
        UNION ALL
SELECT '2-probe', 'LOT-5bb4c7c0', 1.0, '600ul', '2026-12-31'
        UNION ALL
SELECT 'tRNA-probe', 'LOT-a3ff5634', 1.0, '900ul', '2026-12-31'
        UNION ALL
SELECT 'hrp3-probe', 'LOT-048fdc77', 1.0, '600ul', '2026-12-31'
        UNION ALL
SELECT 'Pfhrp2-probe', 'LOT-84cb21c1', 1.0, '600ul', '2026-12-31'
        UNION ALL
SELECT 'Pfidh-probe', 'LOT-6e725e20', 1.0, '600ul', '2026-12-31'
        UNION ALL
SELECT 'Pfhrp3-probe', 'LOT-f724e723', 1.0, '300ul', '2026-12-31'
        UNION ALL
SELECT 'Stq-P probe', 'LOT-513fecee', 1.0, '600ul', '2026-12-31'
        UNION ALL
SELECT 'Uq-P probe', 'LOT-3f15cf17', 1.0, '600ul', '2026-12-31'
        UNION ALL
SELECT 'HumanTuBB-P probe', 'LOT-60fec7b5', 1.0, '300ul', '2026-12-31'
        UNION ALL
SELECT 'Pfhrp3_probe', 'LOT-22ddcb61', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pf18s-Fw (Nij)', 'LOT-4c441946', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pv18s-Fw', 'LOT-892f72c0', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pv18s-fw', 'LOT-6df6c6ff', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pf18s-Rv (Nij)', 'LOT-78915c4a', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pv18s-rv', 'LOT-2a8eaff1', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'PfMGET-fw', 'LOT-84daf133', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'PfMGET-Rv', 'LOT-577f0939', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'PfMGET- FW', 'LOT-ebee1ab8', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'PfMGET- Rev multiplex', 'LOT-c7120aa5', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pf18s-RNA-fw', 'LOT-68342622', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pv18s -fw', 'LOT-610dd441', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pv18s-Rv', 'LOT-fc4a98b2', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pv18s-Rv', 'LOT-ec4a1eb0', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pv18s-fw', 'LOT-e838f78d', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pf18s-RNA-RV', 'LOT-72921271', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pfs25_FW', 'LOT-a1bf7dcb', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pfs25_Rv', 'LOT-8c21b131', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pfs25_FW', 'LOT-9e97e12b', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pfs25_Rv', 'LOT-09bcb41b', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'SBP1-RV', 'LOT-fced796a', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'SBP1-Fw', 'LOT-b8c38b17', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pfs25-Rv', 'LOT-e41c746c', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pf18s-DNA-RV(Swit)', 'LOT-f409f170', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pf18s_Nji_RV', 'LOT-e715d873', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pf18s-Rv', 'LOT-8e678212', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pf18s-DNA-FW(Swit)', 'LOT-fb561164', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pf18s_Nji_FW', 'LOT-a92fd7e7', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pf18s Rev', 'LOT-4d677b25', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pf18s for', 'LOT-73e037d5', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pv-18s Fw', 'LOT-6ebaf2d0', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pv-18s Rv', 'LOT-81a0b9c0', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'CCp4 Rev', 'LOT-653b10b6', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'CCp4 Fw', 'LOT-98f4afbe', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pfs25-FW', 'LOT-b2fa501f', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'PfMGET probe (FAM)', 'LOT-222b3419', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'CCP4 probe TEXAS-RED)', 'LOT-76c8380d', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pvs25 probe (FAM)', 'LOT-de0e948e', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pv18s probe (HEX)', 'LOT-1f17b025', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pvs25 probe (FAM)', 'LOT-71d16589', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pv18s probe (FAM)', 'LOT-7217a933', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pvs25 probe (FAM)', 'LOT-50587f17', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'ITS2B-mod2, 20.5nmol', 'LOT-6bb055c1', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'PAR, 24.7nmol', 'LOT-63495fef', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'UN, 19.8nmol', 'LOT-0784a4d1', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Stq-F, 21.2nmol', 'LOT-7dfd9b9e', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'LESS, 22.8', 'LOT-4f96c15b', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'RIV,23.0 nmol', 'LOT-8de07e2a', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'UD2, 22.3nmol', 'LOT-bb78c1c6', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'VAN, 22.5nmol', 'LOT-4d10eee8', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'FUN,19.9nmol', 'LOT-7bd52236', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'ITS2A, 19.5nmol', 'LOT-ce3a2166', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'ITS2-steph-R, 22.2nmol', 'LOT-acef3910', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'U5.8S-F', 'LOT-d2d81422', 1.0, '', '2026-12-31'
        UNION ALL
SELECT '02Ga FW 100uM', 'LOT-91969d26', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT '63CT FW 100 uM', 'LOT-0abecbd0', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT '02GA RV 100 uM', 'LOT-5d13a335', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT '63CT RV 100 uM', 'LOT-efc890e7', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT '76AG FW 100uM', 'LOT-ec97120d', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT '76AG RV 100uM', 'LOT-305b3573', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'N1RPFMDR1034 100UM', 'LOT-33cd7cd6', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'N2FPFMDR1034 100UM', 'LOT-f8cad5fb', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'N1FPFMDR1034', 'LOT-1511ccd0', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'N2RPFMDR1034', 'LOT-e389301f', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'N1FPFMDR86 100UM', 'LOT-26499933', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'N1RPFMDR86 100UM', 'LOT-bf3976da', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'N2FPFMDR86 100UM', 'LOT-230f5b16', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'N2RPFMDR86 100UM', 'LOT-d7fe0642', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'Ovis F 134.9 nMol (Sheep)', 'LOT-9315c4fa', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'Can 2F 125 nMol', 'LOT-92086b89', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'ME (1) 143.5 nMol', 'LOT-23024c7e', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'CAN 2R (2) 161.6 nMol', 'LOT-434292d9', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'UN (1) 139.8 nMol', 'LOT-418f3354', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'Ovis 2R (2) 140.4 nMol (sheep)', 'LOT-5a6bacdb', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'Hmn F (2) 126.3 nMol', 'LOT-eb66a512', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'AR (1) 130.4 nMol', 'LOT-30cdbd00', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'Sus F 145.9 nMol', 'LOT-6aa9403e', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'Bos 2F (2) 149nMol', 'LOT-6e685d38', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'Bos 2R 122.2 nMol', 'LOT-31ce7118', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'Hmn R (1) 145.5 nMol', 'LOT-ba48857d', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'Sus R (2) 147.5 nMol', 'LOT-d1d22e94', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'ITS 2A(2) 125.7nMol', 'LOT-b0fbd74f', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'ITS2-Steph-R(2) 143.2 nMol', 'LOT-cd930f62', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'GA 170.3 nMol', 'LOT-309b37ca', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'Hco2 198 (1) 113.7 nMol', 'LOT-1f3e2d77', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'LCO 1490(2) 99.7 nMol', 'LOT-a6572f13', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'S200x6.1-F 132.3 nMol', 'LOT-200359f9', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'Cap3R 126.8 nMol', 'LOT-e4e4d476', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'plsR (revers primer)(1) 644.4 nMol', 'LOT-a4834ce0', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'S200X6.1-R(2) 157.2 nMol', 'LOT-4da9c24b', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'PlsF(forward primer)(1) 738.6 nMol', 'LOT-123ad492', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'Cap3F 136.8 nMol', 'LOT-f177180f', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'QD(1) 134.9 nMol', 'LOT-adbff029', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'N1RPFmdr86 41.9.nMol', 'LOT-b0c30790', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'N2RPVmdr1 76.4.nMol', 'LOT-71c91f11', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'N1RPFcrt 41.1 nMol', 'LOT-5fea4a30', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'N1FPFmdr86 47.0.nMol', 'LOT-006c03eb', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'N2RPFmdr1034 43.9 nMol', 'LOT-2d5c678b', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'N2FPFmdr 1034 61.5 nMol', 'LOT-ba0118e9', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'N2 FPFmdr86 39.1nMol', 'LOT-8b8ea9c7', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'N2FPFcrt 40.6nMol', 'LOT-a714537a', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'N1RPVmdr1 77.7nMol', 'LOT-a8039991', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'N1FPVmdr1 75.5nMol', 'LOT-78e3cb1c', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'N1RPFmdr1034 61.2 nMol', 'LOT-17fd7c74', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'N2FPVmdr1 60.9nMol', 'LOT-433f8b60', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'MDR3 (sense) for PFMDR1', 'LOT-f40a52e1', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'MDR4 (anti-sense) for PFMDR1', 'LOT-e783dc2d', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'MDR3 (sence) ', 'LOT-94104ec4', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'PV-25 FW', 'LOT-2206f587', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'PV-25 RV', 'LOT-41ccfc49', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'CCp4 FW', 'LOT-d49ef68a', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'PF MGET ', 'LOT-e7549595', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'CCp4RV', 'LOT-d68af66b', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'CCp4 RV', 'LOT-31e7af3f', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'CCp4 FW', 'LOT-03d04a9f', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'Pf MGET RV', 'LOT-227e4854', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'CCp4 probe ', 'LOT-f8ca8537', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'Pf MGET probe', 'LOT-a26a97f5', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'M1-RR primer 2.5 nMol', 'LOT-d84c62f0', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'M1-RF primer 2.5 nMol', 'LOT-a2718ca0', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'MGET FW 50uM', 'LOT-d40ef3be', 1.0, 'working', '2026-12-31'
        UNION ALL
SELECT 'MGET RV 50uM', 'LOT-aba845a0', 1.0, 'working', '2026-12-31'
        UNION ALL
SELECT 'CCp4 forward reverse 50uM', 'LOT-877474cf', 1.0, 'working', '2026-12-31'
        UNION ALL
SELECT 'CCp4 forward  50uM', 'LOT-ce5d52e1', 1.0, 'working', '2026-12-31'
        UNION ALL
SELECT 'CCp4 reverse 50uM', 'LOT-f550f622', 1.0, 'working', '2026-12-31'
        UNION ALL
SELECT 'HumTuBB RV 100mM', 'LOT-bc10e0f8', 1.0, 'stock (100mM)', '2026-12-31'
        UNION ALL
SELECT 'HumTuBB FW 100mM', 'LOT-145249bd', 1.0, 'stock (100mM)', '2026-12-31'
        UNION ALL
SELECT 'COX1 Plasmo F ', 'LOT-e64d5fd3', 1.0, 'stock (100mM)', '2026-12-31'
        UNION ALL
SELECT 'COX1 Plasmo R', 'LOT-4989e815', 1.0, 'stock (100mM)', '2026-12-31'
        UNION ALL
SELECT 'Cow 121 F', 'LOT-a1364f2f', 1.0, 'stock (100mM)', '2026-12-31'
        UNION ALL
SELECT 'Dog 368 F', 'LOT-ced2716a', 1.0, 'stock (100mM)', '2026-12-31'
        UNION ALL
SELECT 'Human 741 F', 'LOT-938121bd', 1.0, 'stock (100mM)', '2026-12-31'
        UNION ALL
SELECT 'Pig 573 F', 'LOT-79371b9a', 1.0, 'stock (100mM)', '2026-12-31'
        UNION ALL
SELECT 'Got 894 F', 'LOT-4be9507a', 1.0, 'stock (100mM)', '2026-12-31'
        UNION ALL
SELECT 'UnRev1025', 'LOT-c12e395e', 1.0, 'stock (100mM)', '2026-12-31'
        UNION ALL
SELECT '100 bp ladder (ready made)', 'LOT-276a3dc2', 7.0, '1.25', '2018-08-31'
        UNION ALL
SELECT 'Ultera low range DNAladder, 0.5ug/ul', 'LOT-29c42209', 2.0, '', '2019-11-30'
        UNION ALL
SELECT 'Trakit cyan/yellow loading buffer, 6x', 'LOT-0d483026', 4.0, '0.5', '2019-12-01'
        UNION ALL
SELECT 'Ultera low range DNAladder, 0.5ug/ul (used)', 'LOT-d1f6fd01', 3.0, 'Used', '2019-11-30'
        UNION ALL
SELECT '100bp DNA ladder', 'LOT-8e5d9936', 9.0, '250.0', '2025-02-04'
        UNION ALL
SELECT 'blue/orange loading day 6X conc', 'LOT-661d5bac', 8.0, '1.0', '2025-02-04'
        UNION ALL
SELECT 'gel loading dye purple 6X conc', 'LOT-bb6efa67', 1.0, '0.5', '2024-01-30'
        UNION ALL
SELECT 'gel loading dye Blue/orange 6X conc', 'LOT-20909b98', 1.0, '1.0', '2025-07-03'
        UNION ALL
SELECT '100bp DNA lader with loadind dye(Blue/orange 6x)', 'LOT-fb3e6f0d', 1.0, '250.0', '2026-09-13'
        UNION ALL
SELECT 'cyan/orange loading buffer', 'LOT-9338cbe1', 2.0, '0.2', '2026-12-31'
        UNION ALL
SELECT '50bp DNA ladder', 'LOT-8ef44257', 1.0, '50.0', '2026-12-31'
        UNION ALL
SELECT '100bp DNA ladder', 'LOT-e7f1b310', 2.0, '', '2026-12-31'
        UNION ALL
SELECT '5x DNA loading buffer (blue)', 'LOT-44827642', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'blue/orange 6x oading dye', 'LOT-defbef3b', 5.0, '1.0', '2017-09-24'
        UNION ALL
SELECT 'ultra low rabge DNA ladder', 'LOT-8d770fc0', 1.0, '50.0', '2026-12-31'
        UNION ALL
SELECT '1kb + DNA lader', 'LOT-e458e12d', 2.0, '250.0', '2026-12-31'
        UNION ALL
SELECT 'hyper ladder 100bp 100 lans', 'LOT-526d5ed1', 2.0, '', '2026-12-31'
        UNION ALL
SELECT 'primer IPCF -2.5 mM', 'LOT-889f5408', 2.0, '1.0', '2022-02-01'
        UNION ALL
SELECT 'primer west -26uM', 'LOT-490ffaba', 1.0, '1.0', '2022-02-01'
        UNION ALL
SELECT 'primer WT (west) 25uM', 'LOT-073ddba3', 1.0, '1.0', '2022-02-01'
        UNION ALL
SELECT 'primer WT (east) ', 'LOT-628af9c1', 1.0, '1.0', '2022-02-01'
        UNION ALL
SELECT 'primer east ', 'LOT-5351abfe', 1.0, '1.0', '2022-02-01'
        UNION ALL
SELECT 'primer ALT rev ', 'LOT-2bb2a957', 2.0, '1.0', '2022-02-01'
        UNION ALL
SELECT 'An.gambiae RSP-ST', 'LOT-ec4da5ad', 1.0, '10.0', '2020-11-23'
        UNION ALL
SELECT 'An.coluzzii AKDR', 'LOT-968a1570', 1.0, '10.0', '2020-11-24'
        UNION ALL
SELECT 'ITS2A (1) ', 'LOT-f20c0d22', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'ITS2-Steph-R(1) ', 'LOT-434c5218', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'QD', 'LOT-9959dacd', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'UN', 'LOT-83ebdf66', 2.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'GA', 'LOT-df86d6f0', 2.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'AR', 'LOT-876ef8c9', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'BW', 'LOT-22764cfa', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'QDA', 'LOT-47d9aafd', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'ME', 'LOT-728625cc', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'GA RV', 'LOT-5a312e22', 1.0, 'stock (100mM)', '2026-12-31'
        UNION ALL
SELECT 'AR RV', 'LOT-ee9ae39a', 1.0, 'stock (100mM)', '2026-12-31'
        UNION ALL
SELECT 'QDA RV', 'LOT-7e1e5f94', 1.0, 'stock (100mM)', '2026-12-31'
        UNION ALL
SELECT 'ME RV', 'LOT-742629bf', 1.0, 'stock (100mM)', '2026-12-31'
        UNION ALL
SELECT 'QD RV', 'LOT-e7a3a273', 1.0, 'stock (100mM)', '2026-12-31'
        UNION ALL
SELECT 'UN FW', 'LOT-9bfcfe88', 1.0, 'stock (100mM)', '2026-12-31'
        UNION ALL
SELECT 'IMP-S1', 'LOT-f7405e60', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'QD-3T', 'LOT-eb6f1dff', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'IMP-UN', 'LOT-7e662ea4', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'IMP-M1', 'LOT-db35c28d', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'AR-3T', 'LOT-2a74babe', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'GA-3T', 'LOT-67365d99', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'ME-3T', 'LOT-764061ff', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'An. arabiensis Dong 5 F211', 'LOT-740d19b7', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'An. merus, Maf F185', 'LOT-77427c20', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'An. quadriannus, Sangwe, F180', 'LOT-8b7c020c', 1.0, '', '2026-12-31'
)
INSERT INTO clinlims.inventory_lot (
    id, fhir_uuid, inventory_item_id, lot_number, initial_quantity, current_quantity,
    unit_size, expiration_date, qc_status, status, last_updated, version
)
SELECT
    nextval('clinlims.inventory_lot_seq'),
    gen_random_uuid(),
    ii.id,
    ld.lot_number,
    ld.quantity,
    ld.quantity,
    ld.volume,
    ld.expiry_date::timestamp,
    'PENDING',
    'ACTIVE',
    NOW(),
    1
FROM lot_data ld(item_name, lot_number, quantity, volume, expiry_date)
INNER JOIN clinlims.inventory_item ii ON ii.name = ld.item_name
WHERE ii.project_name = 'Malaria and Neglected Tropical Disease (MNTD) Laboratory';

-- Insert Transactions
INSERT INTO clinlims.inventory_transaction (
    id, lot_id, transaction_type, quantity_change, quantity_after,
    transaction_date, notes, performed_by_user, last_updated
)
SELECT
    nextval('clinlims.inventory_transaction_seq'),
    il.id,
    'RECEIPT',
    il.initial_quantity,
    il.current_quantity,
    NOW(),
    'Initial import from MNTD Excel',
    (SELECT id FROM clinlims.system_user WHERE login_name = 'admin' LIMIT 1),
    NOW()
FROM clinlims.inventory_lot il
INNER JOIN clinlims.inventory_item ii ON il.inventory_item_id = ii.id
WHERE ii.project_name = 'Malaria and Neglected Tropical Disease (MNTD) Laboratory';

COMMIT TRANSACTION;
