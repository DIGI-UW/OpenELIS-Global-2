# OE Developers’ Notes

#

**Project Organization:**

##

**Root Files:**

Folder: `/`

These files are placed here for ease of use, visibility, or proper project
structure

- `build.sh` - used to kick of the build process to make an installer that can
  be used remotely
- `docker-compose.yml` – used for local development options. In production, the
  docker-compose.yml file is generated from a file under
  `/install/installerTemplate/linux/templates/docker-compose.yml`
- `Dockerfile` – used to create the docker image through docker build, or
  `mvn dockerfile:build`
- `pom.xml` – used to control the maven build process, and update versions

##

**Source Code:**

Folder: `/src`

Code here is compiled and placed into the resulting .war that is deployed to
Tomcat

##

**Java code:**

Folder: `/src/main/java`

Follows a package pattern of `prefix.(s).module.layer`
(`org.openelisglobal.patient.controller`)

##

**Application resources:**

Folder: `/src/main/resources`

Resources used by the server to generate content, and control behaviour of the
application Files placed here will exist in the classpath of the project and can
be retrieved from the classloader

- hibernate
- persistence
- languages
- liquibase
- reports
- plugin
- tiles
- properties

##

**Web Resources:**

Folder: `/src/main/webapp`

Tomcat files such as `web.xml`, `server.xml`, and client resources such as jsps
(css, and js can be moved here, but are under `/src/main/resources` as that is
how I was able to get it to work)

##

**Dev Resources:**

Folder: `/dev`

Custom files to be used in development should be placed here All other files
used in the project (except for `/docker-compose.yml`), should be agnostic of
where it is being run. This means:

- Don’t commit modifications to context.xml to connect to the database
- Don’t hardcode property file locations to your machine

If you are running in docker, instead mount your file to where it needs to be in
the docker image in the docker-compose.yml ie. if OE reads in a properties file
at `/var/lib/openelis-global/somefile.properties` in docker, add the following
line under volumes under the openelis service
`./dev/properties/somefile.properties://var/lib/openelis-global/somefile.properties`

If you are running in eclipse, create a symlink that points to a file in your
dev folder. The sym link should be in the same location on your machine that it
would be in the docker container. ie. if OE reads in a properties file at
`/run/secrets/common.properties` in docker, make a symlink on your device at
`/run/secrets/common.properties -> /path/to/project/dev/properties/mycommon.properties`
Using this strategy will keep all your files in your workspace which will enable
you to edit them more easily. When the installer is run, it will actually create
files in the specified locations.

##

**Documentation:**

Folder: `/docs`

##

**Installation:**

Folder: `/install`

Only exception is `/build.sh` which is at the root of the project for
convenience and visibility when running... `sudo build.sh -ib develop` ...the
files under `/install/installerTemplate/{os_name}` are used as a template to
build the installer package for the target operating system (currently only
linux with no current plans to branch out)

###

**Installer Template:**

Folder: `/install/installerTemplate/linux`

Files here are used as is except `/install/installerTemplate/linux/templates/`
which are read into memory by `setup_OpenELIS.py` with variables replaced when
the setup script is ran. Variables in these files are written in the format
`[% variable_name %]` General structure:

- `setup.ini` - contains options that control the behaviour of
  `setup_OpenELIS.py`. This gets copied to `/etc/openelis-global/setup.ini` on
  install so it is preserved between upgrades
- `/scripts` – folder that contains scripts that are run on installation
- `/initDB` – folder that is ran to create the original database. Some files
  from /templates are placed here when setup is run.
- `/crosstab` – folder that installs crosstab into the db
- `/dockerImage` – folder that contains docker images to save on local bandwidth
  when installing For more information about how installation works, please read
  the code/comments in `setup.py`

##

**Tools:**

Folder: `/tools`

Various tools used in conjunction with OpenELIS Some of these are archives of
old process used (Liquibase-Outdated), while others are single use tools
(Password-Migrator) `OpenELIS_java_formatter.xml` is an eclipse code format
export file that can be imported back into eclipse so devs can use the same
format rules across devices

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
- `HibernateConfig` – configures the entitiy manager and transaction manager
  used by Hibernate.

##

**Liquibase:**

Location: `org.openelisglobal.liquibase` configures liquibase specifically. This
calss can be moved into config

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
