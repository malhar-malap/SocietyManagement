import React, { useState, useEffect, useCallback, useMemo, useRef } from 'react';
import { notifySuccess, notifyError, errorMessage } from '../utils/notify';
import PageHeader from '../components/ui/PageHeader';
import DataTable from '../components/ui/DataTable';
import StatusChip from '../components/ui/StatusChip';
import Button from '../components/ui/Button';
import Modal from '../components/ui/Modal';
import Menu from '../components/ui/Menu';
import { Select, TextInput, Textarea } from '../components/ui/Field';
import { Alert, EmptyState, Segmented, Skeleton } from '../components/ui/Feedback';
import DateRangeFilter from '../components/ui/DateRangeFilter';
import { ALL_DATES, inDateRange, isRangeActive } from '../utils/dateRange';
import {
    ArrowDownRight, ArrowLeftRight, ArrowUpRight, Banknote, Building2, CheckCircle2, Download, Lock, Mail,
    MinusCircle, RefreshCw, RotateCcw, Scale, ShieldAlert, Unlock, XCircle,
} from 'lucide-react';
import { formatDate, formatMoney } from '../utils/format';
import { newIdempotencyKey } from '../utils/idempotency';
import { emailReceipt, reconcileBankAccounts } from '../api/adminApi';
import { downloadAuthenticatedFile } from '../api/fileUtils';

const EXPENSE_CATEGORIES = [
    { value: 'STAFF_SALARY', label: 'Staff salary' },
    { value: 'DAILY_WAGES', label: 'Daily wages' },
    { value: 'UTILITY_EXPENSE', label: 'Electricity, water and utilities' },
    { value: 'VENDOR_PAYMENT', label: 'Vendor or contractor' },
    { value: 'OTHER_INCOME', label: 'Other expense' }
];

const PAYMENT_MODES = [
    { value: 'BANK_TRANSFER', label: 'Bank transfer (NEFT / IMPS)' },
    { value: 'CASH', label: 'Cash' },
    { value: 'CHEQUE', label: 'Cheque' },
    { value: 'UPI_MANUAL', label: 'UPI' }
];

const today = () => new Date().toISOString().split('T')[0];

const AdminTransactions = () => {
    const [transactions, setTransactions] = useState([]);
    const [bankAccounts, setBankAccounts] = useState([]);
    const [amendmentRequests, setAmendmentRequests] = useState([]);
    const [isLoading, setIsLoading] = useState(true);

    // Modals
    const [isExpenseModalOpen, setIsExpenseModalOpen] = useState(false);
    const [isReverseModalOpen, setIsReverseModalOpen] = useState(false);
    const [isQueueModalOpen, setIsQueueModalOpen] = useState(false);
    const [isReconcileModalOpen, setIsReconcileModalOpen] = useState(false);
    const [reconciliationList, setReconciliationList] = useState([]);
    const [isLoadingReconciliation, setIsLoadingReconciliation] = useState(false);
    const [selectedTx, setSelectedTx] = useState(null);

    // Form states
    const [expenseForm, setExpenseForm] = useState({
        amount: '',
        category: 'STAFF_SALARY',
        bankAccountId: '',
        paymentMode: 'BANK_TRANSFER',
        referenceNumber: '',
        paymentDate: today(),
        payeeName: '',
        notes: ''
    });
    const [expenseErrors, setExpenseErrors] = useState({});
    const [isSubmittingExpense, setIsSubmittingExpense] = useState(false);
    const expenseKey = useRef(null);
    const expenseOutcomeUnknown = useRef(false);

    const [reverseReason, setReverseReason] = useState('');
    const [reverseError, setReverseError] = useState('');
    const [isSubmittingReverse, setIsSubmittingReverse] = useState(false);
    const [reversalNotification, setReversalNotification] = useState(null);
    const [direction, setDirection] = useState('all');
    const [dateRange, setDateRange] = useState(ALL_DATES);

    const fetchData = useCallback(async () => {
        setIsLoading(true);
        try {
            const { getTransactions, getBankAccounts, getAmendmentRequests } = await import('../api/adminApi');
            const [txData, bankData, amendData] = await Promise.all([
                getTransactions().catch(() => []),
                getBankAccounts().catch(() => []),
                getAmendmentRequests().catch(() => [])
            ]);
            setTransactions(txData);
            setBankAccounts(bankData);
            setAmendmentRequests(amendData);

            if (bankData.length > 0 && !expenseForm.bankAccountId) {
                setExpenseForm(prev => ({ ...prev, bankAccountId: bankData[0].id }));
            }
        } catch (err) {
            notifyError('Could not load the ledger. Please refresh the page.');
        } finally {
            setIsLoading(false);
        }
    }, [expenseForm.bankAccountId]);

    useEffect(() => {
        fetchData();
    }, [fetchData]);

    const handleOpenExpense = () => {
        setExpenseErrors({});
        setExpenseForm({
            amount: '',
            category: 'STAFF_SALARY',
            bankAccountId: bankAccounts[0]?.id || '',
            paymentMode: 'BANK_TRANSFER',
            referenceNumber: '',
            paymentDate: today(),
            payeeName: '',
            notes: ''
        });
        expenseKey.current = newIdempotencyKey('exp');
        expenseOutcomeUnknown.current = false;
        setIsExpenseModalOpen(true);
    };

    const handleRecordExpense = async (e) => {
        e.preventDefault();
        const errs = {};
        const amt = parseFloat(expenseForm.amount);
        if (!expenseForm.amount || isNaN(amt) || amt <= 0) {
            errs.amount = 'Enter an amount above zero.';
        }
        if (!expenseForm.bankAccountId) {
            errs.bankAccountId = 'Choose the account it was paid from.';
        }
        if (expenseForm.paymentMode !== 'CASH' && !expenseForm.referenceNumber.trim()) {
            errs.referenceNumber = 'Add the transfer, cheque or UPI reference.';
        }
        if (!expenseForm.paymentDate) {
            errs.paymentDate = 'Choose the payment date.';
        } else if (expenseForm.paymentDate > today()) {
            errs.paymentDate = 'The date cannot be in the future.';
        }

        if (Object.keys(errs).length > 0) {
            setExpenseErrors(errs);
            return;
        }

        setIsSubmittingExpense(true);
        setExpenseErrors({});
        try {
            const { recordExpense } = await import('../api/adminApi');
            const payload = {
                amount: parseFloat(amt.toFixed(2)),
                category: expenseForm.category,
                bankAccountId: Number(expenseForm.bankAccountId),
                paymentMode: expenseForm.paymentMode,
                referenceNumber: expenseForm.paymentMode !== 'CASH' ? expenseForm.referenceNumber.trim() : null,
                paymentDate: expenseForm.paymentDate,
                payeeName: expenseForm.payeeName.trim() || null,
                notes: expenseForm.notes.trim() || null,
                idempotencyKey: expenseKey.current
            };
            await recordExpense(payload);
            notifySuccess(`Expense of ${formatMoney(payload.amount, 2)} recorded`);
            setIsExpenseModalOpen(false);
            await fetchData();
        } catch (err) {
            // The server rejects a reused key. If the previous try never got an answer, that means it was saved.
            if (err?.response?.status === 409 && expenseOutcomeUnknown.current) {
                notifySuccess('This expense was already recorded');
                setIsExpenseModalOpen(false);
                await fetchData();
                return;
            }
            if (!err?.response) expenseOutcomeUnknown.current = true;
            const msg = err?.response?.data?.message || (err?.response
                ? 'We could not record this expense. Please try again.'
                : 'No response from the server. Try again — it will not be recorded twice.');
            setExpenseErrors({ submit: msg });
        } finally {
            setIsSubmittingExpense(false);
        }
    };

    const handleOpenReverse = (tx) => {
        setSelectedTx(tx);
        setReverseReason('');
        setReverseError('');
        setIsReverseModalOpen(true);
    };

    const handleConfirmReverse = async (e) => {
        e.preventDefault();
        if (!reverseReason.trim()) {
            setReverseError('Explain why this entry is being reversed.');
            return;
        }

        setIsSubmittingReverse(true);
        setReverseError('');
        try {
            const { reverseTransaction } = await import('../api/adminApi');
            const res = await reverseTransaction(selectedTx.id, reverseReason.trim());
            setIsReverseModalOpen(false);

            if (res.status === 202 || res.data?.pendingApproval) {
                setReversalNotification({
                    type: 'warning',
                    title: 'Sent for master admin approval',
                    message: `Entry #${selectedTx.id} is more than 24 hours old, so the reversal is waiting in the master admin’s approval queue. Balances change once it is approved.`
                });
            } else {
                setReversalNotification({
                    type: 'success',
                    title: 'Entry reversed',
                    message: `Entry #${selectedTx.id} has been reversed. Account balances and the related bill were updated.`
                });
            }
            await fetchData();
        } catch (err) {
            const msg = err?.response?.data?.message || 'We could not reverse this entry. Please try again.';
            setReverseError(msg);
        } finally {
            setIsSubmittingReverse(false);
        }
    };

    const handleResolveAmendment = async (reqId, action) => {
        try {
            const { resolveAmendmentRequest } = await import('../api/adminApi');
            await resolveAmendmentRequest(reqId, action, `Resolved via Admin Portal`);
            notifySuccess(action === 'APPROVE' ? 'Reversal approved and applied' : 'Request rejected');
            await fetchData();
        } catch (err) {
            notifyError(errorMessage(err, 'We could not resolve this request.'));
        }
    };

    const handleOpenReconcile = async () => {
        setIsReconcileModalOpen(true);
        setIsLoadingReconciliation(true);
        try {
            const data = await reconcileBankAccounts();
            setReconciliationList(Array.isArray(data) ? data : []);
        } catch (err) {
            notifyError(errorMessage(err, 'Reconciliation failed. Please try again.'));
        } finally {
            setIsLoadingReconciliation(false);
        }
    };

    const pendingQueueCount = amendmentRequests.filter(r => r.status === 'PENDING').length;

    const sendReceiptEmail = async (row) => {
        try {
            const res = await emailReceipt(row.id);
            notifySuccess(`Receipt emailed to ${res.sentTo}`);
        } catch (err) {
            notifyError(errorMessage(err, 'We could not email the receipt.'));
        }
    };

    const inRange = useMemo(
        () => transactions.filter((t) => inDateRange(t.paymentDate || t.timestamp, dateRange)),
        [transactions, dateRange]);

    const totals = useMemo(() => inRange.reduce((acc, t) => {
        if (t.status !== 'SUCCESS') return acc;
        if (t.entryDirection === 'DEBIT') acc.out += Number(t.amount || 0);
        else acc.in += Number(t.amount || 0);
        return acc;
    }, { in: 0, out: 0 }), [inRange]);

    const visible = useMemo(() => {
        if (direction === 'in') return inRange.filter((t) => t.entryDirection !== 'DEBIT');
        if (direction === 'out') return inRange.filter((t) => t.entryDirection === 'DEBIT');
        return inRange;
    }, [inRange, direction]);

    const payeeOf = (row) => (row.roomNumber && row.roomNumber !== 'Unassigned'
        ? `${row.roomNumber}${row.residentName && row.residentName !== 'Unknown' ? ` · ${row.residentName}` : ''}`
        : (row.notes ? row.notes.split('.')[0] : 'Society operations'));

    const columns = [
        {
            header: 'Entry',
            searchValue: (row) => `${row.receiptNumber || ''} ${row.referenceNumber || ''} ${payeeOf(row)}`,
            render: (row) => {
                const isCredit = row.entryDirection !== 'DEBIT';
                return (
                    <div className="row" style={{ '--gap': '0.7rem' }}>
                        <span className={`tx-icon ${isCredit ? 'is-in' : 'is-out'}`}>{isCredit ? <ArrowDownRight size={15} /> : <ArrowUpRight size={15} />}</span>
                        <div style={{ minWidth: 0 }}>
                            <div className="cell-primary truncate" style={{ maxWidth: 260 }}>{payeeOf(row)}</div>
                            <div className="cell-sub">{(row.category || 'MAINTENANCE').replace(/_/g, ' ').toLowerCase()} · {row.receiptNumber || `#${row.id}`}</div>
                        </div>
                    </div>
                );
            },
        },
        {
            header: 'Account',
            render: (row) => (
                <div>
                    <div className="text-sm">{row.bankAccountName || 'Main bank account'}</div>
                    {row.referenceNumber && <div className="cell-sub"><code>{row.referenceNumber}</code></div>}
                </div>
            ),
        },
        { header: 'Date', render: (row) => <span className="text-muted nowrap">{formatDate(row.paymentDate || row.timestamp)}</span> },
        {
            header: 'Status',
            render: (row) => (
                <div className="row" style={{ '--gap': '0.4rem' }}>
                    <StatusChip status={row.status} label={row.status === 'SUCCESS' ? 'Settled' : undefined} />
                    {row.status === 'SUCCESS' && (
                        <span className="text-subtle" title={row.locked ? 'Older than 24 hours: reversing needs master admin approval' : 'Within 24 hours: can be reversed directly'}>
                            {row.locked ? <Lock size={13} aria-label="Locked" /> : <Unlock size={13} aria-label="Editable" />}
                        </span>
                    )}
                </div>
            ),
        },
        {
            header: 'Amount',
            align: 'right',
            render: (row) => {
                const isCredit = row.entryDirection !== 'DEBIT';
                return <span className={`fw-600 ${isCredit ? 'text-success' : ''}`}>{isCredit ? '+' : '−'}{formatMoney(row.amount, 2)}</span>;
            },
        },
        {
            header: <span className="visually-hidden">Actions</span>,
            render: (row) => (
                <div className="row-actions">
                    <Button variant="ghost" size="sm" iconOnly icon={Download} title="Download receipt" aria-label={`Download receipt for entry ${row.id}`}
                        onClick={() => downloadAuthenticatedFile(`/transactions/${row.id}/receipt-pdf`, `receipt_${row.receiptNumber || row.id}.pdf`)} />
                    <Menu
                        label={`More actions for entry ${row.id}`}
                        items={[
                            { label: 'Email receipt to resident', icon: Mail, onClick: () => sendReceiptEmail(row), hidden: row.entryDirection === 'DEBIT' },
                            { label: row.locked ? 'Request reversal…' : 'Reverse entry…', icon: RotateCcw, danger: true, onClick: () => handleOpenReverse(row), hidden: row.status !== 'SUCCESS' },
                        ]}
                    />
                </div>
            ),
        },
    ];

    const toolbar = (
        <>
            <Segmented
                label="Filter by direction"
                value={direction}
                onChange={setDirection}
                options={[{ value: 'all', label: 'All' }, { value: 'in', label: 'Money in' }, { value: 'out', label: 'Money out' }]}
            />
            <DateRangeFilter value={dateRange} onChange={setDateRange} label="Filter entries by date" />
        </>
    );

    const setField = (key) => (e) => setExpenseForm({ ...expenseForm, [key]: e.target.value });

    return (
        <div>
            <PageHeader
                title="Transactions"
                description="The society’s ledger: every payment received and every expense paid, with live account balances."
                actions={(
                    <>
                        {pendingQueueCount > 0 && (
                            <Button variant="secondary" icon={ShieldAlert} onClick={() => setIsQueueModalOpen(true)}>
                                Approvals<span className="count-pill">{pendingQueueCount}</span>
                            </Button>
                        )}
                        <Button variant="secondary" icon={Scale} onClick={handleOpenReconcile}>Reconcile</Button>
                        <Button icon={MinusCircle} onClick={handleOpenExpense}>Record expense</Button>
                    </>
                )}
            />

            <div className="balance-strip mb-6">
                {isLoading && bankAccounts.length === 0 ? (
                    [0, 1, 2].map((i) => <Skeleton key={i} height={86} style={{ borderRadius: 14 }} />)
                ) : (
                    <>
                        {bankAccounts.map((acc) => {
                            const isCash = acc.accountType === 'CASH_LEDGER';
                            return (
                                <div key={acc.id} className="card stat">
                                    <div className="stat-top">
                                        <span className="stat-label truncate">{acc.accountName}</span>
                                        <span className={`stat-icon tone-${isCash ? 'warning' : 'info'}`}>{isCash ? <Banknote size={16} /> : <Building2 size={16} />}</span>
                                    </div>
                                    <div className="stat-value">{formatMoney(acc.currentBalance, 2)}</div>
                                </div>
                            );
                        })}
                        <div className="card stat flow-card">
                            <div className="stat-top"><span className="stat-label">{isRangeActive(dateRange) ? 'Totals for selected dates' : 'Ledger totals'}</span>
                                <Button variant="ghost" size="sm" iconOnly icon={RefreshCw} aria-label="Refresh balances" onClick={fetchData} /></div>
                            <div className="flow-rows">
                                <span>In</span><strong className="num text-success">+{formatMoney(totals.in)}</strong>
                                <span>Out</span><strong className="num">−{formatMoney(totals.out)}</strong>
                            </div>
                        </div>
                    </>
                )}
            </div>

            {reversalNotification && (
                <Alert
                    tone={reversalNotification.type === 'warning' ? 'warning' : 'success'}
                    title={reversalNotification.title}
                    className="mb-4"
                    action={<button type="button" className="icon-btn icon-btn-sm" aria-label="Dismiss" onClick={() => setReversalNotification(null)}><XCircle size={16} /></button>}
                >
                    {reversalNotification.message}
                </Alert>
            )}

            <DataTable
                columns={columns}
                data={visible}
                isLoading={isLoading}
                toolbar={toolbar}
                searchPlaceholder="Search flat, receipt, reference or note"
                emptyIcon={ArrowLeftRight}
                emptyTitle="No entries yet"
                emptyMessage="Payments and expenses will appear here as they are recorded."
            />

            <Modal
                isOpen={isExpenseModalOpen}
                onClose={() => setIsExpenseModalOpen(false)}
                as="form"
                onSubmit={handleRecordExpense}
                icon={MinusCircle}
                title="Record an expense"
                description="Salaries, wages, utilities and vendor payments. The amount is taken out of the account you choose."
                dismissible={!isSubmittingExpense}
                footer={(
                    <>
                        <Button variant="secondary" onClick={() => setIsExpenseModalOpen(false)} disabled={isSubmittingExpense}>Cancel</Button>
                        <Button type="submit" isLoading={isSubmittingExpense}>Record expense</Button>
                    </>
                )}
            >
                <div className="stack">
                    {expenseErrors.submit && <Alert tone="error">{expenseErrors.submit}</Alert>}
                    <div className="form-grid">
                        <Select label="Category" value={expenseForm.category} onChange={setField('category')}>
                            {EXPENSE_CATEGORIES.map((c) => <option key={c.value} value={c.value}>{c.label}</option>)}
                        </Select>
                        <Select label="Paid from" value={expenseForm.bankAccountId} onChange={setField('bankAccountId')} error={expenseErrors.bankAccountId}>
                            {bankAccounts.map((b) => <option key={b.id} value={b.id}>{b.accountName} · {formatMoney(b.currentBalance)}</option>)}
                        </Select>
                        <TextInput label="Amount" prefix="₹" type="number" step="0.01" min="0.01" placeholder="0.00" value={expenseForm.amount} onChange={setField('amount')} error={expenseErrors.amount} />
                        <TextInput label="Paid on" type="date" max={today()} value={expenseForm.paymentDate} onChange={setField('paymentDate')} error={expenseErrors.paymentDate} />
                        <Select label="Payment mode" value={expenseForm.paymentMode} onChange={setField('paymentMode')}>
                            {PAYMENT_MODES.map((m) => <option key={m.value} value={m.value}>{m.label}</option>)}
                        </Select>
                        <TextInput label="Reference" optional={expenseForm.paymentMode === 'CASH'} placeholder={expenseForm.paymentMode === 'CASH' ? '' : 'UTR, cheque or UPI number'}
                            value={expenseForm.referenceNumber} onChange={setField('referenceNumber')} error={expenseErrors.referenceNumber} />
                        <TextInput label="Paid to" optional className="span-all" placeholder="e.g. Ramesh (security), MSEDCL electricity" value={expenseForm.payeeName} onChange={setField('payeeName')} />
                        <Textarea label="Notes" optional className="span-all" rows={2} placeholder="What was this for?" value={expenseForm.notes} onChange={setField('notes')} />
                    </div>
                </div>
            </Modal>

            <Modal
                isOpen={isReverseModalOpen && Boolean(selectedTx)}
                onClose={() => setIsReverseModalOpen(false)}
                as="form"
                onSubmit={handleConfirmReverse}
                icon={RotateCcw}
                tone="danger"
                title={selectedTx?.locked ? 'Request a reversal' : 'Reverse this entry'}
                description={selectedTx ? `#${selectedTx.id} · ${selectedTx.entryDirection === 'DEBIT' ? 'expense' : 'payment'} of ${formatMoney(selectedTx.amount, 2)}` : undefined}
                dismissible={!isSubmittingReverse}
                footer={(
                    <>
                        <Button variant="secondary" onClick={() => setIsReverseModalOpen(false)} disabled={isSubmittingReverse}>Cancel</Button>
                        <Button type="submit" variant="danger" isLoading={isSubmittingReverse}>{selectedTx?.locked ? 'Send for approval' : 'Reverse entry'}</Button>
                    </>
                )}
            >
                {selectedTx && (
                    <div className="stack">
                        {selectedTx.locked ? (
                            <Alert tone="warning" title="Older than 24 hours">This entry is locked, so your request goes to the master admin. Balances change only after they approve it.</Alert>
                        ) : (
                            <Alert tone="info" title="Within the 24-hour window">Reversing restores the account balance and the related bill straight away.</Alert>
                        )}
                        <Textarea label="Reason" rows={3} value={reverseReason} onChange={(e) => setReverseReason(e.target.value)} error={reverseError}
                            placeholder="e.g. Credited to the wrong flat, or recorded twice." hint="Saved to the system log." />
                    </div>
                )}
            </Modal>

            <Modal
                isOpen={isQueueModalOpen}
                onClose={() => setIsQueueModalOpen(false)}
                size="lg"
                icon={ShieldAlert}
                tone="warning"
                title="Reversal approvals"
                description="Reversal requests for entries older than 24 hours."
                footer={<Button variant="secondary" onClick={() => setIsQueueModalOpen(false)}>Close</Button>}
            >
                {amendmentRequests.length === 0 ? (
                    <EmptyState compact icon={CheckCircle2} title="Nothing to review">There are no reversal requests.</EmptyState>
                ) : (
                    <ul className="occupant-list">
                        {amendmentRequests.map((req) => (
                            <li key={req.id}>
                                <div className="occupant-row" style={{ alignItems: 'flex-start' }}>
                                    <div className="grow stack stack-sm">
                                        <div className="row row-wrap" style={{ '--gap': '0.5rem' }}>
                                            <span className="fw-600">Entry #{req.transactionId}</span>
                                            <StatusChip status={req.status} />
                                        </div>
                                        <div className="text-sm text-muted">Requested by {req.requestedByAdminName || 'an admin'}</div>
                                        <blockquote className="quote">{req.reason}</blockquote>
                                    </div>
                                </div>
                                {req.status === 'PENDING' && (
                                    <div className="occupant-panel" style={{ flexDirection: 'row', justifyContent: 'flex-end' }}>
                                        <Button variant="danger-ghost" size="sm" icon={XCircle} onClick={() => handleResolveAmendment(req.id, 'REJECT')}>Reject</Button>
                                        <Button variant="success" size="sm" icon={CheckCircle2} onClick={() => handleResolveAmendment(req.id, 'APPROVE')}>Approve and apply</Button>
                                    </div>
                                )}
                            </li>
                        ))}
                    </ul>
                )}
            </Modal>

            <Modal
                isOpen={isReconcileModalOpen}
                onClose={() => setIsReconcileModalOpen(false)}
                size="lg"
                icon={Scale}
                tone="success"
                title="Reconcile accounts"
                description="Opening balance + money in − money out, checked against each account’s running balance."
                footer={<Button variant="secondary" onClick={() => setIsReconcileModalOpen(false)}>Close</Button>}
            >
                {isLoadingReconciliation ? (
                    <div className="stack">{[0, 1].map((i) => <Skeleton key={i} height={120} style={{ borderRadius: 10 }} />)}</div>
                ) : reconciliationList.length === 0 ? (
                    <EmptyState compact icon={Building2} title="No accounts">Add a bank account to reconcile it.</EmptyState>
                ) : (
                    <div className="stack">
                        {reconciliationList.map((rec) => (
                            <section key={rec.bankAccountId} className="recon">
                                <div className="row row-between row-wrap">
                                    <div>
                                        <div className="fw-600">{rec.accountName}</div>
                                        <div className="text-xs text-subtle">{rec.accountType === 'CASH_LEDGER' ? 'Petty cash' : 'Bank account'} · {rec.transactionCount} entries checked</div>
                                    </div>
                                    {rec.reconciled
                                        ? <StatusChip tone="success" label="Balanced" />
                                        : <StatusChip tone="danger" label={`Off by ${formatMoney(rec.discrepancy, 2)}`} />}
                                </div>
                                <div className="recon-grid">
                                    <div><span>Opening</span><strong>{formatMoney(rec.openingBalance, 2)}</strong></div>
                                    <div><span>Money in</span><strong className="text-success">+{formatMoney(rec.totalCredits, 2)}</strong></div>
                                    <div><span>Money out</span><strong>−{formatMoney(rec.totalDebits, 2)}</strong></div>
                                    <div><span>Expected</span><strong>{formatMoney(rec.calculatedBalance, 2)}</strong></div>
                                    <div><span>Actual</span><strong>{formatMoney(rec.currentBalance, 2)}</strong></div>
                                </div>
                            </section>
                        ))}
                    </div>
                )}
            </Modal>
        </div>
    );
};

export default AdminTransactions;
