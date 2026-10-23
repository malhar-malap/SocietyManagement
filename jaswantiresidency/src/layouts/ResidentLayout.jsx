import React from 'react';
import { Outlet, Navigate, NavLink } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import './layout.css';
import { Home, Bell, LogOut, DollarSign, User, AlertCircle } from 'lucide-react';

const ResidentLayout = () => {
    const { logout, user } = useAuth();

    if (!user) return <Navigate to="/login" replace />;

    return (
        <div className="layout-container">
            <aside className="sidebar">
                <div className="sidebar-brand">Jaswanti Residency</div>
                <nav className="sidebar-nav">
                    <NavLink to="/resident/dashboard" className={({isActive}) => `nav-link ${isActive ? 'active' : ''}`}><Home size={18}/> Dashboard</NavLink>
                    <NavLink to="/resident/invoices" className={({isActive}) => `nav-link ${isActive ? 'active' : ''}`}><DollarSign size={18}/> Bills & Invoices</NavLink>
                    <NavLink to="/resident/complaints" className={({isActive}) => `nav-link ${isActive ? 'active' : ''}`}><AlertCircle size={18}/> Complaints</NavLink>
                    <NavLink to="/resident/profile" className={({isActive}) => `nav-link ${isActive ? 'active' : ''}`}><User size={18}/> Profile</NavLink>
                </nav>
            </aside>
            <main className="main-content">
                <header className="topbar">
                    <h2 className="page-title">Resident Portal</h2>
                    <div className="topbar-actions">
                        <button className="icon-btn"><Bell size={20} /></button>
                        <div className="profile-menu">
                            <span className="profile-name"><User size={16}/> {user?.name || 'Resident'}</span>
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
