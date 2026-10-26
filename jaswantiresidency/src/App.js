import React, { Suspense, lazy } from 'react';
import { BrowserRouter, Routes, Route, Navigate, Link } from 'react-router-dom';
import { ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import { AuthProvider, useAuth } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import { HibiscusMark, PageLoader } from './components/ui/Hibiscus';

// Layouts
import ResidentLayout from './layouts/ResidentLayout';
import AdminLayout from './layouts/AdminLayout';

// Public Pages
import Login from './pages/Login';
import ForgotPassword from './pages/ForgotPassword';
import SetupProfile from './pages/SetupProfile';

// Resident Pages
const ResidentDashboard = lazy(() => import('./pages/ResidentDashboard'));
const ResidentInvoices = lazy(() => import('./pages/ResidentInvoices'));
const ResidentComplaints = lazy(() => import('./pages/ResidentComplaints'));
const ResidentProfile = lazy(() => import('./pages/ResidentProfile'));

// Admin Pages
const AdminDashboard = lazy(() => import('./pages/AdminDashboard'));
const AdminResidents = lazy(() => import('./pages/AdminResidents'));
const AdminRooms = lazy(() => import('./pages/AdminRooms'));
const AdminBilling = lazy(() => import('./pages/AdminBilling'));
const AdminTransactions = lazy(() => import('./pages/AdminTransactions'));
const AdminComplaints = lazy(() => import('./pages/AdminComplaints'));
const AdminAnnouncements = lazy(() => import('./pages/AdminAnnouncements'));
const SystemLogs = lazy(() => import('./pages/SystemLogs'));

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

const ErrorPage = ({ code, title, message }) => {
    const { user } = useAuth();
    const homeTarget = !user
        ? '/login'
        : (user.role === 'ADMIN' || user.role === 'MASTER_ADMIN' ? '/admin/dashboard' : '/resident/dashboard');

    return (
        <main className="error-page">
            <HibiscusMark size={56} />
            <p className="error-code">{code}</p>
            <h1>{title}</h1>
            <p className="text-muted">{message}</p>
            <Link to={homeTarget} className="btn btn-primary btn-lg">{user ? 'Back to my dashboard' : 'Go to sign in'}</Link>
        </main>
    );
};

function App() {
    return (
        <AuthProvider>
            <BrowserRouter>
                <Suspense fallback={<PageLoader label="Loading" />}>
                <Routes>
                    {/* Public Routes */}
                    <Route path="/" element={<RootRedirect />} />
                    <Route path="/login" element={<Login />} />
                    {/* Email verification happens inside the first-login setup flow */}
                    <Route path="/verify-email" element={<Navigate to="/setup" replace />} />
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
                        <Route path="system-logs" element={<ProtectedRoute allowedRoles={['MASTER_ADMIN']}><SystemLogs /></ProtectedRoute>} />
                        <Route path="activity-logs" element={<Navigate to="/admin/system-logs" replace />} />
                    </Route>

                    {/* Error Pages */}
                    <Route path="/403" element={<ErrorPage code="403" title="This page is not for your account" message="You are signed in, but your role does not have access here." />} />
                    <Route path="*" element={<ErrorPage code="404" title="We could not find that page" message="The link may be old, or the page may have moved." />} />
                </Routes>
                </Suspense>
            </BrowserRouter>
            <ToastContainer position="top-right" autoClose={4000} newestOnTop pauseOnFocusLoss={false} hideProgressBar closeOnClick theme="light" />
        </AuthProvider>
    );
}

export default App;
