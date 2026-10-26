import React, { useState, useEffect, useMemo } from 'react';
import PageHeader from '../components/ui/PageHeader';
import DataTable from '../components/ui/DataTable';
import StatusChip from '../components/ui/StatusChip';
import Button from '../components/ui/Button';
import ConfirmDialog from '../components/ui/ConfirmDialog';
import Menu from '../components/ui/Menu';
import { Tabs } from '../components/ui/Feedback';
import { Ban, CheckCircle2, KeyRound, Pencil, ShieldCheck, ShieldPlus, UserMinus, UserPlus, UserRound, Users } from 'lucide-react';
import { notifyError, notifySuccess } from '../utils/notify';
import { initials } from '../layouts/PortalLayout';
import TemporaryPasswordDialog from '../components/TemporaryPasswordDialog';
import ResidentModal from '../components/ResidentModal';
import { useAuth } from '../context/AuthContext';

const ROLE_LABEL = { RESIDENT: 'Resident', ADMIN: 'Admin', MASTER_ADMIN: 'Master admin' };
const ROLE_TONE = { RESIDENT: 'neutral', ADMIN: 'info', MASTER_ADMIN: 'brand' };

const AdminResidents = () => {
    const { user } = useAuth();
    const isMasterAdmin = user?.role === 'MASTER_ADMIN';

    const [isResidentModalOpen, setIsResidentModalOpen] = useState(false);
    const [selectedResident, setSelectedResident] = useState(null);
    const [isProcessing, setIsProcessing] = useState(false);
    const [residents, setResidents] = useState([]);
    const [isLoading, setIsLoading] = useState(true);

    const [tab, setTab] = useState('all');
    // Feedback is shown as toasts; these keep the old call sites readable
    const setErrorMessage = (m) => { if (m) notifyError(m); };
    const setSuccessMessage = (m) => { if (m) notifySuccess(m); };
    const [temporaryPassword, setTemporaryPassword] = useState(null);

    const [confirmDialog, setConfirmDialog] = useState({
        isOpen: false,
        title: '',
        message: '',
        confirmText: 'Confirm',
        confirmVariant: 'primary',
        onConfirm: () => {}
    });

    const fetchResidents = async () => {
        setIsLoading(true);
        try {
            const { getResidents } = await import('../api/adminApi');
            const data = await getResidents();
            setResidents(data);
        } catch (err) {
            notifyError('Could not load the user list. Please refresh the page.');
            setResidents([]);
        } finally {
            setIsLoading(false);
        }
    };

    useEffect(() => {
        fetchResidents();
    }, []);

    const handleSaveResident = async (formData) => {
        setErrorMessage('');
        setSuccessMessage('');
        try {
            const { addResident, updateResident } = await import('../api/adminApi');
            if (selectedResident) {
                await updateResident(selectedResident.id, formData);
                setSuccessMessage("Details updated");
            } else {
                const created = await addResident(formData);
                setSuccessMessage(`${formData.name} added`);
                if (created?.temporaryPassword) {
                    setTemporaryPassword({ name: created.fullName || formData.name, password: created.temporaryPassword });
                }
            }
            await fetchResidents();
        } catch (err) {
            // The dialog stays open and shows this, so nothing typed is lost
            throw new Error(err.response?.data?.message || err.response?.data?.error || 'We could not save these details. Please try again.');
        }
    };

    const handleRoleChangePrompt = (row, newRole) => {
        if (!isMasterAdmin) return;
        if (row.role === newRole) return;
        setErrorMessage('');
        setSuccessMessage('');

        const targetName = row.fullName || row.name || row.email;
        const currentRole = row.role || 'RESIDENT';

        setConfirmDialog({
            isOpen: true,
            title: 'Change role',
            message: `${targetName} will change from ${ROLE_LABEL[currentRole] || currentRole} to ${ROLE_LABEL[newRole] || newRole}. Their access changes the next time they sign in.`,
            confirmText: `Make ${ROLE_LABEL[newRole] || newRole}`,
            confirmVariant: newRole === 'MASTER_ADMIN' ? 'danger' : 'primary',
            onConfirm: async () => {
                setIsProcessing(true);
                try {
                    const { updateUserRole } = await import('../api/adminApi');
                    await updateUserRole(row.id, newRole);
                    setSuccessMessage(`${targetName} is now ${ROLE_LABEL[newRole] || newRole}`);
                    setConfirmDialog(prev => ({ ...prev, isOpen: false }));
                    await fetchResidents();
                } catch (err) {
                    const msg = err.response?.data?.message || err.response?.data?.error || err.message || "Failed to update user role.";
                    setErrorMessage(msg);
                    setConfirmDialog(prev => ({ ...prev, isOpen: false }));
                } finally {
                    setIsProcessing(false);
                }
            }
        });
    };

    const handleStatusTogglePrompt = (row, newStatus) => {
        if (!isMasterAdmin) return;
        setErrorMessage('');
        setSuccessMessage('');

        const targetName = row.fullName || row.name || row.email;
        const isReactivate = newStatus === 'ACTIVE';
        const actionLabel = isReactivate ? 'Reactivate' : 'Block';

        setConfirmDialog({
            isOpen: true,
            title: `${actionLabel} account`,
            message: isReactivate ? `${targetName} will be able to sign in again.` : `${targetName} will be signed out and cannot sign in until you unblock them.`,
            confirmText: actionLabel,
            confirmVariant: isReactivate ? 'primary' : 'danger',
            onConfirm: async () => {
                setIsProcessing(true);
                try {
                    const { updateResidentStatus } = await import('../api/adminApi');
                    const updated = await updateResidentStatus(row.id, newStatus);
                    setSuccessMessage(isReactivate ? `${targetName} can sign in again` : `${targetName} is blocked`);
                    // Accounts that never finished first sign-in get a fresh temporary password on reactivation
                    if (updated?.temporaryPassword) {
                        setTemporaryPassword({ name: targetName, password: updated.temporaryPassword });
                    }
                    setConfirmDialog(prev => ({ ...prev, isOpen: false }));
                    await fetchResidents();
                } catch (err) {
                    const msg = err.response?.data?.message || err.response?.data?.error || err.message || `Failed to ${actionLabel.toLowerCase()} account.`;
                    setErrorMessage(msg);
                    setConfirmDialog(prev => ({ ...prev, isOpen: false }));
                } finally {
                    setIsProcessing(false);
                }
            }
        });
    };

    const handleResetPasswordPrompt = (row) => {
        setErrorMessage('');
        setSuccessMessage('');
        const targetName = row.fullName || row.name || row.email;
        setConfirmDialog({
            isOpen: true,
            title: 'Reset password',
            message: `Issue a new temporary password for ${targetName}? Their current password stops working and they are signed out everywhere.`,
            confirmText: 'Reset password',
            confirmVariant: 'danger',
            onConfirm: async () => {
                setIsProcessing(true);
                try {
                    const { resetUserPassword } = await import('../api/adminApi');
                    const result = await resetUserPassword(row.id);
                    setConfirmDialog(prev => ({ ...prev, isOpen: false }));
                    
                    setTemporaryPassword({ name: targetName, password: result.temporaryPassword });
                } catch (err) {
                    const msg = err.response?.data?.message || err.response?.data?.error || err.message || "Failed to reset password.";
                    setErrorMessage(msg);
                    setConfirmDialog(prev => ({ ...prev, isOpen: false }));
                } finally {
                    setIsProcessing(false);
                }
            }
        });
    };

    const handleDeactivatePrompt = (row) => {
        if (!isMasterAdmin) return;
        setErrorMessage('');
        setSuccessMessage('');

        const targetName = row.fullName || row.name || row.email;

        setConfirmDialog({
            isOpen: true,
            title: 'Deactivate account',
            message: `${targetName} will no longer be able to sign in. Their bills and history are kept, and you can reactivate them later.`,
            confirmText: 'Deactivate',
            confirmVariant: 'danger',
            onConfirm: async () => {
                setIsProcessing(true);
                try {
                    const { deleteResident } = await import('../api/adminApi');
                    await deleteResident(row.id);
                    setSuccessMessage(`${targetName} deactivated`);
                    setConfirmDialog(prev => ({ ...prev, isOpen: false }));
                    await fetchResidents();
                } catch (err) {
                    const msg = err.response?.data?.message || err.response?.data?.error || err.message || "Failed to deactivate account.";
                    setErrorMessage(msg);
                    setConfirmDialog(prev => ({ ...prev, isOpen: false }));
                } finally {
                    setIsProcessing(false);
                }
            }
        });
    };

    const counts = useMemo(() => ({
        all: residents.length,
        RESIDENT: residents.filter((r) => (r.role || 'RESIDENT') === 'RESIDENT').length,
        staff: residents.filter((r) => r.role === 'ADMIN' || r.role === 'MASTER_ADMIN').length,
        inactive: residents.filter((r) => r.active === false || r.status === 'BLOCKED').length,
    }), [residents]);

    const visible = useMemo(() => {
        if (tab === 'RESIDENT') return residents.filter((r) => (r.role || 'RESIDENT') === 'RESIDENT');
        if (tab === 'staff') return residents.filter((r) => r.role === 'ADMIN' || r.role === 'MASTER_ADMIN');
        if (tab === 'inactive') return residents.filter((r) => r.active === false || r.status === 'BLOCKED');
        return residents;
    }, [residents, tab]);

    const statusChip = (row) => {
        if (row.active === false) return <StatusChip tone="neutral" label="Deactivated" />;
        if (row.status === 'BLOCKED') return <StatusChip tone="danger" label="Blocked" />;
        if (row.forcePasswordChange) return <StatusChip tone="warning" label="Invited" />;
        return <StatusChip tone="success" label="Active" />;
    };

    const rowActions = (row) => {
        const role = row.role || 'RESIDENT';
        const isSelf = String(row.id) === String(user?.userId);
        const canReset = (isMasterAdmin || role === 'RESIDENT') && !isSelf;
        return [
            { label: 'Edit details', icon: Pencil, onClick: () => { setSelectedResident(row); setIsResidentModalOpen(true); } },
            { label: 'Reset password', icon: KeyRound, onClick: () => handleResetPasswordPrompt(row), hidden: !canReset },
            ...(isMasterAdmin && !isSelf ? [
                'separator',
                { label: 'Make resident', icon: UserRound, onClick: () => handleRoleChangePrompt(row, 'RESIDENT'), hidden: role === 'RESIDENT' },
                { label: 'Make admin', icon: ShieldCheck, onClick: () => handleRoleChangePrompt(row, 'ADMIN'), hidden: role === 'ADMIN' },
                { label: 'Make master admin', icon: ShieldPlus, onClick: () => handleRoleChangePrompt(row, 'MASTER_ADMIN'), hidden: role === 'MASTER_ADMIN' },
                'separator',
                row.status === 'BLOCKED' || row.active === false
                    ? { label: 'Reactivate', icon: CheckCircle2, onClick: () => handleStatusTogglePrompt(row, 'ACTIVE') }
                    : { label: 'Block sign-in', icon: Ban, onClick: () => handleStatusTogglePrompt(row, 'BLOCKED') },
                { label: 'Deactivate', icon: UserMinus, danger: true, onClick: () => handleDeactivatePrompt(row), hidden: row.active === false },
            ] : []),
        ];
    };

    const columns = [
        {
            header: 'Name',
            accessor: 'fullName',
            render: (row) => (
                <div className="row" style={{ '--gap': '0.7rem' }}>
                    <span className="avatar avatar-sm" aria-hidden="true">{initials(row.fullName || row.name || row.email)}</span>
                    <div style={{ minWidth: 0 }}>
                        <div className="cell-primary">{row.fullName || row.name || '—'}</div>
                        <div className="cell-sub">{row.email}</div>
                    </div>
                </div>
            ),
        },
        { header: 'Flat', accessor: 'roomNumber', render: (row) => (row.roomNumber ? <span className="fw-600 num">{row.roomNumber}</span> : <span className="text-subtle">—</span>) },
        { header: 'Phone', accessor: 'phoneNumber', render: (row) => <span className="num">{row.phoneNumber || row.phone || '—'}</span> },
        {
            header: 'Role',
            render: (row) => {
                const role = row.role || 'RESIDENT';
                return <span className={`chip chip-${ROLE_TONE[role]} no-dot`}>{ROLE_LABEL[role] || role}</span>;
            },
        },
        { header: 'Status', render: statusChip },
        {
            header: <span className="visually-hidden">Actions</span>,
            render: (row) => (
                <div className="row-actions">
                    <Button variant="ghost" size="sm" iconOnly icon={Pencil} aria-label={`Edit ${row.fullName || row.email}`} title="Edit details"
                        onClick={() => { setSelectedResident(row); setIsResidentModalOpen(true); }} />
                    <Menu label={`More actions for ${row.fullName || row.email}`} items={rowActions(row).slice(1)} />
                </div>
            ),
        },
    ];

    const tabs = [
        { value: 'all', label: 'Everyone', count: counts.all },
        { value: 'RESIDENT', label: 'Residents', count: counts.RESIDENT },
        { value: 'staff', label: 'Committee & admins', count: counts.staff },
        { value: 'inactive', label: 'Blocked or inactive', count: counts.inactive },
    ];

    return (
        <div>
            <PageHeader
                title={isMasterAdmin ? 'Users & roles' : 'Residents'}
                description={isMasterAdmin
                    ? 'Everyone with an account: add residents, reset passwords, and decide who can administer the society.'
                    : 'Residents with portal access. New residents sign in with a temporary password and set up their own account.'}
                actions={<Button icon={UserPlus} onClick={() => { setSelectedResident(null); setIsResidentModalOpen(true); }}>Add resident</Button>}
            />

            <Tabs tabs={tabs} value={tab} onChange={setTab} label="Filter people" />

            <DataTable
                columns={columns}
                data={visible}
                isLoading={isLoading}
                searchPlaceholder="Search name, email, flat or phone"
                emptyIcon={Users}
                emptyTitle="No one here"
                emptyMessage={tab === 'all' ? 'Add your first resident to give them portal access.' : 'Nobody matches this filter.'}
            />

            <ResidentModal
                isOpen={isResidentModalOpen}
                onClose={() => setIsResidentModalOpen(false)}
                onSave={handleSaveResident}
                initialData={selectedResident}
            />

            <ConfirmDialog
                isOpen={confirmDialog.isOpen}
                title={confirmDialog.title}
                message={confirmDialog.message}
                confirmText={confirmDialog.confirmText}
                confirmVariant={confirmDialog.confirmVariant}
                onConfirm={confirmDialog.onConfirm}
                onCancel={() => setConfirmDialog(prev => ({ ...prev, isOpen: false }))}
                isLoading={isProcessing}
            />

            <TemporaryPasswordDialog info={temporaryPassword} onClose={() => setTemporaryPassword(null)} />
        </div>
    );
};

export default AdminResidents;
