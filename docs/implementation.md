# OpenELIS Global - Implementation Notes

## Equipments needs

we are working on the deployment of an application using network configuration.
The main application is installed on a computer (server). several client
workstations will be able to access to this server via a network to be
configured

- _server minimal configuration_

  `Processor: Core Intel i5, i7+ recommended`;
  `RAM: 8 Gigabyte, 16+ recommended`;

  `Hard Disk: 1 Terabyte, 6+ recommended`; `External Hard Disk: 2TB`;

  `Ethernet Output available or provide wifi key. Physical connection recommended`;

  `UPS: 1000 /1500 VA`;

- _workstation minimal configuration_

  `Processor: Dual core`;

  `RAM: 4 Go`;

  `Hard Disk: 500 Go`;

  `External Hard Disk: 500 Go`;

  `Ethernet Output available or provide wifi key`;

  `UPS: 650 VA`;

  `Anything that can run Google Chrome, or Chromium`;

- _Network Configuration_

  `Wifi Router`;

  `Provide wifi card/key or ethernet cable per workstation`;

## Server and workstations Configurations

## Site Assessments

The initial assessment is an **IMPORTANT** step in the preparation for
implementation. It is good to have an idea of the existing in terms of
infrastructure, process, organization, existing documents, existing software.
The general best practice is to try to replicate the current workflow as much as
possible to make the addition of the new system as seamless as possible.

- _Site Overview_

  a short Description of the laboratory activity (EG: which lab units are
  present, how are they oprganized)

- _Workflows_

If possible, Attach:

    1. A map of the physical layout of the lab
    2. A description of the physical sample processing workflow
    3. Any sample forms used in the workflow
    4. A description of the QA/QC workflows
    5. A full test catalog
    6. A list of all analyzers used

The map and description will help to determine how many workstations you will
need, where they will be placed, and how many and which should have bar code
printers.

- _External Electronic Systems Interoperability_

Collect any information concerning existing software used at the health facility
for potential system interoprability (EG: An OpenMRS-based electronic medical
records system, sample storage system, etc)

- _Infrastructure Needs_

Short description of current and our proposed power failure backup plan,
stability of current power, etc. current internet connection and local area
network;

    1. Power Supply
    2. Internet Network (used to connect to the consolidated server, send and recieve refferals, etc)
    3. Local Area Network (used to connect the OpenELIS server with the workstations and analyzers)
    4. Equipment Needs and Placement (ensure that all equipment can be networked)
