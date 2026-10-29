import { useEffect, useState } from 'react';
import api from './axiosConfig';
import { notifyError } from '../utils/notify';
import { isNativeApp } from '../utils/nativeApp';
import { previewBlob, saveBlob } from '../utils/saveFile';

// All protected files are fetched through the authenticated axios client, so the access token travels in the
// Authorization header. It is never put in a URL, where it would leak into logs, history and Referer headers.

const fetchBlob = async (url) => {
    const response = await api.get(url, { responseType: 'blob' });
    const type = response.headers['content-type'] || 'application/octet-stream';
    return { blob: new Blob([response.data], { type }), headers: response.headers };
};

const filenameFrom = (headers, fallback) => {
    const match = (headers['content-disposition'] || '').match(/filename=["']?([^"';]+)["']?/);
    return match && match[1] ? match[1] : fallback;
};

/**
 * Downloads a file: a browser save on the website, the share sheet in the Android app.
 */
export const downloadAuthenticatedFile = async (url, fallbackFilename = 'download') => {
    try {
        const { blob, headers } = await fetchBlob(url);
        await saveBlob(blob, filenameFrom(headers, fallbackFilename));
    } catch (err) {
        console.error('File download failed:', err);
        notifyError('Failed to download the file. Please try again.');
    }
};

/**
 * Opens a file (PDF/image). On the website it opens in a new tab, opened synchronously inside the click so popup
 * blockers allow it; in the Android app it goes to the share sheet, where the resident picks a viewer.
 */
export const viewAuthenticatedFile = async (url, fallbackFilename = 'file') => {
    const preview = isNativeApp() ? null : window.open('', '_blank');
    try {
        const { blob, headers } = await fetchBlob(url);
        await previewBlob(blob, filenameFrom(headers, fallbackFilename), preview);
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
