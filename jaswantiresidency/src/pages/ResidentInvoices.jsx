import React, { useState, useEffect, useCallback } from 'react';
import PageHeader from '../components/ui/PageHeader';
import DataTable from '../components/ui/DataTable';
import StatusChip from '../components/ui/StatusChip';
import Button from '../components/ui/Button';
import PaymentModal from '../components/PaymentModal';
import { Download, CreditCard, UploadCloud, CheckCircle2, Clock, AlertCircle } from 'lucide-react';
import { getMyBills, getMyPaymentSubmissions } from '../api/billingApi';
import { downloadAuthenticatedFile } from '../api/fileUtils';

const ResidentInvoices = () => {
    const [activeTab, setActiveTab] = useState('invoices'); // 'invoices' | 'submissions'
    const [invoices, setInvoices] = useState([]);
    const [submissions, setSubmissions] = useState([]);
    const [isLoading, setIsLoading] = useState(true);
    const [isPaymentModalOpen, setIsPaymentModalOpen] = useState(false);
    const [selectedBill, setSelectedBill] = useState(null);

    const loadData = useCallback(async () => {
        setIsLoading(true);
        try {
            const [billsData, subsData] = await Promise.all([
                getMyBills().catch(() => []),
                getMyPaymentSubmissions().catch(() => [])
            ]);
            const sortedBills = (billsData || []).sort((a, b) => new Date(b.dueDate) - new Date(a.dueDate));
            const sortedSubs = (subsData || []).sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
            setInvoices(sortedBills);
            setSubmissions(sortedSubs);
        } catch (err) {
            console.error("Failed to fetch resident invoices or submissions", err);
        } finally {
            setIsLoading(false);
        }
    }, []);

    useEffect(() => {
        loadData();
    }, [loadData]);

    const handlePayClick = (bill) => {
        setSelectedBill(bill);
        setIsPaymentModalOpen(true);
    };

    const handleOpenSubmitProof = () => {
        setSelectedBill(null);
        setIsPaymentModalOpen(true);
    };

    const invoiceColumns = [
        { header: 'Invoice ID', accessor: 'id' },
        { header: 'Billing Month', accessor: 'billingMonth' },
        { header: 'Charge Type', accessor: 'chargeType' },
        { header: 'Amount', render: (row) => `₹${Number(row.amount).toFixed(2)}` },
        {
            header: 'Paid / Due',
            render: (row) => {
                const total = (parseFloat(row.amount) || 0) + (parseFloat(row.lateFee) || 0);
                const paid = parseFloat(row.paidAmount) || 0;
                const outstanding = Math.max(0, total - paid);
                return (
                    <span style={{ fontSize: '0.85rem' }}>
                        {paid > 0 && <span style={{ color: 'var(--success)' }}>Paid: ₹{paid.toFixed(2)} </span>}
                        {outstanding > 0 ? (
                            <span style={{ color: 'var(--danger)', fontWeight: 600 }}>Due: ₹{outstanding.toFixed(2)}</span>
                        ) : (
                            <span style={{ color: 'var(--success)', fontWeight: 600 }}>Fully Cleared</span>
                        )}
                    </span>
                );
            }
        },
        { header: 'Due Date', accessor: 'dueDate' },
        { header: 'Status', render: (row) => <StatusChip status={row.status} /> },
        {
            header: 'Actions',
            render: (row) => (
                <div style={{ display: 'flex', gap: '0.5rem' }}>
                    <Button
                        variant="secondary"
                        title="Download PDF Invoice"
                        style={{ padding: '0.4rem 0.6rem' }}
                        onClick={() => downloadAuthenticatedFile(`/bills/${row.id}/invoice-pdf`, `invoice_${row.billingMonth || row.id}.pdf`)}
                    >
                        <Download size={16} />
                    </Button>
                    {['PENDING', 'PARTIALLY_PAID'].includes(row.status) && (
                        <Button onClick={() => handlePayClick(row)} style={{ padding: '0.4rem 0.8rem' }}>
                            <CreditCard size={16} /> Pay / Submit Proof
                        </Button>
                    )}
                </div>
            )
        }
    ];

    const submissionColumns = [
        { header: 'Sub #', accessor: 'id' },
        { header: 'Room', accessor: 'roomNumber' },
        {
            header: 'Submitted At',
            render: (row) => new Date(row.createdAt).toLocaleDateString() + ' ' + new Date(row.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
        },
        { header: 'Amount', render: (row) => <strong>₹{Number(row.amount).toFixed(2)}</strong> },
        {
            header: 'Payment Info',
            render: (row) => (
                <div style={{ fontSize: '0.8rem' }}>
                    <div>Mode: {row.paymentMode}</div>
                    {row.referenceNumber && <div>Ref: <code>{row.referenceNumber}</code></div>}
                    {row.advanceMonthsCount > 0 && <span style={{ color: 'var(--primary)', fontWeight: 600 }}>+{row.advanceMonthsCount} months advance</span>}
                </div>
            )
        },
        {
            header: 'Status',
            render: (row) => <StatusChip status={row.status} />
        },
        {
            header: 'Details & Remarks',
            render: (row) => (
                <div style={{ fontSize: '0.8rem' }}>
                    {row.status === 'APPROVED' && (
                        <div style={{ color: 'var(--success)' }}>
                            <div>Receipt: <strong>{row.receiptNumber || 'N/A'}</strong></div>
                            <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary-light)' }}>
                                Approved by {row.approvedByUserName || 'Admin'}
                            </div>
                        </div>
                    )}
                    {row.status === 'REJECTED' && (
                        <div style={{ color: 'var(--danger)' }}>
                            <strong>Rejected:</strong> {row.rejectionReason || 'No reason provided'}
                        </div>
                    )}
                    {row.status === 'PENDING' && (
                        <span style={{ color: 'var(--warning)' }}>Awaiting society admin approval</span>
                    )}
                </div>
            )
        }
    ];

    const pendingSubmissionsCount = submissions.filter(s => s.status === 'PENDING').length;

    return (
        <div>
            <PageHeader
                title="My Invoices & Payments"
                breadcrumbs={['Resident', 'Invoices']}
                actionButton={
                    <Button onClick={handleOpenSubmitProof}>
                        <UploadCloud size={18} /> Submit Payment Proof
                    </Button>
                }
            />

            {/* Tab Navigation */}
            <div style={{ display: 'flex', gap: '1rem', borderBottom: '1px solid var(--border-light)', marginBottom: '1.5rem' }}>
                <button
                    onClick={() => setActiveTab('invoices')}
                    style={{
                        padding: '0.6rem 1rem',
                        border: 'none',
                        background: 'none',
                        borderBottom: activeTab === 'invoices' ? '2px solid var(--primary)' : '2px solid transparent',
                        color: activeTab === 'invoices' ? 'var(--primary)' : 'var(--text-secondary-light)',
                        fontWeight: activeTab === 'invoices' ? 600 : 500,
                        cursor: 'pointer',
                        fontSize: '0.95rem'
                    }}
                >
                    Invoices & Bills ({invoices.length})
                </button>
                <button
                    onClick={() => setActiveTab('submissions')}
                    style={{
                        padding: '0.6rem 1rem',
                        border: 'none',
                        background: 'none',
                        borderBottom: activeTab === 'submissions' ? '2px solid var(--primary)' : '2px solid transparent',
                        color: activeTab === 'submissions' ? 'var(--primary)' : 'var(--text-secondary-light)',
                        fontWeight: activeTab === 'submissions' ? 600 : 500,
                        cursor: 'pointer',
                        fontSize: '0.95rem',
                        display: 'flex',
                        alignItems: 'center',
                        gap: '0.4rem'
                    }}
                >
                    Submitted Payment Proofs ({submissions.length})
                    {pendingSubmissionsCount > 0 && (
                        <span style={{
                            background: '#fef3c7',
                            color: '#92400e',
                            borderRadius: '10px',
                            padding: '0.1rem 0.5rem',
                            fontSize: '0.75rem',
                            fontWeight: 700
                        }}>
                            {pendingSubmissionsCount} Pending
                        </span>
                    )}
                </button>
            </div>

            {activeTab === 'invoices' ? (
                <DataTable
                    columns={invoiceColumns}
                    data={invoices}
                    isLoading={isLoading}
                    emptyMessage="No invoices found."
                />
            ) : (
                <DataTable
                    columns={submissionColumns}
                    data={submissions}
                    isLoading={isLoading}
                    emptyMessage="No payment submissions found."
                />
            )}

            {isPaymentModalOpen && (
                <PaymentModal
                    amount={selectedBill?.amount}
                    bills={selectedBill ? [selectedBill] : []}
                    onClose={() => {
                        setIsPaymentModalOpen(false);
                        setSelectedBill(null);
                    }}
                    onSuccess={() => {
                        loadData();
                    }}
                />
            )}
        </div>
    );
};

export default ResidentInvoices;
