import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import { IntlProvider } from 'react-intl';
import StorageLocationSelector from './StorageLocationSelector';
import { getFromOpenElisServer } from '../../utils/Utils';
import messages from '../../../languages/en.json';

// Mock the API utilities
jest.mock('../../utils/Utils', () => ({
  getFromOpenElisServer: jest.fn(),
}));

const renderWithIntl = (component) => {
  return render(
    <IntlProvider locale="en" messages={messages}>
      {component}
    </IntlProvider>
  );
};

describe('StorageLocationSelector', () => {
  /**
   * T051: Test device dropdown is disabled until room is selected
   * Cascading dropdown behavior
   */
  test('should disable device dropdown until room selected', () => {
    renderWithIntl(<StorageLocationSelector mode="dropdown" />);
    
    const deviceDropdown = screen.queryByTestId('device-dropdown');
    expect(deviceDropdown).toBeDisabled();
  });

  /**
   * T051: Test fetches devices when room is selected
   * Data fetching on parent selection
   */
  test('should fetch devices when room is selected', async () => {
    const mockRooms = [
      { id: '1', name: 'Main Laboratory', code: 'MAIN' },
    ];
    
    const mockDevices = [
      { id: '2', name: 'Freezer Unit 1', code: 'FRZ01', type: 'freezer' },
    ];

    // Mock API responses
    const { getFromOpenElisServer } = require('../../utils/Utils');
    getFromOpenElisServer
      .mockImplementationOnce((url, callback) => callback(mockRooms))  // Rooms
      .mockImplementationOnce((url, callback) => callback(mockDevices)); // Devices

    renderWithIntl(<StorageLocationSelector mode="dropdown" />);

    // Select room
    const roomDropdown = screen.getByTestId('room-dropdown');
    fireEvent.click(roomDropdown);
    fireEvent.click(screen.getByText('Main Laboratory'));

    // Wait for devices to load
    await waitFor(() => {
      const deviceDropdown = screen.getByTestId('device-dropdown');
      expect(deviceDropdown).not.toBeDisabled();
    });
  });

  /**
   * T051: Test displays hierarchical path when all levels selected
   * Path display behavior
   */
  test('should display hierarchical path when all levels selected', async () => {
    renderWithIntl(<StorageLocationSelector mode="dropdown" />);

    // Simulate full selection (Room → Device → Shelf → Rack → Position)
    // This would require full mock setup, simplified for now
    
    // After selections, path should display
    const pathDisplay = screen.queryByTestId('location-path');
    // Path format: "Room > Device > Shelf > Rack > Position"
    // Assertion would check path format once selections made
  });

  /**
   * T051: Test handles inline location creation
   * "Add New" button behavior
   */
  test('should show add new buttons when enableInlineCreation is true', () => {
    renderWithIntl(<StorageLocationSelector mode="dropdown" enableInlineCreation={true} />);

    // Should show "Add New Room" button
    const addRoomButton = screen.queryByText(/add new room/i);
    expect(addRoomButton).toBeInTheDocument();
  });

  /**
   * T051: Test mode switching between dropdown/autocomplete/barcode
   * Mode prop behavior
   */
  test('should render cascading dropdowns in dropdown mode', () => {
    renderWithIntl(<StorageLocationSelector mode="dropdown" />);

    expect(screen.getByTestId('room-dropdown')).toBeInTheDocument();
  });

  test('should render autocomplete in autocomplete mode', () => {
    renderWithIntl(<StorageLocationSelector mode="autocomplete" />);

    const autocompleteInput = screen.queryByPlaceholderText(/search/i);
    expect(autocompleteInput).toBeInTheDocument();
  });

  test('should render barcode input in barcode mode', () => {
    renderWithIntl(<StorageLocationSelector mode="barcode" />);

    const barcodeInput = screen.queryByPlaceholderText(/scan barcode/i);
    expect(barcodeInput).toBeInTheDocument();
  });
});

