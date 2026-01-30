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
SELECT 'Pfcrt-F1', 'LOT-0af93efd', 1.0, 'Received reconstitute', '2026-12-31'
        UNION ALL
SELECT 'Pfcrt-R1 sequence', 'LOT-57df7e88', 1.0, 'Received reconstitute', '2026-12-31'
        UNION ALL
SELECT 'Pfdhps-F', 'LOT-283a243a', 1.0, 'Received reconstitute', '2026-12-31'
        UNION ALL
SELECT 'Pfdhps-R', 'LOT-195da30e', 1.0, 'Received reconstitute', '2026-12-31'
        UNION ALL
SELECT 'plasmepsin 3R', 'LOT-032df67e', 1.0, '', '2021-12-20'
        UNION ALL
SELECT 'plasmepsin 2R', 'LOT-83f99738', 1.0, '', '2021-12-21'
        UNION ALL
SELECT 'plasmepsin 2F', 'LOT-dad39359', 1.0, '', '2021-12-22'
        UNION ALL
SELECT 'plasmepsin 3F', 'LOT-2ff98162', 1.0, '', '2021-12-23'
        UNION ALL
SELECT 'plasmepsin 3P', 'LOT-ed2f9bc7', 1.0, '', '2021-12-24'
        UNION ALL
SELECT 'plasmepsin 2P', 'LOT-e4f023b1', 1.0, '', '2022-01-22'
        UNION ALL
SELECT 'Pf 18s fwd', 'LOT-6ffbd4bc', 2.0, '', '2021-12-20'
        UNION ALL
SELECT 'Pf 18s rev', 'LOT-88d8e59e', 2.0, '', '2021-12-21'
        UNION ALL
SELECT 'Beta tubulin F', 'LOT-95491f33', 1.0, '', '2021-12-22'
        UNION ALL
SELECT 'beta tubulin R', 'LOT-631b6ff7', 1.0, '', '2022-01-21'
        UNION ALL
SELECT 'beta tublin P', 'LOT-2c6064bd', 1.0, '', '2022-01-22'
        UNION ALL
SELECT 'cyt B-F1', 'LOT-ffe0f04f', 1.0, '', '2022-01-23'
        UNION ALL
SELECT 'cyt B-R1', 'LOT-3f59793e', 1.0, '', '2021-12-22'
        UNION ALL
SELECT 'mitochndorion -R1', 'LOT-3b608139', 1.0, '', '2021-12-23'
        UNION ALL
SELECT 'mitochndorion -F1', 'LOT-bdf68a93', 1.0, '', '2021-12-24'
        UNION ALL
SELECT 'Pf k13-F1', 'LOT-f95caf6c', 1.0, '', '2021-12-25'
        UNION ALL
SELECT 'Pf k13-R1', 'LOT-b3efa99f', 1.0, '', '2021-12-26'
        UNION ALL
SELECT 'pfmdr1 F', 'LOT-70265904', 2.0, '', '2021-12-27'
        UNION ALL
SELECT 'pfmdr1 R', 'LOT-e446b470', 2.0, '', '2021-12-28'
        UNION ALL
SELECT 'FastDigest Alul enzyme with buffer ', 'LOT-5c85fd7d', 1.0, '', '2020-12-02'
        UNION ALL
SELECT 'SmartCut enzyme with buffer ', 'LOT-bb19dcbe', 1.0, '', '2018-11-01'
        UNION ALL
SELECT 'mbo II 5000 U/ml', 'LOT-e95cb973', 1.0, '0.3', '2017-05-31'
        UNION ALL
SELECT 'Nla III  10,000 U/ml', 'LOT-a5cbc2c5', 2.0, '0.25', '2017-07-30'
        UNION ALL
SELECT 'Ase I  10,000 U/ml', 'LOT-3e9097f7', 1.0, '0.2', '2017-04-30'
        UNION ALL
SELECT 'Fok I 5,000 U/ml', 'LOT-e88fd682', 1.0, '1.0', '2018-01-30'
        UNION ALL
SELECT 'Fok I 5,000 U/ml (cutsmart)', 'LOT-ac3ff5a6', 1.0, '1.0', '2018-11-30'
        UNION ALL
SELECT 'Dra I  20,000 U/ml', 'LOT-312528d5', 1.0, '0.1', '2017-02-01'
        UNION ALL
SELECT 'ApoI 10,000 U/ml', 'LOT-8ab67ee3', 1.0, '0.1', '2017-03-01'
        UNION ALL
SELECT 'Dde I   10,000 U/ml', 'LOT-daf9e9e6', 1.0, '0.1', '2017-03-02'
        UNION ALL
SELECT 'Eco RV 20,000 U/ml', 'LOT-faf66236', 1.0, '0.2', '2016-08-01'
        UNION ALL
SELECT 'NEBuffer  3.1  10x conc.', 'LOT-1144e195', 5.0, '1.25', '2018-10-01'
        UNION ALL
SELECT 'NEBuffer  4  10x conc.', 'LOT-c41943df', 2.0, '5.0', '2015-03-01'
        UNION ALL
SELECT 'Afa I  1,000 U/ml', 'LOT-40c709ff', 1.0, '', '2011-11-01'
        UNION ALL
SELECT 'Pvu II 15U/ml', 'LOT-ef691db3', 1.0, '3000.0', '2007-11-01'
        UNION ALL
SELECT '10x M buffer', 'LOT-dbcac990', 3.0, '1.0', '2026-12-31'
        UNION ALL
SELECT 'cutsmart buffer  10x conc.', 'LOT-6af24b89', 3.0, '1.25', '2018-07-01'
        UNION ALL
SELECT 'Qiagen rnase free water', 'LOT-de2d58b8', 4.0, '1.9', '2026-12-31'
        UNION ALL
SELECT '1KB + DNA ladder', 'LOT-5d008507', 1.0, '', '2026-12-31'
        UNION ALL
SELECT '0.1% BSA ', 'LOT-6d40febc', 1.0, '1.0', '2026-12-31'
        UNION ALL
SELECT 'pscam II (alpha DNA)', 'LOT-2e2dde2c', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'T2 (alpha DNA)', 'LOT-215a0632', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'ILRI 161108 R1 66.1 nmol', 'LOT-97de1f0b', 1.0, '6060.9', '2026-12-31'
        UNION ALL
SELECT 'ILRI 161108 R2 60.3 nmol', 'LOT-4bd4f4c9', 1.0, '603.1', '2026-12-31'
        UNION ALL
SELECT 'ILRI 161108 R3 60.5 nmol', 'LOT-5d6405c2', 1.0, '604.6', '2026-12-31'
        UNION ALL
SELECT 'ILRI 161108 F3 45.1 nmol', 'LOT-b3d4b840', 1.0, '451.1', '2026-12-31'
        UNION ALL
SELECT 'ILRI 161108 F2 45.1 nmol', 'LOT-5be719b7', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'loading dye', 'LOT-5c823f69', 1.0, 'working', '2026-12-31'
        UNION ALL
SELECT 'Eurofins genomics 2R', 'LOT-6d5ad210', 1.0, '', '2017-09-12'
        UNION ALL
SELECT 'Eurofins genomics 1R', 'LOT-4b9f5293', 1.0, '', '2017-09-12'
        UNION ALL
SELECT 'Eurofins genomics pair 1R', 'LOT-5e9fdc0a', 1.0, '', '2017-07-07'
        UNION ALL
SELECT 'Eurofins genomics pair 2F', 'LOT-ffd463ea', 1.0, '', '2017-07-08'
        UNION ALL
SELECT 'Eurofins genomics pair 2R', 'LOT-355b4a1b', 1.0, '', '2017-07-07'
        UNION ALL
SELECT 'Eurofins genomics 2F', 'LOT-69b1f620', 1.0, '', '2017-09-12'
        UNION ALL
SELECT 'Eurofins genomics 1F', 'LOT-0e9f005a', 1.0, '', '2017-09-13'
        UNION ALL
SELECT 'Eurofins genomics pair 1F', 'LOT-2f207500', 1.0, '', '2017-07-07'
        UNION ALL
SELECT '10x M buffer', 'LOT-7bdebe53', 5.0, '1.0', '2026-12-31'
        UNION ALL
SELECT 'Hae III', 'LOT-51622bb2', 9.0, '10.0', '2007-10-01'
        UNION ALL
SELECT 'Hha I', 'LOT-667242cf', 3.0, '10.0', '2008-03-01'
        UNION ALL
SELECT '10x React 2', 'LOT-c803d3d1', 7.0, '1.0', '2026-12-31'
        UNION ALL
SELECT 'quanti Tect probe PCR mastermix', 'LOT-8c31c732', 2.0, '1.7', '2026-12-31'
        UNION ALL
SELECT '10x buffer T', 'LOT-7e2e8980', 1.0, '1.0', '2026-12-31'
        UNION ALL
SELECT 'Hae III', 'LOT-6568f32e', 1.0, '10.0', '2024-11-01'
        UNION ALL
SELECT 'Hha I', 'LOT-32d05614', 1.0, '0.1', '2014-10-01'
        UNION ALL
SELECT 'Hha1 use with react 2', 'LOT-8476033c', 1.0, '10.0', '2010-02-28'
        UNION ALL
SELECT '10x react I', 'LOT-c89822ce', 1.0, '1.0', '2026-12-31'
        UNION ALL
SELECT '10x PCR buffer (-mgcl2)', 'LOT-bcac1ef7', 1.0, '1.25', '2026-12-31'
        UNION ALL
SELECT 'HuPoR (alpha DNA)(5'' GGACTTCGTTTGTACCCGTTG )', 'LOT-134b317c', 2.0, '', '2026-12-31'
        UNION ALL
SELECT 'b- actin R (5'' CGTCATACTCCTGCTTGCTGATCCACATCTGC)', 'LOT-35d0154f', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'LaeV5f (5'' CGTGATGTGCCCGAGTGCA)', 'LOT-7076f72d', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'cpb EF rev', 'LOT-b6f01488', 1.0, 'stock ', '2026-12-31'
        UNION ALL
SELECT 'cpb EFfwd', 'LOT-80a1b9bf', 1.0, 'stock ', '2026-12-31'
        UNION ALL
SELECT 'K26 fwd', 'LOT-b126f239', 1.0, 'stock ', '2026-12-31'
        UNION ALL
SELECT 'k26 rev', 'LOT-85036f48', 1.0, 'stock ', '2026-12-31'
        UNION ALL
SELECT 'List R', 'LOT-29f4f4c8', 1.0, 'working', '2026-12-31'
        UNION ALL
SELECT '10xFast DigestGreen Buffer', 'LOT-44e44ccf', 1.0, '1.0', '2020-12-01'
        UNION ALL
SELECT '10xFast Digest Buffer (colorless)', 'LOT-889cdbdb', 1.0, '1.0', '2020-12-02'
        UNION ALL
SELECT 'Fast Digest Buffer Alul', 'LOT-1aabb50c', 1.0, '100.0', '2020-12-03'
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
SELECT 'G-OFprimer (Glurp) 2.5 nMol', 'LOT-4996c12b', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'OuterG4 for GLURP FW', 'LOT-a1404b77', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'G-OR 2.5 nMol', 'LOT-5b52e12d', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'OuterG5 for GLURP RV ', 'LOT-7017868c', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'Nested S4 for MSP-2 FW', 'LOT-9deea979', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'Nested S1 for MSP-2 FW', 'LOT-70c42f19', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'MSP-2-S2 FW', 'LOT-4d254611', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'MSP-2-S3 RV', 'LOT-61354836', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'MSP-2-S1 tail fw', 'LOT-1d7b128b', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'MSP2-FC27 RV (FAM dye labeled)', 'LOT-cb44a585', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'MSP2-3D7-RV (VIC dye labeled)', 'LOT-095d9f40', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'M1-OR 2.5 nMol (MSP1M1OR)', 'LOT-0d21b34c', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'MSP1 M1 RV 2.5 nMol', 'LOT-e450c61d', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'MSP1M1-FW 2.5 nMol', 'LOT-69d3e645', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'MSP1M1-OF 2.5 nMol', 'LOT-ce47f313', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'MSP1M1-KF 2.5 nMol', 'LOT-6953aa92', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'MSP1M1-KR 2.5 nMol', 'LOT-b259b5ac', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'Nested N1 for MSP1 FW', 'LOT-29c8ff75', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'Nested N2 for MSP1 RV', 'LOT-f660be6c', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'N2RPfmdr86', 'LOT-4fe247cd', 1.0, 'Lyophilized', '2026-12-31'
        UNION ALL
SELECT 'N2RPfcrt', 'LOT-16dc2698', 1.0, 'Lyophilized', '2026-12-31'
        UNION ALL
SELECT 'N1FPfcrt', 'LOT-039cf448', 1.0, 'Lyophilized', '2026-12-31'
        UNION ALL
SELECT 'N1FPfmdr1034', 'LOT-2211ee6c', 1.0, 'Lyophilized', '2026-12-31'
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
SELECT 'phix control V3', 'LOT-63d88708', 1.0, '', '2026-12-31'
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
SELECT 'Qiagen Exraction kit', 'LOT-06afa934', 2.0, '', '2026-12-31'
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
SELECT 'sample (DNA elution) storage tube', 'LOT-21f2ed9c', 1.0, '25.0', '2026-12-31'
        UNION ALL
SELECT 'Quick gel extraction & PCR Purification combo kit', 'K220001', 3.0, '50.0', '2025-10-31'
        UNION ALL
SELECT 'Bio analyzer-High sensitivity DNA chips', 'Lot: AR01BK50', 1.0, '', '2023-04-01'
        UNION ALL
SELECT 'Bio analyzer-syringe kit', 'G293868706', 2.0, '', '2026-12-31'
        UNION ALL
SELECT 'Top vision Agrose', 'R0492', 3.0, '500.0', '2026-03-01'
        UNION ALL
SELECT 'Qubit 1x ds DNA HS assay kit', 'LOT-5346ef59', 12.0, '', '2026-12-31'
        UNION ALL
SELECT 'Qubit 1x dsL DNA BR assay standard is ', 'LOT-6110bb3e', 12.0, '', '2026-12-31'
        UNION ALL
SELECT 'AmpureXP', 'LOT-816aa140', 6.0, '', '2026-12-31'
        UNION ALL
SELECT 'Hot start MasterMix, high fidelity', 'LOT-f0f575a3', 15.0, '', '2026-12-31'
        UNION ALL
SELECT 'Q5 Blood Direct MasterMix', 'LOT-b6576d2e', 12.0, '', '2026-12-31'
        UNION ALL
SELECT 'cpbF (alpha DNA)', 'LOT-79c4c13c', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'cpbEF-F  (alpha DNA)', 'LOT-31f665bf', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'B4 (alpha DNA)', 'LOT-5cad6a8d', 3.0, '', '2026-12-31'
        UNION ALL
SELECT 'K26 f 15pM', 'LOT-b68cba79', 3.0, '', '2026-12-31'
        UNION ALL
SELECT 'T2 (alpha DNA)', 'LOT-8571ce47', 3.0, '', '2026-12-31'
        UNION ALL
SELECT 'seqcpbr2 (apha DNA)', 'LOT-c00801d7', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Cpbef frd (alpha DNA)', 'LOT-8934c334', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'seqcpbf1 (alpha DNA)', 'LOT-97e0e16b', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'cpbEFrev (alpha DNA)', 'LOT-f9cffb23', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'cpbEF R  (alpha DNA)', 'LOT-4a8a2629', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'dNTPs 10mM', 'LOT-15c24a03', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'vsp15pM', 'LOT-41dad4a3', 3.0, '', '2026-12-31'
        UNION ALL
SELECT 'one-4 phor-all buffer + 10x conc.', 'LOT-e42d5e9a', 1.0, '1.0', '2026-12-31'
        UNION ALL
SELECT 'taq DNA poly', 'LOT-bca0c013', 1.0, '250.0', '2026-12-31'
        UNION ALL
SELECT 'k26r (alpha DNA)', 'LOT-7c057e25', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'LITSR (alpha DNA)', 'LOT-6b726437', 4.0, '', '2026-12-31'
        UNION ALL
SELECT 'L%.8s  (alpha DNA)', 'LOT-44f67ec4', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Laev 10 R', 'LOT-4f818703', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'cpb TAG', 'LOT-f785ce5c', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'cpbr (alpha DNA)', 'LOT-1ce4cb87', 1.0, '150.0', '2026-12-31'
        UNION ALL
SELECT 'cpb ATG (alpha DNA)', 'LOT-bfcd5ac5', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'big dye terminator v 3.1 cycle sequencing RR-24', 'LOT-ac466658', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Ilri-161108F1', 'LOT-5c5b8abc', 1.0, '604.2', '2026-12-31'
        UNION ALL
SELECT 'pgem3Zf(+)', 'LOT-6385718a', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'm13(-21)primer', 'LOT-93df04b9', 1.0, '', '2026-12-31'
        UNION ALL
SELECT '202GA-Rv', 'LOT-823daeb0', 1.0, '', '2026-12-31'
        UNION ALL
SELECT '202GA-Fw', 'LOT-377679a2', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Tailing Segment FW', 'LOT-5c25653f', 1.0, '14.6', '2026-12-31'
        UNION ALL
SELECT 'Tailing Segment RV', 'LOT-111473c2', 1.0, '18.9', '2026-12-31'
        UNION ALL
SELECT 'Tailing Segment FW', 'LOT-1de8fa5b', 1.0, '14.6', '2026-12-31'
        UNION ALL
SELECT 'Tailing Segment RV', 'LOT-e147e99e', 1.0, '18.9', '2026-12-31'
        UNION ALL
SELECT 'hrp2 one-step FW', 'LOT-63666fe5', 1.0, '14.6', '2026-12-31'
        UNION ALL
SELECT 'hrp2 one-step RV', 'LOT-a56b5b07', 1.0, '20.0', '2026-12-31'
        UNION ALL
SELECT 'hrp2 one-step FW', 'LOT-c8a07d47', 1.0, '14.6', '2026-12-31'
        UNION ALL
SELECT 'hrp2 one-step RV', 'LOT-8d7d2fde', 1.0, '20.0', '2026-12-31'
        UNION ALL
SELECT 'N1FPfcrt FW', 'LOT-0581cb06', 1.0, '11.8', '2026-12-31'
        UNION ALL
SELECT 'N1FPfcrt RV', 'LOT-1866ad8e', 1.0, '7.2', '2026-12-31'
        UNION ALL
SELECT 'N1FPfcrt FW', 'LOT-5d6f3750', 1.0, '11.8', '2026-12-31'
        UNION ALL
SELECT 'N1FPfcrt RV', 'LOT-3b249981', 1.0, '7.2', '2026-12-31'
        UNION ALL
SELECT 'N2FPfcrt FW', 'LOT-41986975', 1.0, '13.9', '2026-12-31'
        UNION ALL
SELECT 'N2FPfcrt RV', 'LOT-41600166', 1.0, '12.4', '2026-12-31'
        UNION ALL
SELECT 'N2FPfcrt FW', 'LOT-5bec1dbb', 1.0, '13.9', '2026-12-31'
        UNION ALL
SELECT 'N2FPfcrt RV', 'LOT-4a4eb6ba', 1.0, '12.4', '2026-12-31'
        UNION ALL
SELECT 'N1-Mdr1-86Y Fw', 'LOT-ca36533e', 1.0, '12.3', '2026-12-31'
        UNION ALL
SELECT 'N1-Mdr1-86Y Rv', 'LOT-5f03f0eb', 1.0, '12.9', '2026-12-31'
        UNION ALL
SELECT 'N1-Mdr1-86Y Fw', 'LOT-7bbd152a', 1.0, '12.3', '2026-12-31'
        UNION ALL
SELECT 'N1-Mdr1-86Y Rv', 'LOT-9b2ed586', 1.0, '12.9', '2026-12-31'
        UNION ALL
SELECT 'N2-Mdr1-86Y FW', 'LOT-071f9b7d', 1.0, '16.8', '2026-12-31'
        UNION ALL
SELECT 'N2-Mdr1-86Y Rv', 'LOT-0033a8a8', 1.0, '17.3', '2026-12-31'
        UNION ALL
SELECT 'N2-Mdr1-86Y FW', 'LOT-c82507a5', 1.0, '16.8', '2026-12-31'
        UNION ALL
SELECT 'N2-Mdr1-86Y Rv', 'LOT-e853c5fd', 1.0, '17.3', '2026-12-31'
        UNION ALL
SELECT 'N1FPfcrt FW', 'LOT-e345ce46', 1.0, '12.3', '2026-12-31'
        UNION ALL
SELECT 'N1FPfcrt RV', 'LOT-72e1cb7d', 1.0, '14.1', '2026-12-31'
        UNION ALL
SELECT 'N2FPfcrt FW', 'LOT-589a5136', 1.0, '15.4', '2026-12-31'
        UNION ALL
SELECT 'N2FPfcrt RV', 'LOT-cd37685d', 1.0, '14.0', '2026-12-31'
        UNION ALL
SELECT 'N1-Mdr1-86Y Fw', 'LOT-69999f05', 1.0, '11.4', '2026-12-31'
        UNION ALL
SELECT 'N1-Mdr1-86Y Rv', 'LOT-47074b5e', 1.0, '12.1', '2026-12-31'
        UNION ALL
SELECT 'N1-Mdr1-86Y Fw', 'LOT-a12458c6', 1.0, '12.4', '2026-12-31'
        UNION ALL
SELECT 'N1-Mdr1-86Y Rv', 'LOT-d6ccbd10', 1.0, '12.1', '2026-12-31'
        UNION ALL
SELECT 'N2-mdr1-86Y FW', 'LOT-4affec08', 1.0, '8.6', '2026-12-31'
        UNION ALL
SELECT 'N2-mdr1-86Y RV', 'LOT-77a574ca', 1.0, '10.4', '2026-12-31'
        UNION ALL
SELECT 'N2-mdr1-86Y FW', 'LOT-c7038df6', 1.0, '8.6', '2026-12-31'
        UNION ALL
SELECT 'N2-mdr1-86Y RV', 'LOT-82c6626c', 1.0, '10.4', '2026-12-31'
        UNION ALL
SELECT 'M1-OF', 'LOT-d877ce13', 1.0, '9.6', '2026-12-31'
        UNION ALL
SELECT 'M1-OR', 'LOT-59229707', 1.0, '11.1', '2026-12-31'
        UNION ALL
SELECT 'M1-OF', 'LOT-54b4abbb', 1.0, '9.6', '2026-12-31'
        UNION ALL
SELECT 'M1-OR', 'LOT-5142b2d1', 1.0, '11.1', '2026-12-31'
        UNION ALL
SELECT 'RNaseP-F', 'LOT-49429148', 1.0, '62.1', '2026-12-31'
        UNION ALL
SELECT 'RNaseP-R', 'LOT-9d2635cf', 1.0, '65.4', '2026-12-31'
        UNION ALL
SELECT 'msp-1(N)-FW', 'LOT-a9b2e57d', 1.0, '25.3', '2026-12-31'
        UNION ALL
SELECT 'msp-1(N)-RV', 'LOT-9d0cd562', 1.0, '10.2', '2026-12-31'
        UNION ALL
SELECT 'msp-1(N1)-FW', 'LOT-0175baee', 1.0, '25.3', '2026-12-31'
        UNION ALL
SELECT 'msp-1(N1)-RV', 'LOT-1932693b', 1.0, '10.2', '2026-12-31'
        UNION ALL
SELECT 'msp-outer-F', 'LOT-f498ee80', 1.0, '60.6', '2026-12-31'
        UNION ALL
SELECT 'msp-outer-R', 'LOT-4821d82f', 1.0, '59.7', '2026-12-31'
        UNION ALL
SELECT '3D7/ICfamily(N-2)-FW', 'LOT-0290f36c', 1.0, '25.9', '2026-12-31'
        UNION ALL
SELECT '3D7/ICfamily(N-2)-FW', 'LOT-cf50e51a', 1.0, '25.9', '2026-12-31'
        UNION ALL
SELECT 'msp2-S2-fw', 'LOT-15e8b189', 1.0, '26.5', '2026-12-31'
        UNION ALL
SELECT 'msp2-S3-rev', 'LOT-86d2dc60', 1.0, '24.2', '2026-12-31'
        UNION ALL
SELECT 'msp-2(N1)-FW', 'LOT-9e4be06a', 1.0, '9.3', '2026-12-31'
        UNION ALL
SELECT 'msp-2(N1)-Rv', 'LOT-370ecf0d', 1.0, '28.4', '2026-12-31'
        UNION ALL
SELECT 'msp2-S2-fw', 'LOT-36a189ea', 1.0, '26.5', '2026-12-31'
        UNION ALL
SELECT 'msp2-S3-rev', 'LOT-1206644f', 1.0, '24.2', '2026-12-31'
        UNION ALL
SELECT 'msp-2(N1)-FW', 'LOT-b938f88c', 1.0, '9.3', '2026-12-31'
        UNION ALL
SELECT 'msp-2(N1)-RV', 'LOT-cb2bb012', 1.0, '28.4', '2026-12-31'
        UNION ALL
SELECT 'msp2-S2-F', 'LOT-f0e0730f', 1.0, '57.5', '2026-12-31'
        UNION ALL
SELECT 'msp2-S3-R', 'LOT-0ee41458', 1.0, '56.0', '2026-12-31'
        UNION ALL
SELECT 'msp2-S1Tail-fw', 'LOT-66df23ec', 1.0, '18.9', '2026-12-31'
        UNION ALL
SELECT 'msp2-S1Tail-fw', 'LOT-5058543d', 1.0, '18.9', '2026-12-31'
        UNION ALL
SELECT 'msp2-S1Tail-f', 'LOT-8509fbf8', 1.0, '53.8', '2026-12-31'
        UNION ALL
SELECT 'FC27family(N2)-FW', 'LOT-21514c31', 1.0, '25.9', '2026-12-31'
        UNION ALL
SELECT 'FC27family(N2)-RV', 'LOT-6df15d72', 1.0, '11.8', '2026-12-31'
        UNION ALL
SELECT 'FC27family(N2)-FW', 'LOT-38e8aa23', 1.0, '25.9', '2026-12-31'
        UNION ALL
SELECT 'FC27family(N2)-RV', 'LOT-193e3e4e', 1.0, '11.8', '2026-12-31'
        UNION ALL
SELECT 'msp1-k1-F', 'LOT-df246217', 1.0, '38.8', '2026-12-31'
        UNION ALL
SELECT 'msp1-k1-R', 'LOT-efd3dd89', 1.0, '56.6', '2026-12-31'
        UNION ALL
SELECT 'msp1-Mad20-F', 'LOT-5590ada3', 1.0, '49.6', '2026-12-31'
        UNION ALL
SELECT 'msp1-Mad20-R', 'LOT-4f30c8d3', 1.0, '51.5', '2026-12-31'
        UNION ALL
SELECT 'Fc27', 'LOT-9bda3ffc', 1.0, '57.8', '2026-12-31'
        UNION ALL
SELECT '3D7', 'LOT-898fbf9c', 1.0, '57.4', '2026-12-31'
        UNION ALL
SELECT 'msp2-3D7-N5-RV-probe', 'LOT-f743e06f', 1.0, '20.9', '2026-12-31'
        UNION ALL
SELECT 'msp2-FC27-MS-RV-probe', 'LOT-7ef4d0ba', 1.0, '20.1', '2026-12-31'
        UNION ALL
SELECT 'Nuclase free water', 'LOT-fa4aa68e', 6.0, '1.5', '2026-12-31'
        UNION ALL
SELECT 'NOT I', 'LOT-7daaa9f9', 1.0, '0.05', '2022-09-01'
        UNION ALL
SELECT 'NE buffer 3.1  ', 'LOT-b11f11d8', 1.0, '1.25', '2024-01-01'
        UNION ALL
SELECT 'PhHv10^3', 'LOT-1493da1d', 1.0, '80.0', '2026-12-31'
        UNION ALL
SELECT 'PhHv-26267s ', 'LOT-5cc47965', 1.0, '232.0', '2026-12-31'
        UNION ALL
SELECT 'probephhv-305TQ-cy5', 'LOT-ea6e7185', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'phhv-337as', 'LOT-ad03fcce', 1.0, '210.0', '2011-12-01'
        UNION ALL
SELECT 'ENDFORWARD (Genosys 7112-012)', 'LOT-0caf30d0', 1.0, 'Lyophilized', '2026-12-31'
        UNION ALL
SELECT 'ENDREVERSE (gENOSYS 7112-013)', 'LOT-99b4dd4e', 1.0, 'Lyophilized', '2026-12-31'
        UNION ALL
SELECT '563CT- Fw', 'LOT-302f0366', 1.0, 'Lyophilized', '2026-12-31'
        UNION ALL
SELECT 'DNase I, RNase free', 'LOT-2284b683', 13.0, '', '2025-06-30'
        UNION ALL
SELECT '10X reaction buffer with MgCl2 for DNase I', 'LOT-4fb8df3c', 13.0, '1.25', '2026-12-31'
        UNION ALL
SELECT 'MnCl2', 'LOT-dd16adb6', 13.0, '1.0', '2026-12-31'
        UNION ALL
SELECT 'reaction buffer with out MnCl2 for DNase I', 'LOT-8a4312ce', 13.0, '1.0', '2026-12-31'
        UNION ALL
SELECT 'EDTA', 'LOT-a0ce4dd8', 13.0, '1.0', '2026-12-31'
        UNION ALL
SELECT 'cell proliferatio kit I (MTT)', 'LOT-a074e9dd', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'cell proliferatio kit I (MTT)', 'LOT-727d113c', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'RQ1R  DNase, stop solution', 'LOT-5d504e46', 1.0, '1.0', '2016-04-30'
        UNION ALL
SELECT 'RQ1 DNase, 10x rxn buffer', 'LOT-ac1b8c7b', 1.0, '1.0', '2016-04-30'
        UNION ALL
SELECT 'RQ1R  RNase freeDNase', 'LOT-45b7c74e', 1.0, '1.0', '2016-04-30'
        UNION ALL
SELECT 'PV25probe', 'LOT-b4a7fa69', 1.0, '20.4', '2026-12-31'
        UNION ALL
SELECT 'PV_DBP_probe', 'LOT-d77e9a41', 1.0, '20.0', '2026-12-31'
        UNION ALL
SELECT 'CCp4 probe', 'LOT-c34d7af7', 1.0, '20.0', '2026-12-31'
        UNION ALL
SELECT 'pfMGET', 'LOT-6d3668ba', 1.0, '20.7', '2026-12-31'
        UNION ALL
SELECT 'Ps18S', 'LOT-8a719509', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'QIAcuity probe PCR kit', 'LOT-d7d26406', 15.0, '1.0', '2024-09-17'
        UNION ALL
SELECT 'Probe PCR Kit 25ml', 'LOT-6b56eded', 1.0, '25.0', '2026-12-31'
        UNION ALL
SELECT 'Probe PCR Kit 50ml', 'LOT-68c0b86f', 10.0, '50.0', '2026-12-31'
        UNION ALL
SELECT 'Probe PCR Kit 15ml', 'LOT-8ac6b1f0', 1.0, '15.0', '2026-12-31'
        UNION ALL
SELECT 'varATS ', 'LOT-450a9b2a', 18.0, 'stock', '2026-12-31'
        UNION ALL
SELECT '2E12F1', 'LOT-dad5eead', 1.0, '15.5', '2026-12-31'
        UNION ALL
SELECT '2E12R1', 'LOT-84cc0c0b', 1.0, '13.2', '2026-12-31'
        UNION ALL
SELECT '2E12F1', 'LOT-c6d390b3', 1.0, '15.5', '2026-12-31'
        UNION ALL
SELECT '2E12R1', 'LOT-ae26aef9', 1.0, '13.2', '2026-12-31'
        UNION ALL
SELECT '2E12F', 'LOT-d650e8b4', 1.0, '20.4', '2026-12-31'
        UNION ALL
SELECT '2E12R', 'LOT-1d822a69', 1.0, '13.6', '2026-12-31'
        UNION ALL
SELECT '2E12F', 'LOT-eb63a073', 1.0, '20.4', '2026-12-31'
        UNION ALL
SELECT '2E12R', 'LOT-a31b2631', 1.0, '13.6', '2026-12-31'
        UNION ALL
SELECT '2E2F', 'LOT-00a8c1d4', 1.0, '14.9', '2026-12-31'
        UNION ALL
SELECT '2E2R1', 'LOT-f4053a08', 1.0, '16.6', '2026-12-31'
        UNION ALL
SELECT '2E2F', 'LOT-f2edc91b', 1.0, '14.9', '2026-12-31'
        UNION ALL
SELECT '2E2R1', 'LOT-ba13f02e', 1.0, '16.6', '2026-12-31'
        UNION ALL
SELECT '2E2F1', 'LOT-da06ba04', 1.0, '12.0', '2026-12-31'
        UNION ALL
SELECT '2E2R', 'LOT-9e80fe8d', 1.0, '13.8', '2026-12-31'
        UNION ALL
SELECT '2E2F1', 'LOT-547b1531', 1.0, '12.0', '2026-12-31'
        UNION ALL
SELECT '2E2R', 'LOT-dce7e5ee', 1.0, '13.8', '2026-12-31'
        UNION ALL
SELECT 'BRAVO_F', 'LOT-9d2be71d', 1.0, '14.6', '2026-12-31'
        UNION ALL
SELECT 'BRAVO_R', 'LOT-2dbaf3d7', 1.0, '17.9', '2026-12-31'
        UNION ALL
SELECT 'BRAVO_F', 'LOT-65e58754', 1.0, '14.6', '2026-12-31'
        UNION ALL
SELECT 'BRAVO_R', 'LOT-52228502', 1.0, '17.9', '2026-12-31'
        UNION ALL
SELECT 'R1(2)', 'LOT-480d2f7c', 1.0, '19.0', '2026-12-31'
        UNION ALL
SELECT 'F1 (M1-F1)', 'LOT-fcee6009', 1.0, '16.2', '2026-12-31'
        UNION ALL
SELECT 'R1(2)', 'LOT-edbecf71', 1.0, '19.0', '2026-12-31'
        UNION ALL
SELECT 'F1 (M1-F1)', 'LOT-07bc367c', 1.0, '16.2', '2026-12-31'
        UNION ALL
SELECT 'R2', 'LOT-d46dcdc0', 1.0, '11.6', '2026-12-31'
        UNION ALL
SELECT 'F2', 'LOT-64add7bd', 1.0, '14.4', '2026-12-31'
        UNION ALL
SELECT 'R2', 'LOT-647e0a27', 1.0, '11.6', '2026-12-31'
        UNION ALL
SELECT 'F2', 'LOT-19859fef', 1.0, '14.4', '2026-12-31'
        UNION ALL
SELECT 'Out PCR barcoding fw', 'LOT-f5cdffa5', 1.0, '15.3', '2026-12-31'
        UNION ALL
SELECT 'Out PCR barcoding rv', 'LOT-5208031d', 1.0, '15.9', '2026-12-31'
        UNION ALL
SELECT 'Out PCR barcoding fw', 'LOT-12257842', 1.0, '15.3', '2026-12-31'
        UNION ALL
SELECT 'Out PCR barcoding rv', 'LOT-98b5c4da', 1.0, '15.9', '2026-12-31'
        UNION ALL
SELECT 'M1-R1', 'LOT-63082f92', 1.0, '16.9', '2026-12-31'
        UNION ALL
SELECT 'M1-IR', 'LOT-589c275e', 1.0, '16.9', '2026-12-31'
        UNION ALL
SELECT 'Tailing Segment FW', 'LOT-37955eaf', 1.0, '14.6', '2026-12-31'
        UNION ALL
SELECT 'Tailing Segment RV', 'LOT-76d24160', 1.0, '18.9', '2026-12-31'
        UNION ALL
SELECT 'Tailing Segment FW', 'LOT-d7c1e94f', 1.0, '14.6', '2026-12-31'
        UNION ALL
SELECT 'Tailing Segment RV', 'LOT-cd8a1c4e', 1.0, '18.9', '2026-12-31'
        UNION ALL
SELECT 'hrp2 one-step FW', 'LOT-499be627', 1.0, '14.6', '2026-12-31'
        UNION ALL
SELECT 'hrp2 one-step RV', 'LOT-353c72f6', 1.0, '20.0', '2026-12-31'
        UNION ALL
SELECT 'hrp2 one-step FW', 'LOT-bc5db4c8', 1.0, '14.6', '2026-12-31'
        UNION ALL
SELECT 'hrp2 one-step RV', 'LOT-a2811e1f', 1.0, '20.0', '2026-12-31'
        UNION ALL
SELECT 'N1FPfcrt FW', 'LOT-5c49f2b8', 1.0, '11.8', '2026-12-31'
        UNION ALL
SELECT 'N1FPfcrt RV', 'LOT-a77ec0a6', 1.0, '7.2', '2026-12-31'
        UNION ALL
SELECT 'N1FPfcrt FW', 'LOT-0adec5a6', 1.0, '11.8', '2026-12-31'
        UNION ALL
SELECT 'N1FPfcrt RV', 'LOT-0eff241c', 1.0, '7.2', '2026-12-31'
        UNION ALL
SELECT 'N2FPfcrt FW', 'LOT-82f404a0', 1.0, '13.9', '2026-12-31'
        UNION ALL
SELECT 'N2FPfcrt RV', 'LOT-f889d5f2', 1.0, '12.4', '2026-12-31'
        UNION ALL
SELECT 'N2FPfcrt FW', 'LOT-cbd8e4df', 1.0, '13.9', '2026-12-31'
        UNION ALL
SELECT 'N2FPfcrt RV', 'LOT-2efefb8d', 1.0, '12.4', '2026-12-31'
        UNION ALL
SELECT 'N1-Mdr1-86Y Fw', 'LOT-95f4c8ad', 1.0, '12.3', '2026-12-31'
        UNION ALL
SELECT 'N1-Mdr1-86Y Rv', 'LOT-eb7a08c8', 1.0, '12.9', '2026-12-31'
        UNION ALL
SELECT 'N1-Mdr1-86Y Fw', 'LOT-43bbcfd6', 1.0, '12.3', '2026-12-31'
        UNION ALL
SELECT 'N1-Mdr1-86Y Rv', 'LOT-363404f4', 1.0, '12.9', '2026-12-31'
        UNION ALL
SELECT 'N2-Mdr1-86Y FW', 'LOT-6d5fdbbb', 1.0, '16.8', '2026-12-31'
        UNION ALL
SELECT 'N2-Mdr1-86Y Rv', 'LOT-f39172f4', 1.0, '17.3', '2026-12-31'
        UNION ALL
SELECT 'N2-Mdr1-86Y FW', 'LOT-00f69bd6', 1.0, '16.8', '2026-12-31'
        UNION ALL
SELECT 'N2-Mdr1-86Y Rv', 'LOT-4aebe2db', 1.0, '17.3', '2026-12-31'
        UNION ALL
SELECT 'N1FPfcrt FW', 'LOT-749c31aa', 1.0, '12.3', '2026-12-31'
        UNION ALL
SELECT 'N1FPfcrt RV', 'LOT-64b9cebb', 1.0, '14.1', '2026-12-31'
        UNION ALL
SELECT 'N2FPfcrt FW', 'LOT-a7b97d93', 1.0, '15.4', '2026-12-31'
        UNION ALL
SELECT 'N2FPfcrt RV', 'LOT-b5b5eb21', 1.0, '14.0', '2026-12-31'
        UNION ALL
SELECT 'N1-Mdr1-86Y Fw', 'LOT-708b5eb5', 1.0, '11.4', '2026-12-31'
        UNION ALL
SELECT 'N1-Mdr1-86Y Rv', 'LOT-0988ab53', 1.0, '12.1', '2026-12-31'
        UNION ALL
SELECT 'N1-Mdr1-86Y Fw', 'LOT-b96a6317', 1.0, '12.4', '2026-12-31'
        UNION ALL
SELECT 'N1-Mdr1-86Y Rv', 'LOT-c3bddcff', 1.0, '12.1', '2026-12-31'
        UNION ALL
SELECT 'N2-mdr1-86Y FW', 'LOT-97d4f57a', 1.0, '8.6', '2026-12-31'
        UNION ALL
SELECT 'N2-mdr1-86Y RV', 'LOT-7eda9d18', 1.0, '10.4', '2026-12-31'
        UNION ALL
SELECT 'N2-mdr1-86Y FW', 'LOT-10c12423', 1.0, '8.6', '2026-12-31'
        UNION ALL
SELECT 'N2-mdr1-86Y RV', 'LOT-95081f8b', 1.0, '10.4', '2026-12-31'
        UNION ALL
SELECT 'M1-OF', 'LOT-3380c2ff', 1.0, '9.6', '2026-12-31'
        UNION ALL
SELECT 'M1-OR', 'LOT-65ee3394', 1.0, '11.1', '2026-12-31'
        UNION ALL
SELECT 'M1-OF', 'LOT-15b7f16b', 1.0, '9.6', '2026-12-31'
        UNION ALL
SELECT 'M1-OR', 'LOT-ef43941b', 1.0, '11.1', '2026-12-31'
        UNION ALL
SELECT 'Pfmdr11246-B', 'LOT-e7a1d48f', 1.0, '26.3', '2026-12-31'
        UNION ALL
SELECT 'Pfmdr11246-A', 'LOT-6a308476', 1.0, '24.7', '2026-12-31'
        UNION ALL
SELECT 'Pfmdr1_RV', 'LOT-3dcaec1b', 1.0, '40.2', '2026-12-31'
        UNION ALL
SELECT 'Pfmdr1_FW', 'LOT-fdab0f7c', 1.0, '50.6', '2026-12-31'
        UNION ALL
SELECT 'Pf--tubulin_FW', 'LOT-05862da4', 1.0, '54.9', '2026-12-31'
        UNION ALL
SELECT 'Pf--tubulin_RV', 'LOT-a56aa499', 1.0, '45.3', '2026-12-31'
        UNION ALL
SELECT 'Pfplasmepsin2__FW', 'LOT-2936dd5c', 1.0, '63.4', '2026-12-31'
        UNION ALL
SELECT 'Pfplasmepsin2__RV', 'LOT-5e74c8ed', 1.0, '55.3', '2026-12-31'
        UNION ALL
SELECT 'Pfmdr1246-D1', 'LOT-36bdbe84', 1.0, '22.2', '2026-12-31'
        UNION ALL
SELECT 'Pfmdr1246-D2', 'LOT-3845f254', 1.0, '25.6', '2026-12-31'
        UNION ALL
SELECT 'Pfmdr1246-A', 'LOT-11a2f0bd', 1.0, '24.7', '2026-12-31'
        UNION ALL
SELECT 'Pfmdr1246-B', 'LOT-9acfe144', 1.0, '26.3', '2026-12-31'
        UNION ALL
SELECT 'Pfmdr1246-D1', 'LOT-5bca924d', 1.0, '22.2', '2026-12-31'
        UNION ALL
SELECT 'Pfmdr1246-D2', 'LOT-f85ba222', 1.0, '25.6', '2026-12-31'
        UNION ALL
SELECT 'RV11', 'LOT-c3ed9b11', 1.0, '52.7', '2026-12-31'
        UNION ALL
SELECT 'RV12', 'LOT-866b756a', 1.0, '53.9', '2026-12-31'
        UNION ALL
SELECT '8633F', 'LOT-93885245', 1.0, '55.9', '2026-12-31'
        UNION ALL
SELECT '9211R', 'LOT-c47e4995', 1.0, '62.5', '2026-12-31'
        UNION ALL
SELECT '8945F', 'LOT-53b8d5fe', 1.0, '69.7', '2026-12-31'
        UNION ALL
SELECT '9577R', 'LOT-c6c43296', 1.0, '59.5', '2026-12-31'
        UNION ALL
SELECT '8669F', 'LOT-4a820050', 1.0, '65.0', '2026-12-31'
        UNION ALL
SELECT '9541R', 'LOT-089aa1f8', 1.0, '59.4', '2026-12-31'
        UNION ALL
SELECT 'RV12-2', 'LOT-24e149f8', 1.0, '63.9', '2026-12-31'
        UNION ALL
SELECT 'Pv210-Pc', 'LOT-3fa173e6', 3.0, '9.1', '2026-12-31'
        UNION ALL
SELECT 'pf-pc2', 'LOT-abaae358', 1.0, '0.25', '2026-12-31'
        UNION ALL
SELECT 'pv-210-6', 'LOT-6eb4f04b', 1.0, '0.5', '2026-12-31'
        UNION ALL
SELECT 'pf-pc5', 'LOT-eac04774', 1.0, '0.25', '2026-12-31'
        UNION ALL
SELECT 'pf-pc1', 'LOT-90dadac4', 1.0, '0.25', '2026-12-31'
        UNION ALL
SELECT 'pv-210-5', 'LOT-a1539cce', 1.0, '0.3', '2026-12-31'
        UNION ALL
SELECT 'pf-pc4', 'LOT-e244286f', 1.0, '0.3', '2026-12-31'
        UNION ALL
SELECT 'pv-210-2', 'LOT-b70a6207', 1.0, '0.25', '2026-12-31'
        UNION ALL
SELECT 'pv-210-1', 'LOT-203b844a', 1.0, '0.25', '2026-12-31'
        UNION ALL
SELECT 'pf-pc6', 'LOT-78e8c6fb', 1.0, '0.5', '2026-12-31'
        UNION ALL
SELECT 'pv-210 pc ELISA(stock)', 'LOT-2f6369a4', 1.0, '0.25', '2026-12-31'
        UNION ALL
SELECT 'pv-247 pc ELISA(stock)', 'LOT-49fc2734', 1.0, '0.25', '2026-12-31'
        UNION ALL
SELECT 'Pf-PC', 'LOT-339e84b2', 2.0, '0.1', '2026-12-31'
        UNION ALL
SELECT 'PV-210-3', 'LOT-ad5b3f44', 1.0, '0.25', '2026-12-31'
        UNION ALL
SELECT 'Pf-PC3', 'LOT-fd3e8b24', 1.0, '0.25', '2026-12-31'
        UNION ALL
SELECT 'pf-210-4', 'LOT-1c59fe31', 1.0, '0.25', '2026-12-31'
        UNION ALL
SELECT 'New Naive-serum', 'LOT-48bbaf39', 1.0, '1.5', '2026-12-31'
        UNION ALL
SELECT 'conjugate m Ab pf', 'LOT-6a5788d9', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'pf-pc csp', 'LOT-cbced18b', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'pv hrp swp', 'LOT-d558b3e6', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'pv-210-pc ELISA(stock)', 'LOT-94286dae', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'pv247-pc', 'LOT-771cf5f5', 1.0, '4.6', '2026-12-31'
        UNION ALL
SELECT 'pv capture AB', 'LOT-5684ff58', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'pf capture AB', 'LOT-33ec7a63', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'pv-210-1 m ab', 'LOT-2c0765a9', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'pv-210 2nd mab', 'LOT-b91542ad', 2.0, '', '2026-12-31'
        UNION ALL
SELECT 'pv-219 pc ELISA', 'LOT-55f54479', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'CCP4-RV', 'LOT-0d324c3d', 1.0, '500.0', '2026-12-31'
        UNION ALL
SELECT 'BCFB FY2021', 'LOT-3c309b7e', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'PfMGET-FW', 'LOT-b8ba6aaa', 1.0, '1000.0', '2026-12-31'
        UNION ALL
SELECT 'PfMGET-RV', 'LOT-3021795b', 1.0, '500.0', '2026-12-31'
        UNION ALL
SELECT 'rPLU-5', 'LOT-1fb476b1', 1.0, '1000.0', '2026-12-31'
        UNION ALL
SELECT 'Pv18s-RV', 'LOT-86f34dd4', 1.0, '500.0', '2026-12-31'
        UNION ALL
SELECT 'rVIV-1', 'LOT-9e91273b', 1.0, '300.0', '2026-12-31'
        UNION ALL
SELECT 'Pv525-RV', 'LOT-8924f147', 1.0, '500.0', '2026-12-31'
        UNION ALL
SELECT 'rPLU-6', 'LOT-31c6a113', 1.0, '1000.0', '2026-12-31'
        UNION ALL
SELECT 'PV525-FW', 'LOT-68951fc8', 1.0, '500.0', '2026-12-31'
        UNION ALL
SELECT 'Pf 18s-FW', 'LOT-ffd622f1', 1.0, '300.0', '2026-12-31'
        UNION ALL
SELECT 'rFAL-2', 'LOT-c51fd952', 1.0, '300.0', '2026-12-31'
        UNION ALL
SELECT 'PV18S probe', 'LOT-8af3d0a9', 1.0, '300.0', '2026-12-31'
        UNION ALL
SELECT 'rVIV-2', 'LOT-4f8aae08', 1.0, '300.0', '2026-12-31'
        UNION ALL
SELECT 'rFAL-1', 'LOT-ea23a327', 1.0, '300.0', '2026-12-31'
        UNION ALL
SELECT 'PfMEGT probe', 'LOT-7d599fc5', 1.0, '7443.9', '2026-12-31'
        UNION ALL
SELECT 'Pf 18s turbo probe', 'LOT-5ebf1f81', 1.0, '900.0', '2026-12-31'
        UNION ALL
SELECT 'CCP4 probe', 'LOT-4dbd38ba', 1.0, '900.0', '2026-12-31'
        UNION ALL
SELECT 'PF18S RV', 'LOT-38826290', 1.0, '500.0', '2026-12-31'
        UNION ALL
SELECT 'PVS25 Probe', 'LOT-115c0ead', 1.0, '600.0', '2026-12-31'
        UNION ALL
SELECT 'PV18S Probe', 'LOT-b21d51b7', 1.0, '300.0', '2026-12-31'
        UNION ALL
SELECT 'E-COLI', 'LOT-fb117291', 5.0, '~1', '2026-12-31'
        UNION ALL
SELECT 'New N-serum', 'LOT-3c7b8792', 6.0, '~2', '2026-12-31'
        UNION ALL
SELECT 'E-COLI Lysate', 'LOT-c36e5f3f', 4.0, '8.64', '2026-12-31'
        UNION ALL
SELECT 'WHO PF standard(+ve control)', 'LOT-bc1293be', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'PF +VE control(cp3 NEAT)', 'LOT-77123985', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'cp3(1:1)(50%)', 'LOT-86b332dd', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Et-pf pooled serum', 'LOT-d79302c3', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'E-COLI Lysate(5.2mg/ml)', 'LOT-812711f1', 1.0, '1.6', '2026-12-31'
        UNION ALL
SELECT 'CP3 1:10', 'LOT-42c4a524', 1.0, '0.5', '2026-12-31'
        UNION ALL
SELECT 'S1PV +VE control', 'LOT-74398a76', 1.0, '~2,5', '2026-12-31'
        UNION ALL
SELECT 'Hot start Taq 2x Master Mix 500rxn', 'LOT-72c2af65', 1.0, '', '2024-08-01'
        UNION ALL
SELECT 'Dream Taq Master Mix 2x', 'LOT-ec25f148', 5.0, '', '2026-12-31'
        UNION ALL
SELECT 'PCR buffer+green+white+mgcl', 'LOT-48796ab6', 1.0, '2ml', '2019-09-14'
        UNION ALL
SELECT 'PCR buffer+green+white+mgcl', 'LOT-0673b372', 1.0, '2ml', '2021-10-15'
        UNION ALL
SELECT 'PCR buffer+green+white+mgcl', 'LOT-48ee8004', 1.0, '2ml', '2026-12-31'
        UNION ALL
SELECT 'DNA ladder 100bp with buffer', 'LOT-545a7528', 2.0, '', '2026-12-31'
        UNION ALL
SELECT 'Invitrogen DNA ladder 50bp with buffer', 'LOT-5be76bfa', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'GoTaq nPCR componenet ', 'LOT-ddbfd91c', 40.0, '', '2024-11-04'
        UNION ALL
SELECT 'GoTaq nPCR componenet ', 'LOT-b3ebe56b', 20.0, '', '2026-12-31'
        UNION ALL
SELECT 'GoTaq nPCR componenet ', 'LOT-697cef22', 40.0, '', '2024-04-11'
        UNION ALL
SELECT 'GoTaq nPCR componenet ', 'LOT-ae7c2471', 3.0, '', '2021-10-15'
        UNION ALL
SELECT 'GoTaq nPCR componenet ', 'LOT-c2df4c7c', 18.0, '', '2022-12-11'
        UNION ALL
SELECT 'GoTaq 100nM dNTPs set', 'LOT-4e4dd721', 11.0, '', '2024-01-08'
        UNION ALL
SELECT 'Bio-dNTP Mix ', 'LOT-87b99774', 1.0, '', '2023-02-01'
        UNION ALL
SELECT 'MBL-dNTP Mix ', 'LOT-e5c0a28a', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Thermoscintific- dNTP Mix ', 'LOT-12540b4d', 1.0, '', '2024-05-01'
        UNION ALL
SELECT 'GoTaq dNTP set', 'LOT-35012206', 1.0, '', '2026-12-31'
        UNION ALL
SELECT '100bp DNA ladder ', 'LOT-7a7cb33e', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'PCR Nucleotide Mix', 'LOT-338cbe3a', 1.0, '', '2024-04-01'
        UNION ALL
SELECT 'rplu5-100mM', 'LOT-f06d1557', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'rplu5-100mM', 'LOT-9a28da5a', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'rplu6-100mM', 'LOT-437bf3ee', 4.0, '', '2026-12-31'
        UNION ALL
SELECT 'rplu6-100mM', 'LOT-561d448b', 2.0, '', '2026-12-31'
        UNION ALL
SELECT 'rrplu1-100mM', 'LOT-36dad796', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'rplu3-100mM', 'LOT-65916209', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'rplu3-100mM', 'LOT-96156203', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'rplu4-100mM', 'LOT-f3f20a51', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'rviv1-100mM', 'LOT-a3667afd', 4.0, '', '2026-12-31'
        UNION ALL
SELECT 'rviv1-100mM', 'LOT-e735c5ef', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'rviv2-100mM', 'LOT-096b9feb', 4.0, '', '2026-12-31'
        UNION ALL
SELECT 'rviv2-100mM', 'LOT-37d6db4c', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'rfal2-100mM', 'LOT-091dfd5d', 4.0, '', '2026-12-31'
        UNION ALL
SELECT 'rfal2-100mM', 'LOT-a582e582', 2.0, '', '2026-12-31'
        UNION ALL
SELECT 'rfal1-100mM', 'LOT-b6dcd1a4', 4.0, '', '2026-12-31'
        UNION ALL
SELECT 'rfal1-100mM', 'LOT-96f3cce0', 2.0, '', '2026-12-31'
        UNION ALL
SELECT 'rmal1-100mM', 'LOT-1c1c253a', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'rmal2-100mM', 'LOT-1b79a556', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'rova1-100mM', 'LOT-5150db0a', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'rova2-100mM', 'LOT-27ba6171', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pf - Rv (rFAL2)', 'LOT-8ee087bb', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pf-fw (rFAL1)', 'LOT-82c44a83', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pv Rv (rVIV 2)', 'LOT-574ebfb0', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pv Fw (rVIV 1)', 'LOT-44fdf26a', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'rPLU 6 forward', 'LOT-00ad152c', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'rPLU 5 reverse', 'LOT-dcf0197b', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'dCTP-100mM', 'LOT-c6d7b1d3', 2.0, '1.0', '2023-04-09'
        UNION ALL
SELECT 'dTTP-100mM', 'LOT-48b8a553', 2.0, '1.0', '2023-04-09'
        UNION ALL
SELECT 'dATP-100mM', 'LOT-b43a4ad2', 2.0, '1.0', '2023-04-09'
        UNION ALL
SELECT 'dGTP-100mM', 'LOT-2bc9a770', 2.0, '1.0', '2023-04-13'
        UNION ALL
SELECT 'dTTP-100mM', 'LOT-ff54b44c', 4.0, '0.4&1', '2022-02-17'
        UNION ALL
SELECT 'dGTP-100mM', 'LOT-beb14f64', 4.0, '0.4&1', '2022-04-28'
        UNION ALL
SELECT 'dATP-100mM', 'LOT-3a61171b', 4.0, '0.4&1', '2022-05-27'
        UNION ALL
SELECT 'dCTP-100mM', 'LOT-a42bccaf', 4.0, '0.4&1', '2022-02-17'
        UNION ALL
SELECT 'dATP-100mM', 'LOT-fd9347db', 11.0, '0.4&1', '2024-09-19'
        UNION ALL
SELECT 'dGTP-100mM', 'LOT-11da95e7', 11.0, '0.4&1', '2024-09-26'
        UNION ALL
SELECT 'dCTP-100mM', 'LOT-2f863cb5', 11.0, '0.4&1', '2024-09-19'
        UNION ALL
SELECT 'dTTP-100mM', 'LOT-7a3c632a', 11.0, '0.4&1', '2024-08-01'
        UNION ALL
SELECT 'dNTP Mix -2.5mM', 'LOT-e31c0386', 13.0, '1.0', '2026-12-31'
        UNION ALL
SELECT 'dATP-100mM', 'LOT-e56c96c0', 2.0, '1.0', '2022-05-27'
        UNION ALL
SELECT 'dCTP-100mM', 'LOT-eeca7b19', 2.0, '1.0', '2022-02-17'
        UNION ALL
SELECT 'dTTP-100mM', 'LOT-06ec9e66', 2.0, '1.0', '2022-02-17'
        UNION ALL
SELECT 'dGTP-100mM', 'LOT-c7502a16', 2.0, '1.0', '2022-04-28'
        UNION ALL
SELECT 'dNTP MIX -10mM', 'LOT-b149eae2', 1.0, '1.0', '2022-05-12'
        UNION ALL
SELECT 'dATP-100mM   100umol', 'LOT-faf35153', 1.0, '1.0', '2018-06-01'
        UNION ALL
SELECT 'dCTP-100mM  100umol', 'LOT-225c104b', 1.0, '1.0', '2018-06-01'
        UNION ALL
SELECT 'dGTP-100mM  100umol', 'LOT-d2d96358', 2.0, '1.0', '2018-06-01'
        UNION ALL
SELECT 'dTTP-100mM  100umol', 'LOT-1da9f324', 1.0, '1.0', '2018-06-01'
        UNION ALL
SELECT 'dNTP MIX with dUTP-12.5mM', 'LOT-456205c5', 1.0, '1.0', '2026-12-31'
        UNION ALL
SELECT 'dNTP MIX with dTTP-10mM', 'LOT-3ef65fa2', 1.0, '1.0', '2026-12-31'
        UNION ALL
SELECT 'dNTP mix 10mM', 'LOT-ea53d7ef', 10.0, '1.0', '2023-02-01'
        UNION ALL
SELECT 'AF1III ', 'LOT-c69416f6', 1.0, '0.025', '2024-01-01'
        UNION ALL
SELECT 'AF1III ', 'LOT-8b00fe63', 1.0, '0.025', '2024-01-01'
        UNION ALL
SELECT 'APoI', 'LOT-c55aae02', 1.0, '0.1', '2018-07-01'
        UNION ALL
SELECT 'N1FPfcrt-100mM', 'LOT-f7ba3fe6', 2.0, '', '2026-12-31'
        UNION ALL
SELECT 'N2FPfcrt-100mM', 'LOT-db103c67', 2.0, '', '2026-12-31'
        UNION ALL
SELECT 'N1FPfcrt RW-100mM', 'LOT-19333839', 1.0, '', '2022-12-01'
        UNION ALL
SELECT 'N1FPfcrt FW-100mM', 'LOT-eab10baa', 1.0, '', '2022-12-01'
        UNION ALL
SELECT 'N2FPfcrt RW-100mM', 'LOT-66ccbefd', 1.0, '', '2022-12-01'
        UNION ALL
SELECT 'N2FPfcrt FW-100mM', 'LOT-3a065d27', 1.0, '', '2022-12-01'
        UNION ALL
SELECT 'green Go Taq buffer', 'LOT-d3f52cb2', 15.0, '1.0', '2026-12-31'
        UNION ALL
SELECT 'MgCl2 -25mM', 'LOT-14080fb1', 16.0, '1.2', '2026-12-31'
        UNION ALL
SELECT 'colorless Go Taq buffer', 'LOT-b4211bf8', 25.0, '1.0', '2026-12-31'
        UNION ALL
SELECT 'colorless Go Taq buffer', 'LOT-6235421d', 68.0, '1.0', '2022-11-22'
        UNION ALL
SELECT 'colorless Go Taq buffer', 'LOT-cdabece6', 33.0, '1.0', '2026-12-31'
        UNION ALL
SELECT 'MgCl2 -25mM', 'LOT-68dd199a', 17.0, '1.2', '2026-12-31'
        UNION ALL
SELECT 'green Go Taq buffer', 'LOT-612cd2ad', 23.0, '1.0', '2026-12-31'
        UNION ALL
SELECT 'green Go Taq buffer', 'LOT-2d50358a', 69.0, '1.0', '2022-11-12'
        UNION ALL
SELECT 'MgCl2 -25mM', 'LOT-10b11342', 54.0, '1.2', '2022-11-12'
        UNION ALL
SELECT 'green Go Taq buffer', 'LOT-729470dd', 59.0, '1.0', '2024-04-11'
        UNION ALL
SELECT 'green Go Taq buffer', 'LOT-f12fe7d6', 56.0, '1.0', '2024-04-11'
        UNION ALL
SELECT 'green Go Taq buffer', 'LOT-89039534', 44.0, '1.0', '2024-04-11'
        UNION ALL
SELECT 'colorless Go Taq buffer', 'LOT-2e03cf7d', 40.0, '1.0', '2024-04-11'
        UNION ALL
SELECT 'colorless Go Taq buffer', 'LOT-310462ee', 53.0, '1.0', '2024-04-11'
        UNION ALL
SELECT 'colorless Go Taq buffer', 'LOT-625505aa', 58.0, '1.0', '2024-04-11'
        UNION ALL
SELECT 'MgCl2 -25mM', 'LOT-0eb065f7', 43.0, '1.2', '2024-04-11'
        UNION ALL
SELECT 'MgCl2 -25mM', 'LOT-dd34c39f', 48.0, '1.2', '2024-04-11'
        UNION ALL
SELECT 'MgCl2 -25mM', 'LOT-bf04a32a', 27.0, '1.2', '2024-04-11'
        UNION ALL
SELECT 'GO Taq G2 flexi DNA polymerases 5 u/ul', 'LOT-1dd6cd30', 40.0, '500.0', '2024-12-12'
        UNION ALL
SELECT 'HOT Start Taq 2X master mix (500 rxn / vial )', 'LOT-f9bf0fa1', 2.0, '50.0', '2024-08-31'
        UNION ALL
SELECT 'dNTP set 4x25 umol', 'LOT-0d2b093b', 4.0, '1.0', '2024-05-31'
        UNION ALL
SELECT 'PCR nucleotide mix 10mM', 'LOT-94fbc261', 1.0, '200.0', '2024-01-04'
        UNION ALL
SELECT 'flexi DNA polymerase', 'LOT-e35e9fbe', 12.0, '500.0', '2020-09-18'
        UNION ALL
SELECT 'GO Taq G2 flexi DNA polymerases 5u/ul', 'LOT-d54cb7cb', 18.0, '500.0', '2022-11-12'
        UNION ALL
SELECT 'Go Taq flexi DNA polymerase', 'LOT-c55f66e5', 3.0, '500.0', '2021-10-15'
        UNION ALL
SELECT 'MgCl2 -25mM', 'LOT-413a39ea', 15.0, '1.2', '2021-10-15'
        UNION ALL
SELECT 'green Go Taq buffer', 'LOT-3323e93f', 20.0, '1.0', '2021-10-15'
        UNION ALL
SELECT 'colorless Go Taq buffer', 'LOT-137db57b', 20.0, '1.0', '2021-10-15'
        UNION ALL
SELECT 'NPCR components (Go Taq)', 'LOT-65cefa1f', 13.0, '', '2020-09-18'
        UNION ALL
SELECT 'Flexi Go Taq DNA polymerase', 'LOT-dd7e7990', 1.0, '', '2024-04-11'
        UNION ALL
SELECT 'F-Rv+Fw', 'LOT-93abddfa', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'O-Rv+Fw', 'LOT-2b2eac51', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'M-Rv+Fw', 'LOT-e9533b2a', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'V-Rv+Fw', 'LOT-d313e1bb', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'K-Rv+Fw', 'LOT-d802907d', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'P-Rv+Fw', 'LOT-c6cba98f', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'rCutSmart Buffer', 'LOT-6f4ba19b', 1.0, '1.25', '2025-04-01'
        UNION ALL
SELECT 'rCutSmart Buffer', 'LOT-a90f0c73', 1.0, '1.25', '2025-02-01'
        UNION ALL
SELECT 'rCutSmart Buffer', 'LOT-210ba55b', 1.0, '1.25', '2024-07-01'
        UNION ALL
SELECT 'rCutSmart Buffer', 'LOT-decbff31', 1.0, '1.25', '2025-04-01'
        UNION ALL
SELECT 'gel loading dye purple', 'LOT-4d279077', 1.0, '0.5', '2024-12-01'
        UNION ALL
SELECT 'gel loading dye purple', 'LOT-2b0a1e8d', 1.0, '0.5', '2024-09-01'
        UNION ALL
SELECT 'gel loading dye purple', 'LOT-fb570f41', 1.0, '0.5', '2024-03-01'
        UNION ALL
SELECT 'gel loading dye purple', 'LOT-963fb592', 1.0, '0.5', '2024-05-01'
        UNION ALL
SELECT 'NEBuffer', 'LOT-5300f3a7', 1.0, '1.25', '2024-12-01'
        UNION ALL
SELECT 'NEBuffer', 'LOT-6fe99759', 2.0, '1.25', '2024-10-01'
        UNION ALL
SELECT 'HhaI', 'LOT-010e5892', 1.0, '0.1', '2024-02-01'
        UNION ALL
SELECT 'EcoRI-HF', 'LOT-0b2bedfe', 2.0, '1.25', '2024-03-01'
        UNION ALL
SELECT 'BgI-II', 'LOT-2ab30d8d', 1.0, '0.2', '2023-08-01'
        UNION ALL
SELECT 'Dde-I', 'LOT-f18f3919', 1.0, '0.1', '2023-07-01'
        UNION ALL
SELECT 'Pst-I', 'LOT-bd50a666', 1.0, '0.5', '2023-11-01'
        UNION ALL
SELECT 'BIO Taq DNA polymerase', 'LOT-df4adb46', 20.0, '100.0', '2023-12-30'
        UNION ALL
SELECT 'NH4 reaction buffer, MgCl2 free', 'LOT-13d9bfc8', 40.0, '1.2', '2023-12-30'
        UNION ALL
SELECT 'MgCl2 ', 'LOT-1e1adf7a', 20.0, '1.2', '2023-12-30'
        UNION ALL
SELECT 'RQ1 RNase-Free DNase', 'LOT-58b7fb4e', 1.0, '', '2016-04-30'
        UNION ALL
SELECT 'DNase I, RNase-free', 'LOT-1647319d', 15.0, '', '2025-06-01'
        UNION ALL
SELECT 'RNase inhibitor', 'LOT-8712be04', 5.0, '', '2019-07-01'
        UNION ALL
SELECT 'RNase inhibitor', 'LOT-af0955cf', 1.0, '', '2017-08-01'
        UNION ALL
SELECT 'High capacity cDNA Reverse T kit', 'LOT-7feaba56', 1.0, '', '2015-06-23'
        UNION ALL
SELECT 'One step RT-qPCR kit 2500rxn', 'LOT-eec6dcf1', 1.0, '', '2022-04-01'
        UNION ALL
SELECT 'One step RT-qPCR kit 500rxn', 'LOT-456e779b', 1.0, '', '2019-08-01'
        UNION ALL
SELECT 'One step RT-SuperMix kit 25rxn', 'LOT-dcfc4ec0', 1.0, '', '2020-02-01'
        UNION ALL
SELECT 'Luna warm start RT enzyme mix, 20x conc', 'LOT-a8e27c6f', 1.0, 'used', '2019-08-31'
        UNION ALL
SELECT 'Luna universal probe 1 step reaction mix 2x conc', 'LOT-88731a4a', 1.0, '1.0', '2020-01-30'
        UNION ALL
SELECT 'GO Taq PCR master mix 2X c0nc', 'LOT-2b7e6ece', 2.0, 'used', '2020-05-28'
        UNION ALL
SELECT 'MQ', 'LOT-2f306619', 1.0, '1.5', '2020-11-30'
        UNION ALL
SELECT 'Luna One step RT-qPCR kit 2,500 rxn', 'LOT-4fab6e10', 2.0, '', '2021-04-30'
        UNION ALL
SELECT 'Luna One step RT-qPCR kit 2,500 rxn', 'LOT-780a092c', 2.0, '', '2022-04-30'
        UNION ALL
SELECT 'Luna One step RT-qPCR kit 2,500 rxn', 'LOT-6c8d41a3', 6.0, '', '2024-01-30'
        UNION ALL
SELECT 'Luna One step RT-qPCR kit 2,500 rxn', 'LOT-35bb32b8', 6.0, '', '2024-01-30'
        UNION ALL
SELECT 'High capacity cDNA rt kit', '4.0', 1.0, '1.0', '2023-01-31'
        UNION ALL
SELECT 'UD2-R_2_P.PIGNATELL', 'LOT-24d8f314', 1.0, '', '2022-08-09'
        UNION ALL
SELECT 'St-F_2_P.PIGNATELL', 'LOT-30867103', 1.0, '', '2022-08-09'
        UNION ALL
SELECT 'U5.8S-F_2_P.PIGNATELL', 'LOT-19ed5b23', 1.0, '', '2022-08-09'
        UNION ALL
SELECT 'ITS2-steph-R_P.PIGNATELL', 'LOT-8234bc2f', 1.0, '', '2021-07-26'
        UNION ALL
SELECT 'ITS2A_P.PIGNATELL', 'LOT-010121c9', 1.0, '', '2021-07-26'
        UNION ALL
SELECT 'ITS2B_mod2_P.PIGNATELL', 'LOT-159c35b4', 1.0, '', '2021-07-26'
        UNION ALL
SELECT 'OVM_115402_C8', 'LOT-7a041ccf', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'OVM_115394_C4', 'LOT-6e336f4d', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pfr364af_fw', 'LOT-0a4f208d', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pfr364af_rb', 'LOT-55595d9d', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pvdhfrv_fw', 'LOT-a401d380', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pvdhfrv_rv', 'LOT-5f49c195', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Hrp2 exon 2 fwd', 'LOT-3fc9ede8', 1.0, '', '2022-01-31'
        UNION ALL
SELECT 'Hrp2 exon 2 rev', 'LOT-c10068cc', 1.0, '', '2022-01-31'
        UNION ALL
SELECT 'Hrp3 rev ', 'LOT-dbb5f3ca', 1.0, '', '2022-01-31'
        UNION ALL
SELECT 'Hrp3 fwd', 'LOT-ca945d4d', 1.0, '', '2022-01-31'
        UNION ALL
SELECT 'Trna rev', 'LOT-99ce9e4a', 1.0, '', '2022-01-31'
        UNION ALL
SELECT 'Trna fwd', 'LOT-9a962cb2', 1.0, '', '2022-01-31'
        UNION ALL
SELECT 'Hrp2 exon 2 probe', 'LOT-a00dede4', 1.0, '', '2022-01-29'
        UNION ALL
SELECT 'Hrp2 probe', 'LOT-bed7aa3c', 1.0, '', '2022-02-17'
        UNION ALL
SELECT 'Hrp3 probe', 'LOT-1c4964a0', 1.0, '', '2022-02-17'
        UNION ALL
SELECT 'Trna probe', 'LOT-e8425171', 1.0, '', '2022-02-02'
        UNION ALL
SELECT 'Pvmsp2_n_rev', 'LOT-63befbf3', 1.0, '', '2022-02-05'
        UNION ALL
SELECT 'Pvmsp2_p_fwd', 'LOT-74642d5b', 1.0, '', '2022-02-05'
        UNION ALL
SELECT 'Pvmsp2_n_fwd', 'LOT-fcd63e9e', 1.0, '', '2022-01-28'
        UNION ALL
SELECT 'Pvmsp2_p_rev', 'LOT-5684b736', 1.0, '', '2022-02-05'
        UNION ALL
SELECT 'Pvmsp1f3_p_rev', 'LOT-f908497d', 1.0, '', '2022-02-09'
        UNION ALL
SELECT 'Pvmsp1f3_n_fwd', 'LOT-69464a81', 1.0, '', '2022-01-28'
        UNION ALL
SELECT 'Pvmsp1f3_p_fwd', 'LOT-f7b5d329', 1.0, '', '2022-02-09'
        UNION ALL
SELECT 'Pvmsp1f3_n_rev', 'LOT-4fe1cf32', 1.0, '', '2022-02-05'
        UNION ALL
SELECT 'Msp2_s1 tail_fwd', 'LOT-91b57e2c', 1.0, '', '2022-02-05'
        UNION ALL
SELECT 'Msp2_fc27_m5_rev', 'LOT-cb1d0c56', 1.0, '', '2022-01-28'
        UNION ALL
SELECT 'Msp2_3d7_n5_rev', 'LOT-c2ddab1d', 1.0, '', '2022-01-28'
        UNION ALL
SELECT 'Pfmsp2_s2_fw', 'LOT-4286d37b', 1.0, '', '2022-02-05'
        UNION ALL
SELECT 'Pfmsp2_s3_rv', 'LOT-97d60c53', 1.0, '', '2022-02-05'
        UNION ALL
SELECT 'ed protein (hyp8) rv', 'LOT-5823215e', 1.0, '', '2026-12-31'
        UNION ALL
SELECT '563ct_rv', 'LOT-8fe2dbaf', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'rPLU3', 'LOT-61e4a5fb', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'rPLU4', 'LOT-8b80f46d', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'TARE_2_fw', 'LOT-0d4ab740', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'TARE_2_rv', 'LOT-1f606164', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'RESA rv', 'LOT-e0649a98', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'RESA fw', 'LOT-5018e820', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pfs25_fw', 'LOT-c4c37245', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'rPLU1', 'LOT-d1edb8c9', 1.0, '', '2026-12-31'
        UNION ALL
SELECT '376AG fw', 'LOT-327bd384', 1.0, '', '2026-12-31'
        UNION ALL
SELECT '376AG rv', 'LOT-3f2fe136', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pv18s_ fw', 'LOT-9f91359c', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pv18s_ rv', 'LOT-00d3f8be', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pmal_qPCR_ fw', 'LOT-07bd9df7', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pmal_qPCR_ rv', 'LOT-7a5e266e', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pova_qPCR_fw', 'LOT-56fa8934', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pova_qPCR_rv', 'LOT-ea1a9494', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'PgMET_fw', 'LOT-13e9cbba', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'PgMET_rw', 'LOT-0f0b625a', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pvs25 fw', 'LOT-ee8d1109', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pvs25 rev', 'LOT-cb2309b3', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'QMAL fw', 'LOT-48d34331', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'QMAL rv', 'LOT-fa7bd7ca', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'QMAL fw', 'LOT-e5875c6c', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'QMAL rv', 'LOT-682f96ae', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pfs230p_fw', 'LOT-c1a75a1b', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pfs230p_rv', 'LOT-36dde7a6', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'varATS_fw', 'LOT-2be1e890', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'varATS_rv', 'LOT-139e8eb7', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'varATS_fw', 'LOT-9844b045', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'varATS_rv', 'LOT-5abf2cb6', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pf18S RV 100uM', 'LOT-a2eaa020', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'PV18S FW MPX 100uM', 'LOT-ffe2b4ee', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'Pf18S probe 100uM', 'LOT-5c7b06f7', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'Pv18S MPX 100uM', 'LOT-388803ef', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'PV18S RV 100uM  ', 'LOT-c9af7bc5', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'Pf18s FW 100uM', 'LOT-bd082e19', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'PV25 probe 5.3 nMol', 'LOT-0c0bcbc2', 1.0, 'lyophilized ', '2026-12-31'
        UNION ALL
SELECT 'PfMDR1 probe 5.1 nMol', 'LOT-43835f6e', 1.0, 'lyophilized ', '2026-12-31'
        UNION ALL
SELECT 'Beta TT probe 5.2 nMol', 'LOT-988b072a', 1.0, 'lyophilized ', '2026-12-31'
        UNION ALL
SELECT 'Pf plasmepsin2 probe 5.3 nMol', 'LOT-5651b2dd', 1.0, 'lyophilized ', '2026-12-31'
        UNION ALL
SELECT 'Viv f(shako) ', 'LOT-c8ced578', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'Fal-F(shako)', 'LOT-c817d525', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'Pf18S shoko probe 5.3 nMOL', 'LOT-4d3d2875', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'Pv18s SHOKO probe ', 'LOT-4bc4ad08', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'Plasmo rev 100UM Shoko (for Pv- Pf mpx)', 'LOT-15aafdbb', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'Pv 18S REV', 'LOT-caf18a44', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'Pv-25 Fw', 'LOT-8d8d465f', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pv-25 Rev', 'LOT-2f5f96f7', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pv-25 Rv', 'LOT-8e0bf933', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pvs25 Fw', 'LOT-a6c9a851', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pv-18s new short', 'LOT-6a536836', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pf-18s- DNA-Rv', 'LOT-dab3fcb9', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pf-18s- DNA-Fw', 'LOT-240a481a', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'StD2', 'LOT-969430b2', 1.0, '300ul', '2026-12-31'
        UNION ALL
SELECT 'GATA-1F', 'LOT-db66463b', 1.0, '1000ul', '2026-12-31'
        UNION ALL
SELECT 'GATA-1R', 'LOT-6fcba331', 1.0, '1000ul', '2026-12-31'
        UNION ALL
SELECT 'hrp2-Exon-FW', 'LOT-098333cc', 1.0, '600ul', '2026-12-31'
        UNION ALL
SELECT 'tRNA-FW', 'LOT-6bed00aa', 1.0, '600ul', '2026-12-31'
        UNION ALL
SELECT 'hrp3-FW', 'LOT-be629032', 1.0, '600ul', '2026-12-31'
        UNION ALL
SELECT 'uq-R', 'LOT-7eb9a51b', 1.0, '600ul', '2026-12-31'
        UNION ALL
SELECT 'UD2-R', 'LOT-28ee10e4', 1.0, '600ul', '2026-12-31'
        UNION ALL
SELECT 'St-F', 'LOT-d374bb28', 1.0, '600ul', '2026-12-31'
        UNION ALL
SELECT 'StD2-R', 'LOT-be8c928f', 1.0, '600ul', '2026-12-31'
        UNION ALL
SELECT 'us-85-F', 'LOT-1c5b0717', 1.0, '600ul', '2026-12-31'
        UNION ALL
SELECT 'st-F', 'LOT-66282412', 1.0, '600ul', '2026-12-31'
        UNION ALL
SELECT 'UD2-R', 'LOT-f40e7af0', 1.0, '600ul', '2026-12-31'
        UNION ALL
SELECT 'tRNA-Rv', 'LOT-40eeee37', 1.0, '600ul', '2026-12-31'
        UNION ALL
SELECT 'hrp2-Exon2-RV', 'LOT-017725b7', 1.0, '600ul', '2026-12-31'
        UNION ALL
SELECT 'HumTuBB-R', 'LOT-2aa692a3', 1.0, '600ul', '2026-12-31'
        UNION ALL
SELECT 'Uq-R', 'LOT-1b1d465f', 1.0, '600ul', '2026-12-31'
        UNION ALL
SELECT 'U5.8S-F', 'LOT-915a2c12', 1.0, '600ul', '2026-12-31'
        UNION ALL
SELECT 'Pfldh-F', 'LOT-8a06c750', 1.0, '600ul', '2026-12-31'
        UNION ALL
SELECT 'Pfldh-R', 'LOT-e81dfbce', 1.0, '1200ul', '2026-12-31'
        UNION ALL
SELECT 'HumTubBB-F', 'LOT-793cd6b5', 1.0, '900ul', '2026-12-31'
        UNION ALL
SELECT 'stq-F', 'LOT-cb2c3df3', 1.0, '1200.0', '2026-12-31'
        UNION ALL
SELECT 'stq-R', 'LOT-edbe864d', 1.0, '900ul', '2026-12-31'
        UNION ALL
SELECT 'uq-F', 'LOT-31a97c0e', 1.0, '1200.0', '2026-12-31'
        UNION ALL
SELECT 'Pf Hrp3-R2', 'LOT-c6693cc1', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Hrp3-Rv', 'LOT-e0896f0d', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'PfHrp2-F1', 'LOT-d76e6ae5', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'PfHrp2-R3', 'LOT-25b29087', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'PfHrp2-F2', 'LOT-b95de66f', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'PfHrp3-R1', 'LOT-e0f8ade5', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'PfHrp2-R2', 'LOT-6ad519f3', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pf Hrp3-F1', 'LOT-25f3bef4', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pf Hrp3-P1', 'LOT-d2919d88', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'PfHrp2-R1', 'LOT-258de4d8', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'PfHrp3-F2', 'LOT-3d87eac7', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'PfHrp2-F3', 'LOT-0da510c6', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pfhrp3_F2', 'LOT-2150c9a7', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pfhrp3_R2', 'LOT-4702f0cf', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'GATA-MT-probe', 'LOT-d8e82274', 1.0, '300ul', '2026-12-31'
        UNION ALL
SELECT 'GATA-WT-probe', 'LOT-b2620495', 1.0, '600ul', '2026-12-31'
        UNION ALL
SELECT '2-probe', 'LOT-fb6b609d', 1.0, '600ul', '2026-12-31'
        UNION ALL
SELECT 'tRNA-probe', 'LOT-30621084', 1.0, '900ul', '2026-12-31'
        UNION ALL
SELECT 'hrp3-probe', 'LOT-b7806359', 1.0, '600ul', '2026-12-31'
        UNION ALL
SELECT 'Pfhrp2-probe', 'LOT-5225ad3a', 1.0, '600ul', '2026-12-31'
        UNION ALL
SELECT 'Pfidh-probe', 'LOT-ad6f143c', 1.0, '600ul', '2026-12-31'
        UNION ALL
SELECT 'Pfhrp3-probe', 'LOT-87be9fab', 1.0, '300ul', '2026-12-31'
        UNION ALL
SELECT 'Stq-P probe', 'LOT-04d9a00d', 1.0, '600ul', '2026-12-31'
        UNION ALL
SELECT 'Uq-P probe', 'LOT-1748e6f3', 1.0, '600ul', '2026-12-31'
        UNION ALL
SELECT 'HumanTuBB-P probe', 'LOT-161f2c5a', 1.0, '300ul', '2026-12-31'
        UNION ALL
SELECT 'Pfhrp3_probe', 'LOT-131d9c59', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pf18s-Fw (Nij)', 'LOT-15dbcfdf', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pv18s-Fw', 'LOT-fad9203b', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pv18s-fw', 'LOT-cfa6581b', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pf18s-Rv (Nij)', 'LOT-38f902ee', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pv18s-rv', 'LOT-a6364f5a', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'PfMGET-fw', 'LOT-5a1620cd', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'PfMGET-Rv', 'LOT-84238455', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'PfMGET- FW', 'LOT-34b18ac0', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'PfMGET- Rev multiplex', 'LOT-bf61c02e', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pf18s-RNA-fw', 'LOT-3f4e6b87', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pv18s -fw', 'LOT-59d10818', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pv18s-Rv', 'LOT-33eaf817', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pv18s-Rv', 'LOT-85356197', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pv18s-fw', 'LOT-ec2c2e79', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pf18s-RNA-RV', 'LOT-9bb7853e', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pfs25_FW', 'LOT-462a0a36', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pfs25_Rv', 'LOT-119a5814', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pfs25_FW', 'LOT-f9e59303', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pfs25_Rv', 'LOT-c556392d', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'SBP1-RV', 'LOT-d8d5a218', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'SBP1-Fw', 'LOT-717326ae', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pfs25-Rv', 'LOT-16c7eabc', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pf18s-DNA-RV(Swit)', 'LOT-381acded', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pf18s_Nji_RV', 'LOT-141f2c69', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pf18s-Rv', 'LOT-866f0a4c', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pf18s-DNA-FW(Swit)', 'LOT-199bf87e', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pf18s_Nji_FW', 'LOT-68a18a04', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pf18s Rev', 'LOT-0487e11d', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pf18s for', 'LOT-92698b02', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pv-18s Fw', 'LOT-c21dcfc8', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pv-18s Rv', 'LOT-89bbcf5c', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'CCp4 Rev', 'LOT-14fe95d1', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'CCp4 Fw', 'LOT-1843f25a', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pfs25-FW', 'LOT-1cb5e7f3', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'PfMGET probe (FAM)', 'LOT-5c7bd160', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'CCP4 probe TEXAS-RED)', 'LOT-d750f5de', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pvs25 probe (FAM)', 'LOT-01587e1a', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pv18s probe (HEX)', 'LOT-cfb9b8c8', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pvs25 probe (FAM)', 'LOT-d55da76e', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pv18s probe (FAM)', 'LOT-2e19b7ca', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pvs25 probe (FAM)', 'LOT-f8d289cb', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'ITS2B-mod2, 20.5nmol', 'LOT-2bbc291f', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'PAR, 24.7nmol', 'LOT-8f1e2c98', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'UN, 19.8nmol', 'LOT-904f8bb6', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Stq-F, 21.2nmol', 'LOT-79807780', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'LESS, 22.8', 'LOT-d39be399', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'RIV,23.0 nmol', 'LOT-a1a6b0ff', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'UD2, 22.3nmol', 'LOT-b03e9138', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'VAN, 22.5nmol', 'LOT-ac7f0fa4', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'FUN,19.9nmol', 'LOT-66b3cae1', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'ITS2A, 19.5nmol', 'LOT-242c9f49', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'ITS2-steph-R, 22.2nmol', 'LOT-8b217af2', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'U5.8S-F', 'LOT-df6661d5', 1.0, '', '2026-12-31'
        UNION ALL
SELECT '02Ga FW 100uM', 'LOT-df24c588', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT '63CT FW 100 uM', 'LOT-52bac107', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT '02GA RV 100 uM', 'LOT-978175fb', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT '63CT RV 100 uM', 'LOT-82621c47', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT '76AG FW 100uM', 'LOT-db9fe15b', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT '76AG RV 100uM', 'LOT-a33db5bf', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'N1RPFMDR1034 100UM', 'LOT-e3df70fa', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'N2FPFMDR1034 100UM', 'LOT-2bc15c7e', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'N1FPFMDR1034', 'LOT-2ef49fbe', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'N2RPFMDR1034', 'LOT-4f46db6a', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'N1FPFMDR86 100UM', 'LOT-6ae9bc46', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'N1RPFMDR86 100UM', 'LOT-60c85151', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'N2FPFMDR86 100UM', 'LOT-a7acb780', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'N2RPFMDR86 100UM', 'LOT-efa75615', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'Ovis F 134.9 nMol (Sheep)', 'LOT-2fc91a83', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'Can 2F 125 nMol', 'LOT-e4a9b1ad', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'ME (1) 143.5 nMol', 'LOT-986e144e', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'CAN 2R (2) 161.6 nMol', 'LOT-bffee130', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'UN (1) 139.8 nMol', 'LOT-56aa12ef', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'Ovis 2R (2) 140.4 nMol (sheep)', 'LOT-4b66ab8a', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'Hmn F (2) 126.3 nMol', 'LOT-c9e83083', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'AR (1) 130.4 nMol', 'LOT-8791cad5', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'Sus F 145.9 nMol', 'LOT-b6f269da', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'Bos 2F (2) 149nMol', 'LOT-1dedfd3e', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'Bos 2R 122.2 nMol', 'LOT-992e1260', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'Hmn R (1) 145.5 nMol', 'LOT-4684807d', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'Sus R (2) 147.5 nMol', 'LOT-e3ea8839', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'ITS 2A(2) 125.7nMol', 'LOT-e3e4fe9d', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'ITS2-Steph-R(2) 143.2 nMol', 'LOT-f10da1f2', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'GA 170.3 nMol', 'LOT-617f5d4d', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'Hco2 198 (1) 113.7 nMol', 'LOT-cf9081ba', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'LCO 1490(2) 99.7 nMol', 'LOT-3d7d2fe8', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'S200x6.1-F 132.3 nMol', 'LOT-40923671', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'Cap3R 126.8 nMol', 'LOT-c34139a1', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'plsR (revers primer)(1) 644.4 nMol', 'LOT-e23c31a9', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'S200X6.1-R(2) 157.2 nMol', 'LOT-e1fb4b57', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'PlsF(forward primer)(1) 738.6 nMol', 'LOT-32e51d7a', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'Cap3F 136.8 nMol', 'LOT-27424195', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'QD(1) 134.9 nMol', 'LOT-475f0fc9', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'N1RPFmdr86 41.9.nMol', 'LOT-962e200e', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'N2RPVmdr1 76.4.nMol', 'LOT-65e35a19', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'N1RPFcrt 41.1 nMol', 'LOT-994b1008', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'N1FPFmdr86 47.0.nMol', 'LOT-d759ef1a', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'N2RPFmdr1034 43.9 nMol', 'LOT-2a432fe7', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'N2FPFmdr 1034 61.5 nMol', 'LOT-ba1528d5', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'N2 FPFmdr86 39.1nMol', 'LOT-f6c6255e', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'N2FPFcrt 40.6nMol', 'LOT-09e7f8b1', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'N1RPVmdr1 77.7nMol', 'LOT-e9671e66', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'N1FPVmdr1 75.5nMol', 'LOT-d6ba75c1', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'N1RPFmdr1034 61.2 nMol', 'LOT-f540aa90', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'N2FPVmdr1 60.9nMol', 'LOT-a3af2a50', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'MDR3 (sense) for PFMDR1', 'LOT-1897670e', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'MDR4 (anti-sense) for PFMDR1', 'LOT-5904f6a7', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'MDR3 (sence) ', 'LOT-8b84c310', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'PV-25 FW', 'LOT-22564cc0', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'PV-25 RV', 'LOT-5063a816', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'CCp4 FW', 'LOT-5dc32e18', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'PF MGET ', 'LOT-80ca3884', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'CCp4RV', 'LOT-7c729e36', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'CCp4 RV', 'LOT-60bb8af3', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'CCp4 FW', 'LOT-13da9cb9', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'Pf MGET RV', 'LOT-036cc1a5', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'CCp4 probe ', 'LOT-e9c213bf', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'Pf MGET probe', 'LOT-8971fc84', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'M1-RR primer 2.5 nMol', 'LOT-13a0b7e8', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'M1-RF primer 2.5 nMol', 'LOT-d1aa610c', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'MGET FW 50uM', 'LOT-cdc16e52', 1.0, 'working', '2026-12-31'
        UNION ALL
SELECT 'MGET RV 50uM', 'LOT-fe5d0cf3', 1.0, 'working', '2026-12-31'
        UNION ALL
SELECT 'CCp4 forward reverse 50uM', 'LOT-abb8b082', 1.0, 'working', '2026-12-31'
        UNION ALL
SELECT 'CCp4 forward  50uM', 'LOT-da6277c5', 1.0, 'working', '2026-12-31'
        UNION ALL
SELECT 'CCp4 reverse 50uM', 'LOT-d100f00b', 1.0, 'working', '2026-12-31'
        UNION ALL
SELECT 'HumTuBB RV 100mM', 'LOT-81e5ea93', 1.0, 'stock (100mM)', '2026-12-31'
        UNION ALL
SELECT 'HumTuBB FW 100mM', 'LOT-b9e891b2', 1.0, 'stock (100mM)', '2026-12-31'
        UNION ALL
SELECT 'COX1 Plasmo F ', 'LOT-189567f8', 1.0, 'stock (100mM)', '2026-12-31'
        UNION ALL
SELECT 'COX1 Plasmo R', 'LOT-1b0aa06b', 1.0, 'stock (100mM)', '2026-12-31'
        UNION ALL
SELECT 'Cow 121 F', 'LOT-f1e22472', 1.0, 'stock (100mM)', '2026-12-31'
        UNION ALL
SELECT 'Dog 368 F', 'LOT-c8c326fb', 1.0, 'stock (100mM)', '2026-12-31'
        UNION ALL
SELECT 'Human 741 F', 'LOT-2f6759b9', 1.0, 'stock (100mM)', '2026-12-31'
        UNION ALL
SELECT 'Pig 573 F', 'LOT-50c0d8c8', 1.0, 'stock (100mM)', '2026-12-31'
        UNION ALL
SELECT 'Got 894 F', 'LOT-910c6f24', 1.0, 'stock (100mM)', '2026-12-31'
        UNION ALL
SELECT 'UnRev1025', 'LOT-615e778e', 1.0, 'stock (100mM)', '2026-12-31'
        UNION ALL
SELECT '100 bp ladder (ready made)', 'LOT-910d0f16', 7.0, '1.25', '2018-08-31'
        UNION ALL
SELECT 'Ultera low range DNAladder, 0.5ug/ul', 'LOT-91042e97', 2.0, '', '2019-11-30'
        UNION ALL
SELECT 'Trakit cyan/yellow loading buffer, 6x', 'LOT-3e815846', 4.0, '0.5', '2019-12-01'
        UNION ALL
SELECT 'Ultera low range DNAladder, 0.5ug/ul (used)', 'LOT-059156a0', 3.0, 'Used', '2019-11-30'
        UNION ALL
SELECT '100bp DNA ladder', 'LOT-a6fe8ef3', 9.0, '250.0', '2025-02-04'
        UNION ALL
SELECT 'blue/orange loading day 6X conc', 'LOT-3a104d65', 8.0, '1.0', '2025-02-04'
        UNION ALL
SELECT 'gel loading dye purple 6X conc', 'LOT-334cd514', 1.0, '0.5', '2024-01-30'
        UNION ALL
SELECT 'gel loading dye Blue/orange 6X conc', 'LOT-48e14506', 1.0, '1.0', '2025-07-03'
        UNION ALL
SELECT '100bp DNA lader with loadind dye(Blue/orange 6x)', 'LOT-59a18d37', 1.0, '250.0', '2026-09-13'
        UNION ALL
SELECT 'cyan/orange loading buffer', 'LOT-23b8ff99', 2.0, '0.2', '2026-12-31'
        UNION ALL
SELECT '50bp DNA ladder', 'LOT-6a46835f', 1.0, '50.0', '2026-12-31'
        UNION ALL
SELECT '100bp DNA ladder', 'LOT-d7f663db', 2.0, '', '2026-12-31'
        UNION ALL
SELECT '5x DNA loading buffer (blue)', 'LOT-2ff41da5', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'blue/orange 6x oading dye', 'LOT-3c0a3b40', 5.0, '1.0', '2017-09-24'
        UNION ALL
SELECT 'ultra low rabge DNA ladder', 'LOT-089bbe47', 1.0, '50.0', '2026-12-31'
        UNION ALL
SELECT '1kb + DNA lader', 'LOT-08f1a08f', 2.0, '250.0', '2026-12-31'
        UNION ALL
SELECT 'hyper ladder 100bp 100 lans', 'LOT-d93acf21', 2.0, '', '2026-12-31'
        UNION ALL
SELECT 'primer IPCF -2.5 mM', 'LOT-5fe47f0e', 2.0, '1.0', '2022-02-01'
        UNION ALL
SELECT 'primer west -26uM', 'LOT-5f497166', 1.0, '1.0', '2022-02-01'
        UNION ALL
SELECT 'primer WT (west) 25uM', 'LOT-13419b03', 1.0, '1.0', '2022-02-01'
        UNION ALL
SELECT 'primer WT (east) ', 'LOT-935b30d8', 1.0, '1.0', '2022-02-01'
        UNION ALL
SELECT 'primer east ', 'LOT-5f8ce465', 1.0, '1.0', '2022-02-01'
        UNION ALL
SELECT 'primer ALT rev ', 'LOT-b2a0aebc', 2.0, '1.0', '2022-02-01'
        UNION ALL
SELECT 'An.gambiae RSP-ST', 'LOT-ac4503d3', 1.0, '10.0', '2020-11-23'
        UNION ALL
SELECT 'An.coluzzii AKDR', 'LOT-62a77c63', 1.0, '10.0', '2020-11-24'
        UNION ALL
SELECT 'ITS2A (1) ', 'LOT-7aab3b09', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'ITS2-Steph-R(1) ', 'LOT-a8719e4a', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'QD', 'LOT-d5ece5c6', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'UN', 'LOT-b433038d', 2.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'GA', 'LOT-469edde1', 2.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'AR', 'LOT-49264b7e', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'BW', 'LOT-93eddfdc', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'QDA', 'LOT-e528996d', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'ME', 'LOT-6771dd23', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'GA RV', 'LOT-55c08e13', 1.0, 'stock (100mM)', '2026-12-31'
        UNION ALL
SELECT 'AR RV', 'LOT-2bc25bc7', 1.0, 'stock (100mM)', '2026-12-31'
        UNION ALL
SELECT 'QDA RV', 'LOT-feecf109', 1.0, 'stock (100mM)', '2026-12-31'
        UNION ALL
SELECT 'ME RV', 'LOT-f2da0e25', 1.0, 'stock (100mM)', '2026-12-31'
        UNION ALL
SELECT 'QD RV', 'LOT-e939e55e', 1.0, 'stock (100mM)', '2026-12-31'
        UNION ALL
SELECT 'UN FW', 'LOT-877f2c2a', 1.0, 'stock (100mM)', '2026-12-31'
        UNION ALL
SELECT 'IMP-S1', 'LOT-43f8b606', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'QD-3T', 'LOT-c30ec6db', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'IMP-UN', 'LOT-0f24bb44', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'IMP-M1', 'LOT-6bb2dbb2', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'AR-3T', 'LOT-e89b5be6', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'GA-3T', 'LOT-0a5fc754', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'ME-3T', 'LOT-21e64174', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'An. arabiensis Dong 5 F211', 'LOT-21c7e7cf', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'An. merus, Maf F185', 'LOT-95c94332', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'An. quadriannus, Sangwe, F180', 'LOT-cf676350', 1.0, '', '2026-12-31'
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
