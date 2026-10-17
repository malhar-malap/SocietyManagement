import React, { useState, useEffect } from 'react';
import PageHeader from '../components/ui/PageHeader';
import Card from '../components/ui/Card';
import Button from '../components/ui/Button';
import Input from '../components/ui/Input';
import { useAuth } from '../context/AuthContext';
import { Mail, Phone, Home, ShieldCheck } from 'lucide-react';

const ResidentProfile = () => {
    const { user } = useAuth();
    const [profile, setProfile] = useState(null);
    const [isLoading, setIsLoading] = useState(true);
    
    const [oldPassword, setOldPassword] = useState('');
    const [newPassword, setNewPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [isChangingPassword, setIsChangingPassword] = useState(false);

    useEffect(() => {
        const fetchProfile = async () => {
            if (!user?.userId) return;
            try {
                const { getResidentProfile } = await import('../api/residentApi');
                const data = await getResidentProfile(user.userId);
                setProfile({
                    name: data.fullName,
                    email: data.email,
                    phone: data.phoneNumber || 'Not Provided',
                    room: data.roomNumber || 'Unassigned',
                    isEmailVerified: data.status === 'ACTIVE'
                });
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

    const handlePasswordChange = async (e) => {
        e.preventDefault();
        if (newPassword !== confirmPassword) {
            alert('New passwords do not match');
            return;
        }
        try {
            setIsChangingPassword(true);
            const { changePassword } = await import('../api/residentApi');
            await changePassword(user.userId, { oldPassword, newPassword });
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

    return (
        <div>
            <PageHeader 
                title="My Profile" 
                breadcrumbs={['Resident', 'Profile']} 
            />

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(350px, 1fr))', gap: '2rem' }}>
                <Card title="Personal Details">
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                            <Home className="text-secondary-light" />
                            <div>
                                <p className="input-label" style={{margin: 0}}>Room Number</p>
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
                </Card>

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
