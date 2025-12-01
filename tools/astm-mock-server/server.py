#!/usr/bin/env python3
"""
ASTM LIS2-A2 Mock Server for OpenELIS Analyzer Testing

This server simulates an ASTM-compatible laboratory analyzer for testing
the OpenELIS analyzer field mapping feature.

Reference Documents:
- specs/004-astm-analyzer-mapping/research.md Section 1 (ASTM Protocol)
- specs/004-astm-analyzer-mapping/spec.md FR-001, FR-002

ASTM LIS2-A2 Protocol Overview:
1. Client sends ENQ (0x05) to initiate communication
2. Server responds with ACK (0x06) if ready
3. Client sends data frames: <STX><FN><data><ETX/ETB><checksum><CR><LF>
4. Server ACKs each frame
5. Client sends EOT (0x04) to end transmission
6. Roles can reverse for bidirectional communication

Usage:
    python server.py [--port PORT] [--analyzer-type TYPE]

Environment Variables:
    ASTM_PORT: Server port (default: 5000)
    ANALYZER_TYPE: Analyzer type from fields.json (default: HEMATOLOGY)
    RESPONSE_DELAY_MS: Simulated response delay in milliseconds (default: 100)
"""

import socket
import threading
import json
import os
import sys
import time
import logging
import argparse
import urllib.request
import urllib.error
from typing import Optional, Dict, List, Any
from datetime import datetime
import random
from http.server import HTTPServer, BaseHTTPRequestHandler
from urllib.parse import urlparse, parse_qs

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# ASTM LIS2-A2 Control Characters
ENQ = b'\x05'  # Enquiry - Start transmission
ACK = b'\x06'  # Acknowledge - Positive response
NAK = b'\x15'  # Negative Acknowledge
EOT = b'\x04'  # End of Transmission
STX = b'\x02'  # Start of Text (frame start)
ETX = b'\x03'  # End of Text (frame end with checksum)
ETB = b'\x17'  # End of Text Block (intermediate frame)
CR = b'\x0D'   # Carriage Return
LF = b'\x0A'   # Line Feed

# Server Configuration
DEFAULT_PORT = 5000
DEFAULT_ANALYZER_TYPE = 'HEMATOLOGY'
DEFAULT_RESPONSE_DELAY_MS = 100

# CLSI LIS1-A Timeout Requirements
ESTABLISHMENT_TIMEOUT = 15  # seconds - ENQ response timeout
FRAME_ACK_TIMEOUT = 15      # seconds - Frame ACK timeout  
RECEIVER_TIMEOUT = 30       # seconds - Receiver waiting for frame/EOT
SOCKET_TIMEOUT = 60         # seconds - Overall socket timeout (keep for safety)
MAX_CONNECTIONS = 10

# Restricted characters per CLSI LIS1-A 8.6
RESTRICTED_CHARS = [
    b'\x01',  # SOH
    b'\x02',  # STX
    b'\x03',  # ETX
    b'\x04',  # EOT
    b'\x05',  # ENQ
    b'\x06',  # ACK
    b'\x10',  # DLE
    b'\x15',  # NAK
    b'\x16',  # SYN
    b'\x17',  # ETB
    b'\x0A',  # LF (only allowed as last char of frame)
    b'\x11',  # DC1
    b'\x12',  # DC2
    b'\x13',  # DC3
    b'\x14',  # DC4
]


class ASTMProtocolHandler:
    """Handles ASTM LIS2-A2 protocol communication for a single client."""
    
    def __init__(self, conn: socket.socket, addr: tuple, fields_config: Dict, 
                 response_delay_ms: int = DEFAULT_RESPONSE_DELAY_MS):
        self.conn = conn
        self.addr = addr
        self.fields_config = fields_config
        self.response_delay_ms = response_delay_ms
        self.frame_number = 0
        self.last_accepted_frame = 0  # Track last accepted frame number per CLSI LIS1-A
        self.retransmit_count = 0  # Track retransmissions per CLSI LIS1-A
        self.received_data: List[bytes] = []
        self.running = True
        
    def handle(self):
        """Main handler loop for client connection."""
        logger.info(f"Client connected: {self.addr}")
        self.conn.settimeout(SOCKET_TIMEOUT)
        
        try:
            while self.running:
                data = self._receive_byte()
                if not data:
                    break
                    
                if data == ENQ:
                    self._handle_enq()
                elif data == EOT:
                    self._handle_eot()
                elif data == STX:
                    self._handle_frame()
                else:
                    logger.warning(f"Unexpected byte received: {data.hex()}")
                    
        except socket.timeout:
            logger.info(f"Client timeout: {self.addr}")
        except ConnectionResetError:
            logger.info(f"Client disconnected: {self.addr}")
        except Exception as e:
            logger.error(f"Error handling client {self.addr}: {e}")
        finally:
            self._cleanup()
            
    def _receive_byte(self) -> Optional[bytes]:
        """Receive a single byte from client."""
        try:
            return self.conn.recv(1)
        except socket.timeout:
            return None
            
    def _receive_until(self, terminator: bytes, max_length: int = 4096) -> bytes:
        """Receive data until terminator sequence."""
        data = b''
        while len(data) < max_length:
            byte = self._receive_byte()
            if not byte:
                break
            data += byte
            if data.endswith(terminator):
                break
        return data
        
    def _send(self, data: bytes):
        """Send data to client with optional delay."""
        if self.response_delay_ms > 0:
            time.sleep(self.response_delay_ms / 1000.0)
        self.conn.send(data)
        
    def _handle_enq(self):
        """Handle ENQ (enquiry) - client wants to send data."""
        logger.debug(f"Received ENQ from {self.addr}")
        # Per CLSI LIS1-A: Must respond within establishment timeout
        self._send(ACK)
        logger.debug(f"Sent ACK to {self.addr}")
        
    def _handle_eot(self):
        """Handle EOT (end of transmission)."""
        logger.debug(f"Received EOT from {self.addr}")
        # Process any received data
        if self.received_data:
            self._process_received_data()
        # Connection remains open for potential next transmission
        
    def _handle_frame(self):
        """Handle incoming ASTM data frame."""
        # Read frame content until ETX/ETB + checksum + CR + LF
        frame_data = self._receive_until(CR + LF)
        
        if not frame_data:
            logger.warning("Empty frame received")
            self._send(NAK)
            self.retransmit_count += 1
            if self.retransmit_count >= 6:
                logger.error("Aborting: Frame retransmitted 6 times without success")
                self._send(EOT)
                self.running = False
            return
            
        # Parse frame: <FN><data><ETX/ETB><checksum>
        if len(frame_data) < 4:  # Minimum: FN + ETX + checksum(2) + CRLF
            logger.warning(f"Frame too short: {len(frame_data)}")
            self._send(NAK)
            self.retransmit_count += 1
            if self.retransmit_count >= 6:
                logger.error("Aborting: Frame retransmitted 6 times without success")
                self._send(EOT)
                self.running = False
            return
            
        # Extract frame content (between STX and checksum)
        # Frame number is first byte after STX
        frame_num_bytes = frame_data[0:1]
        
        # Extract frame number (ASCII digit '1'-'7')
        try:
            frame_num_char = frame_num_bytes.decode('ascii', errors='strict')
            if not frame_num_char.isdigit():
                logger.warning(f"Invalid frame number: {frame_num_char}")
                self._send(NAK)
                self.retransmit_count += 1
                if self.retransmit_count >= 6:
                    logger.error("Aborting: Frame retransmitted 6 times without success")
                    self._send(EOT)
                    self.running = False
                return
            
            frame_num = int(frame_num_char)
        except (UnicodeDecodeError, ValueError) as e:
            logger.warning(f"Invalid frame number encoding: {e}")
            self._send(NAK)
            self.retransmit_count += 1
            if self.retransmit_count >= 6:
                logger.error("Aborting: Frame retransmitted 6 times without success")
                self._send(EOT)
                self.running = False
            return
        
        # Validate frame number per CLSI LIS1-A: must be same as last accepted OR one higher (modulo 8)
        if self.last_accepted_frame == 0:
            # First frame - accept any valid frame number (1-7)
            if frame_num < 1 or frame_num > 7:
                logger.warning(f"Invalid frame number range: {frame_num} (must be 1-7)")
                self._send(NAK)
                self.retransmit_count += 1
                if self.retransmit_count >= 6:
                    logger.error("Aborting: Frame retransmitted 6 times without success")
                    self._send(EOT)
                    self.running = False
                return
        else:
            # Subsequent frames: must be same as last accepted OR one higher (modulo 8)
            # Per CLSI LIS1-A: frame number must be same as last accepted OR one higher (modulo 8)
            # Frame numbers are 1-7, so "one higher" means: (last_accepted_frame % 7) + 1
            expected_frame = (self.last_accepted_frame % 7) + 1
            if frame_num != self.last_accepted_frame and frame_num != expected_frame:
                logger.warning(f"Frame number mismatch: expected {expected_frame} or {self.last_accepted_frame}, got {frame_num}")
                self._send(NAK)
                self.retransmit_count += 1
                if self.retransmit_count >= 6:
                    logger.error("Aborting: Frame retransmitted 6 times without success")
                    self._send(EOT)
                    self.running = False
                return
        
        # Find ETX or ETB position
        etx_pos = frame_data.find(ETX)
        etb_pos = frame_data.find(ETB)
        
        if etx_pos == -1 and etb_pos == -1:
            logger.warning("No frame terminator found")
            self._send(NAK)
            self.retransmit_count += 1
            if self.retransmit_count >= 6:
                logger.error("Aborting: Frame retransmitted 6 times without success")
                self._send(EOT)
                self.running = False
            return
            
        term_pos = etx_pos if etx_pos != -1 else etb_pos
        content = frame_data[1:term_pos]
        
        # Validate message characters per CLSI LIS1-A 8.6
        if not self._validate_message_chars(content):
            logger.warning("Restricted characters found in message text")
            self._send(NAK)
            self.retransmit_count += 1
            if self.retransmit_count >= 6:
                logger.error("Aborting: Frame retransmitted 6 times without success")
                self._send(EOT)
                self.running = False
            return
        
        # Extract checksum (2 hex digits after ETX/ETB)
        checksum_start = term_pos + 1
        if len(frame_data) < checksum_start + 2:
            logger.warning("Incomplete checksum in frame")
            self._send(NAK)
            self.retransmit_count += 1
            if self.retransmit_count >= 6:
                logger.error("Aborting: Frame retransmitted 6 times without success")
                self._send(EOT)
                self.running = False
            return
        
        checksum_str = frame_data[checksum_start:checksum_start+2].decode('ascii', errors='ignore')
        
        # Calculate expected checksum: sum of bytes from frame number to ETX, mod 256
        checksum_data = frame_data[0:term_pos+1]  # FN + content + ETX
        calculated_checksum = sum(checksum_data) % 256
        expected_checksum_str = f'{calculated_checksum:02X}'
        
        if checksum_str.upper() != expected_checksum_str:
            logger.warning(f"Checksum mismatch: expected {expected_checksum_str}, got {checksum_str}")
            self._send(NAK)
            self.retransmit_count += 1
            if self.retransmit_count >= 6:
                logger.error("Aborting: Frame retransmitted 6 times without success")
                self._send(EOT)
                self.running = False
            return
        
        # Frame is valid - reset retransmit counter and update last accepted frame
        self.retransmit_count = 0
        self.last_accepted_frame = frame_num
        
        logger.debug(f"Received frame {frame_num}: {content[:50]}...")
        self.received_data.append(content)
        self._send(ACK)
        
    def _process_received_data(self):
        """Process accumulated received data."""
        logger.info(f"[MESSAGE] Processing {len(self.received_data)} frames from {self.addr}")
        
        # Detect if this was a query request BEFORE processing/clearing data
        # Query is detected when we receive a complete message with header but no patient/order records
        is_query = self._is_field_query()
        logger.debug(f"[MESSAGE] Query detection: is_query={is_query}")
        
        for frame_idx, frame in enumerate(self.received_data, 1):
            try:
                decoded = frame.decode('utf-8', errors='replace')
                logger.debug(f"[MESSAGE] Frame {frame_idx}/{len(self.received_data)}: {decoded[:100]}")
                
                # Parse ASTM records
                if decoded.startswith('H|'):
                    self._process_header(decoded)
                elif decoded.startswith('P|'):
                    self._process_patient(decoded)
                elif decoded.startswith('O|'):
                    self._process_order(decoded)
                elif decoded.startswith('R|'):
                    self._process_result(decoded)
                elif decoded.startswith('Q|'):
                    self._process_qc(decoded)
                elif decoded.startswith('L|'):
                    self._process_terminator(decoded)
                    
            except Exception as e:
                logger.error(f"[MESSAGE] Error processing frame {frame_idx}: {e}", exc_info=True)
                
        # Clear received data
        self.received_data = []
        
        # Respond to query if detected
        if is_query:
            logger.info(f"[MESSAGE] Field query detected from {self.addr}, sending field list")
            self.send_field_query_response()
        else:
            logger.debug(f"[MESSAGE] Not a field query, no response needed")
        
    def _process_header(self, record: str):
        """Process H (Header) record."""
        logger.info(f"Header record received: {record[:60]}...")
        
    def _process_patient(self, record: str):
        """Process P (Patient) record."""
        parts = record.split('|')
        patient_id = parts[3] if len(parts) > 3 else 'Unknown'
        logger.info(f"Patient record: ID={patient_id}")
        
    def _process_order(self, record: str):
        """Process O (Order) record."""
        parts = record.split('|')
        sample_id = parts[2] if len(parts) > 2 else 'Unknown'
        logger.info(f"Order record: Sample={sample_id}")
        
    def _process_result(self, record: str):
        """Process R (Result) record."""
        parts = record.split('|')
        if len(parts) >= 4:
            test_code = parts[2] if len(parts) > 2 else 'Unknown'
            value = parts[3] if len(parts) > 3 else ''
            unit = parts[4] if len(parts) > 4 else ''
            logger.info(f"Result record: {test_code} = {value} {unit}")
            
    def _process_qc(self, record: str):
        """Process Q (QC/Quality Control) record."""
        parts = record.split('|')
        logger.info(f"QC record: {record[:60]}...")
        
    def _process_terminator(self, record: str):
        """Process L (Terminator) record."""
        logger.debug("Message terminator received")
    
    def _is_field_query(self) -> bool:
        """Detect if received message is a field query request.
        
        Query is detected when:
        - Header (H) record is present
        - No Patient (P) or Order (O) records follow
        - Only header + terminator received
        """
        if not self.received_data:
            return False
        
        has_header = False
        has_patient_or_order = False
        
        for frame in self.received_data:
            try:
                decoded = frame.decode('utf-8', errors='replace')
                if decoded.startswith('H|'):
                    has_header = True
                elif decoded.startswith('P|') or decoded.startswith('O|'):
                    has_patient_or_order = True
            except:
                pass
        
        return has_header and not has_patient_or_order
    
    def _validate_message_chars(self, content: bytes) -> bool:
        """Validate message text doesn't contain restricted characters per CLSI LIS1-A 8.6."""
        # LF is allowed as last character of frame, so check content without trailing LF
        content_to_check = content.rstrip(LF)
        for restricted in RESTRICTED_CHARS:
            if restricted in content_to_check:
                logger.warning(f"Restricted character found in message: {restricted.hex()}")
                return False
        return True
        
    def send_field_query_response(self):
        """Send available fields in response to a query."""
        logger.info(f"[FIELD_QUERY] Sending field query response to {self.addr}")
        
        # Wait for client to be ready (they should send ENQ first, but we'll initiate)
        # In ASTM, the server can also initiate by sending ENQ
        logger.debug(f"[FIELD_QUERY] Sending ENQ to initiate response")
        self._send(ENQ)
        
        # Wait for ACK (with timeout)
        try:
            response = self._receive_byte()
            if response != ACK:
                logger.warning(f"[FIELD_QUERY] Did not receive ACK for field query response, got: {response.hex() if response else 'none'}")
                return
            logger.debug(f"[FIELD_QUERY] Received ACK, proceeding with field list")
        except socket.timeout:
            logger.warning("[FIELD_QUERY] Timeout waiting for ACK")
            return
            
        # Send header frame
        header_record = f"H|\\^&|||MockAnalyzer^ASTM-Mock^1.0|||||||LIS2-A2"
        logger.debug(f"[FIELD_QUERY] Sending header: {header_record}")
        self._send_frame(header_record)
        
        # Send result frames for each field in the configured analyzer type
        frame_seq = 1
        analyzer_type = os.getenv('ANALYZER_TYPE', DEFAULT_ANALYZER_TYPE)
        fields = self.fields_config.get(analyzer_type, [])
        
        if not fields:
            logger.warning(f"[FIELD_QUERY] No fields configured for analyzer type: {analyzer_type}")
            # Fallback: use first available analyzer type
            if self.fields_config:
                analyzer_type = list(self.fields_config.keys())[0]
                fields = self.fields_config[analyzer_type]
                logger.info(f"[FIELD_QUERY] Using fields from analyzer type: {analyzer_type}")
        
        logger.info(f"[FIELD_QUERY] Sending {len(fields)} fields for analyzer type: {analyzer_type}")
        
        for field in fields:
            field_name = field.get('name', 'Unknown')
            display_name = field.get('displayName', field_name)
            field_type = field.get('type', 'NUMERIC')
            unit = field.get('unit', '')
            # astmRef should be just the test identifier (e.g., "^^^WBC" or "^^^WBC^White Blood Cell Count")
            # NOT including "R|seq|" prefix - that's part of the record format, not the ref
            astm_ref = field.get('astmRef', f'^^^{field_name}')
            
            # Format as ASTM R record per LIS2-A2 spec: R|seq|test_id^test_name|value|units|reference_range|abnormal_flags|status|...
            # For query response (no values), format: R|seq|test_id^test_name||units|||field_type
            # Where test_id^test_name is the astm_ref (may be just "^^^WBC" or "^^^WBC^Display Name")
            if display_name != field_name and '^' not in astm_ref:
                # If we have a display name and astm_ref doesn't include it, append it
                test_id = f"{astm_ref}^{display_name}"
            else:
                test_id = astm_ref
            
            record = f"R|{frame_seq}|{test_id}||{unit}|||{field_type}"
            logger.debug(f"[FIELD_QUERY] Sending R record {frame_seq}: field={field_name}, display={display_name}, astmRef={astm_ref}, unit={unit}, type={field_type}, record={record}")
            if not self._send_frame(record):
                # Frame send failed (receiver interrupt or timeout)
                logger.warning(f"[FIELD_QUERY] Stopping field query response due to send failure at frame {frame_seq}")
                break
            frame_seq += 1
            
        # Send terminator
        logger.debug("[FIELD_QUERY] Sending terminator record")
        if not self._send_frame("L|1|N"):
            logger.warning("[FIELD_QUERY] Terminator frame send failed")
        
        # End transmission
        self._send(EOT)
        logger.info(f"[FIELD_QUERY] Successfully sent {frame_seq - 1} fields in response to query from {self.addr}")
        
    def _send_frame(self, content: str):
        """Send an ASTM frame with proper framing."""
        # Per CLSI LIS1-A: Frame numbers are 1-7, then wrap to 1.
        # Since frame_number starts at 0, we use (frame_number % 7) + 1 to get 1-7
        self.frame_number = (self.frame_number % 7) + 1
        
        # Build frame: <STX><FN><content><ETX><checksum><CR><LF>
        frame_num = str(self.frame_number).encode()
        content_bytes = content.encode('utf-8')
        
        # Calculate checksum (sum of bytes from frame num to ETX, mod 256)
        checksum_data = frame_num + content_bytes + ETX
        checksum = sum(checksum_data) % 256
        checksum_str = f'{checksum:02X}'.encode()
        
        frame = STX + frame_num + content_bytes + ETX + checksum_str + CR + LF
        self.conn.send(frame)
        
        # Wait for ACK with proper timeout per CLSI LIS1-A
        self.conn.settimeout(FRAME_ACK_TIMEOUT)
        try:
            response = self.conn.recv(1)
            if response == EOT:
                # Receiver interrupt request per CLSI LIS1-A 8.3.5
                logger.info("Receiver interrupt requested (EOT received)")
                return False  # Signal to stop sending
            elif response != ACK:
                logger.warning(f"Frame not ACKed: {response.hex() if response else 'none'}")
                # Per CLSI LIS1-A: Should retransmit on NAK, abort after 6 failures
                return False
        except socket.timeout:
            logger.warning("Timeout waiting for frame ACK (15s limit per CLSI LIS1-A)")
            return False
        
        return True
            
    def _cleanup(self):
        """Clean up connection resources."""
        try:
            self.conn.close()
        except:
            pass
        logger.info(f"Client disconnected: {self.addr}")


class ASTMMockServer:
    """ASTM LIS2-A2 Mock Server for analyzer testing."""
    
    def __init__(self, port: int = DEFAULT_PORT, 
                 analyzer_type: str = DEFAULT_ANALYZER_TYPE,
                 response_delay_ms: int = DEFAULT_RESPONSE_DELAY_MS):
        self.port = port
        self.analyzer_type = analyzer_type
        self.response_delay_ms = response_delay_ms
        self.fields_config = self._load_fields_config()
        self.running = False
        self.server_socket: Optional[socket.socket] = None
        self.client_threads: List[threading.Thread] = []
        
    def _load_fields_config(self) -> Dict:
        """Load analyzer field configuration from JSON file."""
        config_path = os.path.join(os.path.dirname(__file__), 'fields.json')
        
        if os.path.exists(config_path):
            try:
                with open(config_path, 'r') as f:
                    return json.load(f)
            except Exception as e:
                logger.error(f"Error loading fields.json: {e}")
                
        # Return default configuration if file not found
        return self._get_default_fields()
        
    def _get_default_fields(self) -> Dict:
        """Return default field configuration."""
        return {
            "HEMATOLOGY": [
                {"name": "WBC", "astmRef": "R|1|^^^WBC", "type": "NUMERIC", "unit": "10^3/μL"},
                {"name": "RBC", "astmRef": "R|1|^^^RBC", "type": "NUMERIC", "unit": "10^6/μL"},
                {"name": "HGB", "astmRef": "R|1|^^^HGB", "type": "NUMERIC", "unit": "g/dL"},
                {"name": "HCT", "astmRef": "R|1|^^^HCT", "type": "NUMERIC", "unit": "%"},
                {"name": "PLT", "astmRef": "R|1|^^^PLT", "type": "NUMERIC", "unit": "10^3/μL"}
            ],
            "CHEMISTRY": [
                {"name": "Glucose", "astmRef": "R|1|^^^GLUCOSE", "type": "NUMERIC", "unit": "mg/dL"},
                {"name": "Creatinine", "astmRef": "R|1|^^^CREATININE", "type": "NUMERIC", "unit": "mg/dL"}
            ]
        }
        
    def start(self):
        """Start the mock server."""
        self.running = True
        self.server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        
        try:
            self.server_socket.bind(('0.0.0.0', self.port))
            self.server_socket.listen(MAX_CONNECTIONS)
            logger.info(f"ASTM Mock Server started on port {self.port}")
            logger.info(f"Analyzer type: {self.analyzer_type}")
            logger.info(f"Response delay: {self.response_delay_ms}ms")
            
            while self.running:
                try:
                    self.server_socket.settimeout(1.0)
                    conn, addr = self.server_socket.accept()
                    
                    # Handle client in separate thread
                    handler = ASTMProtocolHandler(
                        conn, addr, self.fields_config, self.response_delay_ms
                    )
                    thread = threading.Thread(target=handler.handle, daemon=True)
                    thread.start()
                    self.client_threads.append(thread)
                    
                except socket.timeout:
                    # Check if we should stop
                    continue
                    
        except KeyboardInterrupt:
            logger.info("Server shutdown requested")
        except Exception as e:
            logger.error(f"Server error: {e}")
        finally:
            self.stop()
            
    def stop(self):
        """Stop the mock server."""
        self.running = False
        if self.server_socket:
            try:
                self.server_socket.close()
            except:
                pass
        logger.info("ASTM Mock Server stopped")


def generate_astm_message(analyzer_type: str, fields_config: Dict, 
                          patient_id: str = None, sample_id: str = None,
                          patient_name: str = None, patient_dob: str = None,
                          patient_sex: str = None) -> str:
    """
    Generate a complete ASTM LIS2-A2 message with H, P, O, R, L segments.
    
    Args:
        analyzer_type: Type of analyzer (HEMATOLOGY, CHEMISTRY, etc.)
        fields_config: Field configuration dictionary
        patient_id: Patient ID (default: auto-generated)
        sample_id: Sample/Order ID (default: auto-generated)
        patient_name: Patient name in format "Last^First^Middle" (default: auto-generated)
        patient_dob: Patient date of birth YYYYMMDD (default: auto-generated)
        patient_sex: Patient sex M/F (default: random)
    
    Returns:
        Complete ASTM message as string (newline-delimited segments)
    """
    # Get fields for this analyzer type
    fields = fields_config.get(analyzer_type, [])
    if not fields:
        # Fallback to first available type
        if fields_config:
            analyzer_type = list(fields_config.keys())[0]
            fields = fields_config[analyzer_type]
            logger.warning(f"No fields for analyzer type, using {analyzer_type}")
        else:
            logger.error("No fields configuration available")
            return ""
    
    # Generate timestamps
    now = datetime.now()
    timestamp = now.strftime("%Y%m%d%H%M%S")
    order_timestamp = (now.replace(second=0, microsecond=0)).strftime("%Y%m%d%H%M%S")
    result_timestamp = now.strftime("%Y%m%d%H%M%S")
    
    # Generate patient/sample IDs if not provided
    if not patient_id:
        patient_id = f"PAT-{now.strftime('%Y%m%d')}-{random.randint(100, 999)}"
    if not sample_id:
        sample_id = f"SAMPLE-{now.strftime('%Y%m%d')}-{random.randint(1000, 9999)}"
    if not patient_name:
        first_names = ["John", "Mary", "James", "Sarah", "Robert", "Emily"]
        last_names = ["Smith", "Johnson", "Williams", "Brown", "Jones"]
        patient_name = f"{random.choice(last_names)}^{random.choice(first_names)}"
    if not patient_dob:
        # Generate random DOB between 1950 and 2000
        year = random.randint(1950, 2000)
        month = random.randint(1, 12)
        day = random.randint(1, 28)
        patient_dob = f"{year}{month:02d}{day:02d}"
    if not patient_sex:
        patient_sex = random.choice(["M", "F"])
    
    # Determine analyzer name from type
    analyzer_names = {
        "HEMATOLOGY": "Sysmex^XN-1000^V1.0",
        "CHEMISTRY": "Beckman^AU5800^V2.1",
        "IMMUNOLOGY": "Roche^Cobas^V1.5",
        "MICROBIOLOGY": "BD^Phoenix^V2.0"
    }
    analyzer_name = analyzer_names.get(analyzer_type, f"MockAnalyzer^{analyzer_type}^1.0")
    
    # Build message segments
    segments = []
    
    # H - Header Record
    header = f"H|\\^&|||{analyzer_name}|||||||LIS2-A2|{timestamp}"
    segments.append(header)
    
    # P - Patient Record
    patient = f"P|1||{patient_id}|{patient_name}||{patient_sex}|{patient_dob}"
    segments.append(patient)
    
    # O - Order Record
    panel_name = "CBC" if analyzer_type == "HEMATOLOGY" else "CHEM" if analyzer_type == "CHEMISTRY" else analyzer_type
    order = f"O|1|{sample_id}^LAB|{panel_name}^{panel_name} Panel||{order_timestamp}"
    segments.append(order)
    
    # R - Result Records
    result_seq = 1
    for field in fields:
        field_name = field.get('name', 'Unknown')
        display_name = field.get('displayName', field_name)
        field_type = field.get('type', 'NUMERIC')
        unit = field.get('unit', '')
        normal_range = field.get('normalRange', '')
        astm_ref = field.get('astmRef', f'R|{result_seq}|^^^{field_name}')
        
        # Generate realistic test value based on type
        if field_type == 'NUMERIC':
            # Generate value within normal range if available
            if normal_range:
                # Parse range like "4.5-11.0" or "<200" or ">40"
                try:
                    if '-' in normal_range:
                        low, high = map(float, normal_range.split('-'))
                        value = round(random.uniform(low, high), 2)
                    elif normal_range.startswith('<'):
                        max_val = float(normal_range[1:])
                        value = round(random.uniform(0, max_val * 0.9), 2)
                    elif normal_range.startswith('>'):
                        min_val = float(normal_range[1:])
                        value = round(random.uniform(min_val * 1.1, min_val * 2), 2)
                    else:
                        value = round(random.uniform(1, 100), 2)
                except:
                    value = round(random.uniform(1, 100), 2)
            else:
                value = round(random.uniform(1, 100), 2)
            
            result = f"R|{result_seq}|{astm_ref}^{display_name}|{value}|{unit}|{normal_range}|N||F|{result_timestamp}"
        elif field_type == 'QUALITATIVE':
            possible_values = field.get('possibleValues', ['POSITIVE', 'NEGATIVE'])
            value = random.choice(possible_values)
            result = f"R|{result_seq}|{astm_ref}^{display_name}|{value}|||N||F|{result_timestamp}"
        else:  # TEXT
            value = f"Sample result for {display_name}"
            result = f"R|{result_seq}|{astm_ref}^{display_name}|{value}|||N||F|{result_timestamp}"
        
        segments.append(result)
        result_seq += 1
    
    # L - Terminator Record
    terminator = "L|1|N"
    segments.append(terminator)
    
    # Join segments with newlines (ASTM format)
    message = '\n'.join(segments) + '\n'
    return message


class PushAPIHandler(BaseHTTPRequestHandler):
    """HTTP API handler for triggering pushes."""
    
    fields_config = {}
    openelis_url = None
    
    def do_POST(self):
        """Handle POST requests to trigger pushes."""
        if self.path == '/push' or self.path.startswith('/push?'):
            self.handle_push_request()
        else:
            self.send_error(404, "Not Found")
    
    def do_GET(self):
        """Handle GET requests for health check and info."""
        if self.path == '/health' or self.path == '/':
            self.send_response(200)
            self.send_header('Content-Type', 'application/json')
            self.end_headers()
            response = {
                "status": "ok",
                "service": "ASTM Mock Server Push API",
                "endpoints": {
                    "POST /push": "Trigger a push to OpenELIS",
                    "GET /health": "Health check"
                }
            }
            self.wfile.write(json.dumps(response, indent=2).encode('utf-8'))
        else:
            self.send_error(404, "Not Found")
    
    def handle_push_request(self):
        """Handle push trigger request."""
        client_addr = self.address_string()
        try:
            logger.info(f"[API] Push request received from {client_addr}: {self.path}")
            
            # Parse query parameters
            parsed_path = urlparse(self.path)
            query_params = parse_qs(parsed_path.query)
            
            # Get analyzer type from query or default
            analyzer_type = query_params.get('analyzer_type', ['HEMATOLOGY'])[0].upper()
            count = int(query_params.get('count', ['1'])[0])
            
            logger.info(f"[API] Request parameters: analyzer_type={analyzer_type}, count={count}")
            
            # Read request body if present (JSON)
            content_length = int(self.headers.get('Content-Length', 0))
            request_body = {}
            if content_length > 0:
                body_data = self.rfile.read(content_length)
                try:
                    request_body = json.loads(body_data.decode('utf-8'))
                    logger.debug(f"[API] Request body: {json.dumps(request_body)}")
                    # Override query params with body if present
                    analyzer_type = request_body.get('analyzer_type', analyzer_type).upper()
                    count = int(request_body.get('count', count))
                    logger.info(f"[API] Overridden parameters from body: analyzer_type={analyzer_type}, count={count}")
                except json.JSONDecodeError as e:
                    logger.warning(f"[API] Failed to parse JSON body: {e}")
            
            # Generate and push messages
            results = []
            success_count = 0
            
            logger.info(f"[API] Generating and pushing {count} message(s) of type {analyzer_type}")
            
            for i in range(count):
                logger.debug(f"[API] Generating message {i+1}/{count}")
                message = generate_astm_message(
                    analyzer_type=analyzer_type,
                    fields_config=self.fields_config
                )
                
                if message:
                    logger.debug(f"[API] Pushing message {i+1}/{count} to {self.openelis_url}")
                    success = push_to_openelis(self.openelis_url, message)
                    results.append({
                        "message_number": i + 1,
                        "success": success,
                        "analyzer_type": analyzer_type
                    })
                    if success:
                        success_count += 1
                        logger.debug(f"[API] Message {i+1}/{count} pushed successfully")
                    else:
                        logger.warning(f"[API] Message {i+1}/{count} push failed")
                else:
                    logger.error(f"[API] Failed to generate message {i+1}/{count}")
                    results.append({
                        "message_number": i + 1,
                        "success": False,
                        "error": "Failed to generate message"
                    })
            
            # Send response
            self.send_response(200)
            self.send_header('Content-Type', 'application/json')
            self.end_headers()
            
            response = {
                "status": "completed",
                "total": count,
                "successful": success_count,
                "failed": count - success_count,
                "results": results
            }
            
            logger.info(f"[API] Push request completed: {success_count}/{count} successful")
            self.wfile.write(json.dumps(response, indent=2).encode('utf-8'))
            
        except Exception as e:
            logger.error(f"[API] Error handling push request from {client_addr}: {e}", exc_info=True)
            self.send_response(500)
            self.send_header('Content-Type', 'application/json')
            self.end_headers()
            error_response = {
                "status": "error",
                "message": str(e)
            }
            self.wfile.write(json.dumps(error_response, indent=2).encode('utf-8'))
    
    def log_message(self, format, *args):
        """Override to use our logger instead of default."""
        logger.info(f"{self.address_string()} - {format % args}")


def start_push_api_server(api_port: int, openelis_url: str, fields_config: Dict):
    """Start HTTP API server for triggering pushes."""
    PushAPIHandler.fields_config = fields_config
    PushAPIHandler.openelis_url = openelis_url
    
    server = HTTPServer(('0.0.0.0', api_port), PushAPIHandler)
    logger.info(f"Push API server started on port {api_port}")
    logger.info(f"  POST /push - Trigger push to OpenELIS")
    logger.info(f"  GET /health - Health check")
    
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        logger.info("Push API server stopped")
        server.shutdown()


def push_to_openelis(openelis_url: str, astm_message: str, timeout: int = 30) -> bool:
    """
    Push an ASTM message to OpenELIS via HTTP POST.
    
    Args:
        openelis_url: Base URL for OpenELIS (e.g., "https://localhost:8443")
        astm_message: Complete ASTM message as string
        timeout: Request timeout in seconds
    
    Returns:
        True if push succeeded, False otherwise
    """
    endpoint = f"{openelis_url}/api/OpenELIS-Global/analyzer/astm"
    
    try:
        message_lines = astm_message.strip().split('\n')
        message_type = "unknown"
        if message_lines:
            first_line = message_lines[0]
            if first_line.startswith('H|'):
                # Try to extract analyzer info from header
                parts = first_line.split('|')
                if len(parts) >= 5:
                    analyzer_info = parts[4] if parts[4] else "Unknown"
                    message_type = f"ASTM message from {analyzer_info}"
        
        logger.info(f"[PUSH] Pushing ASTM message to {endpoint}")
        logger.info(f"[PUSH] Message type: {message_type}, size: {len(astm_message)} bytes, lines: {len(message_lines)}")
        logger.debug(f"[PUSH] Message preview (first 300 chars): {astm_message[:300]}...")
        
        # Create HTTP request
        req = urllib.request.Request(
            endpoint,
            data=astm_message.encode('utf-8'),
            headers={
                'Content-Type': 'text/plain; charset=utf-8',
            },
            method='POST'
        )
        
        # Disable SSL verification for development (OpenELIS uses self-signed certs)
        import ssl
        ssl_context = ssl.create_default_context()
        ssl_context.check_hostname = False
        ssl_context.verify_mode = ssl.CERT_NONE
        
        # Send request
        start_time = time.time()
        with urllib.request.urlopen(req, timeout=timeout, context=ssl_context) as response:
            elapsed_time = time.time() - start_time
            status_code = response.getcode()
            response_body = response.read().decode('utf-8')
            
            if status_code == 200:
                logger.info(f"[PUSH] ✓ Push successful (HTTP {status_code}) in {elapsed_time:.2f}s")
                if response_body:
                    logger.debug(f"[PUSH] Response body: {response_body[:200]}")
                return True
            else:
                logger.error(f"[PUSH] ✗ Push failed: HTTP {status_code}")
                if response_body:
                    logger.error(f"[PUSH] Response: {response_body[:500]}")
                return False
                
    except urllib.error.HTTPError as e:
        error_body = e.read().decode('utf-8') if e.fp else "No error details"
        logger.error(f"[PUSH] ✗ HTTP error {e.code}: {e.reason}")
        logger.error(f"[PUSH] Error response: {error_body[:500]}")
        return False
    except urllib.error.URLError as e:
        logger.error(f"[PUSH] ✗ URL error: {e.reason}")
        return False
    except Exception as e:
        logger.error(f"[PUSH] ✗ Push failed with exception: {e}", exc_info=True)
        return False


def main():
    """Main entry point."""
    parser = argparse.ArgumentParser(
        description='ASTM LIS2-A2 Mock Server for OpenELIS Analyzer Testing'
    )
    parser.add_argument(
        '--port', '-p',
        type=int,
        default=int(os.environ.get('ASTM_PORT', DEFAULT_PORT)),
        help=f'Server port (default: {DEFAULT_PORT})'
    )
    parser.add_argument(
        '--analyzer-type', '-t',
        type=str,
        default=os.environ.get('ANALYZER_TYPE', DEFAULT_ANALYZER_TYPE),
        help=f'Analyzer type (default: {DEFAULT_ANALYZER_TYPE})'
    )
    parser.add_argument(
        '--response-delay', '-d',
        type=int,
        default=int(os.environ.get('RESPONSE_DELAY_MS', DEFAULT_RESPONSE_DELAY_MS)),
        help=f'Response delay in ms (default: {DEFAULT_RESPONSE_DELAY_MS})'
    )
    parser.add_argument(
        '--verbose', '-v',
        action='store_true',
        help='Enable verbose logging'
    )
    parser.add_argument(
        '--push', '-P',
        type=str,
        metavar='URL',
        help='Push mode: Send ASTM message to OpenELIS at URL (e.g., https://localhost:8443)'
    )
    parser.add_argument(
        '--push-count', '-c',
        type=int,
        default=1,
        help='Number of messages to push (default: 1)'
    )
    parser.add_argument(
        '--push-interval', '-i',
        type=int,
        default=1,
        help='Interval between pushes in seconds (default: 1)'
    )
    parser.add_argument(
        '--push-continuous', '-C',
        action='store_true',
        help='Continuous push mode: Push messages indefinitely at specified interval (simulates real analyzer behavior)'
    )
    parser.add_argument(
        '--api-port', '-a',
        type=int,
        metavar='PORT',
        help='Start HTTP API server on specified port for triggering pushes (e.g., 8080)'
    )
    
    args = parser.parse_args()
    
    if args.verbose:
        logging.getLogger().setLevel(logging.DEBUG)
    
    # Push mode: Send messages to OpenELIS
    if args.push:
        print("=" * 60)
        print("  ASTM Mock Server - Push Mode")
        print("=" * 60)
        print(f"  OpenELIS URL: {args.push}")
        print(f"  Analyzer Type: {args.analyzer_type}")
        print(f"  Message Count: {args.push_count}")
        print(f"  Interval: {args.push_interval}s")
        print("=" * 60)
        print()
        
        # Load fields configuration
        fields_config = {}
        fields_file = os.path.join(os.path.dirname(__file__), 'fields.json')
        if os.path.exists(fields_file):
            try:
                with open(fields_file, 'r') as f:
                    fields_config = json.load(f)
            except Exception as e:
                logger.error(f"Error loading fields.json: {e}")
                return 1
        
        if not fields_config:
            logger.error("No fields configuration available")
            return 1
        
        # Push messages
        success_count = 0
        total_pushed = 0
        
        if args.push_continuous:
            # Continuous mode: Push indefinitely (simulates real analyzer behavior)
            logger.info("Starting continuous push mode (press Ctrl+C to stop)")
            try:
                while True:
                    total_pushed += 1
                    logger.info(f"Generating and pushing message #{total_pushed}")
                    
                    # Generate ASTM message
                    message = generate_astm_message(
                        analyzer_type=args.analyzer_type,
                        fields_config=fields_config
                    )
                    
                    if not message:
                        logger.error("Failed to generate message")
                        time.sleep(args.push_interval)
                        continue
                    
                    # Push to OpenELIS
                    if push_to_openelis(args.push, message):
                        success_count += 1
                    else:
                        logger.warning(f"Push #{total_pushed} failed")
                    
                    # Wait before next push
                    time.sleep(args.push_interval)
            except KeyboardInterrupt:
                logger.info("Continuous push mode stopped by user")
        else:
            # Batch mode: Push specified number of messages
            for i in range(args.push_count):
                total_pushed += 1
                logger.info(f"Generating and pushing message {i+1}/{args.push_count}")
                
                # Generate ASTM message
                message = generate_astm_message(
                    analyzer_type=args.analyzer_type,
                    fields_config=fields_config
                )
                
                if not message:
                    logger.error("Failed to generate message")
                    continue
                
                # Push to OpenELIS
                if push_to_openelis(args.push, message):
                    success_count += 1
                else:
                    logger.warning(f"Push {i+1} failed")
                
                # Wait before next push (except for last one)
                if i < args.push_count - 1:
                    time.sleep(args.push_interval)
        
        print()
        print("=" * 60)
        if args.push_continuous:
            print(f"  Push Complete: {success_count}/{total_pushed} successful")
        else:
            print(f"  Push Complete: {success_count}/{args.push_count} successful")
        print("=" * 60)
        return 0 if success_count == total_pushed else 1
    
    # API server mode: Start HTTP API for triggering pushes
    if args.api_port:
        if not args.push:
            logger.error("--api-port requires --push URL to be specified")
            return 1
        
        # Load fields configuration
        fields_config = {}
        fields_file = os.path.join(os.path.dirname(__file__), 'fields.json')
        if os.path.exists(fields_file):
            try:
                with open(fields_file, 'r') as f:
                    fields_config = json.load(f)
            except Exception as e:
                logger.error(f"Error loading fields.json: {e}")
                return 1
        
        if not fields_config:
            logger.error("No fields configuration available")
            return 1
        
        print("=" * 60)
        print("  ASTM Mock Server - API Mode")
        print("=" * 60)
        print(f"  API Port: {args.api_port}")
        print(f"  OpenELIS URL: {args.push}")
        print(f"  Default Analyzer Type: {args.analyzer_type}")
        print("=" * 60)
        print("  Endpoints:")
        print(f"    POST http://localhost:{args.api_port}/push")
        print(f"    GET  http://localhost:{args.api_port}/health")
        print("=" * 60)
        print("  Press Ctrl+C to stop")
        print("=" * 60)
        print()
        
        start_push_api_server(args.api_port, args.push, fields_config)
        return 0
    
    # Server mode: Listen for connections
    print("=" * 60)
    print("  ASTM LIS2-A2 Mock Server for OpenELIS")
    print("=" * 60)
    print(f"  Port: {args.port}")
    print(f"  Analyzer Type: {args.analyzer_type}")
    print(f"  Response Delay: {args.response_delay}ms")
    print("=" * 60)
    print("  Press Ctrl+C to stop")
    print("=" * 60)
    print()
    
    server = ASTMMockServer(
        port=args.port,
        analyzer_type=args.analyzer_type,
        response_delay_ms=args.response_delay
    )
    server.start()


if __name__ == '__main__':
    main()


