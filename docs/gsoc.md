 <img src="https://github.com/I-TECH-UW/OpenELIS-Global-2/assets/29783151/a17e5666-156d-4882-91a0-606c8dc2f52e" alt="gsoc" width="800" align="center"/>

[**OpenELIS Global**](https://openelis-global.org/) is hoping to be a mentoring
organization for [Google Summer of Code](https://summerofcode.withgoogle.com/)
2024 for the first Time. We're extremely excited about the projects and
mentorship opportunities available this year. Coding for **OpenELIS Global** is
a great way to practice your coding skills and, at the same time help making a
positive impact on the world through empowering labs with the best tools and
support to deliver quality health care to clients.

If you are new to **OpenELIS Global**, we recommend starting with our
[Installation](install.md) and [Developer](dev_setup.md) Instructions

|     | Page Outline                                                          |
| --- | --------------------------------------------------------------------- |
| 1   | [GSoC 2024 Program Administrators](#gsoc-2024-program-administrators) |
| 2   | [Selected Projects for GSoC 2024](#selected-projects-for-gsoc-2024)   |
| 3   | [Program Timeline](#program-timeline)                                 |
| 4   | [Guidelines](#guidelines)                                             |

## GSoC 2024 Program Administrators

1. Mutesasira Moses
1. Casey Iiams-Hauser
1. Herbert Yiga
1. Brynn McKinney

## Selected Projects for GSoC 2024

| Project Name                                                                                                                                               | Project Size | Project Description                                                                                                                                                                                                                                                                                                                                                                                                                      | Expected Out Come                                                  | Required Skills                          | Selected Contributor      | Mentors            |
| ---------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------ | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------ | ---------------------------------------- | ------------------------- | ------------------ |
| [Enhanced Search Functionality for OpenELIS](https://summerofcode.withgoogle.com/organizations/openelis-global/projects/details/vhkjzlFu)                  | 350 hours    | This project aims to enhance the search capabilities within the OpenELIS system, providing users with a more effiecient way for retrieving vital information efficiently . We would need to Integrate a Search Engine library For indexing key coulmns and providing a faster way to Search Results                                                                                                                                      | Enhanced and Efficient Search Functionality                        | Java , Spring                            | Rahul Pamnani             | Mutesasira Moses   |
| [Improve E2E Tests Coverage](https://summerofcode.withgoogle.com/organizations/openelis-global/projects/details/KDXjqUsx)                                  | 350 hours    | The Current E2E tests have a very low test coverage and are not very stable. This project is dedicated to improve the End-to-End (E2E) testing coverage for the New React front end and ensuring robust validation of the entire application workflow . E2E tests play a critical role in verifying the seamless interaction between various components, ensuring that the application functions as expected from the user's perspective | Atleast 70% Test Coverage for the new UI                           | react , cypress                          | Manish Rabindra Kumar Jha | Namanya Abert      |
| [Rewite Admin Page components from JSP to React](https://summerofcode.withgoogle.com/organizations/openelis-global/projects/details/66IXZZXq)              | 350 hours    | Most of the current Admin page Functionalities are still on the old JSP UI. This project aims at re-writing key admin components from JSP to React including User management , Organization Management and Provider Management                                                                                                                                                                                                           | Admin Functionalities migrated to the new React UI                 | react , TypescripT, java , Spring , REST | Aditya Deshlahre          | Gita Cliff         |
| [Add Support for User UI Notifications](https://summerofcode.withgoogle.com/organizations/openelis-global/projects/details/g7zlNzjA)                       | 350 hours    | This projects aims at adding support for User Notifications in the new Front End ie when critical Orders and Results are made. This will require creating a generic Notification framework that can support adding any kind of System Notifications whenever needed                                                                                                                                                                      | A Generic Notification Framework for Displaying user Notifications | react , Typescript, java , Spring , REST | Vivek Rama Krishna Kona   | Casey Iiams-Hauser |
| [Integrating OpenElis with a FHIR-based Open Client Registry](https://summerofcode.withgoogle.com/organizations/openelis-global/projects/details/kyZvDB9m) | 350 hours    | This integration aims to connect OpenELIS, a laboratory information system, with a FHIR-Based Open Client Registry which will allow users to search for patients within their local OpenELIS system, If the patient isn't found locally, search the client registry                                                                                                                                                                      | Intgrating a FHIR based Client Registry with in OpenELIS           | react , Typescript, java , Spring , REST | Muhereza Herman           | Reagn Makoba       |

## Program Timeline

| Date          | Status | Activity                                                                                                        |
| ------------- | ------ | --------------------------------------------------------------------------------------------------------------- |
| 20/Nov/2023   | ✅     | GSoC 2024 Anounced                                                                                              |
| 22/Jan/2024   | ✅     | Mentoring organizations can begin submitting applications to Google                                             |
| 06/Feb/2024   | ✅     | Mentoring organization application deadline                                                                     |
| 21/Feb/20204  | ✅     | List of accepted mentoring organizations announced                                                              |
| 22/Feb/20204  | ✅     | Potential GSoC contributors discuss application ideas with mentoring organizations                              |
| 18/Mar/2024   | ✅     | GSoC contributor application period begins                                                                      |
| 02/Apr/2024   | ✅     | GSoC contributor application deadline                                                                           |
| 24/April/2024 | ✅     | GSoC contributor proposal rankings due from Org Admins                                                          |
| 29/Apr/2024   | ✅     | Slot Allocation Deadline                                                                                        |
| 30/Apr/2024   | ✅     | Projects Announced to Orgs                                                                                      |
| 01/May/2024   | ✅     | Accepted GSoC contributor projects announced                                                                    |
| 01/May/2024   | ✅     | Community Bonding Period. Students get to know mentors, read documentation, prepare for work on their projects  |
| 27/May/2024   | ✅     | Coding officially begins                                                                                        |
| 08/Jul/2024   | ⌛     | Mentors and GSoC contributors can begin submitting midterm evaluations                                          |
| 12/July/2024  | ⌛     | Midterm evaluation deadline (standard coding period)                                                            |
| 12/July/2024  | ⌛     | Work Period. GSoC contributors work on their project with guidance from Mentors                                 |
| 19/Aug/2024   | ⌛     | Final week. GSoC contributors submit their final work product and their final mentor evaluation (coding period) |
| 26/Aug/2024   | ⌛     | Mentors submit final GSoC contributor evaluations (standard coding period) for medium size project.             |
| 03/Sept/2024  | ⌛     | Initial results of Google Summer of Code 2023 announced                                                         |
| 03/Sep/2024   | ⌛     | GSoC contributors with extended timelines continue coding                                                       |
| 04/Nov/2024   | ⌛     | Final date for all GSoC contributors to submit their final work product and final evaluation                    |
| 11/Nov/2024   | ⌛     | Final date for mentors to submit evaluations for GSoC contributor projects with extended deadlines              |

see full [Timeline](https://developers.google.com/open-source/gsoc/timeline)

## Guidelines

### Student's guidelines

- [GSoC - Guidelines for Students](gsocstudentguide.md)

### Mentor's guidelines

- [GSoC - Guidelines for Mentors](https://google.github.io/gsocguides/mentor/what-makes-a-good-mentor)

!!! tip  
 Please Ensure to Follow the Students Guidelines Above
