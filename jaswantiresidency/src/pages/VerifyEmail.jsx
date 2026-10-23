import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import Card from '../components/ui/Card';
import Button from '../components/ui/Button';
import Input from '../components/ui/Input';
import { MailCheck } from 'lucide-react';
import './auth.css';

const VerifyEmail = () => {
    const [step, setStep] = useState(1);
    
    return (
        <div className="auth-container">
            <div className="auth-card-wrapper glass">
                <Card>
                    <div style={{ textAlign: 'center', marginBottom: '1.5rem' }}>
                        <div style={{ display: 'inline-flex', padding: '1rem', backgroundColor: 'rgba(79, 70, 229, 0.1)', borderRadius: '50%', color: 'var(--primary-color)', marginBottom: '1rem' }}>
                            <MailCheck size={32} />
                        </div>
                        <h2 className="dialog-title">Verify Your Email</h2>
                        <p className="subtitle">Secure your resident account</p>
                    </div>

                    {step === 1 ? (
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                            <Input label="Email Address" placeholder="Enter your registered email" />
                            <Button onClick={() => setStep(2)}>Send OTP</Button>
                            <Link to="/login" style={{ textAlign: 'center', fontSize: '0.9rem', color: 'var(--text-secondary-light)', marginTop: '1rem' }}>Back to Login</Link>
                        </div>
                    ) : (
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                            <p className="subtitle text-center">We've sent a 6-digit OTP to your email.</p>
                            <Input label="Enter OTP" placeholder="• • • • • •" style={{ letterSpacing: '0.5rem', textAlign: 'center', fontSize: '1.5rem' }} />
                            <Button onClick={() => alert('Email Verified Successfully!')}>Verify & Continue</Button>
                            <Button variant="secondary" onClick={() => setStep(1)}>Use a different email</Button>
                        </div>
                    )}
                </Card>
            </div>
        </div>
    );
};

export default VerifyEmail;
