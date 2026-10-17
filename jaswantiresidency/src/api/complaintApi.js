import api from './axiosConfig';

export const getComplaints = async () => {
  const response = await api.get('/complaints');
  return response.data;
};

export const getComplaintById = async (id) => {
  const response = await api.get(`/complaints/${id}`);
  return response.data;
};

export const getComplaintsByResident = async (residentId) => {
  const response = await api.get(`/complaints/resident/${residentId}`);
  return response.data;
};

export const createComplaint = async (formData) => {
  const response = await api.post('/complaints', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  });
  return response.data;
};

export const updateComplaintStatus = async (id, status) => {
  const response = await api.patch(`/complaints/${id}/status`, { status });
  return response.data;
};

export const deleteComplaint = async (id) => {
  const response = await api.delete(`/complaints/${id}`);
  return response.data;
};
