# ASTM HTTP Bridge Multi-Analyzer & Bi-Directional Communication Analysis

**Date**: 2025-01-28  
**Feature**: 004-astm-analyzer-mapping  
**Analysis Type**: Architecture Gap Analysis  
**Scope**: Minimal updates needed to `tools/astm-http-bridge` for multi-analyzer
mediation and bi-directional query support

---

## Executive Summary

The `astm-http-bridge` is a **simple TCP-HTTP protocol router**. It should
remain focused on protocol translation, not business logic. OpenELIS handles
analyzer identification, routing, and query management.

**Key Findings**:

- ✅ **Multi-analyzer support**: Bridge already handles multiple concurrent
  analyzer connections (one thread per connection via `ASTMServlet`)
- ✅ **Bi-directional communication**: Bridge supports both directions (Analyzer
  → OpenELIS and OpenELIS → Analyzer)
- ✅ **OpenELIS → Analyzer**: Bridge already supports target IP/port via request
  parameters (`forwardAddress`, `forwardPort`)
- ❌ **Analyzer → OpenELIS**: Bridge needs to include source analyzer IP in HTTP
  request so OpenELIS can identify which analyzer sent the message

---

## Current Architecture Analysis

### Current State: Multi-Analyzer, Bi-Directional TCP-HTTP Router

**ASTM → HTTP Flow** (Analyzer → OpenELIS):

```
Multiple Analyzers (TCP) → ASTMServlet (port 12001) → ASTMHandlerService →
DefaultForwardingASTMToHTTPHandler → OpenELIS (HTTP POST)
```

**Current Capabilities**:

- ✅ **Multi-analyzer support**: `ASTMServlet.listen()` accepts multiple
  connections (line 70: `serverSocket.accept()` in while loop, spawns new thread
  per connection)
- ✅ Forwards ASTM messages to OpenELIS HTTP endpoint
- ❌ **Missing**: Source analyzer IP not included in HTTP request (OpenELIS
  can't identify which analyzer sent the message)

**HTTP → ASTM Flow** (OpenELIS → Analyzer):

```
OpenELIS (HTTP POST) → HTTPListenController → HTTPHandlerService →
DefaultForwardingHTTPToASTMHandler → Target Analyzer (TCP)
```

**Current Capabilities**:

- ✅ **Bi-directional support**: OpenELIS can send messages to analyzers
- ✅ **Multi-analyzer routing**: Accepts `forwardAddress` and `forwardPort`
  request parameters (lines 75-76 in HTTPListenController)
- ✅ Forwards ASTM messages to target analyzer IP/port (can route to different
  analyzers per request)
- ✅ Supports protocol version selection (`forwardAstmVersion` parameter)

### Code Evidence

**OpenELIS → Analyzer Already Supports Target IP/Port**
(`HTTPListenController.java:72-92`):

```java
@PostMapping
public HTTPHandlerServiceResponse recieveASTMMessageOverHttp(
  @RequestBody(required = false) String requestBody,
  @RequestParam(required = false) String forwardAddress,  // ✅ Target analyzer IP
  @RequestParam(required = false, defaultValue = "0") Integer forwardPort,  // ✅ Target analyzer port
  @RequestParam(required = false, defaultValue = "LIS01_A") ASTMVersion forwardAstmVersion,
  HttpServletResponse response
) {
  // Handler uses forwardAddress and forwardPort from request parameters
  HTTPForwardingHandlerInfo handlerInfo = new HTTPForwardingHandlerInfo();
  handlerInfo.setForwardAddress(forwardAddress);
  handlerInfo.setForwardPort(forwardPort);
  // ...
}
```

**Analyzer → OpenELIS Missing Source IP**
(`DefaultForwardingASTMToHTTPHandler.java:58-73`):

```java
@Override
public ASTMHandlerResponse handle(ASTMMessage message) {
  Builder requestBuilder = HttpRequest.newBuilder()
    .uri(forwardingUri)
    .POST(HttpRequest.BodyPublishers.ofString(message.getMessage()));
  // ❌ Missing: Source analyzer IP not included in request
  // OpenELIS needs this to identify which analyzer sent the message
}
```

**Socket Available in Receive Thread** (`ASTMReceiveThread.java:70-94`):

```java
public void run() {
  // Socket is available here - can extract remote IP
  // Socket socket = ... (from constructor)
  // String sourceIp = socket.getRemoteSocketAddress().toString();
  // But this IP is not passed to handler
}
```

---

## Required Updates (Minimal - Bridge Stays Simple)

### 1. Include Source Analyzer IP in HTTP Forward (CRITICAL)

**Problem**: When bridge forwards messages from analyzer to OpenELIS, OpenELIS
needs to know which analyzer sent the message to apply correct field mappings
(FR-011).

**Solution**: Extract source IP from socket and include in HTTP request header.

**Required Changes**:

1. **Update `ASTMReceiveThread`**: Extract source IP from socket

   ```java
   public class ASTMReceiveThread extends Thread {
     private final Socket socket;

     public void run() {
       String sourceIp = extractSourceIp(socket);  // NEW
       // Pass sourceIp to handler
     }

     private String extractSourceIp(Socket socket) {
       return ((InetSocketAddress) socket.getRemoteSocketAddress())
         .getAddress().getHostAddress();
     }
   }
   ```

2. **Update `ASTMHandlerService.handle()`**: Accept source IP parameter

   ```java
   public ASTMHandlerServiceResponse handle(ASTMMessage message, String sourceIp) {
     // Pass sourceIp to handlers
   }
   ```

3. **Update `DefaultForwardingASTMToHTTPHandler`**: Include source IP in HTTP
   header
   ```java
   @Override
   public ASTMHandlerResponse handle(ASTMMessage message, String sourceIp) {
     Builder requestBuilder = HttpRequest.newBuilder()
       .uri(forwardingUri)
       .POST(HttpRequest.BodyPublishers.ofString(message.getMessage()))
       .header("X-Source-Analyzer-IP", sourceIp);  // NEW
     // ...
   }
   ```

**OpenELIS Side**: OpenELIS `AnalyzerImportController` extracts
`X-Source-Analyzer-IP` header and looks up analyzer by IP address (per FR-011
analyzer identification strategy).

### 2. Verify OpenELIS → Analyzer Flow (Already Supported)

**Current State**: `HTTPListenController` already accepts `forwardAddress` and
`forwardPort` parameters.

**Verification**: ✅ No changes needed. OpenELIS can send queries by including
target analyzer IP/port in request:

```http
POST /bridge HTTP/1.1
Content-Type: text/plain

H|\^&|||...

?forwardAddress=192.168.1.10&forwardPort=5000&forwardAstmVersion=LIS01_A
```

**OpenELIS Query Flow** (per FR-002):

1. OpenELIS `AnalyzerQueryService` constructs ASTM query message
2. OpenELIS calls bridge:
   `POST /bridge?forwardAddress={analyzerIp}&forwardPort={analyzerPort}`
3. Bridge forwards query to analyzer via TCP
4. Bridge receives response from analyzer
5. Bridge returns response to OpenELIS (synchronous)
6. OpenELIS parses response, extracts fields, stores in job

**Note**: Job management, timeout handling, and field extraction are **OpenELIS
responsibilities**, not bridge responsibilities.

---

## Specification Consistency Analysis

### Coverage Gaps

| Requirement                         | Spec Reference                   | Bridge Support | Gap                                             |
| ----------------------------------- | -------------------------------- | -------------- | ----------------------------------------------- |
| Source analyzer IP in HTTP forward  | FR-011 (analyzer identification) | ❌ Missing     | **CRITICAL** - OpenELIS can't identify analyzer |
| Target analyzer IP/port for queries | FR-002                           | ✅ Supported   | ✅ Already supported                            |
| Query message construction          | FR-002                           | N/A (OpenELIS) | ✅ OpenELIS responsibility                      |
| Response parsing                    | FR-002                           | N/A (OpenELIS) | ✅ OpenELIS responsibility                      |
| Job management                      | FR-002                           | N/A (OpenELIS) | ✅ OpenELIS responsibility                      |

### Specification Alignment Issues

**FR-002 (Query Analyzer)** - Bridge Role:

- ✅ HTTP endpoint to receive query requests (exists: `HTTPListenController`)
- ✅ Target analyzer IP/port support (exists: `forwardAddress`, `forwardPort`
  parameters)
- ✅ Protocol version selection (exists: `forwardAstmVersion` parameter)
- ✅ **OpenELIS handles**: Query message construction, response parsing, job
  management, timeout

**FR-011 (Analyzer Identification)** - Bridge Role:

- ❌ **Missing**: Source analyzer IP in HTTP request header
- ✅ **OpenELIS handles**: IP-based lookup, header parsing fallback, analyzer
  context routing

### Plan.md Alignment

**Plan.md** correctly focuses on OpenELIS implementation. Bridge updates are
minimal (source IP header only).

### Tasks.md Coverage

**Tasks.md** correctly focuses on OpenELIS tasks. Missing bridge task:

- ❌ **MISSING**: TXXX - Include source analyzer IP in HTTP forward header

---

## Recommended Implementation Plan

### Single Phase: Source IP Header (CRITICAL - Blocks FR-011)

**Scope**: Minimal bridge update to include source analyzer IP in HTTP forward.

**Tasks**:

1. **Update `ASTMReceiveThread`** to extract source IP from socket

   - Extract IP:
     `((InetSocketAddress) socket.getRemoteSocketAddress()).getAddress().getHostAddress()`
   - Pass IP to `ASTMHandlerService.handle()` method

2. **Update `ASTMHandlerService.handle()`** method signature

   - Add `sourceIp` parameter: `handle(ASTMMessage message, String sourceIp)`
   - Pass source IP to handlers

3. **Update `DefaultForwardingASTMToHTTPHandler.handle()`** method signature

   - Add `sourceIp` parameter
   - Include header: `requestBuilder.header("X-Source-Analyzer-IP", sourceIp)`

4. **Update OpenELIS `AnalyzerImportController`** (OpenELIS side, not bridge)
   - Extract `X-Source-Analyzer-IP` header from request
   - Lookup analyzer by IP address (per FR-011)
   - Apply analyzer-specific field mappings

**Estimated Effort**: 2-4 hours (simple header addition)

**Dependencies**: None (bridge-only change)

### Verification: OpenELIS → Analyzer Flow

**Status**: ✅ Already supported via `forwardAddress` and `forwardPort`
parameters.

**No bridge changes needed**. OpenELIS query implementation (FR-002) can use
existing bridge endpoint.

---

## Constitution Compliance Check

### Layered Architecture (Principle IV)

**Current Bridge Architecture**: Spring Boot application with:

- Controllers: `HTTPListenController`, `ASTMServerRunner`
- Services: Handler services (implicit)
- **Status**: ✅ Simple router doesn't need full 5-layer pattern

**Required Updates**: None - bridge remains simple protocol translator

### Configuration-Driven Variation (Principle I)

**Current**: Static YAML configuration (`configuration.yml`)

**Required Updates**: None - OpenELIS handles analyzer configuration

### FHIR/IHE Compliance (Principle III)

**Status**: ✅ Not applicable - Bridge is internal middleware, doesn't expose
FHIR resources

### Test-Driven Development (Principle V)

**Required Tests** (minimal):

- Unit test: Verify source IP extraction from socket
- Integration test: Verify `X-Source-Analyzer-IP` header included in HTTP
  forward
- **Note**: OpenELIS tests cover analyzer identification, routing, query
  management

---

## Next Actions

### Immediate (Before Implementation)

1. **Update tasks.md**: Add single bridge task

   - TXXX: Include source analyzer IP in HTTP forward header
     - Update `ASTMReceiveThread` to extract source IP from socket
     - Update `ASTMHandlerService.handle()` to accept source IP parameter
     - Update `DefaultForwardingASTMToHTTPHandler` to include
       `X-Source-Analyzer-IP` header

2. **Update OpenELIS tasks** (already in tasks.md, verify):
   - TXXX: Extract `X-Source-Analyzer-IP` header in `AnalyzerImportController`
   - TXXX: Lookup analyzer by IP address (per FR-011)

### Implementation Order

1. **Bridge Update** (Source IP Header) - **CRITICAL** - 2-4 hours
2. **OpenELIS Update** (Header Extraction & IP Lookup) - **CRITICAL** - Part of
   existing FR-011 tasks

---

## Questions for Clarification

1. **Header Name**: Use `X-Source-Analyzer-IP` or `X-Analyzer-IP`?
   (Recommendation: `X-Source-Analyzer-IP` for clarity)
2. **Backward Compatibility**: Should bridge include header even if IP
   extraction fails? (Recommendation: Include empty header or log warning)

---

## Conclusion

The `astm-http-bridge` requires **minimal updates** to support Feature 004
requirements. The bridge should remain a simple TCP-HTTP protocol router.

**Required Bridge Changes**:

1. **Source IP Header** (CRITICAL) - Include `X-Source-Analyzer-IP` header when
   forwarding analyzer messages to OpenELIS
2. **OpenELIS → Analyzer** (✅ Already supported) - `forwardAddress` and
   `forwardPort` parameters work for queries

**OpenELIS Responsibilities** (not bridge):

- Analyzer identification by IP (FR-011)
- Query message construction (FR-002)
- Response parsing and field extraction (FR-002)
- Job management and timeout handling (FR-002)

**Estimated Effort**: 2-4 hours for bridge update (source IP header).

**Risk Level**: **LOW** - Simple header addition, minimal code changes, no
architectural complexity.
