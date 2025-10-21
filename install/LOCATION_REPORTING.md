# Anonymous Location Reporting Feature

## Overview

The OpenELIS installer now includes an optional anonymous location reporting
feature that helps the development team understand the geographical reach and
scale of OpenELIS deployments worldwide.

## What Data is Collected?

When you opt-in to location reporting, the following **anonymous** data is sent
to the central server:

- **Site ID**: The 5-character site identifier you configure during installation
- **Version**: The version of OpenELIS being installed
- **Application Name**: OpenELIS-Global
- **Timestamp**: When the installation occurred
- **Timezone**: The timezone configured for the installation

## What Data is NOT Collected?

- **No clinical data**: No patient information, test results, or any clinical
  data
- **No personal information**: No names, addresses, emails, or any personally
  identifiable information
- **No system information**: No IP addresses, hardware details, or network
  configuration
- **No database contents**: No data from your database

## How It Works

1. **During Installation**: You will be prompted with a consent message
2. **Default Behavior**: If you simply press Enter, location reporting is
   **enabled** (opt-in by default)
3. **Opting Out**: Enter 'n' or 'no' to decline location reporting
4. **Data Transmission**: If opt-in, anonymous data is sent to
   `https://hub.openelis-global.org/api/location-report`
5. **Network Failure**: If the server is unreachable, installation continues
   normally

## Prompt Message

During installation, you will see:

```
================================================================================
                    ANONYMOUS LOCATION REPORTING
================================================================================

To enhance our understanding of the geographical reach and scale of OpenELIS
usage, we kindly request your consent to collect anonymous location information.

Please be assured that this data will be used solely for statistical analysis
to improve our services. We want to emphasize that no clinical or personal
information will be collected or stored.

Your participation is entirely voluntary and will greatly contribute to
optimizing the effectiveness of OpenELIS.

If you agree to share this anonymous location data, please opt in by answering
Y or simply pressing enter.

Thank you for your support!
================================================================================

Share anonymous location data? [Y/n]:
```

## Configuration File

Your opt-in preference is stored in:

```
/var/lib/openelis-global/config/LOCATION_OPT_IN
```

You can manually change this file to update your preference at any time:

- `true` = opt-in (share location data)
- `false` = opt-out (do not share location data)

## For Developers

### Server Endpoint

The installer sends a POST request to:
`https://hub.openelis-global.org/api/location-report`

### Request Format

```json
{
  "site_id": "12345",
  "version": "2.7.0",
  "app_name": "OpenELIS-Global",
  "timestamp": "2025-10-21 12:30:45",
  "timezone": "Africa/Nairobi"
}
```

### Implementation Details

- Located in: `install/installerTemplate/linux/setup_OpenELIS.py`
- Functions:
  - `set_location_opt_in()`: Prompts user for consent
  - `get_location_opt_in()`: Reads stored preference
  - `send_location_report()`: Sends data to central server
- Called during:
  - Initial installation (`do_install()`)
  - Update installation (`do_update()`)

### Error Handling

The function gracefully handles:

- Network timeouts (10-second timeout)
- Server unavailability
- Invalid responses

Installation continues regardless of whether the location report succeeds or
fails.

## Privacy Statement

This feature respects user privacy by:

1. **Requiring explicit consent** (opt-in model)
2. **Collecting only anonymous metadata** (no clinical or personal data)
3. **Being transparent** about what data is collected
4. **Allowing opt-out** at any time
5. **Failing gracefully** if server is unavailable

## Questions or Concerns

If you have questions about this feature, please contact the OpenELIS
development team or file an issue on the GitHub repository.
