import React, { useState, useEffect } from 'react';
import PageHeader from '../components/ui/PageHeader';
import DataTable from '../components/ui/DataTable';
import StatusChip from '../components/ui/StatusChip';
import Button from '../components/ui/Button';
import ConfirmDialog from '../components/ui/ConfirmDialog';
import { Plus, Edit, Trash2, Ban, CheckCircle2 } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import DeleteConfirmationDialog from '../components/DeleteConfirmationDialog';
import ResidentModal from '../components/ResidentModal';

const AdminResidents = () => {
    const navigate = useNavigate();
    const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
    const [isResidentModalOpen, setIsResidentModalOpen] = useState(false);
    const [selectedResident, setSelectedResident] = useState(null);
    const [isProcessing, setIsProcessing] = useState(false);
    const [residents, setResidents] = useState([]);
    const [isLoading, setIsLoading] = useState(true);

    const fetchResidents = async () => {
        setIsLoading(true);
        try {
            const { getResidents } = await import('../api/adminApi');
            const data = await getResidents();
            setResidents(data);
            } catch (err) {
                console.error("Failed to fetch residents", err);
                // Fallback to stub if API fails
                setResidents([
                    { id: '1', roomNumber: 'A-101', fullName: 'John Doe', email: 'john@example.com', phoneNumber: '9876543210', status: 'Active' },
                    { id: '2', roomNumber: 'A-102', fullName: 'Jane Smith', email: 'jane@example.com', phoneNumber: '8765432109', status: 'Active' },
                    { id: '3', roomNumber: 'B-203', fullName: 'Bob Wilson', email: 'bob@example.com', phoneNumber: '7654321098', status: 'Blocked' }
                ]);
            } finally {
                setIsLoading(false);
            }
        };
    useEffect(() => {
        fetchResidents();
    }, []);

    const handleSaveResident = async (formData) => {
        const { addResident, updateResident } = await import('../api/adminApi');
        if (selectedResident) {
            await updateResident(selectedResident.id, formData);
        } else {
            await addResident(formData);
        }
        await fetchResidents();
    };

    const handleDelete = async () => {
        if (!selectedResident) return;
        setIsProcessing(true);
        try {
            const { deleteResident } = await import('../api/adminApi');
            await deleteResident(selectedResident.id);
            await fetchResidents();
            setIsDeleteModalOpen(false);
        } catch (e) {
            console.error(e);
        } finally {
            setIsProcessing(false);
        }
    };

    const handleToggleStatus = async (row) => {
        try {
            const newStatus = row.status === 'ACTIVE' ? 'BLOCKED' : 'ACTIVE';
            const { updateResidentStatus } = await import('../api/adminApi');
            await updateResidentStatus(row.id, newStatus);
            await fetchResidents();
        } catch (e) {
            console.error(e);
        }
    };

    const columns = [
        { header: 'Room', accessor: 'roomNumber' },
        { header: 'Name', accessor: 'fullName' },
        { header: 'Email', accessor: 'email' },
        { header: 'Phone', accessor: 'phoneNumber' },
        { header: 'Status', render: (row) => <StatusChip status={row.status} /> },
        { header: 'Actions', render: (row) => (
            <div style={{ display: 'flex', gap: '0.5rem' }}>
                <Button variant="secondary" title="Edit" style={{ padding: '0.4rem 0.6rem' }} onClick={() => { setSelectedResident(row); setIsResidentModalOpen(true); }}>
                    <Edit size={16} />
                </Button>
                <Button variant="secondary" title={row.status === 'ACTIVE' ? 'Block' : 'Activate'} style={{ padding: '0.4rem 0.6rem' }} onClick={() => handleToggleStatus(row)}>
                    {row.status === 'ACTIVE' ? <Ban size={16} className="text-warning" /> : <CheckCircle2 size={16} className="text-success" />}
                </Button>
                <Button variant="secondary" title="Delete" style={{ padding: '0.4rem 0.6rem' }} onClick={() => { setSelectedResident(row); setIsDeleteModalOpen(true); }}>
                    <Trash2 size={16} className="text-danger" />
                </Button>
            </div>
        )}
    ];

    return (
        <div>
            <PageHeader 
                title="Manage Residents" 
                breadcrumbs={['Admin', 'Residents']} 
                actionButton={
                    <div style={{ display: 'flex', gap: '1rem' }}>
                        <Button onClick={() => { setSelectedResident(null); setIsResidentModalOpen(true); }}><Plus size={18} /> Add Resident</Button>
                    </div>
                }
            />

            <DataTable 
                columns={columns} 
                data={residents} 
                isLoading={isLoading}
            />

            <ResidentModal 
                isOpen={isResidentModalOpen}
                onClose={() => setIsResidentModalOpen(false)}
                onSave={handleSaveResident}
                initialData={selectedResident}
            />

            <DeleteConfirmationDialog 
                isOpen={isDeleteModalOpen}
                title="Delete Resident"
                itemName={selectedResident?.fullName}
                onClose={() => setIsDeleteModalOpen(false)}
                onConfirm={handleDelete}
                isDeleting={isProcessing}
            />
        </div>
    );
};

export default AdminResidents;
