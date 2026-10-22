import React from 'react';
import { Outlet, Navigate, NavLink } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import './layout.css';
import { Home, Users, DollarSign, Activity, FileText, LogOut, Bell, Search } from 'lucide-react';
import logo from '../assets/logo.png';

const AdminLayout = () => {
    const { logout, user } = useAuth();

    if (!user) return <Navigate to="/login" replace />;

    return (
        <div className="layout-container">
            <aside className="sidebar">
                <div className="sidebar-brand">
                    <img src={logo} alt="Jaswanti Logo" />
                    <span>
                        JASWANTI
                        <span className="brand-sub">Admin Portal</span>
                    </span>
                </div>
                <nav className="sidebar-nav">
                    <NavLink to="/admin/dashboard" className="nav-link"><Home size={18}/> Dashboard</NavLink>
                    <NavLink to="/admin/rooms" className="nav-link"><Home size={18}/> Rooms</NavLink>
                    <NavLink to="/admin/residents" className="nav-link"><Users size={18}/> {user?.role === 'MASTER_ADMIN' ? 'Users & Roles' : 'Residents'}</NavLink>
                    <NavLink to="/admin/billing" className="nav-link"><DollarSign size={18}/> Billing</NavLink>
                    <NavLink to="/admin/transactions" className="nav-link"><Activity size={18}/> Transactions</NavLink>
                    <NavLink to="/admin/complaints" className="nav-link"><FileText size={18}/> Complaints</NavLink>
                    <NavLink to="/admin/announcements" className="nav-link"><Bell size={18}/> Announcements</NavLink>
                    <NavLink to="/admin/activity-logs" className="nav-link"><Activity size={18}/> Logs</NavLink>
                </nav>
                <div className="sidebar-footer">
                    <div className="sidebar-profile">
                        <div className="sidebar-avatar">
                            {user.name ? user.name.charAt(0).toUpperCase() : 'A'}
                        </div>
                        <div className="sidebar-profile-info">
                            <span className="sidebar-profile-name">{user.name}</span>
                            <span className="sidebar-profile-role">{user.role === 'MASTER_ADMIN' ? 'Master Administrator' : 'Administrator'}</span>
                            <span className="sidebar-profile-status">Online</span>
                        </div>
                    </div>
                </div>
            </aside>
            <main className="main-content">
                <header className="topbar">
                    <h2 className="page-title">Management Dashboard</h2>
                    <div className="topbar-actions">
                        <button className="icon-btn" title="Search"><Search size={20} /></button>
                        <button className="icon-btn" title="Notifications">
                            <div style={{ position: 'relative' }}>
                                <Bell size={20} />
                                <span style={{ position: 'absolute', top: -5, right: -5, background: 'var(--primary-color)', color: 'white', borderRadius: '50%', fontSize: '0.6rem', width: 14, height: 14, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>3</span>
                            </div>
                        </button>
                        <div className="profile-menu">
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

export default AdminLayout;
