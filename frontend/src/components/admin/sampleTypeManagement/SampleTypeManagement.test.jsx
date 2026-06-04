import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { IntlProvider } from 'react-intl';
import SampleTypeManagement from './SampleTypeManagement';

// Mock react-intl
const mockIntl = {
  formatMessage: ({ id, defaultMessage }) => defaultMessage || id
};

// Wrapper component with IntlProvider
const TestWrapper = ({ children }) => (
  <IntlProvider locale="en" messages={{}}>
    {children}
  </IntlProvider>
);

describe('SampleTypeManagement', () => {
  test('renders sample type management page', () => {
    render(
      <TestWrapper>
        <SampleTypeManagement intl={mockIntl} />
      </TestWrapper>
    );

    expect(screen.getByText('Sample Type Management')).toBeInTheDocument();
    expect(screen.getByText('Add Sample Type')).toBeInTheDocument();
  });

  test('opens add sample type form when Add button is clicked', () => {
    render(
      <TestWrapper>
        <SampleTypeManagement intl={mockIntl} />
      </TestWrapper>
    );

    const addButton = screen.getByText('Add Sample Type');
    fireEvent.click(addButton);

    expect(screen.getByText('Add New Sample Type')).toBeInTheDocument();
    expect(screen.getByText('Create a new sample type by filling in the required information below.')).toBeInTheDocument();
  });

  test('can fill out and submit new sample type form with validation', async () => {
    render(
      <TestWrapper>
        <SampleTypeManagement intl={mockIntl} />
      </TestWrapper>
    );

    // Click Add Sample Type button
    const addButton = screen.getByText('Add Sample Type');
    fireEvent.click(addButton);

    // Check that info notification is shown
    expect(screen.getByText(/Fields marked with \* are required/)).toBeInTheDocument();

    // Fill out the required fields
    const nameInput = screen.getByLabelText(/Name/);
    const descriptionInput = screen.getByLabelText(/Description/);
    const domainSelect = screen.getByLabelText(/Sample Domain/);

    fireEvent.change(nameInput, { target: { value: 'Test Sample Type' } });
    fireEvent.change(descriptionInput, { target: { value: 'This is a detailed test description for the sample type' } });
    fireEvent.change(domainSelect, { target: { value: 'CLINICAL' } });

    // Optional fields
    const abbreviationInput = screen.getByLabelText(/Abbreviation/);
    fireEvent.change(abbreviationInput, { target: { value: 'TST' } });

    // Submit the form
    const createButton = screen.getByText('Create Sample Type');
    expect(createButton).not.toBeDisabled();
    fireEvent.click(createButton);

    // Should show creating state
    expect(screen.getByText('Creating...')).toBeInTheDocument();

    // Should return to list view with success message
    await waitFor(() => {
      expect(screen.getByText('Sample Type Management')).toBeInTheDocument();
      expect(screen.getByText('Add Sample Type')).toBeInTheDocument();

      // Verify success message appears
      expect(screen.getByText(/Sample type created successfully!/)).toBeInTheDocument();
    }, { timeout: 3000 });
  });

  test('validates required fields and shows errors', async () => {
    render(
      <TestWrapper>
        <SampleTypeManagement intl={mockIntl} />
      </TestWrapper>
    );

    // Click Add Sample Type button
    const addButton = screen.getByText('Add Sample Type');
    fireEvent.click(addButton);

    // Try to submit with only name filled
    const nameInput = screen.getByLabelText(/Name/);
    fireEvent.change(nameInput, { target: { value: 'Test' } });

    const createButton = screen.getByText('Create Sample Type');
    fireEvent.click(createButton);

    // Should show validation errors
    await waitFor(() => {
      expect(screen.getByText(/Description is required/)).toBeInTheDocument();
    });

    // Fill description with too few characters
    const descriptionInput = screen.getByLabelText(/Description/);
    fireEvent.change(descriptionInput, { target: { value: 'Short' } });

    fireEvent.click(createButton);

    await waitFor(() => {
      expect(screen.getByText(/Description must be at least 10 characters long/)).toBeInTheDocument();
    });
  });

  test('form validation prevents submission with empty name', () => {
    render(
      <TestWrapper>
        <SampleTypeManagement intl={mockIntl} />
      </TestWrapper>
    );

    // Click Add Sample Type button
    const addButton = screen.getByText('Add Sample Type');
    fireEvent.click(addButton);

    // Try to submit without filling required fields
    const createButton = screen.getByText('Create Sample Type');
    expect(createButton).toBeDisabled();
  });

  test('can navigate back to list from add form', () => {
    render(
      <TestWrapper>
        <SampleTypeManagement intl={mockIntl} />
      </TestWrapper>
    );

    // Click Add Sample Type button
    const addButton = screen.getByText('Add Sample Type');
    fireEvent.click(addButton);

    // Click Back button
    const backButton = screen.getByText('← Back to List');
    fireEvent.click(backButton);

    // Should return to list view
    expect(screen.getByText('Sample Type Management')).toBeInTheDocument();
    expect(screen.getByText('Add Sample Type')).toBeInTheDocument();
  });
});