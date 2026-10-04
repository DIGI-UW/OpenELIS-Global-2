# EQA authorization: two models, and why OGC-384 stepped back

## Status

OGC-384 (privilege-based RBAC) originally gated the EQA module at the service
layer. Those changes were reverted before the rebase onto develop. EQA
authorization is owned by the `qa.*` model introduced with EQA V2 (OGC-608/609).
This document records why, and what a later reconciliation would involve.

## The two models

|                | OGC-384                                      | EQA V2 (develop)                             |
| -------------- | -------------------------------------------- | -------------------------------------------- |
| Gate location  | Service interfaces (`@PreAuthorize`)         | REST controllers (`@PreAuthorize`)           |
| Authority form | `PRIV_EQA_VIEW`                              | `qa.view.eqa`                                |
| Source table   | `system_privilege` + `system_role_privilege` | `system_module` (QA permission prefix)       |
| Inherited CRUD | `CrudGate` + `@CrudPrivileges`               | not addressed                                |
| Tiers          | `eqa:view`, `eqa:manage`                     | view, manage, provider, participant, unblind |

Both mint their authorities in `CustomUserDetailsService.getGrantedAuthorities`,
from different tables.

## Why OGC-384 stepped back

Keeping both gates would stack them: a user holding `qa.view.eqa` but not
`PRIV_EQA_VIEW` passes the controller and is then denied at the service. Since
develop seeds no `system_privilege` rows, that is every EQA user. The service
gates had to go for the two models to coexist.

The bug OGC-384 found in the older EQA (any authenticated user could create a
programme through ungated inherited CRUD) was real and was verified live. EQA V2
fixes it independently at the controller layer, so reverting does not
reintroduce it.

EQA V2's model is also finer-grained: it separates provider from participant
from unblind, which two tiers cannot express. A later reconciliation must not
collapse those.

## For the later rework

`EqaModuleAccessTest.java.spec` in this directory is the reverted test. It
encodes which roles should read and write EQA, with inversions, including that
`eqa:view` alone must not permit a write. It is a starting specification, not
compilable against the current tree.

## Update after the develop merge: both gates kept, grants aligned

EQA is now gated at both layers: `EQAGuards` on the controllers (develop's five
tiers) and `@CrudPrivileges` plus per-method `PRIV_EQA_*` on the services.
`012-004t` grants `eqa:view` to every role holding `qa.view.eqa` and
`eqa:manage` to every role holding ANY EQA write tier, including Reception and
Results, which hold only the participant tier. That is deliberate: the
controller gate is evaluated first and keeps the tier precision (a participant
still cannot reach a MANAGE handler), so the service gate is a coarse backstop
that must not contradict it. Two service-layer privileges cannot express five
tiers; the reconciliation will either seed the missing tiers as privileges or
retire one of the two models.

The reverted `EqaModuleAccessTest` in this directory predates the merge; the
live one now pins the service half of the double gate.
