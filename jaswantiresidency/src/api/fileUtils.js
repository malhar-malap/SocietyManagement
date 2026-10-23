import api from './axiosConfig';

/**
 * Downloads a binary file using authenticated Axios and triggers a browser save dialog.
 */
export const downloadAuthenticatedFile = async (url, fallbackFilename = 'download') => {
    try {
        const token = localStorage.getItem('accessToken');
        const urlWithToken = `${url}${url.includes('?') ? '&' : '?'}token=${encodeURIComponent(token || '')}`;
        const response = await api.get(urlWithToken, { responseType: 'blob' });
        
        const disposition = response.headers['content-disposition'];
        let filename = fallbackFilename;
        if (disposition) {
            const match = disposition.match(/filename=["']?([^"';]+)["']?/);
            if (match && match[1]) filename = match[1];
        }

        const blob = new Blob([response.data], { 
            type: response.headers['content-type'] || 'application/octet-stream' 
        });
        const blobUrl = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = blobUrl;
        link.download = filename;
        document.body.appendChild(link);
        link.click();
        link.remove();
        setTimeout(() => window.URL.revokeObjectURL(blobUrl), 2000);
    } catch (err) {
        console.error('File download failed:', err);
        alert('Failed to download file. Please ensure you are logged in.');
    }
};

/**
 * Opens a file (PDF/Image) in a new browser tab with authentication.
 */
export const viewAuthenticatedFile = async (url) => {
    try {
        const token = localStorage.getItem('accessToken');
        const urlWithToken = `${url}${url.includes('?') ? '&' : '?'}token=${encodeURIComponent(token || '')}`;
        const response = await api.get(urlWithToken, { responseType: 'blob' });
        const contentType = response.headers['content-type'] || 'application/pdf';
        const blob = new Blob([response.data], { type: contentType });
        const blobUrl = window.URL.createObjectURL(blob);
        window.open(blobUrl, '_blank');
        setTimeout(() => window.URL.revokeObjectURL(blobUrl), 60000);
    } catch (err) {
        console.error('File preview failed:', err);
        alert('Failed to preview file.');
    }
};

/**
 * Generates an authenticated URL for <img> tags.
 */
export const getAuthenticatedMediaUrl = (url) => {
    const token = localStorage.getItem('accessToken');
    if (!token) return url;
    const separator = url.includes('?') ? '&' : '?';
    return `${url}${separator}token=${encodeURIComponent(token)}`;
};
