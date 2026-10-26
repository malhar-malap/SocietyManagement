import React, { useState, useEffect } from 'react';
import { notifySuccess, notifyError, errorMessage } from '../utils/notify';
import Card from '../components/ui/Card';
import Button from '../components/ui/Button';
import { TextInput } from '../components/ui/Field';
import { EmptyState, Skeleton } from '../components/ui/Feedback';
import StatusChip from '../components/ui/StatusChip';
import PasswordChecklist from '../components/auth/PasswordChecklist';
import { useAuth } from '../context/AuthContext';
import { initials } from '../layouts/PortalLayout';
import { passwordPolicyError } from '../utils/passwordPolicy';
import { formatDate, formatMoney } from '../utils/format';
import { AtSign, BadgeCheck, Building2, Download, KeyRound, Pencil, Phone, UserRound } from 'lucide-react';
import api from '../api/axiosConfig';
import { downloadAuthenticatedFile } from '../api/fileUtils';

const ResidentProfile = () => {
    const { user, login } = useAuth();
    const [profile, setProfile] = useState(null);
    const [myRooms, setMyRooms] = useState([]);
    const [isLoading, setIsLoading] = useState(true);

    // Edit Profile State
    const [isEditingProfile, setIsEditingProfile] = useState(false);
    const [editName, setEditName] = useState('');
    const [editPhone, setEditPhone] = useState('');
    const [isSavingProfile, setIsSavingProfile] = useState(false);

    // Change Password State
    const [oldPassword, setOldPassword] = useState('');
    const [newPassword, setNewPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [isChangingPassword, setIsChangingPassword] = useState(false);

    useEffect(() => {
        const fetchProfile = async () => {
            if (!user?.userId) return;
            try {
                const { getResidentProfile } = await import('../api/residentApi');
                const [data, roomsRes] = await Promise.all([
                    getResidentProfile(user.userId),
                    api.get('/rooms/my-rooms').catch(() => ({ data: [] }))
                ]);
                const loadedName = data.fullName || data.name || user?.name || '';
                const loadedPhone = data.phoneNumber || data.phone || '';
                setProfile({
                    name: loadedName,
                    email: data.email,
                    phone: loadedPhone || 'Not Provided',
                    room: data.roomNumber || 'Unassigned',
                    isEmailVerified: data.status === 'ACTIVE'
                });
                setEditName(loadedName);
                setEditPhone(loadedPhone);
                setMyRooms(roomsRes.data || []);
            } catch (err) {
                notifyError('Could not load your profile. Please refresh the page.');
            } finally {
                setIsLoading(false);
            }
        };
        fetchProfile();
    }, [user]);

    if (isLoading) {
        return (
            <div className="stack" aria-busy="true">
                <Skeleton height={120} style={{ borderRadius: 14 }} />
                <div className="grid grid-2"><Skeleton height={260} style={{ borderRadius: 14 }} /><Skeleton height={260} style={{ borderRadius: 14 }} /></div>
            </div>
        );
    }
    if (!profile) return <div className="card"><EmptyState icon={UserRound} title="Profile unavailable">Please refresh the page or sign in again.</EmptyState></div>;

    const handleProfileSave = async (e) => {
        e.preventDefault();
        if (!editName.trim()) {
            notifyError('Your name cannot be empty.');
            return;
        }
        setIsSavingProfile(true);
        try {
            const { updateResidentProfile } = await import('../api/residentApi');
            const updated = await updateResidentProfile(user.userId, {
                name: editName.trim(),
                phone: editPhone.trim()
            });
            const updatedName = updated.name || editName.trim();
            const updatedPhone = updated.phone || editPhone.trim();

            setProfile(prev => ({
                ...prev,
                name: updatedName,
                phone: updatedPhone || 'Not Provided'
            }));

            // Sync with global auth state and localStorage
            login({
                ...user,
                name: updatedName
            });

            setIsEditingProfile(false);
            notifySuccess('Your details are saved');
        } catch (err) {
            notifyError(errorMessage(err, 'We could not save your details. Please try again.'));
        } finally {
            setIsSavingProfile(false);
        }
    };

    const handlePasswordChange = async (e) => {
        e.preventDefault();
        const policy = passwordPolicyError(newPassword);
        if (policy) { notifyError(policy); return; }
        if (newPassword !== confirmPassword) {
            notifyError('The new passwords do not match.');
            return;
        }
        try {
            setIsChangingPassword(true);
            const { changePassword } = await import('../api/residentApi');
            const res = await changePassword(user.userId, { oldPassword, newPassword });
            // Changing the password revokes old tokens; keep this session alive with the fresh pair
            if (res && res.accessToken) {
                login(res);
            }
            notifySuccess('Password changed');
            setOldPassword('');
            setNewPassword('');
            setConfirmPassword('');
        } catch (error) {
            notifyError(errorMessage(error, 'We could not change your password. Check your current password and try again.'));
        } finally {
            setIsChangingPassword(false);
        }
    };

    const handleDownloadCertificate = (roomId, roomNumber) => {
        downloadAuthenticatedFile(`/rooms/${roomId}/share-certificate/download`, `share_certificate_${roomNumber}.pdf`);
    };

    const cancelEdit = () => {
        setIsEditingProfile(false);
        setEditName(profile.name);
        setEditPhone(profile.phone === 'Not Provided' ? '' : profile.phone);
    };

    return (
        <div className="stack stack-lg">
            <section className="profile-hero card">
                <span className="avatar avatar-lg" aria-hidden="true">{initials(profile.name)}</span>
                <div className="grow">
                    <h1 className="profile-name">{profile.name}</h1>
                    <div className="row row-wrap text-sm text-muted" style={{ '--gap': '0.4rem 1rem' }}>
                        <span className="row" style={{ '--gap': '0.3rem' }}><Building2 size={14} /> {profile.room}</span>
                        <span className="row" style={{ '--gap': '0.3rem' }}><AtSign size={14} /> {profile.email}</span>
                        {profile.isEmailVerified && <span className="chip chip-success no-dot"><BadgeCheck size={12} /> Verified</span>}
                    </div>
                </div>
            </section>

            <div className="profile-grid">
                <Card
                    title="Personal details"
                    subtitle="Shown to the society office and committee."
                    action={!isEditingProfile && <Button variant="secondary" size="sm" icon={Pencil} onClick={() => setIsEditingProfile(true)}>Edit</Button>}
                >
                    {!isEditingProfile ? (
                        <dl className="detail-list detail-list-stack">
                            <div><dt>Full name</dt><dd>{profile.name}</dd></div>
                            <div><dt>Mobile</dt><dd>{profile.phone}</dd></div>
                            <div><dt>Email</dt><dd>{profile.email}</dd></div>
                        </dl>
                    ) : (
                        <form onSubmit={handleProfileSave} className="stack" noValidate>
                            <TextInput label="Full name" icon={UserRound} value={editName} onChange={(e) => setEditName(e.target.value)} autoFocus />
                            <TextInput label="Mobile" icon={Phone} value={editPhone} onChange={(e) => setEditPhone(e.target.value)} placeholder="98765 43210" />
                            <p className="text-xs text-subtle">To change your sign-in email, contact the society office.</p>
                            <div className="row row-end">
                                <Button variant="ghost" onClick={cancelEdit} disabled={isSavingProfile}>Cancel</Button>
                                <Button type="submit" isLoading={isSavingProfile}>Save</Button>
                            </div>
                        </form>
                    )}
                </Card>

                <Card title="Change password" subtitle="You will stay signed in on this device.">
                    <form onSubmit={handlePasswordChange} className="stack" noValidate>
                        <TextInput type="password" label="Current password" icon={KeyRound} autoComplete="current-password" value={oldPassword} onChange={(e) => setOldPassword(e.target.value)} />
                        <TextInput type="password" label="New password" autoComplete="new-password" value={newPassword} onChange={(e) => setNewPassword(e.target.value)} />
                        {newPassword && <PasswordChecklist password={newPassword} />}
                        <TextInput type="password" label="Confirm new password" autoComplete="new-password" value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)}
                            error={confirmPassword && confirmPassword !== newPassword ? 'Passwords do not match.' : undefined} />
                        <div className="row row-end">
                            <Button type="submit" isLoading={isChangingPassword} disabled={!oldPassword || !newPassword || !confirmPassword}>Update password</Button>
                        </div>
                    </form>
                </Card>

                <Card title="My home" subtitle="Units linked to your account" className="span-all">
                    {myRooms.length === 0 ? (
                        <EmptyState compact icon={Building2} title="No unit linked yet">Ask the society office to link your flat to your account.</EmptyState>
                    ) : (
                        <div className="unit-cards">
                            {myRooms.map((r) => (
                                <div key={r.id} className="unit-card">
                                    <div className="row row-between">
                                        <span className="unit-badge">{r.roomNumber}</span>
                                        <span className="tag">{(r.unitCategory || 'RESIDENTIAL').toLowerCase().replace(/^./, (c) => c.toUpperCase())}</span>
                                    </div>
                                    <dl className="kv">
                                        <dt>Location</dt><dd>Wing {r.wing} · Floor {r.floor}</dd>
                                        <dt>Share certificate</dt><dd>{r.shareCertificateNumber || <span className="text-subtle">Not issued yet</span>}</dd>
                                        {r.shareCertificateIssuedDate && <><dt>Issued on</dt><dd>{formatDate(r.shareCertificateIssuedDate)}</dd></>}
                                        {r.nocApplicable && <><dt>NOC charge</dt><dd>{formatMoney(r.nocAmount, 2)} / month</dd></>}
                                    </dl>
                                    {(r.shareCertificateData || r.shareCertificateNumber) ? (
                                        <Button variant="secondary" size="sm" icon={Download} onClick={() => handleDownloadCertificate(r.id, r.roomNumber)}>Download certificate</Button>
                                    ) : <StatusChip tone="neutral" label="Certificate pending" />}
                                </div>
                            ))}
                        </div>
                    )}
                </Card>
            </div>
        </div>
    );
};

export default ResidentProfile;
