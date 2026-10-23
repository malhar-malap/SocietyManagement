import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import Card from '../components/ui/Card';
import Input from '../components/ui/Input';
import Button from '../components/ui/Button';
import api from '../api/axiosConfig';

const SetupProfile = () => {
    const { user, login } = useAuth();
    const navigate = useNavigate();
    const [formData, setFormData] = useState({
        name: user?.name || '',
        phone: '',
        personalEmail: '',
        newPassword: '',
        confirmPassword: ''
    });
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [successMessage, setSuccessMessage] = useState(null);
    
    // For Verification Step
    const [verificationStep, setVerificationStep] = useState(false);
    const [verificationToken, setVerificationToken] = useState('');

    const handleChange = (e) => {
        setFormData({ ...formData, [e.target.name]: e.target.value });
    };

    const getUserId = () => user?.userId || user?.id || localStorage.getItem('userId');

    const handleSetupSubmit = async (e) => {
        if (e) e.preventDefault();
        setError(null);
        
        if (formData.newPassword !== formData.confirmPassword) {
            setError("Passwords do not match.");
            return;
        }

        if (formData.newPassword.length < 6) {
            setError("New password must be at least 6 characters long.");
            return;
        }

        setLoading(true);
        try {
            const userId = getUserId();
            await api.post(`/users/${userId}/setup`, {
                name: formData.name.trim(),
                phone: formData.phone.trim(),
                personalEmail: formData.personalEmail.trim().toLowerCase(),
                newPassword: formData.newPassword
            });

            setSuccessMessage(`A 4-digit verification code has been sent to ${formData.personalEmail}. Please enter it below.`);
            setVerificationStep(true);
        } catch (err) {
            const msg = err?.response?.data || err?.message || "Failed to initiate profile setup";
            setError(typeof msg === 'string' ? msg : JSON.stringify(msg));
        } finally {
            setLoading(false);
        }
    };

    const handleResendOtp = async () => {
        setError(null);
        setLoading(true);
        try {
            const userId = getUserId();
            await api.post(`/users/${userId}/setup`, {
                name: formData.name.trim(),
                phone: formData.phone.trim(),
                personalEmail: formData.personalEmail.trim().toLowerCase(),
                newPassword: formData.newPassword
            });
            setSuccessMessage(`A fresh 4-digit verification code has been sent to ${formData.personalEmail}.`);
        } catch (err) {
            const msg = err?.response?.data || err?.message || "Failed to resend code";
            setError(typeof msg === 'string' ? msg : JSON.stringify(msg));
        } finally {
            setLoading(false);
        }
    };

    const handleVerifySubmit = async (e) => {
        e.preventDefault();
        setError(null);

        const cleanToken = (verificationToken || '').trim().replace(/\s+/g, '');
        if (!cleanToken) {
            setError("Please enter the verification code received via email.");
            return;
        }

        setLoading(true);

        try {
            const userId = getUserId();
            const res = await api.post(`/users/${userId}/verify-setup?token=${encodeURIComponent(cleanToken)}`);

            // If backend returns updated AuthResponse JSON with fresh tokens
            if (res.data && res.data.accessToken) {
                login(res.data);
            } else {
                const newUserData = { ...user, forcePasswordChange: false };
                login({
                    ...newUserData,
                    accessToken: localStorage.getItem('accessToken'),
                    refreshToken: localStorage.getItem('refreshToken')
                });
            }

            // Redirect to home/dashboard
            navigate('/');
        } catch (err) {
            const msg = err?.response?.data || err?.message || "Invalid or expired verification token.";
            setError(typeof msg === 'string' ? msg : JSON.stringify(msg));
        } finally {
            setLoading(false);
        }
    };

    return (
        <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '100vh', background: 'var(--background-color)', padding: '1rem' }}>
            <Card style={{ maxWidth: '500px', width: '100%' }} title="Complete Your Profile">
                {!verificationStep ? (
                    <form onSubmit={handleSetupSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                        <p style={{ color: 'var(--text-light)', marginBottom: '0.5rem', fontSize: '0.9rem' }}>
                            Welcome to Jaswanti Residency! For your security, you must update your profile, provide your personal email, and set your new password before accessing the portal.
                        </p>
                        
                        {error && <div className="alert alert-error">{error}</div>}

                        <Input 
                            label="Full Name" 
                            name="name" 
                            value={formData.name} 
                            onChange={handleChange} 
                            required 
                        />
                        <Input 
                            label="Phone Number" 
                            name="phone" 
                            value={formData.phone} 
                            onChange={handleChange} 
                            placeholder="e.g. 9876543210"
                            required 
                        />
                        <Input 
                            label="Personal Email" 
                            type="email" 
                            name="personalEmail" 
                            value={formData.personalEmail} 
                            onChange={handleChange} 
                            placeholder="name@example.com"
                            required 
                        />
                        <Input 
                            label="New Password" 
                            type="password" 
                            name="newPassword" 
                            value={formData.newPassword} 
                            onChange={handleChange} 
                            placeholder="Minimum 6 characters"
                            required 
                        />
                        <Input 
                            label="Confirm New Password" 
                            type="password" 
                            name="confirmPassword" 
                            value={formData.confirmPassword} 
                            onChange={handleChange} 
                            placeholder="Repeat new password"
                            required 
                        />

                        <Button type="submit" disabled={loading} style={{ marginTop: '0.5rem' }}>
                            {loading ? 'Sending Code...' : 'Submit Profile Details'}
                        </Button>
                    </form>
                ) : (
                    <form onSubmit={handleVerifySubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
                        {successMessage && <div className="alert alert-success">{successMessage}</div>}
                        {error && <div className="alert alert-error">{error}</div>}
                        
                        <p style={{ color: 'var(--text-light)', fontSize: '0.9rem' }}>
                            A 4-digit verification code was sent to <strong>{formData.personalEmail}</strong>. Enter it below to complete your setup.
                        </p>

                        <div>
                            <Input
                                label="4-Digit Verification Code"
                                value={verificationToken}
                                onChange={(e) => setVerificationToken(e.target.value)}
                                placeholder="e.g. 7336"
                                maxLength={6}
                                autoFocus
                                required
                            />
                            <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: '0.5rem', fontSize: '0.85rem' }}>
                                <button
                                    type="button"
                                    onClick={() => setVerificationStep(false)}
                                    style={{ background: 'none', border: 'none', color: 'var(--primary-color)', cursor: 'pointer', padding: 0 }}
                                >
                                    ← Edit details / email
                                </button>
                                <button
                                    type="button"
                                    onClick={handleResendOtp}
                                    disabled={loading}
                                    style={{ background: 'none', border: 'none', color: 'var(--text-light)', cursor: 'pointer', textDecoration: 'underline', padding: 0 }}
                                >
                                    Resend code
                                </button>
                            </div>
                        </div>

                        <Button type="submit" disabled={loading} style={{ marginTop: '0.5rem' }}>
                            {loading ? 'Verifying...' : 'Verify and Continue'}
                        </Button>
                    </form>
                )}
            </Card>
        </div>
    );
};

export default SetupProfile;
