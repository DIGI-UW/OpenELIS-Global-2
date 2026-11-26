# Pathology staining and coverslip checks

A pathology case standing at Staining cannot be sent on until each of its slides
in use is marked stained on the case view. A case standing at Coverslipping
cannot be sent on until each slide has passed the coverslip check. Each mark
keeps the name of the user and the server time.

Marks are taken only while the case is at Staining or Coverslipping. Once the
case is with the pathologist, it is returned to the bench to change a slide's
staining or coverslip check.

A slide is marked stained only once a stain is saved on it. Slides cut before
stains were recorded carry none, so on a case standing at Staining or
Coverslipping on upgrade day, pick and save each slide's stain first.

The stain is picked from the `Pathology Stains` dictionary. A laboratory whose
deployment keeps its own `dictionaries` configuration folder needs
`pathology-stains.csv` in it (or its own stains under Admin > Dictionary) before
a case can be sent on from Staining.

Where the laboratory tracks Staining, a slide's coverslip check is recorded only
after the slide is marked stained. A case standing at Coverslipping on upgrade
day therefore needs each slide marked stained first, then checked.

Slides already on file carry no staining record. On a case that is already with
the pathologist they read "Not recorded", because nobody noted who stained them.

A laboratory that does not track a stage switches it off under Result &
Validation Configuration. It is not asked to record that step.
