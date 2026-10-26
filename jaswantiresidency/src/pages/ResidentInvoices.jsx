import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { CreditCard, Download, Receipt, Send, UploadCloud } from 'lucide-react';
import PageHeader from '../components/ui/PageHeader';
import DataTable from '../components/ui/DataTable';
import StatusChip from '../components/ui/StatusChip';
import Button from '../components/ui/Button';
import { Tabs } from '../components/ui/Feedback';
import PaymentModal from '../components/PaymentModal';
import { getMyBills, getMyPaymentSubmissions } from '../api/billingApi';
import { downloadAuthenticatedFile } from '../api/fileUtils';
import { notifyError } from '../utils/notify';
import { formatDate, formatDateTime, formatMoney, monthLabel } from '../utils/format';

const UNPAID = ['PENDING', 'PARTIALLY_PAID'];
const outstandingOf = (row) => Math.max(0, (parseFloat(row.amount) || 0) + (parseFloat(row.lateFee) || 0) - (parseFloat(row.paidAmount) || 0));
const isOverdue = (row) => UNPAID.includes(row.status) && row.dueDate && new Date(row.dueDate) < new Date(new Date().toDateString());

const ResidentInvoices = () => {
    const [activeTab, setActiveTab] = useState('invoices');
    const [invoices, setInvoices] = useState([]);
    const [submissions, setSubmissions] = useState([]);
    const [isLoading, setIsLoading] = useState(true);
    const [payment, setPayment] = useState(null); // { amount, bills }

    const loadData = useCallback(async () => {
        setIsLoading(true);
        try {
            const [billsData, subsData] = await Promise.all([getMyBills(), getMyPaymentSubmissions().catch(() => [])]);
            setInvoices((billsData || []).sort((a, b) => new Date(b.dueDate) - new Date(a.dueDate)));
            setSubmissions((subsData || []).sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt)));
        } catch (err) {
            notifyError('Could not load your bills. Please refresh the page.');
        } finally {
            setIsLoading(false);
        }
    }, []);

    useEffect(() => { loadData(); }, [loadData]);

    const unpaid = useMemo(() => invoices.filter((b) => UNPAID.includes(b.status) && outstandingOf(b) > 0), [invoices]);
    const totalDue = unpaid.reduce((sum, b) => sum + outstandingOf(b), 0);
    const pendingSubmissions = submissions.filter((s) => s.status === 'PENDING').length;

    const payBills = (bills) => setPayment({ amount: Number(bills.reduce((s, b) => s + outstandingOf(b), 0).toFixed(2)), bills });

    const invoiceColumns = [
        {
            header: 'Bill',
            accessor: 'billingMonth',
            searchValue: (row) => monthLabel(row.billingMonth),
            render: (row) => (
                <div>
                    <div className="cell-primary">{monthLabel(row.billingMonth) || row.billingMonth}</div>
                    <div className="cell-sub">{row.chargeType === 'NOC' ? 'NOC charge' : 'Maintenance'} · #{row.id}</div>
                </div>
            ),
        },
        { header: 'Due', render: (row) => <span className={isOverdue(row) ? 'text-danger fw-600 nowrap' : 'text-muted nowrap'}>{formatDate(row.dueDate)}</span> },
        { header: 'Status', render: (row) => (isOverdue(row) ? <StatusChip tone="danger" label="Overdue" /> : <StatusChip status={row.status} />) },
        {
            header: 'Amount',
            align: 'right',
            render: (row) => {
                const total = (parseFloat(row.amount) || 0) + (parseFloat(row.lateFee) || 0);
                const due = outstandingOf(row);
                return (
                    <div>
                        <div className="fw-600">{formatMoney(total, 2)}</div>
                        <div className="cell-sub">{due > 0 && due < total ? `${formatMoney(due, 2)} left to pay` : parseFloat(row.lateFee) > 0 ? `incl. ${formatMoney(row.lateFee)} late fee` : ''}</div>
                    </div>
                );
            },
        },
        {
            header: <span className="visually-hidden">Actions</span>,
            render: (row) => (
                <div className="row-actions">
                    {UNPAID.includes(row.status) && outstandingOf(row) > 0 && <Button size="sm" icon={CreditCard} onClick={() => payBills([row])}>Pay</Button>}
                    <Button variant="ghost" size="sm" iconOnly icon={Download} title="Download bill" aria-label={`Download bill ${row.id}`}
                        onClick={() => downloadAuthenticatedFile(`/bills/${row.id}/invoice-pdf`, `invoice_${row.billingMonth || row.id}.pdf`)} />
                </div>
            ),
        },
    ];

    const modeLabel = (m) => ({ UPI_MANUAL: 'UPI', BANK_TRANSFER: 'Bank transfer', CASH: 'Cash', CHEQUE: 'Cheque' }[m] || m);

    const submissionColumns = [
        {
            header: 'Sent',
            render: (row) => (
                <div>
                    <div className="cell-primary">{formatDateTime(row.createdAt)}</div>
                    <div className="cell-sub">{modeLabel(row.paymentMode)}{row.referenceNumber ? ` · ${row.referenceNumber}` : ''}</div>
                </div>
            ),
        },
        {
            header: 'Status',
            render: (row) => <StatusChip status={row.status} label={row.status === 'PENDING' ? 'Being checked' : undefined} />,
        },
        {
            header: 'Update from the office',
            render: (row) => (
                <div className="text-sm" style={{ maxWidth: 360 }}>
                    {row.status === 'APPROVED' && <span>Approved{row.approvedByUserName ? ` by ${row.approvedByUserName}` : ''}{row.receiptNumber ? ` · receipt ${row.receiptNumber}` : ''}</span>}
                    {row.status === 'REJECTED' && <span className="text-danger">{row.rejectionReason || 'Not accepted. Please contact the office.'}</span>}
                    {row.status === 'PENDING' && <span className="text-muted">The office will check it shortly.</span>}
                    {row.advanceMonthsCount > 0 && <div className="cell-sub">Includes {row.advanceMonthsCount} months in advance</div>}
                </div>
            ),
        },
        { header: 'Amount', align: 'right', render: (row) => <span className="fw-600">{formatMoney(row.amount, 2)}</span> },
    ];

    return (
        <div>
            <PageHeader
                title="Bills & payments"
                description="Every bill for your home, and the payments you have sent to the society office."
                actions={<Button variant="secondary" icon={UploadCloud} onClick={() => setPayment({ amount: undefined, bills: [] })}>Report a payment</Button>}
            />

            {!isLoading && (
                <div className={`due-banner ${totalDue > 0 ? '' : 'is-clear'}`}>
                    {totalDue > 0 ? (
                        <>
                            <div className="grow">
                                <div className="text-sm text-muted">You owe</div>
                                <div className="due-amount num">{formatMoney(totalDue, 2)}</div>
                                <div className="text-xs text-subtle">{unpaid.length} unpaid bill{unpaid.length === 1 ? '' : 's'}{pendingSubmissions ? ` · ${pendingSubmissions} payment${pendingSubmissions === 1 ? '' : 's'} being checked` : ''}</div>
                            </div>
                            <Button size="lg" icon={CreditCard} onClick={() => payBills(unpaid)}>Pay all</Button>
                        </>
                    ) : (
                        <div className="grow">
                            <div className="fw-600 text-success">You’re all paid up</div>
                            <div className="text-sm text-muted">{pendingSubmissions ? `${pendingSubmissions} payment${pendingSubmissions === 1 ? ' is' : 's are'} being checked by the office.` : 'Nothing is due right now.'}</div>
                        </div>
                    )}
                </div>
            )}

            <Tabs
                value={activeTab}
                onChange={setActiveTab}
                label="Bills or payments"
                tabs={[
                    { value: 'invoices', label: 'Bills', icon: Receipt, count: invoices.length },
                    { value: 'submissions', label: 'Payments sent', icon: Send, count: submissions.length },
                ]}
            />

            {activeTab === 'invoices' ? (
                <DataTable columns={invoiceColumns} data={invoices} isLoading={isLoading} searchPlaceholder="Search by month"
                    emptyIcon={Receipt} emptyTitle="No bills yet" emptyMessage="Your maintenance bills will appear here each month." />
            ) : (
                <DataTable columns={submissionColumns} data={submissions} isLoading={isLoading} searchable={false}
                    emptyIcon={Send} emptyTitle="No payments sent yet" emptyMessage="When you pay and report it, you can follow its approval here." />
            )}

            {payment && (
                <PaymentModal
                    amount={payment.amount}
                    bills={payment.bills}
                    onClose={() => setPayment(null)}
                    onSuccess={loadData}
                />
            )}
        </div>
    );
};

export default ResidentInvoices;
