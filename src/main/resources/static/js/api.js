/**
 * API client module for Group Expense Settlement Backend
 */
const API = (() => {
  const BASE_URL = window.location.origin;

  async function request(endpoint, options = {}) {
    const url = `${BASE_URL}${endpoint}`;
    const defaultHeaders = {
      'Content-Type': 'application/json',
      'Accept': 'application/json',
    };

    const config = {
      ...options,
      headers: {
        ...defaultHeaders,
        ...(options.headers || {}),
      },
    };

    try {
      const response = await fetch(url, config);
      const isJson = response.headers.get('content-type')?.includes('application/json');
      const data = isJson ? await response.json() : null;

      if (!response.ok) {
        let errorMessage = 'An unexpected server error occurred.';
        if (data) {
          if (data.validationErrors && Array.isArray(data.validationErrors) && data.validationErrors.length > 0) {
            errorMessage = data.validationErrors.map(e => `${e.field}: ${e.message}`).join('; ');
          } else if (data.fieldErrors && Array.isArray(data.fieldErrors) && data.fieldErrors.length > 0) {
            errorMessage = data.fieldErrors.map(e => `${e.field}: ${e.message}`).join('; ');
          } else if (data.message) {
            errorMessage = data.message;
          } else if (data.error) {
            errorMessage = data.error;
          }
        }
        const error = new Error(errorMessage);
        error.status = response.status;
        error.data = data;
        throw error;
      }

      return data;
    } catch (err) {
      console.error(`API Request Error [${endpoint}]:`, err);
      throw err;
    }
  }

  return {
    // Groups
    getGroups: () => request('/api/groups'),
    getGroupById: (id) => request(`/api/groups/${id}`),
    createGroup: (payload) => request('/api/groups', {
      method: 'POST',
      body: JSON.stringify(payload),
    }),

    // Members
    getMembers: (groupId) => request(`/api/groups/${groupId}/members`),
    addMember: (groupId, payload) => request(`/api/groups/${groupId}/members`, {
      method: 'POST',
      body: JSON.stringify(payload),
    }),

    // Expenses
    getExpenses: (groupId) => request(`/api/groups/${groupId}/expenses`),
    recordExpense: (groupId, payload) => request(`/api/groups/${groupId}/expenses`, {
      method: 'POST',
      body: JSON.stringify(payload),
    }),

    // Balances & Settlements
    getBalances: (groupId) => request(`/api/groups/${groupId}/balances`),
    getSettlements: (groupId) => request(`/api/groups/${groupId}/settlements`),
  };
})();
