// One key per user intent (opening the "record payment" dialog, say), reused on every retry of that action,
// so a retry after a lost response is recognised by the server instead of recording the money twice.
export const newIdempotencyKey = (prefix = 'ui') => {
    const id = typeof crypto !== 'undefined' && crypto.randomUUID
        ? crypto.randomUUID()
        : `${Date.now().toString(36)}-${Math.random().toString(36).slice(2)}${Math.random().toString(36).slice(2)}`;
    return `${prefix}-${id}`;
};
