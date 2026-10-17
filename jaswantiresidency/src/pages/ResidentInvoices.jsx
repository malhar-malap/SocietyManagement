import React, { useState, useEffect } from 'react';
import PageHeader from '../components/ui/PageHeader';
import DataTable from '../components/ui/DataTable';
import StatusChip from '../components/ui/StatusChip';
import Button from '../components/ui/Button';
import PaymentModal from '../components/PaymentModal';
import { Download, CreditCard } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

const ResidentInvoices = () => {
    const { user } = useAuth();
    const [invoices, setInvoices] = useState([]);
    const [isLoading, setIsLoading] = useState(true);
    const [isPaymentModalOpen, setIsPaymentModalOpen] = useState(false);
    const [selectedBill, setSelectedBill] = useState(null);

    useEffect(() => {
        const fetchInvoices = async () => {
            if (!user?.roomId) return;
            try {
                const { getBillsByRoom } = await import('../api/billingApi');
                const data = await getBillsByRoom(user.roomId);
                // Sort so newest are first
                const sorted = data.sort((a, b) => new Date(b.dueDate) - new Date(a.dueDate));
                setInvoices(sorted);
            } catch (err) {
                console.error("Failed to fetch invoices", err);
                // Fallback to stub if API fails
                setInvoices([
                    { id: 'INV-2026-07', amount: 3500, date: '2026-07-01', dueDate: '2026-07-15', status: 'PAID' },
                    { id: 'INV-2026-08', amount: 3500, date: '2026-08-01', dueDate: '2026-08-15', status: 'PENDING' }
                ]);
            } finally {
                setIsLoading(false);
            }
        };
        fetchInvoices();
    }, [user]);

    const handlePayClick = (bill) => {
        setSelectedBill(bill);
        setIsPaymentModalOpen(true);
    };

    const columns = [
        { header: 'Invoice ID', accessor: 'id' },
        { header: 'Billing Month', accessor: 'billingMonth' },
        { header: 'Amount', render: (row) => `₹${row.amount}` },
        { header: 'Due Date', accessor: 'dueDate' },
        { header: 'Status', render: (row) => <StatusChip status={row.status} /> },
        { header: 'Actions', render: (row) => (
            <div style={{ display: 'flex', gap: '0.5rem' }}>
                <Button variant="secondary" title="Download PDF" style={{ padding: '0.4rem 0.6rem' }} onClick={() => alert('Downloading PDF...')}>
                    <Download size={16} />
                </Button>
                {row.status === 'PENDING' && (
                    <Button onClick={() => handlePayClick(row)} style={{ padding: '0.4rem 0.8rem' }}>
                        <CreditCard size={16} /> Pay
                    </Button>
                )}
            </div>
        )}
    ];

    return (
        <div>
            <PageHeader 
                title="My Invoices" 
                breadcrumbs={['Resident', 'Invoices']} 
            />
            
            <DataTable 
                columns={columns} 
                data={invoices} 
                isLoading={isLoading}
                emptyMessage="No invoices found."
            />

            {isPaymentModalOpen && (
                <PaymentModal 
                    amount={selectedBill?.amount} 
                    bills={[selectedBill]}
                    onClose={() => {
                        setIsPaymentModalOpen(false);
                        setSelectedBill(null);
                    }} 
                />
            )}
        </div>
    );
};

export default ResidentInvoices;
