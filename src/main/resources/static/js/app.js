/**
 * Group Expense Settlement - Main Frontend Application
 */
document.addEventListener('DOMContentLoaded', () => {
  // App State
  const state = {
    groups: [],
    currentGroup: null,
    members: [],
    expenses: [],
    balances: [],
    settlements: [],
    activeTab: 'tab-expenses',
    modalGroupMembers: [],
  };

  // UI Element Selectors
  const elements = {
    // Theme & Brand
    themeToggleBtn: document.getElementById('theme-toggle-btn'),
    themeIcon: document.getElementById('theme-icon'),
    brandHomeBtn: document.getElementById('brand-home-btn'),

    // Views
    viewDashboard: document.getElementById('view-dashboard'),
    viewGroupDetail: document.getElementById('view-group-detail'),

    // Dashboard Stats & Container
    statTotalGroups: document.getElementById('stat-total-groups'),
    statTotalMembers: document.getElementById('stat-total-members'),
    statTotalExpenses: document.getElementById('stat-total-expenses'),
    groupsContainer: document.getElementById('groups-container'),
    createGroupBtn: document.getElementById('create-group-btn'),
    navNewGroupBtn: document.getElementById('nav-new-group-btn'),
    navRecordExpenseBtn: document.getElementById('nav-record-expense-btn'),

    // Group Detail Elements
    backToDashboardBtn: document.getElementById('back-to-dashboard-btn'),
    detailGroupName: document.getElementById('detail-group-name'),
    detailGroupDesc: document.getElementById('detail-group-desc'),
    detailGroupBadge: document.getElementById('detail-group-badge'),
    detailGroupMembersChips: document.getElementById('detail-group-members-chips'),
    btnOpenAddMember: document.getElementById('btn-open-add-member'),
    btnOpenRecordExpense: document.getElementById('btn-open-record-expense'),
    btnMembersTabAdd: document.getElementById('btn-members-tab-add'),

    // Tabs & Counters
    tabButtons: document.querySelectorAll('.tab-btn'),
    tabPanels: document.querySelectorAll('.tab-panel'),
    countTabExpenses: document.getElementById('count-tab-expenses'),
    countTabSettlements: document.getElementById('count-tab-settlements'),
    countTabMembers: document.getElementById('count-tab-members'),

    // Tab Contents
    totalSpentBadge: document.getElementById('total-spent-badge'),
    expensesTableBody: document.getElementById('expenses-table-body'),
    zeroSumBadge: document.getElementById('zero-sum-badge'),
    balancesGridContainer: document.getElementById('balances-grid-container'),
    settlementsContainer: document.getElementById('settlements-container'),
    copySettlementsBtn: document.getElementById('copy-settlements-btn'),
    membersTableBody: document.getElementById('members-table-body'),

    // Modals
    modalCreateGroup: document.getElementById('modal-create-group'),
    formCreateGroup: document.getElementById('form-create-group'),
    groupNameInput: document.getElementById('group-name-input'),

    modalAddMember: document.getElementById('modal-add-member'),
    formAddMember: document.getElementById('form-add-member'),
    memberNameInput: document.getElementById('member-name-input'),

    modalRecordExpense: document.getElementById('modal-record-expense'),
    formRecordExpense: document.getElementById('form-record-expense'),
    expenseGroupSelect: document.getElementById('expense-group-select'),
    noMembersWarning: document.getElementById('no-members-warning'),
    btnQuickAddMember: document.getElementById('btn-quick-add-member'),
    expenseDescInput: document.getElementById('expense-desc-input'),
    expenseAmountInput: document.getElementById('expense-amount-input'),
    expenseDateInput: document.getElementById('expense-date-input'),
    expensePayerSelect: document.getElementById('expense-payer-select'),
    splitMethodEqual: document.getElementById('split-method-equal'),
    splitMethodCustom: document.getElementById('split-method-custom'),
    sharesSectionLabel: document.getElementById('shares-section-label'),
    sharesListContainer: document.getElementById('shares-list-container'),
    splitTracker: document.getElementById('split-tracker'),
    trackerMsg: document.getElementById('tracker-msg'),
    trackerSummary: document.getElementById('tracker-summary'),
    btnSubmitExpense: document.getElementById('btn-submit-expense'),
    saveRecordAction: document.getElementById('save-record-action'),
    btnSaveExpenseRecord: document.getElementById('btn-save-expense-record'),

    modalViewShares: document.getElementById('modal-view-shares'),
    modalViewSharesTitle: document.getElementById('modal-view-shares-title'),
    sharesDetailBody: document.getElementById('shares-detail-body'),

    toastContainer: document.getElementById('toast-container'),
  };

  // ==========================================
  // Helper & Utility Functions
  // ==========================================

  function showToast(message, type = 'info') {
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    const icon = type === 'success' ? '✓' : type === 'error' ? '⚠️' : 'ℹ️';
    toast.innerHTML = `<span>${icon}</span><span>${escapeHtml(message)}</span>`;

    elements.toastContainer.appendChild(toast);
    setTimeout(() => {
      toast.style.opacity = '0';
      toast.style.transform = 'translateY(10px)';
      toast.style.transition = 'all 0.25s ease';
      setTimeout(() => toast.remove(), 250);
    }, 4500);
  }

  function formatCurrency(amount) {
    if (amount === null || amount === undefined) return '₹0.00';
    const num = Number(amount);
    return '₹' + num.toLocaleString('en-IN', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    });
  }

  function formatDate(dateStr) {
    if (!dateStr) return '';
    try {
      const parts = dateStr.split('-');
      if (parts.length === 3) {
        // Parse local date from YYYY-MM-DD
        const date = new Date(parseInt(parts[0], 10), parseInt(parts[1], 10) - 1, parseInt(parts[2], 10));
        return date.toLocaleDateString('en-IN', {
          year: 'numeric',
          month: 'short',
          day: 'numeric',
        });
      }
      const date = new Date(dateStr);
      return date.toLocaleDateString('en-IN', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
      });
    } catch {
      return dateStr;
    }
  }

  function getLocalDateString() {
    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  function escapeHtml(str) {
    if (!str) return '';
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
  }

  function setSaveRecordActionVisible(visible) {
    if (!elements.saveRecordAction) return;
    const shouldShow = Boolean(visible);
    const wasVisible = elements.saveRecordAction.classList.contains('visible');
    elements.saveRecordAction.classList.toggle('visible', shouldShow);
    // When the action first appears, bring it into view inside the scrollable
    // modal body so it is immediately usable after a valid split.
    if (shouldShow && !wasVisible) {
      elements.saveRecordAction.scrollIntoView({ block: 'nearest' });
    }
  }

  // ==========================================
  // Theme Switching
  // ==========================================

  function initTheme() {
    const savedTheme = localStorage.getItem('split_settler_theme') || 'light';
    setTheme(savedTheme);
  }

  function setTheme(theme) {
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem('split_settler_theme', theme);
    elements.themeIcon.textContent = theme === 'dark' ? '☀️' : '🌙';
  }

  elements.themeToggleBtn.addEventListener('click', () => {
    const currentTheme = document.documentElement.getAttribute('data-theme') || 'light';
    setTheme(currentTheme === 'dark' ? 'light' : 'dark');
  });

  // ==========================================
  // Navigation & URL Hash Routing
  // ==========================================

  function showDashboard(updateHash = true) {
    state.currentGroup = null;
    elements.viewGroupDetail.style.display = 'none';
    elements.viewDashboard.style.display = 'block';
    if (updateHash) {
      window.location.hash = '#groups';
    }
    loadDashboardData();
  }

  function showGroupDetail(group, updateHash = true) {
    state.currentGroup = group;
    elements.viewDashboard.style.display = 'none';
    elements.viewGroupDetail.style.display = 'block';
    elements.detailGroupName.textContent = group.name;
    elements.detailGroupBadge.textContent = `Group #${group.id}`;
    elements.detailGroupDesc.textContent = `Created on ${formatDate(group.createdAt)}`;

    if (updateHash) {
      window.location.hash = `#group-${group.id}`;
    }

    switchTab('tab-expenses');
    refreshGroupData();
  }

  async function handleHashRoute() {
    const hash = window.location.hash;
    const match = hash.match(/^#group-(\d+)$/);
    if (match) {
      const groupId = parseInt(match[1], 10);
      try {
        const group = await API.getGroupById(groupId);
        showGroupDetail(group, false);
      } catch (err) {
        console.warn('Could not load group from hash URL:', err);
        showDashboard(false);
      }
    } else {
      showDashboard(false);
    }
  }

  window.addEventListener('hashchange', handleHashRoute);

  function switchTab(tabId) {
    state.activeTab = tabId;
    elements.tabButtons.forEach(btn => {
      btn.classList.toggle('active', btn.getAttribute('data-tab') === tabId);
    });
    elements.tabPanels.forEach(panel => {
      panel.classList.toggle('active', panel.id === tabId);
    });
  }

  elements.tabButtons.forEach(btn => {
    btn.addEventListener('click', () => {
      const targetTab = btn.getAttribute('data-tab');
      switchTab(targetTab);
    });
  });

  elements.brandHomeBtn.addEventListener('click', () => showDashboard(true));
  elements.backToDashboardBtn.addEventListener('click', () => showDashboard(true));

  // ==========================================
  // Modals Management
  // ==========================================

  function openModal(modal) {
    modal.classList.add('open');
    const firstInput = modal.querySelector('input:not([type="hidden"]), select, textarea');
    if (firstInput) firstInput.focus();
  }

  function closeModal(modal) {
    modal.classList.remove('open');
  }

  document.querySelectorAll('[data-close-modal]').forEach(btn => {
    btn.addEventListener('click', () => {
      const modalId = btn.getAttribute('data-close-modal');
      const modal = document.getElementById(modalId);
      if (modal) closeModal(modal);
    });
  });

  document.querySelectorAll('.modal-backdrop').forEach(modal => {
    modal.addEventListener('click', (e) => {
      if (e.target === modal) closeModal(modal);
    });
  });

  // Open modals listeners
  elements.createGroupBtn.addEventListener('click', () => {
    elements.formCreateGroup.reset();
    openModal(elements.modalCreateGroup);
  });

  elements.navNewGroupBtn.addEventListener('click', () => {
    elements.formCreateGroup.reset();
    openModal(elements.modalCreateGroup);
  });

  elements.btnOpenAddMember.addEventListener('click', () => {
    elements.formAddMember.reset();
    openModal(elements.modalAddMember);
  });

  elements.btnMembersTabAdd.addEventListener('click', () => {
    elements.formAddMember.reset();
    openModal(elements.modalAddMember);
  });

  elements.btnOpenRecordExpense.addEventListener('click', () => {
    const targetGroupId = state.currentGroup ? state.currentGroup.id : null;
    openRecordExpenseModal(targetGroupId);
  });

  elements.navRecordExpenseBtn.addEventListener('click', () => {
    const targetGroupId = state.currentGroup ? state.currentGroup.id : null;
    openRecordExpenseModal(targetGroupId);
  });

  // Quick add member button from inside the expense modal
  elements.btnQuickAddMember.addEventListener('click', () => {
    const chosenGroupId = parseInt(elements.expenseGroupSelect.value, 10);
    closeModal(elements.modalRecordExpense);
    elements.formAddMember.reset();
    if (chosenGroupId && (!state.currentGroup || state.currentGroup.id !== chosenGroupId)) {
      API.getGroupById(chosenGroupId).then(g => {
        state.currentGroup = g;
        openModal(elements.modalAddMember);
      });
    } else {
      openModal(elements.modalAddMember);
    }
  });

  // ==========================================
  // Dashboard Logic
  // ==========================================

  async function loadDashboardData() {
    try {
      const groups = await API.getGroups();
      state.groups = groups || [];

      // Calculate stats
      elements.statTotalGroups.textContent = state.groups.length;
      let totalMembersCount = 0;
      let totalExpensesVolume = 0;

      // Fetch expenses count across all groups
      const expensePromises = state.groups.map(g => API.getExpenses(g.id).catch(() => []));
      const groupExpenses = await Promise.all(expensePromises);
      groupExpenses.forEach(expList => {
        if (Array.isArray(expList)) {
          totalExpensesVolume += expList.length;
        }
      });
      elements.statTotalExpenses.textContent = totalExpensesVolume;

      state.groups.forEach(g => {
        if (g.members) totalMembersCount += g.members.length;
      });
      elements.statTotalMembers.textContent = totalMembersCount;

      renderGroupsList();
    } catch (err) {
      showToast('Failed to load groups: ' + err.message, 'error');
    }
  }

  function renderGroupsList() {
    elements.groupsContainer.innerHTML = '';

    if (state.groups.length === 0) {
      elements.groupsContainer.innerHTML = `
        <div class="empty-state" style="grid-column: 1 / -1;">
          <div class="empty-state-icon">📂</div>
          <h3>No Expense Groups Yet</h3>
          <p>Create your first group to start splitting bills with friends, roommates, or trip buddies.</p>
          <button class="btn btn-primary" style="margin-top: 1rem;" onclick="document.getElementById('create-group-btn').click()">
            + Create Group
          </button>
        </div>
      `;
      return;
    }

    state.groups.forEach(group => {
      const card = document.createElement('div');
      card.className = 'group-card';
      const memberCount = group.members ? group.members.length : 0;

      card.innerHTML = `
        <div class="group-card-header">
          <span class="group-card-title">${escapeHtml(group.name)}</span>
          <span class="badge badge-primary">#${group.id}</span>
        </div>
        <div class="group-card-desc">
          ${memberCount} member${memberCount === 1 ? '' : 's'} registered in this expense pool.
        </div>
        <div class="group-card-meta">
          <span>Created: ${formatDate(group.createdAt)}</span>
          <div style="display: flex; gap: 0.5rem; align-items: center;">
            <button class="btn btn-outline btn-sm quick-expense-btn" data-group-id="${group.id}" title="Add Expense to this group" style="padding: 0.2rem 0.5rem;">
              + Expense
            </button>
            <span style="font-weight: 600; color: var(--primary);">View →</span>
          </div>
        </div>
      `;

      card.addEventListener('click', (e) => {
        if (e.target.closest('.quick-expense-btn')) {
          e.stopPropagation();
          const targetGroupId = parseInt(e.target.closest('.quick-expense-btn').dataset.groupId, 10);
          openRecordExpenseModal(targetGroupId);
          return;
        }
        showGroupDetail(group, true);
      });

      elements.groupsContainer.appendChild(card);
    });
  }

  // ==========================================
  // Group Detail Logic
  // ==========================================

  async function refreshGroupData() {
    if (!state.currentGroup) return;
    const groupId = state.currentGroup.id;

    try {
      // 1. Fetch updated group with members
      const groupData = await API.getGroupById(groupId);
      state.currentGroup = groupData;
      state.members = groupData.members || [];

      renderGroupHeader();
      renderMembersTab();

      // 2. Fetch expenses
      const expenses = await API.getExpenses(groupId);
      state.expenses = expenses || [];
      renderExpensesTab();

      // 3. Fetch balances
      const balances = await API.getBalances(groupId);
      state.balances = balances || [];
      renderBalancesTab();

      // 4. Fetch settlements
      const settlements = await API.getSettlements(groupId);
      state.settlements = settlements || [];
      renderSettlementsTab();

      // Update counters
      elements.countTabExpenses.textContent = state.expenses.length;
      elements.countTabSettlements.textContent = state.settlements.length;
      elements.countTabMembers.textContent = state.members.length;
    } catch (err) {
      showToast('Error refreshing group data: ' + err.message, 'error');
    }
  }

  function renderGroupHeader() {
    elements.detailGroupName.textContent = state.currentGroup.name;
    elements.detailGroupBadge.textContent = `Group #${state.currentGroup.id}`;

    elements.detailGroupMembersChips.innerHTML = '';
    if (state.members.length === 0) {
      elements.detailGroupMembersChips.innerHTML = '<span style="font-size: 0.8rem; color: var(--text-muted);">No members added yet. Add members to start recording expenses!</span>';
      return;
    }

    state.members.forEach(m => {
      const chip = document.createElement('span');
      chip.className = 'member-chip';
      const initial = m.name ? m.name.charAt(0).toUpperCase() : '?';
      chip.innerHTML = `<span class="avatar">${initial}</span> ${escapeHtml(m.name)}`;
      elements.detailGroupMembersChips.appendChild(chip);
    });
  }

  function renderExpensesTab() {
    elements.expensesTableBody.innerHTML = '';

    let totalSpend = 0;
    state.expenses.forEach(e => totalSpend += Number(e.totalAmount || 0));
    elements.totalSpentBadge.textContent = `Total Group Spend: ${formatCurrency(totalSpend)}`;

    if (state.expenses.length === 0) {
      elements.expensesTableBody.innerHTML = `
        <tr>
          <td colspan="6" style="text-align: center; padding: 2.5rem; color: var(--text-muted);">
            No expenses recorded yet in this group.<br>
            <button class="btn btn-primary btn-sm" style="margin-top: 0.75rem;" onclick="document.getElementById('btn-open-record-expense').click()">
              + Record First Expense
            </button>
          </td>
        </tr>
      `;
      return;
    }

    state.expenses.forEach(exp => {
      const row = document.createElement('tr');
      const splitBadgeClass = exp.splitMethod === 'EQUAL' ? 'badge-primary' : 'badge-warning';

      row.innerHTML = `
        <td style="color: var(--text-muted);">${formatDate(exp.expenseDate)}</td>
        <td style="font-weight: 600;">${escapeHtml(exp.description)}</td>
        <td>
          <span class="member-chip" style="font-size: 0.8rem;">
            <span class="avatar">${escapeHtml(exp.payerName ? exp.payerName.charAt(0) : 'P')}</span>
            ${escapeHtml(exp.payerName)}
          </span>
        </td>
        <td><span class="badge ${splitBadgeClass}">${escapeHtml(exp.splitMethod)}</span></td>
        <td class="amount-display">${formatCurrency(exp.totalAmount)}</td>
        <td style="text-align: right;">
          <button class="btn btn-outline btn-sm view-shares-btn" data-exp-id="${exp.id}">
            View Shares (${exp.shares ? exp.shares.length : 0})
          </button>
        </td>
      `;

      const viewBtn = row.querySelector('.view-shares-btn');
      viewBtn.addEventListener('click', () => openSharesModal(exp));
      elements.expensesTableBody.appendChild(row);
    });
  }

  function openSharesModal(exp) {
    elements.modalViewSharesTitle.textContent = `Shares for: ${exp.description}`;
    elements.sharesDetailBody.innerHTML = '';

    if (!exp.shares || exp.shares.length === 0) {
      elements.sharesDetailBody.innerHTML = `
        <tr><td colspan="2" style="text-align: center;">No shares available</td></tr>
      `;
    } else {
      exp.shares.forEach(s => {
        const row = document.createElement('tr');
        row.innerHTML = `
          <td><strong>${escapeHtml(s.memberName)}</strong></td>
          <td style="text-align: right;" class="amount-display">${formatCurrency(s.assignedAmount)}</td>
        `;
        elements.sharesDetailBody.appendChild(row);
      });
    }

    openModal(elements.modalViewShares);
  }

  function renderBalancesTab() {
    elements.balancesGridContainer.innerHTML = '';

    if (state.balances.length === 0) {
      elements.balancesGridContainer.innerHTML = `
        <div class="empty-state" style="grid-column: 1 / -1;">
          <div class="empty-state-icon">⚖️</div>
          <h3>No Balances to Compute</h3>
          <p>Add group members and record expenses to see net balances.</p>
        </div>
      `;
      return;
    }

    let netSum = 0;
    state.balances.forEach(b => netSum += Number(b.netBalance || 0));
    elements.zeroSumBadge.textContent = `✓ Zero-Sum Invariant: Sum = ${formatCurrency(netSum)}`;

    state.balances.forEach(b => {
      const card = document.createElement('div');
      card.className = 'balance-card';

      const net = Number(b.netBalance || 0);
      let statusBadgeHtml = '';
      let netDisplayClass = '';
      let statusText = '';

      if (net > 0) {
        statusBadgeHtml = `<span class="badge badge-success">Gets Back</span>`;
        netDisplayClass = 'amount-positive';
        statusText = `Gets back ${formatCurrency(net)}`;
      } else if (net < 0) {
        statusBadgeHtml = `<span class="badge badge-danger">Owes</span>`;
        netDisplayClass = 'amount-negative';
        statusText = `Owes ${formatCurrency(Math.abs(net))}`;
      } else {
        statusBadgeHtml = `<span class="badge badge-subtle">Settled Up</span>`;
        netDisplayClass = 'amount-zero';
        statusText = `Settled (₹0.00)`;
      }

      card.innerHTML = `
        <div class="balance-card-header">
          <div class="balance-card-name">${escapeHtml(b.memberName)}</div>
          ${statusBadgeHtml}
        </div>
        <div class="balance-row">
          <span>Total Paid</span>
          <span class="amount-display">${formatCurrency(b.totalPaid)}</span>
        </div>
        <div class="balance-row">
          <span>Share Assigned</span>
          <span class="amount-display">${formatCurrency(b.totalOwed)}</span>
        </div>
        <div class="balance-row net">
          <span>Net Balance</span>
          <span class="amount-display ${netDisplayClass}">${statusText}</span>
        </div>
      `;

      elements.balancesGridContainer.appendChild(card);
    });
  }

  function renderSettlementsTab() {
    elements.settlementsContainer.innerHTML = '';

    if (state.settlements.length === 0) {
      elements.settlementsContainer.innerHTML = `
        <div class="empty-state">
          <div class="empty-state-icon">🎉</div>
          <h3>All Settled Up!</h3>
          <p>There are currently no outstanding debts in this group. Everyone is even.</p>
        </div>
      `;
      elements.copySettlementsBtn.style.display = 'none';
      return;
    }

    elements.copySettlementsBtn.style.display = 'inline-flex';

    state.settlements.forEach((item) => {
      const card = document.createElement('div');
      card.className = 'settlement-card';

      card.innerHTML = `
        <div class="settlement-flow">
          <div class="settlement-party">
            <span class="avatar" style="background: var(--danger); width: 28px; height: 28px; border-radius: 50%; color: #fff; display: inline-flex; align-items: center; justify-content: center; font-size: 0.8rem; font-weight: 700;">
              ${escapeHtml(item.fromMemberName ? item.fromMemberName.charAt(0) : 'D')}
            </span>
            <span>${escapeHtml(item.fromMemberName)}</span>
          </div>

          <div class="settlement-arrow">
            <span>pays</span>
            <svg width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24" style="margin-left: 0.35rem;"><path d="M5 12h14M12 5l7 7-7 7"></path></svg>
          </div>

          <div class="settlement-party">
            <span class="avatar" style="background: var(--success); width: 28px; height: 28px; border-radius: 50%; color: #fff; display: inline-flex; align-items: center; justify-content: center; font-size: 0.8rem; font-weight: 700;">
              ${escapeHtml(item.toMemberName ? item.toMemberName.charAt(0) : 'C')}
            </span>
            <span>${escapeHtml(item.toMemberName)}</span>
          </div>
        </div>

        <div>
          <span class="settlement-amount-tag">${formatCurrency(item.amount)}</span>
        </div>
      `;

      elements.settlementsContainer.appendChild(card);
    });
  }

  // Copy Settlement Plan to Clipboard
  elements.copySettlementsBtn.addEventListener('click', () => {
    if (!state.settlements || state.settlements.length === 0) return;

    let text = `🤝 Settlement Plan for "${state.currentGroup ? state.currentGroup.name : 'Group'}":\n\n`;
    state.settlements.forEach((s, idx) => {
      text += `${idx + 1}. ${s.fromMemberName} pays ${s.toMemberName} -> ${formatCurrency(s.amount)}\n`;
    });
    text += `\nGenerated by SplitSettler via Greedy Settlement Algorithm.`;

    navigator.clipboard.writeText(text).then(() => {
      showToast('Settlement transfer plan copied to clipboard!', 'success');
    }).catch(() => {
      showToast('Could not copy to clipboard automatically.', 'error');
    });
  });

  function renderMembersTab() {
    elements.membersTableBody.innerHTML = '';

    if (state.members.length === 0) {
      elements.membersTableBody.innerHTML = `
        <tr>
          <td colspan="3" style="text-align: center; padding: 2rem; color: var(--text-muted);">
            No members have been added to this group yet.<br>
            <button class="btn btn-primary btn-sm" style="margin-top: 0.75rem;" onclick="document.getElementById('btn-open-add-member').click()">
              + Add First Member
            </button>
          </td>
        </tr>
      `;
      return;
    }

    state.members.forEach(m => {
      const row = document.createElement('tr');
      row.innerHTML = `
        <td style="color: var(--text-muted);">#${m.id}</td>
        <td style="font-weight: 600;">${escapeHtml(m.name)}</td>
        <td style="color: var(--text-muted); font-size: 0.85rem;">Joined ${formatDate(m.joinedAt)}</td>
      `;
      elements.membersTableBody.appendChild(row);
    });
  }

  // ==========================================
  // Form Submission Handlers
  // ==========================================

  // Create Group Form
  elements.formCreateGroup.addEventListener('submit', async (e) => {
    e.preventDefault();
    const name = elements.groupNameInput.value.trim();
    if (!name) return;

    try {
      const newGroup = await API.createGroup({ name });
      showToast(`Group "${newGroup.name}" created successfully!`, 'success');
      closeModal(elements.modalCreateGroup);
      await loadDashboardData();
      showGroupDetail(newGroup, true);
    } catch (err) {
      showToast(err.message, 'error');
    }
  });

  // Add Member Form
  elements.formAddMember.addEventListener('submit', async (e) => {
    e.preventDefault();
    if (!state.currentGroup) {
      showToast('Please select or open a group first.', 'error');
      return;
    }

    const name = elements.memberNameInput.value.trim();
    if (!name) return;

    try {
      const added = await API.addMember(state.currentGroup.id, { name });
      showToast(`Member "${added.name}" added to group!`, 'success');
      closeModal(elements.modalAddMember);
      await refreshGroupData();
    } catch (err) {
      showToast(err.message, 'error');
    }
  });

  // ==========================================
  // Dynamic Expense Modal & Split Engine
  // ==========================================

  function openRecordExpenseModal(targetGroupId = null) {
    // 1. Ensure groups exist
    if (!state.groups || state.groups.length === 0) {
      showToast('Please create an expense group first.', 'info');
      openModal(elements.modalCreateGroup);
      return;
    }

    elements.formRecordExpense.reset();
    setSaveRecordActionVisible(false);

    // Default expense date to today in local user timezone
    elements.expenseDateInput.value = getLocalDateString();

    // 2. Populate Group Select
    elements.expenseGroupSelect.innerHTML = '<option value="">Select group...</option>';
    state.groups.forEach(g => {
      const opt = document.createElement('option');
      opt.value = g.id;
      opt.textContent = g.name;
      elements.expenseGroupSelect.appendChild(opt);
    });

    const activeGroupId = targetGroupId || (state.currentGroup ? state.currentGroup.id : state.groups[0].id);
    elements.expenseGroupSelect.value = activeGroupId;

    // Default split method to EQUAL
    elements.splitMethodEqual.checked = true;

    // 3. Immediately render members for the selected group
    updateModalMembersForGroup(activeGroupId);

    // 4. Open modal immediately
    openModal(elements.modalRecordExpense);
  }

  async function updateModalMembersForGroup(groupId) {
    if (!groupId) {
      state.modalGroupMembers = [];
      renderModalMembers();
      return;
    }

    // Check if members are already in state
    const group = state.groups.find(g => g.id === groupId);
    if (group && Array.isArray(group.members) && group.members.length > 0) {
      state.modalGroupMembers = group.members;
      renderModalMembers();
      return;
    }

    if (state.currentGroup && state.currentGroup.id === groupId && state.members.length > 0) {
      state.modalGroupMembers = state.members;
      renderModalMembers();
      return;
    }

    try {
      const members = await API.getMembers(groupId);
      state.modalGroupMembers = members || [];
      // Also cache onto the group
      if (group) group.members = state.modalGroupMembers;
    } catch (err) {
      console.warn('Could not load group members:', err);
      state.modalGroupMembers = [];
    }

    renderModalMembers();
  }

  function renderModalMembers() {
    const members = state.modalGroupMembers;

    // Check if group has members
    if (members.length === 0) {
      elements.noMembersWarning.style.display = 'block';
      elements.expensePayerSelect.innerHTML = '<option value="">No members in group...</option>';
      elements.sharesListContainer.innerHTML = '<div style="padding: 1rem; text-align: center; color: var(--text-muted);">Please add members to this group first.</div>';
      elements.splitTracker.className = 'split-tracker mismatch';
      elements.trackerMsg.textContent = 'Add group members to calculate splits.';
      elements.trackerSummary.textContent = '0 members';
      setSaveRecordActionVisible(false);
      return;
    }

    elements.noMembersWarning.style.display = 'none';

    // Populate Payer Select
    elements.expensePayerSelect.innerHTML = '<option value="">Select payer...</option>';
    members.forEach((m, idx) => {
      const opt = document.createElement('option');
      opt.value = m.id;
      opt.textContent = m.name;
      if (idx === 0) opt.selected = true; // Default to first member as payer
      elements.expensePayerSelect.appendChild(opt);
    });

    // Render Split shares inputs based on current radio selection
    const method = elements.splitMethodCustom.checked ? 'CUSTOM' : 'EQUAL';
    renderSplitSharesInputs(method);
  }

  elements.expenseGroupSelect.addEventListener('change', async () => {
    const selectedGroupId = parseInt(elements.expenseGroupSelect.value, 10);
    await updateModalMembersForGroup(selectedGroupId);
  });

  elements.splitMethodEqual.addEventListener('change', () => {
    if (elements.splitMethodEqual.checked) {
      renderSplitSharesInputs('EQUAL');
    }
  });

  elements.splitMethodCustom.addEventListener('change', () => {
    if (elements.splitMethodCustom.checked) {
      renderSplitSharesInputs('CUSTOM');
    }
  });

  elements.expenseAmountInput.addEventListener('input', () => {
    updateSplitCalculation();
  });

  function renderSplitSharesInputs(method) {
    elements.sharesListContainer.innerHTML = '';
    const members = state.modalGroupMembers;

    if (members.length === 0) return;

    if (method === 'EQUAL') {
      elements.sharesSectionLabel.textContent = 'Select Members Included in Equal Split:';

      members.forEach(m => {
        const row = document.createElement('div');
        row.className = 'share-item-row';
        row.innerHTML = `
          <label style="display: flex; align-items: center; gap: 0.5rem; cursor: pointer; flex: 1; margin-bottom: 0;">
            <input type="checkbox" class="equal-member-cb" value="${m.id}" checked>
            <span>${escapeHtml(m.name)}</span>
          </label>
          <span class="amount-display equal-share-val" style="color: var(--text-muted); font-size: 0.85rem;">₹0.00</span>
        `;

        row.querySelector('.equal-member-cb').addEventListener('change', updateSplitCalculation);
        elements.sharesListContainer.appendChild(row);
      });
    } else {
      elements.sharesSectionLabel.textContent = 'Specify Custom Amount for Each Member:';

      members.forEach(m => {
        const row = document.createElement('div');
        row.className = 'share-item-row';
        row.innerHTML = `
          <label style="flex: 1; margin-bottom: 0; font-weight: 500;">
            ${escapeHtml(m.name)}
          </label>
          <div style="display: flex; align-items: center; gap: 0.35rem;">
            <span>₹</span>
            <input type="number" step="0.01" min="0" placeholder="0.00" class="form-control custom-share-input" data-member-id="${m.id}">
          </div>
        `;

        row.querySelector('.custom-share-input').addEventListener('input', updateSplitCalculation);
        elements.sharesListContainer.appendChild(row);
      });
    }

    updateSplitCalculation();
  }

  function updateSplitCalculation() {
    const totalAmount = parseFloat(elements.expenseAmountInput.value) || 0;
    const isCustom = elements.splitMethodCustom.checked;

    if (!isCustom) {
      // EQUAL
      const checkedBoxes = Array.from(elements.sharesListContainer.querySelectorAll('.equal-member-cb:checked'));
      const count = checkedBoxes.length;

      if (count === 0) {
        elements.splitTracker.className = 'split-tracker mismatch';
        elements.trackerMsg.textContent = '⚠️ Select at least one member to participate in the split.';
        elements.trackerSummary.textContent = '0 selected';
        setSaveRecordActionVisible(false);
        return;
      }

      // Calculate exact cent split matching backend calculateEqualShares
      const sortedChecked = checkedBoxes.map(cb => ({
        id: parseInt(cb.value, 10),
        cb: cb,
        row: cb.closest('.share-item-row')
      })).sort((a, b) => a.id - b.id);

      const totalPaise = Math.round(totalAmount * 100);
      const basePaise = Math.floor(totalPaise / count);
      const remainderPaise = totalPaise % count;

      elements.sharesListContainer.querySelectorAll('.share-item-row').forEach(row => {
        const valSpan = row.querySelector('.equal-share-val');
        if (valSpan) {
          valSpan.textContent = '₹0.00';
          valSpan.style.color = 'var(--text-muted)';
        }
      });

      sortedChecked.forEach((item, index) => {
        const paise = basePaise + (index < remainderPaise ? 1 : 0);
        const valSpan = item.row.querySelector('.equal-share-val');
        if (valSpan) {
          valSpan.textContent = formatCurrency(paise / 100);
          valSpan.style.color = 'var(--text-main)';
        }
      });

      elements.splitTracker.className = 'split-tracker matched';
      elements.trackerMsg.textContent = `Split across ${count} member${count === 1 ? '' : 's'}:`;
      elements.trackerSummary.textContent = totalAmount > 0
        ? (remainderPaise === 0 ? `${formatCurrency(basePaise / 100)} each` : `~${formatCurrency(basePaise / 100)} each`)
        : '₹0.00 each';
      setSaveRecordActionVisible(totalAmount > 0);
    } else {
      // CUSTOM
      const inputs = Array.from(elements.sharesListContainer.querySelectorAll('.custom-share-input'));
      let allocated = 0;

      inputs.forEach(inp => {
        allocated += parseFloat(inp.value) || 0;
      });

      const diff = Math.round((totalAmount - allocated) * 100) / 100;

      if (totalAmount <= 0) {
        elements.splitTracker.className = 'split-tracker mismatch';
        elements.trackerMsg.textContent = 'Enter a total expense amount first.';
        elements.trackerSummary.textContent = `Allocated: ${formatCurrency(allocated)}`;
      } else if (Math.abs(diff) === 0) {
        elements.splitTracker.className = 'split-tracker matched';
        elements.trackerMsg.textContent = '✓ Split matches total amount exactly!';
        elements.trackerSummary.textContent = `${formatCurrency(allocated)} / ${formatCurrency(totalAmount)}`;
      } else {
        elements.splitTracker.className = 'split-tracker mismatch';
        const diffText = diff > 0 ? `₹${diff.toFixed(2)} remaining to assign` : `₹${Math.abs(diff).toFixed(2)} over allocated`;
        elements.trackerMsg.textContent = `⚠️ Sum of shares must equal total (${diffText}).`;
        elements.trackerSummary.textContent = `${formatCurrency(allocated)} / ${formatCurrency(totalAmount)}`;
      }

      setSaveRecordActionVisible(totalAmount > 0 && Math.abs(diff) === 0);
    }
  }

  // Record Expense Form Submission
  elements.formRecordExpense.addEventListener('submit', async (e) => {
    e.preventDefault();

    const targetGroupId = parseInt(elements.expenseGroupSelect.value, 10);
    if (!targetGroupId) {
      showToast('Please select a group for this expense.', 'error');
      return;
    }

    const description = elements.expenseDescInput.value.trim();
    const rawAmount = parseFloat(elements.expenseAmountInput.value);
    const totalAmount = isNaN(rawAmount) ? 0 : parseFloat(rawAmount.toFixed(2));
    const expenseDate = elements.expenseDateInput.value;
    const payerMemberId = parseInt(elements.expensePayerSelect.value, 10);
    const isCustom = elements.splitMethodCustom.checked;

    if (!description) {
      showToast('Description is required.', 'error');
      return;
    }
    if (description.length > 255) {
      showToast('Description cannot exceed 255 characters.', 'error');
      return;
    }
    if (!totalAmount || totalAmount <= 0) {
      showToast('Total amount must be greater than zero.', 'error');
      return;
    }
    if (totalAmount > 9999999999.99) {
      showToast('Total amount exceeds maximum limit of ₹9,999,999,999.99.', 'error');
      return;
    }
    if (!payerMemberId) {
      showToast('Please select who paid for the expense.', 'error');
      return;
    }
    if (!expenseDate) {
      showToast('Please select the expense date.', 'error');
      return;
    }

    const payload = {
      description,
      totalAmount,
      expenseDate,
      payerMemberId,
      splitMethod: isCustom ? 'CUSTOM' : 'EQUAL',
    };

    if (!isCustom) {
      const checkedBoxes = Array.from(elements.sharesListContainer.querySelectorAll('.equal-member-cb:checked'));
      if (checkedBoxes.length === 0) {
        showToast('Please select at least one participant for the equal split.', 'error');
        return;
      }
      payload.participantMemberIds = checkedBoxes.map(cb => parseInt(cb.value, 10));
    } else {
      const inputs = Array.from(elements.sharesListContainer.querySelectorAll('.custom-share-input'));
      const customShares = [];
      let sum = 0;

      for (const inp of inputs) {
        const rawVal = parseFloat(inp.value);
        const val = isNaN(rawVal) ? 0 : parseFloat(rawVal.toFixed(2));
        if (val < 0) {
          showToast('Custom share amounts cannot be negative.', 'error');
          return;
        }
        const memberId = parseInt(inp.getAttribute('data-member-id'), 10);
        customShares.push({
          memberId,
          amount: val,
        });
        sum += val;
      }

      sum = parseFloat(sum.toFixed(2));
      const diff = Math.round((totalAmount - sum) * 100) / 100;
      if (Math.abs(diff) !== 0) {
        showToast(`Sum of shares (₹${sum.toFixed(2)}) must exactly equal total amount (₹${totalAmount.toFixed(2)}).`, 'error');
        return;
      }

      payload.customShares = customShares;
    }

    // Prevent duplicate submissions: disable every save button during the API request
    const submitButtons = [elements.btnSubmitExpense, elements.btnSaveExpenseRecord].filter(Boolean);
    submitButtons.forEach(btn => { btn.disabled = true; });
    if (elements.btnSubmitExpense) elements.btnSubmitExpense.innerHTML = '<span>⏳</span> Saving...';
    if (elements.btnSaveExpenseRecord) elements.btnSaveExpenseRecord.innerHTML = '<span>⏳</span> Saving...';

    try {
      const savedExpense = await API.recordExpense(targetGroupId, payload);
      showToast(`Expense "${savedExpense.description}" (${formatCurrency(savedExpense.totalAmount)}) saved successfully!`, 'success');
      closeModal(elements.modalRecordExpense);

      // If user was viewing a different view or dashboard, navigate to the target group
      if (!state.currentGroup || state.currentGroup.id !== targetGroupId) {
        window.location.hash = `#group-${targetGroupId}`;
        const targetGroup = await API.getGroupById(targetGroupId);
        showGroupDetail(targetGroup, false);
      } else {
        await refreshGroupData();
      }

      // Background update of dashboard counts
      API.getGroups().then(g => { state.groups = g || []; }).catch(() => {});
    } catch (err) {
      showToast(err.message, 'error');
    } finally {
      submitButtons.forEach(btn => { btn.disabled = false; });
      if (elements.btnSubmitExpense) elements.btnSubmitExpense.innerHTML = 'Save Expense';
      if (elements.btnSaveExpenseRecord) elements.btnSaveExpenseRecord.innerHTML = 'Save Expense Record';
    }
  });

  // ==========================================
  // App Initialization
  // ==========================================

  initTheme();

  // Load initial route based on URL hash or fallback to dashboard
  API.getGroups().then(groups => {
    state.groups = groups || [];
    handleHashRoute();
  }).catch(() => {
    handleHashRoute();
  });
});
