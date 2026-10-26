import React, { useState, useEffect } from 'react';
import PageHeader from '../components/ui/PageHeader';
import DataTable from '../components/ui/DataTable';
import StatusChip from '../components/ui/StatusChip';
import Button from '../components/ui/Button';
import ConfirmDialog from '../components/ui/ConfirmDialog';
import { Plus, Edit, Trash2, Ban, CheckCircle2, ShieldAlert, CheckCircle, KeyRound } from 'lucide-react';
import TemporaryPasswordDialog from '../components/TemporaryPasswordDialog';
import ResidentModal from '../components/ResidentModal';
import { useAuth } from '../context/AuthContext';

const AdminResidents = () => {
    const { user } = useAuth();
    const isMasterAdmin = user?.role === 'MASTER_ADMIN';

    const [isResidentModalOpen, setIsResidentModalOpen] = useState(false);
    const [selectedResident, setSelectedResident] = useState(null);
    const [isProcessing, setIsProcessing] = useState(false);
    const [residents, setResidents] = useState([]);
    const [isLoading, setIsLoading] = useState(true);

    const [errorMessage, setErrorMessage] = useState('');
    const [successMessage, setSuccessMessage] = useState('');
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
            console.error("Failed to fetch residents", err);
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
                setSuccessMessage("Resident updated successfully.");
            } else {
                const created = await addResident(formData);
                setSuccessMessage("Resident added successfully.");
                if (created?.temporaryPassword) {
                    setTemporaryPassword({ name: created.fullName || formData.name, password: created.temporaryPassword });
                }
            }
            await fetchResidents();
        } catch (err) {
            console.error(err);
            const msg = err.response?.data?.message || err.response?.data?.error || err.message || "Failed to save resident.";
            setErrorMessage(msg);
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
            title: 'Change User Role',
            message: `Are you sure you want to change the role of "${targetName}" from ${currentRole} to ${newRole}?`,
            confirmText: 'Change Role',
            confirmVariant: newRole === 'MASTER_ADMIN' ? 'danger' : 'primary',
            onConfirm: async () => {
                setIsProcessing(true);
                try {
                    const { updateUserRole } = await import('../api/adminApi');
                    await updateUserRole(row.id, newRole);
                    setSuccessMessage(`Role successfully updated to ${newRole} for "${targetName}".`);
                    setConfirmDialog(prev => ({ ...prev, isOpen: false }));
                    await fetchResidents();
                } catch (err) {
                    console.error("Failed to update role", err);
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
            title: `${actionLabel} Account`,
            message: `Are you sure you want to ${actionLabel.toLowerCase()} the account for "${targetName}"?`,
            confirmText: actionLabel,
            confirmVariant: isReactivate ? 'primary' : 'danger',
            onConfirm: async () => {
                setIsProcessing(true);
                try {
                    const { updateResidentStatus } = await import('../api/adminApi');
                    const updated = await updateResidentStatus(row.id, newStatus);
                    setSuccessMessage(`Account ${actionLabel.toLowerCase()}d successfully for "${targetName}".`);
                    // Accounts that never finished first sign-in get a fresh temporary password on reactivation
                    if (updated?.temporaryPassword) {
                        setTemporaryPassword({ name: targetName, password: updated.temporaryPassword });
                    }
                    setConfirmDialog(prev => ({ ...prev, isOpen: false }));
                    await fetchResidents();
                } catch (err) {
                    console.error("Failed to update status", err);
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
            title: 'Reset Password',
            message: `Issue a new temporary password for "${targetName}"? Their current password stops working and they will be signed out everywhere.`,
            confirmText: 'Reset Password',
            confirmVariant: 'danger',
            onConfirm: async () => {
                setIsProcessing(true);
                try {
                    const { resetUserPassword } = await import('../api/adminApi');
                    const result = await resetUserPassword(row.id);
                    setConfirmDialog(prev => ({ ...prev, isOpen: false }));
                    setSuccessMessage(`Temporary password issued for "${targetName}".`);
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
            title: 'Deactivate Account',
            message: `Are you sure you want to deactivate "${targetName}"? The account will be marked inactive and the user will be unable to log in.`,
            confirmText: 'Deactivate',
            confirmVariant: 'danger',
            onConfirm: async () => {
                setIsProcessing(true);
                try {
                    const { deleteResident } = await import('../api/adminApi');
                    await deleteResident(row.id);
                    setSuccessMessage(`Account deactivated successfully for "${targetName}".`);
                    setConfirmDialog(prev => ({ ...prev, isOpen: false }));
                    await fetchResidents();
                } catch (err) {
                    console.error("Failed to deactivate user", err);
                    const msg = err.response?.data?.message || err.response?.data?.error || err.message || "Failed to deactivate account.";
                    setErrorMessage(msg);
                    setConfirmDialog(prev => ({ ...prev, isOpen: false }));
                } finally {
                    setIsProcessing(false);
                }
            }
        });
    };

    const getRoleBadgeStyle = (role) => {
        switch (role) {
            case 'MASTER_ADMIN':
                return {
                    backgroundColor: '#f3e8ff',
                    color: '#6b21a8',
                    border: '1px solid #d8b4fe',
                    padding: '0.2rem 0.55rem',
                    borderRadius: '9999px',
                    fontSize: '0.75rem',
                    fontWeight: '600',
                    display: 'inline-block'
                };
            case 'ADMIN':
                return {
                    backgroundColor: '#e0f2fe',
                    color: '#0369a1',
                    border: '1px solid #7dd3fc',
                    padding: '0.2rem 0.55rem',
                    borderRadius: '9999px',
                    fontSize: '0.75rem',
                    fontWeight: '600',
                    display: 'inline-block'
                };
            default:
                return {
                    backgroundColor: '#f1f5f9',
                    color: '#475569',
                    border: '1px solid #cbd5e1',
                    padding: '0.2rem 0.55rem',
                    borderRadius: '9999px',
                    fontSize: '0.75rem',
                    fontWeight: '500',
                    display: 'inline-block'
                };
        }
    };

    const columns = [
        { header: 'Room', accessor: 'roomNumber', render: (row) => row.roomNumber || '—' },
        { header: 'Name', accessor: 'fullName', render: (row) => row.fullName || row.name || '—' },
        { header: 'Email', accessor: 'email' },
        { header: 'Phone', accessor: 'phoneNumber', render: (row) => row.phoneNumber || row.phone || '—' },
        {
            header: 'Role',
            render: (row) => {
                const role = row.role || 'RESIDENT';
                const roleLabel = role === 'MASTER_ADMIN' ? 'Master Admin' : role === 'ADMIN' ? 'Admin' : 'Resident';

                if (!isMasterAdmin) {
                    return <span style={getRoleBadgeStyle(role)}>{roleLabel}</span>;
                }

                return (
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', flexWrap: 'wrap' }}>
                        <span style={getRoleBadgeStyle(role)}>{roleLabel}</span>
                        <select
                            value={role}
                            disabled={isProcessing}
                            onChange={(e) => handleRoleChangePrompt(row, e.target.value)}
                            style={{
                                padding: '0.2rem 0.4rem',
                                fontSize: '0.75rem',
                                borderRadius: '0.25rem',
                                border: '1px solid #cbd5e1',
                                backgroundColor: '#ffffff',
                                cursor: 'pointer',
                                color: '#1e293b'
                            }}
                            title="Change user role"
                            aria-label={`Change role for ${row.fullName || row.email}`}
                        >
                            <option value="RESIDENT">Resident</option>
                            <option value="ADMIN">Admin</option>
                            <option value="MASTER_ADMIN">Master Admin</option>
                        </select>
                    </div>
                );
            }
        },
        {
            header: 'Status',
            render: (row) => {
                if (row.active === false) {
                    return <StatusChip status="Deactivated" />;
                }
                return <StatusChip status={row.status || 'Active'} />;
            }
        },
        {
            header: 'Actions',
            render: (row) => (
                <div style={{ display: 'flex', gap: '0.4rem', alignItems: 'center' }}>
                    <Button
                        variant="secondary"
                        title="Edit details"
                        style={{ padding: '0.4rem 0.6rem' }}
                        onClick={() => { setSelectedResident(row); setIsResidentModalOpen(true); }}
                    >
                        <Edit size={16} />
                    </Button>

                    {/* Admins may reset residents; a Master Admin may reset anyone */}
                    {(isMasterAdmin || (row.role || 'RESIDENT') === 'RESIDENT') && String(row.id) !== String(user?.userId) && (
                        <Button
                            variant="secondary"
                            title="Reset password"
                            aria-label="Reset password"
                            style={{ padding: '0.4rem 0.6rem' }}
                            onClick={() => handleResetPasswordPrompt(row)}
                        >
                            <KeyRound size={16} />
                        </Button>
                    )}

                    {isMasterAdmin && (
                        <>
                            {/* Block / Reactivate Status Button */}
                            {row.status === 'BLOCKED' ? (
                                <Button
                                    variant="secondary"
                                    title="Reactivate (Unblock) Account"
                                    style={{ padding: '0.4rem 0.6rem' }}
                                    onClick={() => handleStatusTogglePrompt(row, 'ACTIVE')}
                                >
                                    <CheckCircle2 size={16} className="text-success" />
                                </Button>
                            ) : (
                                row.active !== false && (
                                    <Button
                                        variant="secondary"
                                        title="Block Account"
                                        style={{ padding: '0.4rem 0.6rem' }}
                                        onClick={() => handleStatusTogglePrompt(row, 'BLOCKED')}
                                    >
                                        <Ban size={16} className="text-warning" />
                                    </Button>
                                )
                            )}

                            {/* Deactivate / Reactivate Account Button */}
                            {row.active === false ? (
                                <Button
                                    variant="secondary"
                                    title="Reactivate Deactivated Account"
                                    style={{ padding: '0.4rem 0.6rem', color: '#16a34a' }}
                                    onClick={() => handleStatusTogglePrompt(row, 'ACTIVE')}
                                >
                                    <CheckCircle2 size={16} className="text-success" />
                                </Button>
                            ) : (
                                <Button
                                    variant="secondary"
                                    title="Deactivate Account"
                                    style={{ padding: '0.4rem 0.6rem' }}
                                    onClick={() => handleDeactivatePrompt(row)}
                                >
                                    <Trash2 size={16} className="text-danger" />
                                </Button>
                            )}
                        </>
                    )}
                </div>
            )
        }
    ];

    return (
        <div>
            <PageHeader
                title={isMasterAdmin ? "Manage Users & Roles" : "Manage Residents"}
                breadcrumbs={['Admin', isMasterAdmin ? 'User Management' : 'Residents']}
                actionButton={
                    <div style={{ display: 'flex', gap: '1rem' }}>
                        <Button onClick={() => { setSelectedResident(null); setIsResidentModalOpen(true); }}>
                            <Plus size={18} /> Add Resident
                        </Button>
                    </div>
                }
            />

            {errorMessage && (
                <div style={{
                    padding: '0.85rem 1.25rem',
                    backgroundColor: '#fef2f2',
                    border: '1px solid #f87171',
                    color: '#991b1b',
                    borderRadius: '0.5rem',
                    marginBottom: '1.25rem',
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    boxShadow: '0 1px 3px 0 rgba(0, 0, 0, 0.05)'
                }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
                        <ShieldAlert size={20} color="#b91c1c" />
                        <span style={{ fontSize: '0.9rem' }}><strong>Safeguard Alert:</strong> {errorMessage}</span>
                    </div>
                    <button
                        onClick={() => setErrorMessage('')}
                        style={{ background: 'none', border: 'none', cursor: 'pointer', fontSize: '1.25rem', color: '#991b1b', lineHeight: 1 }}
                        aria-label="Dismiss alert"
                    >
                        ×
                    </button>
                </div>
            )}

            {successMessage && (
                <div style={{
                    padding: '0.85rem 1.25rem',
                    backgroundColor: '#f0fdf4',
                    border: '1px solid #4ade80',
                    color: '#166534',
                    borderRadius: '0.5rem',
                    marginBottom: '1.25rem',
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    boxShadow: '0 1px 3px 0 rgba(0, 0, 0, 0.05)'
                }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
                        <CheckCircle size={20} color="#16a34a" />
                        <span style={{ fontSize: '0.9rem' }}>{successMessage}</span>
                    </div>
                    <button
                        onClick={() => setSuccessMessage('')}
                        style={{ background: 'none', border: 'none', cursor: 'pointer', fontSize: '1.25rem', color: '#166534', lineHeight: 1 }}
                        aria-label="Dismiss alert"
                    >
                        ×
                    </button>
                </div>
            )}

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
