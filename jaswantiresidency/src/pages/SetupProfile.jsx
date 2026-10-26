import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft, ArrowRight, AtSign, Check, LogOut, MailCheck, Phone, UserRound } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import api from '../api/axiosConfig';
import { passwordPolicyError } from '../utils/passwordPolicy';
import { HibiscusMark } from '../components/ui/Hibiscus';
import Button from '../components/ui/Button';
import { TextInput } from '../components/ui/Field';
import { Alert } from '../components/ui/Feedback';
import OtpInput from '../components/ui/OtpInput';
import PasswordChecklist from '../components/auth/PasswordChecklist';

const STEPS = ['About you', 'Email', 'Password', 'Verify'];
const RESEND_SECONDS = 45;
const normalisePhone = (p) => p.replace(/[\s-]/g, '').replace(/^(\+?91|0)(?=\d{10}$)/, '');
const homeFor = (role) => (role === 'ADMIN' || role === 'MASTER_ADMIN' ? '/admin/dashboard' : '/resident/dashboard');

const messageFrom = (err, fallback) => {
    const data = err?.response?.data;
    if (typeof data === 'string' && data.trim()) return data;
    return data?.message || (err?.response ? fallback : 'We could not reach the server. Check your connection and try again.');
};

const Stepper = ({ current }) => (
    <ol className="stepper" aria-label="Setup progress">
        {STEPS.map((label, i) => {
            const state = i < current ? 'done' : i === current ? 'current' : 'todo';
            return (
                <li key={label} className={`stepper-item is-${state}`} aria-current={state === 'current' ? 'step' : undefined}>
                    <span className="stepper-dot">{state === 'done' ? <Check size={13} strokeWidth={3} /> : i + 1}</span>
                    <span className="stepper-label">{label}</span>
                </li>
            );
        })}
    </ol>
);

/**
 * First sign-in: residents (and newly created admins) confirm their details, add a personal email,
 * choose a password, and verify the email with a 6-digit code before the portal opens.
 */
const SetupProfile = () => {
    const { user, login, logout } = useAuth();
    const navigate = useNavigate();
    const [step, setStep] = useState(0);
    const [form, setForm] = useState({ name: user?.name || '', phone: '', personalEmail: '', newPassword: '', confirmPassword: '' });
    const [errors, setErrors] = useState({});
    const [formError, setFormError] = useState('');
    const [busy, setBusy] = useState(false);
    const [otp, setOtp] = useState('');
    const [cooldown, setCooldown] = useState(0);
    const [notice, setNotice] = useState('');

    useEffect(() => {
        if (user && !user.forcePasswordChange) navigate(homeFor(user.role), { replace: true });
    }, [user, navigate]);

    useEffect(() => {
        if (cooldown <= 0) return undefined;
        const t = setTimeout(() => setCooldown((c) => c - 1), 1000);
        return () => clearTimeout(t);
    }, [cooldown]);

    const set = (name) => (e) => {
        setForm((f) => ({ ...f, [name]: e.target.value }));
        setErrors((er) => ({ ...er, [name]: undefined }));
        setFormError('');
    };

    const getUserId = () => user?.userId || user?.id || localStorage.getItem('userId');
    const firstName = (form.name || user?.name || '').trim().split(/\s+/)[0];

    const validate = () => {
        const e = {};
        if (step === 0) {
            if (form.name.trim().length < 2) e.name = 'Enter your full name.';
            if (!/^[6-9]\d{9}$/.test(normalisePhone(form.phone))) e.phone = 'Enter a 10-digit mobile number.';
        }
        if (step === 1 && !/^\S+@\S+\.\S+$/.test(form.personalEmail.trim())) e.personalEmail = 'Enter a valid email address.';
        if (step === 2) {
            const policy = passwordPolicyError(form.newPassword);
            if (policy) e.newPassword = policy;
            else if (form.newPassword !== form.confirmPassword) e.confirmPassword = 'Passwords do not match.';
        }
        setErrors(e);
        return Object.keys(e).length === 0;
    };

    const sendSetup = async () => {
        await api.post(`/users/${getUserId()}/setup`, {
            name: form.name.trim(),
            phone: normalisePhone(form.phone),
            personalEmail: form.personalEmail.trim().toLowerCase(),
            newPassword: form.newPassword,
        });
        setCooldown(RESEND_SECONDS);
    };

    const next = async (e) => {
        e.preventDefault();
        if (!validate()) return;
        if (step < 2) { setStep(step + 1); return; }
        setBusy(true);
        try {
            await sendSetup();
            setOtp('');
            setNotice('');
            setStep(3);
        } catch (err) {
            setFormError(messageFrom(err, 'We could not save your details. Please try again.'));
        } finally {
            setBusy(false);
        }
    };

    const resend = async () => {
        setBusy(true);
        setFormError('');
        try {
            await sendSetup();
            setOtp('');
            setNotice('A new code is on its way.');
        } catch (err) {
            setFormError(messageFrom(err, 'We could not send a new code.'));
        } finally {
            setBusy(false);
        }
    };

    const verify = async (e) => {
        e.preventDefault();
        if (otp.length !== 6) { setFormError('Enter all 6 digits from the email.'); return; }
        setBusy(true);
        setFormError('');
        try {
            const res = await api.post(`/users/${getUserId()}/verify-setup?token=${encodeURIComponent(otp)}`);
            if (res.data?.accessToken) {
                login(res.data);
                navigate(homeFor(res.data.role), { replace: true });
            } else {
                login({ ...user, forcePasswordChange: false, accessToken: localStorage.getItem('accessToken'), refreshToken: localStorage.getItem('refreshToken') });
                navigate(homeFor(user?.role), { replace: true });
            }
        } catch (err) {
            const msg = messageFrom(err, 'That code is not valid or has expired.');
            setFormError(/invalid or expired/i.test(msg) ? 'That code is not right or has expired. Check the email, or send a new code.' : msg);
            setOtp('');
        } finally {
            setBusy(false);
        }
    };

    const back = () => { setFormError(''); setStep((s) => Math.max(0, s - (s === 3 ? 2 : 1))); };

    return (
        <div className="setup">
            <header className="setup-top">
                <div className="row"><HibiscusMark size={28} /><span className="setup-brand">Jaswanti Residency</span></div>
                <Button variant="ghost" size="sm" icon={LogOut} onClick={() => { logout(); navigate('/login', { replace: true }); }}>Sign out</Button>
            </header>

            <main className="setup-main">
                <div className="setup-intro">
                    <h1>{firstName ? `Welcome, ${firstName}` : 'Welcome'}</h1>
                    <p>Let’s set up your account. It takes about a minute.</p>
                </div>

                <div className="setup-card">
                    <Stepper current={step} />

                    <form className="setup-body stack" onSubmit={step === 3 ? verify : next} noValidate key={step}>
                        {formError && <Alert tone="error">{formError}</Alert>}

                        {step === 0 && (
                            <>
                                <div className="setup-step-head"><h2>Confirm your details</h2><p>This is how the committee and your neighbours will see you.</p></div>
                                <TextInput label="Full name" icon={UserRound} autoComplete="name" value={form.name} onChange={set('name')} error={errors.name} autoFocus />
                                <TextInput label="Mobile number" icon={Phone} prefix="+91" inputMode="tel" autoComplete="tel-national" placeholder="98765 43210"
                                    value={form.phone} onChange={set('phone')} error={errors.phone} hint="Used only by the society office to reach you." />
                            </>
                        )}

                        {step === 1 && (
                            <>
                                <div className="setup-step-head"><h2>Add your personal email</h2><p>You will sign in with this email from now on, and receipts and notices are sent here.</p></div>
                                <TextInput label="Email address" type="email" icon={AtSign} autoComplete="email" placeholder="you@example.com"
                                    value={form.personalEmail} onChange={set('personalEmail')} error={errors.personalEmail} autoFocus />
                            </>
                        )}

                        {step === 2 && (
                            <>
                                <div className="setup-step-head"><h2>Choose a password</h2><p>Replace the temporary password from the office with one only you know.</p></div>
                                <TextInput label="New password" type="password" autoComplete="new-password" value={form.newPassword} onChange={set('newPassword')} error={errors.newPassword} autoFocus />
                                <PasswordChecklist password={form.newPassword} />
                                <TextInput label="Confirm password" type="password" autoComplete="new-password" value={form.confirmPassword} onChange={set('confirmPassword')} error={errors.confirmPassword} />
                            </>
                        )}

                        {step === 3 && (
                            <>
                                <div className="setup-step-head">
                                    <span className="auth-heading-icon"><MailCheck size={22} /></span>
                                    <h2>Verify your email</h2>
                                    <p>We sent a 6-digit code to <strong>{form.personalEmail.trim().toLowerCase()}</strong>.</p>
                                </div>
                                {notice && <Alert tone="success">{notice}</Alert>}
                                <OtpInput value={otp} onChange={(v) => { setOtp(v); setFormError(''); }} invalid={Boolean(formError)} autoFocus disabled={busy} />
                                <div className="auth-resend">
                                    <button type="button" className="btn btn-link btn-sm" onClick={() => { setStep(1); setFormError(''); }}>Change email</button>
                                    {cooldown > 0 ? <span>Resend in {cooldown}s</span> : (
                                        <button type="button" className="btn btn-link btn-sm" onClick={resend} disabled={busy}>Resend code</button>
                                    )}
                                </div>
                            </>
                        )}

                        <div className="setup-actions">
                            {step > 0 && step < 3 && <Button variant="ghost" icon={ArrowLeft} onClick={back} disabled={busy}>Back</Button>}
                            <Button type="submit" size="lg" isLoading={busy} className="setup-next">
                                {step < 2 && <>Continue <ArrowRight size={17} aria-hidden="true" /></>}
                                {step === 2 && 'Send verification code'}
                                {step === 3 && 'Verify and finish'}
                            </Button>
                        </div>
                    </form>
                </div>
                <p className="setup-foot">Your details are only visible to the society committee.</p>
            </main>
        </div>
    );
};

export default SetupProfile;
