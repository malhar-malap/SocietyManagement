import React from 'react';
import { toast } from 'react-toastify';

export const UNDO_WINDOW_MS = 8000;

const UndoToast = ({ message, onUndo, closeToast }) => (
    <div className="undo-toast">
        <span>{message}</span>
        <button type="button" className="btn btn-link btn-sm" onClick={() => { onUndo(); closeToast(); }}>Undo</button>
    </div>
);

/**
 * Reversible deletes without a confirm dialog: the item disappears at once (hide), a toast offers Undo, and the
 * real request (commit) is only sent when the undo window ends. Undo calls restore and nothing reaches the server.
 * If the request then fails, the item is restored and the error shown. Closing the tab inside the window simply
 * means nothing was deleted.
 */
export const deleteWithUndo = ({ message, hide, restore, commit, onError, delay = UNDO_WINDOW_MS }) => {
    let undone = false;
    hide();
    const timer = setTimeout(async () => {
        if (undone) return;
        try {
            await commit();
        } catch (err) {
            restore();
            if (onError) onError(err);
        }
    }, delay);

    toast(
        ({ closeToast }) => (
            <UndoToast
                message={message}
                closeToast={closeToast}
                onUndo={() => { undone = true; clearTimeout(timer); restore(); }}
            />
        ),
        { autoClose: delay, closeOnClick: false, pauseOnHover: false, pauseOnFocusLoss: false, draggable: false, role: 'status', icon: false },
    );
};
