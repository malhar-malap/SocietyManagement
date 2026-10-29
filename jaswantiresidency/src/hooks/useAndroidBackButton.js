import { useEffect } from 'react';
import { App as CapacitorApp } from '@capacitor/app';
import { isNativeApp } from '../utils/nativeApp';

// Screens where Back leaves the app (sends it to the background) instead of going to the previous page
const ROOT_PATHS = ['/', '/login', '/setup', '/admin/dashboard', '/resident/dashboard'];

// Anything open on top of the page: a dialog, an open menu, the open side drawer or user menu
const OVERLAY_SELECTOR = '[role="dialog"][aria-modal="true"], [role="menu"], .shell.drawer-open, [aria-haspopup="menu"][aria-expanded="true"]';

/**
 * What Android's Back button does, in order: close whatever is open on top (every overlay already closes on
 * Escape), else go to the previous page, else send the app to the background like other Android apps.
 */
export const handleBackButton = ({ canGoBack }, env = {}) => {
    const doc = env.document || document;
    const pathname = env.pathname ?? window.location.pathname;
    if (doc.querySelector(OVERLAY_SELECTOR)) {
        doc.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));
        return 'closed-overlay';
    }
    if (canGoBack && !ROOT_PATHS.includes(pathname)) {
        (env.goBack || (() => window.history.back()))();
        return 'went-back';
    }
    (env.minimize || (() => CapacitorApp.minimizeApp()))();
    return 'minimized';
};

/** Wires the Android Back button; does nothing on the website. */
const useAndroidBackButton = () => {
    useEffect(() => {
        if (!isNativeApp()) return undefined;
        const listener = CapacitorApp.addListener('backButton', (event) => { handleBackButton(event); });
        return () => { listener.then((l) => l.remove()); };
    }, []);
};

export default useAndroidBackButton;
