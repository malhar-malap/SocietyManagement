import React, { useEffect, useRef, useState } from 'react';
import { useNavigate, Link, useLocation } from 'react-router-dom';
import { ArrowRight, AtSign, KeyRound } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { login as apiLogin } from '../api/authApi';
import AuthShell from '../components/auth/AuthShell';
import Button from '../components/ui/Button';
import { TextInput } from '../components/ui/Field';
import { Alert } from '../components/ui/Feedback';

const homeFor = (role) => (role === 'ADMIN' || role === 'MASTER_ADMIN' ? '/admin/dashboard' : '/resident/dashboard');

const friendlyError = (err) => {
    if (!err?.response) return 'We could not reach the server. Check your connection and try again.';
    const { status, data } = err.response;
    const message = typeof data === 'string' ? data : data?.message;
    if (status === 429) return message || 'Too many attempts. Please wait a few minutes before trying again.';
    if (!message || /invalid username or password|bad credentials/i.test(message)) return 'That email, flat number or password is not right. Please try again.';
    return message;
};

const Login = () => {
    const [credentials, setCredentials] = useState({ identifier: '', password: '' });
    const [error, setError] = useState('');
    const [capsLock, setCapsLock] = useState(false);
    const [isLoading, setIsLoading] = useState(false);
    const { user, login } = useAuth();
    const navigate = useNavigate();
    const location = useLocation();
    const passwordRef = useRef(null);

    useEffect(() => {
        if (user && !user.forcePasswordChange) navigate(homeFor(user.role), { replace: true });
    }, [user, navigate]);

    const handleChange = (e) => {
        setCredentials({ ...credentials, [e.target.name]: e.target.value });
        if (error) setError('');
    };

    const detectCaps = (e) => setCapsLock(Boolean(e.getModifierState?.('CapsLock')));

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!credentials.identifier.trim() || !credentials.password) {
            setError('Enter your email or flat number and your password.');
            return;
        }
        setError('');
        setIsLoading(true);
        try {
            const data = await apiLogin({ identifier: credentials.identifier.trim(), password: credentials.password });
            login(data);
            if (data.forcePasswordChange) {
                navigate('/setup', { replace: true });
            } else {
                const from = location.state?.from?.pathname;
                navigate(from && from !== '/' ? from : homeFor(data.role), { replace: true });
            }
        } catch (err) {
            setError(friendlyError(err));
            setCredentials((c) => ({ ...c, password: '' }));
            passwordRef.current?.focus();
        } finally {
            setIsLoading(false);
        }
    };

    const isFirstTime = /^[a-z]{1,2}-?\d{2,4}$/i.test(credentials.identifier.trim());

    return (
        <AuthShell>
            <div className="auth-heading">
                <h1>Welcome back</h1>
                <p>Sign in to your Jaswanti Residency account.</p>
            </div>

            <form className="stack" onSubmit={handleSubmit} noValidate>
                {error && <Alert tone="error">{error}</Alert>}

                <TextInput
                    label="Email or flat number"
                    name="identifier"
                    icon={AtSign}
                    autoComplete="username"
                    placeholder="you@example.com"
                    value={credentials.identifier}
                    onChange={handleChange}
                    hint={isFirstTime ? 'Flat numbers work for your first sign-in only. Afterwards, use your verified email.' : undefined}
                    autoFocus
                />

                <div className="stack stack-sm">
                    <TextInput
                        ref={passwordRef}
                        label="Password"
                        name="password"
                        type="password"
                        icon={KeyRound}
                        autoComplete="current-password"
                        placeholder="Your password"
                        value={credentials.password}
                        onChange={handleChange}
                        onKeyUp={detectCaps}
                        onKeyDown={detectCaps}
                        hint={capsLock ? 'Caps Lock is on' : undefined}
                    />
                    <Link to="/forgot-password" className="auth-forgot">Forgot password?</Link>
                </div>

                <Button type="submit" size="lg" block isLoading={isLoading}>
                    Sign in <ArrowRight size={17} aria-hidden="true" />
                </Button>
            </form>

            <div className="auth-note">
                <strong>New resident?</strong> Your account is created by the society office. Sign in with your flat number
                and temporary password, and we will walk you through setting up your own.
            </div>
        </AuthShell>
    );
};

export default Login;
