import React, { useState, useEffect } from 'react';
import PageHeader from '../components/ui/PageHeader';
import DataTable from '../components/ui/DataTable';
import StatusChip from '../components/ui/StatusChip';
import Button from '../components/ui/Button';
import { Plus, Edit, Trash2 } from 'lucide-react';
import AnnouncementModal from '../components/AnnouncementModal';
import DeleteConfirmationDialog from '../components/DeleteConfirmationDialog';
import { useAuth } from '../context/AuthContext';

const AdminAnnouncements = () => {
    const { user } = useAuth();
    const [announcements, setAnnouncements] = useState([]);
    const [isLoading, setIsLoading] = useState(true);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
    const [selectedAnnouncement, setSelectedAnnouncement] = useState(null);
    const [isProcessing, setIsProcessing] = useState(false);

    const fetchAnnouncements = async () => {
        setIsLoading(true);
        try {
            const { getAllAnnouncements } = await import('../api/adminApi');
            const data = await getAllAnnouncements();
            setAnnouncements(data);
        } catch (err) {
            console.error("Failed to fetch announcements", err);
            // Fallback to stub if API fails
            setAnnouncements([
                { id: 1, title: 'Scheduled Power Outage', description: 'Power will be cut for maintenance from 10 AM to 2 PM.', createdDate: '2026-07-15T10:00:00', expiryDate: '2026-07-15T14:00:00', status: 'Active' },
                { id: 2, title: 'Society Meeting', description: 'Annual general body meeting in clubhouse.', createdDate: '2026-07-20T11:00:00', expiryDate: '2026-07-20T13:00:00', status: 'Active' },
                { id: 3, title: 'Lift Maintenance', description: 'A-wing lift under repair.', createdDate: '2026-07-01T09:00:00', expiryDate: '2026-07-01T18:00:00', status: 'Expired' }
            ]);
        } finally {
            setIsLoading(false);
        }
    };

    useEffect(() => {
        fetchAnnouncements();
    }, []);

    const handleSaveAnnouncement = async (formData) => {
        const { createAnnouncement, updateAnnouncement } = await import('../api/adminApi');
        if (selectedAnnouncement) {
            await updateAnnouncement(selectedAnnouncement.id, formData);
        } else {
            await createAnnouncement(formData, user?.userId);
        }
        await fetchAnnouncements();
        setIsModalOpen(false);
    };

    const handleDelete = async () => {
        if (!selectedAnnouncement) return;
        setIsProcessing(true);
        try {
            const { deleteAnnouncement } = await import('../api/adminApi');
            await deleteAnnouncement(selectedAnnouncement.id);
            await fetchAnnouncements();
            setIsDeleteModalOpen(false);
        } catch (e) {
            console.error(e);
        } finally {
            setIsProcessing(false);
        }
    };

    const columns = [
        { header: 'Title', accessor: 'title' },
        { header: 'Start Date', render: (row) => row.createdDate ? new Date(row.createdDate).toLocaleDateString() : 'N/A' },
        { header: 'Expiry Date', render: (row) => row.expiryDate ? new Date(row.expiryDate).toLocaleDateString() : 'N/A' },
        { header: 'Status', render: (row) => <StatusChip status={row.expiryDate && new Date(row.expiryDate) < new Date() ? 'Expired' : 'Active'} /> },
        { header: 'Actions', render: (row) => (
            <div style={{ display: 'flex', gap: '0.5rem' }}>
                <Button variant="secondary" title="Edit" style={{ padding: '0.4rem 0.6rem' }} onClick={() => { setSelectedAnnouncement(row); setIsModalOpen(true); }}>
                    <Edit size={16} />
                </Button>
                <Button variant="secondary" title="Delete" style={{ padding: '0.4rem 0.6rem' }} onClick={() => { setSelectedAnnouncement(row); setIsDeleteModalOpen(true); }}>
                    <Trash2 size={16} className="text-danger" />
                </Button>
            </div>
        )}
    ];

    return (
        <div>
            <PageHeader 
                title="Announcements" 
                breadcrumbs={['Admin', 'Announcements']} 
                actionButton={
                    <Button onClick={() => { setSelectedAnnouncement(null); setIsModalOpen(true); }}>
                        <Plus size={18} /> New Notice
                    </Button>
                }
            />

            <DataTable 
                columns={columns} 
                data={announcements} 
                isLoading={isLoading}
            />

            <AnnouncementModal 
                isOpen={isModalOpen}
                onClose={() => setIsModalOpen(false)}
                onSave={handleSaveAnnouncement}
                initialData={selectedAnnouncement}
            />

            <DeleteConfirmationDialog 
                isOpen={isDeleteModalOpen}
                title="Delete Announcement"
                itemName={selectedAnnouncement?.title}
                onClose={() => setIsDeleteModalOpen(false)}
                onConfirm={handleDelete}
                isDeleting={isProcessing}
            />
        </div>
    );
};

export default AdminAnnouncements;
