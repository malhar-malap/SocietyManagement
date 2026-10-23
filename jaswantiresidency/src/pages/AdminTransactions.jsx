import React, { useState, useEffect, useCallback } from 'react';
import PageHeader from '../components/ui/PageHeader';
import DataTable from '../components/ui/DataTable';
import StatusChip from '../components/ui/StatusChip';
import Button from '../components/ui/Button';
import {
    Building2, Wallet, Banknote, Plus, RotateCcw, Lock, Unlock,
    CheckCircle2, XCircle, AlertCircle, X, ArrowDownRight, ArrowUpRight,
    ShieldAlert, RefreshCw, Download, Mail, Scale
} from 'lucide-react';
import { emailReceipt, reconcileBankAccounts } from '../api/adminApi';
import { downloadAuthenticatedFile } from '../api/fileUtils';
import '../components/ui/shared.css';

const EXPENSE_CATEGORIES = [
    { value: 'STAFF_SALARY', label: 'Staff Salary' },
    { value: 'DAILY_WAGES', label: 'Daily Wages' },
    { value: 'UTILITY_EXPENSE', label: 'Electricity / Water / Utility' },
    { value: 'VENDOR_PAYMENT', label: 'Vendor / Contractor Payment' },
    { value: 'OTHER_INCOME', label: 'Other Operational Expense' }
];

const PAYMENT_MODES = [
    { value: 'BANK_TRANSFER', label: 'Bank Transfer (NEFT/IMPS)' },
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

    const [reverseReason, setReverseReason] = useState('');
    const [reverseError, setReverseError] = useState('');
    const [isSubmittingReverse, setIsSubmittingReverse] = useState(false);
    const [reversalNotification, setReversalNotification] = useState(null);

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
            console.error("Failed to fetch transaction data", err);
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
        setIsExpenseModalOpen(true);
    };

    const handleRecordExpense = async (e) => {
        e.preventDefault();
        const errs = {};
        const amt = parseFloat(expenseForm.amount);
        if (!expenseForm.amount || isNaN(amt) || amt <= 0) {
            errs.amount = 'Amount must be greater than zero.';
        }
        if (!expenseForm.bankAccountId) {
            errs.bankAccountId = 'Bank account is required.';
        }
        if (expenseForm.paymentMode !== 'CASH' && !expenseForm.referenceNumber.trim()) {
            errs.referenceNumber = 'Reference number is required for non-cash modes.';
        }
        if (!expenseForm.paymentDate) {
            errs.paymentDate = 'Payment date is required.';
        } else if (expenseForm.paymentDate > today()) {
            errs.paymentDate = 'Payment date cannot be in the future.';
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
                notes: expenseForm.notes.trim() || null
            };
            await recordExpense(payload);
            setIsExpenseModalOpen(false);
            await fetchData();
        } catch (err) {
            const msg = err?.response?.data?.message || err?.message || 'Failed to record expense.';
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
            setReverseError('A mandatory reason is required for any ledger reversal.');
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
                    title: 'Amendment Queued for Master Admin Approval (202 Accepted)',
                    message: `Transaction #${selectedTx.id} was approved more than 24 hours ago. In accordance with the 24-Hour Admin Edit Lock safeguard, a pending amendment request has been placed in the Master Admin queue.`
                });
            } else {
                setReversalNotification({
                    type: 'success',
                    title: 'Transaction Successfully Reversed',
                    message: `Transaction #${selectedTx.id} has been reversed. Live bank account balances and unit bills have been updated immediately.`
                });
            }
            await fetchData();
        } catch (err) {
            const msg = err?.response?.data?.message || err?.message || 'Failed to reverse transaction.';
            setReverseError(msg);
        } finally {
            setIsSubmittingReverse(false);
        }
    };

    const handleResolveAmendment = async (reqId, action) => {
        try {
            const { resolveAmendmentRequest } = await import('../api/adminApi');
            await resolveAmendmentRequest(reqId, action, `Resolved via Admin Portal`);
            await fetchData();
        } catch (err) {
            alert('Failed to resolve amendment request: ' + (err?.response?.data?.message || err.message));
        }
    };

    const handleOpenReconcile = async () => {
        setIsReconcileModalOpen(true);
        setIsLoadingReconciliation(true);
        try {
            const data = await reconcileBankAccounts();
            setReconciliationList(Array.isArray(data) ? data : []);
        } catch (err) {
            console.error('Failed to reconcile bank accounts:', err);
        } finally {
            setIsLoadingReconciliation(false);
        }
    };

    const pendingQueueCount = amendmentRequests.filter(r => r.status === 'PENDING').length;

    const columns = [
        {
            header: 'Receipt / Ref',
            render: (row) => (
                <div>
                    <div style={{ fontWeight: 600, color: 'var(--text-primary-light)' }}>
                        {row.receiptNumber || `TX-${row.id}`}
                    </div>
                    {row.referenceNumber && (
                        <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary-light)' }}>
                            Ref: {row.referenceNumber}
                        </div>
                    )}
                </div>
            )
        },
        {
            header: 'Category & Direction',
            render: (row) => {
                const isCredit = row.entryDirection === 'CREDIT';
                return (
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                        <span style={{
                            display: 'inline-flex', alignItems: 'center', gap: '0.2rem',
                            padding: '0.2rem 0.5rem', borderRadius: 4, fontSize: '0.75rem', fontWeight: 700,
                            background: isCredit ? 'rgba(16,185,129,0.12)' : 'rgba(239,68,68,0.12)',
                            color: isCredit ? '#10b981' : '#ef4444'
                        }}>
                            {isCredit ? <ArrowDownRight size={12} /> : <ArrowUpRight size={12} />}
                            {row.entryDirection || 'CREDIT'}
                        </span>
                        <span style={{ fontSize: '0.8rem', color: 'var(--text-secondary-light)' }}>
                            {(row.category || 'MAINTENANCE').replace(/_/g, ' ')}
                        </span>
                    </div>
                );
            }
        },
        {
            header: 'Room / Payee',
            render: (row) => (
                <div>
                    {row.roomNumber && row.roomNumber !== 'Unassigned' ? (
                        <span>Room <strong>{row.roomNumber}</strong> ({row.residentName || 'Resident'})</span>
                    ) : (
                        <span>{row.notes ? row.notes.split('.')[0] : 'Society Operations'}</span>
                    )}
                </div>
            )
        },
        {
            header: 'Bank / Cash Account',
            render: (row) => (
                <span style={{ fontSize: '0.85rem', color: 'var(--text-secondary-light)' }}>
                    {row.bankAccountName || 'Main Bank A/c'}
                </span>
            )
        },
        {
            header: 'Amount',
            render: (row) => {
                const isCredit = row.entryDirection === 'CREDIT';
                return (
                    <span style={{
                        fontWeight: 700,
                        color: isCredit ? '#10b981' : '#ef4444',
                        fontFamily: 'monospace', fontSize: '0.95rem'
                    }}>
                        {isCredit ? '+' : '-'}₹{Number(row.amount || 0).toFixed(2)}
                    </span>
                );
            }
        },
        {
            header: 'Date',
            render: (row) => row.paymentDate || (row.timestamp ? String(row.timestamp).split('T')[0] : 'N/A')
        },
        {
            header: 'Status',
            render: (row) => <StatusChip status={row.status} />
        },
        {
            header: '24h Lock',
            render: (row) => {
                const isLocked = row.locked;
                return (
                    <span style={{
                        display: 'inline-flex', alignItems: 'center', gap: '0.3rem',
                        padding: '0.2rem 0.5rem', borderRadius: 12, fontSize: '0.7rem', fontWeight: 600,
                        background: isLocked ? 'rgba(245,158,11,0.1)' : 'rgba(59,130,246,0.1)',
                        color: isLocked ? '#d97706' : '#2563eb'
                    }}>
                        {isLocked ? <Lock size={11} /> : <Unlock size={11} />}
                        {isLocked ? 'Locked (>24h)' : 'Editable (<24h)'}
                    </span>
                );
            }
        },
        {
            header: 'Actions',
            render: (row) => {
                const handleSendEmail = async () => {
                    try {
                        const res = await emailReceipt(row.id);
                        alert(`Receipt emailed successfully to ${res.sentTo}`);
                    } catch (err) {
                        alert(err?.response?.data?.error || err?.response?.data?.message || 'Failed to email receipt.');
                    }
                };

                return (
                    <div style={{ display: 'flex', gap: '0.35rem', alignItems: 'center' }}>
                        <button
                            className="custom-btn secondary"
                            style={{ padding: '0.3rem 0.5rem', fontSize: '0.75rem' }}
                            onClick={() => downloadAuthenticatedFile(`/transactions/${row.id}/receipt-pdf`, `receipt_${row.receiptNumber || row.id}.pdf`)}
                            title="Download Receipt PDF"
                        >
                            <Download size={13} />
                        </button>
                        <button
                            className="custom-btn secondary"
                            style={{ padding: '0.3rem 0.5rem', fontSize: '0.75rem' }}
                            onClick={handleSendEmail}
                            title="Email Receipt to Resident"
                        >
                            <Mail size={13} />
                        </button>
                        {row.status === 'SUCCESS' && (
                            <button
                                className="custom-btn secondary"
                                style={{ padding: '0.3rem 0.5rem', fontSize: '0.75rem', gap: '0.2rem', color: 'var(--danger)' }}
                                onClick={() => handleOpenReverse(row)}
                                title={row.locked ? "Reversal requires Master Admin approval queue (>24h)" : "Immediate reversal (<24h)"}
                            >
                                <RotateCcw size={12} /> Reverse
                            </button>
                        )}
                    </div>
                );
            }
        }
    ];

    return (
        <div>
            <PageHeader
                title="Transaction & Expense Ledger"
                breadcrumbs={['Admin', 'Transactions']}
                actionButton={
                    <div style={{ display: 'flex', gap: '0.5rem' }}>
                        {pendingQueueCount > 0 && (
                            <button
                                className="custom-btn secondary"
                                style={{ background: 'rgba(245,158,11,0.1)', color: '#d97706', border: '1px solid rgba(245,158,11,0.3)', gap: '0.4rem' }}
                                onClick={() => setIsQueueModalOpen(true)}
                            >
                                <ShieldAlert size={16} /> Approvals Queue ({pendingQueueCount})
                            </button>
                        )}
                        <Button onClick={handleOpenExpense} style={{ background: 'var(--primary-color)' }}>
                            <Plus size={16} /> Record Expense / Wages
                        </Button>
                    </div>
                }
            />

            {/* Live Bank Balances Bar */}
            <div style={{ marginBottom: '1.5rem' }}>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
                    <span style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-secondary-light)', display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                        <Building2 size={16} style={{ color: 'var(--primary-color)' }} /> LIVE SOCIETY BALANCES
                    </span>
                    <div style={{ display: 'flex', gap: '0.4rem' }}>
                        <button onClick={handleOpenReconcile} className="custom-btn secondary" style={{ padding: '0.2rem 0.6rem', fontSize: '0.75rem', gap: '0.3rem', color: '#10b981', borderColor: 'rgba(16,185,129,0.4)', background: 'rgba(16,185,129,0.06)' }}>
                            <Scale size={13} /> Reconcile Accounts
                        </button>
                        <button onClick={fetchData} className="custom-btn secondary" style={{ padding: '0.2rem 0.5rem', fontSize: '0.75rem', gap: '0.3rem' }}>
                            <RefreshCw size={12} /> Refresh
                        </button>
                    </div>
                </div>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1rem' }}>
                    {bankAccounts.map((acc) => (
                        <div key={acc.id} style={{
                            background: '#ffffff', border: '1px solid var(--border-light)',
                            borderLeft: acc.accountType === 'CASH_LEDGER' ? '4px solid #f59e0b' : '4px solid #10b981',
                            borderRadius: 12, padding: '0.85rem 1rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center'
                        }}>
                            <div>
                                <div style={{ fontSize: '0.8rem', color: 'var(--text-secondary-light)', fontWeight: 500 }}>
                                    {acc.accountName}
                                </div>
                                <div style={{ fontSize: '1.2rem', fontWeight: 700, color: 'var(--text-primary-light)', marginTop: '0.2rem' }}>
                                    ₹{Number(acc.currentBalance || 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                                </div>
                            </div>
                            <div style={{
                                width: 36, height: 36, borderRadius: '50%',
                                background: acc.accountType === 'CASH_LEDGER' ? 'rgba(245,158,11,0.1)' : 'rgba(16,185,129,0.1)',
                                color: acc.accountType === 'CASH_LEDGER' ? '#f59e0b' : '#10b981',
                                display: 'flex', alignItems: 'center', justifyContent: 'center'
                            }}>
                                {acc.accountType === 'CASH_LEDGER' ? <Banknote size={18} /> : <Wallet size={18} />}
                            </div>
                        </div>
                    ))}
                </div>
            </div>

            {/* Reversal Notification Banner */}
            {reversalNotification && (
                <div style={{
                    padding: '1rem', borderRadius: 8, marginBottom: '1.5rem',
                    background: reversalNotification.type === 'warning' ? 'rgba(245,158,11,0.1)' : 'rgba(16,185,129,0.1)',
                    border: reversalNotification.type === 'warning' ? '1px solid rgba(245,158,11,0.3)' : '1px solid rgba(16,185,129,0.3)',
                    display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start'
                }}>
                    <div>
                        <div style={{ fontWeight: 700, fontSize: '0.9rem', color: reversalNotification.type === 'warning' ? '#d97706' : '#10b981', marginBottom: '0.25rem' }}>
                            {reversalNotification.title}
                        </div>
                        <div style={{ fontSize: '0.85rem', color: 'var(--text-primary-light)' }}>
                            {reversalNotification.message}
                        </div>
                    </div>
                    <button onClick={() => setReversalNotification(null)} style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--text-secondary-light)' }}>
                        <X size={16} />
                    </button>
                </div>
            )}

            <DataTable
                columns={columns}
                data={transactions}
                isLoading={isLoading}
            />

            {/* Record Expense Modal */}
            {isExpenseModalOpen && (
                <div className="premium-modal-overlay">
                    <div className="premium-modal-content" style={{ maxWidth: '520px' }}>
                        <div className="premium-modal-header">
                            <div>
                                <h2 className="premium-modal-title" style={{ fontSize: '1.2rem' }}>Record Society Expense / Wages</h2>
                                <p className="premium-modal-subtitle">Automatically debits the selected Society Bank / Cash account.</p>
                            </div>
                            <button className="premium-modal-close" onClick={() => setIsExpenseModalOpen(false)}>
                                <X size={20} />
                            </button>
                        </div>

                        <form onSubmit={handleRecordExpense} style={{ padding: '1.5rem' }}>
                            <div className="input-group-modern">
                                <label className="input-label">Expense Category <span style={{ color: 'var(--danger)' }}>*</span></label>
                                <select
                                    className="modern-input"
                                    style={{ paddingLeft: '1rem' }}
                                    value={expenseForm.category}
                                    onChange={e => setExpenseForm({ ...expenseForm, category: e.target.value })}
                                >
                                    {EXPENSE_CATEGORIES.map(c => (
                                        <option key={c.value} value={c.value}>{c.label}</option>
                                    ))}
                                </select>
                            </div>

                            <div className="input-group-modern">
                                <label className="input-label">Pay From Bank / Cash Account <span style={{ color: 'var(--danger)' }}>*</span></label>
                                <select
                                    className="modern-input"
                                    style={{ paddingLeft: '1rem' }}
                                    value={expenseForm.bankAccountId}
                                    onChange={e => setExpenseForm({ ...expenseForm, bankAccountId: e.target.value })}
                                >
                                    {bankAccounts.map(b => (
                                        <option key={b.id} value={b.id}>
                                            {b.accountName} (Balance: ₹{Number(b.currentBalance).toFixed(2)})
                                        </option>
                                    ))}
                                </select>
                                {expenseErrors.bankAccountId && <p style={{ color: 'var(--danger)', fontSize: '0.8rem' }}>{expenseErrors.bankAccountId}</p>}
                            </div>

                            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                                <div className="input-group-modern">
                                    <label className="input-label">Amount (₹) <span style={{ color: 'var(--danger)' }}>*</span></label>
                                    <input
                                        type="number"
                                        step="0.01"
                                        min="0.01"
                                        className="modern-input"
                                        style={{ paddingLeft: '1rem' }}
                                        placeholder="0.00"
                                        value={expenseForm.amount}
                                        onChange={e => setExpenseForm({ ...expenseForm, amount: e.target.value })}
                                    />
                                    {expenseErrors.amount && <p style={{ color: 'var(--danger)', fontSize: '0.8rem' }}>{expenseErrors.amount}</p>}
                                </div>
                                <div className="input-group-modern">
                                    <label className="input-label">Payment Date <span style={{ color: 'var(--danger)' }}>*</span></label>
                                    <input
                                        type="date"
                                        max={today()}
                                        className="modern-input"
                                        style={{ paddingLeft: '1rem' }}
                                        value={expenseForm.paymentDate}
                                        onChange={e => setExpenseForm({ ...expenseForm, paymentDate: e.target.value })}
                                    />
                                    {expenseErrors.paymentDate && <p style={{ color: 'var(--danger)', fontSize: '0.8rem' }}>{expenseErrors.paymentDate}</p>}
                                </div>
                            </div>

                            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                                <div className="input-group-modern">
                                    <label className="input-label">Payment Mode <span style={{ color: 'var(--danger)' }}>*</span></label>
                                    <select
                                        className="modern-input"
                                        style={{ paddingLeft: '1rem' }}
                                        value={expenseForm.paymentMode}
                                        onChange={e => setExpenseForm({ ...expenseForm, paymentMode: e.target.value })}
                                    >
                                        {PAYMENT_MODES.map(m => (
                                            <option key={m.value} value={m.value}>{m.label}</option>
                                        ))}
                                    </select>
                                </div>
                                <div className="input-group-modern">
                                    <label className="input-label">
                                        Reference No. {expenseForm.paymentMode !== 'CASH' && <span style={{ color: 'var(--danger)' }}>*</span>}
                                    </label>
                                    <input
                                        type="text"
                                        className="modern-input"
                                        style={{ paddingLeft: '1rem' }}
                                        placeholder={expenseForm.paymentMode === 'CASH' ? 'Optional' : 'NEFT / Cheque / UTR No.'}
                                        value={expenseForm.referenceNumber}
                                        onChange={e => setExpenseForm({ ...expenseForm, referenceNumber: e.target.value })}
                                    />
                                    {expenseErrors.referenceNumber && <p style={{ color: 'var(--danger)', fontSize: '0.8rem' }}>{expenseErrors.referenceNumber}</p>}
                                </div>
                            </div>

                            <div className="input-group-modern">
                                <label className="input-label">Payee Name / Contractor</label>
                                <input
                                    type="text"
                                    className="modern-input"
                                    style={{ paddingLeft: '1rem' }}
                                    placeholder="e.g. Ramesh Guard, MSEB Electricity"
                                    value={expenseForm.payeeName}
                                    onChange={e => setExpenseForm({ ...expenseForm, payeeName: e.target.value })}
                                />
                            </div>

                            <div className="input-group-modern" style={{ marginBottom: 0 }}>
                                <label className="input-label">Notes / Description</label>
                                <textarea
                                    className="modern-input"
                                    style={{ paddingLeft: '1rem', minHeight: '60px' }}
                                    placeholder="Purpose of expense"
                                    value={expenseForm.notes}
                                    onChange={e => setExpenseForm({ ...expenseForm, notes: e.target.value })}
                                />
                            </div>

                            {expenseErrors.submit && (
                                <div style={{ color: 'var(--danger)', fontSize: '0.85rem', marginTop: '1rem', display: 'flex', gap: '0.4rem', alignItems: 'center' }}>
                                    <AlertCircle size={14} /> {expenseErrors.submit}
                                </div>
                            )}

                            <div className="premium-actions">
                                <button type="button" className="custom-btn secondary" onClick={() => setIsExpenseModalOpen(false)}>
                                    Cancel
                                </button>
                                <button type="submit" className="custom-btn primary" disabled={isSubmittingExpense}>
                                    {isSubmittingExpense ? 'Processing...' : 'Record Expense'}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}

            {/* Reverse Transaction Modal */}
            {isReverseModalOpen && selectedTx && (
                <div className="premium-modal-overlay">
                    <div className="premium-modal-content" style={{ maxWidth: '480px' }}>
                        <div className="premium-modal-header">
                            <div>
                                <h2 className="premium-modal-title" style={{ fontSize: '1.2rem' }}>Reverse Transaction #{selectedTx.id}</h2>
                                <p className="premium-modal-subtitle">
                                    Amount: <strong>₹{Number(selectedTx.amount).toFixed(2)}</strong> ({selectedTx.entryDirection})
                                </p>
                            </div>
                            <button className="premium-modal-close" onClick={() => setIsReverseModalOpen(false)}>
                                <X size={20} />
                            </button>
                        </div>

                        <form onSubmit={handleConfirmReverse} style={{ padding: '1.5rem' }}>
                            {selectedTx.locked ? (
                                <div style={{
                                    padding: '0.85rem', borderRadius: 8, background: 'rgba(245,158,11,0.1)',
                                    border: '1px solid rgba(245,158,11,0.3)', marginBottom: '1rem', fontSize: '0.85rem', color: '#b45309'
                                }}>
                                    <strong style={{ display: 'flex', alignItems: 'center', gap: '0.3rem', marginBottom: '0.25rem' }}>
                                        <Lock size={14} /> 24-Hour Admin Edit Lock Active
                                    </strong>
                                    This entry was approved over 24 hours ago. As an Admin, submitting this reversal will <strong>not</strong> directly mutate balances; instead, it will place an amendment request into the Master Admin approval queue.
                                </div>
                            ) : (
                                <div style={{
                                    padding: '0.85rem', borderRadius: 8, background: 'rgba(59,130,246,0.1)',
                                    border: '1px solid rgba(59,130,246,0.3)', marginBottom: '1rem', fontSize: '0.85rem', color: '#1d4ed8'
                                }}>
                                    <strong style={{ display: 'flex', alignItems: 'center', gap: '0.3rem', marginBottom: '0.25rem' }}>
                                        <Unlock size={14} /> Within 24-Hour Edit Window
                                    </strong>
                                    Reversing this transaction will immediately restore the live bank account balance and recalculate associated unit dues.
                                </div>
                            )}

                            <div className="input-group-modern" style={{ marginBottom: 0 }}>
                                <label className="input-label">Mandatory Reversal Reason <span style={{ color: 'var(--danger)' }}>*</span></label>
                                <textarea
                                    className="modern-input"
                                    style={{ paddingLeft: '1rem', minHeight: '80px' }}
                                    placeholder="Explain why this entry is being reversed (e.g. resident transferred from incorrect room, wrong account credited)..."
                                    value={reverseReason}
                                    onChange={e => setReverseReason(e.target.value)}
                                    required
                                />
                            </div>

                            {reverseError && (
                                <div style={{ color: 'var(--danger)', fontSize: '0.85rem', marginTop: '1rem', display: 'flex', gap: '0.4rem', alignItems: 'center' }}>
                                    <AlertCircle size={14} /> {reverseError}
                                </div>
                            )}

                            <div className="premium-actions">
                                <button type="button" className="custom-btn secondary" onClick={() => setIsReverseModalOpen(false)}>
                                    Cancel
                                </button>
                                <button type="submit" className="custom-btn primary" style={{ background: 'var(--danger)', border: 'none' }} disabled={isSubmittingReverse}>
                                    {isSubmittingReverse ? 'Processing...' : (selectedTx.locked ? 'Queue for Approval' : 'Confirm Reversal')}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}

            {/* Master Admin Amendment Queue Modal */}
            {isQueueModalOpen && (
                <div className="premium-modal-overlay">
                    <div className="premium-modal-content" style={{ maxWidth: '650px' }}>
                        <div className="premium-modal-header">
                            <div>
                                <h2 className="premium-modal-title" style={{ fontSize: '1.2rem' }}>Master Admin Approval Queue</h2>
                                <p className="premium-modal-subtitle">Review ledger amendment requests submitted after the 24-hour edit lock.</p>
                            </div>
                            <button className="premium-modal-close" onClick={() => setIsQueueModalOpen(false)}>
                                <X size={20} />
                            </button>
                        </div>

                        <div style={{ padding: '1.5rem', maxHeight: '450px', overflowY: 'auto' }}>
                            {amendmentRequests.length === 0 ? (
                                <p style={{ textAlign: 'center', color: 'var(--text-secondary-light)', margin: '2rem 0' }}>
                                    No pending amendment requests found.
                                </p>
                            ) : (
                                amendmentRequests.map(req => (
                                    <div key={req.id} style={{
                                        border: '1px solid var(--border-light)', borderRadius: 10,
                                        padding: '1rem', marginBottom: '1rem', background: '#f8fafc'
                                    }}>
                                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                                            <span style={{ fontWeight: 700, fontSize: '0.9rem' }}>
                                                Request #{req.id} · Transaction #{req.transactionId}
                                            </span>
                                            <StatusChip status={req.status} />
                                        </div>
                                        <div style={{ fontSize: '0.85rem', color: 'var(--text-primary-light)', marginBottom: '0.4rem' }}>
                                            <strong>Requested By:</strong> {req.requestedByAdminName || 'Admin'}
                                        </div>
                                        <div style={{ fontSize: '0.85rem', color: 'var(--text-secondary-light)', marginBottom: '0.75rem' }}>
                                            <strong>Reason:</strong> {req.reason}
                                        </div>
                                        {req.status === 'PENDING' && (
                                            <div style={{ display: 'flex', gap: '0.5rem', justifyContent: 'flex-end' }}>
                                                <button
                                                    className="custom-btn secondary"
                                                    style={{ padding: '0.3rem 0.7rem', fontSize: '0.8rem', color: 'var(--danger)' }}
                                                    onClick={() => handleResolveAmendment(req.id, 'REJECT')}
                                                >
                                                    <XCircle size={14} /> Reject
                                                </button>
                                                <button
                                                    className="custom-btn primary"
                                                    style={{ padding: '0.3rem 0.7rem', fontSize: '0.8rem', background: '#10b981', border: 'none' }}
                                                    onClick={() => handleResolveAmendment(req.id, 'APPROVE')}
                                                >
                                                    <CheckCircle2 size={14} /> Approve & Apply
                                                </button>
                                            </div>
                                        )}
                                    </div>
                                ))
                            )}
                        </div>

                        <div className="premium-actions" style={{ padding: '1rem 1.5rem', borderTop: '1px solid var(--border-light)' }}>
                            <button type="button" className="custom-btn secondary" onClick={() => setIsQueueModalOpen(false)}>
                                Close
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* Bank Accounts Reconciliation Modal */}
            {isReconcileModalOpen && (
                <div className="premium-modal-backdrop animate-fade-in">
                    <div className="premium-modal-card animate-scale-up" style={{ maxWidth: 720 }}>
                        <div className="premium-modal-header" style={{ borderBottom: '1px solid var(--border-light)', padding: '1.25rem 1.5rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
                                <div style={{ width: 36, height: 36, borderRadius: 8, background: 'rgba(16,185,129,0.1)', color: '#10b981', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                                    <Scale size={20} />
                                </div>
                                <div>
                                    <h3 style={{ margin: 0, fontSize: '1.1rem', fontWeight: 700 }}>Bank Account Reconciliation</h3>
                                    <p style={{ margin: 0, fontSize: '0.8rem', color: 'var(--text-secondary-light)' }}>
                                        Formula: Opening Balance + Total Credits (Incomes) − Total Debits (Expenses)
                                    </p>
                                </div>
                            </div>
                            <button className="icon-close-btn" onClick={() => setIsReconcileModalOpen(false)}>
                                <X size={18} />
                            </button>
                        </div>

                        <div className="premium-modal-body" style={{ padding: '1.5rem', maxHeight: '65vh', overflowY: 'auto' }}>
                            {isLoadingReconciliation ? (
                                <div style={{ textAlign: 'center', padding: '2rem' }}>Reconciling accounts with ledger transactions...</div>
                            ) : reconciliationList.length === 0 ? (
                                <div style={{ textAlign: 'center', padding: '2rem', color: 'var(--text-secondary-light)' }}>No active bank accounts found.</div>
                            ) : (
                                reconciliationList.map(rec => (
                                    <div key={rec.bankAccountId} style={{
                                        border: '1px solid var(--border-light)', borderRadius: 10, padding: '1.2rem',
                                        marginBottom: '1rem', background: '#ffffff',
                                        borderLeft: rec.reconciled ? '4px solid #10b981' : '4px solid #ef4444'
                                    }}>
                                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.8rem' }}>
                                            <div>
                                                <h4 style={{ margin: 0, fontSize: '1rem', fontWeight: 700 }}>{rec.accountName}</h4>
                                                <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary-light)' }}>
                                                    {rec.accountType} • {rec.transactionCount} transactions analyzed
                                                </span>
                                            </div>
                                            <span style={{
                                                fontSize: '0.75rem', fontWeight: 700, padding: '0.25rem 0.6rem', borderRadius: 20,
                                                background: rec.reconciled ? 'rgba(16,185,129,0.12)' : 'rgba(239,68,68,0.12)',
                                                color: rec.reconciled ? '#10b981' : '#ef4444'
                                            }}>
                                                {rec.reconciled ? '✓ 100% Reconciled (₹0 Discrepancy)' : `⚠ Discrepancy: ₹${Number(rec.discrepancy || 0).toFixed(2)}`}
                                            </span>
                                        </div>

                                        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(130px, 1fr))', gap: '0.75rem', background: 'var(--bg-light)', padding: '0.85rem', borderRadius: 8 }}>
                                            <div>
                                                <div style={{ fontSize: '0.7rem', color: 'var(--text-secondary-light)' }}>Opening Balance</div>
                                                <div style={{ fontWeight: 600, fontSize: '0.9rem' }}>₹{Number(rec.openingBalance || 0).toLocaleString(undefined, { minimumFractionDigits: 2 })}</div>
                                            </div>
                                            <div>
                                                <div style={{ fontSize: '0.7rem', color: '#10b981' }}>+ Total Credits</div>
                                                <div style={{ fontWeight: 600, fontSize: '0.9rem', color: '#10b981' }}>₹{Number(rec.totalCredits || 0).toLocaleString(undefined, { minimumFractionDigits: 2 })}</div>
                                            </div>
                                            <div>
                                                <div style={{ fontSize: '0.7rem', color: '#ef4444' }}>− Total Debits</div>
                                                <div style={{ fontWeight: 600, fontSize: '0.9rem', color: '#ef4444' }}>₹{Number(rec.totalDebits || 0).toLocaleString(undefined, { minimumFractionDigits: 2 })}</div>
                                            </div>
                                            <div>
                                                <div style={{ fontSize: '0.7rem', color: 'var(--text-secondary-light)' }}>Calculated Balance</div>
                                                <div style={{ fontWeight: 700, fontSize: '0.95rem', color: '#2563eb' }}>₹{Number(rec.calculatedBalance || 0).toLocaleString(undefined, { minimumFractionDigits: 2 })}</div>
                                            </div>
                                            <div>
                                                <div style={{ fontSize: '0.7rem', color: 'var(--text-secondary-light)' }}>Live Running Balance</div>
                                                <div style={{ fontWeight: 700, fontSize: '0.95rem', color: '#10b981' }}>₹{Number(rec.currentBalance || 0).toLocaleString(undefined, { minimumFractionDigits: 2 })}</div>
                                            </div>
                                        </div>
                                    </div>
                                ))
                            )}
                        </div>

                        <div className="premium-actions" style={{ padding: '1rem 1.5rem', borderTop: '1px solid var(--border-light)' }}>
                            <button type="button" className="custom-btn secondary" onClick={() => setIsReconcileModalOpen(false)}>
                                Close
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default AdminTransactions;
