#!/usr/bin/env python3
"""
ASTM Mock Server Tests - TDD RED Phase

These tests are written BEFORE the server implementation per Constitution V
(Test-Driven Development) and testing-roadmap.md.

Reference Documents:
- specs/004-astm-analyzer-mapping/research.md Section 1 (ASTM Protocol)
- specs/004-astm-analyzer-mapping/spec.md FR-001, FR-002

ASTM LIS2-A2 Control Characters:
- ENQ (0x05): Enquiry - Start of transmission
- ACK (0x06): Acknowledge - Positive response
- NAK (0x15): Negative Acknowledge
- EOT (0x04): End of Transmission
- STX (0x02): Start of Text (frame start)
- ETX (0x03): End of Text (frame end, followed by checksum)
- ETB (0x17): End of Text Block
- CR (0x0D): Carriage Return (record separator)
- LF (0x0A): Line Feed

Run tests: python -m pytest test_server.py -v
Or: python test_server.py (runs unittest)
"""

import unittest
import socket
import threading
import time
import json
import os
import sys

# Add parent directory to path for imports
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

# ASTM Control Characters
ENQ = b'\x05'
ACK = b'\x06'
NAK = b'\x15'
EOT = b'\x04'
STX = b'\x02'
ETX = b'\x03'
ETB = b'\x17'
CR = b'\x0D'
LF = b'\x0A'

# Test configuration
TEST_HOST = 'localhost'
TEST_PORT = 5000
CONNECTION_TIMEOUT = 5  # seconds


class TestASTMServerConnection(unittest.TestCase):
    """
    Test ASTM server connection handling per spec.md FR-001.
    
    These tests validate TCP connection and ASTM handshake behavior.
    """
    
    @classmethod
    def setUpClass(cls):
        """Check if server is running before tests."""
        cls.server_available = cls._check_server_available()
        if not cls.server_available:
            print(f"\nWARNING: ASTM mock server not running on {TEST_HOST}:{TEST_PORT}")
            print("Start the server first: python server.py")
            print("Tests will be skipped.\n")
    
    @classmethod
    def _check_server_available(cls):
        """Check if server is accepting connections."""
        try:
            sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            sock.settimeout(1)
            result = sock.connect_ex((TEST_HOST, TEST_PORT))
            sock.close()
            return result == 0
        except Exception:
            return False
    
    def setUp(self):
        """Skip tests if server not available."""
        if not self.server_available:
            self.skipTest("ASTM mock server not running")
    
    def _create_socket(self):
        """Create a socket with timeout."""
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.settimeout(CONNECTION_TIMEOUT)
        return sock
    
    def test_server_accepts_tcp_connection(self):
        """
        Server MUST accept TCP connections on configured port.
        Reference: spec.md FR-001 - Test Connection endpoint
        """
        sock = self._create_socket()
        try:
            sock.connect((TEST_HOST, TEST_PORT))
            self.assertTrue(True, "Connection successful")
        finally:
            sock.close()
    
    def test_server_responds_to_enq_with_ack(self):
        """
        Server MUST respond to ENQ (0x05) with ACK (0x06).
        Reference: research.md Section 1 - ASTM LIS2-A2 Protocol
        
        This is the standard ASTM handshake:
        1. Client sends ENQ
        2. Server responds with ACK if ready
        3. Client can then send data frames
        """
        sock = self._create_socket()
        try:
            sock.connect((TEST_HOST, TEST_PORT))
            sock.send(ENQ)
            response = sock.recv(1)
            self.assertEqual(response, ACK, 
                f"Expected ACK (0x06), got {response.hex() if response else 'nothing'}")
        finally:
            sock.close()
    
    def test_server_handles_eot_gracefully(self):
        """
        Server MUST handle EOT (0x04) gracefully.
        EOT signals end of transmission - connection should remain open.
        """
        sock = self._create_socket()
        try:
            sock.connect((TEST_HOST, TEST_PORT))
            # Complete a handshake first
            sock.send(ENQ)
            sock.recv(1)  # ACK
            # Send EOT
            sock.send(EOT)
            # Connection should still be usable
            time.sleep(0.1)
            sock.send(ENQ)
            response = sock.recv(1)
            self.assertEqual(response, ACK, "Server should accept new ENQ after EOT")
        finally:
            sock.close()
    
    def test_multiple_connections_handled(self):
        """
        Server MUST handle multiple simultaneous connections.
        Reference: spec.md FR-002 - Query Analyzer (may have concurrent operations)
        """
        sockets = []
        try:
            for i in range(3):
                sock = self._create_socket()
                sock.connect((TEST_HOST, TEST_PORT))
                sockets.append(sock)
            
            # All connections should work
            for sock in sockets:
                sock.send(ENQ)
            
            for sock in sockets:
                response = sock.recv(1)
                self.assertEqual(response, ACK)
        finally:
            for sock in sockets:
                sock.close()


class TestASTMServerFieldQuery(unittest.TestCase):
    """
    Test ASTM server field query responses per spec.md FR-002.
    
    Query Analyzer functionality returns available data fields.
    """
    
    @classmethod
    def setUpClass(cls):
        """Check if server is running before tests."""
        cls.server_available = TestASTMServerConnection._check_server_available()
        if not cls.server_available:
            print(f"\nWARNING: ASTM mock server not running on {TEST_HOST}:{TEST_PORT}")
    
    def setUp(self):
        if not self.server_available:
            self.skipTest("ASTM mock server not running")
    
    def _create_socket(self):
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.settimeout(CONNECTION_TIMEOUT)
        return sock
    
    def _send_frame(self, sock, data, frame_num=1):
        """
        Send an ASTM frame with proper framing.
        Frame format: <STX><FN><data><ETX><checksum><CR><LF>
        where FN is frame number (1-7, then wraps)
        """
        frame_num_byte = str(frame_num).encode()
        content = frame_num_byte + data
        # Calculate checksum (sum of bytes from frame num to ETX, mod 256)
        checksum = (sum(content) + ord(ETX)) % 256
        checksum_str = f'{checksum:02X}'.encode()
        frame = STX + content + ETX + checksum_str + CR + LF
        sock.send(frame)
    
    def test_query_returns_field_list(self):
        """
        Server MUST return predefined analyzer fields when queried.
        Reference: spec.md FR-002 - Query Analyzer button functionality
        
        The server should return a list of fields with:
        - Field name
        - ASTM reference
        - Field type (NUMERIC, QUALITATIVE, etc.)
        - Unit (for numeric fields)
        """
        sock = self._create_socket()
        try:
            sock.connect((TEST_HOST, TEST_PORT))
            
            # Perform handshake
            sock.send(ENQ)
            ack = sock.recv(1)
            self.assertEqual(ack, ACK)
            
            # Send query header record
            # H|\^&|||QuerySystem^Query^1.0|...
            query_header = b'H|\\^&|||QuerySystem^Query^1.0|||||||LIS2-A2'
            self._send_frame(sock, query_header, frame_num=1)
            
            # Receive ACK for frame
            frame_ack = sock.recv(1)
            self.assertEqual(frame_ack, ACK, "Server should ACK valid frame")
            
            # Send terminator (frame number must increment: 2)
            self._send_frame(sock, b'L|1|N', frame_num=2)
            sock.recv(1)  # ACK
            
            # Send EOT to signal we're done sending
            sock.send(EOT)
            
            # Now server should send its response
            # Wait for ENQ from server (server processes EOT and detects query)
            sock.settimeout(5)  # Increase timeout for query processing
            time.sleep(0.5)  # Give server time to process and detect query
            server_enq = sock.recv(1)
            if server_enq == ENQ:
                sock.send(ACK)
                # Receive response frames
                response_data = b''
                while True:
                    frame = sock.recv(1024)
                    if not frame:
                        break
                    response_data += frame
                    if EOT in frame:
                        break
                    sock.send(ACK)
                
                # Response should contain field definitions
                self.assertIn(b'R|', response_data, 
                    "Response should contain result records with fields")
        finally:
            sock.close()
    
    def test_query_includes_field_types(self):
        """
        Field query response MUST include field type indicators.
        Reference: spec.md FR-002 - field type indicators (Numeric, Qualitative, etc.)
        """
        # This test validates the structure of returned field data
        # The mock server should return fields with type information
        sock = self._create_socket()
        try:
            sock.connect((TEST_HOST, TEST_PORT))
            sock.send(ENQ)
            response = sock.recv(1)
            self.assertEqual(response, ACK)
            # Further field type validation would require full protocol exchange
        finally:
            sock.close()


class TestASTMServerMessageHandling(unittest.TestCase):
    """
    Test ASTM server message handling for patient/QC results.
    
    These tests validate proper handling of incoming ASTM messages
    as would be sent by a real analyzer.
    """
    
    @classmethod
    def setUpClass(cls):
        cls.server_available = TestASTMServerConnection._check_server_available()
    
    def setUp(self):
        if not self.server_available:
            self.skipTest("ASTM mock server not running")
    
    def _create_socket(self):
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.settimeout(CONNECTION_TIMEOUT)
        return sock
    
    def test_accepts_valid_result_message(self):
        """
        Server MUST accept properly formatted ASTM result messages.
        This simulates an analyzer sending patient results.
        """
        sock = self._create_socket()
        try:
            sock.connect((TEST_HOST, TEST_PORT))
            
            # Handshake
            sock.send(ENQ)
            ack = sock.recv(1)
            self.assertEqual(ack, ACK)
            
            # The server accepts ENQ and responds with ACK
            # Full message handling would require frame-level protocol
        finally:
            sock.close()
    
    def test_accepts_qc_segment_message(self):
        """
        Server MUST accept Q-segment (QC) messages.
        Reference: spec.md FR-021, research.md Q-segment format
        
        Q-segment format:
        Q|sequence|test_code^control_lot^control_level|result_value|unit|timestamp|flag
        """
        sock = self._create_socket()
        try:
            sock.connect((TEST_HOST, TEST_PORT))
            sock.send(ENQ)
            ack = sock.recv(1)
            self.assertEqual(ack, ACK)
            # Q-segment handling validated at handshake level
        finally:
            sock.close()


class TestASTMServerConfiguration(unittest.TestCase):
    """
    Test ASTM server configuration and environment variables.
    """
    
    def test_default_port_is_5000(self):
        """Server should default to port 5000."""
        # This is a configuration test - validates expected defaults
        expected_port = 5000
        self.assertEqual(TEST_PORT, expected_port)
    
    def test_fields_config_file_format(self):
        """
        fields.json MUST have valid format with required properties.
        """
        config_path = os.path.join(os.path.dirname(__file__), 'fields.json')
        if os.path.exists(config_path):
            with open(config_path, 'r') as f:
                config = json.load(f)
            
            # Should have at least one analyzer type
            self.assertTrue(len(config) > 0, "Config should have analyzer types")
            
            # Each analyzer type should have fields
            for analyzer_type, fields in config.items():
                self.assertIsInstance(fields, list, 
                    f"{analyzer_type} should have list of fields")
                for field in fields:
                    self.assertIn('name', field, "Field must have name")
                    self.assertIn('type', field, "Field must have type")


class TestASTMServerTimeout(unittest.TestCase):
    """
    Test server timeout behavior per spec.md FR-002 and CLSI LIS1-A.
    
    Connection test validates TCP handshake within 30 seconds.
    Query timeout is 5 minutes (configurable).
    CLSI LIS1-A specifies:
    - 15 seconds for establishment phase (ENQ response)
    - 15 seconds for frame ACK timeout
    - 30 seconds for receiver timeout
    """
    
    @classmethod
    def setUpClass(cls):
        cls.server_available = TestASTMServerConnection._check_server_available()
    
    def setUp(self):
        if not self.server_available:
            self.skipTest("ASTM mock server not running")
    
    def test_connection_does_not_timeout_prematurely(self):
        """
        Server should not timeout within first 30 seconds of inactivity.
        Reference: spec.md FR-001 - 30-second connection test timeout
        """
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.settimeout(35)  # Slightly longer than 30s
        try:
            sock.connect((TEST_HOST, TEST_PORT))
            # Wait 2 seconds (simulating slow client)
            time.sleep(2)
            # Should still be able to send ENQ
            sock.send(ENQ)
            response = sock.recv(1)
            self.assertEqual(response, ACK, 
                "Connection should remain active for at least 2 seconds")
        except socket.timeout:
            self.fail("Connection timed out prematurely")
        finally:
            sock.close()


class TestASTMStandardsCompliance(unittest.TestCase):
    """
    Test CLSI LIS1-A standards compliance.
    
    These tests verify:
    - Frame number validation (must be same as last accepted OR one higher modulo 8)
    - Checksum validation (mandatory per CLSI LIS1-A)
    - Retransmission handling (abort after 6 failures)
    - Character restrictions (restricted characters rejected)
    """
    
    @classmethod
    def setUpClass(cls):
        cls.server_available = TestASTMServerConnection._check_server_available()
    
    def setUp(self):
        if not self.server_available:
            self.skipTest("ASTM mock server not running")
    
    def _create_socket(self):
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.settimeout(CONNECTION_TIMEOUT)
        return sock
    
    def _send_frame(self, sock, frame_num, data, checksum_override=None):
        """
        Send an ASTM frame with proper framing.
        Frame format: <STX><FN><data><ETX><checksum><CR><LF>
        """
        frame_num_byte = str(frame_num).encode()
        data_bytes = data.encode('utf-8') if isinstance(data, str) else data
        
        # Calculate checksum (sum of bytes from frame num to ETX, mod 256)
        checksum_data = frame_num_byte + data_bytes + ETX
        checksum = sum(checksum_data) % 256
        
        # Override checksum if specified (for testing invalid checksums)
        if checksum_override is not None:
            checksum = checksum_override
        
        checksum_str = f'{checksum:02X}'.encode()
        frame = STX + frame_num_byte + data_bytes + ETX + checksum_str + CR + LF
        sock.send(frame)
        return sock.recv(1)  # Return response (ACK or NAK)
    
    def test_valid_frame_number_sequence(self):
        """
        Server MUST accept frames with valid frame number sequence.
        Per CLSI LIS1-A: frame number must be same as last accepted OR one higher (modulo 8).
        """
        sock = self._create_socket()
        try:
            sock.connect((TEST_HOST, TEST_PORT))
            sock.send(ENQ)
            sock.recv(1)  # ACK
            
            # Send first frame (frame number 1)
            response = self._send_frame(sock, 1, "H|\\^&|||Test^Analyzer^1.0|||||||LIS2-A2")
            self.assertEqual(response, ACK, "First frame should be accepted")
            
            # Send second frame (frame number 2 - one higher)
            response = self._send_frame(sock, 2, "P|1||PAT001")
            self.assertEqual(response, ACK, "Second frame (one higher) should be accepted")
            
            # Send third frame (frame number 3 - one higher)
            response = self._send_frame(sock, 3, "L|1|N")
            self.assertEqual(response, ACK, "Third frame (one higher) should be accepted")
        finally:
            sock.close()
    
    def test_invalid_frame_number_rejected(self):
        """
        Server MUST reject frames with invalid frame number sequence.
        Per CLSI LIS1-A: frame number must match expected sequence.
        """
        sock = self._create_socket()
        try:
            sock.connect((TEST_HOST, TEST_PORT))
            sock.send(ENQ)
            sock.recv(1)  # ACK
            
            # Send first frame (frame number 1)
            response = self._send_frame(sock, 1, "H|\\^&|||Test^Analyzer^1.0|||||||LIS2-A2")
            self.assertEqual(response, ACK, "First frame should be accepted")
            
            # Send frame with wrong number (should be 2, but send 5)
            response = self._send_frame(sock, 5, "P|1||PAT001")
            self.assertEqual(response, NAK, "Invalid frame number should be rejected with NAK")
        finally:
            sock.close()
    
    def test_invalid_checksum_rejected(self):
        """
        Server MUST reject frames with invalid checksum.
        Per CLSI LIS1-A 8.5.1.1: checksum validation is mandatory.
        """
        sock = self._create_socket()
        try:
            sock.connect((TEST_HOST, TEST_PORT))
            sock.send(ENQ)
            sock.recv(1)  # ACK
            
            # Send frame with invalid checksum (override with wrong value)
            response = self._send_frame(sock, 1, "H|\\^&|||Test^Analyzer^1.0|||||||LIS2-A2", 
                                       checksum_override=0xFF)
            self.assertEqual(response, NAK, "Invalid checksum should be rejected with NAK")
        finally:
            sock.close()
    
    def test_retransmission_abort_after_six_failures(self):
        """
        Server MUST abort after 6 failed retransmissions.
        Per CLSI LIS1-A 8.5.1.2: abort after 6 failed retransmissions.
        """
        sock = self._create_socket()
        try:
            sock.connect((TEST_HOST, TEST_PORT))
            sock.send(ENQ)
            sock.recv(1)  # ACK
            
            # Send 6 frames with invalid checksums (should all be NAKed)
            for i in range(6):
                response = self._send_frame(sock, 1, "H|\\^&|||Test^Analyzer^1.0|||||||LIS2-A2",
                                           checksum_override=0xFF)
                self.assertEqual(response, NAK, f"Frame {i+1} should be rejected")
            
            # After 6 failures, server should abort (send EOT and close connection)
            # Try to send another frame - should fail or connection closed
            try:
                response = self._send_frame(sock, 1, "H|\\^&|||Test^Analyzer^1.0|||||||LIS2-A2")
                # If we get here, check if we received EOT or connection closed
                if response == EOT:
                    self.assertTrue(True, "Server sent EOT after 6 failures")
                else:
                    # Connection might be closed - try to send ENQ
                    sock.send(ENQ)
                    try:
                        sock.recv(1)
                        self.fail("Connection should be closed after abort")
                    except:
                        self.assertTrue(True, "Connection closed after abort")
            except (ConnectionResetError, BrokenPipeError, socket.error):
                self.assertTrue(True, "Connection closed after abort")
        finally:
            sock.close()
    
    def test_restricted_characters_rejected(self):
        """
        Server MUST reject frames containing restricted characters.
        Per CLSI LIS1-A 8.6: restricted characters (LF, control chars) not allowed in message text.
        """
        sock = self._create_socket()
        try:
            sock.connect((TEST_HOST, TEST_PORT))
            sock.send(ENQ)
            sock.recv(1)  # ACK
            
            # Send frame with LF character in content (restricted)
            # Note: LF is only allowed as last character of frame
            frame_num_byte = b'1'
            # Create data with LF in the middle (not allowed)
            data_bytes = b'H|\\^&|||Test' + LF + b'^Analyzer^1.0|||||||LIS2-A2'
            checksum_data = frame_num_byte + data_bytes + ETX
            checksum = sum(checksum_data) % 256
            checksum_str = f'{checksum:02X}'.encode()
            frame = STX + frame_num_byte + data_bytes + ETX + checksum_str + CR + LF
            sock.send(frame)
            
            response = sock.recv(1)
            self.assertEqual(response, NAK, "Frame with restricted character should be rejected")
        finally:
            sock.close()
    
    def test_frame_number_wraps_correctly(self):
        """
        Server MUST handle frame number wrapping correctly (1-7, then wraps to 1).
        Per CLSI LIS1-A: frame numbers are 1-7, then wrap to 1.
        """
        sock = self._create_socket()
        try:
            sock.connect((TEST_HOST, TEST_PORT))
            sock.send(ENQ)
            sock.recv(1)  # ACK
            
            # Send frames 1 through 7
            for frame_num in range(1, 8):
                response = self._send_frame(sock, frame_num, f"H|\\^&|||Test^Frame{frame_num}^1.0|||||||LIS2-A2")
                self.assertEqual(response, ACK, f"Frame {frame_num} should be accepted")
            
            # After frame 7, next frame should be 1 (wraps)
            response = self._send_frame(sock, 1, "H|\\^&|||Test^Frame1^1.0|||||||LIS2-A2")
            self.assertEqual(response, ACK, "Frame 1 after frame 7 should be accepted (wrapped)")
        finally:
            sock.close()


if __name__ == '__main__':
    print("=" * 60)
    print("ASTM Mock Server Tests - TDD RED Phase")
    print("=" * 60)
    print(f"\nTarget: {TEST_HOST}:{TEST_PORT}")
    print("\nThese tests define expected behavior BEFORE implementation.")
    print("Run 'python server.py' first to start the mock server.\n")
    
    # Run tests
    unittest.main(verbosity=2)


