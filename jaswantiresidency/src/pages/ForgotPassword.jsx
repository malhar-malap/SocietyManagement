import React, { useState } from 'react';
import { notifySuccess, notifyError, errorMessage } from '../utils/notify';
import Card from '../components/ui/Card';
import Button from '../components/ui/Button';
import Input from '../components/ui/Input';
import { KeyRound } from 'lucide-react';
import { forgotPassword, resetPassword } from '../api/authApi';
import { useNavigate, Link } from 'react-router-dom';
import './auth.css';

const ForgotPassword = () => {
    const [step, setStep] = useState(1);
    const [email, setEmail] = useState('');
    const [otp, setOtp] = useState('');
    const [newPassword, setNewPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const navigate = useNavigate();

    const [isSubmitting, setIsSubmitting] = useState(false);

    const handleSendOtp = async () => {
        if (!email.trim()) {
            notifyError('Enter your registered email address.');
            return;
        }
        setIsSubmitting(true);
        try {
            await forgotPassword(email.trim());
            setStep(2);
        } catch (e) {
            notifyError(errorMessage(e, 'Could not send the reset code. Please try again.'));
        } finally {
            setIsSubmitting(false);
        }
    };

    const handleReset = async () => {
        if (newPassword !== confirmPassword) {
            notifyError('Passwords do not match.');
            return;
        }
        setIsSubmitting(true);
        try {
            await resetPassword(email.trim(), otp.trim(), newPassword);
            notifySuccess('Password reset. Please sign in with your new password.');
            navigate('/login');
        } catch (e) {
            notifyError(errorMessage(e, 'Could not reset the password. Check the code and try again.'));
        } finally {
            setIsSubmitting(false);
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
                            <Button onClick={handleSendOtp} isLoading={isSubmitting} disabled={isSubmitting}>Send Reset OTP</Button>
                            <Link to="/login" style={{ textAlign: 'center', fontSize: '0.9rem', color: 'var(--text-secondary-light)', marginTop: '1rem' }}>Back to Login</Link>
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
                            <Button onClick={handleReset} isLoading={isSubmitting} disabled={isSubmitting} className="mt-2">Reset Password</Button>
                        </div>
                    )}
                </Card>
            </div>
        </div>
    );
};

export default ForgotPassword;
