import React, { useState, useEffect } from 'react';
import PageHeader from '../components/ui/PageHeader';
import DataTable from '../components/ui/DataTable';
import StatusChip from '../components/ui/StatusChip';
import Button from '../components/ui/Button';
import ConfirmDialog from '../components/ui/ConfirmDialog';
import { Plus, Download, CheckCircle, CreditCard, Banknote } from 'lucide-react';
import GenerateBillsDialog from '../components/GenerateBillsDialog';
import MarkPaidCashDialog from '../components/MarkPaidCashDialog';

const AdminBilling = () => {
    const [bills, setBills] = useState([]);
    const [isLoading, setIsLoading] = useState(true);
    const [isGenerateModalOpen, setIsGenerateModalOpen] = useState(false);
    const [isMarkPaidModalOpen, setIsMarkPaidModalOpen] = useState(false);
    const [selectedBill, setSelectedBill] = useState(null);

    const fetchBills = async () => {
        setIsLoading(true);
        try {
            const { getBills } = await import('../api/adminApi');
            const data = await getBills();
            setBills(data);
        } catch (err) {
            console.error("Failed to fetch bills", err);
            setBills([
                { id: 'INV-2026-07-1', roomNumber: 'A-101', residentName: 'John Doe', amount: 3500, status: 'PENDING', issueDate: '2026-07-01' },
                { id: 'INV-2026-07-2', roomNumber: 'A-102', residentName: 'Jane Smith', amount: 3500, status: 'PAID', issueDate: '2026-07-01' },
                { id: 'INV-2026-07-3', roomNumber: 'B-203', residentName: 'Bob Wilson', amount: 4000, status: 'PAID_CASH', issueDate: '2026-07-01' }
            ]);
        } finally {
            setIsLoading(false);
        }
    };

    useEffect(() => {
        fetchBills();
    }, []);

    const handleGenerateBills = async (formData) => {
        try {
            const { generateMonthlyBills } = await import('../api/adminApi');
            await generateMonthlyBills(formData.billingMonth, formData.amount, formData.dueDate);
            await fetchBills();
        } catch (err) {
            console.error("Failed to generate bills", err);
        }
    };

    const handleMarkPaidCash = async (billId) => {
        try {
            const { markPaidCash } = await import('../api/adminApi');
            await markPaidCash(billId);
            await fetchBills();
        } catch (err) {
            console.error("Failed to mark as paid", err);
        }
    };

    const columns = [
        { header: 'Invoice ID', accessor: 'id' },
        { header: 'Room', accessor: 'roomNumber' },
        { header: 'Resident', accessor: 'residentName' },
        { header: 'Amount', render: (row) => `₹${row.amount}` },
        { header: 'Issued On', accessor: 'issueDate' },
        { header: 'Status', render: (row) => <StatusChip status={row.status} /> },
        { header: 'Actions', render: (row) => (
            <div style={{ display: 'flex', gap: '0.5rem' }}>
                <Button variant="secondary" title="Download Invoice" style={{ padding: '0.4rem 0.6rem' }} onClick={() => alert('Downloading...')}>
                    <Download size={16} />
                </Button>
                {row.status === 'PENDING' && (
                    <Button variant="secondary" title="Mark Paid (Cash)" style={{ padding: '0.4rem 0.6rem' }} onClick={() => { setSelectedBill(row); setIsMarkPaidModalOpen(true); }}>
                        <Banknote size={16} className="text-success" />
                    </Button>
                )}
            </div>
        )}
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

            <MarkPaidCashDialog 
                isOpen={isMarkPaidModalOpen}
                onClose={() => setIsMarkPaidModalOpen(false)}
                onConfirm={handleMarkPaidCash}
                billDetails={selectedBill}
            />
        </div>
    );
};

export default AdminBilling;
