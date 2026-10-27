import React from 'react';
import { render, screen, fireEvent, act } from '@testing-library/react';
import { ToastContainer } from 'react-toastify';
import { deleteWithUndo } from './undo';

beforeEach(() => jest.useFakeTimers());
afterEach(() => { act(() => { jest.runOnlyPendingTimers(); }); jest.useRealTimers(); });

const setup = (commit = jest.fn().mockResolvedValue()) => {
    const calls = { hide: jest.fn(), restore: jest.fn(), commit, onError: jest.fn() };
    render(<ToastContainer />);
    act(() => { deleteWithUndo({ message: 'Request #5 deleted', ...calls }); });
    return calls;
};

test('hides at once, shows an Undo toast with role status, and only deletes after the window', async () => {
    const calls = setup();
    expect(calls.hide).toHaveBeenCalled();
    expect(await screen.findByText('Request #5 deleted')).toBeInTheDocument();
    expect(screen.getByText('Request #5 deleted').closest('[role="status"]')).not.toBeNull();
    expect(calls.commit).not.toHaveBeenCalled();
    await act(async () => { jest.advanceTimersByTime(8000); });
    expect(calls.commit).toHaveBeenCalledTimes(1);
});

test('Undo restores the item and never sends the delete', async () => {
    const calls = setup();
    fireEvent.click(await screen.findByRole('button', { name: 'Undo' }));
    expect(calls.restore).toHaveBeenCalled();
    await act(async () => { jest.advanceTimersByTime(10000); });
    expect(calls.commit).not.toHaveBeenCalled();
});

test('a failed delete puts the item back and reports the error', async () => {
    const calls = setup(jest.fn().mockRejectedValue(new Error('boom')));
    await act(async () => { jest.advanceTimersByTime(8000); });
    expect(calls.restore).toHaveBeenCalled();
    expect(calls.onError).toHaveBeenCalled();
});
