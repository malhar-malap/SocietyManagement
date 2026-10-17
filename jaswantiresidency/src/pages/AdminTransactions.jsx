import React, { useState, useEffect } from 'react';
import PageHeader from '../components/ui/PageHeader';
import DataTable from '../components/ui/DataTable';
import StatusChip from '../components/ui/StatusChip';

const AdminTransactions = () => {
    const [transactions, setTransactions] = useState([]);
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        const fetchTransactions = async () => {
            try {
                const { getTransactions } = await import('../api/adminApi');
                const data = await getTransactions();
                setTransactions(data);
            } catch (err) {
                console.error("Failed to fetch transactions", err);
                // Fallback to stub if API fails
                setTransactions([
                    { id: 'TXN-9876543210', room: 'A-102', resident: 'Jane Smith', amount: 3500, mode: 'Razorpay', gatewayRef: 'pay_ABC123XYZ', status: 'Success', date: '2026-07-02' },
                    { id: 'TXN-9876543211', room: 'B-203', resident: 'Bob Wilson', amount: 4000, mode: 'Cash', gatewayRef: 'N/A', status: 'Success', date: '2026-07-05' },
                    { id: 'TXN-9876543212', room: 'A-101', resident: 'John Doe', amount: 3500, mode: 'Razorpay', gatewayRef: 'pay_DEF456XYZ', status: 'Failed', date: '2026-07-10' }
                ]);
            } finally {
                setIsLoading(false);
            }
        };
        fetchTransactions();
    }, []);

    const columns = [
        { header: 'Transaction ID', accessor: 'id' },
        { header: 'Room', accessor: 'room' },
        { header: 'Amount', render: (row) => `₹${row.amount}` },
        { header: 'Mode', accessor: 'mode' },
        { header: 'Gateway Ref', accessor: 'gatewayRef' },
        { header: 'Date', accessor: 'date' },
        { header: 'Status', render: (row) => <StatusChip status={row.status === 'Success' ? 'Paid' : 'Blocked'} /> }
    ];

    return (
        <div>
            <PageHeader 
                title="Transaction History" 
                breadcrumbs={['Admin', 'Transactions']} 
            />

            <DataTable 
                columns={columns} 
                data={transactions} 
                isLoading={isLoading}
            />
        </div>
    );
};

export default AdminTransactions;
