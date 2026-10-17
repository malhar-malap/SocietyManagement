import api from './axiosConfig';

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

export const addResident = async (residentData) => {
    const response = await api.post('/users/register', residentData);
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

export const importResidentsCsv = async (file) => {
    const formData = new FormData();
    formData.append('file', file);
    const response = await api.post('/users/import', formData, {
        headers: {
            'Content-Type': 'multipart/form-data',
        },
    });
    return response.data;
};

export const getActivityLogs = async () => {
    const response = await api.get('/activity-logs');
    return response.data;
};

// Billing Endpoints

export const getBills = async () => {
    const response = await api.get('/bills');
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

export const deleteComplaint = async (id) => {
    const response = await api.delete(`/complaints/${id}`);
    return response.data;
};

// Announcements Endpoints
export const getAllAnnouncements = async () => {
    const response = await api.get('/announcements');
    return response.data;
};

export const createAnnouncement = async (data, adminId) => {
    let url = '/announcements';
    if (adminId) {
        url += `?adminId=${adminId}`;
    }
    const response = await api.post(url, data);
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
