import React, { useEffect, useMemo, useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { ArrowLeft, AtSign, CheckCircle2, MailCheck } from 'lucide-react';
import { forgotPassword, resetPassword } from '../api/authApi';
import { errorMessage, notifySuccess } from '../utils/notify';
import { passwordPolicyError } from '../utils/passwordPolicy';
import AuthShell from '../components/auth/AuthShell';
import Button from '../components/ui/Button';
import { TextInput } from '../components/ui/Field';
import { Alert } from '../components/ui/Feedback';
import OtpInput from '../components/ui/OtpInput';
import PasswordChecklist from '../components/auth/PasswordChecklist';

const RESEND_SECONDS = 45;

const ForgotPassword = () => {
    const navigate = useNavigate();
    const [step, setStep] = useState('email'); // email -> reset -> done
    const [email, setEmail] = useState('');
    const [otp, setOtp] = useState('');
    const [newPassword, setNewPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [error, setError] = useState('');
    const [busy, setBusy] = useState(false);
    const [cooldown, setCooldown] = useState(0);

    useEffect(() => {
        if (cooldown <= 0) return undefined;
        const t = setTimeout(() => setCooldown((c) => c - 1), 1000);
        return () => clearTimeout(t);
    }, [cooldown]);

    const confirmError = useMemo(
        () => (confirmPassword && confirmPassword !== newPassword ? 'Passwords do not match.' : ''),
        [confirmPassword, newPassword],
    );

    const sendCode = async (e) => {
        e?.preventDefault();
        if (!/^\S+@\S+\.\S+$/.test(email.trim())) { setError('Enter the email address you use to sign in.'); return; }
        setError('');
        setBusy(true);
        try {
            await forgotPassword(email.trim());
            setStep('reset');
            setCooldown(RESEND_SECONDS);
        } catch (err) {
            setError(errorMessage(err, 'We could not send the code. Please try again.'));
        } finally {
            setBusy(false);
        }
    };

    const reset = async (e) => {
        e.preventDefault();
        if (otp.length !== 6) { setError('Enter the 6-digit code from the email.'); return; }
        const policy = passwordPolicyError(newPassword);
        if (policy) { setError(policy); return; }
        if (newPassword !== confirmPassword) { setError('Passwords do not match.'); return; }
        setError('');
        setBusy(true);
        try {
            await resetPassword(email.trim(), otp, newPassword);
            setStep('done');
        } catch (err) {
            setError(errorMessage(err, 'That code did not work. Check it, or request a new one.'));
        } finally {
            setBusy(false);
        }
    };

    return (
        <AuthShell>
            {step === 'email' && (
                <>
                    <Link to="/login" className="auth-back"><ArrowLeft size={16} /> Back to sign in</Link>
                    <div className="auth-heading">
                        <h1>Reset your password</h1>
                        <p>Enter the email you use to sign in and we will send you a 6-digit code.</p>
                    </div>
                    <form className="stack" onSubmit={sendCode} noValidate>
                        {error && <Alert tone="error">{error}</Alert>}
                        <TextInput label="Email address" type="email" icon={AtSign} autoComplete="email" placeholder="you@example.com"
                            value={email} onChange={(e) => { setEmail(e.target.value); setError(''); }} autoFocus />
                        <Button type="submit" size="lg" block isLoading={busy}>Send code</Button>
                    </form>
                    <div className="auth-note">
                        Still on your first sign-in? Your temporary password comes from the society office — ask them to issue a new one.
                    </div>
                </>
            )}

            {step === 'reset' && (
                <>
                    <button type="button" className="auth-back" onClick={() => { setStep('email'); setError(''); setOtp(''); }}>
                        <ArrowLeft size={16} /> Use a different email
                    </button>
                    <div className="auth-heading">
                        <span className="auth-heading-icon"><MailCheck size={22} /></span>
                        <h1>Check your email</h1>
                        <p>If <strong>{email.trim()}</strong> has an account, a 6-digit code is on its way. It expires in a few minutes.</p>
                    </div>
                    <form className="stack" onSubmit={reset} noValidate>
                        {error && <Alert tone="error">{error}</Alert>}
                        <div className="field">
                            <span className="field-label">Verification code</span>
                            <OtpInput value={otp} onChange={(v) => { setOtp(v); setError(''); }} autoFocus />
                            <div className="auth-resend">
                                {cooldown > 0 ? <span>Resend code in {cooldown}s</span> : (
                                    <button type="button" className="btn btn-link btn-sm" onClick={() => sendCode()} disabled={busy}>Resend code</button>
                                )}
                            </div>
                        </div>
                        <TextInput label="New password" type="password" autoComplete="new-password" value={newPassword}
                            onChange={(e) => { setNewPassword(e.target.value); setError(''); }} />
                        <PasswordChecklist password={newPassword} />
                        <TextInput label="Confirm new password" type="password" autoComplete="new-password" value={confirmPassword}
                            onChange={(e) => { setConfirmPassword(e.target.value); setError(''); }} error={confirmError} />
                        <Button type="submit" size="lg" block isLoading={busy}>Reset password</Button>
                    </form>
                </>
            )}

            {step === 'done' && (
                <div className="auth-success">
                    <span className="auth-success-icon"><CheckCircle2 size={28} /></span>
                    <h1>Password updated</h1>
                    <p>You can now sign in with your new password.</p>
                    <Button size="lg" block onClick={() => { notifySuccess('Password reset — please sign in.'); navigate('/login', { replace: true }); }}>
                        Go to sign in
                    </Button>
                </div>
            )}
        </AuthShell>
    );
};

export default ForgotPassword;
