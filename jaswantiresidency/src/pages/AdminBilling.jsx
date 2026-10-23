import React, { useState, useEffect, useCallback, useRef } from 'react';
import PageHeader from '../components/ui/PageHeader';
import DataTable from '../components/ui/DataTable';
import StatusChip from '../components/ui/StatusChip';
import Button from '../components/ui/Button';
import { Plus, Download, Banknote, CheckSquare, FileSpreadsheet, Upload, FileText } from 'lucide-react';
import GenerateBillsDialog from '../components/GenerateBillsDialog';
import RecordPaymentModal from '../components/RecordPaymentModal';
import PendingPaymentApprovalsModal from '../components/PendingPaymentApprovalsModal';
import { getBills, exportBillsExcel, downloadImportTemplate, importBillsExcel } from '../api/adminApi';

const AdminBilling = () => {
    const [bills, setBills] = useState([]);
    const [isLoading, setIsLoading] = useState(true);
    const [selectedFY, setSelectedFY] = useState('');
    const [isGenerateModalOpen, setIsGenerateModalOpen] = useState(false);
    const [isPaymentModalOpen, setIsPaymentModalOpen] = useState(false);
    const [isApprovalsModalOpen, setIsApprovalsModalOpen] = useState(false);
    const [selectedBill, setSelectedBill] = useState(null);
    const [isImporting, setIsImporting] = useState(false);

    const fileInputRef = useRef(null);

    const fetchBills = useCallback(async () => {
        setIsLoading(true);
        try {
            const data = await getBills(selectedFY || undefined);
            setBills(data || []);
        } catch (err) {
            console.error('Failed to fetch bills', err);
            setBills([]);
        } finally {
            setIsLoading(false);
        }
    }, [selectedFY]);

    useEffect(() => {
        fetchBills();
    }, [fetchBills]);

    const handleGenerateBills = async (formData) => {
        try {
            const { generateMonthlyBills } = await import('../api/adminApi');
            await generateMonthlyBills(formData.billingMonth, formData.amount, formData.dueDate);
            await fetchBills();
        } catch (err) {
            console.error('Failed to generate bills', err);
            throw err;
        }
    };

    const handleImportFileChange = async (e) => {
        const file = e.target.files?.[0];
        if (!file) return;

        setIsImporting(true);
        try {
            const result = await importBillsExcel(file);
            alert(`Excel Import Finished:\n- Total Rows Processed: ${result.totalRows}\n- Successfully Created: ${result.createdCount}\n- Skipped (Duplicates): ${result.skippedCount}${result.errors?.length > 0 ? '\n- Errors: ' + result.errors.slice(0, 5).join('; ') : ''}`);
            await fetchBills();
        } catch (err) {
            console.error('Excel import failed', err);
            alert(err?.response?.data?.message || err.message || 'Import failed.');
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

    const canPay = (row) =>
        ['PENDING', 'PARTIALLY_PAID'].includes(row.status);

    const columns = [
        { header: 'Invoice ID', accessor: 'id' },
        { header: 'Room', accessor: 'roomNumber' },
        { header: 'Resident', accessor: 'residentName' },
        { header: 'Period', accessor: 'billingMonth' },
        {
            header: 'Type',
            render: (row) => (
                <span style={{
                    padding: '0.15rem 0.45rem',
                    borderRadius: 4,
                    fontSize: '0.75rem',
                    fontWeight: 600,
                    background: row.chargeType === 'NOC' ? '#fef3c7' : '#e0e7ff',
                    color: row.chargeType === 'NOC' ? '#92400e' : '#3730a3'
                }}>
                    {row.chargeType || 'MAINTENANCE'}
                </span>
            )
        },
        {
            header: 'Amount',
            render: (row) => {
                const total = (parseFloat(row.amount) || 0) + (parseFloat(row.lateFee) || 0);
                const paid = parseFloat(row.paidAmount) || 0;
                const outstanding = Math.max(0, total - paid);
                return (
                    <span>
                        ₹{(parseFloat(row.amount) || 0).toFixed(2)}
                        {parseFloat(row.lateFee) > 0 && (
                            <span style={{ color: 'var(--warning)', fontSize: '0.8rem', marginLeft: '0.3rem' }}>
                                +₹{parseFloat(row.lateFee).toFixed(2)} late fee
                            </span>
                        )}
                        {paid > 0 && (
                            <span style={{ color: 'var(--success)', fontSize: '0.8rem', marginLeft: '0.3rem' }}>
                                · Paid ₹{paid.toFixed(2)}
                                {outstanding > 0 && ` · Due ₹${outstanding.toFixed(2)}`}
                            </span>
                        )}
                    </span>
                );
            },
        },
        { header: 'Due Date', accessor: 'dueDate' },
        { header: 'Status', render: (row) => <StatusChip status={row.status} /> },
        {
            header: 'Actions',
            render: (row) => (
                <div style={{ display: 'flex', gap: '0.5rem' }}>
                    <Button
                        variant="secondary"
                        title="Download Invoice PDF"
                        style={{ padding: '0.4rem 0.6rem' }}
                        onClick={() => window.open(`/api/bills/${row.id}/invoice-pdf`, '_blank')}
                    >
                        <Download size={16} />
                    </Button>
                    {canPay(row) && (
                        <Button
                            variant="secondary"
                            title="Record Payment"
                            style={{ padding: '0.4rem 0.6rem' }}
                            onClick={() => openPaymentModal(row)}
                        >
                            <Banknote size={16} style={{ color: 'var(--success)' }} />
                        </Button>
                    )}
                </div>
            ),
        },
    ];

    return (
        <div>
            <PageHeader
                title="Billing Management"
                breadcrumbs={['Admin', 'Billing']}
                actionButton={
                    <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
                        <Button variant="secondary" onClick={() => setIsApprovalsModalOpen(true)}>
                            <CheckSquare size={18} /> Review Payment Submissions
                        </Button>
                        <Button onClick={() => setIsGenerateModalOpen(true)}>
                            <Plus size={18} /> Generate Bills
                        </Button>
                    </div>
                }
            />

            {/* Controls Bar: FY Filter + Excel Bulk Operations */}
            <div style={{
                background: '#fff',
                padding: '0.85rem 1rem',
                borderRadius: 8,
                border: '1px solid var(--border-light)',
                marginBottom: '1rem',
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                flexWrap: 'wrap',
                gap: '0.75rem'
            }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                    <span style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-secondary-light)' }}>
                        Operating Period / FY:
                    </span>
                    <select
                        value={selectedFY}
                        onChange={(e) => setSelectedFY(e.target.value)}
                        style={{ padding: '0.45rem 0.75rem', borderRadius: 6, border: '1px solid var(--border-light)', fontSize: '0.85rem', fontWeight: 500 }}
                    >
                        <option value="">All Operating Years (2026 – 2031)</option>
                        <option value="2026-2027">FY 2026–27 (Apr 2026 – Mar 2027)</option>
                        <option value="2027-2028">FY 2027–28 (Apr 2027 – Mar 2028)</option>
                        <option value="2028-2029">FY 2028–29 (Apr 2028 – Mar 2029)</option>
                        <option value="2029-2030">FY 2029–30 (Apr 2029 – Mar 2030)</option>
                        <option value="2030-2031">FY 2030–31 (Apr 2030 – Mar 2031)</option>
                    </select>
                </div>

                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    <Button
                        variant="secondary"
                        onClick={() => exportBillsExcel(selectedFY)}
                        title="Export current view to Excel workbook"
                        style={{ padding: '0.4rem 0.75rem', fontSize: '0.85rem' }}
                    >
                        <FileSpreadsheet size={15} style={{ color: 'var(--success)' }} /> Export Excel
                    </Button>

                    <Button
                        variant="secondary"
                        onClick={downloadImportTemplate}
                        title="Download sample template for bulk import"
                        style={{ padding: '0.4rem 0.75rem', fontSize: '0.85rem' }}
                    >
                        <FileText size={15} /> Template
                    </Button>

                    <input
                        type="file"
                        ref={fileInputRef}
                        accept=".xlsx"
                        style={{ display: 'none' }}
                        onChange={handleImportFileChange}
                    />

                    <Button
                        variant="secondary"
                        onClick={() => fileInputRef.current?.click()}
                        disabled={isImporting}
                        title="Import bills from Excel workbook"
                        style={{ padding: '0.4rem 0.75rem', fontSize: '0.85rem' }}
                    >
                        <Upload size={15} /> {isImporting ? 'Importing...' : 'Import Excel'}
                    </Button>
                </div>
            </div>

            <DataTable
                columns={columns}
                data={bills}
                isLoading={isLoading}
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
                onClose={() => setIsApprovalsModalOpen(false)}
                onActionSuccess={fetchBills}
            />
        </div>
    );
};

export default AdminBilling;
