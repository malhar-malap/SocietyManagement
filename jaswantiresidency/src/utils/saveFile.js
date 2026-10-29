import { Directory, Filesystem } from '@capacitor/filesystem';
import { Share } from '@capacitor/share';
import { isNativeApp } from './nativeApp';

// Android's WebView ignores <a download> and new tabs, so inside the app a file is written to the app's cache and
// handed to the share sheet (open in a PDF viewer, save to Files, send on WhatsApp). The website downloads as usual.

/** Keeps a server-supplied name safe to use as a file path. */
export const safeFilename = (name, fallback = 'download') => {
    // Drop control characters, then anything a file path could misuse
    const printable = Array.from(String(name || '')).filter((c) => c.charCodeAt(0) >= 32).join('');
    const cleaned = printable.replace(/[\\/:*?"<>|]+/g, '_').replace(/^\.+/, '').trim();
    return cleaned.slice(0, 120) || fallback;
};

const toBase64 = (blob) => new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result).split(',', 2)[1] || '');
    reader.onerror = () => reject(reader.error || new Error('Could not read the file'));
    reader.readAsDataURL(blob);
});

const EXTENSIONS = {
    'application/pdf': 'pdf',
    'image/jpeg': 'jpg',
    'image/png': 'png',
    'text/csv': 'csv',
    'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet': 'xlsx',
};

/** Viewer apps pick a file by its extension, so add one from the content type when the name has none. */
export const withExtension = (filename, type) => {
    if (/\.[a-z0-9]{2,5}$/i.test(filename)) return filename;
    const ext = EXTENSIONS[String(type || '').split(';')[0].trim()];
    return ext ? `${filename}.${ext}` : filename;
};

const shareFromApp = async (blob, filename) => {
    const { uri } = await Filesystem.writeFile({
        path: withExtension(filename, blob.type),
        data: await toBase64(blob),
        directory: Directory.Cache,
    });
    await Share.share({ title: filename, files: [uri], dialogTitle: 'Open or save file' });
};

/** Saves a file: a normal download on the website, the share sheet in the app. */
export const saveBlob = async (blob, filename) => {
    const name = safeFilename(filename);
    if (isNativeApp()) {
        await shareFromApp(blob, name);
        return;
    }
    const blobUrl = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = blobUrl;
    link.download = name;
    document.body.appendChild(link);
    link.click();
    link.remove();
    setTimeout(() => window.URL.revokeObjectURL(blobUrl), 2000);
};

/** Shows a file: a new tab on the website (opened by the caller inside the click), the share sheet in the app. */
export const previewBlob = async (blob, filename, previewWindow) => {
    if (isNativeApp()) {
        await shareFromApp(blob, safeFilename(filename, 'file'));
        return;
    }
    const blobUrl = window.URL.createObjectURL(blob);
    if (previewWindow) {
        previewWindow.location.href = blobUrl;
    } else {
        window.open(blobUrl, '_blank');
    }
    setTimeout(() => window.URL.revokeObjectURL(blobUrl), 60000);
};
