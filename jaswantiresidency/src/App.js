import React from 'react';
import { BrowserRouter, Routes, Route, Navigate, Link } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';

// Layouts
import ResidentLayout from './layouts/ResidentLayout';
import AdminLayout from './layouts/AdminLayout';

// Public Pages
import Login from './pages/Login';
import VerifyEmail from './pages/VerifyEmail';
import ForgotPassword from './pages/ForgotPassword';
import SetupProfile from './pages/SetupProfile';

// Resident Pages
import ResidentDashboard from './pages/ResidentDashboard';
import ResidentInvoices from './pages/ResidentInvoices';
import ResidentComplaints from './pages/ResidentComplaints';
import ResidentProfile from './pages/ResidentProfile';

// Admin Pages
import AdminDashboard from './pages/AdminDashboard';
import AdminResidents from './pages/AdminResidents';
import AdminRooms from './pages/AdminRooms';
import AdminBilling from './pages/AdminBilling';
import AdminTransactions from './pages/AdminTransactions';
import AdminComplaints from './pages/AdminComplaints';
import AdminAnnouncements from './pages/AdminAnnouncements';
import AdminActivityLogs from './pages/AdminActivityLogs';

const RootRedirect = () => {
    const { user, loading } = useAuth();
    if (loading) return null;
    if (!user) return <Navigate to="/login" replace />;
    if (user.forcePasswordChange) return <Navigate to="/setup" replace />;
    if (user.role === 'ADMIN' || user.role === 'MASTER_ADMIN') {
        return <Navigate to="/admin/dashboard" replace />;
    }
    return <Navigate to="/resident/dashboard" replace />;
};

const ErrorPage = ({ code, message }) => {
    const { user } = useAuth();
    const homeTarget = !user
        ? "/login"
        : (user.role === 'ADMIN' || user.role === 'MASTER_ADMIN' ? "/admin/dashboard" : "/resident/dashboard");

    return (
        <div style={{ padding: '4rem 2rem', textAlign: 'center' }}>
            <h1 style={{ fontSize: '4rem', color: 'var(--primary-color)' }}>{code}</h1>
            <h2>{message}</h2>
            <Link to={homeTarget} style={{ color: 'var(--primary-color)', marginTop: '2rem', display: 'inline-block', fontWeight: 600 }}>
                Return to Dashboard
            </Link>
        </div>
    );
};

function App() {
    return (
        <AuthProvider>
            <BrowserRouter>
                <Routes>
                    {/* Public Routes */}
                    <Route path="/" element={<RootRedirect />} />
                    <Route path="/login" element={<Login />} />
                    <Route path="/verify-email" element={<VerifyEmail />} />
                    <Route path="/forgot-password" element={<ForgotPassword />} />
                    <Route path="/setup" element={<ProtectedRoute><SetupProfile /></ProtectedRoute>} />

                    {/* Resident Routes */}
                    <Route path="/resident" element={<ProtectedRoute allowedRoles={['RESIDENT']}><ResidentLayout /></ProtectedRoute>}>
                        <Route index element={<Navigate to="/resident/dashboard" replace />} />
                        <Route path="dashboard" element={<ResidentDashboard />} />
                        <Route path="invoices" element={<ResidentInvoices />} />
                        <Route path="complaints" element={<ResidentComplaints />} />
                        <Route path="profile" element={<ResidentProfile />} />
                    </Route>

                    {/* Admin Routes */}
                    <Route path="/admin" element={<ProtectedRoute allowedRoles={['ADMIN', 'MASTER_ADMIN']}><AdminLayout /></ProtectedRoute>}>
                        <Route index element={<Navigate to="/admin/dashboard" replace />} />
                        <Route path="dashboard" element={<AdminDashboard />} />
                        <Route path="rooms" element={<AdminRooms />} />
                        <Route path="residents" element={<AdminResidents />} />
                        <Route path="users" element={<AdminResidents />} />
                        <Route path="billing" element={<AdminBilling />} />
                        <Route path="transactions" element={<AdminTransactions />} />
                        <Route path="complaints" element={<AdminComplaints />} />
                        <Route path="announcements" element={<AdminAnnouncements />} />
                        <Route path="activity-logs" element={<AdminActivityLogs />} />
                    </Route>

                    {/* Error Pages */}
                    <Route path="/403" element={<ErrorPage code="403" message="Access Forbidden" />} />
                    <Route path="*" element={<ErrorPage code="404" message="Page Not Found" />} />
                </Routes>
            </BrowserRouter>
        </AuthProvider>
    );
}

export default App;
