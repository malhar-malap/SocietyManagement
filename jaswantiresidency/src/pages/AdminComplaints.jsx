import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { MessageSquare, MessageSquareReply, Search, Trash2 } from 'lucide-react';
import { notifyError, notifySuccess, errorMessage } from '../utils/notify';
import UpdateComplaintModal from '../components/UpdateComplaintModal';
import DeleteConfirmationDialog from '../components/DeleteConfirmationDialog';
import TicketCard, { TYPE_LABELS } from '../components/helpdesk/TicketCard';
import PageHeader from '../components/ui/PageHeader';
import Button from '../components/ui/Button';
import Menu from '../components/ui/Menu';
import { EmptyState, Segmented, Skeleton, Tabs } from '../components/ui/Feedback';
import { getComplaints, respondToComplaint, deleteComplaint } from '../api/adminApi';

const OPEN_STATES = ['OPEN', 'IN_PROGRESS'];

const AdminComplaints = () => {
    const [complaints, setComplaints] = useState([]);
    const [selectedType, setSelectedType] = useState('ALL');
    const [statusFilter, setStatusFilter] = useState('open');
    const [query, setQuery] = useState('');
    const [isLoading, setIsLoading] = useState(true);
    const [isUpdateModalOpen, setIsUpdateModalOpen] = useState(false);
    const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
    const [selectedComplaint, setSelectedComplaint] = useState(null);
    const [isProcessing, setIsProcessing] = useState(false);

    const fetchComplaints = useCallback(async () => {
        setIsLoading(true);
        try {
            const data = await getComplaints();
            setComplaints((data || []).sort((a, b) => new Date(b.createdDate || 0) - new Date(a.createdDate || 0)));
        } catch (err) {
            notifyError('Could not load helpdesk requests. Please refresh the page.');
            setComplaints([]);
        } finally {
            setIsLoading(false);
        }
    }, []);

    useEffect(() => { fetchComplaints(); }, [fetchComplaints]);

    const handleUpdateStatus = async (complaintId, newStatus, adminResponse) => {
        try {
            await respondToComplaint(complaintId, { status: newStatus, adminResponse });
            notifySuccess('Reply saved — the resident has been notified');
            await fetchComplaints();
            setIsUpdateModalOpen(false);
        } catch (err) {
            throw new Error(errorMessage(err, 'We could not save your reply. Please try again.'));
        }
    };

    const handleDelete = async () => {
        if (!selectedComplaint) return;
        setIsProcessing(true);
        try {
            await deleteComplaint(selectedComplaint.id);
            notifySuccess(`Request #${selectedComplaint.id} deleted`);
            await fetchComplaints();
            setIsDeleteModalOpen(false);
        } catch (e) {
            notifyError(errorMessage(e, 'We could not delete this request.'));
        } finally {
            setIsProcessing(false);
        }
    };

    const byType = useMemo(() => complaints.filter((c) => selectedType === 'ALL' || c.submissionType === selectedType), [complaints, selectedType]);

    const visible = useMemo(() => {
        const term = query.trim().toLowerCase();
        return byType
            .filter((c) => (statusFilter === 'all' ? true : statusFilter === 'open' ? OPEN_STATES.includes(c.status) : !OPEN_STATES.includes(c.status)))
            .filter((c) => !term || [c.description, c.category, c.roomNumber, c.residentName, String(c.id)].join(' ').toLowerCase().includes(term));
    }, [byType, statusFilter, query]);

    const openCount = (type) => complaints.filter((c) => (type === 'ALL' || c.submissionType === type) && OPEN_STATES.includes(c.status)).length;
    const tabs = ['ALL', 'COMPLAINT', 'QUERY', 'SUGGESTION', 'PERMISSION'].map((t) => ({
        value: t,
        label: t === 'ALL' ? 'All requests' : `${TYPE_LABELS[t]}s`,
        count: openCount(t) || undefined,
    }));

    const respond = (c) => { setSelectedComplaint(c); setIsUpdateModalOpen(true); };

    return (
        <div>
            <PageHeader
                title="Helpdesk"
                description="Complaints, questions, suggestions and permission requests from residents. Replies are sent to the resident straight away."
            />

            <Tabs tabs={tabs} value={selectedType} onChange={setSelectedType} label="Request type" />

            <div className="row row-wrap row-between mb-4">
                <Segmented
                    label="Filter by status"
                    value={statusFilter}
                    onChange={setStatusFilter}
                    options={[{ value: 'open', label: 'Needs attention' }, { value: 'closed', label: 'Closed' }, { value: 'all', label: 'All' }]}
                />
                <div className="table-search" style={{ flex: '0 1 300px' }}>
                    <Search size={16} aria-hidden="true" />
                    <input type="search" placeholder="Search requests" aria-label="Search requests" value={query} onChange={(e) => setQuery(e.target.value)} />
                </div>
            </div>

            {isLoading ? (
                <div className="ticket-list">{[0, 1, 2].map((i) => <Skeleton key={i} height={140} style={{ borderRadius: 14 }} />)}</div>
            ) : visible.length === 0 ? (
                <div className="card">
                    <EmptyState icon={MessageSquare} title={statusFilter === 'open' ? 'Nothing needs attention' : 'No requests here'}>
                        {statusFilter === 'open' ? 'Every request has been answered. New ones will appear here.' : 'Try a different filter.'}
                    </EmptyState>
                </div>
            ) : (
                <div className="ticket-list">
                    {visible.map((c) => (
                        <TicketCard
                            key={c.id}
                            ticket={c}
                            showResident
                            onOpen={() => respond(c)}
                            actions={(
                                <>
                                    <Button size="sm" variant={OPEN_STATES.includes(c.status) ? 'primary' : 'secondary'} icon={MessageSquareReply} onClick={() => respond(c)}>
                                        {c.adminResponse ? 'Update reply' : 'Reply'}
                                    </Button>
                                    <Menu label={`More actions for request ${c.id}`} items={[
                                        { label: 'Delete request', icon: Trash2, danger: true, onClick: () => { setSelectedComplaint(c); setIsDeleteModalOpen(true); } },
                                    ]} />
                                </>
                            )}
                        />
                    ))}
                </div>
            )}

            <UpdateComplaintModal
                isOpen={isUpdateModalOpen}
                onClose={() => setIsUpdateModalOpen(false)}
                onConfirm={handleUpdateStatus}
                complaintDetails={selectedComplaint}
            />

            <DeleteConfirmationDialog
                isOpen={isDeleteModalOpen}
                title="Delete request"
                itemName={`request #${selectedComplaint?.id}`}
                onClose={() => setIsDeleteModalOpen(false)}
                onConfirm={handleDelete}
                isDeleting={isProcessing}
            />
        </div>
    );
};

export default AdminComplaints;
