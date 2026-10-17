import React, { useState } from 'react';
import Card from '../components/ui/Card';
import Button from '../components/ui/Button';
import Input from '../components/ui/Input';
import { KeyRound } from 'lucide-react';
import { forgotPassword, resetPassword } from '../api/authApi';
import { useNavigate } from 'react-router-dom';
import './auth.css';

const ForgotPassword = () => {
    const [step, setStep] = useState(1);
    const [email, setEmail] = useState('');
    const [otp, setOtp] = useState('');
    const [newPassword, setNewPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const navigate = useNavigate();

    const handleSendOtp = async () => {
        try {
            await forgotPassword(email);
            setStep(2);
        } catch (e) {
            alert('Failed to send OTP');
        }
    };
    
    const handleReset = async () => {
        if (newPassword !== confirmPassword) {
            alert('Passwords do not match');
            return;
        }
        try {
            await resetPassword(email, otp, newPassword);
            alert('Password Reset Successfully!');
            navigate('/login');
        } catch (e) {
            alert('Failed to reset password');
        }
    };
    
    return (
        <div className="auth-container">
            <div className="auth-card-wrapper glass">
                <Card>
                    <div style={{ textAlign: 'center', marginBottom: '1.5rem' }}>
                        <div style={{ display: 'inline-flex', padding: '1rem', backgroundColor: 'rgba(79, 70, 229, 0.1)', borderRadius: '50%', color: 'var(--primary-color)', marginBottom: '1rem' }}>
                            <KeyRound size={32} />
                        </div>
                        <h2 className="dialog-title">Forgot Password</h2>
                        <p className="subtitle">Let's get you back into your account</p>
                    </div>

                    {step === 1 ? (
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                            <Input label="Registered Email" placeholder="Enter your email address" value={email} onChange={(e) => setEmail(e.target.value)} />
                            <Button onClick={handleSendOtp}>Send Reset OTP</Button>
                            <a href="/login" style={{ textAlign: 'center', fontSize: '0.9rem', color: 'var(--text-secondary-light)', marginTop: '1rem' }}>Back to Login</a>
                        </div>
                    ) : step === 2 ? (
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                            <Input label="Enter OTP" placeholder="• • • • • •" style={{ letterSpacing: '0.5rem', textAlign: 'center', fontSize: '1.5rem' }} value={otp} onChange={(e) => setOtp(e.target.value)} />
                            <Button onClick={() => setStep(3)}>Verify OTP</Button>
                            <Button variant="secondary" onClick={() => setStep(1)}>Back</Button>
                        </div>
                    ) : (
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                            <Input type="password" label="New Password" placeholder="Enter new password" value={newPassword} onChange={(e) => setNewPassword(e.target.value)} />
                            <Input type="password" label="Confirm Password" placeholder="Confirm new password" value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} />
                            <Button onClick={handleReset} className="mt-2">Reset Password</Button>
                        </div>
                    )}
                </Card>
            </div>
        </div>
    );
};

export default ForgotPassword;
