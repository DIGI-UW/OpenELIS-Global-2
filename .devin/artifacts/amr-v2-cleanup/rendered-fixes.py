from pathlib import Path
import re
r=Path('/Users/pmanko/code/openelis-work/.worktrees/amr-v2-doc-cleanup')
m=r/'designs/microbiology'
p=m/'amr-micro-v2-preview.html';s=p.read_text()
s=s.replace('Visual Preview: Microbiology v2 amendments | Route: /Microbiology/cases/:caseId, /Microbiology/worklist?status=attention | OpenELIS Design Mockup | Not a live application','Visual Preview: Microbiology V2 | OpenELIS Design Mockup | Not a live application')
s=s.replace('from Admin › Plating templates; a suggestion, not a gate',"from the culture test’s media links; a suggestion, not a gate")
s=re.sub(r'&nbsp;existing component \(M-04, built\): reuse, do not re-implement\. Drawing abbreviated; v2 additions only as listed \(A-16\)','',s)
s=s.replace('existing component (Clinical Order Entry v4 Refer out, built): reuse, do not re-implement. v2 adds only what to refer','Shared referral workflow')
s=s.replace('Kept as built (A-16):','Susceptibility history:').replace('Use for reporting across attempts.','Use for reporting for each agent across attempts.')
s=s.replace("The built Reagents section of the test (test_reagent_link) also takes Microbiology medium items .",'The test catalog’s Reagents section also accepts culture media.')
s=s.replace('Default Diagnostic. Only Diagnostic counts in the antibiogram and GLASS.','Default Diagnostic. Purpose is retained for surveillance selection. Antibiogram and GLASS are future capabilities.')
s=s.replace('Bacterial track, so after final release the isolate goes to the WHONET and GLASS-AMR exports (Diagnostic purpose).','Bacterial track: eligible Diagnostic isolates can be selected for WHONET after final release. GLASS-AMR is future work.')
s=s.replace('TB track, so the case goes to the NTP and GLASS-TB exports.','TB track recorded for future NTP and GLASS-TB exports.')
s=s.replace('the reporting track decides the exports (WHONET and GLASS-AMR: Bacterial; NTP and GLASS-TB: TB).','the reporting track supports WHONET selection. GLASS and NTP exports remain future work.')
p.write_text(s)
p=m/'amr-micro-v2-mockup.jsx';s=p.read_text()
s=re.sub(r'// Routes:.*?(?=// SideNav:)', '// Views: clinical case, worklist, reception and shared administration.\n', s, flags=re.S)
s=re.sub(r'// Regions marked <ExistingFence>.*?\n\n','// Abbreviated shared workflow examples are functional mock content.\n\n',s,flags=re.S)
a=s.index('function ExistingFence(');b=s.index('\nfunction Section(',a)
s=s[:a]+'function ExistingFence({ children }) {\n  return <div>{children}</div>;\n}\n'+s[b:]
s=s.replace('Default Diagnostic. Only Diagnostic counts in the antibiogram and GLASS.','Default Diagnostic. Purpose is retained for surveillance selection. Antibiogram and GLASS are future capabilities.')
s=s.replace('Bacterial track: after final release the isolate goes to the WHONET and GLASS-AMR exports.','Bacterial track: eligible Diagnostic isolates can be selected for WHONET after final release. GLASS-AMR is future work.')
s=s.replace('TB track: the case goes to the NTP and GLASS-TB exports.','TB track recorded for future NTP and GLASS-TB exports.')
s=s.replace('the track decides the exports (WHONET and GLASS-AMR: Bacterial; NTP and GLASS-TB: TB).','the track supports WHONET selection. GLASS and NTP exports remain future work.')
p.write_text(s)
p=r/'mockup-viewer/src/App.jsx';s=p.read_text().replace("jira: ['OGC-1383', 'OGC-782']","jira: ['OGC-1383']");p.write_text(s)
for name in ['amr-micro-v2-preview.html','amr-micro-v2-mockup.jsx']:
 p=m/name
 dst=r/'mockup-viewer/public/designs/microbiology'/name
 if dst.exists():dst.write_bytes(p.read_bytes())
print('Clinical mock wording and gallery authority link corrected.')
