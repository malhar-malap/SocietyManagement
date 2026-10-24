import { useEffect, useState } from 'react';
import api from './axiosConfig';
import { notifyError } from '../utils/notify';

// All protected files are fetched through the authenticated axios client, so the access token travels in the
// Authorization header. It is never put in a URL, where it would leak into logs, history and Referer headers.

const fetchBlob = async (url) => {
    const response = await api.get(url, { responseType: 'blob' });
    const type = response.headers['content-type'] || 'application/octet-stream';
    return { blob: new Blob([response.data], { type }), headers: response.headers };
};

/**
 * Downloads a binary file and triggers a browser save dialog.
 */
export const downloadAuthenticatedFile = async (url, fallbackFilename = 'download') => {
    try {
        const { blob, headers } = await fetchBlob(url);
        const disposition = headers['content-disposition'];
        let filename = fallbackFilename;
        if (disposition) {
            const match = disposition.match(/filename=["']?([^"';]+)["']?/);
            if (match && match[1]) filename = match[1];
        }
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
        notifyError('Failed to download the file. Please try again.');
    }
};

/**
 * Opens a file (PDF/image) in a new tab. The tab is opened synchronously, inside the click, so popup blockers
 * allow it; the file is loaded into it once downloaded.
 */
export const viewAuthenticatedFile = async (url) => {
    const preview = window.open('', '_blank');
    try {
        const { blob } = await fetchBlob(url);
        const blobUrl = window.URL.createObjectURL(blob);
        if (preview) {
            preview.location.href = blobUrl;
        } else {
            window.open(blobUrl, '_blank');
        }
        setTimeout(() => window.URL.revokeObjectURL(blobUrl), 60000);
    } catch (err) {
        console.error('File preview failed:', err);
        if (preview) preview.close();
        notifyError('Failed to open the file. Please try again.');
    }
};

/**
 * Loads a protected image for an <img> tag. Returns { src, error, loading }; src is a blob: URL that is revoked
 * automatically when the URL changes or the component unmounts.
 */
export const useAuthenticatedMedia = (url) => {
    const [state, setState] = useState({ src: null, error: false, loading: Boolean(url) });

    useEffect(() => {
        if (!url) {
            setState({ src: null, error: false, loading: false });
            return undefined;
        }
        let objectUrl = null;
        let cancelled = false;
        setState({ src: null, error: false, loading: true });
        fetchBlob(url)
            .then(({ blob }) => {
                if (cancelled) return;
                objectUrl = window.URL.createObjectURL(blob);
                setState({ src: objectUrl, error: false, loading: false });
            })
            .catch(() => {
                if (!cancelled) setState({ src: null, error: true, loading: false });
            });
        return () => {
            cancelled = true;
            if (objectUrl) window.URL.revokeObjectURL(objectUrl);
        };
    }, [url]);

    return state;
};
