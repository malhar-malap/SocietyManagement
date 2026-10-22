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

export const createRazorpayOrder = async (paymentData) => {
  const response = await api.post('/payments/create-order', paymentData);
  return response.data;
};

export const verifyPayment = async (verificationData) => {
  const response = await api.post('/payments/verify', verificationData);
  return response.data;
};
