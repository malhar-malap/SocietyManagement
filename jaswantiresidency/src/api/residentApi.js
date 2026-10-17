import api from './axiosConfig';

export const getResidents = async () => {
  const response = await api.get('/residents');
  return response.data;
};

export const getResidentDashboard = async (residentId, roomId) => {
  const response = await api.get(`/resident/dashboard?residentId=${residentId}&roomId=${roomId}`);
  return response.data;
};

export const getResidentProfile = async (id) => {
  const response = await api.get(`/users/profile/${id}`);
  return response.data;
};

export const changePassword = async (id, data) => {
  const response = await api.post(`/users/${id}/change-password`, data);
  return response.data;
};

export const createResident = async (data) => {
  const response = await api.post('/residents', data);
  return response.data;
};

export const updateResident = async (id, data) => {
  const response = await api.put(`/residents/${id}`, data);
  return response.data;
};

export const deleteResident = async (id) => {
  const response = await api.delete(`/residents/${id}`);
  return response.data;
};
