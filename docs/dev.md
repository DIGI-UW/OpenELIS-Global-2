# Developer reference

Use [Development setup](dev_setup.md) for the supported source workflow.
`scripts/dev-stack` owns interactive startup; the root Compose files are build
and test inputs, not a second installation path.

## Repository layout

- `src/` contains the Java application and its tests.
- `frontend/` contains the React application and its tests.
- `dev/` contains development configuration consumed by the launcher.
- `install/` contains runtime entrypoint, health-check and Tomcat resources used
  by the Dockerfile. It does not contain an installer.
- `docs/` contains engineering and operator documentation.
- `.github/workflows/publish-images.yml` publishes the tested image set and its
  deployment manifest through the existing release pipeline.

Lab installations use the [default distribution](install.md). Online and offline
packages consume the same published image set; neither builds the application on
the lab server. See [release delivery](../RELEASES.md) for publishing and
supported maintenance branches.

#

**Java Layers:**

##

**Controllers:**

Controllers are a spring mvc concept that tie the front end requests from the
client to the functionality. The are all singletons in the project, so avoid
class level variables unless they are completely threadsafe. It is discouraged
to place code that “does an action” in this layer. Code that should be placed
here includes:

- getting form data
- validating form data
- attaching error messages
- adding things to the request or session
- redirecting or forwarding requests, or retrieving a view
- calling services that execute an action

##

**Services:**

Services are middle components that relate to business logic. Calling a service
should begin a workflow, which is why `@Transactional` annotations are placed in
this layer. They usually come in pairs, interfaces and implementations. This
enables Spring to inject it’s own version of the class when services are
injected into other classes, which is what allows annotation’s functionality to
be properly called (such as `@Transactional`, `@Async`, etc.) By placing
workflows into this layer instead of controllers, it allows greater code reuse
to happen so multiple endpoints can execute the same or similar stories.

##

**DAOs:**

DAOs are back end components that retrieve objects from the database. Methods
here usually represent small grained operations like CRUD.

##

**Valueholders:**

Valueholders are usually simple bean objects that hold values, like the name
implies. These are most often objects that map to the database via the files
currently located at `/src/main/resources/hibernate/*.hbm.xml`. Validation
annotations are placed in the valueholders to validate client values as well as
values as they are entered into the db.

##

**Forms:**

Forms are simple bean objects that carry values from the client to the server.
Validation annotations are placed here to validate client values.

##

**Workers:**

Workers are like Services but are usually not threadsafe and are usually not
injected through autowiring

#

**Important Java Modules:**

##

**Config:**

Location: `org.openelisglobal.config`

Config classes have far reaching effects on the behaviour of the system and
modifications to these files should be triple checked to ensure functionality,
and security are not comprimised. Classes:

- `AnnotationWebAppInitializer` – this class connects spring to tomcat and
  starts all the servlets. Without it the application will not be reachable by
  the client.
- `AppConfig` – this is the main application configuration class. It configures
  the views, the locales, the message source, etc. If a configuration options
  doesn’t seem complex/large enough to have its own class, place it in here.
- `ControllerSetup` – this class configures controllers globally, like trimming
  form strings of whitespace.
- `DatabaseConfig` – this class configures the connection with the database
- `HibernateConfig` – configures the entity manager and transaction manager used
  by Hibernate.

##

**Liquibase:**

Location: `org.openelisglobal.liquibase` configures liquibase specifically. This
class can be moved into config

##

**Hibernate:**

Location: `org.openelisglobal.hibernate` Contains objects used in the hibernate
process such as the id generator, interceptors, and unique data types.

#

**Security Concepts:**

##

**Validation:**

Validation is a key aspect of application security. By validating user input we
drastically reduce the ability for an attacker to manipulate how our application
behaves, and also stops users from accidentally submitting bad data. Validation
should be as tight as possible without blocking functionality. The data type
that is the most vulnerable and therefore requires the most validation is the
String data type as it is very open ended. To minimize the risk of strings, try
one of these options (ranked by effectiveness):

1. Use a different data type if possible (Integer, Enum, etc.)
2. Use a whitelist of Strings (Only allow “VL”, “EID”)
3. Use a whitelist of characters (Only allow alphanumeric characters)
4. Use a blacklist of dangerous characters (Depends on context but common ones
   are ;”/&lt;)

##

**Escaping:**

Escaping is a process whereby you encode dangerous characters so that they
cannot be interpreted in the context they are being output to. For example,
`&lt;eviltag>evil&lt;/eviltag>` could be interpreted as xml if it were put into
an xml text context, so it should first be xml escaped so it cannot be
interpreted as xml. This would result in it becoming
`&lt;eviltag&gt;evil&lt;/eviltag&gt;`. Escaping needs to be context aware, as
special characters are different between different data types (ie xml vs html vs
javascript). Not only is the kind of data important, but often the element
within the context is important. For example, xml text is escaped differently
than xml attributes
`&lt;tag attribute="attributeEscapeMe">textEscapeMe&lt;/tag>` Escaping should be
used even with validation as another layer of security.

##

**CSRF:**

CSRF protection is configured in `SecurityConfig`. Currently it is enabled for
all pages that are accessed through the form login page, but is disabled for
pages that are accessed through Http Basic Auth.

##

**Passwords:**

Passwords are stored in the database as cryptographically hashed versions of
themselves, therefore there is no way to get a person’s original password from
the stored value.
