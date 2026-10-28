import { isOverdue, isPaid, isUnpaid } from './billing';

const bill = (billingMonth, status = 'PENDING') => ({ billingMonth, status, dueDate: `${billingMonth}-10` });

describe('unpaid vs overdue', () => {
    const oct27 = new Date(2026, 9, 27);

    test('a bill is unpaid for its whole billing month, even after the due date', () => {
        expect(isUnpaid(bill('2026-10'), oct27)).toBe(true);
        expect(isOverdue(bill('2026-10'), oct27)).toBe(false);
        expect(isUnpaid(bill('2026-10'), new Date(2026, 9, 31))).toBe(true);
    });

    test('it becomes overdue once the next month starts', () => {
        const nov1 = new Date(2026, 10, 1);
        expect(isOverdue(bill('2026-10'), nov1)).toBe(true);
        expect(isUnpaid(bill('2026-10'), nov1)).toBe(false);
        expect(isOverdue(bill('2025-12', 'PARTIALLY_PAID'), oct27)).toBe(true);
    });

    test('paid bills are neither, and the two filters never overlap', () => {
        expect(isOverdue(bill('2026-08', 'PAID'), oct27)).toBe(false);
        expect(isUnpaid(bill('2026-10', 'PAID'), oct27)).toBe(false);
        ['2026-08', '2026-09', '2026-10', '2026-11'].forEach((m) => {
            expect(isOverdue(bill(m), oct27) && isUnpaid(bill(m), oct27)).toBe(false);
        });
    });

    test('falls back to the due date month when there is no billing month', () => {
        expect(isOverdue({ status: 'PENDING', dueDate: '2026-09-10' }, oct27)).toBe(true);
    });

    test('cash payments count as paid', () => {
        expect(isPaid({ status: 'PAID_CASH' })).toBe(true);
        expect(isPaid({ status: 'PAID' })).toBe(true);
        expect(isPaid({ status: 'PARTIALLY_PAID' })).toBe(false);
    });
});
