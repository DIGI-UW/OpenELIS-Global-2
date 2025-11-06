# Setting up ASTM bi-directional interfaces

## List of Components:

- OpenELIS - the LIS, must be version 2.7.0.5+
- Analyzer plugin jar - an extension to OE that processes messages coming from
  analyzers
- ASTM-HTTP bridge - a service that receives messages via HTTP and forwards via
  ASTM protocol and vice versa. Middleware component that allows OpenELIS/plugin
  jar to communicate to and from the analyzer software.
- Analyzer workstation (Aquios in this case)

## Setting up

### Setting up the services

- Ensure an appropriate version of OE is installed in the environment
- Place the jar for the analyzer in /var/lib/openelis-global/plugins/ and
  restart OE with a `sudo docker-compose down` followed by a
  `sudo docker-compose up -d`
  - Only jar that uses ASTM is currently the Aquios
    [here](https://www.dropbox.com/s/zcfhj56yvjhys4z/Aquios-0.0.2.jar?dl=0)
  - The project of all code for plugins that compile to jars is
    [here](https://github.com/openelisglobal/openelisglobal-plugins)
  - To compile a jar, you will need to run maven clean install in the project
    directory (will require a jar version of OE to be specified in the pom.xml)
- Create a config file for the ASTM-HTTP bridge
  - An example is given in the
    [astm-http-bridge github](https://github.com/CalebSLane/astm-http-bridge/wiki/Configuring-the-project).
    Details about what configuration options do are also included.
- Create a docker-compose file (or add to OE’s docker-compose)
  - An example is given in the
    [astm-http-bridge github](https://github.com/CalebSLane/astm-http-bridge/wiki/Configuring-the-project).
    Details about parameters are also included.
  - Run `sudo docker-compose up -d` wherever this service was added

### Configuring OE

- Ensure that the test you are trying to run in the analyzer is defined in OE
- Ensure the test has a loinc code that maps to the test that the analyzer runs
  (the test that was for testing was CD3 percentage, which was given a LOINC of
  8122-4 and is called CD3PCT by the analyzer. This mapping between 8122-4 and
  CD3PCT is accomplished in the plugin jar (java class
  AquiosAnalyzerImplementation.java)

### Executing the Workflow (the workflow stated here is for getting a result from the Aquios Analyzer)

1. Create an order in OE that has the same sample ID as the analyzer and
   contains the same test
2. On the Aquios workstation, navigate to the Review page, find the sample that
   is finished, click the pair of glasses near the top right, and select
   Reviewed → Send to LIS
   - Sometimes it took the analyzer a few seconds before it added the
     sample/results to the queue to return to the LIS and it gave no indication
     of when it completed.
3. Navigate to the analyzer page (Results > Analyzer > Aquios in this case)
4. Near the top of the page is a dropdown that selects which “LIS action” is to
   be executed (“Poll for Result” in this case) when the Run Action button is
   pressed
   - This list of actions is populated from the plugin jar and represents which
     workflows have been implemented. (Unfortunately, there is no capability
     right now for other fields to appear for more complex workflows like
     exporting the order to the analyzer, but that should be added here and
     appear when the workflow is selected)
   - Some workflows will run in the background and may take some time to
     execute. The poll result action with the VM took around 30 seconds to
     complete and see a result returned.
5. Refresh the analyzer page in OE to see the newly imported result
   - If it doesn’t appear after 30 seconds, then something has likely gone wrong
     and configuration/logs should be checked
