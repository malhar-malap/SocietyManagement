import api from './axiosConfig';

export const getBillsByRoom = async (roomId) => {
  const response = await api.get(`/bills/room/${roomId}`);
  return response.data;
};

export const getMyBills = async () => {
  const response = await api.get('/bills/my-bills');
  return response.data;
};

export const getMyTransactions = async () => {
  const response = await api.get('/transactions/my-transactions');
  return response.data;
};

// 100% Screenshot-Based Resident Payment Submissions
export const submitResidentPayment = async (formData) => {
  const response = await api.post('/payments/submissions', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
  return response.data;
};

export const getMyPaymentSubmissions = async () => {
  const response = await api.get('/payments/submissions/my-submissions');
  return response.data;
};

export const getPaymentSubmissions = async () => {
  const response = await api.get('/payments/submissions');
  return response.data;
};

export const approvePaymentSubmission = async (id, data) => {
  const response = await api.post(`/payments/submissions/${id}/approve`, data);
  return response.data;
};

export const rejectPaymentSubmission = async (id, data) => {
  const response = await api.post(`/payments/submissions/${id}/reject`, data);
  return response.data;
};

// Deprecated legacy Razorpay stubs
export const createRazorpayOrder = async (paymentData) => {
  const response = await api.post('/payments/create-order', paymentData);
  return response.data;
};

export const verifyPayment = async (verificationData) => {
  const response = await api.post('/payments/verify', verificationData);
  return response.data;
};
