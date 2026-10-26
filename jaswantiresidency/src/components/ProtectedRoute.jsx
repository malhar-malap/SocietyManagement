import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { PageLoader } from './ui/Hibiscus';

const ProtectedRoute = ({ children, allowedRoles }) => {
    const { user, loading } = useAuth();
    const location = useLocation();

    if (loading) return <PageLoader fullScreen label="Opening your portal" />;

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
