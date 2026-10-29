/**
 * Copies text. Uses the Clipboard API where it works, and falls back to a hidden textarea + execCommand, which
 * Android's WebView (the app) supports even when the Clipboard API is unavailable or refused. Resolves true when
 * the text was copied.
 */
export const copyText = async (text) => {
    const value = String(text ?? '');
    try {
        if (navigator.clipboard && window.isSecureContext) {
            await navigator.clipboard.writeText(value);
            return true;
        }
    } catch {
        // fall through to the legacy path
    }
    const area = document.createElement('textarea');
    area.value = value;
    area.setAttribute('readonly', '');
    area.style.position = 'fixed';
    area.style.opacity = '0';
    area.style.pointerEvents = 'none';
    document.body.appendChild(area);
    area.select();
    area.setSelectionRange(0, value.length);
    let copied = false;
    try {
        copied = document.execCommand('copy');
    } catch {
        copied = false;
    }
    area.remove();
    return copied;
};
