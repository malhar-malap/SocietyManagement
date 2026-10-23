import React, { useState, useEffect } from 'react';
import { MessageSquare, Trash2, Paperclip } from 'lucide-react';
import UpdateComplaintModal from '../components/UpdateComplaintModal';
import DeleteConfirmationDialog from '../components/DeleteConfirmationDialog';
import PageHeader from '../components/ui/PageHeader';
import DataTable from '../components/ui/DataTable';
import Button from '../components/ui/Button';
import StatusChip from '../components/ui/StatusChip';
import { getComplaints, respondToComplaint, deleteComplaint } from '../api/adminApi';
import { viewAuthenticatedFile } from '../api/fileUtils';

const AdminComplaints = () => {
    const [complaints, setComplaints] = useState([]);
    const [selectedType, setSelectedType] = useState('ALL');
    const [isLoading, setIsLoading] = useState(true);
    const [isUpdateModalOpen, setIsUpdateModalOpen] = useState(false);
    const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
    const [selectedComplaint, setSelectedComplaint] = useState(null);
    const [isProcessing, setIsProcessing] = useState(false);

    const fetchComplaints = async () => {
        setIsLoading(true);
        try {
            const data = await getComplaints();
            setComplaints(data || []);
        } catch (err) {
            console.error("Failed to fetch complaints", err);
            setComplaints([]);
        } finally {
            setIsLoading(false);
        }
    };

    useEffect(() => {
        fetchComplaints();
    }, []);

    const handleUpdateStatus = async (complaintId, newStatus, adminResponse) => {
        try {
            await respondToComplaint(complaintId, { status: newStatus, adminResponse });
            await fetchComplaints();
            setIsUpdateModalOpen(false);
        } catch (err) {
            console.error("Failed to update status", err);
            alert(err?.response?.data?.message || 'Failed to update response.');
        }
    };

    const handleDelete = async () => {
        if (!selectedComplaint) return;
        setIsProcessing(true);
        try {
            await deleteComplaint(selectedComplaint.id);
            await fetchComplaints();
            setIsDeleteModalOpen(false);
        } catch (e) {
            console.error(e);
        } finally {
            setIsProcessing(false);
        }
    };

    const filteredComplaints = complaints.filter(c => {
        if (selectedType === 'ALL') return true;
        return c.submissionType === selectedType;
    });

    const getTypeBadge = (type) => {
        const styles = {
            COMPLAINT: { bg: '#fee2e2', text: '#991b1b' },
            QUERY: { bg: '#e0e7ff', text: '#3730a3' },
            SUGGESTION: { bg: '#fef3c7', text: '#92400e' },
            PERMISSION: { bg: '#dcfce7', text: '#166534' }
        };
        const s = styles[type] || styles.COMPLAINT;
        return (
            <span style={{ background: s.bg, color: s.text, padding: '0.15rem 0.5rem', borderRadius: 4, fontSize: '0.75rem', fontWeight: 700 }}>
                {type || 'COMPLAINT'}
            </span>
        );
    };

    const columns = [
        { header: 'ID', accessor: 'id' },
        {
            header: 'Type',
            render: (row) => getTypeBadge(row.submissionType)
        },
        { header: 'Room', accessor: 'roomNumber' },
        { header: 'Resident', accessor: 'residentName' },
        { header: 'Category', accessor: 'category' },
        {
            header: 'Description / Scope',
            render: (row) => (
                <div style={{ maxWidth: '280px' }}>
                    <div style={{ fontSize: '0.85rem' }}>{row.description}</div>
                    {row.submissionType === 'PERMISSION' && (
                        <div style={{ fontSize: '0.75rem', color: 'var(--primary)', marginTop: '0.2rem' }}>
                            Scope: {row.permissionCategory} ({row.startDate || '?'} to {row.endDate || '?'})
                        </div>
                    )}
                    {row.hasAttachment && (
                        <button
                            type="button"
                            onClick={() => viewAuthenticatedFile(`/complaints/${row.id}/attachment`)}
                            style={{ background: 'none', border: 'none', cursor: 'pointer', padding: 0, display: 'inline-flex', alignItems: 'center', gap: '0.25rem', fontSize: '0.75rem', color: '#2563eb', marginTop: '0.2rem' }}
                        >
                            <Paperclip size={12} /> View Attached Proof
                        </button>
                    )}
                </div>
            )
        },
        {
            header: 'Admin Response',
            render: (row) => row.adminResponse ? (
                <span style={{ fontSize: '0.8rem', color: 'var(--text-secondary-light)', fontStyle: 'italic' }}>
                    "{row.adminResponse}"
                </span>
            ) : (
                <span style={{ fontSize: '0.75rem', color: '#94a3b8' }}>None</span>
            )
        },
        { header: 'Status', render: (row) => <StatusChip status={row.status} /> },
        {
            header: 'Actions',
            render: (row) => (
                <div style={{ display: 'flex', gap: '0.4rem' }}>
                    <Button
                        variant="secondary"
                        title="Respond / Update Status"
                        style={{ padding: '0.35rem 0.6rem', fontSize: '0.8rem' }}
                        onClick={() => { setSelectedComplaint(row); setIsUpdateModalOpen(true); }}
                    >
                        <MessageSquare size={14} className="text-primary" /> Respond
                    </Button>
                    <Button
                        variant="secondary"
                        title="Delete Request"
                        style={{ padding: '0.35rem 0.6rem' }}
                        onClick={() => { setSelectedComplaint(row); setIsDeleteModalOpen(true); }}
                    >
                        <Trash2 size={14} className="text-danger" />
                    </Button>
                </div>
            )
        }
    ];

    return (
        <div>
            <PageHeader
                title="Helpdesk & Resident Requests"
                breadcrumbs={['Admin', 'Helpdesk']}
            />

            {/* Filter Tabs */}
            <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '1.25rem', flexWrap: 'wrap' }}>
                {['ALL', 'COMPLAINT', 'QUERY', 'SUGGESTION', 'PERMISSION'].map(type => (
                    <button
                        key={type}
                        onClick={() => setSelectedType(type)}
                        style={{
                            padding: '0.45rem 0.85rem',
                            borderRadius: '6px',
                            border: selectedType === type ? '1px solid var(--primary)' : '1px solid var(--border-light)',
                            background: selectedType === type ? 'var(--primary)' : '#fff',
                            color: selectedType === type ? '#fff' : 'var(--text-secondary-light)',
                            fontWeight: 600,
                            fontSize: '0.85rem',
                            cursor: 'pointer'
                        }}
                    >
                        {type === 'ALL' ? 'All Requests' : type.charAt(0) + type.slice(1).toLowerCase() + 's'}
                    </button>
                ))}
            </div>

            <DataTable
                columns={columns}
                data={filteredComplaints}
                isLoading={isLoading}
                emptyMessage="No requests found."
            />

            <UpdateComplaintModal
                isOpen={isUpdateModalOpen}
                onClose={() => setIsUpdateModalOpen(false)}
                onConfirm={handleUpdateStatus}
                complaintDetails={selectedComplaint}
            />

            <DeleteConfirmationDialog
                isOpen={isDeleteModalOpen}
                title="Delete Request"
                itemName={`Request #${selectedComplaint?.id}`}
                onClose={() => setIsDeleteModalOpen(false)}
                onConfirm={handleDelete}
                isDeleting={isProcessing}
            />
        </div>
    );
};

export default AdminComplaints;
