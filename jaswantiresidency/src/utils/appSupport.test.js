import { Filesystem } from '@capacitor/filesystem';
import { Share } from '@capacitor/share';
import { isNativeApp } from './nativeApp';
import { safeFilename, saveBlob, withExtension } from './saveFile';
import { copyText } from './clipboard';
import { handleBackButton } from '../hooks/useAndroidBackButton';
import { resolveApiBaseUrl } from '../api/axiosConfig';

jest.mock('./nativeApp', () => ({ isNativeApp: jest.fn(() => false) }));
jest.mock('@capacitor/filesystem', () => ({
    Directory: { Cache: 'CACHE' },
    Filesystem: { writeFile: jest.fn(async () => ({ uri: 'file:///cache/receipt.pdf' })) },
}));
jest.mock('@capacitor/share', () => ({ Share: { share: jest.fn(async () => ({})) } }));
jest.mock('@capacitor/app', () => ({ App: { addListener: jest.fn(), minimizeApp: jest.fn() } }));

afterEach(() => {
    jest.clearAllMocks();
    document.body.innerHTML = '';
});

describe('saving files', () => {
    test('in the Android app a file goes to the share sheet, not a browser download', async () => {
        isNativeApp.mockReturnValue(true);
        Filesystem.writeFile.mockResolvedValue({ uri: 'file:///cache/receipt.pdf' });
        Share.share.mockResolvedValue({});
        const click = jest.spyOn(HTMLAnchorElement.prototype, 'click');
        await saveBlob(new Blob(['%PDF-1.4'], { type: 'application/pdf' }), 'receipt_42');
        expect(Filesystem.writeFile).toHaveBeenCalledWith(expect.objectContaining({ path: 'receipt_42.pdf', directory: 'CACHE' }));
        expect(Share.share).toHaveBeenCalledWith(expect.objectContaining({ files: ['file:///cache/receipt.pdf'] }));
        expect(click).not.toHaveBeenCalled();
        click.mockRestore();
    });

    test('on the website it downloads as before', async () => {
        isNativeApp.mockReturnValue(false);
        window.URL.createObjectURL = jest.fn(() => 'blob:x');
        window.URL.revokeObjectURL = jest.fn();
        const click = jest.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {});
        await saveBlob(new Blob(['a,b'], { type: 'text/csv' }), 'logs.csv');
        expect(click).toHaveBeenCalled();
        expect(Filesystem.writeFile).not.toHaveBeenCalled();
        click.mockRestore();
    });

    test('server-supplied names cannot escape the app folder', () => {
        expect(safeFilename('../../etc/passwd')).toBe('_.._etc_passwd');
        expect(safeFilename('  ')).toBe('download');
        expect(withExtension('invoice', 'application/pdf')).toBe('invoice.pdf');
        expect(withExtension('photo.png', 'image/jpeg')).toBe('photo.png');
    });
});

describe('Android back button', () => {
    test('closes an open dialog first', () => {
        document.body.innerHTML = '<div role="dialog" aria-modal="true"></div>';
        const onKey = jest.fn();
        document.addEventListener('keydown', onKey);
        const goBack = jest.fn();
        expect(handleBackButton({ canGoBack: true }, { pathname: '/admin/billing', goBack })).toBe('closed-overlay');
        expect(onKey.mock.calls[0][0].key).toBe('Escape');
        expect(goBack).not.toHaveBeenCalled();
        document.removeEventListener('keydown', onKey);
    });

    test('goes to the previous page, and leaves the app from a home screen', () => {
        const goBack = jest.fn();
        const minimize = jest.fn();
        expect(handleBackButton({ canGoBack: true }, { pathname: '/admin/billing', goBack, minimize })).toBe('went-back');
        expect(handleBackButton({ canGoBack: true }, { pathname: '/resident/dashboard', goBack, minimize })).toBe('minimized');
        expect(handleBackButton({ canGoBack: false }, { pathname: '/admin/billing', goBack, minimize })).toBe('minimized');
        expect(goBack).toHaveBeenCalledTimes(1);
        expect(minimize).toHaveBeenCalledTimes(2);
    });
});

describe('copy buttons', () => {
    test('fall back to the legacy copy when the Clipboard API is unavailable', async () => {
        Object.defineProperty(navigator, 'clipboard', { value: undefined, configurable: true });
        document.execCommand = jest.fn(() => true);
        await expect(copyText('IFSC0001')).resolves.toBe(true);
        expect(document.execCommand).toHaveBeenCalledWith('copy');
        expect(document.querySelector('textarea')).toBeNull();
    });
});

describe('API address', () => {
    test('production builds use the site\'s own /api, never localhost', () => {
        expect(resolveApiBaseUrl({ NODE_ENV: 'production' })).toBe('/api');
        expect(resolveApiBaseUrl({ NODE_ENV: 'production', REACT_APP_API_BASE_URL: 'https://api.example.in/api' })).toBe('https://api.example.in/api');
        expect(resolveApiBaseUrl({ NODE_ENV: 'development' })).toBe('http://localhost:8080/api');
    });
});
