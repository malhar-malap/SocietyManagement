import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import Card from '../components/ui/Card';
import Input from '../components/ui/Input';
import Button from '../components/ui/Button';

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

    const handleSetupSubmit = async (e) => {
        e.preventDefault();
        setError(null);
        
        if (formData.newPassword !== formData.confirmPassword) {
            setError("Passwords do not match.");
            return;
        }

        setLoading(true);
        try {
            const token = localStorage.getItem('accessToken');
            const res = await fetch(`http://localhost:8080/api/users/${user.userId}/setup`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify(formData)
            });

            if (!res.ok) {
                const text = await res.text();
                throw new Error(text || "Failed to initiate setup");
            }

            setSuccessMessage("Setup initiated! Please check your server console (or email) for the verification token.");
            setVerificationStep(true);
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    };

    const handleVerifySubmit = async (e) => {
        e.preventDefault();
        setError(null);
        setLoading(true);

        try {
            const token = localStorage.getItem('accessToken');
            const res = await fetch(`http://localhost:8080/api/users/${user.userId}/verify-setup?token=${encodeURIComponent(verificationToken)}`, {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            });

            if (!res.ok) {
                const text = await res.text();
                throw new Error(text || "Verification failed");
            }

            // Verification successful, update context & localStorage
            const newUserData = { ...user, forcePasswordChange: false };
            login({
                ...newUserData,
                accessToken: localStorage.getItem('accessToken'),
                refreshToken: localStorage.getItem('refreshToken')
            });

            // Redirect
            navigate('/');
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '100vh', background: 'var(--background-color)' }}>
            <Card style={{ maxWidth: '500px', width: '100%' }} title="Complete Your Profile">
                {!verificationStep ? (
                    <form onSubmit={handleSetupSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                        <p style={{ color: 'var(--text-light)', marginBottom: '1rem', fontSize: '0.9rem' }}>
                            Welcome to Jaswanti Residency! For your security, you must update your profile, provide a personal email, and change your password before you can access the dashboard.
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
                            required 
                        />
                        <Input 
                            label="Personal Email" 
                            type="email" 
                            name="personalEmail" 
                            value={formData.personalEmail} 
                            onChange={handleChange} 
                            required 
                        />
                        <Input 
                            label="New Password" 
                            type="password" 
                            name="newPassword" 
                            value={formData.newPassword} 
                            onChange={handleChange} 
                            required 
                        />
                        <Input 
                            label="Confirm New Password" 
                            type="password" 
                            name="confirmPassword" 
                            value={formData.confirmPassword} 
                            onChange={handleChange} 
                            required 
                        />

                        <Button type="submit" disabled={loading} style={{ marginTop: '1rem' }}>
                            {loading ? 'Processing...' : 'Submit Profile details'}
                        </Button>
                    </form>
                ) : (
                    <form onSubmit={handleVerifySubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                        {successMessage && <div className="alert alert-success">{successMessage}</div>}
                        {error && <div className="alert alert-error">{error}</div>}
                        
                        <p style={{ color: 'var(--text-light)', marginBottom: '1rem', fontSize: '0.9rem' }}>
                            A verification token has been sent to your new email address (or logged to the backend console). Enter it below to complete setup.
                        </p>

                        <Input 
                            label="Verification Token" 
                            value={verificationToken} 
                            onChange={(e) => setVerificationToken(e.target.value)} 
                            required 
                        />

                        <Button type="submit" disabled={loading} style={{ marginTop: '1rem' }}>
                            {loading ? 'Verifying...' : 'Verify and Continue'}
                        </Button>
                    </form>
                )}
            </Card>
        </div>
    );
};

export default SetupProfile;
