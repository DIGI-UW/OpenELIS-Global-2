The OpenELIS Global software is an open enterprise-level laboratory information
system built on open source web-based technologies that has been tailored for
low-and-middle income country public health laboratories.

The software serves as both an effective laboratory software solution and
business process framework. It supports the effective functioning of public
health laboratories for best laboratory practice and accreditation. And such a
great task calls for great minds just like you. Find out more ways of
contributing to this noble cause
https://openelis-global.org/community/get-involved/

## How to Contribute

- **Pull request guidelines, checklist, and branch conventions:** see
  [PULL_REQUEST_TIPS.md](PULL_REQUEST_TIPS.md).
- **Project overview and dev setup:** see [README.md](README.md) and the
  detailed developer docs under [docs/](docs/).
- **Agent onboarding (AI contributors):** see [AGENTS.md](AGENTS.md) for the
  canonical agent guide; Claude Code-specific notes live in
  [CLAUDE.md](CLAUDE.md).
- **Governance and non-negotiables:** see
  [.specify/memory/constitution.md](.specify/memory/constitution.md) for the
  authoritative principles all changes must respect.
- **Feature specs and plans:** active feature work lives under [specs/](specs/)
  (numbered feature folders, plus `specs/plans/` for cross-cutting plans and
  `specs/roadmaps/` for strategic documents). Historical / retired plans are
  archived under [.specify/plan-archive/](.specify/plan-archive/).

For code of conduct expectations, see [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md).

For **security vulnerabilities**, see [SECURITY.md](SECURITY.md). Do not open
public GitHub issues for security reports.

## Backend Test Patterns

The backend has four test styles. Pick the lightest one that still covers the
behavior. **If the test does not query the database, it should not extend
`BaseWebContextSensitiveTest`.**

Longer MockMvc and JSONPath notes live in
[backend-testing-best-practices.md](.specify/guides/backend-testing-best-practices.md).

| Pattern | Base class | When to use |
| --- | --- | --- |
| Full DB integration | `extends BaseWebContextSensitiveTest` | Service, DAO, or controller tests that need a real DB |
| Security slice | `extends SecuritySliceMockMvcTest` | Controller tests that verify auth/role enforcement |
| Standalone MockMvc | `@RunWith(MockitoJUnitRunner.class)` + `standaloneSetup` | Controller tests that verify response shape without DB |
| Plain JUnit | No runner | Pure logic, no Spring, no DB |

### Full DB integration

Use this when the test must hit PostgreSQL. `RoleServiceTest` is the pattern:

```java
public class RoleServiceTest extends BaseWebContextSensitiveTest {
    @Autowired
    RoleService roleService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/role.xml");
    }
}
```

### Security slice

Use `SecuritySliceMockMvcTest` when the test is about auth or roles, not
business data. `AnalyzerIngressSecurityTest` is the pattern:

```java
public class AnalyzerIngressSecurityTest extends SecuritySliceMockMvcTest {
    @Test
    public void missingCredentialsFailClosed() throws Exception {
        mockMvc.perform(post("/rest/analyzer/events/ast"))
                .andExpect(status().isUnauthorized());
    }
}
```

### Standalone MockMvc

Use Mockito plus `MockMvcBuilders.standaloneSetup` for controller HTTP shape
with no Spring context and no DB. `QCRestControllerTest` is the pattern:

```java
@RunWith(MockitoJUnitRunner.class)
public class QCRestControllerTest {
    @Mock
    private QCControlLotService controlLotService;

    @InjectMocks
    private QCRestController controller;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }
}
```

### Plain JUnit

Use a plain class with `@Test` methods for pure logic. `StringUtilTest` is the
pattern:

```java
public class StringUtilTest {
    @Test
    public void isNullorNill_shouldReturnTrueForNull() {
        assertTrue(StringUtil.isNullorNill(null));
    }
}
```
