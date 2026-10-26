import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { MessageSquare, MessageSquarePlus } from 'lucide-react';
import { notifySuccess, errorMessage, notifyError } from '../utils/notify';
import PageHeader from '../components/ui/PageHeader';
import Button from '../components/ui/Button';
import { EmptyState, Skeleton, Tabs } from '../components/ui/Feedback';
import TicketCard from '../components/helpdesk/TicketCard';
import NewRequestModal from '../components/helpdesk/NewRequestModal';
import { submitComplaint } from '../api/adminApi';
import { getComplaints } from '../api/complaintApi';

const OPEN_STATES = ['OPEN', 'IN_PROGRESS'];

const ResidentComplaints = () => {
    const [isLoading, setIsLoading] = useState(true);
    const [isFormOpen, setIsFormOpen] = useState(false);
    const [tab, setTab] = useState('open');
    const [complaints, setComplaints] = useState([]);

    const fetchComplaints = useCallback(async () => {
        setIsLoading(true);
        try {
            const data = await getComplaints();
            setComplaints((data || []).sort((a, b) => new Date(b.createdDate) - new Date(a.createdDate)));
        } catch (err) {
            notifyError('Could not load your requests. Please refresh the page.');
            setComplaints([]);
        } finally {
            setIsLoading(false);
        }
    }, []);

    useEffect(() => { fetchComplaints(); }, [fetchComplaints]);

    const open = useMemo(() => complaints.filter((c) => OPEN_STATES.includes(c.status)), [complaints]);
    const closed = useMemo(() => complaints.filter((c) => !OPEN_STATES.includes(c.status)), [complaints]);
    const list = tab === 'open' ? open : closed;

    const onSubmit = async (data) => {
        const formData = new FormData();
        formData.append('submissionType', data.submissionType);
        formData.append('category', data.submissionType === 'PERMISSION' ? 'Permission' : (data.category || 'General'));
        formData.append('description', `${data.title.trim()} - ${data.description.trim()}`);
        if (data.submissionType === 'PERMISSION') {
            formData.append('permissionCategory', data.permissionCategory);
            if (data.startDate) formData.append('startDate', data.startDate);
            if (data.endDate) formData.append('endDate', data.endDate);
        }
        if (data.file) formData.append('file', data.file);

        try {
            await submitComplaint(formData);
        } catch (err) {
            throw new Error(errorMessage(err, 'We could not send your request. Please try again.'));
        }
        notifySuccess('Request sent — the office will reply here');
        setTab('open');
        await fetchComplaints();
    };

    return (
        <div>
            <PageHeader
                title="Helpdesk"
                description="Report a problem, ask a question or request permission. Replies from the office appear on each request."
                actions={<Button icon={MessageSquarePlus} onClick={() => setIsFormOpen(true)}>New request</Button>}
            />

            <Tabs
                value={tab}
                onChange={setTab}
                label="Request status"
                tabs={[{ value: 'open', label: 'In progress', count: open.length }, { value: 'closed', label: 'Closed', count: closed.length }]}
            />

            {isLoading ? (
                <div className="ticket-list">{[0, 1].map((i) => <Skeleton key={i} height={130} style={{ borderRadius: 14 }} />)}</div>
            ) : list.length === 0 ? (
                <div className="card">
                    <EmptyState
                        icon={MessageSquare}
                        title={tab === 'open' ? 'No open requests' : 'Nothing closed yet'}
                        action={tab === 'open' && <Button size="sm" icon={MessageSquarePlus} onClick={() => setIsFormOpen(true)}>New request</Button>}
                    >
                        {tab === 'open' ? 'If something needs fixing or you have a question, the office is a message away.' : 'Resolved requests move here.'}
                    </EmptyState>
                </div>
            ) : (
                <div className="ticket-list">
                    {list.map((c) => <TicketCard key={c.id} ticket={c} />)}
                </div>
            )}

            <NewRequestModal isOpen={isFormOpen} onClose={() => setIsFormOpen(false)} onSubmit={onSubmit} />
        </div>
    );
};

export default ResidentComplaints;
