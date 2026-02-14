# Analyzer Interfaces in OpenELIS Global

## Background

We support two models of importing analyzer results. The older model, which we
are phasing out, has the code for importing analyzer results as part of the core
of OpenELIS. The newer model which all new analyzers should use is a plugin
model.

You can find a set of precompiled analyzer plug-ins in our
[installer folder here](https://www.dropbox.com/sh/xzjxca5ya73dylt/AACf35yDhjerS1V-C1SVNgJ2a?dl=0)

## Basics

There is a directory /var/lib/openelis-global/plugins/ in OpenElis. Any jar file
in this directory will be processed and if it is a valid analyzer plugin it will
be added to the application. If the plugin is later removed any imported data
will be retained but the analyzer will no longer appear in the menu and openELIS
will not accept analyzer files meant for that analyzer.

The plugins are loaded during the application startup. A future feature will be
to enable/disable them during runtime.

## Working with plugins

Plugins can be developed completely independently of OpenELIS or they can be
checked into `https://github.com/openelisglobal/openelisglobal-plugins`\_.
Currently we are only supporting plugins for analyzers but we expect to also
support plugins for electronic and paper reports. The analyzer plugins should be
added under "analyzer". There is an example called WeberAnalyzer which can be
used as a template. Note that it also includes a **SMALL** input file sample as
well as contact information.

## Good manners

If you want to modify an existing analyzer written by a different organization
please contact the creator of the analyzer and work with them to make sure that
your changes do not break their imports. You should also include the core group
with confirmation that you have the right to modify the work from a different
organization or else we will not allow the change to be made in repository. An
alternative is to make a copy of the other persons work. Note that you should
honor the copyright notice and name yourself as a contributor, not as the
creator.

If you no longer are using a particular analyzer and do not care if the work is
modified please note that in the contact file.

## Working with git

The plugins are in a separate repository from the core but the IDE is only
tracking the core repository. When you do a commit from within the IDE you are
only committing what is in openelisglobal-core, not anything from
openelisglobal-plugin. To maintain that repository you will need to either work
from the command line or use a tool such as `SourceTree`\_.

## For all IDE's

The JDK that is being used to compile the plugin **MUST** be as old or older
than the Java version under which tomcat is running. i.e. If tomcat is using JRE
1.7 and the plugin was compiled with JDK 1.8 then bad things will happen

## Setting up for Eclipse

Creation Steps

1. Create a new Java project. Accept all of the defaults. The name of the
   project should match the name of the analyzer.
2. Open up the properties for the project
3. Select the Java build path (left hand side of the dialog)
4. Select projects tab and add (right hand side of the dialog) openElisGlobal 1

.. \_`https://github.com/openelisglobal/openelisglobal-plugins`:
https://github.com/openelisglobal/openelisglobal-plugins .. \_SourceTree:
http://www.sourcetreeapp.com/

4. login to OpenELIS -> Results -> from Analyzer -> Analyzer name.

# How are analyzer results imported to OpenELIS?

The model being used is that the analyzer will write a results file to the file
system and a perl script will monitor the directory where the results files are
written. That directory is referred to as the staging directory. When a new file
is detected it is copied to another directory called the transmissionQueue. The
script will then send the file to a service in OpenELIS and as soon as it is
successfully sent it will be deleted from the transmissionQueue.

Some points to keep in mind:

1. The file is copied from the staging directory to the transmissionQueue.
   Management of the staging directory is not part of the model

1. The script depends on the timestamp of the files in the staging directory to
   insure that they are not moved to the transmissionQueue more than once. There
   are some strangely named files in the transmissionQueue that track the
   timestamp, do not delete them or all of the files from the staging directory
   will be resent.

1. The file in the transmissionQueue folder is deleted after successful
   transmission.

1. During development it is easiest to copy (not move) the test file to the
   transmissionQueue directory rather than updating the timestamp and adding it
   to the staging directory.

The perl file is in
"openelisglobal-core\tools\AnalyzerSideDataImport\FlatFileTransport.pl". Note
that it exist to type of export file , one for windows environment and another
for linux. the main difference is how we mention the folder path in the perl
script.

Depending on where perl is installed on your system the script will be invoked
with something similar to: `c:\Perl\bin\perl.exe FlatFileTransport.pl`

There are four configurable values in the file:

- _The location of curl_  
  Curl is the application that actually sends the file to OpenELIS. In the perl
  script it is assumed that it is installed in the root of the C drive.
  `my $curlExe = 'C:\curl\curl.exe';` If it differs on the host machine this
  should be edited

- _The target application_  
   In the following case the application is haitiOpenElis on the local host. `my $upLoadtargetURL = 'https://localhost:8443/[openelisURL]/importAnalyzer';`

- _The user name and password._  
   This user name and password must have been added as a user to the application.
  The user should be added with the results entry roles. `my $upLoadUserName = ''; my $upLoadPassword = '';`

- _The location of the staging directory._  
   The directory to which the analyzer is writing it's output files. In the perl
  script is an example of a single script supporting two analyzers but one of them
  has been commented out. In this example the name of the directory is a directory
  relative to the perl script named staging `my $stagingDir1 = ".\\staging";` --
  for windows

`my $stagingDir1 = ".//staging";` -- for linux

    * In this example the directory has been mapped from one machine to the machine the perl script is on to a drive named Y

'my $stagingDir2 = "Y:";`

## Troubleshooting

Perl and curl give good error messages so most problems in the script will be
obvious. However sometimes the server returns errors which are not as obvious.

If you get an HTML response it is one of two things.

1.  A response from Tomcat that it could not find the resources. This is usually
    about three lines of HTML and buried in it is the missing resource message.
    That indicates that the url is not correct
1.  A response from openELIS than an exception has been thrown. This is the HTML
    for the gray screen of death and is usually dozens of lines wrong. This
    indicates that there was an error in the code and you should look for the
    stack trace in the console or in the logs

Another common message is that the user name or password was not correct.
Revisit the section above about setting them correctly
