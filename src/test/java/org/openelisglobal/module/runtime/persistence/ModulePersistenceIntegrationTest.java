package org.openelisglobal.module.runtime.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import javax.sql.DataSource;
import org.hibernate.cfg.AvailableSettings;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.openelisglobal.IsolatedDatabase;
import org.openelisglobal.module.api.ModuleState;
import org.openelisglobal.module.runtime.ContractVersions;
import org.openelisglobal.module.runtime.ModuleDiscovery;
import org.openelisglobal.module.runtime.ModuleRecord;
import org.openelisglobal.module.runtime.ModuleRegistry;
import org.openelisglobal.module.runtime.ModuleStartupException;
import org.openelisglobal.module.runtime.ModuleSystemProperties;
import org.openelisglobal.module.testkit.ModuleJarBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Against a real PostgreSQL: a fixture module's changelog runs through the
 * module's class loader with its changeset ids checked, {@code module_state}
 * records the install, the module's entity joins a persistence unit through the
 * registrar and {@code hibernate.classLoaders}, and a downgrade is refused.
 */
@IsolatedDatabase
public class ModulePersistenceIntegrationTest {

    private static PostgreSQLContainer<?> postgres;
    private static DataSource dataSource;

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @BeforeClass
    public static void startDatabase() {
        postgres = new PostgreSQLContainer<>("postgres:14.4").withDatabaseName("clinlims").withUsername("clinlims")
                .withPassword("clinlims");
        postgres.start();
        dataSource = new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("create schema if not exists clinlims");
        jdbc.execute("create table clinlims.module_state (id varchar(64) primary key, version varchar(64),"
                + " status varchar(32) not null, core_version varchar(64), installed_at timestamp not null default now(),"
                + " last_started_at timestamp, last_error varchar(1000))");
    }

    @AfterClass
    public static void stopDatabase() {
        if (postgres != null) {
            postgres.stop();
        }
    }

    private static final String PKG = "org.openelisglobal.module.persist";

    private static String manifest(String version) {
        return "{\"schemaVersion\":1,\"id\":\"persist\",\"kind\":\"module\",\"name\":\"Persist\",\"version\":\""
                + version + "\",\"requires\":{\"openelis\":\">=3.4.0\"},\"backend\":{\"basePackages\":[\"" + PKG
                + "\"],\"entities\":[\"" + PKG + ".Note\"],\"liquibase\":\"liquibase/persist/changelog.xml\"}}";
    }

    private static String changelog(String author) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<databaseChangeLog xmlns=\"http://www.liquibase.org/xml/ns/dbchangelog\""
                + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
                + " xsi:schemaLocation=\"http://www.liquibase.org/xml/ns/dbchangelog"
                + " http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.8.xsd\">\n"
                + " <changeSet id=\"persist-001-note\" author=\"" + author + "\">\n"
                + "  <createSequence sequenceName=\"mod_persist_note_seq\" schemaName=\"clinlims\"/>\n"
                + "  <createTable schemaName=\"clinlims\" tableName=\"mod_persist_note\">\n"
                + "   <column name=\"id\" type=\"BIGINT\"><constraints primaryKey=\"true\" nullable=\"false\"/></column>\n"
                + "   <column name=\"text\" type=\"VARCHAR(200)\"/>\n"
                + "   <column name=\"created_by_user_id\" type=\"INTEGER\"/>\n"
                + "   <column name=\"created_at\" type=\"TIMESTAMP\"><constraints nullable=\"false\"/></column>\n"
                + "   <column name=\"sys_user_id\" type=\"INTEGER\"/>\n"
                + "   <column name=\"lastupdated\" type=\"TIMESTAMP\"><constraints nullable=\"false\"/></column>\n"
                + "  </createTable>\n  <rollback><dropTable schemaName=\"clinlims\" tableName=\"mod_persist_note\"/>"
                + "<dropSequence schemaName=\"clinlims\" sequenceName=\"mod_persist_note_seq\"/></rollback>\n"
                + " </changeSet>\n</databaseChangeLog>\n";
    }

    private static final String ENTITY = "package " + PKG + ";\n"
            + "@jakarta.persistence.Entity @jakarta.persistence.Table(name = \"mod_persist_note\", schema = \"clinlims\")\n"
            + "public class Note extends org.openelisglobal.module.api.ModuleAuditedEntity {\n"
            + "  @jakarta.persistence.Id @jakarta.persistence.GeneratedValue(strategy = jakarta.persistence.GenerationType.SEQUENCE, generator = \"noteSeq\")\n"
            + "  @jakarta.persistence.SequenceGenerator(name = \"noteSeq\", sequenceName = \"mod_persist_note_seq\", schema = \"clinlims\", allocationSize = 1)\n"
            + "  private Long id;\n" + "  @jakarta.persistence.Column(name = \"text\") private String text;\n"
            + "  public Long getId() { return id; }\n" + "  public String getText() { return text; }\n"
            + "  public void setText(String text) { this.text = text; }\n}\n";

    private ModuleRegistry registryFor(Path directory, String version, String author) {
        ModuleJarBuilder.module().manifest(manifest(version)).source(PKG + ".Note", ENTITY)
                .resource("liquibase/persist/changelog.xml", changelog(author))
                .build(directory.resolve("persist-" + version + ".jar"));
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty(ModuleSystemProperties.DIRECTORY, directory.toString());
        environment.setProperty(ModuleSystemProperties.CACHE_DIRECTORY, directory.resolve("cache").toString());
        ModuleSystemProperties properties = new ModuleSystemProperties(environment);
        ContractVersions versions = new ContractVersions("3.4.0", "1.0.0", "1.0.0");
        List<ModuleRecord> records = new ModuleDiscovery(properties, versions, getClass().getClassLoader()).discover();
        assertEquals(records.get(0).reason().orElse(""), ModuleState.VALIDATED, records.get(0).state());
        return new ModuleRegistry(properties, versions, records);
    }

    @Test
    public void changelogRunsEntityMapsAndStateIsRecorded() throws Exception {
        ModuleRegistry registry = registryFor(temp.newFolder("m1").toPath(), "1.0.0", "persist");

        new ModuleLiquibaseRunner(registry, dataSource, "default").run();

        ModuleRecord module = registry.find("persist").orElseThrow();
        assertEquals(ModuleState.MIGRATED, module.state());
        assertTrue(module.isFirstInstall());
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        assertEquals(Integer.valueOf(1), jdbc.queryForObject(
                "select count(*) from clinlims.databasechangelog where id = 'persist-001-note' and author = 'persist'",
                Integer.class));
        assertEquals("MIGRATED",
                jdbc.queryForObject("select status from clinlims.module_state where id = 'persist'", String.class));

        ModuleEntityRegistrar registrar = new ModuleEntityRegistrar(registry);
        assertEquals(List.of(PKG + ".Note"), registrar.entityClassNames());
        EntityManagerFactory emf = entityManagerFactory(registrar);
        try {
            EntityManager em = emf.createEntityManager();
            Class<?> noteType = Class.forName(PKG + ".Note", true, module.classLoader());
            Object note = noteType.getConstructor().newInstance();
            noteType.getMethod("setText", String.class).invoke(note, "written through the module entity");
            em.getTransaction().begin();
            em.persist(note);
            em.getTransaction().commit();
            em.close();
            assertEquals("written through the module entity",
                    jdbc.queryForObject("select text from clinlims.mod_persist_note", String.class));
            assertTrue(jdbc.queryForObject("select lastupdated is not null from clinlims.mod_persist_note",
                    Boolean.class));
        } finally {
            emf.close();
        }

        ModuleRegistry again = registryFor(temp.newFolder("m1-again").toPath(), "1.0.0", "persist");
        new ModuleLiquibaseRunner(again, dataSource, "default").run();
        assertEquals("1.0.0", again.find("persist").orElseThrow().recordedVersion());
        assertTrue(!again.find("persist").orElseThrow().isFirstInstall());

        ModuleRegistry older = registryFor(temp.newFolder("m0").toPath(), "0.9.0", "persist");
        try {
            new ModuleLiquibaseRunner(older, dataSource, "default").run();
            fail("a downgrade must be refused");
        } catch (ModuleStartupException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("allowDowngrade"));
        }
        assertEquals(ModuleState.FAILED, older.find("persist").orElseThrow().state());
    }

    @Test
    public void changelogWithAForeignAuthorIsRefusedBeforeRunning() throws Exception {
        ModuleRegistry registry = registryFor(temp.newFolder("m2").toPath(), "2.0.0", "somebody-else");
        try {
            new ModuleLiquibaseRunner(registry, dataSource, "default").run();
            fail("expected refusal");
        } catch (ModuleStartupException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("author"));
        }
        assertEquals(ModuleState.FAILED, registry.find("persist").orElseThrow().state());
    }

    private static EntityManagerFactory entityManagerFactory(ModuleEntityRegistrar registrar) {
        LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(dataSource);
        factory.setPackagesToScan("org.openelisglobal.module.testkit");
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setPersistenceUnitPostProcessors(registrar);
        Properties properties = new Properties();
        properties.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        properties.setProperty("hibernate.hbm2ddl.auto", "none");
        factory.setJpaProperties(properties);
        factory.getJpaPropertyMap().put(AvailableSettings.CLASSLOADERS, registrar.classLoaders());
        factory.afterPropertiesSet();
        return factory.getObject();
    }
}
