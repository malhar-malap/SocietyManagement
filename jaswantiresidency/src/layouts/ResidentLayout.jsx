import React from 'react';
import { Outlet, Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import './layout.css';
import { Home, FileText, Bell, LogOut, DollarSign, User, AlertCircle } from 'lucide-react';

const ResidentLayout = () => {
    const { logout, user } = useAuth();

    if (!user) return <Navigate to="/login" replace />;

    return (
        <div className="layout-container">
            <aside className="sidebar">
                <div className="sidebar-brand">Jaswanti Residency</div>
                <nav className="sidebar-nav">
                    <a href="/resident/dashboard" className="nav-link"><Home size={18}/> Dashboard</a>
                    <a href="/resident/invoices" className="nav-link"><DollarSign size={18}/> Bills & Invoices</a>
                    <a href="/resident/complaints" className="nav-link"><AlertCircle size={18}/> Complaints</a>
                    <a href="/resident/profile" className="nav-link"><User size={18}/> Profile</a>
                </nav>
            </aside>
            <main className="main-content">
                <header className="topbar">
                    <h2 className="page-title">Resident Portal</h2>
                    <div className="topbar-actions">
                        <button className="icon-btn"><Bell size={20} /></button>
                        <div className="profile-menu">
                            <span className="profile-name"><User size={16}/> {user.name}</span>
                            <button onClick={logout} className="logout-btn"><LogOut size={16}/> Logout</button>
                        </div>
                    </div>
                </header>
                <div className="page-content">
                    <Outlet />
                </div>
            </main>
        </div>
    );
};

export default ResidentLayout;
