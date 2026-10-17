import React, { useState, useEffect } from 'react';
import { Eye, CheckCircle2, MessageSquare, Trash2 } from 'lucide-react';
import UpdateComplaintModal from '../components/UpdateComplaintModal';
import DeleteConfirmationDialog from '../components/DeleteConfirmationDialog';
import PageHeader from '../components/ui/PageHeader';
import DataTable from '../components/ui/DataTable';
import Button from '../components/ui/Button';
import StatusChip from '../components/ui/StatusChip';

const AdminComplaints = () => {
    const [complaints, setComplaints] = useState([]);
    const [isLoading, setIsLoading] = useState(true);
    const [isUpdateModalOpen, setIsUpdateModalOpen] = useState(false);
    const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
    const [selectedComplaint, setSelectedComplaint] = useState(null);
    const [isProcessing, setIsProcessing] = useState(false);

    const fetchComplaints = async () => {
        setIsLoading(true);
        try {
            const { getComplaints } = await import('../api/adminApi');
            const data = await getComplaints();
            setComplaints(data);
        } catch (err) {
            console.error("Failed to fetch complaints", err);
        } finally {
            setIsLoading(false);
        }
    };

    useEffect(() => {
        fetchComplaints();
    }, []);

    const handleUpdateStatus = async (complaintId, newStatus) => {
        try {
            const { updateComplaintStatus } = await import('../api/adminApi');
            await updateComplaintStatus(complaintId, newStatus);
            await fetchComplaints();
            setIsUpdateModalOpen(false);
        } catch (err) {
            console.error("Failed to update status", err);
        }
    };

    const handleDelete = async () => {
        if (!selectedComplaint) return;
        setIsProcessing(true);
        try {
            const { deleteComplaint } = await import('../api/adminApi');
            await deleteComplaint(selectedComplaint.id);
            await fetchComplaints();
            setIsDeleteModalOpen(false);
        } catch (e) {
            console.error(e);
        } finally {
            setIsProcessing(false);
        }
    };

    const columns = [
        { header: 'ID', accessor: 'id' },
        { header: 'Room', accessor: 'roomNumber' },
        { header: 'Category', accessor: 'category' },
        { header: 'Title', accessor: 'title' },
        { header: 'Status', render: (row) => <StatusChip status={row.status} /> },
        { header: 'Actions', render: (row) => (
            <div style={{ display: 'flex', gap: '0.5rem' }}>
                <Button variant="secondary" title="View Details" style={{ padding: '0.4rem 0.6rem' }} onClick={() => alert('Viewing details...')}>
                    <Eye size={16} />
                </Button>
                {row.status !== 'RESOLVED' && (
                    <Button variant="secondary" title="Update Status" style={{ padding: '0.4rem 0.6rem' }} onClick={() => { setSelectedComplaint(row); setIsUpdateModalOpen(true); }}>
                        <CheckCircle2 size={16} className="text-primary" />
                    </Button>
                )}
                <Button variant="secondary" title="Delete" style={{ padding: '0.4rem 0.6rem' }} onClick={() => { setSelectedComplaint(row); setIsDeleteModalOpen(true); }}>
                    <Trash2 size={16} className="text-danger" />
                </Button>
            </div>
        )}
    ];

    return (
        <div>
            <PageHeader 
                title="Manage Complaints" 
                breadcrumbs={['Admin', 'Complaints']} 
            />

            <DataTable 
                columns={columns} 
                data={complaints} 
                isLoading={isLoading}
            />

            <UpdateComplaintModal 
                isOpen={isUpdateModalOpen}
                onClose={() => setIsUpdateModalOpen(false)}
                onConfirm={handleUpdateStatus}
                complaintDetails={selectedComplaint}
            />

            <DeleteConfirmationDialog 
                isOpen={isDeleteModalOpen}
                title="Delete Complaint"
                itemName={`Complaint ${selectedComplaint?.id}`}
                onClose={() => setIsDeleteModalOpen(false)}
                onConfirm={handleDelete}
                isDeleting={isProcessing}
            />
        </div>
    );
};

export default AdminComplaints;
