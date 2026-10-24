import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import Card from '../components/ui/Card';
import Input from '../components/ui/Input';
import Button from '../components/ui/Button';
import { Eye, EyeOff, LogOut } from 'lucide-react';
import logo from '../assets/logo.png';
import api from '../api/axiosConfig';

const SetupProfile = () => {
    const { user, login, logout } = useAuth();
    const navigate = useNavigate();
    const [formData, setFormData] = useState({
        name: user?.name || '',
        phone: '',
        personalEmail: '',
        newPassword: '',
        confirmPassword: ''
    });
    const [showNewPassword, setShowNewPassword] = useState(false);
    const [showConfirmPassword, setShowConfirmPassword] = useState(false);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [successMessage, setSuccessMessage] = useState(null);

    // For Verification Step
    const [verificationStep, setVerificationStep] = useState(false);
    const [verificationToken, setVerificationToken] = useState('');

    useEffect(() => {
        if (user && !user.forcePasswordChange) {
            if (user.role === 'ADMIN' || user.role === 'MASTER_ADMIN') {
                navigate('/admin/dashboard', { replace: true });
            } else {
                navigate('/resident/dashboard', { replace: true });
            }
        }
    }, [user, navigate]);

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

            setSuccessMessage(`A 6-digit verification code has been sent to ${formData.personalEmail}. Please enter it below.`);
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
            setSuccessMessage(`A fresh 6-digit verification code has been sent to ${formData.personalEmail}.`);
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
            let destination = '/resident/dashboard';
            if (res.data && res.data.accessToken) {
                login(res.data);
                if (res.data.role === 'ADMIN' || res.data.role === 'MASTER_ADMIN') {
                    destination = '/admin/dashboard';
                }
            } else {
                const newUserData = { ...user, forcePasswordChange: false };
                login({
                    ...newUserData,
                    accessToken: localStorage.getItem('accessToken'),
                    refreshToken: localStorage.getItem('refreshToken')
                });
                if (user?.role === 'ADMIN' || user?.role === 'MASTER_ADMIN') {
                    destination = '/admin/dashboard';
                }
            }

            // Directly navigate to dashboard without logging out
            navigate(destination, { replace: true });
        } catch (err) {
            const msg = err?.response?.data || err?.message || "Invalid or expired verification token.";
            setError(typeof msg === 'string' ? msg : JSON.stringify(msg));
        } finally {
            setLoading(false);
        }
    };

    return (
        <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '100vh', background: 'var(--background-color)', padding: '1rem' }}>
            <div style={{ maxWidth: '500px', width: '100%' }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1rem' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', fontWeight: 700, color: 'var(--primary-color)' }}>
                    <img src={logo} alt="" style={{ height: 32 }} />
                    Jaswanti Residency
                </div>
                {/* Someone signed into the wrong account must be able to leave this screen */}
                <button type="button" className="logout-btn" onClick={() => { logout(); navigate('/login', { replace: true }); }}
                        style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', background: 'none', border: 'none', color: 'var(--primary-color)', fontWeight: 600, cursor: 'pointer' }}>
                    <LogOut size={16} /> Sign out
                </button>
            </div>
            <Card style={{ width: '100%' }} title="Complete Your Profile">
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
                        <div style={{ position: 'relative' }}>
                            <Input
                                label="New Password"
                                type={showNewPassword ? "text" : "password"}
                                name="newPassword"
                                value={formData.newPassword}
                                onChange={handleChange}
                                placeholder="Minimum 6 characters"
                                required
                            />
                            <button
                                type="button"
                                onClick={() => setShowNewPassword(!showNewPassword)}
                                style={{
                                    position: 'absolute',
                                    right: '12px',
                                    top: '36px',
                                    background: 'none',
                                    border: 'none',
                                    cursor: 'pointer',
                                    color: 'var(--text-secondary-light)'
                                }}
                                title={showNewPassword ? "Hide password" : "Show password"}
                            >
                                {showNewPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                            </button>
                        </div>

                        <div style={{ position: 'relative' }}>
                            <Input
                                label="Confirm New Password"
                                type={showConfirmPassword ? "text" : "password"}
                                name="confirmPassword"
                                value={formData.confirmPassword}
                                onChange={handleChange}
                                placeholder="Repeat new password"
                                required
                            />
                            <button
                                type="button"
                                onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                                style={{
                                    position: 'absolute',
                                    right: '12px',
                                    top: '36px',
                                    background: 'none',
                                    border: 'none',
                                    cursor: 'pointer',
                                    color: 'var(--text-secondary-light)'
                                }}
                                title={showConfirmPassword ? "Hide password" : "Show password"}
                            >
                                {showConfirmPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                            </button>
                        </div>

                        <Button type="submit" disabled={loading} style={{ marginTop: '0.5rem' }}>
                            {loading ? 'Sending Code...' : 'Submit Profile Details'}
                        </Button>
                    </form>
                ) : (
                    <form onSubmit={handleVerifySubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
                        {successMessage && <div className="alert alert-success">{successMessage}</div>}
                        {error && <div className="alert alert-error">{error}</div>}

                        <p style={{ color: 'var(--text-light)', fontSize: '0.9rem' }}>
                            A 6-digit verification code was sent to <strong>{formData.personalEmail}</strong>. Enter it below to complete your setup.
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
        </div>
    );
};

export default SetupProfile;
