import api from './axiosConfig';
import { downloadAuthenticatedFile } from './fileUtils';

export const getAdminDashboard = async () => {
    const response = await api.get('/dashboard/admin');
    return response.data;
};

export const getResidents = async () => {
    const response = await api.get('/users');
    return response.data;
};

// Rooms Endpoints
export const getRooms = async () => {
    const response = await api.get('/rooms');
    return response.data;
};

export const createRoom = async (roomData) => {
    const response = await api.post('/rooms', roomData);
    return response.data;
};

export const updateRoom = async (id, roomData) => {
    const response = await api.put(`/rooms/${id}`, roomData);
    return response.data;
};

export const uploadShareCertificate = async (id, formData) => {
    const response = await api.post(`/rooms/${id}/share-certificate`, formData, {
        headers: {
            'Content-Type': 'multipart/form-data',
        },
    });
    return response.data;
};

export const getShareCertificate = async (id) => {
    const response = await api.get(`/rooms/${id}/share-certificate`);
    return response.data;
};

// Creates a resident account. Leave password blank to have a one-time temporary password generated; the
// response then carries it in `temporaryPassword` (shown to the admin once).
export const addResident = async ({ name, email, phone, password }) => {
    const payload = { name, email, phone, role: 'RESIDENT' };
    if (password && password.trim()) payload.password = password;
    const response = await api.post('/users', payload);
    return response.data;
};

export const resetUserPassword = async (id) => {
    const response = await api.post(`/users/${id}/reset-password`);
    return response.data;
};

export const updateResident = async (id, residentData) => {
    const response = await api.put(`/users/${id}`, residentData);
    return response.data;
};

export const deleteResident = async (id) => {
    const response = await api.delete(`/users/${id}`);
    return response.data;
};

export const updateResidentStatus = async (id, status) => {
    const response = await api.put(`/users/${id}/status?status=${status}`);
    return response.data;
};

export const updateUserRole = async (id, role) => {
    const response = await api.put(`/users/${id}/role?role=${role}`);
    return response.data;
};

// System Logs (Master Admin only)

export const getSystemLogs = async (params) => {
    const response = await api.get('/system-logs', { params });
    return response.data;
};

export const getSystemLogFilters = async () => {
    const response = await api.get('/system-logs/filters');
    return response.data;
};

// Downloads through the authenticated client so the access token stays in the header, not the URL
export const exportSystemLogs = async (params) => {
    const response = await api.get('/system-logs/export', { params, responseType: 'blob' });
    const disposition = response.headers['content-disposition'] || '';
    const match = disposition.match(/filename=["']?([^"';]+)["']?/);
    const url = window.URL.createObjectURL(new Blob([response.data], { type: 'text/csv' }));
    const link = document.createElement('a');
    link.href = url;
    link.download = match ? match[1] : 'system-logs.csv';
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
};

// Billing Endpoints

export const getBills = async (financialYear) => {
    let url = '/bills';
    if (financialYear) {
        url += `?financialYear=${encodeURIComponent(financialYear)}`;
    }
    const response = await api.get(url);
    return response.data;
};

export const exportBillsExcel = (financialYear) => {
    let url = '/bills/export-excel';
    if (financialYear) {
        url += `?financialYear=${encodeURIComponent(financialYear)}`;
    }
    return downloadAuthenticatedFile(url, `maintenance_bills_${financialYear || 'all'}.xlsx`);
};

export const downloadImportTemplate = () => {
    return downloadAuthenticatedFile('/bills/import-template', 'bill_import_template.xlsx');
};

export const importBillsExcel = async (file) => {
    const formData = new FormData();
    formData.append('file', file);
    const response = await api.post('/bills/import-excel', formData, {
        headers: {
            'Content-Type': 'multipart/form-data',
        },
    });
    return response.data;
};

export const emailReceipt = async (transactionId) => {
    const response = await api.post(`/transactions/${transactionId}/email-receipt`);
    return response.data;
};

export const generateMonthlyBills = async (billingMonth, amount, dueDate) => {
    const response = await api.post(`/bills/generate-monthly?billingMonth=${billingMonth}&amount=${amount}&dueDate=${dueDate}`);
    return response.data;
};

export const updateBill = async (id, billData) => {
    const response = await api.put(`/bills/${id}`, billData);
    return response.data;
};

export const markPaidCash = async (id) => {
    const response = await api.put(`/bills/${id}/mark-paid-cash`);
    return response.data;
};

export const recordPayment = async (billId, paymentData, idempotencyKey) => {
    const headers = {};
    if (idempotencyKey) {
        headers['Idempotency-Key'] = idempotencyKey;
    }
    const response = await api.post(`/bills/${billId}/record-payment`, paymentData, { headers });
    return response.data;
};

export const getTransactions = async () => {
    const response = await api.get('/transactions');
    return response.data;
};

// Complaints Endpoints
export const getComplaints = async () => {
    const response = await api.get('/complaints');
    return response.data;
};

export const updateComplaintStatus = async (id, status) => {
    const response = await api.put(`/complaints/${id}/status?status=${status}`);
    return response.data;
};

export const respondToComplaint = async (id, data) => {
    const response = await api.post(`/complaints/${id}/respond`, data);
    return response.data;
};

export const submitComplaint = async (formData) => {
    const response = await api.post('/complaints', formData, {
        headers: {
            'Content-Type': 'multipart/form-data',
        },
    });
    return response.data;
};

export const deleteComplaint = async (id) => {
    const response = await api.delete(`/complaints/${id}`);
    return response.data;
};

// Announcements Endpoints
export const getAllAnnouncements = async () => {
    const response = await api.get('/announcements');
    return response.data;
};

// The author is always the signed-in Master Admin (the server ignores any client-supplied author)
export const createAnnouncement = async (data) => {
    const response = await api.post('/announcements', data);
    return response.data;
};

export const updateAnnouncement = async (id, data) => {
    const response = await api.put(`/announcements/${id}`, data);
    return response.data;
};

export const deleteAnnouncement = async (id) => {
    const response = await api.delete(`/announcements/${id}`);
    return response.data;
};

// Unit Occupancy Endpoints
export const getRoomOccupants = async (roomId) => {
    const response = await api.get(`/rooms/${roomId}/occupants`);
    return response.data;
};

export const assignRoomOccupant = async (roomId, data) => {
    const response = await api.post(`/rooms/${roomId}/occupants`, data);
    return response.data;
};

export const endRoomOccupancy = async (occupantId, endDate) => {
    let url = `/rooms/occupants/${occupantId}/end`;
    if (endDate) {
        url += `?endDate=${endDate}`;
    }
    const response = await api.put(url);
    return response.data;
};

export const correctRoomOccupantDates = async (occupantId, data) => {
    const response = await api.put(`/rooms/occupants/${occupantId}/correct-dates`, data);
    return response.data;
};

// Society Bank Accounts & Expenses
export const getBankAccounts = async () => {
    const response = await api.get('/bank-accounts');
    return response.data;
};

export const reconcileBankAccounts = async () => {
    const response = await api.get('/bank-accounts/reconcile');
    return response.data;
};

export const reconcileBankAccount = async (id) => {
    const response = await api.get(`/bank-accounts/${id}/reconcile`);
    return response.data;
};

export const createBankAccount = async (data) => {
    const response = await api.post('/bank-accounts', data);
    return response.data;
};

export const updateBankAccount = async (id, data) => {
    const response = await api.put(`/bank-accounts/${id}`, data);
    return response.data;
};

export const recordExpense = async (data) => {
    const response = await api.post('/transactions/expense', data);
    return response.data;
};

export const reverseTransaction = async (id, reason) => {
    const response = await api.post(`/transactions/${id}/reverse`, { reason });
    return { data: response.data, status: response.status };
};

export const getAmendmentRequests = async () => {
    const response = await api.get('/transactions/amendment-requests');
    return response.data;
};

export const resolveAmendmentRequest = async (id, action, reviewNotes) => {
    const response = await api.post(`/transactions/amendment-requests/${id}/resolve`, { action, reviewNotes });
    return response.data;
};
