[OGC-41] Westgard Rules Dashboard for Analyzers Created: 24/Mar/25  Updated: 13/Nov/25 

Status:	Community To Do
Project:	OpenELIS Global Community

Components:	None 
Affects versions:	None 
Fix versions:	None 
Security Level:	Open to the Community 

Type: 	Story 	Priority: 	Medium 
Reporter: 	Casey Iiams-Hauser 
Assignee: 	Piotr Mankowski 

Resolution: 	Unresolved 	Votes: 	0 
Labels: 	None 
Remaining Estimate:	Not Specified 
Time Spent:	Not Specified 
Original estimate:	Not Specified 

Attachments: 	 westgard_rules_implementation.md     


 Description  	 
User story
•	As a laboratory technician, when I check the compliance status of instruments, I want to see a clear overview so that I can quickly assess any issues.
•	As a lab manager, when rules are violated, I want to receive automated alerts so that I can take immediate corrective actions.
Context
This issue focuses on designing a dashboard for instrument compliance. The dashboard will provide an overview of compliance status, instrument-specific details, control charts, historical data, alerts, and corrective actions.
Acceptance criteria
•	Overview section displays compliance status with visual indicators: 
o	Green for compliant
o	Yellow for warning
o	Red for out of compliance
•	Each instrument has a card with: 
o	Current compliance status
o	List of triggered rules
o	Date and time of the latest data point analyzed
•	Clickable feature for detailed views, including control charts.
•	Interactive Levey-Jennings charts integrated for each instrument.
•	Westgard Rule limits overlaid on control charts.
•	Points violating rules are highlighted.
•	Trend graphs show compliance over time with filtering options.
•	Automated alerts sent via email or system notifications for rule violations.
•	Real-time feed for ongoing compliance issues.
•	Log for corrective actions taken and task assignments for follow-up.
Other information
•	Use color-coded visuals for clarity.
•	Responsive design for various devices.
•	Customizable thresholds and rule sets.
•	Search or filter bar for quick access to instruments or violations.
•	Dashboard linked to the lab information system (LIS) for live data feeds.
•	Data synchronized from instruments at regular intervals via ASTM interface or flat file import.
•	Sensitive data protected with encryption and secure user authentication.
•	System designed for scalability to support additional instruments or rules.
Mockup: https://kit-mango-37758223.figma.site 


 Comments  	 
Comment by Casey Iiams-Hauser [ 24/Mar/25 ] 

The Westgard Rules are a set of statistical quality control guidelines used in laboratories to monitor the accuracy and precision of analytical systems. These rules help determine whether a test run is "in control" or "out of control" by analyzing patterns in control measurements. They are commonly applied using Levey-Jennings charts, which plot control data against standard deviation limits2.
Some key Westgard Rules include:
•	1 2s Rule: A warning rule triggered when one measurement exceeds 2 standard deviations from the mean.
•	1 3s Rule: Rejects a run if one measurement exceeds 3 standard deviations from the mean.
•	2 2s Rule: Rejects a run if two consecutive measurements exceed 2 standard deviations on the same side of the mean.
•	R 4s Rule: Rejects a run if one measurement exceeds +2 standard deviations and another exceeds -2 standard deviations within the same run.
•	4 1s Rule: Rejects a run if four consecutive measurements exceed 1 standard deviation on the same side of the mean.
•	10 x Rule: Rejects a run if ten consecutive measurements fall on one side of the mean3.
These rules are designed to minimize false alarms while maximizing error detection, ensuring reliable laboratory results. They are particularly useful in clinical chemistry, hematology, and immunoassays2.
Comment by Casey Iiams-Hauser [ 24/Mar/25 ] 

 
Dashboard Layout and Visual Elements
1. Overview Section
This section provides a summary of compliance status for all instruments:
•	Visual Representation: A grid or table with color-coded status indicators: 
o	Green for compliance.
o	Yellow for warning (e.g., Rule 1 2s triggered).
o	Red for non-compliance (e.g., Rule 1 3s violated).
•	Mockup:
Imagine rows labeled with instrument names, accompanied by compliance status icons (✔️, ⚠️, ❌), percentages of compliance, and timestamps for the latest data.
2. Instrument-Specific Details
Each instrument has its own card or section:
•	Visual Representation: Cards with information such as: 
o	Current compliance status.
o	List of triggered rules (e.g., "R 4s violated on [date/time]").
o	Data visualization icons for further exploration.
•	Mockup:
A sleek card for each instrument, showing the last five tests and highlighting rules that were breached.
3. Control Charts
Interactive Levey-Jennings charts for each instrument:
•	Visual Representation: 
o	Plot control data with standard deviation limits clearly marked.
o	Highlight data points that violate Westgard Rules (e.g., red points for violations).
•	Mockup:
Visualizing a graph with an “overlaid” zone for ±1s, ±2s, and ±3s, with flagged points that exceed the limits.
4. Historical Data & Trends
Graphical insights into compliance over time:
•	Visual Representation: 
o	Trend graphs to identify recurring issues and compliance improvement over months.
o	Filtering tools for date ranges or specific instruments.
•	Mockup:
Imagine a line graph labeled with the percentage of runs in compliance, annotated with significant rule violations.
5. Alerts & Notifications
This section flags rule violations:
•	Visual Representation: 
o	A scrolling feed with real-time updates for non-compliance cases.
o	Notifications panel for lab managers.
•	Mockup:
Notifications could appear as cards labeled “Rule 1 3s triggered - Instrument X,” dated and prioritized.
6. Corrective Actions Log
Tracking corrective actions taken:
•	Visual Representation: 
o	A log with timestamps, actions performed (e.g., recalibration), and who handled the task.
•	Mockup:
A list view with sortable columns for date, instrument, corrective action, and status.
 
here's a textual mockup of one of the sections:
 
Instrument Compliance Overview 
Instrument Name	Status	Compliance %	Last Checked	Action Required
Analyzer A	🟢 Compliant	98%	March 24, 2025	No action
Analyzer B	🟡 Warning	85%	March 23, 2025	Investigate rule violations
Analyzer C	🔴 Out of compliance	65%	March 22, 2025	Immediate calibration needed
 
Comment by Casey Iiams-Hauser [ 06/Aug/25 ] 

Mockup is here for the UI: https://urban-poster-21038766.figma.site 

Comment by Ian Bacher [ 07/Nov/25 ] 

I don’t think the actual UI code gets us very far, but I do think having the design mock artifacts is fantastic! Probably after this current push we should see if we can integrate IBM’s Figma for Carbon kit with our Figma setup… That’s more likely to get us to an easily usable artifact.
Comment by Male Samuel [ 13/Nov/25 ] 

A few questions regarding the overall scope of work for this feature:
1.	Analyzer Coverage:
Is this feature intended to work exclusively with ASTM-enabled analyzers, or should we design it to support other communication protocols as well (e.g., HL7, CSV imports, or direct OpenELIS data entry)?
2.	Analyzer Configuration UI:
Do we already have a UI for configuring or mapping ASTM-enabled analyzers, or would part of this work involve creating a configuration panel for instrument setup and QC data mapping?
3.	Backend Components:
While I see that we already have plugins for instrument communication, do we currently have backend components for: 
o	A Westgard rule evaluation service or basic rule engine
o	QC-related database models
o	An alerting or notification service for rule violations
Or should these be developed as part of this story’s implementation?
4.	Rule Definition and Management:
How do we plan to define and maintain the Westgard rules?
Should rules be configurable through a UI (for admins to add or modify), or are we expecting them to be defined via configuration files?
5.	Data Flow:
Are QC results expected to be synchronized in real time from analyzers, or will there be scheduled batch imports?
Comment by Casey Iiams-Hauser [ 13/Nov/25 ] 

westgard_rules_implementation.md 
Here are some additional details. 
Comment by Casey Iiams-Hauser [ 13/Nov/25 ] 

Male Samuel I’ve added some more details above, which detail out the rules for the rule builder, we do indeed already have a mechanism to connect to the analyzers to OpenELIS, and there is a different story to enable the mapping. This would be to build out the rules to run the checks against the QC data, and then show the alerts, and log corrective actions. 
Generated at Fri Nov 14 18:02:02 UTC 2025 by Piotr Mankowski using Jira 1001.0.0-SNAPSHOT#100290-rev:ed7c6f8ea342fcdf654be5e285289be6bf3b4784. 
