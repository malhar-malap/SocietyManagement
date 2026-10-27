import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { CalendarClock, Megaphone, Pencil, Plus, Trash2 } from 'lucide-react';
import { notifyError, notifySuccess, errorMessage } from '../utils/notify';
import PageHeader from '../components/ui/PageHeader';
import StatusChip from '../components/ui/StatusChip';
import Button from '../components/ui/Button';
import { EmptyState, Skeleton, Tabs } from '../components/ui/Feedback';
import AnnouncementModal from '../components/AnnouncementModal';
import { useAuth } from '../context/AuthContext';
import { getAllAnnouncements, createAnnouncement, updateAnnouncement, deleteAnnouncement } from '../api/adminApi';
import { formatDate } from '../utils/format';
import { deleteWithUndo } from '../utils/undo';

const isExpired = (a) => a.expiryDate && new Date(a.expiryDate) < new Date();

const AdminAnnouncements = () => {
    const { user } = useAuth();
    const isMasterAdmin = user?.role === 'MASTER_ADMIN';
    const [announcements, setAnnouncements] = useState([]);
    const [isLoading, setIsLoading] = useState(true);
    const [tab, setTab] = useState('active');
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [selectedAnnouncement, setSelectedAnnouncement] = useState(null);

    const fetchAnnouncements = useCallback(async () => {
        setIsLoading(true);
        try {
            const data = await getAllAnnouncements();
            setAnnouncements((data || []).sort((a, b) => new Date(b.createdDate || 0) - new Date(a.createdDate || 0)));
        } catch (err) {
            notifyError('Could not load announcements. Please try again.');
            setAnnouncements([]);
        } finally {
            setIsLoading(false);
        }
    }, []);

    useEffect(() => { fetchAnnouncements(); }, [fetchAnnouncements]);

    const handleSaveAnnouncement = async (formData) => {
        try {
            if (selectedAnnouncement) await updateAnnouncement(selectedAnnouncement.id, formData);
            else await createAnnouncement(formData);
        } catch (err) {
            throw new Error(errorMessage(err, 'We could not save this notice. Please try again.'));
        }
        notifySuccess(selectedAnnouncement ? 'Notice updated' : 'Notice published to all residents');
        await fetchAnnouncements();
        setIsModalOpen(false);
    };

    // No confirm dialog: the notice disappears straight away and the toast offers Undo for a few seconds
    const handleDelete = (notice) => {
        deleteWithUndo({
            message: `“${notice.title}” deleted`,
            hide: () => setAnnouncements((list) => list.filter((a) => a.id !== notice.id)),
            restore: () => setAnnouncements((list) => (list.some((a) => a.id === notice.id) ? list
                : [...list, notice].sort((a, b) => new Date(b.createdDate || 0) - new Date(a.createdDate || 0)))),
            commit: () => deleteAnnouncement(notice.id),
            onError: (e) => notifyError(errorMessage(e, 'We could not delete this notice, so it is back on the board.')),
        });
    };

    const active = useMemo(() => announcements.filter((a) => !isExpired(a)), [announcements]);
    const expired = useMemo(() => announcements.filter(isExpired), [announcements]);
    const list = tab === 'active' ? active : expired;

    const openNew = () => { setSelectedAnnouncement(null); setIsModalOpen(true); };

    return (
        <div>
            <PageHeader
                title="Announcements"
                description={isMasterAdmin
                    ? 'Notices appear on every resident’s home screen and in their notifications.'
                    : 'Notices shown to residents. Only the master administrator can post or edit them.'}
                actions={isMasterAdmin && <Button icon={Plus} onClick={openNew}>New notice</Button>}
            />

            <Tabs
                tabs={[{ value: 'active', label: 'Live', count: active.length }, { value: 'expired', label: 'Expired', count: expired.length }]}
                value={tab}
                onChange={setTab}
                label="Notice status"
            />

            {isLoading ? (
                <div className="notice-cards">{[0, 1, 2].map((i) => <Skeleton key={i} height={180} style={{ borderRadius: 14 }} />)}</div>
            ) : list.length === 0 ? (
                <div className="card">
                    <EmptyState icon={Megaphone} title={tab === 'active' ? 'No live notices' : 'Nothing has expired yet'}
                        action={tab === 'active' && isMasterAdmin && <Button size="sm" icon={Plus} onClick={openNew}>Post a notice</Button>}>
                        {tab === 'active' ? 'Post a notice to tell residents about maintenance, meetings or events.' : 'Notices move here after their end date.'}
                    </EmptyState>
                </div>
            ) : (
                <div className="notice-cards">
                    {list.map((a) => (
                        <article key={a.id} className={`notice-card ${isExpired(a) ? 'is-expired' : ''}`}>
                            <div className="row row-between">
                                <span className="notice-date">{formatDate(a.createdDate)}</span>
                                <StatusChip tone={isExpired(a) ? 'neutral' : 'success'} label={isExpired(a) ? 'Expired' : 'Live'} />
                            </div>
                            <h3>{a.title}</h3>
                            <p>{a.description}</p>
                            <footer>
                                <span className="text-xs text-subtle row" style={{ '--gap': '0.3rem' }}>
                                    <CalendarClock size={13} /> {a.expiryDate ? `Until ${formatDate(a.expiryDate)}` : 'No end date'}
                                </span>
                                {isMasterAdmin && (
                                    <span className="row" style={{ '--gap': '0.2rem' }}>
                                        <Button variant="ghost" size="sm" iconOnly icon={Pencil} aria-label={`Edit ${a.title}`} title="Edit"
                                            onClick={() => { setSelectedAnnouncement(a); setIsModalOpen(true); }} />
                                        <Button variant="danger-ghost" size="sm" iconOnly icon={Trash2} aria-label={`Delete ${a.title}`} title="Delete"
                                            onClick={() => handleDelete(a)} />
                                    </span>
                                )}
                            </footer>
                        </article>
                    ))}
                </div>
            )}

            <AnnouncementModal
                isOpen={isModalOpen}
                onClose={() => setIsModalOpen(false)}
                onSave={handleSaveAnnouncement}
                initialData={selectedAnnouncement}
            />

        </div>
    );
};

export default AdminAnnouncements;
