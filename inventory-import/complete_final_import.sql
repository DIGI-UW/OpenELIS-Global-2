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
SELECT 'Pfcrt-F1', 'LOT-84833bc2', 1.0, 'Received reconstitute', '2026-12-31'
        UNION ALL
SELECT 'Pfcrt-R1 sequence', 'LOT-9feaf0da', 1.0, 'Received reconstitute', '2026-12-31'
        UNION ALL
SELECT 'Pfdhps-F', 'LOT-cc1b5797', 1.0, 'Received reconstitute', '2026-12-31'
        UNION ALL
SELECT 'Pfdhps-R', 'LOT-82ed6012', 1.0, 'Received reconstitute', '2026-12-31'
        UNION ALL
SELECT 'plasmepsin 3R', 'LOT-309f897a', 1.0, '', '2021-12-20'
        UNION ALL
SELECT 'plasmepsin 2R', 'LOT-62f22cb2', 1.0, '', '2021-12-21'
        UNION ALL
SELECT 'plasmepsin 2F', 'LOT-74d14ba6', 1.0, '', '2021-12-22'
        UNION ALL
SELECT 'plasmepsin 3F', 'LOT-f175c6f9', 1.0, '', '2021-12-23'
        UNION ALL
SELECT 'plasmepsin 3P', 'LOT-8cba8e33', 1.0, '', '2021-12-24'
        UNION ALL
SELECT 'plasmepsin 2P', 'LOT-b22092a3', 1.0, '', '2022-01-22'
        UNION ALL
SELECT 'Pf 18s fwd', 'LOT-e396b5a3', 2.0, '', '2021-12-20'
        UNION ALL
SELECT 'Pf 18s rev', 'LOT-6ba2fe2e', 2.0, '', '2021-12-21'
        UNION ALL
SELECT 'Beta tubulin F', 'LOT-9780c04d', 1.0, '', '2021-12-22'
        UNION ALL
SELECT 'beta tubulin R', 'LOT-6d72f5bd', 1.0, '', '2022-01-21'
        UNION ALL
SELECT 'beta tublin P', 'LOT-eea1e6be', 1.0, '', '2022-01-22'
        UNION ALL
SELECT 'cyt B-F1', 'LOT-7ae4d6d2', 1.0, '', '2022-01-23'
        UNION ALL
SELECT 'cyt B-R1', 'LOT-144bfd3e', 1.0, '', '2021-12-22'
        UNION ALL
SELECT 'mitochndorion -R1', 'LOT-4fa8b9e3', 1.0, '', '2021-12-23'
        UNION ALL
SELECT 'mitochndorion -F1', 'LOT-fe65ab8f', 1.0, '', '2021-12-24'
        UNION ALL
SELECT 'Pf k13-F1', 'LOT-ec6ff22b', 1.0, '', '2021-12-25'
        UNION ALL
SELECT 'Pf k13-R1', 'LOT-49766464', 1.0, '', '2021-12-26'
        UNION ALL
SELECT 'pfmdr1 F', 'LOT-054f66cf', 2.0, '', '2021-12-27'
        UNION ALL
SELECT 'pfmdr1 R', 'LOT-dcf12a64', 2.0, '', '2021-12-28'
        UNION ALL
SELECT 'FastDigest Alul enzyme with buffer ', 'LOT-b971f3f0', 1.0, '', '2020-12-02'
        UNION ALL
SELECT 'SmartCut enzyme with buffer ', 'LOT-502d92fd', 1.0, '', '2018-11-01'
        UNION ALL
SELECT 'mbo II 5000 U/ml', 'LOT-e302f7f7', 1.0, '0.3', '2017-05-31'
        UNION ALL
SELECT 'Nla III  10,000 U/ml', 'LOT-4f72f648', 2.0, '0.25', '2017-07-30'
        UNION ALL
SELECT 'Ase I  10,000 U/ml', 'LOT-0a45b075', 1.0, '0.2', '2017-04-30'
        UNION ALL
SELECT 'Fok I 5,000 U/ml', 'LOT-a781d3a0', 1.0, '1.0', '2018-01-30'
        UNION ALL
SELECT 'Fok I 5,000 U/ml (cutsmart)', 'LOT-90bfc055', 1.0, '1.0', '2018-11-30'
        UNION ALL
SELECT 'Dra I  20,000 U/ml', 'LOT-e8f78fb8', 1.0, '0.1', '2017-02-01'
        UNION ALL
SELECT 'ApoI 10,000 U/ml', 'LOT-b67cb3d4', 1.0, '0.1', '2017-03-01'
        UNION ALL
SELECT 'Dde I   10,000 U/ml', 'LOT-a9c052aa', 1.0, '0.1', '2017-03-02'
        UNION ALL
SELECT 'Eco RV 20,000 U/ml', 'LOT-997c8ed6', 1.0, '0.2', '2016-08-01'
        UNION ALL
SELECT 'NEBuffer  3.1  10x conc.', 'LOT-d42c46cf', 5.0, '1.25', '2018-10-01'
        UNION ALL
SELECT 'NEBuffer  4  10x conc.', 'LOT-adb5ca0c', 2.0, '5.0', '2015-03-01'
        UNION ALL
SELECT 'Afa I  1,000 U/ml', 'LOT-6827fdfc', 1.0, '', '2011-11-01'
        UNION ALL
SELECT 'Pvu II 15U/ml', 'LOT-56497b07', 1.0, '3000.0', '2007-11-01'
        UNION ALL
SELECT '10x M buffer', 'LOT-2c55a618', 3.0, '1.0', '2026-12-31'
        UNION ALL
SELECT 'cutsmart buffer  10x conc.', 'LOT-ae1c7b24', 3.0, '1.25', '2018-07-01'
        UNION ALL
SELECT 'Qiagen rnase free water', 'LOT-df4b4f91', 4.0, '1.9', '2026-12-31'
        UNION ALL
SELECT '1KB + DNA ladder', 'LOT-ad78ab34', 1.0, '', '2026-12-31'
        UNION ALL
SELECT '0.1% BSA ', 'LOT-45335956', 1.0, '1.0', '2026-12-31'
        UNION ALL
SELECT 'pscam II (alpha DNA)', 'LOT-e5c4f810', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'T2 (alpha DNA)', 'LOT-12b66b72', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'ILRI 161108 R1 66.1 nmol', 'LOT-715b02db', 1.0, '6060.9', '2026-12-31'
        UNION ALL
SELECT 'ILRI 161108 R2 60.3 nmol', 'LOT-c1aff510', 1.0, '603.1', '2026-12-31'
        UNION ALL
SELECT 'ILRI 161108 R3 60.5 nmol', 'LOT-9ad2e53b', 1.0, '604.6', '2026-12-31'
        UNION ALL
SELECT 'ILRI 161108 F3 45.1 nmol', 'LOT-de835a85', 1.0, '451.1', '2026-12-31'
        UNION ALL
SELECT 'ILRI 161108 F2 45.1 nmol', 'LOT-eef8a9b7', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'loading dye', 'LOT-a727af1a', 1.0, 'working', '2026-12-31'
        UNION ALL
SELECT 'Eurofins genomics 2R', 'LOT-7d08d393', 1.0, '', '2017-09-12'
        UNION ALL
SELECT 'Eurofins genomics 1R', 'LOT-fbc99fa0', 1.0, '', '2017-09-12'
        UNION ALL
SELECT 'Eurofins genomics pair 1R', 'LOT-f6464bae', 1.0, '', '2017-07-07'
        UNION ALL
SELECT 'Eurofins genomics pair 2F', 'LOT-64f15f29', 1.0, '', '2017-07-08'
        UNION ALL
SELECT 'Eurofins genomics pair 2R', 'LOT-e8978c1c', 1.0, '', '2017-07-07'
        UNION ALL
SELECT 'Eurofins genomics 2F', 'LOT-b2848c04', 1.0, '', '2017-09-12'
        UNION ALL
SELECT 'Eurofins genomics 1F', 'LOT-573a88c3', 1.0, '', '2017-09-13'
        UNION ALL
SELECT 'Eurofins genomics pair 1F', 'LOT-4a3a2260', 1.0, '', '2017-07-07'
        UNION ALL
SELECT '10x M buffer', 'LOT-79bd8e88', 5.0, '1.0', '2026-12-31'
        UNION ALL
SELECT 'Hae III', 'LOT-a3caaa37', 9.0, '10.0', '2007-10-01'
        UNION ALL
SELECT 'Hha I', 'LOT-ee6f037b', 3.0, '10.0', '2008-03-01'
        UNION ALL
SELECT '10x React 2', 'LOT-c62fa8e7', 7.0, '1.0', '2026-12-31'
        UNION ALL
SELECT 'quanti Tect probe PCR mastermix', 'LOT-d2b9a5cf', 2.0, '1.7', '2026-12-31'
        UNION ALL
SELECT '10x buffer T', 'LOT-5922014b', 1.0, '1.0', '2026-12-31'
        UNION ALL
SELECT 'Hae III', 'LOT-fe723111', 1.0, '10.0', '2024-11-01'
        UNION ALL
SELECT 'Hha I', 'LOT-6167f91d', 1.0, '0.1', '2014-10-01'
        UNION ALL
SELECT 'Hha1 use with react 2', 'LOT-c46295e3', 1.0, '10.0', '2010-02-28'
        UNION ALL
SELECT '10x react I', 'LOT-27002ff9', 1.0, '1.0', '2026-12-31'
        UNION ALL
SELECT '10x PCR buffer (-mgcl2)', 'LOT-a9512178', 1.0, '1.25', '2026-12-31'
        UNION ALL
SELECT 'HuPoR (alpha DNA)(5'' GGACTTCGTTTGTACCCGTTG )', 'LOT-8b64a8ac', 2.0, '', '2026-12-31'
        UNION ALL
SELECT 'b- actin R (5'' CGTCATACTCCTGCTTGCTGATCCACATCTGC)', 'LOT-fdbd5e23', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'LaeV5f (5'' CGTGATGTGCCCGAGTGCA)', 'LOT-fa0eeed5', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'cpb EF rev', 'LOT-e5f2ccdb', 1.0, 'stock ', '2026-12-31'
        UNION ALL
SELECT 'cpb EFfwd', 'LOT-03044fa0', 1.0, 'stock ', '2026-12-31'
        UNION ALL
SELECT 'K26 fwd', 'LOT-570a190a', 1.0, 'stock ', '2026-12-31'
        UNION ALL
SELECT 'k26 rev', 'LOT-bcf7eed3', 1.0, 'stock ', '2026-12-31'
        UNION ALL
SELECT 'List R', 'LOT-d216f456', 1.0, 'working', '2026-12-31'
        UNION ALL
SELECT '10xFast DigestGreen Buffer', 'LOT-e332715e', 1.0, '1.0', '2020-12-01'
        UNION ALL
SELECT '10xFast Digest Buffer (colorless)', 'LOT-29b9d778', 1.0, '1.0', '2020-12-02'
        UNION ALL
SELECT 'Fast Digest Buffer Alul', 'LOT-c158f930', 1.0, '100.0', '2020-12-03'
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
SELECT 'G-OFprimer (Glurp) 2.5 nMol', 'LOT-3b0f9246', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'OuterG4 for GLURP FW', 'LOT-65467327', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'G-OR 2.5 nMol', 'LOT-87f2ea78', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'OuterG5 for GLURP RV ', 'LOT-4dc9d6c1', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'Nested S4 for MSP-2 FW', 'LOT-06610b86', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'Nested S1 for MSP-2 FW', 'LOT-5e4844c4', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'MSP-2-S2 FW', 'LOT-5cccd0da', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'MSP-2-S3 RV', 'LOT-7be314cf', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'MSP-2-S1 tail fw', 'LOT-d45eab2c', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'MSP2-FC27 RV (FAM dye labeled)', 'LOT-8d2af20f', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'MSP2-3D7-RV (VIC dye labeled)', 'LOT-a612ae7d', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'M1-OR 2.5 nMol (MSP1M1OR)', 'LOT-695f668b', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'MSP1 M1 RV 2.5 nMol', 'LOT-6b3a1004', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'MSP1M1-FW 2.5 nMol', 'LOT-0c882d68', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'MSP1M1-OF 2.5 nMol', 'LOT-089d659b', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'MSP1M1-KF 2.5 nMol', 'LOT-3fd6c29f', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'MSP1M1-KR 2.5 nMol', 'LOT-a48dcc35', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'Nested N1 for MSP1 FW', 'LOT-a526349d', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'Nested N2 for MSP1 RV', 'LOT-d2b76e09', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'N2RPfmdr86', 'LOT-37347168', 1.0, 'Lyophilized', '2026-12-31'
        UNION ALL
SELECT 'N2RPfcrt', 'LOT-4334aad3', 1.0, 'Lyophilized', '2026-12-31'
        UNION ALL
SELECT 'N1FPfcrt', 'LOT-2d45bb5a', 1.0, 'Lyophilized', '2026-12-31'
        UNION ALL
SELECT 'N1FPfmdr1034', 'LOT-16f4ab3e', 1.0, 'Lyophilized', '2026-12-31'
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
SELECT 'phix control V3', 'LOT-9983beda', 1.0, '', '2026-12-31'
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
SELECT 'Qiagen Exraction kit', 'LOT-448af50f', 2.0, '', '2026-12-31'
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
SELECT 'sample (DNA elution) storage tube', 'LOT-bbd4e256', 1.0, '25.0', '2026-12-31'
        UNION ALL
SELECT 'Quick gel extraction & PCR Purification combo kit', 'K220001', 3.0, '50.0', '2025-10-31'
        UNION ALL
SELECT 'Bio analyzer-High sensitivity DNA chips', 'Lot: AR01BK50', 1.0, '', '2023-04-01'
        UNION ALL
SELECT 'Bio analyzer-syringe kit', 'G293868706', 2.0, '', '2026-12-31'
        UNION ALL
SELECT 'Top vision Agrose', 'R0492', 3.0, '500.0', '2026-03-01'
        UNION ALL
SELECT 'Qubit 1x ds DNA HS assay kit', 'LOT-2a2167d2', 12.0, '', '2026-12-31'
        UNION ALL
SELECT 'Qubit 1x dsL DNA BR assay standard is ', 'LOT-d53004c7', 12.0, '', '2026-12-31'
        UNION ALL
SELECT 'AmpureXP', 'LOT-21e2d509', 6.0, '', '2026-12-31'
        UNION ALL
SELECT 'Hot start MasterMix, high fidelity', 'LOT-24665c44', 15.0, '', '2026-12-31'
        UNION ALL
SELECT 'Q5 Blood Direct MasterMix', 'LOT-5268e1fa', 12.0, '', '2026-12-31'
        UNION ALL
SELECT 'cpbF (alpha DNA)', 'LOT-aeac5330', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'cpbEF-F  (alpha DNA)', 'LOT-93fdc978', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'B4 (alpha DNA)', 'LOT-0763a9bb', 3.0, '', '2026-12-31'
        UNION ALL
SELECT 'K26 f 15pM', 'LOT-e02872f1', 3.0, '', '2026-12-31'
        UNION ALL
SELECT 'T2 (alpha DNA)', 'LOT-147ce1df', 3.0, '', '2026-12-31'
        UNION ALL
SELECT 'seqcpbr2 (apha DNA)', 'LOT-f2124290', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Cpbef frd (alpha DNA)', 'LOT-05dbae48', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'seqcpbf1 (alpha DNA)', 'LOT-92649d05', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'cpbEFrev (alpha DNA)', 'LOT-3cd29539', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'cpbEF R  (alpha DNA)', 'LOT-4f43ae3b', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'dNTPs 10mM', 'LOT-239832a6', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'vsp15pM', 'LOT-6d5ddd7c', 3.0, '', '2026-12-31'
        UNION ALL
SELECT 'one-4 phor-all buffer + 10x conc.', 'LOT-c578b68f', 1.0, '1.0', '2026-12-31'
        UNION ALL
SELECT 'taq DNA poly', 'LOT-4a9169ad', 1.0, '250.0', '2026-12-31'
        UNION ALL
SELECT 'k26r (alpha DNA)', 'LOT-0bbf58a3', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'LITSR (alpha DNA)', 'LOT-7abd3d8e', 4.0, '', '2026-12-31'
        UNION ALL
SELECT 'L%.8s  (alpha DNA)', 'LOT-a675b2b3', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Laev 10 R', 'LOT-1016afb2', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'cpb TAG', 'LOT-ba127607', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'cpbr (alpha DNA)', 'LOT-cfdbe677', 1.0, '150.0', '2026-12-31'
        UNION ALL
SELECT 'cpb ATG (alpha DNA)', 'LOT-f240febb', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'big dye terminator v 3.1 cycle sequencing RR-24', 'LOT-31d1f2b1', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Ilri-161108F1', 'LOT-d3dce67d', 1.0, '604.2', '2026-12-31'
        UNION ALL
SELECT 'pgem3Zf(+)', 'LOT-c71eab22', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'm13(-21)primer', 'LOT-55da4220', 1.0, '', '2026-12-31'
        UNION ALL
SELECT '202GA-Rv', 'LOT-095b7991', 1.0, '', '2026-12-31'
        UNION ALL
SELECT '202GA-Fw', 'LOT-13690923', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Tailing Segment FW', 'LOT-94a7684d', 1.0, '14.6', '2026-12-31'
        UNION ALL
SELECT 'Tailing Segment RV', 'LOT-23d16113', 1.0, '18.9', '2026-12-31'
        UNION ALL
SELECT 'Tailing Segment FW', 'LOT-2182ae7a', 1.0, '14.6', '2026-12-31'
        UNION ALL
SELECT 'Tailing Segment RV', 'LOT-52405e43', 1.0, '18.9', '2026-12-31'
        UNION ALL
SELECT 'hrp2 one-step FW', 'LOT-305e8c6e', 1.0, '14.6', '2026-12-31'
        UNION ALL
SELECT 'hrp2 one-step RV', 'LOT-3a4fecaf', 1.0, '20.0', '2026-12-31'
        UNION ALL
SELECT 'hrp2 one-step FW', 'LOT-31203a0d', 1.0, '14.6', '2026-12-31'
        UNION ALL
SELECT 'hrp2 one-step RV', 'LOT-85afd65b', 1.0, '20.0', '2026-12-31'
        UNION ALL
SELECT 'N1FPfcrt FW', 'LOT-9e5d0736', 1.0, '11.8', '2026-12-31'
        UNION ALL
SELECT 'N1FPfcrt RV', 'LOT-dcb66111', 1.0, '7.2', '2026-12-31'
        UNION ALL
SELECT 'N1FPfcrt FW', 'LOT-994291d8', 1.0, '11.8', '2026-12-31'
        UNION ALL
SELECT 'N1FPfcrt RV', 'LOT-867be9b3', 1.0, '7.2', '2026-12-31'
        UNION ALL
SELECT 'N2FPfcrt FW', 'LOT-5be3aef2', 1.0, '13.9', '2026-12-31'
        UNION ALL
SELECT 'N2FPfcrt RV', 'LOT-7bd67864', 1.0, '12.4', '2026-12-31'
        UNION ALL
SELECT 'N2FPfcrt FW', 'LOT-cefe49ae', 1.0, '13.9', '2026-12-31'
        UNION ALL
SELECT 'N2FPfcrt RV', 'LOT-bb4a101d', 1.0, '12.4', '2026-12-31'
        UNION ALL
SELECT 'N1-Mdr1-86Y Fw', 'LOT-0110ede9', 1.0, '12.3', '2026-12-31'
        UNION ALL
SELECT 'N1-Mdr1-86Y Rv', 'LOT-0c9a9439', 1.0, '12.9', '2026-12-31'
        UNION ALL
SELECT 'N1-Mdr1-86Y Fw', 'LOT-6d381754', 1.0, '12.3', '2026-12-31'
        UNION ALL
SELECT 'N1-Mdr1-86Y Rv', 'LOT-b4ef3db4', 1.0, '12.9', '2026-12-31'
        UNION ALL
SELECT 'N2-Mdr1-86Y FW', 'LOT-75657d46', 1.0, '16.8', '2026-12-31'
        UNION ALL
SELECT 'N2-Mdr1-86Y Rv', 'LOT-c86881dc', 1.0, '17.3', '2026-12-31'
        UNION ALL
SELECT 'N2-Mdr1-86Y FW', 'LOT-02c83aee', 1.0, '16.8', '2026-12-31'
        UNION ALL
SELECT 'N2-Mdr1-86Y Rv', 'LOT-e8cf25a5', 1.0, '17.3', '2026-12-31'
        UNION ALL
SELECT 'N1FPfcrt FW', 'LOT-0a67f8a9', 1.0, '12.3', '2026-12-31'
        UNION ALL
SELECT 'N1FPfcrt RV', 'LOT-9a9cdec8', 1.0, '14.1', '2026-12-31'
        UNION ALL
SELECT 'N2FPfcrt FW', 'LOT-a32643dd', 1.0, '15.4', '2026-12-31'
        UNION ALL
SELECT 'N2FPfcrt RV', 'LOT-c608c33d', 1.0, '14.0', '2026-12-31'
        UNION ALL
SELECT 'N1-Mdr1-86Y Fw', 'LOT-26fd6b92', 1.0, '11.4', '2026-12-31'
        UNION ALL
SELECT 'N1-Mdr1-86Y Rv', 'LOT-5d516dad', 1.0, '12.1', '2026-12-31'
        UNION ALL
SELECT 'N1-Mdr1-86Y Fw', 'LOT-fc760588', 1.0, '12.4', '2026-12-31'
        UNION ALL
SELECT 'N1-Mdr1-86Y Rv', 'LOT-e27085fa', 1.0, '12.1', '2026-12-31'
        UNION ALL
SELECT 'N2-mdr1-86Y FW', 'LOT-450fc2e6', 1.0, '8.6', '2026-12-31'
        UNION ALL
SELECT 'N2-mdr1-86Y RV', 'LOT-b01c0462', 1.0, '10.4', '2026-12-31'
        UNION ALL
SELECT 'N2-mdr1-86Y FW', 'LOT-81b6ff77', 1.0, '8.6', '2026-12-31'
        UNION ALL
SELECT 'N2-mdr1-86Y RV', 'LOT-94d4dd7c', 1.0, '10.4', '2026-12-31'
        UNION ALL
SELECT 'M1-OF', 'LOT-c654ff6e', 1.0, '9.6', '2026-12-31'
        UNION ALL
SELECT 'M1-OR', 'LOT-cb8adcff', 1.0, '11.1', '2026-12-31'
        UNION ALL
SELECT 'M1-OF', 'LOT-5448f6e0', 1.0, '9.6', '2026-12-31'
        UNION ALL
SELECT 'M1-OR', 'LOT-2cfc9ba5', 1.0, '11.1', '2026-12-31'
        UNION ALL
SELECT 'RNaseP-F', 'LOT-e1c86264', 1.0, '62.1', '2026-12-31'
        UNION ALL
SELECT 'RNaseP-R', 'LOT-ddc3fada', 1.0, '65.4', '2026-12-31'
        UNION ALL
SELECT 'msp-1(N)-FW', 'LOT-4d627ced', 1.0, '25.3', '2026-12-31'
        UNION ALL
SELECT 'msp-1(N)-RV', 'LOT-d1b93170', 1.0, '10.2', '2026-12-31'
        UNION ALL
SELECT 'msp-1(N1)-FW', 'LOT-89c972a7', 1.0, '25.3', '2026-12-31'
        UNION ALL
SELECT 'msp-1(N1)-RV', 'LOT-c4e72a01', 1.0, '10.2', '2026-12-31'
        UNION ALL
SELECT 'msp-outer-F', 'LOT-ee2b61b6', 1.0, '60.6', '2026-12-31'
        UNION ALL
SELECT 'msp-outer-R', 'LOT-1b622cf1', 1.0, '59.7', '2026-12-31'
        UNION ALL
SELECT '3D7/ICfamily(N-2)-FW', 'LOT-b0480989', 1.0, '25.9', '2026-12-31'
        UNION ALL
SELECT '3D7/ICfamily(N-2)-FW', 'LOT-9eabf05f', 1.0, '25.9', '2026-12-31'
        UNION ALL
SELECT 'msp2-S2-fw', 'LOT-e649c80c', 1.0, '26.5', '2026-12-31'
        UNION ALL
SELECT 'msp2-S3-rev', 'LOT-b3e0975b', 1.0, '24.2', '2026-12-31'
        UNION ALL
SELECT 'msp-2(N1)-FW', 'LOT-7bdcd758', 1.0, '9.3', '2026-12-31'
        UNION ALL
SELECT 'msp-2(N1)-Rv', 'LOT-c25837ca', 1.0, '28.4', '2026-12-31'
        UNION ALL
SELECT 'msp2-S2-fw', 'LOT-692adc0b', 1.0, '26.5', '2026-12-31'
        UNION ALL
SELECT 'msp2-S3-rev', 'LOT-12ac4997', 1.0, '24.2', '2026-12-31'
        UNION ALL
SELECT 'msp-2(N1)-FW', 'LOT-6e072b22', 1.0, '9.3', '2026-12-31'
        UNION ALL
SELECT 'msp-2(N1)-RV', 'LOT-0431180c', 1.0, '28.4', '2026-12-31'
        UNION ALL
SELECT 'msp2-S2-F', 'LOT-10bc85ad', 1.0, '57.5', '2026-12-31'
        UNION ALL
SELECT 'msp2-S3-R', 'LOT-bd67964d', 1.0, '56.0', '2026-12-31'
        UNION ALL
SELECT 'msp2-S1Tail-fw', 'LOT-b4429067', 1.0, '18.9', '2026-12-31'
        UNION ALL
SELECT 'msp2-S1Tail-fw', 'LOT-a1b13e1b', 1.0, '18.9', '2026-12-31'
        UNION ALL
SELECT 'msp2-S1Tail-f', 'LOT-1ed004a9', 1.0, '53.8', '2026-12-31'
        UNION ALL
SELECT 'FC27family(N2)-FW', 'LOT-baefa20f', 1.0, '25.9', '2026-12-31'
        UNION ALL
SELECT 'FC27family(N2)-RV', 'LOT-750c77c9', 1.0, '11.8', '2026-12-31'
        UNION ALL
SELECT 'FC27family(N2)-FW', 'LOT-fb8c14fb', 1.0, '25.9', '2026-12-31'
        UNION ALL
SELECT 'FC27family(N2)-RV', 'LOT-b827b794', 1.0, '11.8', '2026-12-31'
        UNION ALL
SELECT 'msp1-k1-F', 'LOT-2caaddbd', 1.0, '38.8', '2026-12-31'
        UNION ALL
SELECT 'msp1-k1-R', 'LOT-d804f4c0', 1.0, '56.6', '2026-12-31'
        UNION ALL
SELECT 'msp1-Mad20-F', 'LOT-dc7ce78c', 1.0, '49.6', '2026-12-31'
        UNION ALL
SELECT 'msp1-Mad20-R', 'LOT-c43c2324', 1.0, '51.5', '2026-12-31'
        UNION ALL
SELECT 'Fc27', 'LOT-6e3abc7b', 1.0, '57.8', '2026-12-31'
        UNION ALL
SELECT '3D7', 'LOT-3de580c8', 1.0, '57.4', '2026-12-31'
        UNION ALL
SELECT 'msp2-3D7-N5-RV-probe', 'LOT-51ef8e8c', 1.0, '20.9', '2026-12-31'
        UNION ALL
SELECT 'msp2-FC27-MS-RV-probe', 'LOT-52abdbda', 1.0, '20.1', '2026-12-31'
        UNION ALL
SELECT 'Nuclase free water', 'LOT-f270a611', 6.0, '1.5', '2026-12-31'
        UNION ALL
SELECT 'NOT I', 'LOT-e12937a2', 1.0, '0.05', '2022-09-01'
        UNION ALL
SELECT 'NE buffer 3.1  ', 'LOT-3185428c', 1.0, '1.25', '2024-01-01'
        UNION ALL
SELECT 'PhHv10^3', 'LOT-815684ad', 1.0, '80.0', '2026-12-31'
        UNION ALL
SELECT 'PhHv-26267s ', 'LOT-ac3ea2df', 1.0, '232.0', '2026-12-31'
        UNION ALL
SELECT 'probephhv-305TQ-cy5', 'LOT-55eae18a', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'phhv-337as', 'LOT-b2c17ceb', 1.0, '210.0', '2011-12-01'
        UNION ALL
SELECT 'ENDFORWARD (Genosys 7112-012)', 'LOT-c210afad', 1.0, 'Lyophilized', '2026-12-31'
        UNION ALL
SELECT 'ENDREVERSE (gENOSYS 7112-013)', 'LOT-0dbcd6a5', 1.0, 'Lyophilized', '2026-12-31'
        UNION ALL
SELECT '563CT- Fw', 'LOT-551a1d6f', 1.0, 'Lyophilized', '2026-12-31'
        UNION ALL
SELECT 'DNase I, RNase free', 'LOT-e65586c5', 13.0, '', '2025-06-30'
        UNION ALL
SELECT '10X reaction buffer with MgCl2 for DNase I', 'LOT-6d7ea0ce', 13.0, '1.25', '2026-12-31'
        UNION ALL
SELECT 'MnCl2', 'LOT-18ee122a', 13.0, '1.0', '2026-12-31'
        UNION ALL
SELECT 'reaction buffer with out MnCl2 for DNase I', 'LOT-ff7bd2b6', 13.0, '1.0', '2026-12-31'
        UNION ALL
SELECT 'EDTA', 'LOT-7b84cb46', 13.0, '1.0', '2026-12-31'
        UNION ALL
SELECT 'cell proliferatio kit I (MTT)', 'LOT-4bbfde6b', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'cell proliferatio kit I (MTT)', 'LOT-7a46b98f', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'RQ1R  DNase, stop solution', 'LOT-18214e02', 1.0, '1.0', '2016-04-30'
        UNION ALL
SELECT 'RQ1 DNase, 10x rxn buffer', 'LOT-15a12506', 1.0, '1.0', '2016-04-30'
        UNION ALL
SELECT 'RQ1R  RNase freeDNase', 'LOT-96e6ad4d', 1.0, '1.0', '2016-04-30'
        UNION ALL
SELECT 'PV25probe', 'LOT-b53e00da', 1.0, '20.4', '2026-12-31'
        UNION ALL
SELECT 'PV_DBP_probe', 'LOT-496e9726', 1.0, '20.0', '2026-12-31'
        UNION ALL
SELECT 'CCp4 probe', 'LOT-6e4f4a33', 1.0, '20.0', '2026-12-31'
        UNION ALL
SELECT 'pfMGET', 'LOT-da8079be', 1.0, '20.7', '2026-12-31'
        UNION ALL
SELECT 'Ps18S', 'LOT-d9ab7e69', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'QIAcuity probe PCR kit', 'LOT-95d429b0', 15.0, '1.0', '2024-09-17'
        UNION ALL
SELECT 'Probe PCR Kit 25ml', 'LOT-4a2b435b', 1.0, '25.0', '2026-12-31'
        UNION ALL
SELECT 'Probe PCR Kit 50ml', 'LOT-44afae92', 10.0, '50.0', '2026-12-31'
        UNION ALL
SELECT 'Probe PCR Kit 15ml', 'LOT-e06e14e2', 1.0, '15.0', '2026-12-31'
        UNION ALL
SELECT 'varATS ', 'LOT-0876f44f', 18.0, 'stock', '2026-12-31'
        UNION ALL
SELECT '2E12F1', 'LOT-c72ef6e0', 1.0, '15.5', '2026-12-31'
        UNION ALL
SELECT '2E12R1', 'LOT-fae0d7eb', 1.0, '13.2', '2026-12-31'
        UNION ALL
SELECT '2E12F1', 'LOT-723f4a97', 1.0, '15.5', '2026-12-31'
        UNION ALL
SELECT '2E12R1', 'LOT-11a73a08', 1.0, '13.2', '2026-12-31'
        UNION ALL
SELECT '2E12F', 'LOT-d134e5b7', 1.0, '20.4', '2026-12-31'
        UNION ALL
SELECT '2E12R', 'LOT-6d9c5333', 1.0, '13.6', '2026-12-31'
        UNION ALL
SELECT '2E12F', 'LOT-44e05eba', 1.0, '20.4', '2026-12-31'
        UNION ALL
SELECT '2E12R', 'LOT-05ee9750', 1.0, '13.6', '2026-12-31'
        UNION ALL
SELECT '2E2F', 'LOT-1665c9ed', 1.0, '14.9', '2026-12-31'
        UNION ALL
SELECT '2E2R1', 'LOT-0641860e', 1.0, '16.6', '2026-12-31'
        UNION ALL
SELECT '2E2F', 'LOT-ec495722', 1.0, '14.9', '2026-12-31'
        UNION ALL
SELECT '2E2R1', 'LOT-6cedec00', 1.0, '16.6', '2026-12-31'
        UNION ALL
SELECT '2E2F1', 'LOT-1e34b0de', 1.0, '12.0', '2026-12-31'
        UNION ALL
SELECT '2E2R', 'LOT-3dc1e38d', 1.0, '13.8', '2026-12-31'
        UNION ALL
SELECT '2E2F1', 'LOT-87ab2fce', 1.0, '12.0', '2026-12-31'
        UNION ALL
SELECT '2E2R', 'LOT-c27598c1', 1.0, '13.8', '2026-12-31'
        UNION ALL
SELECT 'BRAVO_F', 'LOT-d498165f', 1.0, '14.6', '2026-12-31'
        UNION ALL
SELECT 'BRAVO_R', 'LOT-b4c367a7', 1.0, '17.9', '2026-12-31'
        UNION ALL
SELECT 'BRAVO_F', 'LOT-025772a7', 1.0, '14.6', '2026-12-31'
        UNION ALL
SELECT 'BRAVO_R', 'LOT-ad31a31e', 1.0, '17.9', '2026-12-31'
        UNION ALL
SELECT 'R1(2)', 'LOT-760a90b7', 1.0, '19.0', '2026-12-31'
        UNION ALL
SELECT 'F1 (M1-F1)', 'LOT-4a6e1194', 1.0, '16.2', '2026-12-31'
        UNION ALL
SELECT 'R1(2)', 'LOT-96dd1c53', 1.0, '19.0', '2026-12-31'
        UNION ALL
SELECT 'F1 (M1-F1)', 'LOT-c800d4ed', 1.0, '16.2', '2026-12-31'
        UNION ALL
SELECT 'R2', 'LOT-bee009f7', 1.0, '11.6', '2026-12-31'
        UNION ALL
SELECT 'F2', 'LOT-21c31571', 1.0, '14.4', '2026-12-31'
        UNION ALL
SELECT 'R2', 'LOT-1d0f666a', 1.0, '11.6', '2026-12-31'
        UNION ALL
SELECT 'F2', 'LOT-ad98544c', 1.0, '14.4', '2026-12-31'
        UNION ALL
SELECT 'Out PCR barcoding fw', 'LOT-c3c3d39c', 1.0, '15.3', '2026-12-31'
        UNION ALL
SELECT 'Out PCR barcoding rv', 'LOT-3b7b11a2', 1.0, '15.9', '2026-12-31'
        UNION ALL
SELECT 'Out PCR barcoding fw', 'LOT-e635fd6d', 1.0, '15.3', '2026-12-31'
        UNION ALL
SELECT 'Out PCR barcoding rv', 'LOT-64805a11', 1.0, '15.9', '2026-12-31'
        UNION ALL
SELECT 'M1-R1', 'LOT-49bc154d', 1.0, '16.9', '2026-12-31'
        UNION ALL
SELECT 'M1-IR', 'LOT-c02f2915', 1.0, '16.9', '2026-12-31'
        UNION ALL
SELECT 'Tailing Segment FW', 'LOT-ee23fc80', 1.0, '14.6', '2026-12-31'
        UNION ALL
SELECT 'Tailing Segment RV', 'LOT-81d06029', 1.0, '18.9', '2026-12-31'
        UNION ALL
SELECT 'Tailing Segment FW', 'LOT-8598c13e', 1.0, '14.6', '2026-12-31'
        UNION ALL
SELECT 'Tailing Segment RV', 'LOT-39ad4550', 1.0, '18.9', '2026-12-31'
        UNION ALL
SELECT 'hrp2 one-step FW', 'LOT-4564d8e9', 1.0, '14.6', '2026-12-31'
        UNION ALL
SELECT 'hrp2 one-step RV', 'LOT-32713623', 1.0, '20.0', '2026-12-31'
        UNION ALL
SELECT 'hrp2 one-step FW', 'LOT-c1ab14e3', 1.0, '14.6', '2026-12-31'
        UNION ALL
SELECT 'hrp2 one-step RV', 'LOT-51eac176', 1.0, '20.0', '2026-12-31'
        UNION ALL
SELECT 'N1FPfcrt FW', 'LOT-80bd15cf', 1.0, '11.8', '2026-12-31'
        UNION ALL
SELECT 'N1FPfcrt RV', 'LOT-f68b91af', 1.0, '7.2', '2026-12-31'
        UNION ALL
SELECT 'N1FPfcrt FW', 'LOT-4dcece93', 1.0, '11.8', '2026-12-31'
        UNION ALL
SELECT 'N1FPfcrt RV', 'LOT-c430f5d2', 1.0, '7.2', '2026-12-31'
        UNION ALL
SELECT 'N2FPfcrt FW', 'LOT-85e65ac4', 1.0, '13.9', '2026-12-31'
        UNION ALL
SELECT 'N2FPfcrt RV', 'LOT-35cecac4', 1.0, '12.4', '2026-12-31'
        UNION ALL
SELECT 'N2FPfcrt FW', 'LOT-c0ab25c8', 1.0, '13.9', '2026-12-31'
        UNION ALL
SELECT 'N2FPfcrt RV', 'LOT-2c2bc5da', 1.0, '12.4', '2026-12-31'
        UNION ALL
SELECT 'N1-Mdr1-86Y Fw', 'LOT-4e12c2b4', 1.0, '12.3', '2026-12-31'
        UNION ALL
SELECT 'N1-Mdr1-86Y Rv', 'LOT-60b153ea', 1.0, '12.9', '2026-12-31'
        UNION ALL
SELECT 'N1-Mdr1-86Y Fw', 'LOT-cbfab0d5', 1.0, '12.3', '2026-12-31'
        UNION ALL
SELECT 'N1-Mdr1-86Y Rv', 'LOT-bb3c863e', 1.0, '12.9', '2026-12-31'
        UNION ALL
SELECT 'N2-Mdr1-86Y FW', 'LOT-f32c8cd8', 1.0, '16.8', '2026-12-31'
        UNION ALL
SELECT 'N2-Mdr1-86Y Rv', 'LOT-673900f2', 1.0, '17.3', '2026-12-31'
        UNION ALL
SELECT 'N2-Mdr1-86Y FW', 'LOT-5d0bbd33', 1.0, '16.8', '2026-12-31'
        UNION ALL
SELECT 'N2-Mdr1-86Y Rv', 'LOT-789d9027', 1.0, '17.3', '2026-12-31'
        UNION ALL
SELECT 'N1FPfcrt FW', 'LOT-cc81a881', 1.0, '12.3', '2026-12-31'
        UNION ALL
SELECT 'N1FPfcrt RV', 'LOT-bcdcfce0', 1.0, '14.1', '2026-12-31'
        UNION ALL
SELECT 'N2FPfcrt FW', 'LOT-98a299cc', 1.0, '15.4', '2026-12-31'
        UNION ALL
SELECT 'N2FPfcrt RV', 'LOT-88938f19', 1.0, '14.0', '2026-12-31'
        UNION ALL
SELECT 'N1-Mdr1-86Y Fw', 'LOT-5f10ae19', 1.0, '11.4', '2026-12-31'
        UNION ALL
SELECT 'N1-Mdr1-86Y Rv', 'LOT-452dc86d', 1.0, '12.1', '2026-12-31'
        UNION ALL
SELECT 'N1-Mdr1-86Y Fw', 'LOT-3934c455', 1.0, '12.4', '2026-12-31'
        UNION ALL
SELECT 'N1-Mdr1-86Y Rv', 'LOT-d36e45f3', 1.0, '12.1', '2026-12-31'
        UNION ALL
SELECT 'N2-mdr1-86Y FW', 'LOT-3369f40f', 1.0, '8.6', '2026-12-31'
        UNION ALL
SELECT 'N2-mdr1-86Y RV', 'LOT-60177e9b', 1.0, '10.4', '2026-12-31'
        UNION ALL
SELECT 'N2-mdr1-86Y FW', 'LOT-3fb4d790', 1.0, '8.6', '2026-12-31'
        UNION ALL
SELECT 'N2-mdr1-86Y RV', 'LOT-70346248', 1.0, '10.4', '2026-12-31'
        UNION ALL
SELECT 'M1-OF', 'LOT-6706df38', 1.0, '9.6', '2026-12-31'
        UNION ALL
SELECT 'M1-OR', 'LOT-0d3f9072', 1.0, '11.1', '2026-12-31'
        UNION ALL
SELECT 'M1-OF', 'LOT-cfa735ac', 1.0, '9.6', '2026-12-31'
        UNION ALL
SELECT 'M1-OR', 'LOT-76bbe102', 1.0, '11.1', '2026-12-31'
        UNION ALL
SELECT 'Pfmdr11246-B', 'LOT-b1fd69ea', 1.0, '26.3', '2026-12-31'
        UNION ALL
SELECT 'Pfmdr11246-A', 'LOT-75b6b324', 1.0, '24.7', '2026-12-31'
        UNION ALL
SELECT 'Pfmdr1_RV', 'LOT-85b154cc', 1.0, '40.2', '2026-12-31'
        UNION ALL
SELECT 'Pfmdr1_FW', 'LOT-9a344729', 1.0, '50.6', '2026-12-31'
        UNION ALL
SELECT 'Pf--tubulin_FW', 'LOT-1390a86a', 1.0, '54.9', '2026-12-31'
        UNION ALL
SELECT 'Pf--tubulin_RV', 'LOT-fa8754b4', 1.0, '45.3', '2026-12-31'
        UNION ALL
SELECT 'Pfplasmepsin2__FW', 'LOT-0058eea5', 1.0, '63.4', '2026-12-31'
        UNION ALL
SELECT 'Pfplasmepsin2__RV', 'LOT-2ca631ed', 1.0, '55.3', '2026-12-31'
        UNION ALL
SELECT 'Pfmdr1246-D1', 'LOT-c953874f', 1.0, '22.2', '2026-12-31'
        UNION ALL
SELECT 'Pfmdr1246-D2', 'LOT-8df073c9', 1.0, '25.6', '2026-12-31'
        UNION ALL
SELECT 'Pfmdr1246-A', 'LOT-aafa7b9f', 1.0, '24.7', '2026-12-31'
        UNION ALL
SELECT 'Pfmdr1246-B', 'LOT-7b9267c7', 1.0, '26.3', '2026-12-31'
        UNION ALL
SELECT 'Pfmdr1246-D1', 'LOT-61abaa4f', 1.0, '22.2', '2026-12-31'
        UNION ALL
SELECT 'Pfmdr1246-D2', 'LOT-c85c0e72', 1.0, '25.6', '2026-12-31'
        UNION ALL
SELECT 'RV11', 'LOT-9e2a7d6a', 1.0, '52.7', '2026-12-31'
        UNION ALL
SELECT 'RV12', 'LOT-2a01977b', 1.0, '53.9', '2026-12-31'
        UNION ALL
SELECT '8633F', 'LOT-38fd4486', 1.0, '55.9', '2026-12-31'
        UNION ALL
SELECT '9211R', 'LOT-1c4f830a', 1.0, '62.5', '2026-12-31'
        UNION ALL
SELECT '8945F', 'LOT-fceeb213', 1.0, '69.7', '2026-12-31'
        UNION ALL
SELECT '9577R', 'LOT-4fc67994', 1.0, '59.5', '2026-12-31'
        UNION ALL
SELECT '8669F', 'LOT-9fa77ee3', 1.0, '65.0', '2026-12-31'
        UNION ALL
SELECT '9541R', 'LOT-ef848a31', 1.0, '59.4', '2026-12-31'
        UNION ALL
SELECT 'RV12-2', 'LOT-731873ab', 1.0, '63.9', '2026-12-31'
        UNION ALL
SELECT 'Pv210-Pc', 'LOT-301f0ccd', 3.0, '9.1', '2026-12-31'
        UNION ALL
SELECT 'pf-pc2', 'LOT-32244b56', 1.0, '0.25', '2026-12-31'
        UNION ALL
SELECT 'pv-210-6', 'LOT-ae09e0ac', 1.0, '0.5', '2026-12-31'
        UNION ALL
SELECT 'pf-pc5', 'LOT-0d4eb085', 1.0, '0.25', '2026-12-31'
        UNION ALL
SELECT 'pf-pc1', 'LOT-81022757', 1.0, '0.25', '2026-12-31'
        UNION ALL
SELECT 'pv-210-5', 'LOT-0e3b7ad9', 1.0, '0.3', '2026-12-31'
        UNION ALL
SELECT 'pf-pc4', 'LOT-d73557a3', 1.0, '0.3', '2026-12-31'
        UNION ALL
SELECT 'pv-210-2', 'LOT-b8610d9a', 1.0, '0.25', '2026-12-31'
        UNION ALL
SELECT 'pv-210-1', 'LOT-b1c1592b', 1.0, '0.25', '2026-12-31'
        UNION ALL
SELECT 'pf-pc6', 'LOT-9e25dac0', 1.0, '0.5', '2026-12-31'
        UNION ALL
SELECT 'pv-210 pc ELISA(stock)', 'LOT-6138a7fa', 1.0, '0.25', '2026-12-31'
        UNION ALL
SELECT 'pv-247 pc ELISA(stock)', 'LOT-09ffaf00', 1.0, '0.25', '2026-12-31'
        UNION ALL
SELECT 'Pf-PC', 'LOT-8217b235', 2.0, '0.1', '2026-12-31'
        UNION ALL
SELECT 'PV-210-3', 'LOT-542096df', 1.0, '0.25', '2026-12-31'
        UNION ALL
SELECT 'Pf-PC3', 'LOT-317c5cf9', 1.0, '0.25', '2026-12-31'
        UNION ALL
SELECT 'pf-210-4', 'LOT-1fc3ddb1', 1.0, '0.25', '2026-12-31'
        UNION ALL
SELECT 'New Naive-serum', 'LOT-5d2ec5d3', 1.0, '1.5', '2026-12-31'
        UNION ALL
SELECT 'conjugate m Ab pf', 'LOT-c81d44e8', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'pf-pc csp', 'LOT-1ba89970', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'pv hrp swp', 'LOT-7c6eecc9', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'pv-210-pc ELISA(stock)', 'LOT-6f2b446c', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'pv247-pc', 'LOT-d0b440d2', 1.0, '4.6', '2026-12-31'
        UNION ALL
SELECT 'pv capture AB', 'LOT-d9cf6a4d', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'pf capture AB', 'LOT-e7fc5c79', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'pv-210-1 m ab', 'LOT-f438f0ba', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'pv-210 2nd mab', 'LOT-93b03100', 2.0, '', '2026-12-31'
        UNION ALL
SELECT 'pv-219 pc ELISA', 'LOT-ab2f1cf4', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'CCP4-RV', 'LOT-bbb23f2b', 1.0, '500.0', '2026-12-31'
        UNION ALL
SELECT 'BCFB FY2021', 'LOT-ca2a5b2b', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'PfMGET-FW', 'LOT-4281be0f', 1.0, '1000.0', '2026-12-31'
        UNION ALL
SELECT 'PfMGET-RV', 'LOT-23a9fb10', 1.0, '500.0', '2026-12-31'
        UNION ALL
SELECT 'rPLU-5', 'LOT-4b382f71', 1.0, '1000.0', '2026-12-31'
        UNION ALL
SELECT 'Pv18s-RV', 'LOT-335b45de', 1.0, '500.0', '2026-12-31'
        UNION ALL
SELECT 'rVIV-1', 'LOT-83094ca2', 1.0, '300.0', '2026-12-31'
        UNION ALL
SELECT 'Pv525-RV', 'LOT-fc756b5a', 1.0, '500.0', '2026-12-31'
        UNION ALL
SELECT 'rPLU-6', 'LOT-84379fb6', 1.0, '1000.0', '2026-12-31'
        UNION ALL
SELECT 'PV525-FW', 'LOT-005f6e76', 1.0, '500.0', '2026-12-31'
        UNION ALL
SELECT 'Pf 18s-FW', 'LOT-90f8b540', 1.0, '300.0', '2026-12-31'
        UNION ALL
SELECT 'rFAL-2', 'LOT-ffa1c9f0', 1.0, '300.0', '2026-12-31'
        UNION ALL
SELECT 'PV18S probe', 'LOT-0380af86', 1.0, '300.0', '2026-12-31'
        UNION ALL
SELECT 'rVIV-2', 'LOT-db43d697', 1.0, '300.0', '2026-12-31'
        UNION ALL
SELECT 'rFAL-1', 'LOT-9a4ac298', 1.0, '300.0', '2026-12-31'
        UNION ALL
SELECT 'PfMEGT probe', 'LOT-2f586b84', 1.0, '7443.9', '2026-12-31'
        UNION ALL
SELECT 'Pf 18s turbo probe', 'LOT-7c9311fe', 1.0, '900.0', '2026-12-31'
        UNION ALL
SELECT 'CCP4 probe', 'LOT-c23b7c08', 1.0, '900.0', '2026-12-31'
        UNION ALL
SELECT 'PF18S RV', 'LOT-8907eda0', 1.0, '500.0', '2026-12-31'
        UNION ALL
SELECT 'PVS25 Probe', 'LOT-8988a366', 1.0, '600.0', '2026-12-31'
        UNION ALL
SELECT 'PV18S Probe', 'LOT-122d8034', 1.0, '300.0', '2026-12-31'
        UNION ALL
SELECT 'E-COLI', 'LOT-6f6dfbf0', 5.0, '~1', '2026-12-31'
        UNION ALL
SELECT 'New N-serum', 'LOT-f63931a0', 6.0, '~2', '2026-12-31'
        UNION ALL
SELECT 'E-COLI Lysate', 'LOT-d12d7e3c', 4.0, '8.64', '2026-12-31'
        UNION ALL
SELECT 'WHO PF standard(+ve control)', 'LOT-0f6768d2', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'PF +VE control(cp3 NEAT)', 'LOT-3749aa75', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'cp3(1:1)(50%)', 'LOT-333b3e26', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Et-pf pooled serum', 'LOT-36c9c869', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'E-COLI Lysate(5.2mg/ml)', 'LOT-e67bff37', 1.0, '1.6', '2026-12-31'
        UNION ALL
SELECT 'CP3 1:10', 'LOT-c7294bc2', 1.0, '0.5', '2026-12-31'
        UNION ALL
SELECT 'S1PV +VE control', 'LOT-cd721e25', 1.0, '~2,5', '2026-12-31'
        UNION ALL
SELECT 'Hot start Taq 2x Master Mix 500rxn', 'LOT-201e6be1', 1.0, '', '2024-08-01'
        UNION ALL
SELECT 'Dream Taq Master Mix 2x', 'LOT-7c2cb2ea', 5.0, '', '2026-12-31'
        UNION ALL
SELECT 'PCR buffer+green+white+mgcl', 'LOT-2e856695', 1.0, '2ml', '2019-09-14'
        UNION ALL
SELECT 'PCR buffer+green+white+mgcl', 'LOT-bbac38ee', 1.0, '2ml', '2021-10-15'
        UNION ALL
SELECT 'PCR buffer+green+white+mgcl', 'LOT-3e5038f9', 1.0, '2ml', '2026-12-31'
        UNION ALL
SELECT 'DNA ladder 100bp with buffer', 'LOT-be597f03', 2.0, '', '2026-12-31'
        UNION ALL
SELECT 'Invitrogen DNA ladder 50bp with buffer', 'LOT-89a32357', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'GoTaq nPCR componenet ', 'LOT-7ca91e63', 40.0, '', '2024-11-04'
        UNION ALL
SELECT 'GoTaq nPCR componenet ', 'LOT-ebb0c8c1', 20.0, '', '2026-12-31'
        UNION ALL
SELECT 'GoTaq nPCR componenet ', 'LOT-e7bc3bb2', 40.0, '', '2024-04-11'
        UNION ALL
SELECT 'GoTaq nPCR componenet ', 'LOT-1929a81c', 3.0, '', '2021-10-15'
        UNION ALL
SELECT 'GoTaq nPCR componenet ', 'LOT-3d2a6afd', 18.0, '', '2022-12-11'
        UNION ALL
SELECT 'GoTaq 100nM dNTPs set', 'LOT-f4e4cf72', 11.0, '', '2024-01-08'
        UNION ALL
SELECT 'Bio-dNTP Mix ', 'LOT-0cd3c4a7', 1.0, '', '2023-02-01'
        UNION ALL
SELECT 'MBL-dNTP Mix ', 'LOT-78c4d815', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Thermoscintific- dNTP Mix ', 'LOT-49fe04db', 1.0, '', '2024-05-01'
        UNION ALL
SELECT 'GoTaq dNTP set', 'LOT-c149ab46', 1.0, '', '2026-12-31'
        UNION ALL
SELECT '100bp DNA ladder ', 'LOT-f10e01ec', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'PCR Nucleotide Mix', 'LOT-8330f698', 1.0, '', '2024-04-01'
        UNION ALL
SELECT 'rplu5-100mM', 'LOT-1557a96f', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'rplu5-100mM', 'LOT-90f47eee', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'rplu6-100mM', 'LOT-34051834', 4.0, '', '2026-12-31'
        UNION ALL
SELECT 'rplu6-100mM', 'LOT-d467dbe6', 2.0, '', '2026-12-31'
        UNION ALL
SELECT 'rrplu1-100mM', 'LOT-6fcf6dc1', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'rplu3-100mM', 'LOT-1cd0d9e0', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'rplu3-100mM', 'LOT-4473dd06', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'rplu4-100mM', 'LOT-ca5c112e', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'rviv1-100mM', 'LOT-bca4e9e8', 4.0, '', '2026-12-31'
        UNION ALL
SELECT 'rviv1-100mM', 'LOT-d5689fb3', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'rviv2-100mM', 'LOT-1e4d27c0', 4.0, '', '2026-12-31'
        UNION ALL
SELECT 'rviv2-100mM', 'LOT-0f0a81bd', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'rfal2-100mM', 'LOT-34af0e8c', 4.0, '', '2026-12-31'
        UNION ALL
SELECT 'rfal2-100mM', 'LOT-4c620372', 2.0, '', '2026-12-31'
        UNION ALL
SELECT 'rfal1-100mM', 'LOT-1591b7ee', 4.0, '', '2026-12-31'
        UNION ALL
SELECT 'rfal1-100mM', 'LOT-911f9643', 2.0, '', '2026-12-31'
        UNION ALL
SELECT 'rmal1-100mM', 'LOT-f5c866ea', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'rmal2-100mM', 'LOT-9d6e3b6d', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'rova1-100mM', 'LOT-7f662393', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'rova2-100mM', 'LOT-551e2a02', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pf - Rv (rFAL2)', 'LOT-325d91c3', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pf-fw (rFAL1)', 'LOT-e95c5de3', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pv Rv (rVIV 2)', 'LOT-b5c172f2', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pv Fw (rVIV 1)', 'LOT-7ce6629e', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'rPLU 6 forward', 'LOT-3f799d14', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'rPLU 5 reverse', 'LOT-1e8e32b2', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'dCTP-100mM', 'LOT-5a770207', 2.0, '1.0', '2023-04-09'
        UNION ALL
SELECT 'dTTP-100mM', 'LOT-19b1c0eb', 2.0, '1.0', '2023-04-09'
        UNION ALL
SELECT 'dATP-100mM', 'LOT-d2430f42', 2.0, '1.0', '2023-04-09'
        UNION ALL
SELECT 'dGTP-100mM', 'LOT-fefce29b', 2.0, '1.0', '2023-04-13'
        UNION ALL
SELECT 'dTTP-100mM', 'LOT-ded58619', 4.0, '0.4&1', '2022-02-17'
        UNION ALL
SELECT 'dGTP-100mM', 'LOT-e88eba3b', 4.0, '0.4&1', '2022-04-28'
        UNION ALL
SELECT 'dATP-100mM', 'LOT-dedaf6e7', 4.0, '0.4&1', '2022-05-27'
        UNION ALL
SELECT 'dCTP-100mM', 'LOT-a503d240', 4.0, '0.4&1', '2022-02-17'
        UNION ALL
SELECT 'dATP-100mM', 'LOT-01cdd00b', 11.0, '0.4&1', '2024-09-19'
        UNION ALL
SELECT 'dGTP-100mM', 'LOT-ee2e6e5e', 11.0, '0.4&1', '2024-09-26'
        UNION ALL
SELECT 'dCTP-100mM', 'LOT-3205fbbc', 11.0, '0.4&1', '2024-09-19'
        UNION ALL
SELECT 'dTTP-100mM', 'LOT-bd64f7f5', 11.0, '0.4&1', '2024-08-01'
        UNION ALL
SELECT 'dNTP Mix -2.5mM', 'LOT-7400048f', 13.0, '1.0', '2026-12-31'
        UNION ALL
SELECT 'dATP-100mM', 'LOT-44e32649', 2.0, '1.0', '2022-05-27'
        UNION ALL
SELECT 'dCTP-100mM', 'LOT-6cf5b991', 2.0, '1.0', '2022-02-17'
        UNION ALL
SELECT 'dTTP-100mM', 'LOT-18782014', 2.0, '1.0', '2022-02-17'
        UNION ALL
SELECT 'dGTP-100mM', 'LOT-53022fdc', 2.0, '1.0', '2022-04-28'
        UNION ALL
SELECT 'dNTP MIX -10mM', 'LOT-9eafe561', 1.0, '1.0', '2022-05-12'
        UNION ALL
SELECT 'dATP-100mM   100umol', 'LOT-d6884579', 1.0, '1.0', '2018-06-01'
        UNION ALL
SELECT 'dCTP-100mM  100umol', 'LOT-7ada356b', 1.0, '1.0', '2018-06-01'
        UNION ALL
SELECT 'dGTP-100mM  100umol', 'LOT-bf1d938b', 2.0, '1.0', '2018-06-01'
        UNION ALL
SELECT 'dTTP-100mM  100umol', 'LOT-5430f732', 1.0, '1.0', '2018-06-01'
        UNION ALL
SELECT 'dNTP MIX with dUTP-12.5mM', 'LOT-b4a5e5e9', 1.0, '1.0', '2026-12-31'
        UNION ALL
SELECT 'dNTP MIX with dTTP-10mM', 'LOT-e7240aeb', 1.0, '1.0', '2026-12-31'
        UNION ALL
SELECT 'dNTP mix 10mM', 'LOT-bada1175', 10.0, '1.0', '2023-02-01'
        UNION ALL
SELECT 'AF1III ', 'LOT-accdb08f', 1.0, '0.025', '2024-01-01'
        UNION ALL
SELECT 'AF1III ', 'LOT-604bea5c', 1.0, '0.025', '2024-01-01'
        UNION ALL
SELECT 'APoI', 'LOT-b8df02e0', 1.0, '0.1', '2018-07-01'
        UNION ALL
SELECT 'N1FPfcrt-100mM', 'LOT-265730ba', 2.0, '', '2026-12-31'
        UNION ALL
SELECT 'N2FPfcrt-100mM', 'LOT-af0d074c', 2.0, '', '2026-12-31'
        UNION ALL
SELECT 'N1FPfcrt RW-100mM', 'LOT-9a87c542', 1.0, '', '2022-12-01'
        UNION ALL
SELECT 'N1FPfcrt FW-100mM', 'LOT-d3592b71', 1.0, '', '2022-12-01'
        UNION ALL
SELECT 'N2FPfcrt RW-100mM', 'LOT-aa876ad8', 1.0, '', '2022-12-01'
        UNION ALL
SELECT 'N2FPfcrt FW-100mM', 'LOT-e5128778', 1.0, '', '2022-12-01'
        UNION ALL
SELECT 'green Go Taq buffer', 'LOT-dae656d8', 15.0, '1.0', '2026-12-31'
        UNION ALL
SELECT 'MgCl2 -25mM', 'LOT-120a8db3', 16.0, '1.2', '2026-12-31'
        UNION ALL
SELECT 'colorless Go Taq buffer', 'LOT-4fe98c60', 25.0, '1.0', '2026-12-31'
        UNION ALL
SELECT 'colorless Go Taq buffer', 'LOT-eefd626e', 68.0, '1.0', '2022-11-22'
        UNION ALL
SELECT 'colorless Go Taq buffer', 'LOT-f4c61495', 33.0, '1.0', '2026-12-31'
        UNION ALL
SELECT 'MgCl2 -25mM', 'LOT-b43b04ef', 17.0, '1.2', '2026-12-31'
        UNION ALL
SELECT 'green Go Taq buffer', 'LOT-fd46b354', 23.0, '1.0', '2026-12-31'
        UNION ALL
SELECT 'green Go Taq buffer', 'LOT-36c2da2b', 69.0, '1.0', '2022-11-12'
        UNION ALL
SELECT 'MgCl2 -25mM', 'LOT-a54f0ed2', 54.0, '1.2', '2022-11-12'
        UNION ALL
SELECT 'green Go Taq buffer', 'LOT-cc1ceae5', 59.0, '1.0', '2024-04-11'
        UNION ALL
SELECT 'green Go Taq buffer', 'LOT-54d63c80', 56.0, '1.0', '2024-04-11'
        UNION ALL
SELECT 'green Go Taq buffer', 'LOT-f745ec99', 44.0, '1.0', '2024-04-11'
        UNION ALL
SELECT 'colorless Go Taq buffer', 'LOT-6d39772e', 40.0, '1.0', '2024-04-11'
        UNION ALL
SELECT 'colorless Go Taq buffer', 'LOT-b77f677c', 53.0, '1.0', '2024-04-11'
        UNION ALL
SELECT 'colorless Go Taq buffer', 'LOT-61b07ba6', 58.0, '1.0', '2024-04-11'
        UNION ALL
SELECT 'MgCl2 -25mM', 'LOT-5ac5cb49', 43.0, '1.2', '2024-04-11'
        UNION ALL
SELECT 'MgCl2 -25mM', 'LOT-46e41b96', 48.0, '1.2', '2024-04-11'
        UNION ALL
SELECT 'MgCl2 -25mM', 'LOT-67ae0dc3', 27.0, '1.2', '2024-04-11'
        UNION ALL
SELECT 'GO Taq G2 flexi DNA polymerases 5 u/ul', 'LOT-e39d8785', 40.0, '500.0', '2024-12-12'
        UNION ALL
SELECT 'HOT Start Taq 2X master mix (500 rxn / vial )', 'LOT-37ff85e5', 2.0, '50.0', '2024-08-31'
        UNION ALL
SELECT 'dNTP set 4x25 umol', 'LOT-0bf543b6', 4.0, '1.0', '2024-05-31'
        UNION ALL
SELECT 'PCR nucleotide mix 10mM', 'LOT-28df401c', 1.0, '200.0', '2024-01-04'
        UNION ALL
SELECT 'flexi DNA polymerase', 'LOT-931f1884', 12.0, '500.0', '2020-09-18'
        UNION ALL
SELECT 'GO Taq G2 flexi DNA polymerases 5u/ul', 'LOT-06c43cde', 18.0, '500.0', '2022-11-12'
        UNION ALL
SELECT 'Go Taq flexi DNA polymerase', 'LOT-d830c427', 3.0, '500.0', '2021-10-15'
        UNION ALL
SELECT 'MgCl2 -25mM', 'LOT-22347262', 15.0, '1.2', '2021-10-15'
        UNION ALL
SELECT 'green Go Taq buffer', 'LOT-678a8fc3', 20.0, '1.0', '2021-10-15'
        UNION ALL
SELECT 'colorless Go Taq buffer', 'LOT-cbc25a7f', 20.0, '1.0', '2021-10-15'
        UNION ALL
SELECT 'NPCR components (Go Taq)', 'LOT-3e2bc9c8', 13.0, '', '2020-09-18'
        UNION ALL
SELECT 'Flexi Go Taq DNA polymerase', 'LOT-1f39ef28', 1.0, '', '2024-04-11'
        UNION ALL
SELECT 'F-Rv+Fw', 'LOT-e4115d8a', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'O-Rv+Fw', 'LOT-2e5b4fb5', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'M-Rv+Fw', 'LOT-9df16ee8', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'V-Rv+Fw', 'LOT-7e77a919', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'K-Rv+Fw', 'LOT-dd5d2617', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'P-Rv+Fw', 'LOT-33fea46e', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'rCutSmart Buffer', 'LOT-d669b3a5', 1.0, '1.25', '2025-04-01'
        UNION ALL
SELECT 'rCutSmart Buffer', 'LOT-a17e60e9', 1.0, '1.25', '2025-02-01'
        UNION ALL
SELECT 'rCutSmart Buffer', 'LOT-11956103', 1.0, '1.25', '2024-07-01'
        UNION ALL
SELECT 'rCutSmart Buffer', 'LOT-61aa7bdb', 1.0, '1.25', '2025-04-01'
        UNION ALL
SELECT 'gel loading dye purple', 'LOT-3a82dd7e', 1.0, '0.5', '2024-12-01'
        UNION ALL
SELECT 'gel loading dye purple', 'LOT-ade054a9', 1.0, '0.5', '2024-09-01'
        UNION ALL
SELECT 'gel loading dye purple', 'LOT-db4f3780', 1.0, '0.5', '2024-03-01'
        UNION ALL
SELECT 'gel loading dye purple', 'LOT-f20ea712', 1.0, '0.5', '2024-05-01'
        UNION ALL
SELECT 'NEBuffer', 'LOT-fd1dc685', 1.0, '1.25', '2024-12-01'
        UNION ALL
SELECT 'NEBuffer', 'LOT-9fe68975', 2.0, '1.25', '2024-10-01'
        UNION ALL
SELECT 'HhaI', 'LOT-5e1a8563', 1.0, '0.1', '2024-02-01'
        UNION ALL
SELECT 'EcoRI-HF', 'LOT-809415fc', 2.0, '1.25', '2024-03-01'
        UNION ALL
SELECT 'BgI-II', 'LOT-8d534b52', 1.0, '0.2', '2023-08-01'
        UNION ALL
SELECT 'Dde-I', 'LOT-371eaffd', 1.0, '0.1', '2023-07-01'
        UNION ALL
SELECT 'Pst-I', 'LOT-8f01dfea', 1.0, '0.5', '2023-11-01'
        UNION ALL
SELECT 'BIO Taq DNA polymerase', 'LOT-1fe9d1ab', 20.0, '100.0', '2023-12-30'
        UNION ALL
SELECT 'NH4 reaction buffer, MgCl2 free', 'LOT-3cb0b53a', 40.0, '1.2', '2023-12-30'
        UNION ALL
SELECT 'MgCl2 ', 'LOT-d336c873', 20.0, '1.2', '2023-12-30'
        UNION ALL
SELECT 'RQ1 RNase-Free DNase', 'LOT-148c6b13', 1.0, '', '2016-04-30'
        UNION ALL
SELECT 'DNase I, RNase-free', 'LOT-098c05c2', 15.0, '', '2025-06-01'
        UNION ALL
SELECT 'RNase inhibitor', 'LOT-3f9e9c22', 5.0, '', '2019-07-01'
        UNION ALL
SELECT 'RNase inhibitor', 'LOT-ff8d9d15', 1.0, '', '2017-08-01'
        UNION ALL
SELECT 'High capacity cDNA Reverse T kit', 'LOT-0b993a16', 1.0, '', '2015-06-23'
        UNION ALL
SELECT 'One step RT-qPCR kit 2500rxn', 'LOT-5cd876ca', 1.0, '', '2022-04-01'
        UNION ALL
SELECT 'One step RT-qPCR kit 500rxn', 'LOT-66bf7c6d', 1.0, '', '2019-08-01'
        UNION ALL
SELECT 'One step RT-SuperMix kit 25rxn', 'LOT-657d8ca0', 1.0, '', '2020-02-01'
        UNION ALL
SELECT 'Luna warm start RT enzyme mix, 20x conc', 'LOT-df936118', 1.0, 'used', '2019-08-31'
        UNION ALL
SELECT 'Luna universal probe 1 step reaction mix 2x conc', 'LOT-7a6a0811', 1.0, '1.0', '2020-01-30'
        UNION ALL
SELECT 'GO Taq PCR master mix 2X c0nc', 'LOT-bc0a2ced', 2.0, 'used', '2020-05-28'
        UNION ALL
SELECT 'MQ', 'LOT-cb1fb378', 1.0, '1.5', '2020-11-30'
        UNION ALL
SELECT 'Luna One step RT-qPCR kit 2,500 rxn', 'LOT-4775b4dc', 2.0, '', '2021-04-30'
        UNION ALL
SELECT 'Luna One step RT-qPCR kit 2,500 rxn', 'LOT-37b5c3f7', 2.0, '', '2022-04-30'
        UNION ALL
SELECT 'Luna One step RT-qPCR kit 2,500 rxn', 'LOT-62c54e06', 6.0, '', '2024-01-30'
        UNION ALL
SELECT 'Luna One step RT-qPCR kit 2,500 rxn', 'LOT-935ba66a', 6.0, '', '2024-01-30'
        UNION ALL
SELECT 'High capacity cDNA rt kit', '4.0', 1.0, '1.0', '2023-01-31'
        UNION ALL
SELECT 'UD2-R_2_P.PIGNATELL', 'LOT-ac3bffe7', 1.0, '', '2022-08-09'
        UNION ALL
SELECT 'St-F_2_P.PIGNATELL', 'LOT-527aa5a2', 1.0, '', '2022-08-09'
        UNION ALL
SELECT 'U5.8S-F_2_P.PIGNATELL', 'LOT-49631ad9', 1.0, '', '2022-08-09'
        UNION ALL
SELECT 'ITS2-steph-R_P.PIGNATELL', 'LOT-ac4ca387', 1.0, '', '2021-07-26'
        UNION ALL
SELECT 'ITS2A_P.PIGNATELL', 'LOT-ef59b6b4', 1.0, '', '2021-07-26'
        UNION ALL
SELECT 'ITS2B_mod2_P.PIGNATELL', 'LOT-3e567797', 1.0, '', '2021-07-26'
        UNION ALL
SELECT 'OVM_115402_C8', 'LOT-3e1aa791', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'OVM_115394_C4', 'LOT-7bc4cc3b', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pfr364af_fw', 'LOT-18826ed1', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pfr364af_rb', 'LOT-63b8ba68', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pvdhfrv_fw', 'LOT-88169294', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pvdhfrv_rv', 'LOT-ee7da5f6', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Hrp2 exon 2 fwd', 'LOT-397b2256', 1.0, '', '2022-01-31'
        UNION ALL
SELECT 'Hrp2 exon 2 rev', 'LOT-6c526bcf', 1.0, '', '2022-01-31'
        UNION ALL
SELECT 'Hrp3 rev ', 'LOT-44566429', 1.0, '', '2022-01-31'
        UNION ALL
SELECT 'Hrp3 fwd', 'LOT-9c943b3e', 1.0, '', '2022-01-31'
        UNION ALL
SELECT 'Trna rev', 'LOT-2856c9a0', 1.0, '', '2022-01-31'
        UNION ALL
SELECT 'Trna fwd', 'LOT-159234a3', 1.0, '', '2022-01-31'
        UNION ALL
SELECT 'Hrp2 exon 2 probe', 'LOT-3d0f6052', 1.0, '', '2022-01-29'
        UNION ALL
SELECT 'Hrp2 probe', 'LOT-b45fc1c4', 1.0, '', '2022-02-17'
        UNION ALL
SELECT 'Hrp3 probe', 'LOT-661a558e', 1.0, '', '2022-02-17'
        UNION ALL
SELECT 'Trna probe', 'LOT-1d72e002', 1.0, '', '2022-02-02'
        UNION ALL
SELECT 'Pvmsp2_n_rev', 'LOT-8bed0613', 1.0, '', '2022-02-05'
        UNION ALL
SELECT 'Pvmsp2_p_fwd', 'LOT-83974b37', 1.0, '', '2022-02-05'
        UNION ALL
SELECT 'Pvmsp2_n_fwd', 'LOT-eba0ba2f', 1.0, '', '2022-01-28'
        UNION ALL
SELECT 'Pvmsp2_p_rev', 'LOT-c623683a', 1.0, '', '2022-02-05'
        UNION ALL
SELECT 'Pvmsp1f3_p_rev', 'LOT-f3748540', 1.0, '', '2022-02-09'
        UNION ALL
SELECT 'Pvmsp1f3_n_fwd', 'LOT-2a30aed1', 1.0, '', '2022-01-28'
        UNION ALL
SELECT 'Pvmsp1f3_p_fwd', 'LOT-9b34e469', 1.0, '', '2022-02-09'
        UNION ALL
SELECT 'Pvmsp1f3_n_rev', 'LOT-446c2330', 1.0, '', '2022-02-05'
        UNION ALL
SELECT 'Msp2_s1 tail_fwd', 'LOT-e6591199', 1.0, '', '2022-02-05'
        UNION ALL
SELECT 'Msp2_fc27_m5_rev', 'LOT-b5540222', 1.0, '', '2022-01-28'
        UNION ALL
SELECT 'Msp2_3d7_n5_rev', 'LOT-802c6772', 1.0, '', '2022-01-28'
        UNION ALL
SELECT 'Pfmsp2_s2_fw', 'LOT-917ccc42', 1.0, '', '2022-02-05'
        UNION ALL
SELECT 'Pfmsp2_s3_rv', 'LOT-5aa08439', 1.0, '', '2022-02-05'
        UNION ALL
SELECT 'ed protein (hyp8) rv', 'LOT-103ff2fd', 1.0, '', '2026-12-31'
        UNION ALL
SELECT '563ct_rv', 'LOT-80086047', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'rPLU3', 'LOT-64ec636e', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'rPLU4', 'LOT-0bb8d4ab', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'TARE_2_fw', 'LOT-17a06b0a', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'TARE_2_rv', 'LOT-83548ce5', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'RESA rv', 'LOT-bd94b123', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'RESA fw', 'LOT-0cdddc85', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pfs25_fw', 'LOT-42ac4331', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'rPLU1', 'LOT-d6b30852', 1.0, '', '2026-12-31'
        UNION ALL
SELECT '376AG fw', 'LOT-469db809', 1.0, '', '2026-12-31'
        UNION ALL
SELECT '376AG rv', 'LOT-5082eea8', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pv18s_ fw', 'LOT-36c40f2d', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pv18s_ rv', 'LOT-ec153401', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pmal_qPCR_ fw', 'LOT-3457dcc4', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pmal_qPCR_ rv', 'LOT-90c17f75', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pova_qPCR_fw', 'LOT-7ae33560', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pova_qPCR_rv', 'LOT-250e8eb2', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'PgMET_fw', 'LOT-4957fb05', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'PgMET_rw', 'LOT-2b55d810', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pvs25 fw', 'LOT-1c47caf1', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pvs25 rev', 'LOT-79b6b324', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'QMAL fw', 'LOT-4ff8eb6e', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'QMAL rv', 'LOT-4ccaefdc', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'QMAL fw', 'LOT-e1bf17aa', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'QMAL rv', 'LOT-cae6bc0e', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pfs230p_fw', 'LOT-e9447bc1', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pfs230p_rv', 'LOT-b1eae2fd', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'varATS_fw', 'LOT-fa8d7581', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'varATS_rv', 'LOT-60c082c1', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'varATS_fw', 'LOT-fe0907f9', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'varATS_rv', 'LOT-e33af2b1', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pf18S RV 100uM', 'LOT-29913bca', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'PV18S FW MPX 100uM', 'LOT-0fbc1147', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'Pf18S probe 100uM', 'LOT-67940335', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'Pv18S MPX 100uM', 'LOT-9e11ca1b', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'PV18S RV 100uM  ', 'LOT-111886b5', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'Pf18s FW 100uM', 'LOT-ff7788b1', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'PV25 probe 5.3 nMol', 'LOT-eaaadfcd', 1.0, 'lyophilized ', '2026-12-31'
        UNION ALL
SELECT 'PfMDR1 probe 5.1 nMol', 'LOT-da6e5b40', 1.0, 'lyophilized ', '2026-12-31'
        UNION ALL
SELECT 'Beta TT probe 5.2 nMol', 'LOT-add2819b', 1.0, 'lyophilized ', '2026-12-31'
        UNION ALL
SELECT 'Pf plasmepsin2 probe 5.3 nMol', 'LOT-acc9af87', 1.0, 'lyophilized ', '2026-12-31'
        UNION ALL
SELECT 'Viv f(shako) ', 'LOT-c088363c', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'Fal-F(shako)', 'LOT-844b4819', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'Pf18S shoko probe 5.3 nMOL', 'LOT-ca731526', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'Pv18s SHOKO probe ', 'LOT-63d462dc', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'Plasmo rev 100UM Shoko (for Pv- Pf mpx)', 'LOT-631b8b9d', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'Pv 18S REV', 'LOT-a989efc8', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'Pv-25 Fw', 'LOT-1d4a3366', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pv-25 Rev', 'LOT-1def0446', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pv-25 Rv', 'LOT-de1bd8b3', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pvs25 Fw', 'LOT-213dd6bf', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pv-18s new short', 'LOT-75bf9a0e', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pf-18s- DNA-Rv', 'LOT-5264f20d', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pf-18s- DNA-Fw', 'LOT-a6aef23b', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'StD2', 'LOT-42e5555c', 1.0, '300ul', '2026-12-31'
        UNION ALL
SELECT 'GATA-1F', 'LOT-25b42347', 1.0, '1000ul', '2026-12-31'
        UNION ALL
SELECT 'GATA-1R', 'LOT-3c4321a0', 1.0, '1000ul', '2026-12-31'
        UNION ALL
SELECT 'hrp2-Exon-FW', 'LOT-fee8a06f', 1.0, '600ul', '2026-12-31'
        UNION ALL
SELECT 'tRNA-FW', 'LOT-75e8fec4', 1.0, '600ul', '2026-12-31'
        UNION ALL
SELECT 'hrp3-FW', 'LOT-e9c5ef29', 1.0, '600ul', '2026-12-31'
        UNION ALL
SELECT 'uq-R', 'LOT-be9ba4cf', 1.0, '600ul', '2026-12-31'
        UNION ALL
SELECT 'UD2-R', 'LOT-6a91e416', 1.0, '600ul', '2026-12-31'
        UNION ALL
SELECT 'St-F', 'LOT-cddb60a0', 1.0, '600ul', '2026-12-31'
        UNION ALL
SELECT 'StD2-R', 'LOT-91916082', 1.0, '600ul', '2026-12-31'
        UNION ALL
SELECT 'us-85-F', 'LOT-01559f8b', 1.0, '600ul', '2026-12-31'
        UNION ALL
SELECT 'st-F', 'LOT-502eae81', 1.0, '600ul', '2026-12-31'
        UNION ALL
SELECT 'UD2-R', 'LOT-5a1a790c', 1.0, '600ul', '2026-12-31'
        UNION ALL
SELECT 'tRNA-Rv', 'LOT-72e34d8e', 1.0, '600ul', '2026-12-31'
        UNION ALL
SELECT 'hrp2-Exon2-RV', 'LOT-28b73b1e', 1.0, '600ul', '2026-12-31'
        UNION ALL
SELECT 'HumTuBB-R', 'LOT-cadc0854', 1.0, '600ul', '2026-12-31'
        UNION ALL
SELECT 'Uq-R', 'LOT-6bc76936', 1.0, '600ul', '2026-12-31'
        UNION ALL
SELECT 'U5.8S-F', 'LOT-acda4dc1', 1.0, '600ul', '2026-12-31'
        UNION ALL
SELECT 'Pfldh-F', 'LOT-7bde596e', 1.0, '600ul', '2026-12-31'
        UNION ALL
SELECT 'Pfldh-R', 'LOT-39f2b00b', 1.0, '1200ul', '2026-12-31'
        UNION ALL
SELECT 'HumTubBB-F', 'LOT-9ac20d3c', 1.0, '900ul', '2026-12-31'
        UNION ALL
SELECT 'stq-F', 'LOT-76c70438', 1.0, '1200.0', '2026-12-31'
        UNION ALL
SELECT 'stq-R', 'LOT-e74b96ce', 1.0, '900ul', '2026-12-31'
        UNION ALL
SELECT 'uq-F', 'LOT-13b06169', 1.0, '1200.0', '2026-12-31'
        UNION ALL
SELECT 'Pf Hrp3-R2', 'LOT-f2354c8e', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Hrp3-Rv', 'LOT-63887831', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'PfHrp2-F1', 'LOT-90048670', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'PfHrp2-R3', 'LOT-0f577350', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'PfHrp2-F2', 'LOT-c714d9bb', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'PfHrp3-R1', 'LOT-ed7238d3', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'PfHrp2-R2', 'LOT-ce4ce96b', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pf Hrp3-F1', 'LOT-0f0028a4', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pf Hrp3-P1', 'LOT-7439d5cc', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'PfHrp2-R1', 'LOT-772c3118', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'PfHrp3-F2', 'LOT-9b69c715', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'PfHrp2-F3', 'LOT-1652ea72', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pfhrp3_F2', 'LOT-2177aaf5', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pfhrp3_R2', 'LOT-d725de5a', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'GATA-MT-probe', 'LOT-bdaf8df5', 1.0, '300ul', '2026-12-31'
        UNION ALL
SELECT 'GATA-WT-probe', 'LOT-ef75726a', 1.0, '600ul', '2026-12-31'
        UNION ALL
SELECT '2-probe', 'LOT-854c88b5', 1.0, '600ul', '2026-12-31'
        UNION ALL
SELECT 'tRNA-probe', 'LOT-a6210ee7', 1.0, '900ul', '2026-12-31'
        UNION ALL
SELECT 'hrp3-probe', 'LOT-5e3533db', 1.0, '600ul', '2026-12-31'
        UNION ALL
SELECT 'Pfhrp2-probe', 'LOT-e8bb4df9', 1.0, '600ul', '2026-12-31'
        UNION ALL
SELECT 'Pfidh-probe', 'LOT-5782367c', 1.0, '600ul', '2026-12-31'
        UNION ALL
SELECT 'Pfhrp3-probe', 'LOT-bde71fe6', 1.0, '300ul', '2026-12-31'
        UNION ALL
SELECT 'Stq-P probe', 'LOT-c6238cf1', 1.0, '600ul', '2026-12-31'
        UNION ALL
SELECT 'Uq-P probe', 'LOT-728c36d1', 1.0, '600ul', '2026-12-31'
        UNION ALL
SELECT 'HumanTuBB-P probe', 'LOT-1ab12161', 1.0, '300ul', '2026-12-31'
        UNION ALL
SELECT 'Pfhrp3_probe', 'LOT-f2052a4d', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pf18s-Fw (Nij)', 'LOT-3a9b86b6', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pv18s-Fw', 'LOT-a7666a97', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pv18s-fw', 'LOT-0834612c', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pf18s-Rv (Nij)', 'LOT-dbce33d9', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pv18s-rv', 'LOT-ab890da3', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'PfMGET-fw', 'LOT-8f97700d', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'PfMGET-Rv', 'LOT-36e4ab3c', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'PfMGET- FW', 'LOT-31c78211', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'PfMGET- Rev multiplex', 'LOT-fd161d1f', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pf18s-RNA-fw', 'LOT-f581f44a', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pv18s -fw', 'LOT-05f29ca5', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pv18s-Rv', 'LOT-475168ed', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pv18s-Rv', 'LOT-743c5fcb', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pv18s-fw', 'LOT-7c081249', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pf18s-RNA-RV', 'LOT-81543ab2', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pfs25_FW', 'LOT-8c7ac20e', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pfs25_Rv', 'LOT-5a216e4f', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pfs25_FW', 'LOT-467d3de6', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pfs25_Rv', 'LOT-2bd6ebe0', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'SBP1-RV', 'LOT-5a75bacc', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'SBP1-Fw', 'LOT-bf6afa11', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pfs25-Rv', 'LOT-335ed59d', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pf18s-DNA-RV(Swit)', 'LOT-775d8504', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pf18s_Nji_RV', 'LOT-051a358c', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pf18s-Rv', 'LOT-ed3fa77d', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pf18s-DNA-FW(Swit)', 'LOT-efc4eb5d', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pf18s_Nji_FW', 'LOT-463768b5', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pf18s Rev', 'LOT-3244beb1', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pf18s for', 'LOT-eec40fd1', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pv-18s Fw', 'LOT-87d1e99a', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pv-18s Rv', 'LOT-d2c4c677', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'CCp4 Rev', 'LOT-e40af462', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'CCp4 Fw', 'LOT-e7dbbde7', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pfs25-FW', 'LOT-b6546b55', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'PfMGET probe (FAM)', 'LOT-006f960d', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'CCP4 probe TEXAS-RED)', 'LOT-7ae74764', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pvs25 probe (FAM)', 'LOT-ee921a8c', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pv18s probe (HEX)', 'LOT-69d0776b', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pvs25 probe (FAM)', 'LOT-2ac6f866', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pv18s probe (FAM)', 'LOT-1682bd70', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Pvs25 probe (FAM)', 'LOT-f8f5b78a', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'ITS2B-mod2, 20.5nmol', 'LOT-612c38d1', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'PAR, 24.7nmol', 'LOT-6f6327f6', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'UN, 19.8nmol', 'LOT-a2b9fbab', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'Stq-F, 21.2nmol', 'LOT-87867db7', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'LESS, 22.8', 'LOT-612893ab', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'RIV,23.0 nmol', 'LOT-2478ec4b', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'UD2, 22.3nmol', 'LOT-10a874b7', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'VAN, 22.5nmol', 'LOT-6faf974a', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'FUN,19.9nmol', 'LOT-7a376806', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'ITS2A, 19.5nmol', 'LOT-80a007dc', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'ITS2-steph-R, 22.2nmol', 'LOT-a37c636d', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'U5.8S-F', 'LOT-84183b5e', 1.0, '', '2026-12-31'
        UNION ALL
SELECT '02Ga FW 100uM', 'LOT-4a7d5037', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT '63CT FW 100 uM', 'LOT-063dcf49', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT '02GA RV 100 uM', 'LOT-4b31d74c', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT '63CT RV 100 uM', 'LOT-ee781ff7', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT '76AG FW 100uM', 'LOT-1cd3ba9f', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT '76AG RV 100uM', 'LOT-0f8ca6a4', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'N1RPFMDR1034 100UM', 'LOT-371d0cfe', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'N2FPFMDR1034 100UM', 'LOT-1431f813', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'N1FPFMDR1034', 'LOT-fc2a2353', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'N2RPFMDR1034', 'LOT-d2c6b87b', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'N1FPFMDR86 100UM', 'LOT-fc2febad', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'N1RPFMDR86 100UM', 'LOT-2d90343e', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'N2FPFMDR86 100UM', 'LOT-a4a9809a', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'N2RPFMDR86 100UM', 'LOT-64f720a7', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'Ovis F 134.9 nMol (Sheep)', 'LOT-e5d2adbe', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'Can 2F 125 nMol', 'LOT-00e36ad6', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'ME (1) 143.5 nMol', 'LOT-d8fcb359', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'CAN 2R (2) 161.6 nMol', 'LOT-80ddfcf4', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'UN (1) 139.8 nMol', 'LOT-59be7b9a', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'Ovis 2R (2) 140.4 nMol (sheep)', 'LOT-7bba354c', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'Hmn F (2) 126.3 nMol', 'LOT-bf0ce286', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'AR (1) 130.4 nMol', 'LOT-1c90bfb6', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'Sus F 145.9 nMol', 'LOT-a8ba88fa', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'Bos 2F (2) 149nMol', 'LOT-188009ec', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'Bos 2R 122.2 nMol', 'LOT-e0465ba4', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'Hmn R (1) 145.5 nMol', 'LOT-8b854ab3', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'Sus R (2) 147.5 nMol', 'LOT-a6541d12', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'ITS 2A(2) 125.7nMol', 'LOT-113462a3', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'ITS2-Steph-R(2) 143.2 nMol', 'LOT-04989f92', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'GA 170.3 nMol', 'LOT-c3a76804', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'Hco2 198 (1) 113.7 nMol', 'LOT-8b6a7015', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'LCO 1490(2) 99.7 nMol', 'LOT-65e4bc38', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'S200x6.1-F 132.3 nMol', 'LOT-b3e20d50', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'Cap3R 126.8 nMol', 'LOT-71b6d8b1', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'plsR (revers primer)(1) 644.4 nMol', 'LOT-dd769736', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'S200X6.1-R(2) 157.2 nMol', 'LOT-ea0ec29a', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'PlsF(forward primer)(1) 738.6 nMol', 'LOT-ebe99481', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'Cap3F 136.8 nMol', 'LOT-c83623d7', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'QD(1) 134.9 nMol', 'LOT-51508dc0', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'N1RPFmdr86 41.9.nMol', 'LOT-77b10d32', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'N2RPVmdr1 76.4.nMol', 'LOT-6985c6e7', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'N1RPFcrt 41.1 nMol', 'LOT-38e49b2d', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'N1FPFmdr86 47.0.nMol', 'LOT-9465122e', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'N2RPFmdr1034 43.9 nMol', 'LOT-bc11c139', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'N2FPFmdr 1034 61.5 nMol', 'LOT-2e871829', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'N2 FPFmdr86 39.1nMol', 'LOT-20b3a5ea', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'N2FPFcrt 40.6nMol', 'LOT-77d4d597', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'N1RPVmdr1 77.7nMol', 'LOT-55cf56ed', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'N1FPVmdr1 75.5nMol', 'LOT-dbe39bf3', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'N1RPFmdr1034 61.2 nMol', 'LOT-d7497da4', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'N2FPVmdr1 60.9nMol', 'LOT-79f1350e', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'MDR3 (sense) for PFMDR1', 'LOT-daf5500f', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'MDR4 (anti-sense) for PFMDR1', 'LOT-17ebfd4f', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'MDR3 (sence) ', 'LOT-a7114bdc', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'PV-25 FW', 'LOT-9403d5b7', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'PV-25 RV', 'LOT-a92a3112', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'CCp4 FW', 'LOT-42439856', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'PF MGET ', 'LOT-cde7d415', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'CCp4RV', 'LOT-66905759', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'CCp4 RV', 'LOT-42dfa166', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'CCp4 FW', 'LOT-68463171', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'Pf MGET RV', 'LOT-599725e7', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'CCp4 probe ', 'LOT-50baf6a2', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'Pf MGET probe', 'LOT-95f6a235', 1.0, 'stock', '2026-12-31'
        UNION ALL
SELECT 'M1-RR primer 2.5 nMol', 'LOT-60616f49', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'M1-RF primer 2.5 nMol', 'LOT-86fddcce', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'MGET FW 50uM', 'LOT-57c8cc5d', 1.0, 'working', '2026-12-31'
        UNION ALL
SELECT 'MGET RV 50uM', 'LOT-0c38d0c3', 1.0, 'working', '2026-12-31'
        UNION ALL
SELECT 'CCp4 forward reverse 50uM', 'LOT-28d58319', 1.0, 'working', '2026-12-31'
        UNION ALL
SELECT 'CCp4 forward  50uM', 'LOT-35479a5c', 1.0, 'working', '2026-12-31'
        UNION ALL
SELECT 'CCp4 reverse 50uM', 'LOT-be21239d', 1.0, 'working', '2026-12-31'
        UNION ALL
SELECT 'HumTuBB RV 100mM', 'LOT-297456c3', 1.0, 'stock (100mM)', '2026-12-31'
        UNION ALL
SELECT 'HumTuBB FW 100mM', 'LOT-d79b156a', 1.0, 'stock (100mM)', '2026-12-31'
        UNION ALL
SELECT 'COX1 Plasmo F ', 'LOT-3410615a', 1.0, 'stock (100mM)', '2026-12-31'
        UNION ALL
SELECT 'COX1 Plasmo R', 'LOT-6c2a1bd2', 1.0, 'stock (100mM)', '2026-12-31'
        UNION ALL
SELECT 'Cow 121 F', 'LOT-1f67dabe', 1.0, 'stock (100mM)', '2026-12-31'
        UNION ALL
SELECT 'Dog 368 F', 'LOT-8b142365', 1.0, 'stock (100mM)', '2026-12-31'
        UNION ALL
SELECT 'Human 741 F', 'LOT-aca2a8ca', 1.0, 'stock (100mM)', '2026-12-31'
        UNION ALL
SELECT 'Pig 573 F', 'LOT-bccad73d', 1.0, 'stock (100mM)', '2026-12-31'
        UNION ALL
SELECT 'Got 894 F', 'LOT-34b8cf8b', 1.0, 'stock (100mM)', '2026-12-31'
        UNION ALL
SELECT 'UnRev1025', 'LOT-76826d51', 1.0, 'stock (100mM)', '2026-12-31'
        UNION ALL
SELECT '100 bp ladder (ready made)', 'LOT-495381aa', 7.0, '1.25', '2018-08-31'
        UNION ALL
SELECT 'Ultera low range DNAladder, 0.5ug/ul', 'LOT-4651b92b', 2.0, '', '2019-11-30'
        UNION ALL
SELECT 'Trakit cyan/yellow loading buffer, 6x', 'LOT-edebcf13', 4.0, '0.5', '2019-12-01'
        UNION ALL
SELECT 'Ultera low range DNAladder, 0.5ug/ul (used)', 'LOT-87809e26', 3.0, 'Used', '2019-11-30'
        UNION ALL
SELECT '100bp DNA ladder', 'LOT-c043d3bd', 9.0, '250.0', '2025-02-04'
        UNION ALL
SELECT 'blue/orange loading day 6X conc', 'LOT-674486a9', 8.0, '1.0', '2025-02-04'
        UNION ALL
SELECT 'gel loading dye purple 6X conc', 'LOT-110824e4', 1.0, '0.5', '2024-01-30'
        UNION ALL
SELECT 'gel loading dye Blue/orange 6X conc', 'LOT-3d63ecdb', 1.0, '1.0', '2025-07-03'
        UNION ALL
SELECT '100bp DNA lader with loadind dye(Blue/orange 6x)', 'LOT-04363944', 1.0, '250.0', '2026-09-13'
        UNION ALL
SELECT 'cyan/orange loading buffer', 'LOT-3bb0db56', 2.0, '0.2', '2026-12-31'
        UNION ALL
SELECT '50bp DNA ladder', 'LOT-ab1e94e6', 1.0, '50.0', '2026-12-31'
        UNION ALL
SELECT '100bp DNA ladder', 'LOT-aa00ce6e', 2.0, '', '2026-12-31'
        UNION ALL
SELECT '5x DNA loading buffer (blue)', 'LOT-0201efbe', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'blue/orange 6x oading dye', 'LOT-057c1424', 5.0, '1.0', '2017-09-24'
        UNION ALL
SELECT 'ultra low rabge DNA ladder', 'LOT-684b0ca5', 1.0, '50.0', '2026-12-31'
        UNION ALL
SELECT '1kb + DNA lader', 'LOT-ea535d41', 2.0, '250.0', '2026-12-31'
        UNION ALL
SELECT 'hyper ladder 100bp 100 lans', 'LOT-137e3258', 2.0, '', '2026-12-31'
        UNION ALL
SELECT 'primer IPCF -2.5 mM', 'LOT-897638fc', 2.0, '1.0', '2022-02-01'
        UNION ALL
SELECT 'primer west -26uM', 'LOT-9fd63833', 1.0, '1.0', '2022-02-01'
        UNION ALL
SELECT 'primer WT (west) 25uM', 'LOT-cde830d5', 1.0, '1.0', '2022-02-01'
        UNION ALL
SELECT 'primer WT (east) ', 'LOT-4e9b7213', 1.0, '1.0', '2022-02-01'
        UNION ALL
SELECT 'primer east ', 'LOT-62706f09', 1.0, '1.0', '2022-02-01'
        UNION ALL
SELECT 'primer ALT rev ', 'LOT-31de6aef', 2.0, '1.0', '2022-02-01'
        UNION ALL
SELECT 'An.gambiae RSP-ST', 'LOT-f82e493e', 1.0, '10.0', '2020-11-23'
        UNION ALL
SELECT 'An.coluzzii AKDR', 'LOT-29f2ceb1', 1.0, '10.0', '2020-11-24'
        UNION ALL
SELECT 'ITS2A (1) ', 'LOT-2e255675', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'ITS2-Steph-R(1) ', 'LOT-aa599116', 1.0, 'lyophilized', '2022-05-25'
        UNION ALL
SELECT 'QD', 'LOT-f403a68d', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'UN', 'LOT-3ba818ae', 2.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'GA', 'LOT-3982acb9', 2.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'AR', 'LOT-1d17eabd', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'BW', 'LOT-b58d1ecc', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'QDA', 'LOT-ad271adf', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'ME', 'LOT-8d2b0dfa', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'GA RV', 'LOT-0ef0f648', 1.0, 'stock (100mM)', '2026-12-31'
        UNION ALL
SELECT 'AR RV', 'LOT-54332d7a', 1.0, 'stock (100mM)', '2026-12-31'
        UNION ALL
SELECT 'QDA RV', 'LOT-80dfe8d1', 1.0, 'stock (100mM)', '2026-12-31'
        UNION ALL
SELECT 'ME RV', 'LOT-218b8b1c', 1.0, 'stock (100mM)', '2026-12-31'
        UNION ALL
SELECT 'QD RV', 'LOT-04e3fcc3', 1.0, 'stock (100mM)', '2026-12-31'
        UNION ALL
SELECT 'UN FW', 'LOT-8235eb27', 1.0, 'stock (100mM)', '2026-12-31'
        UNION ALL
SELECT 'IMP-S1', 'LOT-d011c0e3', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'QD-3T', 'LOT-10110c09', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'IMP-UN', 'LOT-3091cab2', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'IMP-M1', 'LOT-978f494c', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'AR-3T', 'LOT-95226f45', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'GA-3T', 'LOT-746eeac1', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'ME-3T', 'LOT-2a37f04f', 1.0, 'lyophilized', '2026-12-31'
        UNION ALL
SELECT 'An. arabiensis Dong 5 F211', 'LOT-2905fc72', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'An. merus, Maf F185', 'LOT-36e029f9', 1.0, '', '2026-12-31'
        UNION ALL
SELECT 'An. quadriannus, Sangwe, F180', 'LOT-5f1ca0e5', 1.0, '', '2026-12-31'
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
