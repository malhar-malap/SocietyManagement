import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import RecordPaymentModal from './RecordPaymentModal';
import { recordPayment } from '../api/adminApi';

jest.mock('../api/adminApi', () => ({ recordPayment: jest.fn() }));

const bill = { id: 81, roomNumber: 'C-101', billingMonth: '2026-09', amount: 3500, lateFee: 0, paidAmount: 0 };

test('a retry after a lost response reuses the same idempotency key', async () => {
    recordPayment
        .mockRejectedValueOnce(new Error('Network Error'))
        .mockResolvedValueOnce({ receiptNumber: 'REC-1', amountPaid: 3500, paymentDate: '2026-10-27', billStatus: 'PAID', remainingBalance: 0, recordedBy: 'Rahul' });

    render(<RecordPaymentModal isOpen bill={bill} onClose={() => {}} />);
    fireEvent.click(screen.getByRole('button', { name: 'Record payment' }));
    expect(await screen.findByText(/it will not be recorded twice/)).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Record payment' }));
    await screen.findByText('Payment recorded');

    expect(recordPayment).toHaveBeenCalledTimes(2);
    const [firstKey, secondKey] = recordPayment.mock.calls.map((call) => call[2]);
    expect(firstKey).toMatch(/^pay-81-/);
    expect(secondKey).toBe(firstKey);
});

test('opening the dialog again starts a new key', async () => {
    recordPayment.mockReset().mockRejectedValue(new Error('Network Error'));
    const { rerender } = render(<RecordPaymentModal isOpen bill={bill} onClose={() => {}} />);
    fireEvent.click(screen.getByRole('button', { name: 'Record payment' }));
    await waitFor(() => expect(recordPayment).toHaveBeenCalledTimes(1));
    await screen.findByText(/it will not be recorded twice/);

    rerender(<RecordPaymentModal isOpen={false} bill={bill} onClose={() => {}} />);
    rerender(<RecordPaymentModal isOpen bill={bill} onClose={() => {}} />);
    fireEvent.click(screen.getByRole('button', { name: 'Record payment' }));
    await waitFor(() => expect(recordPayment).toHaveBeenCalledTimes(2));
    expect(recordPayment.mock.calls[1][2]).not.toBe(recordPayment.mock.calls[0][2]);
});
