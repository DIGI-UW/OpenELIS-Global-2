If any guidance conflicts with the core SpecKit command, follow this section.

- Use `.specify/scripts/...` paths for scripts (not `scripts/...`).
- Use `.specify/memory/constitution.md` to ensure OpenELIS constraints are
  reflected in the generated spec.

**OpenELIS Constraints to Embed in Generated Specs**:

The generated spec MUST capture these non-negotiable project constraints as
assumptions or requirements (reference Constitution principles):

1. **UI Framework (Principle II)**: Carbon Design System v1.15+ exclusively. NO
   Bootstrap, Tailwind, or custom CSS frameworks.

2. **Internationalization (Principle VII)**: React Intl for ALL user-facing
   strings. NO hardcoded English text. Translations required for en + fr
   minimum.

3. **Architecture (Principle IV)**: 5-layer pattern is mandatory: Valueholder →
   DAO → Service → Controller → Form. Services own transactions (@Transactional
   in services ONLY, never controllers).

4. **Healthcare Interoperability (Principle III)**: FHIR R4 + IHE profiles for
   external-facing entities. Entities with external exposure need `fhir_uuid`.

5. **Schema Changes (Principle VI)**: Liquibase for ALL database changes. NO
   direct DDL/DML in production.

6. **Testing (Principle V)**: TDD workflow. JUnit 4 (NOT JUnit 5). Tests
   required for new features.

7. **Java Platform**: Java 21 LTS, Jakarta EE 9 (jakarta._, NOT javax._), Spring
   Framework 6.2.2 (Traditional MVC, NOT Spring Boot).

8. **Milestone-Based Delivery (Principle IX)**: Features >3 days effort must be
   broken into Validation Milestones (one PR per milestone).

When generating the spec, include an "Assumptions & Constraints" section that
references these architectural decisions so downstream planning respects them.
