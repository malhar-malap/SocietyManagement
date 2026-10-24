import { toast } from 'react-toastify';

// One place for user-facing feedback, replacing blocking alert() dialogs

export const notifySuccess = (message) => toast.success(message);

export const notifyError = (message) => toast.error(message);

export const notifyInfo = (message) => toast.info(message);

/** Best human-readable message from an axios error, falling back to the given text. */
export const errorMessage = (err, fallback) => {
    const data = err?.response?.data;
    if (typeof data === 'string' && data.trim()) return data;
    return data?.message || data?.error || fallback;
};
