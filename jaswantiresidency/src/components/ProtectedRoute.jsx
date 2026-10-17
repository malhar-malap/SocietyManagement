import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const ProtectedRoute = ({ children, allowedRoles }) => {
    const { user, loading } = useAuth();
    const location = useLocation();

    if (loading) {
        return (
            <div style={{ height: '100vh', display: 'flex', justifyContent: 'center', alignItems: 'center' }}>
                <div className="skeleton" style={{ width: '200px', height: '20px' }}></div>
            </div>
        );
    }

    if (!user) {
        // Not logged in, redirect to login page with the return url
        return <Navigate to="/login" state={{ from: location }} replace />;
    }

    if (allowedRoles && !allowedRoles.includes(user.role)) {
        // Logged in but doesn't have the required role
        return <Navigate to="/403" replace />;
    }

    if (user.forcePasswordChange && location.pathname !== '/setup') {
        return <Navigate to="/setup" replace />;
    }

    return children;
};

export default ProtectedRoute;
