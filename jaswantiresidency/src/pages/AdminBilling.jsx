import React, { useState, useEffect, useCallback, useMemo, useRef } from 'react';
import { notifySuccess, notifyError, errorMessage } from '../utils/notify';
import PageHeader from '../components/ui/PageHeader';
import DataTable from '../components/ui/DataTable';
import StatusChip from '../components/ui/StatusChip';
import Button from '../components/ui/Button';
import Menu from '../components/ui/Menu';
import { Select } from '../components/ui/Field';
import { Segmented, StatCard } from '../components/ui/Feedback';
import { AlertTriangle, Banknote, ClipboardCheck, Download, FileSpreadsheet, FileText, IndianRupee, Plus, Receipt, Upload, Wallet } from 'lucide-react';
import { getPaymentSubmissions } from '../api/billingApi';
import { formatDate, formatMoney, monthLabel } from '../utils/format';
import { isOpen, isOverdue, isPaid, isUnpaid } from '../utils/billing';
import GenerateBillsDialog from '../components/GenerateBillsDialog';
import RecordPaymentModal from '../components/RecordPaymentModal';
import PendingPaymentApprovalsModal from '../components/PendingPaymentApprovalsModal';
import { getBills, exportBillsExcel, downloadImportTemplate, importBillsExcel } from '../api/adminApi';
import { downloadAuthenticatedFile } from '../api/fileUtils';

const AdminBilling = () => {
    const [bills, setBills] = useState([]);
    const [isLoading, setIsLoading] = useState(true);
    const [selectedFY, setSelectedFY] = useState('');
    const [isGenerateModalOpen, setIsGenerateModalOpen] = useState(false);
    const [isPaymentModalOpen, setIsPaymentModalOpen] = useState(false);
    const [isApprovalsModalOpen, setIsApprovalsModalOpen] = useState(false);
    const [selectedBill, setSelectedBill] = useState(null);
    const [isImporting, setIsImporting] = useState(false);
    const [statusFilter, setStatusFilter] = useState('all');
    const [pendingSubmissions, setPendingSubmissions] = useState(0);

    const fileInputRef = useRef(null);

    const fetchBills = useCallback(async () => {
        setIsLoading(true);
        try {
            const data = await getBills(selectedFY || undefined);
            setBills(data || []);
        } catch (err) {
            notifyError('Could not load bills. Please refresh the page.');
            setBills([]);
        } finally {
            setIsLoading(false);
        }
    }, [selectedFY]);

    const fetchPendingCount = useCallback(async () => {
        try {
            const list = await getPaymentSubmissions();
            setPendingSubmissions((list || []).filter((s) => s.status === 'PENDING').length);
        } catch {
            /* the badge is a convenience; the review dialog loads its own list */
        }
    }, []);

    useEffect(() => {
        fetchBills();
    }, [fetchBills]);

    useEffect(() => { fetchPendingCount(); }, [fetchPendingCount]);

    const handleGenerateBills = async (formData) => {
        try {
            const { generateMonthlyBills } = await import('../api/adminApi');
            await generateMonthlyBills(formData.billingMonth, formData.amount, formData.dueDate);
            await fetchBills();
        } catch (err) {
            throw err;
        }
    };

    const handleImportFileChange = async (e) => {
        const file = e.target.files?.[0];
        if (!file) return;

        setIsImporting(true);
        try {
            const result = await importBillsExcel(file);
            notifySuccess(`Import finished: ${result.createdCount} bills created, ${result.skippedCount} skipped (${result.totalRows} rows).`);
            if (result.errors && result.errors.length > 0) {
                notifyError(`${result.errors.length} row(s) had problems: ${result.errors.slice(0, 3).join('; ')}${result.errors.length > 3 ? '…' : ''}`);
            }
            await fetchBills();
        } catch (err) {
            notifyError(errorMessage(err, 'Import failed.'));
        } finally {
            setIsImporting(false);
            if (fileInputRef.current) fileInputRef.current.value = '';
        }
    };

    const openPaymentModal = (bill) => {
        setSelectedBill(bill);
        setIsPaymentModalOpen(true);
    };

    const handlePaymentSuccess = async () => {
        await fetchBills();
    };

    const canPay = isOpen;

    const outstandingOf = (row) => Math.max(0, (parseFloat(row.amount) || 0) + (parseFloat(row.lateFee) || 0) - (parseFloat(row.paidAmount) || 0));

    const summary = useMemo(() => bills.reduce((acc, b) => {
        acc.billed += (parseFloat(b.amount) || 0) + (parseFloat(b.lateFee) || 0);
        acc.collected += parseFloat(b.paidAmount) || 0;
        acc.outstanding += canPay(b) ? outstandingOf(b) : 0;
        if (isOverdue(b)) acc.overdue += 1;
        return acc;
    }, { billed: 0, collected: 0, outstanding: 0, overdue: 0 }), [bills]); // eslint-disable-line react-hooks/exhaustive-deps

    const visible = useMemo(() => {
        if (statusFilter === 'unpaid') return bills.filter((b) => isUnpaid(b));
        if (statusFilter === 'overdue') return bills.filter((b) => isOverdue(b));
        if (statusFilter === 'paid') return bills.filter(isPaid);
        return bills;
    }, [bills, statusFilter]); // eslint-disable-line react-hooks/exhaustive-deps

    const columns = [
        {
            header: 'Bill',
            accessor: 'billingMonth',
            searchValue: (row) => monthLabel(row.billingMonth),
            render: (row) => (
                <div>
                    <div className="cell-primary">{monthLabel(row.billingMonth) || row.billingMonth}</div>
                    <div className="cell-sub">#{row.id} · {row.chargeType === 'NOC' ? 'NOC charge' : 'Maintenance'}</div>
                </div>
            ),
        },
        {
            header: 'Flat',
            accessor: 'roomNumber',
            render: (row) => (
                <div>
                    <div className="fw-600 num">{row.roomNumber}</div>
                    <div className="cell-sub truncate" style={{ maxWidth: 180 }}>{row.residentName || '—'}</div>
                </div>
            ),
        },
        {
            header: 'Amount',
            align: 'right',
            render: (row) => {
                const total = (parseFloat(row.amount) || 0) + (parseFloat(row.lateFee) || 0);
                const paid = parseFloat(row.paidAmount) || 0;
                const due = outstandingOf(row);
                return (
                    <div>
                        <div className="fw-600">{formatMoney(total, 2)}</div>
                        <div className="cell-sub">
                            {parseFloat(row.lateFee) > 0 && <span className="text-warning">incl. {formatMoney(row.lateFee)} late fee · </span>}
                            {paid > 0 && due > 0 ? `${formatMoney(due, 2)} left` : paid > 0 ? 'Fully paid' : 'Not paid'}
                        </div>
                    </div>
                );
            },
        },
        {
            header: 'Due',
            accessor: 'dueDate',
            render: (row) => <span className={isOverdue(row) ? 'text-danger fw-600 nowrap' : 'text-muted nowrap'}>{formatDate(row.dueDate)}</span>,
        },
        { header: 'Status', render: (row) => (isOverdue(row) ? <StatusChip tone="danger" label="Overdue" /> : <StatusChip status={row.status} />) },
        {
            header: <span className="visually-hidden">Actions</span>,
            render: (row) => (
                <div className="row-actions">
                    {canPay(row) && <Button variant="secondary" size="sm" icon={Banknote} onClick={() => openPaymentModal(row)}>Record payment</Button>}
                    <Button variant="ghost" size="sm" iconOnly icon={Download} title="Download invoice" aria-label={`Download invoice ${row.id}`}
                        onClick={() => downloadAuthenticatedFile(`/bills/${row.id}/invoice-pdf`, `invoice_${row.billingMonth || row.id}.pdf`)} />
                </div>
            ),
        },
    ];

    const toolbar = (
        <>
            <Segmented
                label="Filter by status"
                value={statusFilter}
                onChange={setStatusFilter}
                options={[{ value: 'all', label: 'All' }, { value: 'unpaid', label: 'Unpaid' }, { value: 'overdue', label: 'Overdue' }, { value: 'paid', label: 'Paid' }]}
            />
            <div style={{ width: 220 }}>
                <Select aria-label="Financial year" value={selectedFY} onChange={(e) => setSelectedFY(e.target.value)}>
                    <option value="">All financial years</option>
                    <option value="2026-2027">FY 2026–27</option>
                    <option value="2027-2028">FY 2027–28</option>
                    <option value="2028-2029">FY 2028–29</option>
                    <option value="2029-2030">FY 2029–30</option>
                    <option value="2030-2031">FY 2030–31</option>
                </Select>
            </div>
            <input type="file" ref={fileInputRef} accept=".xlsx" className="visually-hidden" tabIndex={-1} onChange={handleImportFileChange} />
            <Menu
                label="Excel import and export"
                trigger={(props) => <Button variant="secondary" icon={FileSpreadsheet} isLoading={isImporting} {...props}>Excel</Button>}
                items={[
                    { label: 'Export this view', icon: Download, onClick: () => exportBillsExcel(selectedFY) },
                    { label: 'Download import template', icon: FileText, onClick: downloadImportTemplate },
                    'separator',
                    { label: 'Import bills from Excel…', icon: Upload, onClick: () => fileInputRef.current?.click() },
                ]}
            />
        </>
    );

    return (
        <div>
            <PageHeader
                title="Billing"
                description="Generate monthly maintenance bills, record payments made at the office, and approve payments residents submit online."
                actions={(
                    <>
                        <Button variant="secondary" icon={ClipboardCheck} onClick={() => setIsApprovalsModalOpen(true)}>
                            Review payments{pendingSubmissions > 0 && <span className="count-pill">{pendingSubmissions}</span>}
                        </Button>
                        <Button icon={Plus} onClick={() => setIsGenerateModalOpen(true)}>Generate bills</Button>
                    </>
                )}
            />

            <div className="grid grid-4 mb-6">
                <StatCard label="Billed" value={formatMoney(summary.billed)} icon={Receipt} loading={isLoading} foot={`${bills.length} bills in view`} />
                <StatCard label="Collected" value={formatMoney(summary.collected)} icon={IndianRupee} tone="success" loading={isLoading}
                    foot={summary.billed ? `${Math.round((summary.collected / summary.billed) * 100)}% of billed` : '—'} />
                <StatCard label="Outstanding" value={formatMoney(summary.outstanding)} icon={Wallet} tone="warning" loading={isLoading} onClick={() => setStatusFilter('unpaid')} />
                <StatCard label="Overdue bills" value={summary.overdue} icon={AlertTriangle} tone="danger" loading={isLoading} onClick={() => setStatusFilter('overdue')} foot="Unpaid after their billing month" />
            </div>

            <DataTable
                columns={columns}
                data={visible}
                isLoading={isLoading}
                toolbar={toolbar}
                searchPlaceholder="Search flat, resident or month"
                emptyIcon={Receipt}
                emptyTitle={bills.length ? 'No bills match' : 'No bills yet'}
                emptyMessage={bills.length ? 'Try another status or year.' : 'Generate this month’s maintenance bills to get started.'}
                emptyAction={!bills.length && <Button size="sm" icon={Plus} onClick={() => setIsGenerateModalOpen(true)}>Generate bills</Button>}
            />

            <GenerateBillsDialog
                isOpen={isGenerateModalOpen}
                onClose={() => setIsGenerateModalOpen(false)}
                onConfirm={handleGenerateBills}
            />

            <RecordPaymentModal
                isOpen={isPaymentModalOpen}
                onClose={() => setIsPaymentModalOpen(false)}
                bill={selectedBill}
                onSuccess={handlePaymentSuccess}
            />

            <PendingPaymentApprovalsModal
                isOpen={isApprovalsModalOpen}
                onClose={() => { setIsApprovalsModalOpen(false); fetchPendingCount(); }}
                onActionSuccess={() => { fetchBills(); fetchPendingCount(); }}
            />
        </div>
    );
};

export default AdminBilling;
