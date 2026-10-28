// A bill is "unpaid" during its own billing month and becomes "overdue" once that month has ended without full payment
export const OPEN_STATUSES = ['PENDING', 'PARTIALLY_PAID'];

const monthKey = (date) => `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;

// "YYYY-MM" for the bill; falls back to the due date's month for bills without a billing month
const billMonth = (bill) => String(bill.billingMonth || bill.dueDate || '').slice(0, 7);

export const isOpen = (bill) => OPEN_STATUSES.includes(bill.status);

export const isOverdue = (bill, today = new Date()) => {
    const month = billMonth(bill);
    return isOpen(bill) && month.length === 7 && month < monthKey(today);
};

export const isUnpaid = (bill, today = new Date()) => isOpen(bill) && !isOverdue(bill, today);

export const isPaid = (bill) => bill.status === 'PAID' || bill.status === 'PAID_CASH';
