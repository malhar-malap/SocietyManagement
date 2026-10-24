import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Building2, CreditCard, Bell, Shield, Mail, Lock, Eye, EyeOff } from 'lucide-react';
import './auth.css';

const Login = () => {
    const [credentials, setCredentials] = useState({ identifier: '', password: '' });
    const [showPassword, setShowPassword] = useState(false);
    const [error, setError] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const { user, login } = useAuth();
    const navigate = useNavigate();

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
        setCredentials({ ...credentials, [e.target.name]: e.target.value });
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setIsLoading(true);

        try {
            const { login: apiLogin } = await import('../api/authApi');
            const data = await apiLogin(credentials);
            login(data);

            if (data.role === 'ADMIN' || data.role === 'MASTER_ADMIN') {
                navigate('/admin/dashboard');
            } else {
                navigate('/resident/dashboard');
            }
        } catch (err) {
            setError(err.response?.data?.message || 'Login failed. Please check your credentials.');
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="login-split-container animate-fade-up">
            
            {/* Left Panel: Visual Experience */}
            <div className="login-left-panel">
                <div className="left-panel-content">
                    
                    {/* Header */}
                    <div className="brand-header">
                        <div className="logo-box">
                            <Building2 size={24} color="white" />
                        </div>
                        <span className="brand-name">Jaswanti Residency</span>
                    </div>

                    {/* Headline */}
                    <div className="hero-text-container">
                        <h1 className="hero-title">Welcome to Jaswanti Residency</h1>
                        <p className="hero-tagline">Smart, Secure & Seamless Society Management</p>
                    </div>

                    {/* 3D Illustration */}
                    <div className="illustration-wrapper">
                        <img 
                            src="/login_illustration.png" 
                            alt="Premium 3D Isometric Residential Building" 
                            className="premium-illustration animate-scale-in" 
                        />
                    </div>

                    {/* Feature Highlights */}
                    <div className="feature-highlights">
                        <div className="feature-item">
                            <CreditCard size={18} className="feature-icon" />
                            <span>Online Maintenance Payments</span>
                        </div>
                        <div className="feature-item">
                            <Bell size={18} className="feature-icon" />
                            <span>Society Announcements</span>
                        </div>
                        <div className="feature-item">
                            <Shield size={18} className="feature-icon" />
                            <span>Complaint Tracking</span>
                        </div>
                        <div className="feature-item">
                            <FileTextIcon size={18} className="feature-icon" />
                            <span>Instant Invoice Downloads</span>
                        </div>
                    </div>
                </div>
            </div>

            {/* Right Panel: Login Card */}
            <div className="login-right-panel">
                <div className="login-card glass">
                    <div className="mobile-logo-header">
                        <div className="logo-box mobile">
                            <Building2 size={20} color="white" />
                        </div>
                        <span className="brand-name mobile">Jaswanti Residency</span>
                    </div>

                    <h2 className="login-card-title">Sign in to your account</h2>
                    <p className="login-card-subtitle">Enter your details to access the portal.</p>

                    <form onSubmit={handleSubmit} className="login-form">
                        
                        <div className="input-group-premium">
                            <label className="input-label">Email or Room Number</label>
                            <div className="input-icon-wrapper">
                                <Mail size={18} className="input-icon" />
                                <input
                                    type="text"
                                    name="identifier"
                                    className="custom-input with-icon"
                                    value={credentials.identifier}
                                    onChange={handleChange}
                                    placeholder="Enter your email"
                                    required
                                />
                            </div>
                        </div>

                        <div className="input-group-premium">
                            <label className="input-label">Password</label>
                            <div className="input-icon-wrapper">
                                <Lock size={18} className="input-icon" />
                                <input
                                    type={showPassword ? "text" : "password"}
                                    name="password"
                                    className="custom-input with-icon"
                                    value={credentials.password}
                                    onChange={handleChange}
                                    placeholder="••••••••"
                                    required
                                />
                                <button 
                                    type="button" 
                                    className="password-toggle"
                                    onClick={() => setShowPassword(!showPassword)}
                                    style={{ position: 'absolute', right: '1rem', background: 'none', border: 'none', cursor: 'pointer', color: '#94A3B8' }}
                                >
                                    {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                                </button>
                            </div>
                        </div>

                        <div className="form-actions-row" style={{ justifyContent: 'flex-end' }}>
                            <Link to="/forgot-password" className="forgot-link">Forgot Password?</Link>
                        </div>

                        {error && (
                            <div className="error-banner">
                                {error}
                            </div>
                        )}

                        <button 
                            type="submit" 
                            className="custom-btn primary full-width login-btn"
                            disabled={isLoading}
                        >
                            {isLoading ? <span className="spinner"></span> : 'Sign In'}
                        </button>

                    </form>
                </div>
            </div>
        </div>
    );
};

// Helper component for lucide FileText missing import occasionally
const FileTextIcon = ({size, className}) => (
    <svg xmlns="http://www.w3.org/2000/svg" width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className={className}>
        <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
        <polyline points="14 2 14 8 20 8"></polyline>
        <line x1="16" y1="13" x2="8" y2="13"></line>
        <line x1="16" y1="17" x2="8" y2="17"></line>
        <polyline points="10 9 9 9 8 9"></polyline>
    </svg>
)

export default Login;
