# Integration-Test Isolation — Problem Landscape & Recommended Path

**Context:** Issue #3711 / PR #3712. develop's `01 - Backend` CI is red. The PR
migrates the ~427-class integration suite to per-test transactional rollback
isolation. ~406 classes are green; ~8 fail with a 20-second "lock timeout" (the
"lock-leak"). This document maps how the tests actually work, where the design
diverges from standard Spring practice, why that causes the lock timeout, and
the recommended path — which **removes** machinery rather than adding more.

---

## 1. How Spring integration tests are _supposed_ to work (the standard pattern)

In a normal Spring integration test, **one transaction per test**, and **every
data-access path shares that transaction's single connection**:

```
                      ┌─────────────────────────────────────────────┐
   @Transactional     │              ONE test transaction           │
   test method  ──────│  (Spring binds ONE Connection to the thread) │
                      └───────────────────┬─────────────────────────┘
                                          │  same Connection
            ┌──────────────┬──────────────┼──────────────┬───────────────┐
            ▼              ▼              ▼              ▼               ▼
       EntityManager   JdbcTemplate   @Repository   fixture load    services
       (JPA reads)     (raw SQL)      (Spring Data)  (in the tx)    under test
            └──────────────┴──────────────┴──────────────┴───────────────┘
                          all see each other's data; ALL roll back at test end
```

Key properties (this is just how Spring works — nothing custom needed):

- Spring stores the connection in a `ThreadLocal`. `EntityManager`,
  `JdbcTemplate`, and any `DataSourceUtils.getConnection(dataSource)` call all
  retrieve **that same connection**.
- Fixtures loaded inside the test transaction are **immediately visible** to
  every reader (JPA _and_ raw JDBC), because it's one connection.
- At the end of the test, Spring rolls the transaction back. Fixtures and test
  writes vanish together. **No manual cleanup, no TRUNCATE, no re-seed.**

This is the simple target. OE is not inherently more complex than this.

---

## 2. How OE's tests _actually_ work today

OE re-implements fixture handling by hand on top of the rollback base, and —
critically — **loads fixtures on a _different_ connection than the test
transaction.**

```
   @Transactional test  ──────────────►  Test transaction
                                          Connection  T  (autoCommit=false)
                                            ▲   ▲
              EntityManager / services ─────┘   └──── test's reads hold ACCESS SHARE
                                                       locks on reference tables

   @Before setUp():
     executeDataSetWithStateManagement(dataset):
        Connection L = DataSourceUtils.getConnection(dataSource)   ← SEPARATE connection!
        TRUNCATE ... CASCADE  (needs ACCESS EXCLUSIVE)  on L
        DBUnit REFRESH (insert rows)                     on L
        ensureAuditSystemUser()      ← yet another DataSourceUtils.getConnection
        statusService.refreshCache() ← reads status/dictionary/localization on T

   Tests also use:  jdbcTemplate = new JdbcTemplate(dataSource)   ← SEPARATE connection again
```

Extra machinery layered on (all of it custom, none of it standard):

- per-`@Before` `TRUNCATE ... CASCADE` + DBUnit `REFRESH` of each dataset
- `ensureAuditSystemUser` / `ensureReferenceTable` /
  `ensureSiteInformationPresent` seed restoration helpers
- `statusService.refreshCache()` inside the loader
- the "linchpin": `transactionManager.setDataSource(dataSource)` — an attempt to
  force `DataSourceUtils.getConnection` to return the test-tx connection
- (during this investigation) retry-with-savepoints, connection wrappers, etc.

---

## 3. Why the connection is NOT shared (the actual bug)

The "linchpin" is supposed to make `DataSourceUtils.getConnection(dataSource)`
return the test transaction's connection. It silently fails because of **lazy
connection acquisition**:

```
  test tx begins ──►  JpaTransactionManager opens an EntityManager
                      but Hibernate does NOT grab a JDBC connection yet (lazy).
                      → nothing is bound under the DataSource key.

  @Before runs ───►  DataSourceUtils.getConnection(dataSource)
                      → finds nothing bound → opens a BRAND-NEW connection L.

  later ─────────►  the EM finally runs a query → grabs connection T (a 3rd one).
```

Also: the DataSource is a raw, **unpooled** `DriverManagerDataSource` that is
not transaction-aware — so it never participates in the standard ThreadLocal
sharing that a normal Spring DataSource does.

**Net result:** the fixture loader (L), the test's JPA reads (T), and
`jdbcTemplate` (another connection) are all _different_ connections. This is the
single root defect; the lock timeout is a downstream symptom.

---

## 4. Why that produces the 20-second lock timeout

```
  Connection T (test tx):  holds ACCESS SHARE on  status_of_sample / localization /
                           observation_history_type
                           (taken by refreshCache() and the test's reads; held until the
                            test transaction ends, because it never commits)

  Connection L (loader):   TRUNCATE ... CASCADE needs ACCESS EXCLUSIVE on those same tables
                           (CASCADE widens the lock to FK-linked reference tables)

        L's TRUNCATE  ───X───►  blocked by T's ACCESS SHARE  ──►  waits lock_timeout (20s)  ──►  test fails
```

Proven during this investigation (connection-level instrumentation + a control
test):

- The loader connection and the test-tx connection are confirmed **different
  objects**, both held ~20s.
- **Removing `refreshCache()`** (the read that holds T's locks) made
  `BaseStorage` go green.
- **Loading on the test-tx connection** (via `Session.doWork`) made
  `TypeOfSample` go green — but **broke the 52 tests that read via
  `jdbcTemplate`** (those still use a separate connection and can't see the
  uncommitted fixture / block on its TRUNCATE lock).

That last point is the lesson: **fixing one connection isn't enough — they must
_all_ be one.** Which is exactly the standard pattern in §1.

---

## 5. Recommended path

The fix should move OE _toward_ the standard pattern in §1 and **delete
machinery**.

### Path A — Make all access share the test-transaction connection (recommended)

Restore the standard Spring behavior so `EntityManager`, `JdbcTemplate`, and the
fixture loader all use the one test-transaction connection.

- Wrap the test DataSource so `DataSourceUtils.getConnection` returns the test
  transaction's connection. Spring's `TransactionAwareDataSourceProxy` is
  **not** the right tool here — it exposes the connection bound by a
  `DataSourceTransactionManager`, whereas this suite uses a
  `JpaTransactionManager` (the connection lives on the JPA `EntityManager`, not
  under the DataSource key). Instead use a small **JPA-aware** proxy that reads
  the connection directly off the bound test `EntityManager` (forcing
  Hibernate's lazy acquisition) and hands it back close-suppressed.
- The fixtures then load **in the test transaction** (visible to EM _and_
  JdbcTemplate) and Spring rolls them back. The loader and `ensure*` helpers are
  already written against `DataSourceUtils.getConnection`, so they need no
  rewrite — only the EM's L1 cache must be cleared after the raw-JDBC load.

Result: one connection, one transaction, no deadlock, minimal new code. This is
the standard pattern and the biggest complexity reduction.

_Status (validated):_ implemented as `TransactionAwareTestDataSource` + a
one-line `entityManager.clear()`. The lazy-acquisition concern is resolved —
reading the connection off the EM forces acquisition, and Hibernate's default
`RELEASE_AFTER_TRANSACTION` holds it for the whole test. Confirmed green on the
EM path (`TypeOfSampleServiceTest`) and the jdbcTemplate path
(`BaseStorageTestValidationTest`) with zero lock timeouts.

### Path B — Keep fixtures committed; clear with a non-conflicting lock (smaller, safe)

If sharing the connection proves stubborn, keep today's model (fixtures
committed on a separate connection, visible to all — so the 52 `jdbcTemplate`
tests are untouched) but change the clear from `TRUNCATE` (ACCESS EXCLUSIVE) to
`DELETE` (ROW EXCLUSIVE). `DELETE` does not conflict with the test's ACCESS
SHARE read locks, so the deadlock disappears.

_Tradeoff:_ `DELETE` does not CASCADE to _undeclared_ child rows the way
`TRUNCATE CASCADE` does — needs FK-trigger-disabled deletion (helper already
exists) and care around orphaned children / secondary unique constraints.

### Path C — Question the per-test reload entirely (largest simplification)

Step back from "tear down + rebuild every dataset inside every test." A
committed seed + `@Transactional` rollback for only the test's _own_ writes is
the smallest possible base class and the most standard model. Biggest change,
potentially the simplest end state.

---

## 6. Recommendation

Pursue **Path A** (align with standard Spring connection-sharing, delete
machinery), with **Path B** as the safe fallback if the connection binding can't
be made reliable quickly. Either way the direction is _less_ custom code, not
more.

## Sources

- Spring Framework — Testing / Transaction Management (EM + JdbcTemplate share
  one connection per test transaction; auto-rollback).
- Spring `@Rollback`, `JpaTransactionManager`, `DelegatingDataSource` Javadocs;
  `HibernateJpaDialect.getJdbcConnection()` (documented to return null under
  lazy acquisition).
