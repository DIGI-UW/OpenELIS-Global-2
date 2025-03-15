import ProviderMenuPage from '../../pages/ProviderMenuPage';

describe('Provider Menu', () => {
  
  beforeEach(() => {
    cy.intercept('GET', '/rest/ProviderMenu*', { fixture: 'providerMenuList.json' }).as('getProviderMenu');
    cy.visit('/providerMenu'); 
  });

  it('should load provider menu and show correct title', () => {
    ProviderMenuPage.pageHeading.should('contain.text', 'Provider Menu');
  });

  it('should add a new provider successfully', () => {
    ProviderMenuPage.openAddModal();

    const providerData = {
      lastName: 'Smith',
      firstName: 'John',
      telephone: '1234567890',
      fax: '9876543210',
      isActive: 'Yes',
    };

    ProviderMenuPage.addProvider(providerData.lastName, providerData.firstName, providerData.telephone, providerData.fax, providerData.isActive);

    cy.contains('Provider added successfully').should('be.visible');
    cy.get('table tbody').contains(providerData.lastName);
  });

  it('should search and filter provider list', () => {
    const searchTerm = 'Smith';
    ProviderMenuPage.searchProvider(searchTerm);
    cy.get('table tbody').find('tr').should('have.length.greaterThan', 0);
    cy.get('table tbody').contains(searchTerm);
  });

  it('should update an existing provider', () => {
    const providerId = 1; 
    ProviderMenuPage.openUpdateModal(providerId);

    const updatedData = {
      lastName: 'Doe',
      firstName: 'Jane',
      telephone: '5551234567',
      fax: '5559876543',
      isActive: 'No',
    };
    ProviderMenuPage.updateProvider(updatedData.lastName, updatedData.firstName, updatedData.telephone, updatedData.fax, updatedData.isActive);

    cy.get(`tr[data-id="${providerId}"]`).within(() => {
      cy.contains(updatedData.lastName);
      cy.contains(updatedData.firstName);
      cy.contains(updatedData.telephone);
      cy.contains(updatedData.fax);
    });
  });

  it('should deactivate a provider', () => {
    const providerId = 1;
    ProviderMenuPage.selectRowById(providerId);
    ProviderMenuPage.deactivateButton.click();
    cy.get(`tr[data-id="${providerId}"]`).within(() => {
      cy.contains('No'); 
    });
  });

  it('should paginate through provider menu', () => {
    ProviderMenuPage.navigateToNextPage();
    cy.url().should('include', 'page=2'); 

    ProviderMenuPage.navigateToPreviousPage();
    cy.url().should('include', 'page=1'); 
  });

});
