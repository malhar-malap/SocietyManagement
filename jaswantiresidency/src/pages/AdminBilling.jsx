import React, { useState, useEffect, useCallback } from 'react';
import PageHeader from '../components/ui/PageHeader';
import DataTable from '../components/ui/DataTable';
import StatusChip from '../components/ui/StatusChip';
import Button from '../components/ui/Button';
import { Plus, Download, Banknote } from 'lucide-react';
import GenerateBillsDialog from '../components/GenerateBillsDialog';
import RecordPaymentModal from '../components/RecordPaymentModal';

const AdminBilling = () => {
    const [bills, setBills] = useState([]);
    const [isLoading, setIsLoading] = useState(true);
    const [isGenerateModalOpen, setIsGenerateModalOpen] = useState(false);
    const [isPaymentModalOpen, setIsPaymentModalOpen] = useState(false);
    const [selectedBill, setSelectedBill] = useState(null);

    const fetchBills = useCallback(async () => {
        setIsLoading(true);
        try {
            const { getBills } = await import('../api/adminApi');
            const data = await getBills();
            setBills(data);
        } catch (err) {
            console.error('Failed to fetch bills', err);
            setBills([]);
        } finally {
            setIsLoading(false);
        }
    }, []);

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
            throw err; // let the dialog surface the error
        }
    };

    const openPaymentModal = (bill) => {
        setSelectedBill(bill);
        setIsPaymentModalOpen(true);
    };

    const handlePaymentSuccess = async () => {
        // Refresh the table so updated status/paidAmount are visible
        await fetchBills();
    };

    // Bills that can still accept a payment
    const canPay = (row) =>
        ['PENDING', 'PARTIALLY_PAID'].includes(row.status);

    const columns = [
        { header: 'Invoice ID', accessor: 'id' },
        { header: 'Room',       accessor: 'roomNumber' },
        { header: 'Resident',   accessor: 'residentName' },
        { header: 'Period',     accessor: 'billingMonth' },
        {
            header: 'Amount',
            render: (row) => {
                const total = (parseFloat(row.amount) || 0) + (parseFloat(row.lateFee) || 0);
                const paid  = parseFloat(row.paidAmount) || 0;
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
        { header: 'Due Date',   accessor: 'dueDate' },
        { header: 'Status',     render: (row) => <StatusChip status={row.status} /> },
        {
            header: 'Actions',
            render: (row) => (
                <div style={{ display: 'flex', gap: '0.5rem' }}>
                    <Button
                        variant="secondary"
                        title="Download Invoice"
                        style={{ padding: '0.4rem 0.6rem' }}
                        onClick={() => alert('Downloading...')}
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
                    <Button onClick={() => setIsGenerateModalOpen(true)}>
                        <Plus size={18} /> Generate Bills
                    </Button>
                }
            />

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
        </div>
    );
};

export default AdminBilling;
