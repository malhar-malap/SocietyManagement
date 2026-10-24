import React, { useState, useEffect } from 'react';
import PageHeader from '../components/ui/PageHeader';
import Card from '../components/ui/Card';
import Button from '../components/ui/Button';
import Input from '../components/ui/Input';
import { useAuth } from '../context/AuthContext';
import { Mail, Phone, Home, ShieldCheck, Download, User as UserIcon, Edit2, CheckCircle2 } from 'lucide-react';
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
    const [profileSuccessMsg, setProfileSuccessMsg] = useState('');

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
                console.error("Failed to fetch profile", err);
            } finally {
                setIsLoading(false);
            }
        };
        fetchProfile();
    }, [user]);

    if (isLoading) return <div style={{ padding: '2rem', textAlign: 'center' }}>Loading profile...</div>;
    if (!profile) return <div style={{ padding: '2rem', textAlign: 'center' }}>Profile not found.</div>;

    const handleProfileSave = async (e) => {
        e.preventDefault();
        if (!editName.trim()) {
            alert('Name cannot be empty');
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
            setProfileSuccessMsg('Profile updated successfully! Dashboard greeting updated.');
            setTimeout(() => setProfileSuccessMsg(''), 4000);
        } catch (err) {
            console.error('Failed to update profile', err);
            alert('Failed to update profile: ' + (err.response?.data?.message || err.message));
        } finally {
            setIsSavingProfile(false);
        }
    };

    const handlePasswordChange = async (e) => {
        e.preventDefault();
        if (newPassword !== confirmPassword) {
            alert('New passwords do not match');
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
            alert('Password successfully updated');
            setOldPassword('');
            setNewPassword('');
            setConfirmPassword('');
        } catch (error) {
            console.error(error);
            alert('Failed to change password. Ensure old password is correct.');
        } finally {
            setIsChangingPassword(false);
        }
    };

    const handleDownloadCertificate = (roomId, roomNumber) => {
        downloadAuthenticatedFile(`/rooms/${roomId}/share-certificate/download`, `share_certificate_${roomNumber}.pdf`);
    };

    return (
        <div>
            <PageHeader
                title="My Profile"
                breadcrumbs={['Resident', 'Profile']}
            />

            {profileSuccessMsg && (
                <div style={{ padding: '0.75rem 1rem', background: '#dcfce7', color: '#166534', borderRadius: 8, marginBottom: '1.5rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    <CheckCircle2 size={18} /> {profileSuccessMsg}
                </div>
            )}

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(350px, 1fr))', gap: '2rem' }}>

                {/* Personal Details & Edit Profile */}
                <Card
                    title="Personal Details"
                    action={
                        !isEditingProfile && (
                            <Button
                                variant="secondary"
                                style={{ padding: '0.35rem 0.75rem', fontSize: '0.85rem' }}
                                onClick={() => setIsEditingProfile(true)}
                            >
                                <Edit2 size={14} /> Edit Profile
                            </Button>
                        )
                    }
                >
                    {!isEditingProfile ? (
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                                <UserIcon className="text-secondary-light" />
                                <div>
                                    <p className="input-label" style={{margin: 0}}>Full Name</p>
                                    <p style={{margin: 0, fontWeight: 600, fontSize: '1.05rem', color: 'var(--primary)'}}>
                                        {profile.name}
                                    </p>
                                </div>
                            </div>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                                <Home className="text-secondary-light" />
                                <div>
                                    <p className="input-label" style={{margin: 0}}>Assigned Room(s)</p>
                                    <p style={{margin: 0, fontWeight: 500}}>{profile.room}</p>
                                </div>
                            </div>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                                <Mail className="text-secondary-light" />
                                <div>
                                    <p className="input-label" style={{margin: 0}}>Email Address</p>
                                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                                        <p style={{margin: 0, fontWeight: 500}}>{profile.email}</p>
                                        {profile.isEmailVerified && <ShieldCheck size={16} className="text-success" title="Verified" />}
                                    </div>
                                </div>
                            </div>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                                <Phone className="text-secondary-light" />
                                <div>
                                    <p className="input-label" style={{margin: 0}}>Phone Number</p>
                                    <p style={{margin: 0, fontWeight: 500}}>{profile.phone}</p>
                                </div>
                            </div>
                        </div>
                    ) : (
                        <form onSubmit={handleProfileSave} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                            <Input
                                label="Full Name (Displayed on Dashboard)"
                                value={editName}
                                onChange={(e) => setEditName(e.target.value)}
                                placeholder="Enter your full name"
                                required
                            />
                            <Input
                                label="Phone Number"
                                value={editPhone}
                                onChange={(e) => setEditPhone(e.target.value)}
                                placeholder="Enter your phone number"
                            />
                            <div style={{ display: 'flex', gap: '0.75rem', marginTop: '0.5rem' }}>
                                <Button type="submit" isLoading={isSavingProfile}>
                                    Save Changes
                                </Button>
                                <Button
                                    type="button"
                                    variant="secondary"
                                    onClick={() => {
                                        setIsEditingProfile(false);
                                        setEditName(profile.name);
                                        setEditPhone(profile.phone === 'Not Provided' ? '' : profile.phone);
                                    }}
                                >
                                    Cancel
                                </Button>
                            </div>
                        </form>
                    )}
                </Card>

                {/* Units & Share Certificates */}
                <Card title="Units & Share Certificates">
                    {myRooms.length === 0 ? (
                        <p style={{ color: 'var(--text-secondary-light)', fontSize: '0.875rem' }}>No linked units found.</p>
                    ) : (
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                            {myRooms.map(r => (
                                <div key={r.id} style={{ background: '#f8fafc', padding: '0.875rem', borderRadius: 8, border: '1px solid var(--border-light)' }}>
                                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.4rem' }}>
                                        <span style={{ fontWeight: 700, fontSize: '0.95rem' }}>Room {r.roomNumber} ({r.wing} Wing, Floor {r.floor})</span>
                                        <span style={{ fontSize: '0.75rem', padding: '0.15rem 0.5rem', borderRadius: 4, background: '#e0e7ff', color: '#3730a3', fontWeight: 600 }}>
                                            {r.unitCategory || 'RESIDENTIAL'}
                                        </span>
                                    </div>
                                    <div style={{ fontSize: '0.85rem', color: 'var(--text-secondary-light)', marginBottom: '0.5rem' }}>
                                        {r.nocApplicable && (
                                            <span style={{ color: 'var(--primary)', fontWeight: 600, display: 'block', marginBottom: '0.2rem' }}>
                                                NOC Applicable: ₹{Number(r.nocAmount || 0).toFixed(2)}/month
                                            </span>
                                        )}
                                        <div>
                                            Share Certificate: <strong>{r.shareCertificateNumber || 'Not Issued Yet'}</strong>
                                            {r.shareCertificateIssuedDate && ` (Issued: ${r.shareCertificateIssuedDate})`}
                                        </div>
                                    </div>
                                    {r.shareCertificateData || r.shareCertificateNumber ? (
                                        <Button
                                            variant="secondary"
                                            style={{
                                                display: 'inline-flex',
                                                alignItems: 'center',
                                                gap: '0.4rem',
                                                padding: '0.4rem 0.75rem',
                                                fontSize: '0.85rem'
                                            }}
                                            onClick={() => handleDownloadCertificate(r.id, r.roomNumber)}
                                        >
                                            <Download size={14} /> Download Share Certificate
                                        </Button>
                                    ) : null}
                                </div>
                            ))}
                        </div>
                    )}
                </Card>

                {/* Security Settings */}
                <Card title="Security Settings">
                    <form onSubmit={handlePasswordChange} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                        <p className="subtitle">Update your password to keep your account secure.</p>

                        <Input type="password" label="Current Password" placeholder="Enter current password" value={oldPassword} onChange={(e) => setOldPassword(e.target.value)} required />
                        <Input type="password" label="New Password" placeholder="Enter new password" value={newPassword} onChange={(e) => setNewPassword(e.target.value)} required />
                        <Input type="password" label="Confirm New Password" placeholder="Confirm new password" value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} required />

                        <Button type="submit" isLoading={isChangingPassword} style={{ alignSelf: 'flex-start', marginTop: '1rem' }}>Update Password</Button>
                    </form>
                </Card>
            </div>
        </div>
    );
};

export default ResidentProfile;
