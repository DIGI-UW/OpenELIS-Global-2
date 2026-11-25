# Pathology slide label verification

Block and slide labels now encode the row's own barcode in the barcode symbol,
for example `<lab number>.A1` for a block and `<lab number>.A1.1` for a slide.
Before, the symbol held the bare lab number. The code text printed above the
symbol now reads the row barcode too, and its first segment is the lab number. A
block or slide label scanned into the Pathology dashboard search still opens its
case.

A new setting, `pathology.microtomy.scanVerificationRequired`, is on by default.
You find it under Result Entry Configuration. When it is on, each slide label
must be scanned against its block before the case can move on from Microtomy.
When it is off, for a laboratory without a scanner, the technician types the
block and slide identifiers instead and the same record is kept. An existing
deployment keeps whatever value it has already set.

Slides already on file read Unverified, because nobody scanned them. A case that
is at Microtomy on the day of the upgrade needs each of its slides confirmed
once before it can move on. A legacy slide with no block on record is exempt.
