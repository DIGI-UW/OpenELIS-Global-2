# ASTM LIS2-A2 Communication Pathway

This document outlines the typical communication flow between OpenELIS and an
ASTM-compatible analyzer, with full CLSI LIS1-A standards compliance.

## Protocol Standards

This mock server implements:

- **ASTM LIS2-A2**: Message format specification (application layer)
- **CLSI LIS1-A**: Low-level protocol specification (data link layer)

## Control Characters

| Char | Hex  | Name                | Description                        |
| ---- | ---- | ------------------- | ---------------------------------- |
| ENQ  | 0x05 | Enquiry             | Start transmission                 |
| ACK  | 0x06 | Acknowledge         | Positive response                  |
| NAK  | 0x15 | Neg Acknowledge     | Negative response (error detected) |
| EOT  | 0x04 | End of Transmission | End session or receiver interrupt  |
| STX  | 0x02 | Start of Text       | Frame start                        |
| ETX  | 0x03 | End of Text         | Frame end (with checksum)          |
| ETB  | 0x17 | End of Text Block   | Intermediate frame                 |
| CR   | 0x0D | Carriage Return     | Record separator                   |
| LF   | 0x0A | Line Feed           | Frame terminator (last char only)  |

## Step-by-Step Protocol

### 1. Establishment Phase (Handshake)

```
Client                          Server
  │                               │
  │  ENQ (0x05)                   │
  ├───────────────────────────────>│
  │                               │
  │                    ACK (0x06) │
  │<───────────────────────────────┤
  │                               │
```

**CLSI LIS1-A Requirements:**

- Server MUST respond to ENQ within **15 seconds** (establishment timeout)
- Response must be ACK (ready), NAK (busy), or ENQ (contention)

### 2. Transfer Phase (Data Frames)

```
Client                          Server
  │                               │
  │  STX + Frame                  │
  │  <STX><FN><data><ETX><CS><CR><LF>│
  ├───────────────────────────────>│
  │                               │
  │                    ACK (0x06) │
  │<───────────────────────────────┤
  │                               │
  │  (repeat for each frame)      │
  │                               │
```

**Frame Format:**

```
<STX><FN><data><ETX><checksum><CR><LF>
```

Where:

- **STX** (0x02): Start of Text
- **FN**: Frame number (ASCII digit '1'-'7')
- **data**: Message content (ASTM records)
- **ETX** (0x03): End of Text
- **checksum**: 2 hex digits (sum of bytes from FN to ETX, mod 256)
- **CR** (0x0D): Carriage Return
- **LF** (0x0A): Line Feed

**CLSI LIS1-A Frame Number Requirements:**

- Frame numbers are **1-7**, then wrap to **1**
- First frame: Accept any valid frame number (1-7)
- Subsequent frames: Must be **same as last accepted** OR **one higher**
  (modulo 8)
- Invalid frame numbers are rejected with **NAK**

**CLSI LIS1-A Checksum Requirements:**

- Checksum validation is **mandatory** (not optional)
- Checksum = sum of bytes from frame number to ETX, modulo 256
- Invalid checksums are rejected with **NAK**

**CLSI LIS1-A Timeout Requirements:**

- Frame ACK timeout: **15 seconds** (sender waits for ACK)
- Receiver timeout: **30 seconds** (receiver waits for frame/EOT)
- Server MUST respond to frames within 15 seconds

### 3. Termination Phase

```
Client                          Server
  │                               │
  │  EOT (0x04)                   │
  ├───────────────────────────────>│
  │                               │
  │  (connection remains open)    │
  │                               │
```

**EOT Handling:**

- Signals end of transmission
- Connection remains open for next transmission
- Can also be used as **receiver interrupt** (CLSI LIS1-A 8.3.5)

## Error Recovery (CLSI LIS1-A 8.5)

### Frame Validation

Server validates each frame for:

1. **Frame number sequence** (must match expected)
2. **Checksum** (must match calculated value)
3. **Character restrictions** (restricted chars not allowed in message text)
4. **Frame format** (proper STX, ETX, CR, LF)

### Retransmission Handling

**Per CLSI LIS1-A 8.5.1.2:**

- Invalid frames are rejected with **NAK**
- Sender retransmits frame with same frame number
- After **6 failed retransmissions**, sender MUST abort (send EOT and terminate)
- Server tracks retransmission count and aborts after 6 failures

### Restricted Characters (CLSI LIS1-A 8.6)

The following characters are **NOT permitted** in message text:

- SOH (0x01), STX (0x02), ETX (0x03), EOT (0x04)
- ENQ (0x05), ACK (0x06), DLE (0x10), NAK (0x15)
- SYN (0x16), ETB (0x17), LF (0x0A) - _except as last char of frame_
- DC1-DC4 (0x11-0x14)

**Exception:** LF is allowed **only** as the last character of a frame (after
CR).

## Complete Message Flow Example

### Client Sending Patient Results

```
1. Client → Server: ENQ (0x05)
2. Server → Client: ACK (0x06)
3. Client → Server: Frame 1: H|\^&|||Analyzer^Model^1.0|||||||LIS2-A2
4. Server → Client: ACK
5. Client → Server: Frame 2: P|1||PAT001|Smith^John||M|19850315
6. Server → Client: ACK
7. Client → Server: Frame 3: O|1|SAMPLE-001|CBC||20250115080000
8. Server → Client: ACK
9. Client → Server: Frame 4: R|1|^^^WBC|5.8|10^3/μL|N
10. Server → Client: ACK
11. Client → Server: Frame 5: R|2|^^^RBC|4.92|10^6/μL|N
12. Server → Client: ACK
13. Client → Server: Frame 6: L|1|N
14. Server → Client: ACK
15. Client → Server: EOT (0x04)
```

### Server Responding to Field Query

```
1. Client → Server: ENQ (0x05)
2. Server → Client: ACK (0x06)
3. Client → Server: Frame 1: H|\^&|||QuerySystem^Query^1.0|||||||LIS2-A2
4. Server → Client: ACK
5. Client → Server: Frame 2: L|1|N
6. Server → Client: ACK
7. Client → Server: EOT (0x04)
8. Server detects query (header only, no patient/order records)
9. Server → Client: ENQ (0x05)
10. Client → Server: ACK (0x06)
11. Server → Client: Frame 1: H|\^&|||MockAnalyzer^ASTM-Mock^1.0|||||||LIS2-A2
12. Client → Server: ACK
13. Server → Client: Frame 2: R|1|^^^WBC|WBC||10^3/μL|||NUMERIC
14. Client → Server: ACK
15. Server → Client: Frame 3: R|2|^^^RBC|RBC||10^6/μL|||NUMERIC
16. Client → Server: ACK
17. Server → Client: Frame 4: L|1|N
18. Client → Server: ACK
19. Server → Client: EOT (0x04)
```

## Query Detection

The server detects a field query when:

- **Header (H) record** is present
- **No Patient (P) or Order (O) records** follow
- Only header + terminator received

When a query is detected, the server automatically responds with available
fields.

## Error Scenarios

### Invalid Frame Number

```
Client → Server: Frame 5 (expected Frame 2)
Server → Client: NAK (0x15)
Client retransmits Frame 2
```

### Invalid Checksum

```
Client → Server: Frame with wrong checksum
Server → Client: NAK (0x15)
Client retransmits frame with correct checksum
```

### Retransmission Limit Exceeded

```
Client → Server: Frame 1 (invalid)
Server → Client: NAK
Client → Server: Frame 1 (still invalid)
Server → Client: NAK
... (repeats 6 times)
Server → Client: EOT (abort)
Server closes connection
```

### Receiver Interrupt

```
Server → Client: Frame 1
Client → Server: EOT (interrupt request)
Server stops sending, enters termination phase
```

## Timeout Behavior

### Establishment Timeout (15 seconds)

- If server doesn't respond to ENQ within 15 seconds, client enters termination
  phase

### Frame ACK Timeout (15 seconds)

- If server doesn't ACK a frame within 15 seconds, client aborts transmission

### Receiver Timeout (30 seconds)

- If server doesn't receive a frame or EOT within 30 seconds, server discards
  incomplete message and enters neutral state

## Standards References

- **CLSI LIS1-A**: Standard Specification for Low-Level Protocol to Transfer
  Messages Between Clinical Laboratory Instruments and Computer Systems
- **ASTM E1381-95**: Standard Specification for Transferring Information Between
  Clinical Instruments and Computer Systems
- **ASTM LIS2-A2**: Message format specification

## Implementation Notes

This mock server implements full CLSI LIS1-A compliance including:

- ✅ Frame number validation (1-7, wraps to 1, sequence checking)
- ✅ Mandatory checksum validation
- ✅ Timeout handling (15s establishment, 15s frame ACK, 30s receiver)
- ✅ Retransmission tracking (abort after 6 failures)
- ✅ Receiver interrupt handling (EOT as interrupt)
- ✅ Character restriction validation
- ✅ Query detection (header-only messages)

All validation is performed per CLSI LIS1-A standard to ensure compatibility
with real laboratory analyzers.
