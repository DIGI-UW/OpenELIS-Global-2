#!/usr/bin/env python3
"""
ASTM Mock Server Communication Test

This script tests the ASTM mock server and demonstrates a typical
ASTM LIS2-A2 communication pathway.

Usage:
    python test_communication.py [--host HOST] [--port PORT]

Example:
    python test_communication.py --host localhost --port 5000
"""

import socket
import sys
import argparse
import time

# ASTM LIS2-A2 Control Characters
ENQ = b'\x05'  # Enquiry - Start transmission
ACK = b'\x06'  # Acknowledge - Positive response
NAK = b'\x15'  # Negative Acknowledge
EOT = b'\x04'  # End of Transmission
STX = b'\x02'  # Start of Text (frame start)
ETX = b'\x03'  # End of Text (frame end)
ETB = b'\x17'  # End of Text Block (intermediate frame)
CR = b'\x0D'   # Carriage Return
LF = b'\x0A'   # Line Feed

# Default connection settings
DEFAULT_HOST = 'localhost'
DEFAULT_PORT = 5000
CONNECTION_TIMEOUT = 5


def print_step(step_num, description):
    """Print a formatted step description."""
    print(f"\n{'='*60}")
    print(f"STEP {step_num}: {description}")
    print(f"{'='*60}")


def send_frame(sock, frame_num, data):
    """
    Send an ASTM frame with proper framing.
    
    Frame format: <STX><FN><data><ETX><checksum><CR><LF>
    where FN is frame number (1-7, then wraps)
    """
    frame_num_byte = str(frame_num).encode()
    data_bytes = data.encode('utf-8') if isinstance(data, str) else data
    
    # Calculate checksum (sum of bytes from frame num to ETX, mod 256)
    checksum_data = frame_num_byte + data_bytes + ETX
    checksum = sum(checksum_data) % 256
    checksum_str = f'{checksum:02X}'.encode()
    
    frame = STX + frame_num_byte + data_bytes + ETX + checksum_str + CR + LF
    sock.send(frame)
    
    print(f"  → Sent frame {frame_num}: {data[:50]}...")
    
    # Wait for ACK
    try:
        response = sock.recv(1)
        if response == ACK:
            print(f"  ← Received ACK for frame {frame_num}")
            return True
        else:
            print(f"  ✗ Unexpected response: {response.hex()}")
            return False
    except socket.timeout:
        print(f"  ✗ Timeout waiting for ACK")
        return False


def test_basic_connection(host, port):
    """Test 1: Basic TCP connection and ENQ/ACK handshake."""
    print_step(1, "Basic Connection & Handshake")
    
    try:
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.settimeout(CONNECTION_TIMEOUT)
        sock.connect((host, port))
        print(f"  ✓ Connected to {host}:{port}")
        
        # Send ENQ
        print(f"  → Sending ENQ (0x05)...")
        sock.send(ENQ)
        
        # Receive ACK
        response = sock.recv(1)
        if response == ACK:
            print(f"  ← Received ACK (0x06) - Handshake successful!")
            sock.close()
            return True
        else:
            print(f"  ✗ Unexpected response: {response.hex()}")
            sock.close()
            return False
            
    except Exception as e:
        print(f"  ✗ Connection failed: {e}")
        return False


def test_typical_communication_pathway(host, port):
    """
    Test 2: Typical ASTM LIS2-A2 Communication Pathway
    
    This demonstrates the standard flow:
    1. Client sends ENQ
    2. Server responds ACK
    3. Client sends data frames (H, P, O, R, L records)
    4. Server ACKs each frame
    5. Client sends EOT to end transmission
    """
    print_step(2, "Typical Communication Pathway")
    print("\n  This test demonstrates a complete ASTM message exchange:")
    print("  - Header (H) record")
    print("  - Patient (P) record")
    print("  - Order (O) record")
    print("  - Result (R) records")
    print("  - Terminator (L) record")
    
    try:
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.settimeout(CONNECTION_TIMEOUT)
        sock.connect((host, port))
        print(f"\n  ✓ Connected to {host}:{port}")
        
        # Step 2.1: Handshake
        print(f"\n  2.1 Handshake:")
        print(f"    → Sending ENQ...")
        sock.send(ENQ)
        response = sock.recv(1)
        if response != ACK:
            print(f"    ✗ Expected ACK, got {response.hex()}")
            sock.close()
            return False
        print(f"    ← Received ACK - Ready to send data")
        
        # Step 2.2: Send Header Record
        print(f"\n  2.2 Sending Header Record (H):")
        frame_num = 1
        header = "H|\\^&|||Sysmex^XN-1000^V1.0|||||||LIS2-A2|20250115080000"
        if not send_frame(sock, frame_num, header):
            sock.close()
            return False
        
        # Step 2.3: Send Patient Record
        print(f"\n  2.3 Sending Patient Record (P):")
        frame_num = (frame_num % 7) + 1  # Frame numbers wrap: 1-7, then 1
        patient = "P|1||PAT-2025-001|Smith^John^A||M|19850315|WHITE|123 Main St^^Anytown^ST^12345"
        if not send_frame(sock, frame_num, patient):
            sock.close()
            return False
        
        # Step 2.4: Send Order Record
        print(f"\n  2.4 Sending Order Record (O):")
        frame_num = (frame_num % 7) + 1  # Frame numbers wrap: 1-7, then 1
        order = "O|1|SAMPLE-2025-0001^LAB|CBC^Complete Blood Count||20250115075500||||||||||||||||||||"
        if not send_frame(sock, frame_num, order):
            sock.close()
            return False
        
        # Step 2.5: Send Result Records
        print(f"\n  2.5 Sending Result Records (R):")
        results = [
            "R|1|^^^WBC^White Blood Cell Count|5.8|10^3/μL|4.5-11.0|N||F|20250115080100",
            "R|2|^^^RBC^Red Blood Cell Count|4.92|10^6/μL|4.5-5.5|N||F|20250115080100",
            "R|3|^^^HGB^Hemoglobin|14.8|g/dL|13.5-17.5|N||F|20250115080100",
        ]
        
        for result in results:
            frame_num = (frame_num % 7) + 1  # Frame numbers wrap: 1-7, then 1
            if not send_frame(sock, frame_num, result):
                sock.close()
                return False
        
        # Step 2.6: Send Terminator Record
        print(f"\n  2.6 Sending Terminator Record (L):")
        frame_num = (frame_num % 7) + 1  # Frame numbers wrap: 1-7, then 1
        terminator = "L|1|N"
        if not send_frame(sock, frame_num, terminator):
            sock.close()
            return False
        
        # Step 2.7: End Transmission
        print(f"\n  2.7 Ending Transmission:")
        print(f"    → Sending EOT (0x04)...")
        sock.send(EOT)
        print(f"    ✓ Message transmission complete")
        
        sock.close()
        print(f"\n  ✓ Complete ASTM message sent successfully!")
        return True
        
    except Exception as e:
        print(f"  ✗ Communication failed: {e}")
        return False


def test_qc_segment(host, port):
    """Test 3: QC (Quality Control) Segment Communication."""
    print_step(3, "QC Segment Communication")
    print("\n  This test demonstrates QC result transmission using Q-segments")
    
    try:
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.settimeout(CONNECTION_TIMEOUT)
        sock.connect((host, port))
        print(f"\n  ✓ Connected to {host}:{port}")
        
        # Handshake
        sock.send(ENQ)
        response = sock.recv(1)
        if response != ACK:
            print(f"  ✗ Handshake failed")
            sock.close()
            return False
        
        # Send Header
        frame_num = 1
        header = "H|\\^&|||Beckman^AU5800^V2.1|||||||LIS2-A2|20250115060000"
        if not send_frame(sock, frame_num, header):
            sock.close()
            return False
        
        # Send QC Records
        print(f"\n  Sending QC Records (Q):")
        qc_records = [
            "Q|1|GLUCOSE^QC-CHEM-2025A^NORMAL|102.5|mg/dL|20250115060100|N",
            "Q|2|GLUCOSE^QC-CHEM-2025A^HIGH|285.5|mg/dL|20250115060200|N",
            "Q|3|GLUCOSE^QC-CHEM-2025A^LOW|58.2|mg/dL|20250115060300|N",
        ]
        
        for qc in qc_records:
            frame_num = (frame_num % 7) + 1  # Frame numbers wrap: 1-7, then 1
            if not send_frame(sock, frame_num, qc):
                sock.close()
                return False
        
        # Terminator
        frame_num = (frame_num % 7) + 1  # Frame numbers wrap: 1-7, then 1
        if not send_frame(sock, frame_num, "L|1|N"):
            sock.close()
            return False
        
        sock.send(EOT)
        sock.close()
        print(f"\n  ✓ QC segments sent successfully!")
        return True
        
    except Exception as e:
        print(f"  ✗ QC test failed: {e}")
        return False


def test_multiple_connections(host, port):
    """Test 4: Multiple simultaneous connections."""
    print_step(4, "Multiple Simultaneous Connections")
    print("\n  Testing server's ability to handle concurrent connections")
    
    sockets = []
    try:
        # Create 3 connections
        for i in range(3):
            sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            sock.settimeout(CONNECTION_TIMEOUT)
            sock.connect((host, port))
            sockets.append(sock)
            print(f"  ✓ Connection {i+1} established")
        
        # All send ENQ simultaneously
        print(f"\n  Sending ENQ from all connections...")
        for sock in sockets:
            sock.send(ENQ)
        
        # All should receive ACK
        all_acked = True
        for i, sock in enumerate(sockets):
            response = sock.recv(1)
            if response == ACK:
                print(f"  ✓ Connection {i+1} received ACK")
            else:
                print(f"  ✗ Connection {i+1} unexpected response: {response.hex()}")
                all_acked = False
        
        # Cleanup
        for sock in sockets:
            sock.close()
        
        if all_acked:
            print(f"\n  ✓ All connections handled successfully!")
            return True
        else:
            return False
            
    except Exception as e:
        print(f"  ✗ Multiple connection test failed: {e}")
        for sock in sockets:
            try:
                sock.close()
            except:
                pass
        return False


def main():
    """Main test execution."""
    parser = argparse.ArgumentParser(
        description='Test ASTM Mock Server Communication'
    )
    parser.add_argument(
        '--host', '-H',
        default=DEFAULT_HOST,
        help=f'Server host (default: {DEFAULT_HOST})'
    )
    parser.add_argument(
        '--port', '-p',
        type=int,
        default=DEFAULT_PORT,
        help=f'Server port (default: {DEFAULT_PORT})'
    )
    
    args = parser.parse_args()
    
    print("="*60)
    print("  ASTM Mock Server Communication Test")
    print("="*60)
    print(f"\nTarget: {args.host}:{args.port}")
    print("\nThis test demonstrates typical ASTM LIS2-A2 communication")
    print("and verifies the mock server responds correctly.\n")
    
    results = []
    
    # Run all tests
    results.append(("Basic Connection", test_basic_connection(args.host, args.port)))
    time.sleep(0.5)  # Brief pause between tests
    
    results.append(("Typical Pathway", test_typical_communication_pathway(args.host, args.port)))
    time.sleep(0.5)
    
    results.append(("QC Segments", test_qc_segment(args.host, args.port)))
    time.sleep(0.5)
    
    results.append(("Multiple Connections", test_multiple_connections(args.host, args.port)))
    
    # Summary
    print("\n" + "="*60)
    print("  TEST SUMMARY")
    print("="*60)
    
    passed = sum(1 for _, result in results if result)
    total = len(results)
    
    for test_name, result in results:
        status = "✓ PASS" if result else "✗ FAIL"
        print(f"  {status}: {test_name}")
    
    print(f"\n  Results: {passed}/{total} tests passed")
    
    if passed == total:
        print("\n  ✅ All tests passed!")
        return 0
    else:
        print("\n  ❌ Some tests failed")
        return 1


if __name__ == '__main__':
    sys.exit(main())


